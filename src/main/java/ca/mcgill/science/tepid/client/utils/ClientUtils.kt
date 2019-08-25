package ca.mcgill.science.tepid.client.utils

import ca.mcgill.science.tepid.api.ITepid
import ca.mcgill.science.tepid.api.TepidApi
import ca.mcgill.science.tepid.api.addJobDataFromInput
import ca.mcgill.science.tepid.api.executeDirect
import ca.mcgill.science.tepid.client.interfaces.EventObservable
import ca.mcgill.science.tepid.client.models.*
import ca.mcgill.science.tepid.models.data.Destination
import ca.mcgill.science.tepid.models.data.ErrorResponse
import ca.mcgill.science.tepid.models.data.PrintJob
import ca.mcgill.science.tepid.models.data.Session
import ca.mcgill.science.tepid.models.enums.PrintError
import ca.mcgill.science.tepid.utils.WithLogging
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.glassfish.jersey.jackson.JacksonFeature
import org.tukaani.xz.LZMA2Options
import org.tukaani.xz.XZOutputStream
import java.io.IOException
import java.io.InputStream
import java.lang.Math.pow
import java.util.*
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.MediaType
import javax.ws.rs.ext.WriterInterceptor

class ClientException(message: String) : RuntimeException(message)

fun fail(message: String): Nothing = throw ClientException(message)

fun isInterrupted() = Thread.currentThread().isInterrupted

object ClientUtils : WithLogging() {

    fun newId() = UUID.randomUUID().toString().replace("-", "")

    val api: ITepid by lazy {
        TepidApi(Config.SERVER_URL, Config.DEBUG).create {
            tokenRetriever = Auth::tokenHeader
        }
    }

    val apiNoAuth: ITepid by lazy {
        TepidApi(Config.SERVER_URL, Config.DEBUG).create()
    }

    val objectMapper = jacksonObjectMapper()

    val hostname: String? by lazy {
        var _p: Process? = null
        var _s: Scanner? = null
        try {
            val p = ProcessBuilder("hostname").start()
            _p = p
            val s = Scanner(p.inputStream)
            _s = s
            p.waitFor()
            if (s.useDelimiter("\\A").hasNext()) {
                val hostname = s.next().trim { it <= ' ' }
                log.info("Found hostname: $hostname")
                return@lazy hostname
            }
        } catch (e: Exception) {
            log.error("Failed to get hostname", e)
        } finally {
            _p?.destroy()
            _s?.close()
        }
        null
    }

    fun wildcardMatch(pattern: String, input: String?): Boolean {
        input ?: return false
        return pattern.split(";", "|").map {
            "\\Q${it.replace("*", "\\E.*?\\Q")}\\E"
        }.any {
            input.matches(it.toRegex())
        }
    }

    fun consumeStream(stream: InputStream) {
        try {
            val buf = ByteArray(4092)
            while (stream.read(buf, 0, buf.size) > -1);
        } catch (ignored: IOException) {
        } finally {
            stream.close()
        }
    }

    /**
     * Target used to compress file stream
     */
    private val tepidServerXz: WebTarget by lazy {
        ClientBuilder.newBuilder()
                .register(JacksonFeature::class.java)
                .register(WriterInterceptor { ctx ->
                    val output = ctx.outputStream
                    ctx.outputStream = XZOutputStream(output, LZMA2Options())
                    ctx.proceed()
                }).build().target(Config.SERVER_URL)
    }

    /**
     * Pure function to execute the print request
     * @param job complete [PrintJob] data, except for the data stream
     * @param stream data stream for file
     * @param session session data that must be valid. This will not be tested within this method
     * @param emitter observable to consume status updates
     * @return callable returning true if successful and false otherwise
     */
    fun print(job: PrintJob, stream: InputStream, session: Session, emitter: EventObservable): (() -> Boolean)? {
        log.debug(job)

        log.debug("Creating new print job")

        // todo short user shouldn't be nullable anyways
        val user = session.user.shortUser ?: return null

        val authHeader = session.authHeader

        val api = TepidApi(Config.SERVER_URL, Config.DEBUG).create {
            tokenRetriever = { authHeader }
        }

        val putJob = api.createNewJob(job).executeDirect()

        if (putJob?.ok != true) {
            log.error("Could not properly create new job")
            emitter.notify(Failed(job._id ?: "createerror", null,  Fail.IMMEDIATE, "Failed to send job"))
            consumeStream(stream)
            return null
        }

        val jobId = putJob.id

        log.debug("Sending job data for $jobId")

        fun failJob(failText: String = PrintError.GENERIC.display): () -> Boolean {
            job.fail(failText) // we will emulate the failure change to stay consistent
            return fun(): Boolean {
                val fail = Fail.fromText(job.error!!) //job.fail will set this as non-null
                emitter.notify(Failed(job.getId(), job, fail, "")) // todo
                return false
            }
        }

        // TODO: implement to use ErrorResponse.
        // The problem with a typed response (PutResponse) is that when an ErrorResponse is returned it will try to deserialize to the wrong type, resulting in a deserialization error. The correct thing to do is to set up retrofit to expect different things on failure or success
        // Not to undermine the above, but this will only catch errors submitting the job to be printed, not any actual errors printing it.
        try {
            val response = api.addJobDataFromInput("jobId", stream).execute()
            if (!response.isSuccessful){
                return failJob()
            }
            log.debug("Job sent: ${response.body()}")

            return { watchJob(jobId, user, api, emitter) }
        } catch (e: Exception){

            return failJob()
        }
    }

    /**
     * Watch the job
     * Returns [true] if a success response was captured
     */
    private fun watchJob(jobId: String, user: String, api: ITepid, emitter: EventObservable): Boolean {
        return JobWatcher(api, emitter).watchJob(jobId, user)
    }
}

class JobWatcher(val api: ITepid, val emitter: EventObservable) : WithLogging() {

    lateinit var status:Status

    /**
     * Notifications update on the transition between Statuses so they only fire once.
     */
    fun watchJob(jobId: String, user: String): Boolean {
        log.info("Starting job watcher")
        val origJob = getJob(jobId) ?: return false

        emitter.notify(Processing(jobId, origJob))
        status = Status.PROCESSING
        log.info("Processing")

        for (attempt in 1..10) {
//======Setup==================================================
            log.debug("Watch Attempt $attempt")
            if (isInterrupted()) {
                log.info("Watcher interrupted")
                return false
            }

            Thread.sleep((1000 * pow(1.15, attempt.toDouble())).toLong())
            val job = getJob(jobId) ?: return false
            log.debug("Job snapshot $job")

//====If Failed================================================
            if (job.failed != -1L){
                val fail = Fail.fromText(job.error ?: "") //might actually null, but still failed
                emitter.notify(Failed(job.getId(), job, fail, "")) // todo: add help message text from screensaver's config
                return false
            }

//====Normal Status Processing=================================
            if (status == Status.PROCESSING){
                //transition to sending
                if (job.processed != -1L && job.destination != null){
                    status = Status.SENDING
                    emitter.notify(Sending(jobId, job, getDestinations()[job.destination!!]!!)) // todo notify error if null
                }
            }

            if (status == Status.SENDING){
                //transition to printed
                if (job.printed != -1L){
                    status = Status.PRINTED
                    val quota = api.getQuota(user).executeDirect()
                    if (quota != null) {
                        val oldQuota = quota + job.colorPages * 2 + job.pages
                        emitter.notify(Completed(jobId, job, getDestinations()[job.destination!!]!!, oldQuota, quota))
                    }
                    else{
                        log.warn("Could not fetch quota; cannot display quota counter")
                    }
                }
            }

            if (status == Status.PRINTED){
                log.info("Job succeeded")
                return true
            }
        }

//====If timeout===============================================
        log.error("Finished all loops listening to ${origJob.name}; exiting")
        return false
    }

    private fun getDestinations(): Map<String, Destination> {
        return api.getDestinations().executeDirect() ?: emptyMap()
    }

    private fun getJob (jobId: String): PrintJob?{
        val job = api.getJob(jobId).executeDirect()
        if (job == null) {
            log.error("Job $jobId could not be found; cannot watch")
            emitter.notify(Failed(jobId, null,  Fail.IMMEDIATE, "Could not find print job on server"))
            return null
        }
        return job
    }

}



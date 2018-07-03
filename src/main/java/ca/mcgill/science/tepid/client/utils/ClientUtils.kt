package ca.mcgill.science.tepid.client.utils

import ca.mcgill.science.tepid.api.ITepid
import ca.mcgill.science.tepid.api.TepidApi
import ca.mcgill.science.tepid.api.executeDirect
import ca.mcgill.science.tepid.client.interfaces.EventObservable
import ca.mcgill.science.tepid.client.models.*
import ca.mcgill.science.tepid.models.bindings.PrintError
import ca.mcgill.science.tepid.models.data.ErrorResponse
import ca.mcgill.science.tepid.models.data.PrintJob
import ca.mcgill.science.tepid.models.data.Session
import ca.mcgill.science.tepid.utils.WithLogging
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.glassfish.jersey.jackson.JacksonFeature
import org.tukaani.xz.LZMA2Options
import org.tukaani.xz.XZOutputStream
import java.io.IOException
import java.io.InputStream
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

        val response = tepidServerXz.path("jobs").path(jobId)
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", "Token $authHeader")
                .put(Entity.entity(stream, "application/x-xz"))

        val responseMessage = response.readEntity(String::class.java)
        log.debug("Job sent: $responseMessage")
        val errorResponse = objectMapper.readValue<ErrorResponse>(responseMessage)
        if (errorResponse.status > 0 && errorResponse.error.isNotEmpty()) {
            job.fail(errorResponse.error) // we will emulate the failure change to stay consistent
            return fun():Boolean {
                val fail = Fail.fromText(job.error!!) //job.fail will set this as non-null
                emitter.notify(Failed(job.getId(), job, fail, "")) // todo
                return false
            }
        }
        return { watchJob(jobId, user, api, emitter) }
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


    fun watchJob(jobId: String, user: String): Boolean {
        log.info("Starting job watcher")
        val origJob = getJob(jobId) ?: return false

        emitter.notify(Processing(jobId, origJob))
        status = Status.SENDING

        var reportProcessing = true


        for (attempt in 1..5) {
//======Setup==================================================
            log.debug("Watch Attempt $attempt")
            if (isInterrupted()) {
                log.info("Watcher interrupted")
                return false
            }

            Thread.sleep(200) // todo change
            val job = getJob(jobId) ?: return false
            log.debug("Job snapshot $job")

//====If Failed================================================
            if (job.failed != -1L){
                val fail = Fail.fromText(job.error ?: "") //might actually null, but still failed
                emitter.notify(Failed(job.getId(), job, fail, "")) // todo: add help message text from screensaver's config
                return false
            }

//====If Processing============================================
            if (reportProcessing) {
                if (job.processed != -1L && job.destination != null) {
                    /*
                     * We will only emit processing once, which is why it is now false
                     * Note that the next statement may still run as processing is false,
                     * so if this is already printed, the emitter will notify the observers
                     */
                    reportProcessing = false
                    log.info("Processing")
                    if (job.printed == -1L) {
                        val destinations = api.getDestinations().executeDirect() ?: emptyMap() // todo log error
                        emitter.notify(Sending(jobId, job, destinations[job.destination!!]!!)) // todo notify error if null
                    }
                }
            }

//====If Processed but not printed=============================
            if (!reportProcessing && job.printed == -1L) {
                val quota = api.getQuota(user).executeDirect()
                if (quota != null) {
                    val oldQuota = quota + job.colorPages * 2 + job.pages
                    val destinations = api.getDestinations().executeDirect() ?: emptyMap() // todo log error
                    val destination = destinations[job.destination!!]!!
                    emitter.notify(Completed(jobId, job, destination, oldQuota, quota))
                }
                // todo log failure if quota is null
//====If Sucsessful============================================
                log.info("Job succeeded")
                return true
            }
        }

//====If timeout===============================================
        log.error("Finished all loops listening to ${origJob.name}; exiting")
        return false
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



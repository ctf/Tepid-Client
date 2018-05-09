package ca.mcgill.science.tepid.client.utils

import ca.mcgill.science.tepid.api.ITepid
import ca.mcgill.science.tepid.api.TepidApi
import ca.mcgill.science.tepid.api.executeDirect
import ca.mcgill.science.tepid.api.getJobChanges
import ca.mcgill.science.tepid.client.interfaces.EventObservable
import ca.mcgill.science.tepid.client.models.*
import ca.mcgill.science.tepid.models.bindings.PrintError
import ca.mcgill.science.tepid.models.data.PrintJob
import ca.mcgill.science.tepid.models.data.Session
import ca.mcgill.science.tepid.utils.WithLogging
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
     * @return watcher thread if everything went well, otherwise null
     */
    fun print(job: PrintJob, stream: InputStream, session: Session, emitter: EventObservable): Thread? {
        log.debug(job)

        log.debug("Creating new print job")

        val user = session.user.shortUser ?: return null
        val authHeader = session.authHeader
        val api = TepidApi(Config.SERVER_URL, Config.DEBUG).create {
            tokenRetriever = { authHeader }
        }

        val putJob = api.createNewJob(job).executeDirect()

        if (putJob?.ok != true) {
            log.error("Could not properly create new job")
            emitter.notify(Immediate(job._id ?: "createerror", "Failed to send job"))
            consumeStream(stream)
            return null
        }

        val jobId = putJob.id
        val watcherThread = Thread(Runnable { watchJob(jobId, user, api, emitter) }, "JobWatcher $jobId")
        watcherThread.start()
        val response = tepidServerXz.path("jobs").path(jobId)
                .request(MediaType.TEXT_PLAIN)
                .header("Authorization", "Token ${session.authHeader}")
                .put(Entity.entity(stream, "application/x-xz"))
        log.debug(response.readEntity(String::class.java))
        return watcherThread
    }

    private fun watchJob(jobId: String, user: String, api: ITepid, emitter: EventObservable) {
        log.info("Starting job watcher")
        fun isInterrupted() = Thread.currentThread().isInterrupted
        val origJob = api.getJob(jobId).executeDirect()
        if (origJob == null) {
            log.error("Job $jobId does not exist; cannot watch")
            emitter.notify(Immediate(jobId, "Could not watch print job"))
            return
        }
        emitter.notify(Processing(jobId, origJob))
        var processing = true
        for (attempt in 1..5) {
            if (isInterrupted()) {
                log.debug("Watcher interrupted")
                return
            }
            try {
                api.getJobChanges(jobId).executeDirect()
            } catch (e: Exception) {
                if (attempt == 1)
                    log.error("Malformed job change", e)
            }
            val job = api.getJob(jobId).executeDirect()
            if (job == null) {
                log.error("Job not found; token probably changed")
                emitter.notify(Failed(jobId, null, Fail.GENERIC, "Job could not be located"))
                return
            }
            log.info("Job $job")
            if (job.failed != -1L) {
                val (fail, message) = when (job.error?.toLowerCase()) {
                    PrintError.INSUFFICIENT_QUOTA -> Fail.INSUFFICIENT_QUOTA to ""
                    PrintError.COLOR_DISABLED -> Fail.COLOR_DISABLED to ""
                    else -> Fail.GENERIC to "An error occurred"
                }
                emitter.notify(Failed(jobId, job, fail, message))
                log.info("Job failed")
                return
            }
            if (processing) {
                if (job.processed != -1L && job.destination != null) {
                    processing = false
                    if (job.printed == -1L) {
                        val destinations = api.getDestinations().executeDirect() ?: emptyMap() // todo log error
                        emitter.notify(Sending(jobId, job, destinations[job.destination!!]!!)) // todo notify error if null
                    }
                }
            }
            if (!processing && job.printed == -1L) {
                val quota = api.getQuota(user).executeDirect()
                if (quota != null) {
                    val oldQuota = quota + job.colorPages * 2 + job.pages
                    val destinations = api.getDestinations().executeDirect() ?: emptyMap() // todo log error
                    val destination = destinations[job.destination!!]!!
                    emitter.notify(Completed(jobId, job, destination, oldQuota, quota))
                }
                // todo log failure if quota is null
                log.info("Job succeeded")
                return
            }
        }
        log.info("Finished listening with longpoll")
    }
}
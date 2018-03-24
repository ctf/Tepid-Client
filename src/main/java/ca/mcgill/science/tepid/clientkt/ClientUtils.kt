package ca.mcgill.science.tepid.clientkt

import ca.mcgill.science.tepid.api.ITepid
import ca.mcgill.science.tepid.api.TepidApi
import ca.mcgill.science.tepid.api.executeDirect
import ca.mcgill.science.tepid.api.getJobChanges
import ca.mcgill.science.tepid.client.Event
import ca.mcgill.science.tepid.client.EventObservable
import ca.mcgill.science.tepid.client.Fail
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
            emitter.notify { it.onErrorReceived("Failed to send job") }
            consumeStream(stream)
            return null
        }

        val id = putJob.id
        val watcherThread = Thread(Runnable { watchJob(id, user, api, emitter) }, "Job watcher for $id")
        watcherThread.start()
        val response = tepidServerXz.path("jobs").path(putJob.id)
                .request(MediaType.TEXT_PLAIN)
                .header("Authorization", "Token ${session.authHeader}")
                .put(Entity.entity(stream, "application/x-xz"))
        log.debug(response.readEntity(String::class.java))
        return watcherThread
    }

    private fun watchJob(jobId: String, user: String, api: ITepid, emitter: EventObservable) {
        log.info("JobWatcher $jobId")
        fun isInterrupted() = Thread.currentThread().isInterrupted
        val origJob = api.getJob(jobId).executeDirect()
        if (origJob == null) {
            log.error("Job $jobId does not exist; cannot watch")
            emitter.notify { it.onErrorReceived("Could not watch print job") }
            return
        }
        emitter.notify { it.onJobReceived(origJob, Event.CREATED, Fail.NONE) }
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
                emitter.notify { it.onErrorReceived("Job could not be located") }
                return
            }
            log.info("Job $job")
            if (job.failed != -1L) {
                val fail = when (job.error?.toLowerCase()) {
                    "insufficient quota" -> Fail.INSUFFICIENT_QUOTA
                    "color disabled" -> Fail.COLOR_DISABLED
                    else -> Fail.GENERIC
                }
                emitter.notify { it.onJobReceived(job, Event.FAILED, fail) }
                log.info("Job failed")
                return
            }
            if (processing) {
                if (job.processed != -1L && job.destination != null) {
                    processing = false
                    if (job.printed == -1L) {
                        emitter.notify { it.onJobReceived(job, Event.PROCESSING, Fail.NONE) }
                    }
                }
            }
            if (!processing && job.printed == -1L) {
                val quota = api.getQuota(user).executeDirect()
                if (quota != null) {
                    val oldQuota = quota + job.colorPages * 2 + job.pages
                    emitter.notify { it.onQuotaChanged(quota, oldQuota) }
                }
                log.info("Job succeeded")
                return
            }
        }
        log.info("Finished listening with longpoll")
    }
}
package ca.mcgill.science.tepid.client

import ca.mcgill.science.tepid.api.executeDirect
import ca.mcgill.science.tepid.api.fetch
import ca.mcgill.science.tepid.client.interfaces.EventObservable
import ca.mcgill.science.tepid.client.interfaces.EventObserver
import ca.mcgill.science.tepid.client.lpd.LPDServer
import ca.mcgill.science.tepid.client.models.Immediate
import ca.mcgill.science.tepid.client.models.Init
import ca.mcgill.science.tepid.client.models.SessionAuth
import ca.mcgill.science.tepid.client.printers.PrinterMgmt
import ca.mcgill.science.tepid.client.utils.*
import ca.mcgill.science.tepid.models.data.Session
import ca.mcgill.science.tepid.models.data.SessionRequest
import ca.mcgill.science.tepid.utils.WithLogging
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.*
import kotlin.system.exitProcess

class Client private constructor(observers: Array<out EventObserver>) : EventObservable {

    override val observers = mutableListOf<EventObserver>()

    override val executor: ExecutorService = EventObservable.defaultExecutorService()

    private inline val api
        get() = ClientUtils.api

    private inline val apiNoAuth
        get() = ClientUtils.apiNoAuth

    init {
        create(observers)
    }

    private fun <T> async(action: () -> T): Future<T> =
            executor.submit(action)


    private fun fail(message: String, e: Exception): Nothing {
        log.error(message, e)
        terminateImpl()
        exitProcess(-1)
    }

    private fun create(observers: Array<out EventObserver>) {
        log.info("******************************")
        log.info("    Starting Tepid Client     ")
        log.info("******************************")

        addObservers(*observers)

        val queues = apiNoAuth.getQueues().executeDirect() ?: fail("Could not get queue data")

        log.debug("Found ${queues.size} queues")

        val manager = PrinterMgmt.printerManagement

        if (Auth.hasToken) {
            api.getQuota(Auth.user).fetch { data, _ ->
                data ?: return@fetch
                initialize(Init(data))
            }
        }

        var defaultQueue: String? = null
        val queueIds: Map<String, String> = queues.mapNotNull {
            val name = it.name ?: return@mapNotNull null
            val defaultOn = it.defaultOn
            if (defaultOn != null && ClientUtils.wildcardMatch(defaultOn, ClientUtils.hostname))
                defaultQueue = name
            ClientUtils.newId() to name
        }.toMap()

        try {
            manager.bind(queueIds, defaultQueue)
        } catch (e: Exception) {
            fail("Error binding the manager", e)
        }

        try {
            LPDServer(if (Config.IS_WINDOWS) 515 else 8515).use { lpd ->
                lpd.addJobListener { job, input ->
                    job.queueName = queueIds[job.queueName]
                    log.info("Starting job for ${job.name}")
                    val session = getValidSession()
                    if (session == null) {
                        notify(Immediate("nosession", "No session found"))
                        return@addJobListener
                    }

                    log.trace("Printing job ${job.name}")

                    val watcher = ClientUtils.print(job, input, session, this)
                    if (watcher == null) {
                        log.error("An error occurred during the print process")
                        return@addJobListener
                    }
                    async(watcher)
                }
                lpd.start()
            }
        } catch (e: IOException) {
            fail("Failed to bind LPDServer", e)
        }
    }

    private fun terminateImpl() {
        log.info("Terminating")
        observers.forEach(EventObserver::unbind)
        executor.shutdownNow()
    }

    override fun terminate(): Nothing {
        terminateImpl()
        exitProcess(0)
    }

    /**
     * Attempts to get a valid session by
     * 1. Checking the existing auth content
     * 2. Attempting to get a request from an observer
     */
    private fun getValidSession(): Session? {
        if (Auth.hasToken) {
            val session = api.validateToken(Auth.user, Auth.id).executeDirect()
            if (session?.isValid() == true)
                return session
            log.warn("Could not validate old token ${Auth.token}")
            Auth.clear()
        }
        observers.forEach { obs ->
            for (count in 1..20) {
                val auth = obs.onSessionRequest(count) ?: break
                if (auth == SessionAuth.INVALID) {
                    log.info("Invalid auth; cancelled")
                    return null
                }
                val request = SessionRequest(auth.username, auth.password, true, true)
                val session = api.getSession(request).executeDirect()
                if (session != null) {
                    Auth.set(session.user.shortUser, session._id).save()
                    return session
                } else {
                    log.info("Session request was invalid")
                }
            }
        }
        log.error("No handlers were able to handle the session request")
        return null
    }

    override fun addObserver(vararg observer: EventObserver): Boolean {
        val obs = observer.firstOrNull { it.bind(this) } ?: return false
        observers.add(obs)
        return true
    }

    override fun addObservers(vararg observers: EventObserver): Boolean {
        return observers.map {
            val bound = it.bind(this)
            if (bound)
                this.observers.add(it)
            bound
        }.all { it }
    }

    override fun getObserverNames(): List<String> = observers.map(EventObserver::name)

    override val isWindows: Boolean
        get() = Config.IS_WINDOWS

    companion object : WithLogging() {
        @JvmStatic
        fun create(vararg observers: EventObserver): Client? =
                try {
                    Client(observers)
                } catch (e: ClientException) {
                    log.error("Failed to create client", e)
                    null
                }
    }
}
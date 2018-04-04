package ca.mcgill.science.tepid.clientkt

import ca.mcgill.science.tepid.api.ITepid
import ca.mcgill.science.tepid.api.TepidApi
import ca.mcgill.science.tepid.api.executeDirect
import ca.mcgill.science.tepid.api.fetch
import ca.mcgill.science.tepid.client.LPDServer
import ca.mcgill.science.tepid.clientkt.interfaces.EventObservable
import ca.mcgill.science.tepid.clientkt.interfaces.EventObserver
import ca.mcgill.science.tepid.clientkt.printers.CupsPrinterMgmt
import ca.mcgill.science.tepid.clientkt.printers.WindowsPrinterMgmt
import ca.mcgill.science.tepid.clientkt.utils.Auth
import ca.mcgill.science.tepid.clientkt.utils.ClientUtils
import ca.mcgill.science.tepid.clientkt.utils.Config
import ca.mcgill.science.tepid.models.data.Session
import ca.mcgill.science.tepid.models.data.SessionRequest
import ca.mcgill.science.tepid.utils.WithLogging
import java.io.IOException

class Client private constructor(observers: Array<out EventObserver>) : EventObservable {

    private val observers = mutableListOf<EventObserver>()

    private val api: ITepid by lazy {
        TepidApi(Config.SERVER_URL, Config.DEBUG).create {
            tokenRetriever = Auth::tokenHeader
        }
    }

    private val apiNoAuth: ITepid by lazy {
        TepidApi(Config.SERVER_URL, Config.DEBUG).create()
    }

    init {
        create(observers)
    }

    class ClientException(message: String) : RuntimeException(message)

    private fun create(observers: Array<out EventObserver>) {
        log.info("******************************")
        log.info("    Starting Tepid Client     ")
        log.info("******************************")

        addObservers(*observers)

        val queues = apiNoAuth.getQueues().executeDirect() ?: throw ClientException("Could not get queue data")

        log.debug("Found ${queues.size} queues")

        val manager = if (Config.IS_WINDOWS) WindowsPrinterMgmt() else CupsPrinterMgmt()

        if (Auth.hasToken) {
            api.getQuota(Auth.user).fetch { data, _ ->
                data ?: return@fetch
                notify { it.onQuotaChanged(data, -1) }
            }
        }

        var defaultQueue: String? = null
        val queueIds = queues.filter { it.name != null }.map {
            val defaultOn = it.defaultOn
            if (defaultOn != null && ClientUtils.wildcardMatch(defaultOn, ClientUtils.hostname))
                defaultQueue = it.name
            ClientUtils.newId() to it.name
        }.toMap()

        manager.bind(queueIds, defaultQueue)

        try {
            LPDServer(if (Config.IS_WINDOWS) 515 else 8515).use { lpd ->
                lpd.addJobListener { p, input ->

                    p.queueName = queueIds[p.queueName]
                    log.info("Starting job for ${p.name}")
                    val session = getValidSession()

                    if (session == null) {
                        notify { it.onErrorReceived("No session found") }
                        return@addJobListener
                    }

                    val watcherThread = ClientUtils.print(p, input, session, this)
                    if (watcherThread == null) {
                        log.error("An error occurred during the print process")
                    }
                }
                lpd.start()
            }
        } catch (e: IOException) {
            log.error("Failed to bind LPDServer", e)
            throw ClientException("Failed to bind LDPServer")
        }
    }

    override fun notify(action: (obs: EventObserver) -> Unit) {
        observers.forEach { action(it) }
    }

    private fun getValidSession(): Session? {
        if (Auth.hasToken) {
            val session = api.validateToken(Auth.user, Auth.token).executeDirect()
            if (session?.isValid() == true)
                return session
            log.warn("Could not validate old token")
            Auth.clear()
        }
        observers.forEach { obs ->
            for (count in 1..20) {
                val auth = obs.onSessionRequest(count) ?: break
                val request = SessionRequest(auth.username, auth.password, true, true)
                val session = api.getSession(request).executeDirect()
                if (session != null) {
                    Auth.set(session.user.shortUser, session.user._id).save()
                    return session
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
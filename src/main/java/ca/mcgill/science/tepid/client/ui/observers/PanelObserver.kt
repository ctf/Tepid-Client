package ca.mcgill.science.tepid.client.ui.observers

import ca.mcgill.science.tepid.api.ITepid
import ca.mcgill.science.tepid.api.TepidApi
import ca.mcgill.science.tepid.client.interfaces.EventObservable
import ca.mcgill.science.tepid.client.interfaces.EventObserver
import ca.mcgill.science.tepid.client.models.*
import ca.mcgill.science.tepid.client.ui.notification.Notification
import ca.mcgill.science.tepid.client.ui.text.PasswordDialog
import ca.mcgill.science.tepid.client.utils.Auth
import ca.mcgill.science.tepid.client.utils.Config
import ca.mcgill.science.tepid.common.Utils
import ca.mcgill.science.tepid.models.data.PrintJob
import ca.mcgill.science.tepid.utils.WithLogging
import dorkbox.systemTray.MenuItem
import dorkbox.systemTray.SystemTray
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap


class PanelObserver : EventObserver {

    override val name: String = "Panel"

    private val systemTray: SystemTray? by lazy {
        //        SystemTray.FORCE_GTK2 = true
        try {
            SystemTray.get()
        } catch (e: NoSuchMethodError) {
            log.error("Could not find system tray method; removing icon binding", e)
            null
        }
    }

    override fun bind(observable: EventObservable): Boolean {
        try {
            val systemTray = this.systemTray ?: return false
            val icon = File.createTempFile("tepid", ".png")
            icon.delete()
            Files.copy(Utils.getResourceAsStream(if (Config.IS_WINDOWS) "trayicon/16_loading.png" else "trayicon/32_loading.png"), icon.toPath())
            icon.deleteOnExit()
            systemTray.setImage(icon.absolutePath)
            systemTray.status = "Unauthenticated"
            systemTray.menu.add(MenuItem("Quit", {
                observable.terminate()
            }))
        } catch (e: IOException) {
            log.error("Failed to bind system tray", e)
            return false
        }
        return true
    }

    override fun unbind() {
        systemTray?.shutdown()
    }

    override fun onSessionRequest(attemptCount: Int): SessionAuth? {
        return PasswordDialog.prompt("mail.mcgill.ca").result
    }

    private val notifications: MutableMap<String, Notification> = ConcurrentHashMap()

    private val api: ITepid by lazy {
        TepidApi(Config.SERVER_URL, Config.DEBUG).create {
            tokenRetriever = Auth::tokenHeader
        }
    }

    override fun initialize(init: Init) {
        systemTray?.status = "${init.quota} Pages Left"
    }

    private val PrintJob.shortName
        get() = "\"${truncateName(28)}\""

    /**
     * Gets existing notification or a new one if invalid/not exists
     * if [remove], clears the notification from the map
     * otherwise, adds it back to the map
     */
    private fun notif(id: String, remove: Boolean = false): Notification {
        val notif = notifications[id]?.takeIf { !it.isClosed } ?: Notification()
        if (remove)
            notifications.remove(id)
        else
            notifications[id] = notif
        return notif
    }

    override fun onEvent(event: Event) {
        when (event) {
            is Processing -> {
                notif(event.id).setStatus(0x2196F3,
                        "receiving",
                        "Your job is uploading",
                        "Your print job ${event.job.shortName} is currently being received from the application. ")
            }
            is Sending -> {
                notif(event.id).setStatus(0x4D983E,
                        "sending",
                        "Sending job to printer",
                        "${event.job.shortName} has processed and is being sent to ${event.destination.name}.")
            }
            is Completed -> {
                val notification = notif(event.id, true)
                notification.setQuota(event.quotaBefore, event.quotaNow,
                        "You have ${event.quotaNow} pages left",
                        "${event.job.shortName} sent to printer ${event.destination.name}.")
                try {
                    Thread.sleep(10000)
                } catch (ignored: InterruptedException) {
                }
                notification.close()
            }
            is Failed -> {
                val notification = notif(event.id, true)
                notification.setStatus(0xB71C1C, event.error.icon, event.error.display, event.message)
                try {
                    Thread.sleep(10000)
                } catch (ignored: InterruptedException) {
                }
                notification.close()
            }
        }
//        if (!notification.isVisible)
//            notification.isVisible = true
    }

    private companion object : WithLogging()

}
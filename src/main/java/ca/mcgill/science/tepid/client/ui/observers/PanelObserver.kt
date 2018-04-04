package ca.mcgill.science.tepid.client.ui.observers

import ca.mcgill.science.tepid.client.Event
import ca.mcgill.science.tepid.client.Fail
import ca.mcgill.science.tepid.client.interfaces.EventObservable
import ca.mcgill.science.tepid.client.interfaces.EventObserver
import ca.mcgill.science.tepid.client.models.SessionAuth
import ca.mcgill.science.tepid.client.utils.Config
import ca.mcgill.science.tepid.common.Utils
import ca.mcgill.science.tepid.models.data.PrintJob
import ca.mcgill.science.tepid.utils.WithLogging
import dorkbox.systemTray.SystemTray
import java.io.File
import java.io.IOException
import java.nio.file.Files
import ca.mcgill.science.tepid.client.ui.text.PasswordDialog



class PanelObserver : EventObserver {

    override val name: String = "Panel"

    val systemTray: SystemTray? by lazy {
        SystemTray.FORCE_GTK2 = true
        SystemTray.getSystemTray()
    }

    override fun bind(observable: EventObservable): Boolean {
        val systemTray = this.systemTray ?: return false
        try {
            val icon = File.createTempFile("tepid", ".png")
            icon.delete()
            Files.copy(Utils.getResourceAsStream(if (Config.IS_WINDOWS) "trayicon/16_loading.png" else "trayicon/32_loading.png"), icon.toPath())
            icon.deleteOnExit()
            systemTray.setIcon(icon.absolutePath)
            systemTray.status = "Loading..."
            systemTray.addMenuEntry("Quit") { _, _ -> System.exit(0) }
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

    override fun onJobReceived(printJob: PrintJob, event: Event, fail: Fail) {
        log.info("Job received ${printJob.name} $event $fail")
    }

    override fun onQuotaChanged(quota: Int, oldQuota: Int) {
        systemTray?.status = "$quota Pages Left";
    }

    override fun onErrorReceived(error: String) {
        log.error("ERROR RECEIVED: $error")
    }

    private companion object : WithLogging()

}
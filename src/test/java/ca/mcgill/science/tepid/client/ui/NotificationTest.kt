package ca.mcgill.science.tepid.client.ui

import ca.mcgill.science.tepid.client.ui.notification.Notification
import ca.mcgill.science.tepid.client.ui.notification.NotificationWindow
import org.junit.Test

class NotificationTest {

    @Test
    fun test() {
        val jobName = "blah_blah_blah.docx - Microsoft Word"
        var n = Notification()
        n.setStatus(0x2196F3, "receiving", "Your job is uploading", "Your print job \"$jobName\" is currently being received from the application. ")
        n.isVisible = true
        Thread.sleep(3000)
        n.setStatus(0x4D983E, "sending", "Sending job to printer", "\"$jobName\" has processed and is being sent to 1B18-Southpole.")
        Thread.sleep(3000)
        if (n.isClosed) {
            n = Notification()
            n.isVisible = true
        }
        val credits = 19
        val creditsBefore = 71
        n.setQuota(creditsBefore, credits, "You have $credits pages left", "\"blah_blah_blah.docx - Microsoft Word\" sent to printer 1B18-Right.")
        Thread.sleep(6000)
        n.setStatus(0x2196F3, "receiving", "Your job is uploading", "Your print job isn't actually uploading ;) ")
    }

    @Test
    fun test2() {
        val jobName = "blah_blah_blah.docx - Microsoft Word"
        var n = NotificationWindow()
        n.setStatus(0x2196F3, "receiving", "Your job is uploading", "Your print job \"$jobName\" is currently being received from the application. ")
        n.isVisible = true
        Thread.sleep(3000)
        n.setStatus(0x4D983E, "sending", "Sending job to printer", "\"$jobName\" has processed and is being sent to 1B18-Southpole.")
        Thread.sleep(3000)
        if (n.closed) {
            n = NotificationWindow()
            n.isVisible = true
        }
        val credits = 19
        val creditsBefore = 71
        n.setQuota(creditsBefore, credits, "You have $credits pages left", "\"blah_blah_blah.docx - Microsoft Word\" sent to printer 1B18-Right.")
        Thread.sleep(6000)
        n.setStatus(0x2196F3, "receiving", "Your job is uploading", "Your print job isn't actually uploading ;) ")
    }

}

fun main(vararg args: String) {
    NotificationTest().test2()
}

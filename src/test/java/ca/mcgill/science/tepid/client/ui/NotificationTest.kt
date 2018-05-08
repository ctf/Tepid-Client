package ca.mcgill.science.tepid.client.ui

import ca.mcgill.science.tepid.client.ui.notification.*
import org.junit.Test
import java.awt.Color
import kotlin.test.assertEquals

class NotificationTest {

    @Test
    fun test() {
        val jobName = "blah_blah_blah.docx - Microsoft Word"
        var n = NotificationOld()
        n.setStatus(0x2196F3, "receiving", "Your job is uploading", "Your print job \"$jobName\" is currently being received from the application. ")
        n.isVisible = true
        Thread.sleep(3000)
        n.setStatus(0x4D983E, "sending", "Sending job to printer", "\"$jobName\" has processed and is being sent to 1B18-Southpole.")
        Thread.sleep(3000)
        if (n.isClosed) {
            n = NotificationOld()
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
        Notifications.notify(StateNotification("Your job is uploading", "Your print job \"$jobName\" is currently being received from the application.", 0x2196F3, NotificationIcon.RECEIVING))
        Thread.sleep(3000)
        Notifications.notify(StateNotification("Sending job to printer", "\"$jobName\" has processed and is being sent to 1B18-Southpole.", 0x4D983E, NotificationIcon.SENDING))
        Thread.sleep(3000)
        val credits = 19
        val creditsBefore = 71
        Notifications.notify(TransitionNotification("You have $credits pages left", "\"blah_blah_blah.docx - Microsoft Word\" sent to printer 1B18-Right.", creditsBefore, credits))
        Thread.sleep(6000)
        Notifications.notify(StateNotification("Your job is uploading", "Your print job isn't actually uploading ;) ", 0x2196F3, NotificationIcon.RECEIVING))
        Notifications.thread?.join()
    }

    @Test
    fun test3() {
        arrayOf(0x802290 to 0x229039).forEach { (c1, c2) ->
            assertEquals(NotificationWindow.combineColors(c1, c2),
                    NotificationWindow.combineColors(c2, c1))
        }
    }

}

fun main(vararg args: String) {
    NotificationTest().test2()
}

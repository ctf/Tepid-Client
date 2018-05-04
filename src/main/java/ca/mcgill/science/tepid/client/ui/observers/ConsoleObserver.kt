package ca.mcgill.science.tepid.client.ui.observers

import ca.mcgill.science.tepid.client.interfaces.EventObservable
import ca.mcgill.science.tepid.client.interfaces.EventObserver
import ca.mcgill.science.tepid.client.models.Event
import ca.mcgill.science.tepid.client.models.Fail
import ca.mcgill.science.tepid.client.models.SessionAuth
import ca.mcgill.science.tepid.models.data.PrintJob
import java.util.*

class ConsoleObserver(private val handleLogin: Boolean = false) : EventObserver {

    override val name: String = "Console"

    override fun bind(observable: EventObservable): Boolean {
        return true
    }

    override fun unbind() = Unit

    override fun onSessionRequest(attemptCount: Int): SessionAuth? {
        if (!handleLogin)
            return null
        val scanner = Scanner(System.`in`)
        println("Enter your username: ")
        val username = scanner.nextLine()
        println("Enter your password: ")
        val password = scanner.nextLine()
        return SessionAuth.create(username, password)
    }

    override fun onJobReceived(printJob: PrintJob, event: Event, fail: Fail) {
        println("Console: PrintJob $printJob, $event, $fail")
    }

    override fun onQuotaChanged(quota: Int, oldQuota: Int) {
        println("Console: Quota changed to $quota, old $oldQuota")
    }

    override fun onErrorReceived(error: String) {
        System.err.println("Console: Error: $error")
    }

}
package ca.mcgill.science.tepid.clientkt

import ca.mcgill.science.tepid.client.*
import ca.mcgill.science.tepid.models.data.PrintJob
import java.util.*

class ConsoleObserver(private val handleLogin: Boolean = false) : EventObserver {

    override fun bind(observable: EventObservable): Boolean {
        return true
    }

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
        println("PrintJob $printJob, $event, $fail")
    }

    override fun onQuotaChanged(quota: Int, oldQuota: Int) {
        println("Quota changed to $quota, old $oldQuota")
    }

    override fun onErrorReceived(error: String) {
        println("Error: $error")
    }

}
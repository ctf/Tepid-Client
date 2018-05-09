package ca.mcgill.science.tepid.client.ui.observers

import ca.mcgill.science.tepid.client.interfaces.EventObservable
import ca.mcgill.science.tepid.client.interfaces.EventObserver
import ca.mcgill.science.tepid.client.models.*
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

    override fun initialize(init:Init) {
        println("Initialized: quota ${init.quota}")
    }

    override fun onEvent(event: Event) {
        when (event) {
            is Processing -> println("Processing ${event.job}")
            is Sending -> println("Sending ${event.job} to ${event.destination.name}")
            is Completed -> print("Completed ${event.job} by ${event.destination.name}; quota ${event.quotaBefore} -> ${event.quotaNow}")
            is Failed -> print("Failed ${event.job}: ${event.error.display} - ${event.message}")
        }
    }

}
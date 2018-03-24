package ca.mcgill.science.tepid.client.internal

import ca.mcgill.science.tepid.client.EventObservable
import ca.mcgill.science.tepid.client.EventObserver
import ca.mcgill.science.tepid.clientkt.Config
import ca.mcgill.science.tepid.clientkt.ConsoleObserver

class TestEventObservable : EventObservable {

    private val observer = ConsoleObserver()

    override fun notify(action: (obs: EventObserver) -> Unit) {
        action(observer)
    }

    override val isWindows: Boolean
        get() = Config.IS_WINDOWS

    override fun addObserver(vararg observer: EventObserver): Boolean {
        throw RuntimeException("No-op")
    }

    override fun addObservers(vararg observers: EventObserver): Boolean {
        throw RuntimeException("No-op")
    }
}
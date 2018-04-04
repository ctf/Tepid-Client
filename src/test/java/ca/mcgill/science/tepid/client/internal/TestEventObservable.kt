package ca.mcgill.science.tepid.client.internal

import ca.mcgill.science.tepid.client.interfaces.EventObservable
import ca.mcgill.science.tepid.client.interfaces.EventObserver
import ca.mcgill.science.tepid.client.ui.console.ConsoleObserver
import ca.mcgill.science.tepid.client.utils.Config

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
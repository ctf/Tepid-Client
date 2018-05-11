package ca.mcgill.science.tepid.client.internal

import ca.mcgill.science.tepid.client.interfaces.EventObservable
import ca.mcgill.science.tepid.client.interfaces.EventObserver
import ca.mcgill.science.tepid.client.models.Event
import ca.mcgill.science.tepid.client.models.Init
import ca.mcgill.science.tepid.client.ui.observers.ConsoleObserver
import ca.mcgill.science.tepid.client.utils.Config
import java.util.concurrent.ExecutorService
import kotlin.system.exitProcess

class TestEventObservable : EventObservable {

    override val executor: ExecutorService = EventObservable.defaultExecutorService()

    override val observers: Collection<EventObserver> = listOf(ConsoleObserver())

    override val isWindows: Boolean
        get() = Config.IS_WINDOWS

    override fun addObserver(vararg observer: EventObserver): Boolean {
        throw RuntimeException("No-op")
    }

    override fun addObservers(vararg observers: EventObserver): Boolean {
        throw RuntimeException("No-op")
    }

    override fun terminate(): Nothing {
        exitProcess(0)
    }
}
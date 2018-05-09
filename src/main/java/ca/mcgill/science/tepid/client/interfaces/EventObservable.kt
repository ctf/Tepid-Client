package ca.mcgill.science.tepid.client.interfaces

import ca.mcgill.science.tepid.client.models.Event
import ca.mcgill.science.tepid.client.models.Init
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Main handler for all event related interactions
 */
interface EventObservable {

    val observers: Collection<EventObserver>

    /**
     * Executor to help delegate asynchronous methods
     */
    val executor: ExecutorService

    fun initialize(init: Init) {
        observers.forEach { it.initialize(init) }
    }

    fun notify(event: Event) {
        observers.forEach {
            executor.execute { it.onEvent(event) }
        }
    }


    /**
     * @return [true] if client is currently running on windows, [false] otherwise.
     */
    val isWindows: Boolean

    /**
     * Attempts to add an observer.
     * If multiple observers are provided, the observers will be added in sequence
     * until one of them succeeds. At most one observer will be added from this call.
     *
     * @param observer to add
     * @return [true] if any observer was added successfully, [false] otherwise.
     * Failure to add may result from all provided observers failing to bind,
     * or some other unforeseen exception
     */
    fun addObserver(vararg observer: EventObserver): Boolean

    /**
     * Attempts to add all observers provided.
     * Binding for each observer will be done concurrently, but end result will be synchronous
     *
     * @param observers to add
     * @return [true] if every observer was added successfully, [false] otherwise
     */
    fun addObservers(vararg observers: EventObserver): Boolean

    /**
     * Returns a list of the current observers by name
     */
    fun getObserverNames(): List<String> = observers.map(EventObserver::name)

    /**
     * Called to end the entire observable
     */
    fun terminate(): Nothing

    companion object {

        fun defaultExecutorService(): ExecutorService =
                ThreadPoolExecutor(5, 30, 5, TimeUnit.MINUTES,
                        ArrayBlockingQueue<Runnable>(300, true))

    }

}

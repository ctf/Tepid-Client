package ca.mcgill.science.tepid.client.interfaces

import ca.mcgill.science.tepid.client.models.*
import ca.mcgill.science.tepid.models.data.PrintJob

/**
 * Delegation interface to handle client events
 */
interface EventObserver {

    /**
     * Unique identifier
     */
    val name: String

    /**
     * Called during initialization to allow the listener to bind itself.
     * Any single time set up should be called here.
     * Each bind will be spawned in a different thread per listener, so this process can be long
     * Only when all the bindings finish will the client continue
     *
     * @param observable handler to be bound to. An observer may optionally try to bind
     * other observers through this interface, and may gather more information
     * about the runtime from here
     * @return [true] if listener has been successfully bound, [false] otherwise.
     * Note that if not properly bound, the listener will be discarded
     * and no events will be passed
     */
    fun bind(observable: EventObservable): Boolean

    /**
     * Called when the lifecycle ends, to allow for stuff to unbind
     */
    fun unbind()

    /**
     * Called when a new session is requested. The only goal is to return a provided pair
     * of username and password. All verifications will be handled externally.
     *
     *
     * Note that an observer that does not handle session requests should return null.
     * An observer that handles sessions but did not get any valid input (be it from cancellation) or an error
     * should return [SessionAuth.INVALID]
     *
     * @param attemptCount counter for number of loops called from the observable.
     * @return a session auth where applicable, [SessionAuth.INVALID] if cancelled by user,
     * or null if requests are not handled
     */
    fun onSessionRequest(attemptCount: Int): SessionAuth?

    /**
     * Called when everything is ready
     * Parameters received for initialization will also be passed here
     */
    fun initialize(init: Init)

    /**
     * Handler for any event passed on by the client
     *
     * Note that for any print job if [Processing] is emitted,
     * One of [Completed] or [Failed] is guaranteed to be called
     */
    fun onEvent(event: Event)

}

package ca.mcgill.science.tepid.client.interfaces

import ca.mcgill.science.tepid.client.models.SessionAuth
import ca.mcgill.science.tepid.client.Event
import ca.mcgill.science.tepid.client.Fail
import ca.mcgill.science.tepid.models.data.PrintJob

/**
 * Delegation interface to handle client events
 */
interface EventObserver {

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
     * Handler for any event passed on by the client.
     * Note that this will occur sequentially and synchronously, so the method should return quickly.
     * All events should be streamed into one thread, so if you do need more time to handle each request,
     * consider delegating handlers to only one additional thread with back pressuring
     * in case the input events come too quickly
     *
     * @param printJob job currently being handled
     * @param event    event describing current interaction
     * @param fail     enum describing current failure; defaults to [Fail.NONE]
     * if event is not [Event.FAILED]
     */
    fun onJobReceived(printJob: PrintJob, event: Event, fail: Fail)

    /**
     * Handler for quota change
     * Independent from other calls
     * @param quota new quota value
     * @param oldQuota old quota value; this is optional! check the value before making animations
     */
    fun onQuotaChanged(quota: Int, oldQuota: Int)

    /**
     * Generic observer for errors
     * @param error text
     */
    fun onErrorReceived(error: String)

}

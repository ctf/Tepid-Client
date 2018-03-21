package ca.mcgill.science.tepid.client;

import ca.mcgill.science.tepid.models.data.PrintJob;

/**
 * Delegation interface to handle client events
 */
public interface EventObserver {

    /**
     * Called during initialization to allow the listener to bind itself.
     * Any single time set up should be called here.
     * Each bind will be spawned in a different thread per listener, so this process can be long
     * Only when all the bindings finish will the client continue
     *
     * @param observer handler to be bound to. An observer may optionally try to bind
     *                 other observers through this interface, and may gather more information
     *                 about the runtime from here
     * @return {@link true} if listener has been successfully bound, {@link false} otherwise.
     * Note that if not properly bound, the listener will be discarded
     * and no events will be passed
     */
    boolean bind(EventObserver observer);

    /**
     * Called when a new session is requested. The only goal is to return a provided pair
     * of username and password. All verifications will be handled externally.
     * <p>
     * Note that an observer that does not handle session requests should return null.
     * An observer that handles sessions but did not get any valid input (be it from cancellation) or an error
     * should return {@link SessionAuth#INVALID}
     *
     * @param attemptCount counter for number of loops called from the observable.
     * @return a session auth where applicable, {@link SessionAuth#INVALID} if cancelled by user,
     * or null if requests are not handled
     */
    SessionAuth onSessionRequest(int attemptCount);

    /**
     * Handler for any event passed on by the client.
     * Note that this will occur sequentially and synchronously, so the method should return quickly.
     * All events should be streamed into one thread, so if you do need more time to handle each request,
     * consider delegating handlers to only one additional thread with back pressuring
     * in case the input events come too quickly
     *
     * @param printJob job currently being handled
     * @param event    event describing current interaction
     * @param fail     enum describing current failure; defaults to {@link Fail#NONE}
     *                 if event is not {@link Event#FAILED}
     */
    void onEventReceived(PrintJob printJob, Event event, Fail fail);

}

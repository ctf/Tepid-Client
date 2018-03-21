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
     * Handler for any event passed on by the client.
     * Note that this will occur sequentially and synchronously, so the method should return quickly.
     * All events should be streamed into one thread, so if you do need more time to handle each request,
     * consider delegating handlers to only one additional thread with back pressuring
     * in case the input events come too quickly
     *
     * @param printJob job currently being handled
     * @param event    event describing current interaction
     */
    void onEventReceived(PrintJob printJob, Event event);

}

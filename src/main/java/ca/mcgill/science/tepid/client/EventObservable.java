package ca.mcgill.science.tepid.client;

/**
 * Main handler for all event related interactions
 */
public interface EventObservable {

    /**
     * Attempts to add an observer.
     * If multiple observers are provided, the observers will be added in sequence
     * until one of them succeeds. At most one observer will be added from this call.
     *
     * @param observer to add
     * @return {@link true} if any observer was added successfully, {@link false} otherwise.
     * Failure to add may result from all provided observers failing to bind,
     * or some other unforeseen exception
     */
    boolean addObserver(EventObserver... observer);

    /**
     * Attempts to add all observers provided.
     * Binding for each observer will be done concurrently, but end result will be synchronous
     *
     * @param observers to add
     * @return {@link true} if every observer was added successfully, {@link false} otherwise
     */
    boolean addObservers(EventObserver... observers);

    /**
     * @return {@link true} if client is currently running on windows, {@link false} otherwise.
     */
    boolean isWindows();

}
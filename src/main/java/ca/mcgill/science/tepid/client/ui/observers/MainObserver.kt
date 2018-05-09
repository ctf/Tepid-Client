package ca.mcgill.science.tepid.client.ui.observers

import ca.mcgill.science.tepid.client.interfaces.EventObservable
import ca.mcgill.science.tepid.client.interfaces.EventObserver
import ca.mcgill.science.tepid.client.models.Event
import ca.mcgill.science.tepid.client.models.Init
import ca.mcgill.science.tepid.client.models.SessionAuth

/**
 * Passes the handler to other observers
 * Note that bind is false, so other methods should never be called
 */
object MainObserver : EventObserver {

    override val name: String = "Main Delegate"

    override fun bind(observable: EventObservable): Boolean {
        observable.addObservers(PanelObserver(), ConsoleObserver())
        return false
    }

    private fun noop(): Nothing = throw RuntimeException("Noop")

    override fun unbind() = noop()

    override fun onSessionRequest(attemptCount: Int): SessionAuth? = noop()

    override fun initialize(init: Init) = noop()

    override fun onEvent(event: Event) = noop()
}
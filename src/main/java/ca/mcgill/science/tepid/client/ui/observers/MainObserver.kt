package ca.mcgill.science.tepid.client.ui.observers

import ca.mcgill.science.tepid.client.interfaces.EventObservable
import ca.mcgill.science.tepid.client.interfaces.EventObserver
import ca.mcgill.science.tepid.client.models.Event
import ca.mcgill.science.tepid.client.models.Fail
import ca.mcgill.science.tepid.client.models.SessionAuth
import ca.mcgill.science.tepid.models.data.PrintJob

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

    override fun onJobReceived(printJob: PrintJob, event: Event, fail: Fail) = noop()

    override fun onQuotaChanged(quota: Int, oldQuota: Int) = noop()

    override fun onErrorReceived(error: String) = noop()

}
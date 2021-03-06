package ca.mcgill.science.tepid.client.printers

import ca.mcgill.science.tepid.client.utils.Config
import ca.mcgill.science.tepid.utils.WithLogging
import java.io.IOException

/**
 * Logic for managing printers for various systems
 */
interface PrinterMgmt {

    private fun fullQueueName(queue: String): String = "$queue-${Config.USER_NAME}"

    /**
     * Adds a printer of the given data
     *
     * @param queue     name
     * @param id      id
     * @param isDefault true if name matches default
     */
    fun addPrinter(queue: String, id: String, isDefault: Boolean) {
        val fullQueue = fullQueueName(queue)
        log.info("Adding printer $fullQueue, id $id")
        try {
            addPrinterImpl(fullQueue, id, isDefault)
        } catch (e: IOException) {
            log.error("Add printer error", e)
        }
    }

    /**
     * Delete a printer of the given data
     *
     * @param queue name
     * @param id  id
     */
    fun deletePrinter(queue: String, id: String) {
        val fullQueue = fullQueueName(queue)
        log.info("Deleting printer $fullQueue, id $id")
        try {
            deletePrinterImpl(fullQueue, id)
        } catch (e: Exception) {
            if (e !is NoSuchElementException)
                log.error("Delete printer error", e)
        }
    }

    /**
     * Called when client first starts.
     * Should add printers and remove them upon shutdown
     *
     * @param queueIds     map of ids
     * @param defaultQueue default queue id
     */
    fun bind(queueIds: Map<String, String>, defaultQueue: String?) {
        try {
            if (!preBind()) {
                log.error("Failed to preBind printer management")
                return
            }
        } catch (e: Exception) {
            log.error("Failed to bind", e)
            return
        }
        log.info("Finished prebind")
        // delete all printers on shutdown
        Runtime.getRuntime().addShutdownHook(Thread({
            log.info("Unmounting ${queueIds.size} printers")
            queueIds.forEach { id, queue -> deletePrinter(queue, id) }
        }, "Unmount Printers"))

        // add all printers
        queueIds.forEach { id, queue -> addPrinter(queue, id, queue == defaultQueue) }
    }

    /**
     * Handler to setup content when binding is requested
     *
     * @return `true` if successful, `false` otherwise
     */
    fun preBind(): Boolean

    /**
     * Implicit implementation of [addPrinter] without the try catch
     */
    fun addPrinterImpl(queue: String, id: String, isDefault: Boolean)

    /**
     * Implicit implementation of [deletePrinter] without the try catch
     */
    fun deletePrinterImpl(queue: String, id: String)

    /**
     * Attempts to clean up the printers
     */
    fun cleanPrinters()

    /**
     * @return filepath of tepid data
     */
    fun tepidDataPath(): String

    companion object : WithLogging() {

        /**
         * @return the appropriate [PrinterMgmt] for the current system
         */
        val printerManagement: PrinterMgmt by lazy { if (Config.IS_WINDOWS) WindowsPrinterMgmt() else CupsPrinterMgmt() }
    }

}

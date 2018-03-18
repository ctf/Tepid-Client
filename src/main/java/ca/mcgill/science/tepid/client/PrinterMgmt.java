package ca.mcgill.science.tepid.client;

import java.io.IOException;
import java.util.Map;

/**
 * Logic for managing printers for various systems
 */
public interface PrinterMgmt {

    /**
     * Adds a printer of the given data
     *
     * @param queue     name
     * @param port      port
     * @param isDefault true if name matches default
     */
    default void addPrinter(String queue, String port, boolean isDefault) {
        try {
            addPrinterImpl(queue, port, isDefault);
        } catch (IOException | InterruptedException e) {
            System.err.println("Add printer error");
            e.printStackTrace();
        }
    }

    /**
     * Delete a printer of the given data
     *
     * @param queue name
     * @param port  port
     */
    default void deletePrinter(String queue, String port) {
        try {
            deletePrinterImpl(queue, port);
        } catch (IOException | InterruptedException e) {
            System.err.println("Delete printer error " + e.getMessage());
        }
    }

    /**
     * Called when {@link Main} first starts.
     * Should add printers and remove them upon shutdown
     *
     * @param queueIds     map of ids
     * @param defaultQueue default queue id
     */
    default void bind(Map<String, String> queueIds, String defaultQueue) {
        if (!preBind()) {
            System.err.println("Failed to preBind printer management");
            return;
        }
        // add all printers
        queueIds.forEach((port, queue) -> addPrinter(queue, port, queue.equals(defaultQueue)));
        // delete all printers on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread("Unmount Printers") {
            @Override
            public void run() {
                System.out.println("Unmounting " + queueIds.size() + " printers");
                queueIds.forEach((port, queue) -> deletePrinter(queue, port));
            }
        });
    }

    /**
     * Handler to setup content when binding is requested
     *
     * @return {@code true} if successful, {@code false} otherwise
     */
    boolean preBind();

    /**
     * Implicit implementation of {@link #addPrinter(String, String, boolean)} without the try catch
     */
    void addPrinterImpl(String queue, String port, boolean isDefault) throws IOException, InterruptedException;

    /**
     * Implicit implementation of {@link #deletePrinter(String, String)} without the try catch
     */
    void deletePrinterImpl(String queue, String port) throws IOException, InterruptedException;

    /**
     * Attempts to clean up the printers
     */
    void cleanPrinters();

    /**
     * @return filepath of tepid data
     */
    String tepidDataPath();

    /**
     * @return the appropriate {@link PrinterMgmt} for the current system
     */
    static PrinterMgmt getPrinterManagement() {
        return System.getProperty("os.name").startsWith("Windows") ? new WindowsPrinterMgmt() : new CupsPrinterMgmt();
    }


}

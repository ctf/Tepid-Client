package ca.mcgill.science.tepid.client.internal

import ca.mcgill.science.tepid.client.printers.WindowsPrinterMgmt
import ca.mcgill.science.tepid.client.utils.Config
import ca.mcgill.science.tepid.utils.WithLogging
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class DllBinding {

    private companion object : WithLogging()

    @Test
    fun bind() {
        if (!Config.IS_WINDOWS) {
            log.trace("Skipping dll test")
            return
        }
        assertTrue(WindowsPrinterMgmt.loadJacob())
    }

}
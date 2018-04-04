package ca.mcgill.science.tepid.client.internal

import ca.mcgill.science.tepid.clientkt.printers.WindowsPrinterMgmt
import ca.mcgill.science.tepid.clientkt.utils.Config
import ca.mcgill.science.tepid.utils.WithLogging
import org.junit.Test
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
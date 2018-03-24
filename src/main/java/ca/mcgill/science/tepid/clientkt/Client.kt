package ca.mcgill.science.tepid.clientkt

import ca.mcgill.science.tepid.client.CupsPrinterMgmt
import ca.mcgill.science.tepid.client.WindowsPrinterMgmt
import ca.mcgill.science.tepid.clientkt.util.Config
import ca.mcgill.science.tepid.utils.WithLogging

class Client {

    fun create() {
        log.info("******************************")
        log.info("    Starting Tepid Client     ")
        log.info("******************************")

        val manager = if (Config.IS_WINDOWS) WindowsPrinterMgmt() else CupsPrinterMgmt()
    }

    private companion object : WithLogging()

}
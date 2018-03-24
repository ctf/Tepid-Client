package ca.mcgill.science.tepid.clientkt.util

import ca.mcgill.science.tepid.utils.PropUtils
import ca.mcgill.science.tepid.utils.WithLogging


/**
 * Created by Allan Wang on 24/03/2018.
 *
 * The following are default keys used for testing
 * They are pulled from priv.properties under the root project folder
 * If no file is found, default values will be supplied (usually empty strings)
 */
object Config : WithLogging() {

    private const val TEST_URL = "http://testpid.science.mcgill.ca"
    private const val PRODUCTION_URL = "https://tepid.science.mcgill.ca"

    val BASE_URL: String
    val SERVER_URL: String
    val DEBUG: Boolean
    val IS_WINDOWS = System.getProperty("os.name").startsWith("Windows")

    init {
        log.info("**********************************")
        log.info("*       Setting up Configs       *")
        log.info("**********************************")
        val props = PropUtils.loadProps("priv.properties")

        if (props == null)
            log.warn("No configs found")

        BASE_URL = when (props?.getProperty("URL")) {
            "tepid" -> PRODUCTION_URL
            "testpid" -> TEST_URL
            else -> TEST_URL
        }
        SERVER_URL = BASE_URL + if (BASE_URL == PRODUCTION_URL) ":8080/tepid/" else ":8443/tepid/"
        DEBUG = BASE_URL == PRODUCTION_URL
        log.info("Server url $SERVER_URL")
    }

}
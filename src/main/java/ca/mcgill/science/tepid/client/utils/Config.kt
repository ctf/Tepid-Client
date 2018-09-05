package ca.mcgill.science.tepid.client.utils

import ca.mcgill.science.tepid.utils.*
import org.apache.logging.log4j.Level

/**
 * Created by Allan Wang on 24/03/2018.
 *
 * The following are default keys used for testing
 * They are pulled from priv.properties under the root project folder
 * If no file is found, default values will be supplied (usually empty strings)
 */
object Config : WithLogging() {

    val SERVER_URL: String
    val DEBUG: Boolean
    val IS_WINDOWS = System.getProperty("os.name").startsWith("Windows")
    val PROP_PATH: String
    val USER_NAME: String

    val ACCOUNT_DOMAIN : String

    val LINK_TOS : String


    init {

        log.info("**********************************")
        log.info("*       Setting up Configs       *")
        log.info("**********************************")

        SERVER_URL = PropsURL.SERVER_URL_PRODUCTION ?: ""
        DEBUG = PropsURL.SERVER_URL_TESTING != SERVER_URL

        if (DEBUG) LogUtils.setLoggingLevel(log, Level.TRACE)

        val propDir = if (IS_WINDOWS) System.getenv("appdata")
        else System.getProperty("user.home")

        PROP_PATH = "$propDir/.tepid"
        log.info("Server url $SERVER_URL")

        ACCOUNT_DOMAIN = PropsLDAP.ACCOUNT_DOMAIN ?: ""

        LINK_TOS = PropsAbout.LINK_TOS ?: ""

        USER_NAME = when {
            Auth.hasToken -> Auth.user
            IS_WINDOWS -> System.getProperty("user.name")
            else -> System.getProperty("user.name")
        // todo check
        }

    }

}
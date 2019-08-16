package ca.mcgill.science.tepid.client.utils

import ca.mcgill.science.tepid.utils.*
import org.apache.logging.log4j.Level

object Config : WithLogging() {

    val SERVER_URL: String
    val DEBUG: Boolean
    val IS_WINDOWS = System.getProperty("os.name").startsWith("Windows")
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
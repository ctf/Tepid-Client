package ca.mcgill.science.tepid.client.utils

import ca.mcgill.science.tepid.utils.*
import org.apache.logging.log4j.Level

object Config : WithLogging() {

    val SERVER_URL: String
    val WEB_URL: String
    val DEBUG: Boolean
    val IS_WINDOWS = System.getProperty("os.name").startsWith("Windows")
    val USER_NAME: String

    val ACCOUNT_DOMAIN : String

    val LINK_TOS : String


    init {

        log.info("**********************************")
        log.info("*       Setting up Configs       *")
        log.info("**********************************")

        DEBUG = PropsURL.TESTING?.toBoolean() ?: true

        SERVER_URL = (if (DEBUG) PropsURL.SERVER_URL_TESTING else PropsURL.SERVER_URL_PRODUCTION) ?: ""
        WEB_URL = (if (DEBUG) PropsURL.WEB_URL_TESTING  else PropsURL.WEB_URL_PRODUCTION) ?: ""

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
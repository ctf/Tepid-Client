package ca.mcgill.science.tepid.client.utils

import ca.mcgill.science.tepid.models.bindings.TEPID_URL_PRODUCTION
import ca.mcgill.science.tepid.models.bindings.tepidUrl
import ca.mcgill.science.tepid.utils.PropUtils
import ca.mcgill.science.tepid.utils.WithLogging
import java.util.*


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

    init {
        log.info("**********************************")
        log.info("*       Setting up Configs       *")
        log.info("**********************************")
        val props = PropUtils.loadProps("priv.properties") ?: Properties()

        fun get(key: String, default: String?) = props.getProperty(key, default)
        fun get(key: String) = get(key, "")

        SERVER_URL = tepidUrl(get("URL"))
        DEBUG = SERVER_URL == TEPID_URL_PRODUCTION

        val propDir = if (IS_WINDOWS) System.getenv("appdata")
        else System.getProperty("user.home")

        PROP_PATH = "$propDir/.tepid"
        log.info("Server url $SERVER_URL")

        USER_NAME = when {
            Auth.hasToken -> Auth.user
            IS_WINDOWS -> System.getProperty("user.name")
            else -> System.getProperty("user.name")
            // todo check
        }
    }

}
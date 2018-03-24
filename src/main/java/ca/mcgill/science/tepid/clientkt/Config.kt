package ca.mcgill.science.tepid.clientkt

import ca.mcgill.science.tepid.utils.PropUtils
import ca.mcgill.science.tepid.utils.WithLogging
import java.io.File
import java.util.*


/**
 * Created by Allan Wang on 24/03/2018.
 *
 * The following are default keys used for testing
 * They are pulled from priv.properties under the root project folder
 * If no file is found, default values will be supplied (usually empty strings)
 */
object Config : WithLogging() {

    private const val TEST_URL = "https://testpid.science.mcgill.ca"
    private const val PRODUCTION_URL = "https://tepid.science.mcgill.ca"

    val BASE_URL: String
    val SERVER_URL: String
    val DEBUG: Boolean
    val IS_WINDOWS = System.getProperty("os.name").startsWith("Windows")
    val PROP_PATH: String

    /*
    * Optional arguments used to run unit tests for ldap
    */
    val TEST_USER: String
    val TEST_PASSWORD: String
    var TEST_FILE: String? = null

    init {
        log.info("**********************************")
        log.info("*       Setting up Configs       *")
        log.info("**********************************")
        val props = PropUtils.loadProps("priv.properties") ?: Properties()

        fun get(key: String, default: String?) = props.getProperty(key, default)
        fun get(key: String) = get(key, "")

        val baseUrl = get("URL", TEST_URL) ?: TEST_URL
        BASE_URL = when (baseUrl) {
            "tepid" -> PRODUCTION_URL
            "testpid" -> TEST_URL
            else -> baseUrl
        }
        SERVER_URL = BASE_URL + if (BASE_URL == PRODUCTION_URL) ":8080/tepid/" else ":8443/tepid/"
        DEBUG = BASE_URL == PRODUCTION_URL
        val propDir = if (IS_WINDOWS) System.getenv("appdata") else System.getProperty("user.home")
        PROP_PATH = "$propDir/.tepid"
        log.info("Server url ${SERVER_URL}")

        TEST_USER = get("TEST_USER")
        TEST_PASSWORD = get("TEST_PASSWORD")
        val testFile = get("TEST_FILE")
        if (testFile.isNotBlank()) {
            val file = File("test_files/$testFile")
            if (!file.isFile)
                log.error("${file.absolutePath} is not a valid test file")
            else
                TEST_FILE = file.absolutePath
        }
    }

}
package ca.mcgill.science.tepid.client.internal

import ca.mcgill.science.tepid.api.ITepid
import ca.mcgill.science.tepid.api.TepidApi
import ca.mcgill.science.tepid.clientkt.Config
import ca.mcgill.science.tepid.models.data.Session
import ca.mcgill.science.tepid.models.data.SessionRequest
import org.apache.logging.log4j.LogManager
import retrofit2.Call
import kotlin.test.fail

private val log = LogManager.getLogger("ApiRetriever")

val apiUnauth: ITepid by lazy {
    log.info("Initialized test apiUnauth")
    TepidApi(Config.SERVER_URL, true).create()
}

fun hasTestUser() = Config.TEST_USER.isNotBlank() && Config.TEST_PASSWORD.isNotBlank()

val session: Session by lazy {
    val session = apiUnauth.getSession(
            SessionRequest(Config.TEST_USER, Config.TEST_PASSWORD, false, false)).get()
    log.info("Initialized test api $session")
    session
}

/**
 * Equivalent to [Call.execute] with validation checks and a guaranteed data return if successful
 */
internal fun <T> Call<T>.get(): T {
    try {
        val response = execute()
        if (response.isSuccessful) {
            val body = response.body() ?: fail("Successful test response has a null body")
            println("Successful test response received:\n$body")
            return body
        }
        fail("Unsuccessful ${response.code()} test response:\n${response.errorBody()?.string()}")
    } catch (exception: Exception) {
        fail("An exception occurred: ${exception.message}")
    }
}
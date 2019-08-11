package ca.mcgill.science.tepid.client.internal

import ca.mcgill.science.tepid.test.TestUtilsDelegate
import ca.mcgill.science.tepid.utils.PropsLDAPTestUser
import ca.mcgill.science.tepid.utils.PropsURL
import java.io.File

object TestUtils : TestUtilsDelegate(
        PropsLDAPTestUser.TEST_USER,
        PropsLDAPTestUser.TEST_PASSWORD,
        PropsURL.SERVER_URL_PRODUCTION!!,
        PropsURL.TESTING!!.toBoolean()
) {
    val testFile = "pdf-test.pdf"
}
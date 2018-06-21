package ca.mcgill.science.tepid.client.models

import org.junit.Test
import kotlin.test.assertNotNull

class CurrentUserTest {

    @Test
    fun getCurrentUser() {
        val user = CurrentUser.currentUser
        assertNotNull(user)
        println(user)
    }
}
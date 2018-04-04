package ca.mcgill.science.tepid.client.models

/**
 * User, password pair
 */
data class SessionAuth internal constructor(val username: String, val password: String) {

    companion object {
        @JvmField
        val INVALID = SessionAuth("", "")

        @JvmStatic
        fun create(username: String?, password: String?): SessionAuth {
            return if (username == null || password == null) INVALID else SessionAuth(username, password)
        }
    }
}

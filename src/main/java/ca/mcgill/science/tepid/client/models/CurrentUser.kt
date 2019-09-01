package ca.mcgill.science.tepid.client.models

import ca.mcgill.science.tepid.utils.WithLogging
import java.io.InputStream

data class CurrentUser(val user: String, val domain: String?) {

    companion object : WithLogging() {

        val currentUser: CurrentUser? by lazy {
            try {
                return@lazy getCurrentUser(arrayOf("whoami", "/upn"), "@")
            } catch (e: Exception) {
                try {
                    return@lazy getCurrentUser(arrayOf("whoami"), "\\\\")
                } catch (e1: Exception) {
                    log.error("Failed to get user with upn", e)
                    log.error("Failed to get user with just whoami", e1)
                }
                return@lazy null
            }
        }

        private fun getCurrentUser(command: Array<String>, splitter: String): CurrentUser =
                ProcessBuilder(*command).start().inputStream.getCurrentUser(splitter)

        private fun InputStream.getCurrentUser(splitter: String): CurrentUser = use {
            val out = ByteArray(1024)
            val bytes = read(out)
            val userString = String(out, 0, bytes).trim()
            if (!userString.contains(splitter))
                return@use CurrentUser(userString, null)
            val (user, domain) = userString.split(splitter)
            return@use CurrentUser(user, domain)
        }
    }

}

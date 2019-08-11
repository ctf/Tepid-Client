package ca.mcgill.science.tepid.client.utils

import ca.mcgill.science.tepid.models.data.Session
import ca.mcgill.science.tepid.utils.FilePropLoader
import ca.mcgill.science.tepid.utils.WithLogging

val propDir = if (Config.IS_WINDOWS) System.getenv("appdata") else System.getProperty("user.home")
val PROP_PATH = "$propDir/.tepid"

object Auth : WithLogging() {

    var props = FilePropLoader(PROP_PATH)

    var tokenUser: String?
        get() = props.get("TOKEN_USER")
        set(value) = props.set("TOKEN_USER", value)

    var tokenId: String?
        get() = props.get("TOKEN_ID")
        set(value) = props.set("TOKEN_ID", value)

    val token: String
        get() = "$tokenUser:$tokenId"

    val tokenHeader: String
        get() = Session.encodeToHeader(tokenUser, tokenId)

    val user: String
        get() = tokenUser ?: ""

    val id: String
        get() = tokenId ?: ""

    val hasToken: Boolean
        get() = tokenUser?.isNotBlank() == true && tokenId?.isNotBlank() == true

    fun set(user: String?, id: String?): Auth {
        tokenUser = user
        tokenId = id
        return this
    }

    fun save() {
        log.trace("Saving")
        props.set("token", token)
        props.saveProps()
    }

    fun clear() {
        log.trace("Clearing")
        tokenUser = null
        tokenId = null
    }

}
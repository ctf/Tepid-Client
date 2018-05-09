package ca.mcgill.science.tepid.client.utils

import ca.mcgill.science.tepid.models.data.Session
import ca.mcgill.science.tepid.utils.PropUtils
import ca.mcgill.science.tepid.utils.WithLogging
import java.util.*

object Auth : WithLogging() {

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

    private var tokenUser: String? = null
    private var tokenId: String? = null
    private val props: Properties = PropUtils.loadProps(Config.PROP_PATH) ?: Properties()

    init {
        val token = props.getProperty("token")?.split(":")
        if (token?.size == 2) {
            tokenUser = token[0]
            tokenId = token[1]
        }
    }

    fun set(user: String?, id: String?): Auth {
        tokenUser = user
        tokenId = id
        return this
    }

    fun save() {
        log.trace("Saving")
        props.setProperty("token", token)
        PropUtils.saveProps(props, Config.PROP_PATH, "Tepid")
    }

    fun clear() {
        log.trace("Clearing")
        tokenUser = null
        tokenId = null
    }

}
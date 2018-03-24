package ca.mcgill.science.tepid.clientkt.util

import ca.mcgill.science.tepid.models.data.Session
import java.io.File
import java.io.FileInputStream
import java.util.*

object Token {

    val token: String
        get() = "$tokenUser:$tokenId"

    val tokenHeader: String
        get() = Session.encodeToHeader(tokenUser, tokenId)

    private var tokenUser: String? = null
    private var tokenId: String? = null
    private val props = Properties()

    init {
        val tepidFile = File(if (Config.IS_WINDOWS) "" else "")
        props.load(FileInputStream(tepidFile))
        val token = props.getProperty("token").split(":")
        if (token.size == 2) {
            tokenUser = token[0]
            tokenId = token[1]
        }
    }

    fun set(user: String, id: String): Token {
        tokenUser = user
        tokenId = id
        return this
    }

    fun save() {
        props.setProperty("token", token)
    }

    fun clear() {
        tokenUser = null
        tokenId = null
    }

}
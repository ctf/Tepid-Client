package ca.mcgill.science.tepid.clientkt.util

import ca.mcgill.science.tepid.api.ITepid
import ca.mcgill.science.tepid.api.TepidApi

val API: ITepid by lazy {
    TepidApi(Config.SERVER_URL, Config.DEBUG).create {
        tokenRetriever = Token::token
    }
}
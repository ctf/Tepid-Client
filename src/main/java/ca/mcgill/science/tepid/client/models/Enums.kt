package ca.mcgill.science.tepid.client.models

import ca.mcgill.science.tepid.models.enums.PrintError
import ca.mcgill.science.tepid.utils.PropsPrinting

// todo: add help message text from screensaver's config
enum class Fail(val error: PrintError, val icon: String, val body: String = "") {
    GENERIC(PrintError.GENERIC, "fail"),
    IMMEDIATE(PrintError.IMMEDIATE, "fail"),
    INSUFFICIENT_QUOTA(PrintError.INSUFFICIENT_QUOTA, "noquota"),
    INVALID_DESTINATION(PrintError.INVALID_DESTINATION, "fail"),
    COLOR_DISABLED(PrintError.COLOR_DISABLED, "color"),
    TOO_MANY_PAGES(PrintError.TOO_MANY_PAGES, "fail", "The maximum is ${PropsPrinting.MAX_PAGES_PER_JOB}"),
    NO_INTERNET(PrintError.NO_INTERNET, "fail"),
    ;

    val display: String
        get() = this.error.display

    companion object {

        /**
         * Only accepts a non-null string, since a null string is indicative of no failure.
         * This prevents accidental invocation on actually good jobs
         * Note that one will need to use one of:
         *  - a not-null assertion operator ```Fail.fromText(job.error!!)```, common in the case of invoking job.fail(error:String), which only takes non-null strings
         *  - a short circuit ```Fail.fromText(job.error ?: "")```, for when one cannot guarantee a non-null error field with a non-null failed field
         */
        private val map = Fail.values().associateBy { T -> T.display }

        fun fromText(printErrorText: String): Fail = map[printErrorText] ?: Fail.GENERIC
    }
}

enum class Status {
    SENDING, PROCESSING, PRINTED, FAILED
}
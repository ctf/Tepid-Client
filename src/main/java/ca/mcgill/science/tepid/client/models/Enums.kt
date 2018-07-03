package ca.mcgill.science.tepid.client.models

import ca.mcgill.science.tepid.models.bindings.PrintError
import ca.mcgill.science.tepid.models.data.PrintJob

enum class Fail(val display: String, val icon: String) {
    GENERIC("Generic Failure", "fail"),
    IMMEDIATE("Print Failure", "fail"),
    INSUFFICIENT_QUOTA("Insufficient Quota", "noquota"),
    INVALID_DESTINATION("Invalid Destination", "fail"), // todo update
    COLOR_DISABLED("Color disabled", "color"),
    NO_INTERNET("No Internet Detected", "fail") // todo update
    ;

    companion object {

        /**
         * Only accepts a non-null string, since a null string is indicative of no failure.
         * This prevents accidental invocation on actually good jobs
         * Note that one will need to use one of:
         *  - a not-null assertion operator ```Fail.fromText(job.error!!)```, common in the case of invoking job.fail(error:String), which only takes non-null strings
         *  - a short circuit ```Fail.fromText(job.error ?: "")```, for when one cannot guarantee a non-null error field with a non-null failed field
         */
        fun fromText(printErrorText: String) : Fail{
            return when (printErrorText.toLowerCase()) {
                PrintError.INSUFFICIENT_QUOTA -> Fail.INSUFFICIENT_QUOTA
                PrintError.COLOR_DISABLED -> Fail.COLOR_DISABLED
                PrintError.INVALID_DESTINATION -> Fail.INVALID_DESTINATION
                else -> Fail.GENERIC
            }
        }
    }
}

enum class Status {
    SENDING, PROCESSING, PRINTED, FAILED
}
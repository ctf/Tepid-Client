package ca.mcgill.science.tepid.client.models

enum class Fail(val display: String, val icon: String) {
    GENERIC("Generic Failure", "fail"),
    IMMEDIATE("Print Failure", "fail"),
    INSUFFICIENT_QUOTA("Insufficient Quota", "noquota"),
    COLOR_DISABLED("Color disabled", "color"),
    NO_INTERNET("No Internet Detected", "fail") // todo update
}

enum class Status {
    SENDING, PROCESSING, PRINTED, FAILED
}
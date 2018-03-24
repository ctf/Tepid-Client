package ca.mcgill.science.tepid.client

enum class Event {
    CREATED, PROCESSING, SENDING, COMPLETED, FAILED
}

enum class Fail {
    NONE, GENERIC, INSUFFICIENT_QUOTA, COLOR_DISABLED, NO_INTERNET
}

enum class Status {
    SENDING, PROCESSING, PRINTED, FAILED
}
package ca.mcgill.science.tepid.client.models

enum class Event {
    CREATED, PROCESSING, SENDING, COMPLETED, FAILED
}

enum class Fail {
    NONE, GENERIC, INSUFFICIENT_QUOTA, COLOR_DISABLED, INVALID_DESTINATION, NO_INTERNET
}

enum class Status {
    SENDING, PROCESSING, PRINTED, FAILED
}
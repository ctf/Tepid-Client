package ca.mcgill.science.tepid.client.models

import ca.mcgill.science.tepid.models.data.Destination
import ca.mcgill.science.tepid.models.data.PrintJob

sealed class Event

class Processing(val id: String, val job: PrintJob) : Event()

class Sending(val id: String, val job: PrintJob, val destination: Destination) : Event()

class Completed(val id: String, val job: PrintJob, val destination: Destination, val quotaBefore: Int, val quotaNow: Int) : Event()

open class Failed(val id: String, val job: PrintJob?, val error: Fail, val message: String) : Event()

class Immediate(id: String, message: String) : Failed(id, null, Fail.IMMEDIATE, message)

class Init(val quota: Int)
package ca.mcgill.science.tepid.client

import ca.mcgill.science.tepid.client.interfaces.EventObservable
import ca.mcgill.science.tepid.client.internal.TestEventObservable
import ca.mcgill.science.tepid.client.internal.TestUtils
import ca.mcgill.science.tepid.client.utils.ClientUtils
import ca.mcgill.science.tepid.models.data.PrintJob
import ca.mcgill.science.tepid.utils.WithLogging
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.File
import java.io.FileInputStream


class JobTest {

    companion object : WithLogging() {
        init {
            Assumptions.assumeTrue(TestUtils.hasTestUser, "Testing job test")
        }

        val emitter: EventObservable by lazy { TestEventObservable() }
    }

    @Test
    fun test() {
        val job = PrintJob(name = TestUtils.testUser,
                queueId = "q0",
                originalHost = "Unit Test")
        val watchThread = ClientUtils.print(job,
                FileInputStream(File(
                        this::class.java.classLoader.getResource(TestUtils.testFile).file
                )),
                TestUtils.testSession ?: fail("Invalid session"),
                emitter) ?: fail("Failed to bind watch thread")
        println(System.currentTimeMillis())
        watchThread()
        println(System.currentTimeMillis())
    }

}
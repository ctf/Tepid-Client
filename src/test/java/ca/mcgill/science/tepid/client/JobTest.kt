package ca.mcgill.science.tepid.client

import ca.mcgill.science.tepid.client.internal.TestEventObservable
import ca.mcgill.science.tepid.client.internal.TestUtils
import ca.mcgill.science.tepid.client.utils.ClientUtils
import ca.mcgill.science.tepid.client.interfaces.EventObservable
import ca.mcgill.science.tepid.models.data.PrintJob
import ca.mcgill.science.tepid.models.enums.Room
import ca.mcgill.science.tepid.utils.WithLogging
import org.junit.Assume
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import kotlin.test.fail

class JobTest {

    companion object :WithLogging(){
        init {
            Assume.assumeTrue("Testing job test", TestUtils.hasTestUser && TestUtils.testFile != null)
        }

        val emitter: EventObservable by lazy { TestEventObservable() }
    }

    @Test
    fun test() {
        val job = PrintJob(name = TestUtils.testUser,
                queueName = Room._1B17.toString(),
                originalHost = "Unit Test")
        val watchThread = ClientUtils.print(job,
                FileInputStream(File(TestUtils.testFile)),
                TestUtils.testSession ?: fail("Invalid session"),
                emitter) ?: fail("Failed to bind watch thread")
        println(System.currentTimeMillis())
        watchThread.join()
        println(System.currentTimeMillis())
    }

}
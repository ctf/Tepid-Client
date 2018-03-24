package ca.mcgill.science.tepid.client

import ca.mcgill.science.tepid.client.internal.TestEventObservable
import ca.mcgill.science.tepid.client.internal.hasTestUser
import ca.mcgill.science.tepid.client.internal.session
import ca.mcgill.science.tepid.clientkt.ClientUtils
import ca.mcgill.science.tepid.clientkt.Config
import ca.mcgill.science.tepid.models.data.PrintJob
import ca.mcgill.science.tepid.models.enums.Room
import org.junit.Assume
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import kotlin.test.fail

class JobTest {

    companion object {
        init {
            Assume.assumeTrue("Testing job test", hasTestUser() && Config.TEST_FILE != null)
        }

        val emitter: EventObservable by lazy { TestEventObservable() }
    }

    @Test
    fun test() {
        val job = PrintJob(name = Config.TEST_USER,
                queueName = Room._1B17.toString(),
                originalHost = "Unit Test")
        val watchThread = ClientUtils.print(job,
                FileInputStream(File(Config.TEST_FILE)),
                session, emitter) ?: fail("Failed to bind watch thread")
        watchThread.join()
    }

}
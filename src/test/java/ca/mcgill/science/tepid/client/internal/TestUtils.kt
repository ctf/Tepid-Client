package ca.mcgill.science.tepid.client.internal

import ca.mcgill.science.tepid.test.TestUtilsDelegate
import java.io.File

object TestUtils : TestUtilsDelegate() {
    val testFile: String? by lazy {
        val testFile = get("TEST_FILE")
        if (testFile?.isNotBlank() == true) {
            val file = File("test_files/$testFile")
            if (!file.isFile)
                log.error("${file.absolutePath} is not a valid test file")
            else
                return@lazy file.absolutePath
        }
        null
    }
}
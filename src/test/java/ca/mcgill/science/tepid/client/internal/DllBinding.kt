package ca.mcgill.science.tepid.client.internal

import org.junit.Test
import java.io.File
import kotlin.test.fail

class DllBinding {

    @Test
    fun bind() {
        val bit = System.getProperty("sun.arch.data.model").toInt()

        when (bit) {
            32 -> System.load("jacob-1.18-M2-x86.dll")
            64 -> System.load("jacob-1.18-M2-x64.dll")
            else -> fail("No dll found for $bit bit system")
        }
    }

}
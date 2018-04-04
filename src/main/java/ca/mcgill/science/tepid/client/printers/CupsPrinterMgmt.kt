package ca.mcgill.science.tepid.client.printers

import ca.mcgill.science.tepid.client.utils.Config
import ca.mcgill.science.tepid.common.Utils
import ca.mcgill.science.tepid.utils.WithLogging
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class CupsPrinterMgmt : PrinterMgmt {

    private companion object : WithLogging()

    private val user = Config.USER_NAME

    override fun preBind(): Boolean = true // Purposely blank

    override fun tepidDataPath(): String = System.getProperty("user.home") + "/.tepid"

    override fun addPrinterImpl(queue: String, id: String, isDefault: Boolean) {
        val tmpPpd = File.createTempFile("tepid", ".ppd")
        try {
            Files.copy(Utils.getResourceAsStream("XeroxWorkCentre7556.ppd"), tmpPpd.toPath(), StandardCopyOption.REPLACE_EXISTING)
            val pb = ProcessBuilder("sudo", "lpadmin", "-p", queue + "-" + user, "-E", "-v", "lpd://localhost:8515/" + id, "-P", tmpPpd.absolutePath)
            pb.inheritIO()
            val p = pb.start()
            p.waitFor()
        } finally {
            tmpPpd.delete()
        }
    }

    override fun deletePrinterImpl(queue: String, id: String) {
        val pb = ProcessBuilder("sudo", "lpadmin", "-x", queue + "-" + user)
        pb.inheritIO()
        val p = pb.start()
        p.waitFor()
    }

    override fun cleanPrinters() = Unit

}

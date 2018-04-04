package ca.mcgill.science.tepid.clientkt.printers

import ca.mcgill.science.tepid.clientkt.utils.Config
import ca.mcgill.science.tepid.utils.WithLogging
import com.jacob.activeX.ActiveXComponent
import com.jacob.com.ComFailException
import com.jacob.com.Dispatch
import com.jacob.com.EnumVariant
import com.jacob.com.Variant
import java.io.File
import javax.swing.UIManager

class WindowsPrinterMgmt : PrinterMgmt {

    override fun preBind(): Boolean = try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        true
    } catch (e: Exception) {
        log.error("Failed to bind", e)
        false
    }

    override fun tepidDataPath(): String = System.getenv("appdata") + "/.tepid"

    private fun getDispatch(param: String): Dispatch =
            wmi.invoke("Get", param).toDispatch()

    private fun execDispatch(param: String): EnumVariant =
            EnumVariant(wmi.invoke("ExecQuery", param).toDispatch())

    private fun Dispatch.spawn(vararg data: Pair<String, Any>): Dispatch {
        val port = Dispatch.invoke(this, "SpawnInstance_", Dispatch.Method, emptyArray(), IntArray(0)).toDispatch()
        data.forEach { (n, v) -> Dispatch.put(port, n, v) }
        Dispatch.call(port, "Put_")
        return port
    }

    private fun Dispatch.delete() {
        wmi.invoke("Delete", Dispatch.call(this, "Path_"))
    }

    override fun addPrinterImpl(queue: String, id: String, isDefault: Boolean) {
        wmi.getPropertyAsComponent("Security_")
                .getPropertyAsComponent("Privileges")
                .invoke("AddAsString", Variant("SeLoadDriverPrivilege"), Variant(true))

        getDispatch("Win32_TCPIPPrinterPort").spawn(
                "Name" to id,
                "Protocol" to 2,
                "Queue" to id,
                "HostAddress" to "127.0.0.1"
        )

        getDispatch("Win32_Printer").spawn(
                "DriverName" to "Xerox Global Print Driver PS",
                "PortName" to id,
                "DeviceID" to queue,
                "Direct" to true,
                "Network" to true,
                "Shared" to false,
                "Comment" to "TEPID"
        )

        if (isDefault) {
            val printer = EnumVariant(wmi.invoke("ExecQuery", "Select * from Win32_Printer Where Name = '$queue'")
                    .toDispatch()).nextElement().toDispatch()
            Dispatch.call(printer, "SetDefaultPrinter")
        }
    }

    override fun deletePrinterImpl(queue: String, id: String) {
        val printer = execDispatch("Select * from Win32_Printer Where Name = '$queue'").nextElement().toDispatch()
        wmi.invoke("Delete", Dispatch.call(printer, "Path_"))

        //delete id
        val printerPort = EnumVariant(wmi.invoke("ExecQuery", "Select * from Win32_TCPIPPrinterPort Where Name = '$id'").toDispatch()).nextElement().toDispatch()
        wmi.invoke("Delete", Dispatch.call(printerPort, "Path_"))
    }

    override fun cleanPrinters() {
        val printers = execDispatch("Select Name,PortName,Direct from Win32_Printer")
        for (variant in printers) {
            val printer = variant.toDispatch()
            val direct = Dispatch.get(printer, "Direct").boolean
            if (direct) {
                val name = Dispatch.get(printer, "Name").string
                val portId = Dispatch.get(printer, "PortName").string
                val port = execDispatch("Select * from Win32_TCPIPPrinterPort Where Name = '$portId'").nextElement().toDispatch()
                var printerDeleted = false
                var portDeleted = false
                try {
                    printer.delete()
                    printerDeleted = true
                } catch (ignored: ComFailException) {
                }
                try {
                    port.delete()
                    portDeleted = true
                } catch (ignored: ComFailException) {
                }
                val msg = "$name ($printerDeleted) - $portId ($portDeleted)"
                if (printerDeleted && portDeleted) log.info(msg) else log.error(msg)
            }
        }
    }

    companion object : WithLogging() {

        init {
            if (loadJacob()) {
                log.info("Loaded jacob")
            }
        }

        fun loadJacob(): Boolean {
            if (!Config.IS_WINDOWS)
                return true
            val libs = File("files/libs")
            if (!libs.isDirectory) {
                return false
            }
            System.setProperty("java.library.path", libs.absolutePath)
            //set sys_paths to null
            try {
                val sysPathsField = ClassLoader::class.java.getDeclaredField("sys_paths")
                sysPathsField.isAccessible = true
                sysPathsField.set(null, null)
            } catch (e: Exception) {
                return false
            }

            println("Getting ActiveXComponent")
            val bit = Integer.parseInt(System.getProperty("sun.arch.data.model"))
            println(String.format("%d bit system", bit))
            when (bit) {
                32 -> System.loadLibrary("jacob-1.18-M2-x86")
                64 -> System.loadLibrary("jacob-1.18-M2-x64")
                else -> return false
            }
            return true
        }

        private val wmi = ActiveXComponent("winmgmts:\\\\localhost\\root\\CIMV2")

        init {
            log.info("ActiveXComponent retrieved")
        }
    }
}

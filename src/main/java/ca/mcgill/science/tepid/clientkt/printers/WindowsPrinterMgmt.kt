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

    private fun Dispatch.spawn(vararg data: Pair<String, Any>) {
        val port = Dispatch.invoke(this, "SpawnInstance_", Dispatch.Method, arrayOfNulls(0), IntArray(0)).toDispatch()
        data.forEach { (n, v) -> Dispatch.put(port, n, v) }
    }

    override fun addPrinterImpl(queue: String, id: String, isDefault: Boolean) {
        wmi.getPropertyAsComponent("Security_")
                .getPropertyAsComponent("Privileges")
                .invoke("AddAsString", Variant("SeLoadDriverPrivilege"), Variant(true))
        val win32TcpIpPrinterPort = wmi.invoke("Get", "Win32_TCPIPPrinterPort").toDispatch()
        val port = Dispatch.invoke(win32TcpIpPrinterPort, "SpawnInstance_", Dispatch.Method, arrayOfNulls(0), IntArray(0)).toDispatch()
        Dispatch.put(port, "Name", id)
        Dispatch.put(port, "Protocol", 2)
        Dispatch.put(port, "Queue", id)
        Dispatch.put(port, "HostAddress", "127.0.0.1")
        Dispatch.call(port, "Put_")
        val win32Printer = wmi.invoke("Get", "Win32_Printer").toDispatch()
        var printer = Dispatch.invoke(win32Printer, "SpawnInstance_", Dispatch.Method, arrayOfNulls(0), IntArray(0)).toDispatch()

        val driver = "Xerox Global Print Driver PS"
        Dispatch.put(printer, "DriverName", driver)
        Dispatch.put(printer, "PortName", id)
        Dispatch.put(printer, "DeviceID", queue)
        Dispatch.put(printer, "Direct", true)
        Dispatch.put(printer, "Network", true)
        Dispatch.put(printer, "Shared", false)
        Dispatch.put(printer, "Comment", "TEPID")
        Dispatch.call(printer, "Put_")
        if (isDefault) {
            printer = EnumVariant(wmi.invoke("ExecQuery", "Select * from Win32_Printer Where Name = '$queue'").toDispatch()).nextElement().toDispatch()
            Dispatch.call(printer, "SetDefaultPrinter")
        }
    }

    override fun deletePrinterImpl(queue: String, id: String) {
        val printer = EnumVariant(wmi.invoke("ExecQuery", "Select * from Win32_Printer Where Name = '$queue'").toDispatch()).nextElement().toDispatch()
        wmi.invoke("Delete", Dispatch.call(printer, "Path_"))

        //delete id
        val printerPort = EnumVariant(wmi.invoke("ExecQuery", "Select * from Win32_TCPIPPrinterPort Where Name = '$id'").toDispatch()).nextElement().toDispatch()
        wmi.invoke("Delete", Dispatch.call(printerPort, "Path_"))
    }

    override fun cleanPrinters() {
        val printers = EnumVariant(wmi.invoke("ExecQuery", "Select Name,PortName,Direct from Win32_Printer").toDispatch())
        var printer = printers.nextElement().toDispatch()
        while (printers.hasMoreElements()) {
            val direct = Dispatch.get(printer, "Direct").boolean
            if (direct) {
                val name = Dispatch.get(printer, "Name").string
                val portId = Dispatch.get(printer, "PortName").string
                val port = EnumVariant(wmi.invoke("ExecQuery", "Select * from Win32_TCPIPPrinterPort Where Name = '$portId'").toDispatch()).nextElement().toDispatch()
                var printerDeleted = false
                var portDeleted = false
                try {
                    wmi.invoke("Delete", Dispatch.call(printer, "Path_"))
                    printerDeleted = true
                } catch (ignored: ComFailException) {
                }

                try {
                    wmi.invoke("Delete", Dispatch.call(port, "Path_"))
                    portDeleted = true
                } catch (ignored: ComFailException) {
                }

                (if (printerDeleted && portDeleted) System.out else System.err).println("$name ($printerDeleted) - $portId ($portDeleted)")
            }
            printer = printers.nextElement().toDispatch()
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

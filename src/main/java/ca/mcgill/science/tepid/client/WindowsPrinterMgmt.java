package ca.mcgill.science.tepid.client;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComFailException;
import com.jacob.com.Dispatch;
import com.jacob.com.EnumVariant;
import com.jacob.com.Variant;

import javax.swing.*;
import java.io.IOException;

public class WindowsPrinterMgmt implements PrinterMgmt {

    private String user = System.getProperty("user.name");

    static {
        System.out.println("Getting ActiveXComponent");
    }

    private static ActiveXComponent wmi = new ActiveXComponent("winmgmts:\\\\localhost\\root\\CIMV2");

    static {
        System.out.println("ActiveXComponent retrieved");
    }

    @Override
    public boolean preBind() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            return true;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public String tepidDataPath() {
        return System.getenv("appdata") + "/.tepid";
    }

    public void addPrinterImpl(String queue, String id, boolean isDefault) throws IOException, InterruptedException {
        queue = queue + "-" + user;
        wmi.getPropertyAsComponent("Security_").getPropertyAsComponent("Privileges").invoke("AddAsString", new Variant("SeLoadDriverPrivilege"), new Variant(true));
        Dispatch win32TcpIpPrinterPort = wmi.invoke("Get", "Win32_TCPIPPrinterPort").toDispatch();
        Dispatch port = Dispatch.invoke(win32TcpIpPrinterPort, "SpawnInstance_", Dispatch.Method, new Object[0], new int[0]).toDispatch();
        Dispatch.put(port, "Name", id);
        Dispatch.put(port, "Protocol", 2);
        Dispatch.put(port, "Queue", id);
        Dispatch.put(port, "HostAddress", "127.0.0.1");
        Dispatch.call(port, "Put_");
        Dispatch win32Printer = wmi.invoke("Get", "Win32_Printer").toDispatch();
        Dispatch printer = Dispatch.invoke(win32Printer, "SpawnInstance_", Dispatch.Method, new Object[0], new int[0]).toDispatch();

        String driver = "Xerox Global Print Driver PS";
        Dispatch.put(printer, "DriverName", driver);
        Dispatch.put(printer, "PortName", id);
        Dispatch.put(printer, "DeviceID", queue);
        Dispatch.put(printer, "Direct", true);
        Dispatch.put(printer, "Network", true);
        Dispatch.put(printer, "Shared", false);
        Dispatch.put(printer, "Comment", "TEPID");
        Dispatch.call(printer, "Put_");
        if (isDefault) {
            printer = new EnumVariant(wmi.invoke("ExecQuery", "Select * from Win32_Printer Where Name = '" + queue + "'").toDispatch()).nextElement().toDispatch();
            Dispatch.call(printer, "SetDefaultPrinter");
        }

    }

    @Override
    public void deletePrinterImpl(String queue, String port) throws IOException, InterruptedException {
        queue = queue + "-" + user;
        Dispatch printer = new EnumVariant(wmi.invoke("ExecQuery", "Select * from Win32_Printer Where Name = '" + queue + "'").toDispatch()).nextElement().toDispatch();
        wmi.invoke("Delete", Dispatch.call(printer, "Path_"));

        //delete port
        Dispatch printerPort = new EnumVariant(wmi.invoke("ExecQuery", "Select * from Win32_TCPIPPrinterPort Where Name = '" + port + "'").toDispatch()).nextElement().toDispatch();
        wmi.invoke("Delete", Dispatch.call(printerPort, "Path_"));
    }

    @Override
    public void cleanPrinters() {
        EnumVariant printers = new EnumVariant(wmi.invoke("ExecQuery", "Select Name,PortName,Direct from Win32_Printer").toDispatch());
        for (Dispatch printer = printers.nextElement().toDispatch(); printers.hasMoreElements(); printer = printers.nextElement().toDispatch()) {
            boolean direct = Dispatch.get(printer, "Direct").getBoolean();
            if (direct) {
                String name = Dispatch.get(printer, "Name").getString(),
                        portId = Dispatch.get(printer, "PortName").getString();
                Dispatch port = new EnumVariant(wmi.invoke("ExecQuery", "Select * from Win32_TCPIPPrinterPort Where Name = '" + portId + "'").toDispatch()).nextElement().toDispatch();
                boolean printerDeleted = false, portDeleted = false;
                try {
                    wmi.invoke("Delete", Dispatch.call(printer, "Path_"));
                    printerDeleted = true;
                } catch (ComFailException ignored) {
                }
                try {
                    wmi.invoke("Delete", Dispatch.call(port, "Path_"));
                    portDeleted = true;
                } catch (ComFailException ignored) {
                }
                (printerDeleted && portDeleted ? System.out : System.err).println(name + " (" + printerDeleted + ") - " + portId + " (" + portDeleted + ")");
            }
        }
    }
}

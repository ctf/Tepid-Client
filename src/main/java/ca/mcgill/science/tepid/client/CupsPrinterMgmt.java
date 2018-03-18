package ca.mcgill.science.tepid.client;

import ca.mcgill.science.tepid.common.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class CupsPrinterMgmt implements PrinterMgmt {

    private String user = null;

    @Override
    public boolean preBind() {
        return true; // Purposely blank
    }

    @Override
    public String tepidDataPath() {
        return System.getProperty("user.home") + "/.tepid";
    }

    @Override
    public void addPrinterImpl(String queue, String port, boolean isDefault) throws IOException, InterruptedException {
        if (user == null)
            user = Main.tokenUser == null || Main.tokenUser.isEmpty() ? System.getProperty("user.name") : Main.tokenUser;
        File tmpPpd = File.createTempFile("tepid", ".ppd");
        Files.copy(Utils.getResourceAsStream("XeroxWorkCentre7556.ppd"), tmpPpd.toPath(), StandardCopyOption.REPLACE_EXISTING);
        ProcessBuilder pb = new ProcessBuilder("sudo", "lpadmin", "-p", queue + "-" + user, "-E", "-v", "lpd://localhost:8515/" + port, "-P", tmpPpd.getAbsolutePath());
        pb.inheritIO();
        Process p = pb.start();
        p.waitFor();
        tmpPpd.delete();
    }

    @Override
    public void deletePrinterImpl(String queue, String port) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("sudo", "lpadmin", "-x", queue + "-" + user);
        pb.inheritIO();
        Process p = pb.start();
        p.waitFor();
    }

    @Override
    public void cleanPrinters() {
        // Purposely empty
    }

}

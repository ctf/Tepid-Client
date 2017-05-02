package ca.mcgill.science.tepid.client;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class CupsPrinterMgmt {
	
	private static String user = System.getProperty("user.name");
	
	public static void addPrinter(String queueName, String id) {
		try {
			File tmpPpd = File.createTempFile("tepid", ".ppd");
			Files.copy(CupsPrinterMgmt.class.getResourceAsStream("XeroxWorkCentre7556.ppd"), tmpPpd.toPath(), StandardCopyOption.REPLACE_EXISTING);
			ProcessBuilder pb = new ProcessBuilder(new String[]{"lpadmin", "-p",queueName+"-"+user, "-E", "-v","lpd://localhost:8515/"+id, "-P", tmpPpd.getAbsolutePath()});
			pb.inheritIO();
			Process p = pb.start();
			p.waitFor();
			tmpPpd.delete();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static void deletePrinter(String queueName) {
		try {
			ProcessBuilder pb = new ProcessBuilder(new String[]{"lpadmin", "-x", queueName + "-" + user});
			pb.inheritIO();
			Process p = pb.start();
			p.waitFor();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
}

package ca.mcgill.science.tepid.client;

public class NotificationTest {

	public static void main(String[] args) throws InterruptedException {
		Notification n = new Notification();
		n.setStatus(0x2196F3, "receiving", "Your job is uploading", "Your print job \"blah_blah_blah.docx - Microsoft Word\" is currently being received from the application. ");
		n.setVisible(true);
		Thread.sleep(4000);
		n.setStatus(0x4D983E, "sending", "Sending job to printer", "\"blah_blah_blah.docx - Microsoft Word\" has processed and is being sent to 1B18-Right. ");
		Notification n2 = new Notification();
		n2.setStatus(0x2196F3, "receiving", "Your job is uploading", "Your print job \"blah_blah_blah.docx - Microsoft Word\" is currently being received from the application. ");
		n2.setVisible(true);
		Thread.sleep(4000);
		n.setStatus(0xB71C1C, "noquota", "Insufficient balance", "We cannot print \"blah_blah_blah.docx - Microsoft Word\" (122 pages) because you only have 17 pages left this term.");
		Thread.sleep(4000);
		n.setStatus(0xB71C1C, "fail", "Failed to print", "Your job \"blah_blah_blah.docx - Microsoft Word\" failed during processing, please contact CTF.");
		Thread.sleep(4000);
		n.setStatus(0xB71C1C, "color", "Cannot print colour job", "\"blah_blah_blah.docx - Microsoft Word\" will not print because colour printing is not enabled for your account.");
		Thread.sleep(4000);
		if (n.isClosed()) {
			n = new Notification();
			n.setVisible(true);
		}
		final int credits = 998, creditsBefore = 1200;
		n.setQuota(creditsBefore, credits, "You have "+credits+" pages left", "\"blah_blah_blah.docx - Microsoft Word\" sent to printer 1B18-Right.");
	}

}

package ca.mcgill.science.tepid.client;

import ca.mcgill.science.tepid.common.PrintJob;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

public class LPDClientHandler extends Thread {

	public static final byte[] ack = {0x0};
	private final Socket socket;
	private final LPDServer parent;

	public LPDClientHandler(Socket socket, LPDServer parent) {
		super("LPD Client Handler " + socket.getRemoteSocketAddress());
		this.socket = socket;
		this.parent = parent;
	}

	@Override
	public void run() {
		try {
			OutputStream out = socket.getOutputStream();
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintJob printJob = new PrintJob();
			String line;
			while ((line = in.readLine()) != null) {
				int cmd = line.charAt(0);
				String params = line.substring(1);
				if(0x1 == cmd) {
					System.out.println("1 Print Job");
				} else if(0x2 == cmd) {
					System.out.println("2 Receive Job");
					System.out.println("Queue " + params);
					printJob.setQueueName(params);
					//from here on we process receive job subcommands
					out.write(ack);
					while ((line = in.readLine()) != null) {
						int subcmd = line.charAt(0);
						String subparams = line.substring(1);
						if (subcmd == 0x1) {
							System.out.println("1 Abort");
						} else if (subcmd == 0x2) {
							System.out.println("2 Receive Control File");
							int bytes = 0;
							String name = "";
							for (int i = 0; i < subparams.length(); i++) {
								if (subparams.charAt(i) < '0' || subparams.charAt(i) > '9') {
									name = subparams.substring(i + 1);
									break;
								}
								bytes *= 10;
								bytes += subparams.charAt(i) - '0';
							}
							System.out.printf("%s - %d bytes%n", name, bytes);
							out.write(ack);
							char[] control = new char[bytes + 1]; //+ 1 is to account for null byte sent by lpd to indicate end of control
							in.read(control);
							for (String ctrl : new String(control).split("\n")) {
								char ctrlCmd = ctrl.charAt(0);
								String ctrlVal = ctrl.substring(1).trim();
								switch (ctrlCmd) {
								case 'H':
									printJob.setOriginalHost(ctrlVal);
									break;
								case 'P':
									printJob.setUserIdentification(ctrlVal);
									break;
								case 'J':
									printJob.setName(ctrlVal);
									break;
								}
							}
							out.write(ack);
						} else if (subcmd == 0x3) {
							System.out.println("3 Receive Job Data");
//							sendNotification("Print job received", "Your job has started printing", "timer");
							int bytes = 0;
							String name = "";
							for (int i = 0; i < subparams.length(); i++) {
								if (subparams.charAt(i) < '0' || subparams.charAt(i) > '9') {
									name = subparams.substring(i + 1);
									break;
								}
								bytes *= 10;
								bytes += subparams.charAt(i) - '0';
							}
							System.out.println(subparams);
							System.out.printf("%s - %d bytes%n", name, bytes);
							out.write(ack);
							parent.newJob(printJob, socket.getInputStream(), bytes);
							out.write(ack);
							socket.close();
						}
					}
				} else if(0x3 == cmd) {
					System.out.println("3 Report Queue State Short");
				} else if(0x4 == cmd) {
					System.out.println("4 Report Queue State Long");
				} else if(0x5 == cmd) {
					System.out.println("5 Remove Print Job");
				} else {
					throw new RuntimeException("LPD Command not recognized");
				}
			}
		} catch (IOException e) {
			if (!e.getMessage().equals("Socket closed")) e.printStackTrace();
		}
	}

}

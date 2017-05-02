package ca.mcgill.science.tepid.client;

import ca.mcgill.science.tepid.common.PrintJob;
import javafx.util.Pair;

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

    private Pair<Integer, String> getJobData(String params) {
        int bytes = 0;
        String name = "";
        for (int i = 0; i < params.length(); i++) {
            if (params.charAt(i) < '0' || params.charAt(i) > '9') {
                name = params.substring(i + 1);
                break;
            }
            bytes *= 10;
            bytes += params.charAt(i) - '0';
        }
        return new Pair<>(bytes, name);
    }

    @Override
    public void run() {
        try {
            System.out.println("Begin running LPDClientHandler");
            OutputStream out = socket.getOutputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintJob printJob = new PrintJob();
            String line;
            while ((line = in.readLine()) != null) {
                String params = line.substring(1);
                switch (line.charAt(0)) { // cmd
                    case 0x1:
                        System.out.println("1 Print Job");
                        break;
                    case 0x2:
                        System.out.println("2 Receive Job");
                        System.out.println("Queue " + params);
                        printJob.setQueueName(params);
                        //from here on we process receive job subcommands
                        out.write(ack);
                        while ((line = in.readLine()) != null) {
                            String subparams = line.substring(1);
                            switch (line.charAt(0)) { // subcmd
                                case 0x1:
                                    System.out.println("1 Abort");
                                    break;
                                case 0x2: {
                                    System.out.println("2 Receive Control File");
                                    Pair<Integer, String> data = getJobData(subparams);
                                    int bytes = data.getKey();
                                    String name = data.getValue();
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
                                    break;
                                }
                                case 0x3: {
                                    System.out.println("3 Receive Job Data");
//							sendNotification("Print job received", "Your job has started printing", "timer");
                                    Pair<Integer, String> data = getJobData(subparams);
                                    int bytes = data.getKey();
                                    String name = data.getValue();
                                    System.out.println(subparams);
                                    System.out.printf("%s - %d bytes%n", name, bytes);
                                    out.write(ack);
                                    parent.newJob(printJob, socket.getInputStream(), bytes);
                                    out.write(ack);
                                    socket.close();
                                    break;
                                }
                            }
                        }
                        break;
                    case 0x3:
                        System.out.println("3 Report Queue State Short");
                        break;
                    case 0x4:
                        System.out.println("4 Report Queue State Long");
                        break;
                    case 0x5:
                        System.out.println("5 Remove Print Job");
                        break;
                    default:
                        throw new RuntimeException("LPD Command not recognized");
                }
            }
        } catch (IOException e) {
            if (!e.getMessage().equals("Socket closed")) e.printStackTrace();
        }
    }

}

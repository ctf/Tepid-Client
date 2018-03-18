package ca.mcgill.science.tepid.client;

import ca.mcgill.science.tepid.Api;
import ca.mcgill.science.tepid.client.notifications.Notification;
import ca.mcgill.science.tepid.models.data.Destination;
import ca.mcgill.science.tepid.models.data.PrintJob;
import com.fasterxml.jackson.databind.JsonNode;
import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

public class JobWatcher extends Thread {

    private final String id, auth;
    private final WebTarget tepidServer =
            ClientBuilder.newBuilder().register(JacksonFeature.class).build().target(Config.serverUrl());
    private Status status = Status.PROCESSING;
    private Destination destination;

    public JobWatcher(String id, String auth) {
        super("Job Watcher for " + id);
        this.id = id;
        this.auth = auth;
        this.setDaemon(true);
    }

    @Override
    public void run() {
        PrintJob j = Api.fetch(iTepid -> iTepid.getJob(id));
        //  PrintJob j = tepidServer.path("jobs/job").path(id).request(MediaType.APPLICATION_JSON).header("Authorization", auth).get(PrintJob.class);
        Map<String, Destination> destinations = tepidServer.path("destinations").request(MediaType.APPLICATION_JSON).header("Authorization", auth).get().readEntity(new GenericType<HashMap<String, Destination>>() {
        });
        Notification n = new Notification();
        Main.setTrayPrinting(true);
        n.setStatus(0x2196F3, "receiving", "Your job is uploading", "Your print job \"" + j.truncateName(28) + "\" is currently being received from the application. ");
        n.setVisible(true);
        while (!Thread.currentThread().isInterrupted()) {
            // todo get job changes since last event, not since "now"
            JsonNode change = Api.fetch(iTepid -> iTepid.getJobChanges(id));
            System.out.println(change);
            if (change != null && change.get("results").has(0)) {
                j = Api.fetch(iTepid -> iTepid.getJob(id));
                //j = tepidServer.path("jobs/job").path(id).request(MediaType.APPLICATION_JSON).header("Authorization", auth).get(PrintJob.class);
                if (j.getFailed() != -1) {
                    Main.setTrayPrinting(false);
                    // todo add generic error if actual error is null
                    if (j.getError() != null) {
                        if (j.getError().equalsIgnoreCase("insufficient quota")) {
                            this.status = Status.NO_QUOTA;
                            PrintJob finalJ = j;
                            int credits = Api.fetch(iTepid -> iTepid.getQuota(finalJ.getUserIdentification()));
                            // int credits = tepidServer.path("users").path(j.getUserIdentification()).path("quota").request(MediaType.APPLICATION_JSON).header("Authorization", auth).get(Integer.class);
                            if (n.isClosed()) {
                                n = new Notification();
                                n.setVisible(true);
                            }
                            n.setStatus(0xB71C1C, "noquota", "Insufficient balance", "We cannot print \"" + j.truncateName(28) + "\" (" + j.getPages() + " pages) because you only have " + credits + " pages left this term.");
                        } else if (j.getError().equalsIgnoreCase("color disabled")) {
                            this.status = Status.FAIL_COLOR;
                            if (n.isClosed()) {
                                n = new Notification();
                                n.setVisible(true);
                            }
                            n.setStatus(0xB71C1C, "color", "Cannot print colour job", "\"" + j.truncateName(28) + "\" will not print because colour printing is not enabled for your account.");
                        } else {
                            this.status = Status.FAIL;
                            if (n.isClosed()) {
                                n = new Notification();
                                n.setVisible(true);
                            }
                            n.setStatus(0xB71C1C, "fail", "Failed to print", "Your job \"" + j.truncateName(28) + "\" failed during processing, please contact CTF.");
                        }
                    }
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException ignored) {
                    }
                    n.close();
                    break;
                }
                if (status == Status.PROCESSING) {
                    if (j.getProcessed() != -1 && j.getDestination() != null) {
                        this.status = Status.SENDING;
                        this.destination = destinations.get(j.getDestination());
                        if (n.isClosed()) {
                            n = new Notification();
                            n.setVisible(true);
                        }
                        n.setStatus(0x4D983E, "sending", "Sending job to printer", "\"" + j.truncateName(28) + "\" has processed and is being sent to " + destination.getName() + ".");
                    }
                }
                if (status == Status.SENDING) {
                    if (j.getPrinted() != -1) {
                        Main.setTrayPrinting(false);
                        this.status = Status.PRINTED;
                        //Note that the 2 here is correct as each colour page has already been counted once in j.getPages()
                        PrintJob finalJ1 = j;
                        int credits = Api.fetch(iTepid -> iTepid.getQuota(finalJ1.getUserIdentification()));
                        //int credits = tepidServer.path("users").path(j.getUserIdentification()).path("quota").request(MediaType.APPLICATION_JSON).header("Authorization", auth).get(Integer.class),
                        int creditsBefore = credits + j.getColorPages() * 2 + j.getPages();
                        if (n.isClosed()) {
                            n = new Notification();
                            n.setVisible(true);
                        }
                        n.setQuota(creditsBefore, credits, "You have " + credits + " pages left", "\"" + j.truncateName(28) + "\" sent to printer " + destination.getName() + ".");
                        Main.setTrayQuota(credits);
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException ignored) {
                        }
                        n.close();
                        break;
                    }
                }
            }
        }
    }

    private enum Status {
        PROCESSING, SENDING, PRINTED, NO_QUOTA, FAIL, FAIL_COLOR
    }

}

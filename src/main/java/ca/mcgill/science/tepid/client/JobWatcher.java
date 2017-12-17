package ca.mcgill.science.tepid.client;

import ca.mcgill.science.tepid.client.notifications.Notification;
import ca.mcgill.science.tepid.common.Destination;
import ca.mcgill.science.tepid.common.PrintJob;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

public class JobWatcher extends Thread {

    private final String id, auth;
    private final WebTarget tepidServer = ClientBuilder.newBuilder().register(JacksonFeature.class).build().target(Main.serverUrl);
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
        PrintJob j = tepidServer.path("jobs/job").path(id).request(MediaType.APPLICATION_JSON).header("Authorization", auth).get(PrintJob.class);
        Map<String, Destination> destinations = tepidServer.path("destinations").request(MediaType.APPLICATION_JSON).header("Authorization", auth).get().readEntity(new GenericType<HashMap<String, Destination>>() {
        });
        Notification n = new Notification();
        n.setStatus(0x2196F3, "receiving", "Your job is uploading", "Your print job \"" + j.truncateName(28) + "\" is currently being received from the application. ");
        n.setVisible(true);
        while (!Thread.currentThread().isInterrupted()) {
            JsonNode change = tepidServer.path("jobs/job").path(id).path("_changes").queryParam("feed", "longpoll").queryParam("since", "now")
                    .request(MediaType.APPLICATION_JSON).header("Authorization", auth).get(ObjectNode.class);
            if (change.get("results").has(0)) {
                j = tepidServer.path("jobs/job").path(id).request(MediaType.APPLICATION_JSON).header("Authorization", auth).get(PrintJob.class);
                if (j.getFailed() != null) {
                    if (j.getError().equalsIgnoreCase("insufficient quota")) {
                        this.status = Status.NO_QUOTA;
                        int credits = tepidServer.path("users").path(j.getUserIdentification()).path("quota").request(MediaType.APPLICATION_JSON).header("Authorization", auth).get(Integer.class);
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
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException ignored) {
                    }
                    n.close();
                    break;
                }
                if (status == Status.PROCESSING) {
                    if (j.getProcessed() != null && j.getDestination() != null) {
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
                    if (j.getPrinted() != null) {
                        this.status = Status.PRINTED;
                        int credits = tepidServer.path("users").path(j.getUserIdentification()).path("quota").request(MediaType.APPLICATION_JSON).header("Authorization", auth).get(Integer.class),
                                creditsBefore = credits + j.getColorPages() * 2 + j.getPages();
                        if (n.isClosed()) {
                            n = new Notification();
                            n.setVisible(true);
                        }
                        n.setQuota(creditsBefore, credits, "You have " + credits + " pages left", "\"" + j.truncateName(28) + "\" sent to printer " + destination.getName() + ".");
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

package ca.mcgill.science.tepid.client;

import ca.mcgill.science.tepid.Api;
import ca.mcgill.science.tepid.api.ITepid;
import ca.mcgill.science.tepid.client.PasswordDialog.Result;
import ca.mcgill.science.tepid.common.Utils;
import ca.mcgill.science.tepid.models.data.PrintQueue;
import ca.mcgill.science.tepid.models.data.PutResponse;
import ca.mcgill.science.tepid.models.data.Session;
import ca.mcgill.science.tepid.models.data.SessionRequest;
import dorkbox.systemTray.SystemTray;
import in.waffl.q.PromiseRejectionException;
import org.glassfish.jersey.internal.util.Base64;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.WriterInterceptor;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class Main {


    private final static WebTarget tepidServerXz = ClientBuilder.newBuilder().register(JacksonFeature.class).register((WriterInterceptor) ctx -> {
        final OutputStream outputStream = ctx.getOutputStream();
        ctx.setOutputStream(new XZOutputStream(outputStream, new LZMA2Options()));
        ctx.proceed();
    }).build().target(Config.serverUrl());
    public static String tokenHeader = "", tokenUser = "", token = "";
    private final static Properties persist = new Properties();
    private static final Map<String, String> queueIds = new ConcurrentHashMap<>();


    static {
        SystemTray.FORCE_GTK2 = true;
    }

    private static void updateToken(String user, String id) {
        tokenUser = user != null ? user : "";
        token = user + ":" + id;
        tokenHeader = Session.Companion.encodeToHeader(user, id);
    }

    private static void clearToken() {
        tokenHeader = "";
        tokenUser = "";
        token = "";
    }

    private static SystemTray systemTray = SystemTray.getSystemTray();
    private static File trayIcon, trayIconPrinting;

    public static void main(String[] args) {
        System.out.println("***************************************\n*        Starting Tepid Client        *\n***************************************");
        final PrinterMgmt manager = PrinterMgmt.getPrinterManagement();
        System.out.println(String.format(Locale.CANADA, "Launching %s", manager.getClass().getSimpleName()));
        System.out.println("Server url: " + Config.serverUrl());
        if (args.length > 0) {
            if (args[0].equals("--cleanup")) {
                manager.cleanPrinters();
            } else {
                System.err.println("Only valid flag is --cleanup");
                System.exit(1);
            }
            System.exit(0);
        }
        if (systemTray == null) {
            throw new RuntimeException("Unable to load SystemTray!");
        }

        try {
            File icon = File.createTempFile("tepid", ".png");
            icon.delete(); //maybe delete this line? seems weird to be deleting immediately after creation
            if (System.getProperty("os.name").startsWith("Windows")) {
                Files.copy(Utils.getResourceAsStream("trayicon/16_loading.png"), icon.toPath());
//				Files.copy(Utils.getResourceAsStream("trayicon/32_loading.png"), icon.toPath());
            } else {
                Files.copy(Utils.getResourceAsStream("trayicon/32_loading.png"), icon.toPath());
            }
            icon.deleteOnExit();
            systemTray.setIcon(icon.getAbsolutePath());
            systemTray.setStatus("Loading...");
            systemTray.addMenuEntry("Quit", (systemTray12, menuEntry) -> System.exit(0));
        } catch (IOException e) {
            e.printStackTrace();
        }
        //load persist from file
        try {
            File data = new File(manager.tepidDataPath());
            if (data.exists()) persist.load(new FileInputStream(data));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (persist.getProperty("token") != null) {
            String[] parts = persist.getProperty("token").split(":");
            if (parts.length > 1) {
                String un = parts[0], sessionId = parts[1];
                if (validateToken(un, sessionId)) {
                    updateToken(un, sessionId);
                } else {
                    System.err.println("Saved session token could not be validated");
                }
            }
        }
        Integer quota = null;
        try {
            if (!tokenHeader.isEmpty()) {
                quota = Api.fetch(iTepid -> iTepid.getQuota(tokenUser));
            }
        } catch (Exception ignored) {
        }

        List<PrintQueue> queues = Api.fetch(ITepid::getQueues);

        String defaultQueue = null;
        for (PrintQueue q : queues) {
            queueIds.put(Utils.newId(), q.getName());
            if (q.getDefaultOn() != null && Utils.wildcardMatch(q.getDefaultOn(), Utils.getHostname())) {
                defaultQueue = q.getName();
            }
        }
        manager.bind(queueIds, defaultQueue);

        systemTray.removeMenuEntry("Quit");
        try {
            trayIcon = File.createTempFile("tepid", ".png");
            trayIconPrinting = File.createTempFile("tepid", ".png");
            trayIcon.delete();
            trayIconPrinting.delete();
            if (System.getProperty("os.name").startsWith("Windows")) {
                Files.copy(Utils.getResourceAsStream("trayicon/16.png"), trayIcon.toPath());
                Files.copy(Utils.getResourceAsStream("trayicon/16_printing.png"), trayIconPrinting.toPath());
//				Files.copy(Utils.getResourceAsStream("trayicon/32.png"), icon.toPath());
            } else {
                Files.copy(Utils.getResourceAsStream("trayicon/32.png"), trayIcon.toPath());
                Files.copy(Utils.getResourceAsStream("trayicon/32_printing.png"), trayIconPrinting.toPath());
            }
            trayIcon.deleteOnExit();
            systemTray.setIcon(trayIcon.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (quota != null) setTrayQuota(quota);
        else systemTray.setStatus("Welcome to CTF");
        System.out.println("Welcome to CTF");
        systemTray.addMenuEntry("My Account", (systemTray1, menuEntry) -> {
            if (Desktop.isDesktopSupported()) {
                try {
                    String accountUrl = Config.baseUrl() + "/account?token=" + Base64.encodeAsString(token);
                    URI uri = new URI(accountUrl);
                    System.out.println(uri);
                    Desktop.getDesktop().browse(uri);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        systemTray.addMenuEntry("Quit", (systemTray12, menuEntry) -> System.exit(0));

        try (LPDServer lpd = new LPDServer(System.getProperty("os.name").startsWith("Windows") ? 515 : 8515)) {
            lpd.addJobListener((p, is) -> {
                p.setQueueName(queueIds.get(p.getQueueName()));
                System.out.println(p);
                String auth = "";
                boolean canceled = false;
                PutResponse putResponse = null;
                do {
                    try {
                        System.out.println("old token: " + token);
                        if (token == null || token.isEmpty()) {
                            Session session = null;
                            while (session == null) {
                                Result result = PasswordDialog.prompt("mail.mcgill.ca").getResult();
                                session = getSession(result.upn, result.pw);
                            }
                            updateToken(session.getUser().getShortUser(), session.getId());
                            System.out.println("new token: " + token);
                            try {
                                persist.setProperty("token", token);
                                if (System.getProperty("os.name").startsWith("Windows")) {
                                    persist.store(new FileOutputStream(new File(System.getenv("appdata") + "/.tepid")), "TEPID");
                                } else {
                                    persist.store(new FileOutputStream(new File(System.getProperty("user.home") + "/.tepid")), "TEPID");
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        auth = "Token " + tokenHeader;
                        putResponse = Api.fetch(iTepid -> iTepid.createNewJob(p));
                        //response = tepidServer.path("jobs").request(MediaType.APPLICATION_JSON).header("Authorization", auth).post(Entity.entity(p, MediaType.APPLICATION_JSON));
                    } catch (PromiseRejectionException e) {
                        canceled = true;
                        break;
                    } catch (Exception e) {
                        clearToken();
                        e.printStackTrace();
                    }
                }
                while (putResponse == null || !putResponse.getOk());
                if (!canceled) {
                    new JobWatcher(putResponse.getId(), auth).start();
                    //come back later
                    Response response = tepidServerXz.path("jobs").path(putResponse.getId())
                            .request(MediaType.TEXT_PLAIN).header("Authorization", auth).put(Entity.entity(is, "application/x-xz"));
                    System.err.println(response.readEntity(String.class));
                } else {
                    //make os think you accepted the job
                    try {
                        byte[] buf = new byte[4092];
                        while (is.read(buf, 0, buf.length) > -1) ;
                        is.close();
                    } catch (IOException ignored) {
                    }
                }
            });
            lpd.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean validateToken(String un, String token) {
        try {
            Session s = Api.fetch(iTepid -> iTepid.validateToken(un, token));
            // Session s = tepidServer.path("sessions").path(un).path(token).request(MediaType.APPLICATION_JSON).get(Session.class);
            return s != null &&
                    (s.getExpiration() == -1L || s.getExpiration() > System.currentTimeMillis());
        } catch (Exception e) {
            System.out.println("Could not validate token " + e.getMessage());
        }
        return false;
    }

    private static Session getSession(String un, String pw) {
        try {
            SessionRequest sr = new SessionRequest(un, pw, true, true);
            return Api.fetch(iTepid -> iTepid.getSession(sr));
        } catch (Exception e) {
            System.out.println("Failed to get session " + e.getMessage());
        }
        return null;
    }

    public static void setTrayQuota(int quota) {
        systemTray.setStatus(quota + " Pages Left");
    }

    private static int trayPrinting;

    public static void setTrayPrinting(boolean printing) {
        trayPrinting += printing ? 1 : -1;
        if (trayPrinting > 0) {
            systemTray.setIcon(trayIconPrinting.getAbsolutePath());
        } else {
            systemTray.setIcon(trayIcon.getAbsolutePath());
        }
    }

}

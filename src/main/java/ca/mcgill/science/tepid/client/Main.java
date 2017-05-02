package ca.mcgill.science.tepid.client;

import ca.mcgill.science.tepid.client.PasswordDialog.Result;
import ca.mcgill.science.tepid.common.PrintJob;
import ca.mcgill.science.tepid.common.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dorkbox.systemTray.MenuEntry;
import dorkbox.systemTray.SystemTray;
import dorkbox.systemTray.SystemTrayMenuAction;
import in.waffl.q.PromiseRejectionException;
import org.glassfish.jersey.internal.util.Base64;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;

import javax.swing.*;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    final static String serverUrl = "https://tepid.science.mcgill.ca:8443/tepid";
    //	final static String serverUrl = "http://localhost:8080/tepid";
    final static WebTarget tepidServer = ClientBuilder.newBuilder().register(JacksonFeature.class).build().target(serverUrl),
            tepidServerXz = ClientBuilder.newBuilder().register(JacksonFeature.class).register(new WriterInterceptor() {
                @Override
                public void aroundWriteTo(WriterInterceptorContext ctx) throws IOException, WebApplicationException {
                    final OutputStream outputStream = ctx.getOutputStream();
                    ctx.setOutputStream(new XZOutputStream(outputStream, new LZMA2Options()));
                    ctx.proceed();
                }
            }).build().target(serverUrl);
    static String token = "", tokenUser = "";
    final static Properties persist = new Properties();
    private static final Map<String, String> queueIds = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        if (args.length > 0) {
            if (args[0].equals("--cleanup")) {
                WindowsPrinterMgmt.cleanUpPrinters();
            } else {
                System.err.println("Only valid flag is --cleanup");
                System.exit(1);
            }
            System.exit(0);
        }
        PrintQueue[] queues = tepidServer.path("queues").request(MediaType.APPLICATION_JSON).get(PrintQueue[].class);
        String defaultQueue = null;
        for (PrintQueue q : queues) {
            queueIds.put(Utils.newId(), q.name);
            if (q.defaultOn != null && Utils.wildcardMatch(q.defaultOn, Utils.getHostname())) {
                defaultQueue = q.name;
            }
        }
        if (System.getProperty("os.name").startsWith("Windows")) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
                e.printStackTrace();
            }
            for (final Entry<String, String> queue : queueIds.entrySet()) {
                final String queueName = queue.getValue();
//				WindowsPrinterMgmt.deletePrinter(queueName);
                WindowsPrinterMgmt.addPrinter(queueName, queue.getKey(), "Xerox Global Print Driver PS", queueName.equals(defaultQueue));
                Runtime.getRuntime().addShutdownHook(new Thread("Unmount Printers") {
                    @Override
                    public void run() {
                        WindowsPrinterMgmt.deletePrinter(queueName);
                        WindowsPrinterMgmt.deletePort(queue.getKey());
                    }

                    ;
                });
            }
            try {
                persist.load(new FileInputStream(new File(System.getenv("appdata") + "/.tepid")));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            for (Entry<String, String> queue : queueIds.entrySet()) {
                final String queueName = queue.getValue();
                CupsPrinterMgmt.addPrinter(queueName, queue.getKey());
                Runtime.getRuntime().addShutdownHook(new Thread("Unmount Printers") {
                    @Override
                    public void run() {
                        CupsPrinterMgmt.deletePrinter(queueName);
                    }

                    ;
                });
            }
            try {
                persist.load(new FileInputStream(new File(System.getProperty("user.home") + "/.tepid")));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (persist.getProperty("token") != null) {
            String[] parts = persist.getProperty("token").split(":");
            if (parts.length > 1) {
                String un = parts[0], sessionId = parts[1];
                if (validateToken(un, sessionId)) {
                    token = un + ":" + sessionId;
                    tokenUser = un;
                }
            }
        }

        int quota = 0;
        try {
            if (!token.isEmpty()) {
                String auth = "Token " + new String(Base64.encode(token.getBytes()));
                quota = tepidServer.path("users").path(tokenUser).path("quota")
                        .request(MediaType.APPLICATION_JSON).header("Authorization", auth).get(Integer.class);
            }
        } catch (Exception e) {
        }
        System.out.println(quota);

        SystemTray.FORCE_GTK2 = true;
        SystemTray systemTray = SystemTray.getSystemTray();
        if (systemTray == null) {
            throw new RuntimeException("Unable to load SystemTray!");
        }
        try {
            File icon = File.createTempFile("tepid", ".png");
            icon.delete();
            if (System.getProperty("os.name").startsWith("Windows")) {
                Files.copy(Utils.getResourceAsStream("trayicon/16.png"), icon.toPath());
//				Files.copy(Utils.getResourceAsStream("trayicon/32.png"), icon.toPath());
            } else {
                Files.copy(Utils.getResourceAsStream("trayicon/32.png"), icon.toPath());
            }
            icon.deleteOnExit();
            systemTray.setIcon(icon.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        systemTray.setStatus(quota + " Pages Left");
        systemTray.addMenuEntry("My Account", new SystemTrayMenuAction() {
            @Override
            public void onClick(final SystemTray systemTray, final MenuEntry menuEntry) {
                if (Desktop.isDesktopSupported()) {
                    try {
                        System.out.println(new URI(serverUrl.replace(":8080", "") + "/account?token=" + Base64.encodeAsString(token)));
                        Desktop.getDesktop().browse(new URI(serverUrl.replace(":8080", "") + "/account?token=" + Base64.encodeAsString(token)));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        systemTray.addMenuEntry("Quit", new SystemTrayMenuAction() {
            @Override
            public void onClick(final SystemTray systemTray, final MenuEntry menuEntry) {
                System.exit(0);
            }
        });

        try (LPDServer lpd = new LPDServer(System.getProperty("os.name").startsWith("Windows") ? 515 : 8515)) {
            lpd.addJobListener(new LPDJobListener() {
                @Override
                public void printJob(PrintJob p, InputStream is) {
                    p.setQueueName(queueIds.get(p.getQueueName()));
                    System.out.println(p);
                    String auth = "", id = null;
                    boolean canceled = false;
                    Response response = null;
                    do {
                        try {
                            System.out.println("old token: " + token);
                            if (token == null || token.isEmpty()) {
                                Session session = null;
                                while (session == null) {
                                    Result result = PasswordDialog.prompt().getResult();
                                    session = getSession(result.upn, result.pw);
                                }
                                token = session.getUser().shortUser + ":" + session.getId();
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
                            auth = "Token " + new String(Base64.encode(token.getBytes()));
                            response = tepidServer.path("jobs").request(MediaType.APPLICATION_JSON).header("Authorization", auth).post(Entity.entity(p, MediaType.APPLICATION_JSON));
                            id = response.readEntity(ObjectNode.class).get("id").asText();
                        } catch (PromiseRejectionException e) {
                            canceled = true;
                            break;
                        } catch (Exception e) {
                            token = null;
                            e.printStackTrace();
                        }
                    }
                    while (response == null || id == null || response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode());
                    if (!canceled) {
                        new JobWatcher(id, auth).start();
                        response = tepidServerXz.path("jobs").path(id).request(MediaType.TEXT_PLAIN).header("Authorization", auth).put(Entity.entity(is, "application/x-xz"));
                        System.err.println(response.readEntity(String.class));
                    } else {
                        //make os think you accepted the job
                        try {
                            byte[] buf = new byte[4092];
                            while (is.read(buf, 0, buf.length) > -1) ;
                            is.close();
                        } catch (IOException e1) {
                        }
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
            Session s = tepidServer.path("sessions").path(un).path(token).request(MediaType.APPLICATION_JSON).get(Session.class);
            return s.getExpiration().getTime() > System.currentTimeMillis();
        } catch (Exception e) {
        }
        return false;
    }

    private static Session getSession(String un, String pw) {
        try {
            SessionRequest sr = new SessionRequest().withUsername(un).withPassword(pw).withPersistent(true).withPermanent(true);
            return tepidServer.path("sessions").request(MediaType.APPLICATION_JSON).post(Entity.entity(sr, MediaType.APPLICATION_JSON)).readEntity(Session.class);
        } catch (Exception e) {
        }
        return null;
    }

}

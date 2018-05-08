package ca.mcgill.science.tepid.client.ui.notification;

import ca.mcgill.science.tepid.common.Utils;
import com.io.jimm.StringUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.LinkedBlockingQueue;

public class NotificationOld extends JWindow {
    private static final long serialVersionUID = -1841885707134092550L;
    private static final int green = 0x4D983E, yellow = 0xFFB300, red = 0xFF4033;
    private static final Deque<NotificationOld> active = new ConcurrentLinkedDeque<>();

    double y, ms = 2000;
    CubicBezier easeInOut = CubicBezier.create(0.42, 0, 0.58, 1.0, (1000.0 / 60.0 / ms) / 4.0);
    private boolean closeButtonHover, quotaMode, closed;
    private BufferedImage icon, oldIcon;
    private Color color = Color.BLACK, oldColor = null;
    private String title, body, oldTo, newFrom;
    private Thread animationThread;
    private final BlockingQueue<NotificationEntry> entries = new LinkedBlockingQueue<>();

    public NotificationOld() {
        final int w = 360, h = 90, p = 10;
        y = h;
        Rectangle screenBounds = this.getGraphicsConfiguration().getDevice().getDefaultConfiguration().getBounds();
        this.setBounds(screenBounds.width - w - p, screenBounds.height - h - p - 40, w, h);
        this.setBackground(Color.WHITE);
        this.setAlwaysOnTop(true);
        final JPanel content = new JPanel() {
            private static final long serialVersionUID = -4086812195244652974L;
            private final Font fQuota = getFont().deriveFont(35f).deriveFont(Font.BOLD),
                    fTitle = getFont().deriveFont(18f),
                    fBody = getFont().deriveFont(12f);
            BufferedImage xButton = loadImage(Utils.getResourceAsStream("x.png")),
                    xButtonHover = loadImage(Utils.getResourceAsStream("x_hover.png"));

            @Override
            public void paint(Graphics graphics) {
                super.paint(graphics);
                Graphics2D g = (Graphics2D) graphics;
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setFont(fTitle);
                int titleX = getHeight() + p;
                if (quotaMode) {
                    color = new Color(getQuotaColor(y / getHeight()), true);
                    g.setColor(color);
                } else {
                    if (y >= getHeight()) g.setColor(color);
                    else
                        g.setColor(new Color(combineColors(((int) ((1 - (float) y / getHeight()) * 0xff) << 24) | (oldColor.getRGB() & 0xffffff), color.getRGB()), true));
                }
                g.drawString(title == null ? "" : title, titleX, 24);
                g.fillRect(0, 0, getHeight(), getHeight());
                g.setFont(fBody);
                g.setColor(Color.BLACK);
                List<String> bodyLines = StringUtils.wrap(body == null ? "" : body, g.getFontMetrics(), getWidth() - getHeight() - p * 2);
                int bodyY = 45, lineHeight = g.getFontMetrics().getHeight();
                for (String s : bodyLines) {
                    g.drawString(s, titleX, bodyY);
                    bodyY += lineHeight;
                }
                int highY = (int) (-y % getHeight()),
                        lowY = highY + getHeight();
                if (quotaMode) {
                    String highT = String.valueOf((int) Math.floor(y / getHeight())),
                            lowT = String.valueOf((int) Math.ceil(y / getHeight()));
                    g.setColor(Color.WHITE);
                    g.setFont(fQuota);
                    AffineTransform transform = g.getTransform();
                    Shape s = g.getFont().createGlyphVector(g.getFontRenderContext(), highT).getOutline();
                    Rectangle bounds = s.getBounds();
                    g.translate((getHeight() - bounds.width) / 2 - bounds.x, -highY + (getHeight() - bounds.height) / 2 - bounds.y);
                    g.fill(s);
                    g.setTransform(transform);
                    s = g.getFont().createGlyphVector(g.getFontRenderContext(), lowT).getOutline();
                    bounds = s.getBounds();
                    g.translate((getHeight() - bounds.width) / 2 - bounds.x, -lowY + (getHeight() - bounds.height) / 2 - bounds.y);
                    g.fill(s);
                    g.setTransform(transform);
                } else {
                    AffineTransform transform = g.getTransform();
                    if (oldIcon != null) {
                        Rectangle bounds = new Rectangle(0, 0, oldIcon.getWidth(), oldIcon.getHeight());
                        g.translate((getHeight() - bounds.width) / 2 - bounds.x, -highY + (getHeight() - bounds.height) / 2 - bounds.y);
                        g.drawImage(oldIcon, 0, 0, null);
                    } else if (oldTo != null) {
                        g.setColor(Color.WHITE);
                        g.setFont(fQuota);
                        Shape s = g.getFont().createGlyphVector(g.getFontRenderContext(), oldTo).getOutline();
                        Rectangle bounds = s.getBounds();
                        g.translate((getHeight() - bounds.width) / 2 - bounds.x, -highY + (getHeight() - bounds.height) / 2 - bounds.y);
                        g.fill(s);
                    }
                    g.setTransform(transform);
                    if (icon != null) {
                        Rectangle bounds = new Rectangle(0, 0, icon.getWidth(), icon.getHeight());
                        g.translate((getHeight() - bounds.width) / 2 - bounds.x, -lowY + (getHeight() - bounds.height) / 2 - bounds.y);
                        g.drawImage(icon, 0, 0, null);
                    } else if (newFrom != null) {
                        g.setColor(Color.WHITE);
                        g.setFont(fQuota);
                        Shape s = g.getFont().createGlyphVector(g.getFontRenderContext(), newFrom).getOutline();
                        Rectangle bounds = s.getBounds();
                        g.translate((getHeight() - bounds.width) / 2 - bounds.x, -lowY + (getHeight() - bounds.height) / 2 - bounds.y);
                        g.fill(s);
                    }
                    g.setTransform(transform);

                }
                g.drawImage(closeButtonHover ? xButtonHover : xButton, w - xButton.getWidth() - p, p, null);
            }
        };
        content.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (e.getX() > content.getWidth() - 24 && e.getY() < 24) {
                    if (!closeButtonHover) {
                        closeButtonHover = true;
                        safeRepaint();
                    }
                } else {
                    if (closeButtonHover) {
                        closeButtonHover = false;
                        safeRepaint();
                    }
                }
            }
        });
        content.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                if (closeButtonHover) {
                    closeButtonHover = false;
                    safeRepaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getX() > content.getWidth() - 24 && e.getY() < 24) {
                    close();
                }
            }
        });
        this.setContentPane(content);
        this.setOpacity(0.9f);
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            addActive(this);
        } else {
            removeActive(this);
        }
        super.setVisible(b);
        if (b) this.startAnimationThread();
    }

    public void close() {
        this.closed = true;
        this.dispose();
        removeActive(this);
    }

    public boolean isClosed() {
        return closed;
    }

    private void startAnimationThread() {
        final int height = getHeight(), fps = 60, frameMs = 1000 / fps;
        if (animationThread != null && animationThread.isAlive()) return;
        animationThread = new Thread("Animation") {
            @Override
            public void run() {
                try {
                    NotificationEntry carry = null;
                    while (!Thread.interrupted()) {
                        NotificationEntry e;
                        if (carry != null) {
                            e = carry;
                            carry = null;
                        } else {
                            e = entries.take();
                            if (e.quota && !quotaMode && icon != null) {
                                carry = e;
                                newFrom = String.valueOf(e.from);
                                e = new NotificationEntry(getQuotaColor(e.from), null, e.title, e.body);
                            }
                        }
                        if (title == null) title = e.title;
                        if (body == null) body = e.body;
                        if (e.quota) {
                            quotaMode = true;
                            oldIcon = null;
                            icon = null;
                            oldTo = String.valueOf(e.to);
                        } else {
                            quotaMode = false;
                            oldColor = color == null ? new Color(e.color) : color;
                            NotificationOld.this.color = new Color(e.color);
                            oldIcon = icon;
                            if (e.icon != null) setIcon(e.icon);
                            else icon = null;
                            if (oldIcon == null && oldTo == null) {
                                oldIcon = icon;
                                safeRepaint();
                                Thread.sleep((long) ms);
                                continue;
                            }
                        }
                        double fromY = e.from * height, toY = e.to * height, distY = Math.abs(toY - fromY);
                        long start = System.currentTimeMillis();
                        for (double t = 0; t <= 1; ) {
                            long frameStart = System.currentTimeMillis();
                            t = (double) (frameStart - start) / (double) ms;
                            double pos = easeInOut.calc(t);
                            if (pos >= 0.5 && !title.equals(e.title)) title = e.title;
                            if (pos >= 0.5 && !body.equals(e.body)) body = e.body;
                            if (toY > fromY) {
                                y = pos * distY + fromY;
                            } else {
                                y = distY - (pos * distY) + toY;
                            }
                            safeRepaint();
                            long sleepTime = frameMs - (System.currentTimeMillis() - frameStart);
                            if (sleepTime > 0) Thread.sleep(sleepTime);
                        }
                        oldIcon = icon;
                        if (!e.quota) oldTo = null;
                        else newFrom = null;
                        safeRepaint();
                        Thread.sleep(frameMs * 2);
                    }
                } catch (InterruptedException e) {
                }
            }
        };
        animationThread.start();
    }

    public BufferedImage getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = loadImage(Utils.getResourceAsStream("icons/" + icon + ".png"));
    }

    public void setStatus(int color, String icon, String title, String body) {
        this.entries.add(new NotificationEntry(color, icon, title, body));
    }

    public void setQuota(int from, int to, String title, String body) {
        this.entries.add(new NotificationEntry(from, to, title, body));
    }

    public static BufferedImage loadImage(InputStream input) {
        try {
            return ImageIO.read(input);
        } catch (IOException e) {
            throw new RuntimeException("Image load failed", e);
        }
    }

    public static void addActive(NotificationOld n) {
        active.add(n);
        repositionActive();
    }

    public static void removeActive(NotificationOld n) {
        active.remove(n);
        repositionActive();
    }

    private static void repositionActive() {
        if (active.isEmpty()) return;
        Rectangle screenBounds = active.getFirst().getGraphicsConfiguration().getDevice().getDefaultConfiguration().getBounds();
        final int p = 20;
        int nY = screenBounds.height - 40 - p;
        for (NotificationOld n : active) {
            Rectangle b = n.getBounds();
            nY -= b.height + p;
            n.setBounds(b.x, nY, b.width, b.height);
        }
    }

    private void safeRepaint() {
        if (SwingUtilities.isEventDispatchThread()) {
            this.repaint();
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    NotificationOld.this.repaint();
                }
            });
        }
    }

    public static int combineColors(int a, int b) {
        int aA = (a >> 24) & 0xff,
                rA = (a >> 16) & 0xff,
                gA = (a >> 8) & 0xff,
                bA = a & 0xff,
                aB = (b >> 24) & 0xff,
                rB = (b >> 16) & 0xff,
                gB = (b >> 8) & 0xff,
                bB = b & 0xff,
                rOut = (rA * aA / 255) + (rB * aB * (255 - aA) / (255 * 255)),
                gOut = (gA * aA / 255) + (gB * aB * (255 - aA) / (255 * 255)),
                bOut = (bA * aA / 255) + (bB * aB * (255 - aA) / (255 * 255)),
                aOut = aA + (aB * (255 - aA) / 255);
        return ((aOut & 0xff) << 24) | ((rOut & 0xff) << 16) | ((gOut & 0xff) << 8) | (bOut & 0xff);
    }

    public static int getQuotaColor(double q) {
        float distTo0 = (float) ((Math.max(100 - (q), 50) - 50) / 50),
                distTo50 = Math.min((float) ((Math.max(150 - (q), 50) - 50) / 50), 1);
        int green = 0xff000000 | NotificationOld.green,
                yellow = (((int) (distTo50 * 0xff)) << 24) | NotificationOld.yellow,
                red = (((int) (distTo0 * 0xff)) << 24) | NotificationOld.red;
        return combineColors(red, combineColors(yellow, green));
    }

}

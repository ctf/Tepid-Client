package ca.mcgill.science.tepid.client.notifications;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
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

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

import com.io.jimm.StringUtils;

import ca.mcgill.science.tepid.client.CubicBezier;
import ca.mcgill.science.tepid.common.Utils;

public class Notification extends JWindow {
    private static final long serialVersionUID = -1841885707134092550L;
    private static final Color green = new Color(0x4D983E);
    private static final Deque<Notification> active = new ConcurrentLinkedDeque<>();

    double y, ms = 2000;
    CubicBezier easeInOut = CubicBezier.create(0.42, 0, 0.58, 1.0, (1000.0 / 60.0 / ms) / 4.0);
    private boolean closeButtonHover, quotaMode, closed;
    private BufferedImage icon, oldIcon;
    private Color color = Color.BLACK, oldColor = null;
    private String title = "", body = "";
	private Thread animationThread;
	private final BlockingQueue<NotificationEntry> entries = new LinkedBlockingQueue<>();

    public Notification() {
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
                    float distTo0 = (float) ((Math.max(100 - (y / getHeight()), 50) - 50) / 50),
                            distTo50 = Math.min((float) ((Math.max(150 - (y / getHeight()), 50) - 50) / 50), 1);
                    if (distTo0 == 0 && distTo50 < 0.8) {
                        g.setColor(green);
                        g.fillRect(0, 0, getHeight(), getHeight());
                        g.drawString(Notification.this.title, titleX, 24);
                    }
                    if (distTo50 > 0) {
                        g.setColor(new Color(1, 0.7f, 0, distTo50));
                        g.fillRect(0, 0, getHeight(), getHeight());
                        g.drawString(Notification.this.title, titleX, 24);
                    }
                    if (distTo0 > 0) {
                        g.setColor(new Color(1, 0.25f, 0.2f, distTo0));
                        g.fillRect(0, 0, getHeight(), getHeight());
                        g.drawString(Notification.this.title, titleX, 24);
                    }
                } else {
                    g.setColor(Notification.this.color);
                    g.drawString(Notification.this.title, titleX, 24);
                    g.fillRect(0, 0, getHeight(), getHeight());
                    if (y < getHeight()) {
                        g.setColor(Notification.this.oldColor);
                        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1 - (float) y / getHeight()));
                        g.fillRect(0, 0, getHeight(), getHeight());
                        g.drawString(Notification.this.title, titleX, 24);
                        g.setComposite(AlphaComposite.SrcOver);
                    }
                }
                g.setFont(fBody);
                g.setColor(Color.BLACK);
                List<String> bodyLines = StringUtils.wrap(Notification.this.body, g.getFontMetrics(), getWidth() - getHeight() - p * 2);
                int bodyY = 45, lineHeight = g.getFontMetrics().getHeight();
                for (String s : bodyLines) {
                    g.drawString(s, titleX, bodyY);
                    bodyY += lineHeight;
                }
                int highY = (int) (-y % getHeight()),
                        lowY = highY + getHeight();
                String highT = String.valueOf((int) Math.floor(y / getHeight())),
                        lowT = String.valueOf((int) Math.ceil(y / getHeight()));
                if (quotaMode) {
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
                } else if (icon != null) {
                	synchronized(icon) {
	                    BufferedImage prev = oldIcon == null ? icon : oldIcon;
	                    AffineTransform transform = g.getTransform();
	                    Rectangle bounds = new Rectangle(0, 0, prev.getWidth(), prev.getHeight());
	                    g.translate((getHeight() - bounds.width) / 2 - bounds.x, -highY + (getHeight() - bounds.height) / 2 - bounds.y);
	                    g.drawImage(prev, 0, 0, null);
	                    g.setTransform(transform);
	                    bounds = new Rectangle(0, 0, icon.getWidth(), icon.getHeight());
	                    g.translate((getHeight() - bounds.width) / 2 - bounds.x, -lowY + (getHeight() - bounds.height) / 2 - bounds.y);
	                    g.drawImage(icon, 0, 0, null);
	                    g.setTransform(transform);
                	}
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
                	while (!Thread.interrupted()) {
                		NotificationEntry e = entries.take();
            	        Notification.this.title = e.title;
            	        Notification.this.body = e.body;
                		if (e.quota) {
                			quotaMode = true;
                			oldIcon = null;
                			icon = null;
                		} else {
                	        quotaMode = false;
                	        oldColor = oldColor == null ? new Color(e.color) : Notification.this.color;
                	        Notification.this.color = new Color(e.color);
                	        oldIcon = icon;
                	        setIcon(e.icon);
                	        if (oldIcon == null) {
                	            safeRepaint();
                	            continue;
                	        }
                		}
	                    double fromY = e.from * height, toY = e.to * height, distY = Math.abs(toY - fromY);
	                    long start = System.currentTimeMillis();
	                    for (double t = 0; t <= 1; ) {
	                        long frameStart = System.currentTimeMillis();
	                        t = (double) (frameStart - start) / (double) ms;
	                        double pos = easeInOut.calc(t);
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

    public static void addActive(Notification n) {
        active.add(n);
        repositionActive();
    }

    public static void removeActive(Notification n) {
        active.remove(n);
        repositionActive();
    }

    private static void repositionActive() {
    	if (active.isEmpty()) return;
        Rectangle screenBounds = active.getFirst().getGraphicsConfiguration().getDevice().getDefaultConfiguration().getBounds();
        final int p = 20;
        int nY = screenBounds.height - 40 - p;
        for (Notification n : active) {
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
					Notification.this.repaint();
				}
			});
		}
	}

}

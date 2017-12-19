package ca.mcgill.science.tepid.client;

import ca.mcgill.science.tepid.common.Utils;
import in.waffl.q.Promise;
import in.waffl.q.Q;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

public class PasswordDialog extends JFrame {
    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private boolean ok;
    private final BufferedImage authBg = loadImage(Utils.getResourceAsStream("authbg.png"));
    private JPasswordField txtPassword;
    private Q<Result> deferredResult;
    private JTextField txtUpn;
    private final String user, domain;
	private final boolean inDomain;
    private static final BufferedImage[] icon = new BufferedImage[9];

    static {
        for (int i = 0; i < icon.length; i++) {
            icon[i] = loadImage(Utils.getResourceAsStream("icon/key9-" + i + ".png"));
        }
    }

    public static Promise<Result> prompt(final String domain) {
        final Q<Result> q = Q.defer();
        new Thread("AsyncInvoke") {
            @Override
            public void run() {
                EventQueue.invokeLater(() -> {
                    try {
                        final PasswordDialog frame = new PasswordDialog(q, domain);
                        frame.setVisible(true);
                        frame.toFront();
                        frame.requestFocus();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }.start();
        return q.promise;
    }


    /**
     * Create the frame.
     */
    private PasswordDialog(Q<Result> q, String domain) {
        this.deferredResult = q;
        this.domain = domain;
        setTitle("First Time Printing");
        setBounds(1920 / 2 - 434 / 2, 1080 / 2 - 262 / 2, 434, 262);
        setResizable(false);
        setIconImages(Arrays.asList(icon));
        CurrentUser cu = CurrentUser.getCurrentUser();
        this.inDomain = cu.domain.equals(domain);
        this.user = inDomain ? cu.user : "";
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!deferredResult.resolved()) {
                    if (ok) {
                        deferredResult.resolve(new Result(txtUpn.getText().split("@")[0], new String(txtPassword.getPassword())));
                    } else {
                        deferredResult.reject("Canceled by user");
                    }
                }
            }

            @Override
            public void windowActivated(WindowEvent e) {
                (inDomain ? txtPassword : txtUpn).requestFocusInWindow();
            }
        });
        contentPane = new JPanel() {
            private static final long serialVersionUID = 1;

            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(0xffffff));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(new Color(0xdfdfdf));
                g.drawLine(18, 66, 416, 66);
                g.drawImage(authBg, 22, 82, null);
                g.drawLine(0, 182, 434, 182);
                g.setColor(new Color(0xf0f0f0));
                g.fillRect(0, 183, 434, 89);
                super.paintComponent(g);
            }
        };
        contentPane.setLayout(null);
        contentPane.setBackground(new Color(0x00ffffff, true));
        setContentPane(contentPane);
        JLabel lblAction = new JLabel("Confirm your credentials");
        lblAction.setBounds(18, 6, 416, 30);
        lblAction.setForeground(new Color(0x3a33ab));
        lblAction.setFont(new Font("Arial", Font.PLAIN, 16));
        contentPane.add(lblAction);
        JLabel lblExplanation = new JLabel("Please log in to continue printing");
        lblExplanation.setBounds(18, 26, 416, 40);
        lblExplanation.setForeground(new Color(0x0));
        lblExplanation.setFont(new Font("Arial", Font.PLAIN, 12));
        contentPane.add(lblExplanation);
        JButton btnOk = new JButton("OK");
        btnOk.setBounds(218, 194, 94, 26);
        btnOk.setForeground(new Color(0x0));
        btnOk.setFont(new Font("Arial", Font.PLAIN, 12));
        btnOk.addActionListener(e -> {
            ok = true;
            PasswordDialog.this.dispatchEvent(new WindowEvent(PasswordDialog.this, WindowEvent.WINDOW_CLOSING));
            PasswordDialog.this.dispose();
        });
        contentPane.add(btnOk);
        JButton btnCancel = new JButton("Cancel");
        btnCancel.setBounds(320, 194, 94, 26);
        btnCancel.setForeground(new Color(0x0));
        btnCancel.setFont(new Font("Arial", Font.PLAIN, 12));
        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                PasswordDialog.this.dispatchEvent(new WindowEvent(PasswordDialog.this, WindowEvent.WINDOW_CLOSING));
                PasswordDialog.this.dispose();
            }
        });
        contentPane.add(btnCancel);
        txtUpn = new JTextField(this.user + (inDomain ? ("@" + this.domain) : "")) {
            private static final long serialVersionUID = -8637246599077545633L;
            TextFieldThemer themer = new TextFieldThemer(this, "User name");

            @Override
            public void paint(Graphics g) {
                themer.paint(g);
                super.paint(g);
            }
        };

        txtUpn.setBounds(102, 92, 260, 26);
        contentPane.add(txtUpn);
        txtPassword = new JPasswordField("") {
            private static final long serialVersionUID = -8637246599077545633L;
            TextFieldThemer themer = new TextFieldThemer(this, "Password");

            @Override
            public void paint(Graphics g) {
                themer.paint(g);
                super.paint(g);
            }
        };
        txtPassword.setBounds(102, 120, 260, 26);
        txtPassword.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume();
                    ok = true;
                    PasswordDialog.this.dispatchEvent(new WindowEvent(PasswordDialog.this, WindowEvent.WINDOW_CLOSING));
                    PasswordDialog.this.dispose();
                }
            }
        });
        contentPane.add(txtPassword);
        JLabel lblDomain = new JLabel("Domain: " + this.domain);
        lblDomain.setBounds(105, 144, 220, 18);
        lblDomain.setForeground(new Color(0x0));
        lblDomain.setFont(new Font("Arial", Font.PLAIN, 12));
        contentPane.add(lblDomain);
        JLabel lblDisclaimer = new JLabel("<html><p>By continuing you agree<br>to the <a href=\"***REMOVED***.htm\">terms & conditions</a></p></html>");
        lblDisclaimer.setBounds(18, 194, 200, 26);
        lblDisclaimer.setForeground(new Color(0x0));
        lblDisclaimer.setFont(new Font("Arial", Font.PLAIN, 10));
        lblDisclaimer.setHorizontalAlignment(SwingConstants.LEFT);
        lblDisclaimer.setBackground(Color.WHITE);
        lblDisclaimer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().browse(new URI("http://taskforce.science.mcgill.ca/about.html"));
                    } catch (IOException | URISyntaxException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });
        contentPane.add(lblDisclaimer);
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        super.setAlwaysOnTop(true);
        if (System.getProperty("os.name").startsWith("Windows")) {
            super.setState(JFrame.ICONIFIED);
            super.setState(JFrame.NORMAL);
        }

    }

    public static BufferedImage loadImage(InputStream input) {
        try {
            return ImageIO.read(input);
        } catch (IOException e) {
            throw new RuntimeException("Image load failed", e);
        }
    }

    public static class Result {
        String upn, pw;

        public Result(String upn, String pw) {
            this.upn = upn;
            this.pw = pw;
        }

        @Override
        public String toString() {
            return "Result [upn=" + upn + ", pw=" + pw + "]";
        }
    }

}

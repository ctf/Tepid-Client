package ca.mcgill.science.tepid.client.ui.full.text;

import ca.mcgill.science.tepid.common.Utils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class TextFieldThemer {
    private static final BufferedImage[] bg;

    static {
        bg = new BufferedImage[]{
                loadImage(Utils.getResourceAsStream("textfield-0.png")),
                loadImage(Utils.getResourceAsStream("textfield-1.png")),
                loadImage(Utils.getResourceAsStream("textfield-2.png")),
                loadImage(Utils.getResourceAsStream("textfield-3.png"))
        };
    }

    public static BufferedImage loadImage(InputStream input) {
        try {
            return ImageIO.read(input);
        } catch (IOException e) {
            throw new RuntimeException("Image load failed", e);
        }
    }

    private JTextField textField;
    private boolean hover;

    public TextFieldThemer(final JTextField textField, String placeholder) {
        this.textField = textField;
        textField.setOpaque(false);
        textField.setBorder(BorderFactory.createEmptyBorder(5, 7, 5, 7));
        textField.setForeground(new Color(0x0));
        textField.setFont(new Font("Arial", Font.PLAIN, 12));
        TextPrompt prmUpn = new TextPrompt(placeholder, textField);
        prmUpn.setForeground(new Color(0x666666));
        textField.addFocusListener(new FocusListener() {
            @Override
            public void focusLost(FocusEvent e) {
                textField.repaint();
            }

            @Override
            public void focusGained(FocusEvent e) {
                textField.repaint();
            }
        });
        textField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                textField.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                textField.repaint();
            }
        });
    }

    public void paint(Graphics g) {
        g.setColor(textField.getBackground());
        g.fillRect(4, 4, textField.getWidth() - 8, textField.getHeight() - 8);
        BufferedImage np = textField.hasFocus() ? bg[2] : (hover ? bg[1] : bg[3]);
        drawNinePiece(g, np, 4, textField.getWidth(), textField.getHeight());
    }

    public static void drawNinePiece(Graphics g, BufferedImage img, int cornerSize, int w, int h) {
        int imgW = img.getWidth(), imgH = img.getHeight(), midW = imgW - cornerSize * 2, midH = imgH - cornerSize * 2;
        BufferedImage topLeft = img.getSubimage(0, 0, cornerSize, cornerSize),
                topRight = img.getSubimage(imgW - cornerSize, 0, cornerSize, cornerSize),
                bottomRight = img.getSubimage(imgW - cornerSize, imgH - cornerSize, cornerSize, cornerSize),
                bottomLeft = img.getSubimage(0, imgH - cornerSize, cornerSize, cornerSize),
                top = img.getSubimage(cornerSize, 0, midW, cornerSize),
                bottom = img.getSubimage(cornerSize, imgH - cornerSize, midW, cornerSize),
                right = img.getSubimage(imgW - cornerSize, cornerSize, cornerSize, midH),
                left = img.getSubimage(0, cornerSize, cornerSize, midH),
                mid = img.getSubimage(cornerSize, cornerSize, midW, midH);
        g.drawImage(topLeft, 0, 0, cornerSize, cornerSize, null);
        g.drawImage(topRight, w - cornerSize, 0, cornerSize, cornerSize, null);
        g.drawImage(bottomRight, w - cornerSize, h - cornerSize, cornerSize, cornerSize, null);
        g.drawImage(bottomLeft, 0, h - cornerSize, cornerSize, cornerSize, null);
        g.drawImage(top, cornerSize, 0, w - cornerSize * 2, cornerSize, null);
        g.drawImage(bottom, cornerSize, h - cornerSize, w - cornerSize * 2, cornerSize, null);
        g.drawImage(left, 0, cornerSize, cornerSize, h - cornerSize * 2, null);
        g.drawImage(right, w - cornerSize, cornerSize, cornerSize, h - cornerSize * 2, null);
        g.drawImage(mid, cornerSize, cornerSize, w - cornerSize * 2, h - cornerSize * 2, null);
    }
}

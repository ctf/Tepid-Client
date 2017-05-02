package dorkbox.systemTray.windows;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Win32Icon {

    public static final int BI_RGB = 0, BI_RLE8 = 1, BI_RLE4 = 2, BI_BITFIELDS = 3, BI_JPEG = 4, BI_PNG = 5;

    public static byte[] imageToIco(BufferedImage img, int... sizes) throws IOException {
        int size = 0;
        for (int s : sizes) {
            size += bmpSize(s);
        }
        ByteBuffer ico = ByteBuffer.allocate(6 + (16 * sizes.length) + size - (14 * sizes.length)).order(ByteOrder.LITTLE_ENDIAN);
        ico.putShort((short) 0);
        ico.putShort((short) 1); //image type (1==icon)
        ico.putShort((short) sizes.length); //number of subimages
        int pos = ico.position() + 16 * sizes.length;
        for (int s : sizes) {
            //write icondirentry
            ico.put((byte) (0xff & s));
            ico.put((byte) (0xff & s));
            ico.putShort((short) 0);
            ico.putShort((short) 1); //number of color planes
            ico.putShort((short) 32); //32-bit
            ico.putInt(bmpSize(s) - 14);
            ico.putInt(pos);
            pos += bmpSize(s) - 14;
        }
        for (int s : sizes) {
            byte[] bmp = imageToBitmap(scale(img, s));
            ico.put(bmp, 14, bmp.length - 14);
        }
        return ico.array();
    }

    public static int bmpSize(int s) {
        final int rowSize = (int) (Math.floor((32 * s + 31) / 32) * 4),
                pixelArraySize = rowSize * s;
        return 14 + 40 + pixelArraySize + pixelArraySize / 16;
    }

    public static BufferedImage scale(BufferedImage img, int s) {
        BufferedImage out = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(img, 0, 0, s, s, null);
        g.dispose();
        return out;
    }

    public static byte[] imageToBitmap(BufferedImage img) {
        final int rowSize = (int) (Math.floor((32 * img.getWidth() + 31) / 32) * 4),
                pixelArraySize = rowSize * img.getHeight();
        ByteBuffer bmp = ByteBuffer.allocate(14 + 40 + pixelArraySize + pixelArraySize / 16).order(ByteOrder.LITTLE_ENDIAN);
        bmp.put("BM".getBytes());
        bmp.putInt(bmp.capacity());
        bmp.putInt(0);
        bmp.putInt(14 + 40);
        //begin BITMAPV3HEADER
        bmp.putInt(40); //bytes in header (fixed for v3)
        bmp.putInt(img.getWidth());
        bmp.putInt(img.getHeight() * 2);
        bmp.putShort((short) 1); //number of color planes
        bmp.putShort((short) 32); //32-bit
        bmp.putInt(BI_RGB); //no compression
        bmp.putInt(0); //size of raw bitmap data (unused by ico)
        bmp.putInt(0); //no dpi
        bmp.putInt(0); //no dpi
        bmp.putInt(0);
        bmp.putInt(0);
        for (int i = 0; i < pixelArraySize / 16; i++) {
            bmp.put((byte) 0); //fake the mask required for lower bit-depth icons
        }
        //copy pixels into padded memory block
        int[] pixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int index = 14 + 40 + (x * 4) + ((img.getHeight() - y - 1) * rowSize);
                bmp.putInt(index, pixels[x + y * img.getWidth()]);
            }
        }
        return bmp.array();
    }
}

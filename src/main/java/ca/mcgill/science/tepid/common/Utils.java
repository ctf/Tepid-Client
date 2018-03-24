package ca.mcgill.science.tepid.common;

import java.io.InputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Scanner;
import java.util.UUID;

public class Utils {

    public static boolean booleanValue(String s) {
        return Boolean.parseBoolean(s);
    }

    public static byte byteValue(String s) {
        return byteValue(s, (byte) 0x0);
    }

    public static byte byteValue(String s, byte defaultValue) {
        try {
            return (byte) Integer.parseInt(s);
        } catch (NumberFormatException e) {
        }
        return defaultValue;
    }

    public static short shortValue(String s) {
        return shortValue(s, (short) 0x0);
    }

    public static short shortValue(String s, short defaultValue) {
        try {
            return (short) Integer.parseInt(s);
        } catch (NumberFormatException e) {
        }
        return defaultValue;
    }

    public static int intValue(String s) {
        return intValue(s, -1);
    }

    public static int intValue(String s, int defaultValue) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
        }
        return defaultValue;
    }

    public static long longValue(String s) {
        return longValue(s, -1);
    }

    public static long longValue(String s, long defaultValue) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
        }
        return defaultValue;
    }

    public static float floatValue(String s) {
        return floatValue(s, -1);
    }

    public static float floatValue(String s, float defaultValue) {
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException e) {
        }
        return defaultValue;
    }

    public static double doubleValue(String s) {
        return doubleValue(s, -1);
    }

    public static double doubleValue(String s, double defaultValue) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
        }
        return defaultValue;
    }

    //todo port to actual commons libs
    public static InputStream getResourceAsStream(String path) {
        ClassLoader loader = Utils.class.getClassLoader();
        return loader.getResourceAsStream(path);
    }

    public static String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static SecureRandom random = new SecureRandom();

    public static synchronized String newSessionId() {
        return new BigInteger(130, random).toString(32);
    }

    public static boolean wildcardMatch(String pattern, String input) {
        String[] or = pattern.split("(;|\\|)");
        for (String pat : or) {
            String regex = ("\\Q" + pat.replace("*", "\\E.*?\\Q") + "\\E")
                    .replace("\\Q\\E", "");
            if (input.matches(regex)) return true;
        }
        return false;
    }

    @SuppressWarnings("resource")
    public static String getHostname() {
        try {
            Process p = new ProcessBuilder("hostname").start();
            Scanner s = new Scanner(p.getInputStream());
            p.waitFor();
            if (s.useDelimiter("\\A").hasNext()) return s.next().trim();
            s.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}

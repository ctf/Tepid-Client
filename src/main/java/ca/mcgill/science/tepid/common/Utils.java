package ca.mcgill.science.tepid.common;

import java.io.InputStream;
import java.util.Scanner;

public class Utils {

    //todo port to actual commons libs
    public static InputStream getResourceAsStream(String path) {
        ClassLoader loader = Utils.class.getClassLoader();
        return loader.getResourceAsStream(path);
    }

}

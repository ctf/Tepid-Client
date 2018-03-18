package ca.mcgill.science.tepid.client;

import ca.mcgill.science.tepid.utils.PropUtils;

import java.util.Properties;

public class Config {
    public static String baseUrl;
    public static String serverUrl;
    private final static String TEST_URL = "http://testpid.science.mcgill.ca";

    private final static String PRODUCTION_URL = "https://tepid.science.mcgill.ca";

    public static void setup(){
        Properties props = PropUtils.INSTANCE.loadProps("priv.properties");
        if (props == null) {
            System.out.println("No properties found");
            baseUrl = TEST_URL;
        } else {
            baseUrl = props.getProperty("URL",TEST_URL);
        }
        serverUrl = baseUrl + (PRODUCTION_URL.equals(baseUrl)? ":8080/tepid/": ":8443/tepid");

        System.out.println("Server url: " + Config.serverUrl);

    }

}

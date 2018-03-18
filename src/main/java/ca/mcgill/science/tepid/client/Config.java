package ca.mcgill.science.tepid.client;

import ca.mcgill.science.tepid.utils.PropUtils;

import java.util.Properties;

public class Config {
    private final static String TEST_URL = "http://testpid.science.mcgill.ca";
    private final static String PRODUCTION_URL = "https://tepid.science.mcgill.ca";
    private static Config INSTANCE;

    public static String baseUrl() {
        return getInstance().baseUrl;
    }

    public static String serverUrl() {
        return getInstance().serverUrl;
    }

    private static Config getInstance() {
        if (INSTANCE == null)
            INSTANCE = new Config();
        return INSTANCE;
    }

    private String baseUrl;
    private String serverUrl;

    private Config() {
        Properties props = PropUtils.INSTANCE.loadProps("priv.properties");
        if (props == null) {
            System.out.println("No properties found");
            baseUrl = TEST_URL;
        } else {
            String url = props.getProperty("URL", TEST_URL);
            switch (url.toLowerCase()) {
                case "tepid":
                    baseUrl = PRODUCTION_URL;
                case "testpid":
                    baseUrl = TEST_URL;
                default:
                    baseUrl = url;
            }
        }
        serverUrl = baseUrl + (PRODUCTION_URL.equals(baseUrl) ? ":8080/tepid/" : ":8443/tepid");

        System.out.println("Server url: " + serverUrl);
    }

}

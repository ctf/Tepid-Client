package ca.mcgill.science.tepid.client;

import ca.mcgill.science.tepid.client.ui.observers.MainObserver;
import com.bugsnag.Bugsnag;

public class Main {

    public static void main(String[] args) {

        // Hacky fix for the removal of the --clean option
        if (args.length > 0) {
            System.exit(0);
        }

        Bugsnag bugsnager = new Bugsnag("5870fc6e49bb500adc05defc7703a25e");
        Client client = Client.create(MainObserver.INSTANCE);
    }

}

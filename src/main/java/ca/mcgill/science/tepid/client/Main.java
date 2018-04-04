package ca.mcgill.science.tepid.client;

import ca.mcgill.science.tepid.client.ui.observers.MainObserver;

public class Main {

    public static void main(String[] args) {
        Client client = Client.create(MainObserver.INSTANCE);
    }

}

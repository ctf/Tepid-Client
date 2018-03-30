package ca.mcgill.science.tepid.clientkt;

import ca.mcgill.science.tepid.clientkt.ui.console.ConsoleObserver;

public class Main {

    public static void main (String[] args) {
        new Client(new ConsoleObserver());
    }

}

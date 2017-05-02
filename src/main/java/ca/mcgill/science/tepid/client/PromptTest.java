package ca.mcgill.science.tepid.client;

import javax.swing.*;

public class PromptTest {

    public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        System.out.println(PasswordDialog.prompt().getResult());
        System.exit(0);
    }

}

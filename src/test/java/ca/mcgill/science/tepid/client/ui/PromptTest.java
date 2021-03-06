package ca.mcgill.science.tepid.client.ui;

import ca.mcgill.science.tepid.client.ui.text.PasswordDialog;
import ca.mcgill.science.tepid.client.utils.Config;

import javax.swing.*;

public class PromptTest {

    public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        System.out.println(PasswordDialog.prompt(Config.INSTANCE.getACCOUNT_DOMAIN()).getResult());
        System.exit(0);
    }

}

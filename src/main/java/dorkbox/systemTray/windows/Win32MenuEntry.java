package dorkbox.systemTray.windows;

import dorkbox.systemTray.MenuEntry;
import dorkbox.systemTray.SystemTrayMenuAction;

import java.io.InputStream;
import java.net.URL;

public class Win32MenuEntry implements MenuEntry {

    private SystemTrayMenuAction callback;
    private String text;
//    private Win32SystemTray systemTray;

    Win32MenuEntry(final String label, final String imagePath, final SystemTrayMenuAction callback, final Win32SystemTray systemTray) {
        this.text = label;
        this.callback = callback;
//        this.systemTray = systemTray;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public void setText(String text) {
        this.text = text;
    }

    @Override
    public void setImage(String imagePath) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setImage(URL imageUrl) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setImage(String cacheName, InputStream imageStream) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setImage(InputStream imageStream) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setCallback(SystemTrayMenuAction callback) {
        this.callback = callback;
    }

    SystemTrayMenuAction getCallback() {
        return callback;
    }

    @Override
    public void remove() {
        //remove hook
    }

}

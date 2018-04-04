package dorkbox.systemTray.windows;

import com.sun.jna.WString;
import com.sun.jna.win32.StdCallLibrary.StdCallCallback;
import dorkbox.systemTray.MenuEntry;
import dorkbox.systemTray.SystemTray;
import dorkbox.systemTray.SystemTrayMenuAction;
import dorkbox.systemTray.windows.Win32.*;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.CyclicBarrier;

import static dorkbox.systemTray.windows.Win32.NOTIFYICONDATA.NIF_ICON;
import static dorkbox.systemTray.windows.Win32.NOTIFYICONDATA.NIF_MESSAGE;
import static dorkbox.systemTray.windows.Win32.Shell32.*;
import static dorkbox.systemTray.windows.Win32.User32.*;
import static dorkbox.systemTray.windows.Win32.User32_64.SetWindowLongPtr;

public class Win32SystemTray extends SystemTray {

    private volatile String statusText, iconPath;
    private boolean visible;
    private final NOTIFYICONDATA notifyIconData;
    private Thread trayThread;

    public Win32SystemTray() {
        notifyIconData = new NOTIFYICONDATA();
    }

    @Override
    public void shutdown() {
        setVisible(false);
    }

    @Override
    public String getStatus() {
        return statusText;
    }

    @Override
    public void setStatus(String statusText) {
        this.statusText = statusText;
    }

    @Override
    protected void setIcon_(final String iconPath) {
        this.iconPath = iconPath;
        if (visible) {
            File icon;
            try {
                icon = writeToTmp(Win32Icon.imageToIco(ImageIO.read(new File(iconPath)), 32, 16), "tray", ".ico");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            notifyIconData.hIcon = LoadImage(null, new WString(icon.getAbsolutePath()), IMAGE_ICON, 0, 0, LR_LOADFROMFILE);
            Shell_NotifyIcon(NIM_MODIFY, notifyIconData);
//    		Shell_NotifyIcon(NIM_SETFOCUS, notifyIconData);
        } else {
            this.setVisible(true);
        }
    }

    @Override
    public void addMenuEntry(String menuText, String imagePath, SystemTrayMenuAction callback) {
        this.menuEntries.add(new Win32MenuEntry(menuText, imagePath, callback, this));
    }

    @Override
    public void addMenuEntry(String menuText, URL imageUrl, SystemTrayMenuAction callback) {
        this.menuEntries.add(new Win32MenuEntry(menuText, null, callback, this));

    }

    @Override
    public void addMenuEntry(String menuText, String cacheName, InputStream imageStream, SystemTrayMenuAction callback) {
        this.menuEntries.add(new Win32MenuEntry(menuText, null, callback, this));

    }

    @Override
    public void addMenuEntry(String menuText, InputStream imageStream, SystemTrayMenuAction callback) {
        this.menuEntries.add(new Win32MenuEntry(menuText, null, callback, this));

    }

    private void setVisible(boolean visible) {
        if (this.visible == visible) return;
        this.visible = visible;
        if (!visible) {
            Shell_NotifyIcon(NIM_DELETE, notifyIconData);
            if (trayThread != null) trayThread.interrupt();
        } else {
            final CyclicBarrier barrier = new CyclicBarrier(2);
            final SystemTray tray = this;
            trayThread = new Thread("Tray") {
                public void run() {
                    final POINT mousePosition = new POINT();
                    StdCallCallback wndProc;
                    HWND hwnd = CreateWindowEx(0, new WString("STATIC"), new WString(Win32SystemTray.class.getCanonicalName()), 0, 0, 0, 0, 0, 0, 0, 0, 0);
                    if (hwnd == null) {
                        System.err.println("Unable to create tray window.");
                        System.exit(0);
                    }
                    final int wmTaskbarCreated = RegisterWindowMessage(new WString("TaskbarCreated"));
                    final int wmTrayIcon = WM_USER + 1;
                    File icon;
                    try {
                        icon = writeToTmp(Win32Icon.imageToIco(ImageIO.read(new File(iconPath)), 32, 16), "tray", ".ico");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    notifyIconData.hWnd = hwnd;
                    notifyIconData.uID = 1000;
                    notifyIconData.uFlags = NIF_ICON | NIF_MESSAGE;
                    notifyIconData.uCallbackMessage = wmTrayIcon;
                    notifyIconData.hIcon = LoadImage(null, new WString(icon.getAbsolutePath()), IMAGE_ICON, 0, 0, LR_LOADFROMFILE);
                    notifyIconData.setTooltip("TEPID by CTF");
                    Shell_NotifyIcon(NIM_ADD, notifyIconData);
                    icon.delete();
                    Runtime.getRuntime().addShutdownHook(new Thread() {
                        public void run() {
                            setVisible(false);
                        }
                    });
                    wndProc = new StdCallCallback() {
                        @SuppressWarnings("unused")
                        public int callback(HWND hWnd, int message, Parameter wParameter, Parameter lParameter) {
                            if (message == wmTrayIcon) {
                                int lParam = lParameter.intValue();
                                switch (lParam) {
                                    case WM_LBUTTONDOWN:
                                    case WM_LBUTTONUP:
                                    case WM_RBUTTONDOWN:
                                        break;
                                    case WM_RBUTTONUP:
                                        if (GetCursorPos(mousePosition)) {
                                            HMENU menu = CreatePopupMenu();
                                            if (statusText != null && !statusText.isEmpty()) {
                                                AppendMenu(menu, MF_STRING | MF_GRAYED, 0, statusText);
                                                AppendMenu(menu, MF_SEPARATOR, 0, null);
                                            }
                                            int i = 1;
                                            for (MenuEntry entry : menuEntries) {
                                                final int IDR_ENTRY = (i++);
                                                AppendMenu(menu, MF_STRING, IDR_ENTRY, entry.getText());
                                            }
                                            SetForegroundWindow(hWnd);
                                            int choice = TrackPopupMenuEx(menu, TPM_LEFTALIGN | TPM_TOPALIGN | TPM_RIGHTBUTTON | TPM_NONOTIFY | TPM_RETURNCMD, mousePosition.x, mousePosition.y, hWnd, null);
                                            if (choice > 0) {
                                                Win32MenuEntry chosenEntry = (Win32MenuEntry) menuEntries.get(choice - 1);
                                                if (chosenEntry != null) {
                                                    chosenEntry.getCallback().onClick(tray, chosenEntry);
                                                }
                                            }
                                        }
                                        break;
                                }
                            } else if (message == wmTaskbarCreated) {
                                // Add icon again if explorer crashed.
                                Shell_NotifyIcon(NIM_ADD, notifyIconData);
                            }
                            return DefWindowProc(hWnd, message, wParameter, lParameter);
                        }
                    };
                    if (Win32.is64Bit)
                        SetWindowLongPtr(hwnd, GWL_WNDPROC, wndProc);
                    else
                        SetWindowLong(hwnd, GWL_WNDPROC, wndProc);

                    try {
                        barrier.await();
                    } catch (Exception ignored) {
                    }

                    MSG msg = new MSG();
                    while (GetMessage(msg, null, 0, 0)) {
                        TranslateMessage(msg);
                        DispatchMessage(msg);
                    }
                }
            };
            trayThread.start();
            try {
                barrier.await();
            } catch (Exception ignored) {
            }
        }
    }

    public static File writeToTmp(byte[] bytes, String prefix, String ext) {
        try {
            File tmp = File.createTempFile(prefix, ext);
            FileOutputStream fos = new FileOutputStream(tmp);
            fos.write(bytes, 0, bytes.length);
            fos.close();
            return tmp;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

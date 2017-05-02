package dorkbox.systemTray.windows;


import com.sun.jna.*;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.W32APIOptions;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings({"serial", "rawtypes"})
public class Win32 {
    static public boolean is64Bit = System.getProperty("os.arch").equals("amd64") || System.getProperty("os.arch").equals("x86_64");

    static class CLibrary {
        public static native int strlen(Pointer p);

        public static native int wcslen(Pointer p);

        static {
            Native.register(Platform.C_LIBRARY_NAME);
        }
    }

    static public class Kernel32 {
        static {
            Native.register(NativeLibrary.getInstance("kernel32", W32APIOptions.DEFAULT_OPTIONS));
        }

        static public final int GMEM_MOVEABLE = 0x2;

        static public native Pointer GlobalAlloc(int uFlags, int dwBytes);

        static public native Pointer GlobalLock(Pointer hMem);

        static public native boolean GlobalUnlock(Pointer hMem);
    }

    static public class User32 {
        static {
            Native.register(NativeLibrary.getInstance("user32", W32APIOptions.DEFAULT_OPTIONS));
        }

        static public final int MOD_ALT = 0x1;
        static public final int MOD_CONTROL = 0x2;
        static public final int MOD_SHIFT = 0x4;
        static public final int MOD_WIN = 0x8;
        static public final int MOD_NOREPEAT = 0x4000;

        static public final byte VK_SHIFT = 0x10;
        static public final byte VK_CONTROL = 0x11;
        static public final byte VK_MENU = 0x12;
        static public final byte VK_LWIN = 0x5b;
        static public final byte VK_RWIN = 0x5c;
        static public final int KEYEVENTF_KEYUP = 2;

        static public final int GWL_WNDPROC = -4;

        static public final int WM_HOTKEY = 0x312;
        static public final int WM_CLIPBOARDUPDATE = 0x31D;
        static public final int WM_USER = 0x400;
        static public final int WM_LBUTTONDOWN = 0x201;
        static public final int WM_LBUTTONUP = 0x202;
        static public final int WM_RBUTTONDOWN = 0x204;
        static public final int WM_RBUTTONUP = 0x205;

        static public final int CF_TEXT = 1;
        static public final int CF_UNICODETEXT = 13;
        static public final int CF_HDROP = 15;

        static public final int IMAGE_ICON = 1;
        static public final int LR_LOADFROMFILE = 0x10;

        static public final int MONITOR_DEFAULTTONEAREST = 2;

        static public final int MF_ENABLED = 0;
        static public final int MF_GRAYED = 1;
        static public final int MF_DISABLED = 2;
        static public final int MF_BITMAP = 4;
        static public final int MF_CHECKED = 8;
        static public final int MF_MENUBARBREAK = 32;
        static public final int MF_MENUBREAK = 64;
        static public final int MF_OWNERDRAW = 256;
        static public final int MF_POPUP = 16;
        static public final int MF_SEPARATOR = 0x800;
        static public final int MF_STRING = 0;
        static public final int MF_UNCHECKED = 0;
        static public final int MF_DEFAULT = 4096;
        static public final int MF_SYSMENU = 0x2000;
        static public final int MF_HELP = 0x4000;
        static public final int MF_END = 128;
        static public final int MF_RIGHTJUSTIFY = 0x4000;
        static public final int MF_MOUSESELECT = 0x8000;
        static public final int MF_INSERT = 0;
        static public final int MF_CHANGE = 128;
        static public final int MF_APPEND = 256;
        static public final int MF_DELETE = 512;
        static public final int MF_REMOVE = 4096;
        static public final int MF_USECHECKBITMAPS = 512;
        static public final int MF_UNHILITE = 0;
        static public final int MF_HILITE = 128;

        static public final int TPM_CENTERALIGN = 4;
        static public final int TPM_LEFTALIGN = 0;
        static public final int TPM_RIGHTALIGN = 8;
        static public final int TPM_LEFTBUTTON = 0;
        static public final int TPM_RIGHTBUTTON = 2;
        static public final int TPM_HORIZONTAL = 0;
        static public final int TPM_VERTICAL = 64;
        static public final int TPM_TOPALIGN = 0;
        static public final int TPM_VCENTERALIGN = 16;
        static public final int TPM_BOTTOMALIGN = 32;
        static public final int TPM_NONOTIFY = 128;
        static public final int TPM_RETURNCMD = 256;
        static public final int TPM_RECURSE = 1;

        static public final int MB_USERICON = 128;
        static public final int MB_ICONASTERISK = 64;
        static public final int MB_ICONEXCLAMATION = 0x30;
        static public final int MB_ICONWARNING = 0x30;
        static public final int MB_ICONERROR = 16;
        static public final int MB_ICONHAND = 16;
        static public final int MB_ICONQUESTION = 32;
        static public final int MB_OK = 0;
        static public final int MB_ABORTRETRYIGNORE = 2;
        static public final int MB_APPLMODAL = 0;
        static public final int MB_DEFAULT_DESKTOP_ONLY = 0x20000;
        static public final int MB_HELP = 0x4000;
        static public final int MB_RIGHT = 0x80000;
        static public final int MB_RTLREADING = 0x100000;
        static public final int MB_TOPMOST = 0x40000;
        static public final int MB_DEFBUTTON1 = 0;
        static public final int MB_DEFBUTTON2 = 256;
        static public final int MB_DEFBUTTON3 = 512;
        static public final int MB_DEFBUTTON4 = 0x300;
        static public final int MB_ICONINFORMATION = 64;
        static public final int MB_ICONSTOP = 16;
        static public final int MB_OKCANCEL = 1;
        static public final int MB_RETRYCANCEL = 5;
        static public final int MB_SETFOREGROUND = 0x10000;
        static public final int MB_SYSTEMMODAL = 4096;
        static public final int MB_TASKMODAL = 0x2000;
        static public final int MB_YESNO = 4;
        static public final int MB_YESNOCANCEL = 3;
        static public final int MB_ICONMASK = 240;
        static public final int MB_DEFMASK = 3840;
        static public final int MB_MODEMASK = 0x00003000;
        static public final int MB_MISCMASK = 0x0000C000;
        static public final int MB_NOFOCUS = 0x00008000;
        static public final int MB_TYPEMASK = 15;
        static public final int MB_CANCELTRYCONTINUE = 6;

        // Window

        static public native Pointer FindWindow(WString lpClassName, WString lpWindowName);

        static public native HMENU CreatePopupMenu();

        static public native boolean AppendMenu(HMENU hMenu, int uFlags, int uIDNewItem, String lpNewItem);

        static public native int TrackPopupMenuEx(HMENU hMenu, int fuFlags, int x, int y, HWND hwnd, Structure s);

        static public native int MessageBox(HWND hWnd, String lpText, String lpCaption, int uType);

        static public native HWND CreateWindowEx(int dwExStyle, WString lpClassName, WString lpWindowName, int dwStyle, int x,
                                                 int y, int nWidth, int nHeight, int hWndParent, int hMenu, int hInstance, int lpParam);

        static public native int SetWindowLong(HWND hWnd, int nIndex, Callback procedure);

        static public native int DefWindowProc(HWND hWnd, int uMsg, Parameter wParam, Parameter lParam);

        static public native boolean SetForegroundWindow(HWND hWnd);

        static public native boolean GetMessage(MSG lpMsg, HWND hWnd, int wMsgFilterMin, int wMsgFilterMax);

        static public native boolean TranslateMessage(MSG lpMsg);

        static public native boolean DispatchMessage(MSG lpMsg);

        static public native int RegisterWindowMessage(WString lpString);

        static public native Pointer GetForegroundWindow();

        static public native int GetWindowThreadProcessId(HWND hWnd, IntByReference lpdwProcessId);

        static public native int GetClassName(HWND hWnd, char[] lpClassName, int nMaxCount);

        static public native boolean ClientToScreen(HWND hWnd, POINT lpPoint);

        static public native boolean GetWindowRect(HWND hWnd, RECT rect);

        static public native Pointer MonitorFromWindow(HWND hWnd, int dwFlags);

        static public native boolean GetMonitorInfo(Pointer hMonitor, MONITORINFO lpmi);

        // Keyboard

        static public native boolean RegisterHotKey(HWND hWnd, int id, int fsModifiers, int vk);

        static public native boolean UnregisterHotKey(HWND hWnd, int id);

        static public native void keybd_event(byte bVk, byte bScan, int dwFlags, Pointer dwExtraInfo);

        // Clipboard

        static public native boolean AddClipboardFormatListener(HWND hWnd);

        static public native boolean OpenClipboard(HWND hWnd);

        static public native boolean CloseClipboard(HWND hWnd);

        static public native boolean EmptyClipboard();

        static public native boolean IsClipboardFormatAvailable(int format);

        static public native Pointer GetClipboardData(int format);

        static public native Pointer SetClipboardData(int format, Pointer hMem);

        static public native Pointer GetClipboardOwner();

        // Misc

        static public native boolean GetCursorPos(POINT point);

        static public native boolean GetGUIThreadInfo(int idThread, GUITHREADINFO lpgui);

        static public native Pointer LoadImage(Pointer hinst, WString name, int type, int xDesired, int yDesired, int load);
    }

    static public class User32_64 {
        static {
            Native.register(NativeLibrary.getInstance("user32", W32APIOptions.DEFAULT_OPTIONS));
        }

        static public native int SetWindowLongPtr(HWND hWnd, int nIndex, Callback procedure);
    }

    static public class Shell32 {
        static {
            Native.register(NativeLibrary.getInstance("shell32", W32APIOptions.DEFAULT_OPTIONS));
        }

        static public final int NIM_ADD = 0;
        static public final int NIM_DELETE = 2;

        //

        static public native boolean Shell_NotifyIcon(int dwMessage, NOTIFYICONDATA lpdata);

        static public native int DragQueryFile(Pointer hDrop, int iFile, char[] lpszFile, int cch);
    }

    static public class MSG extends Structure {
        public HWND hWnd;
        public int message;
        public Parameter wParam;
        public Parameter lParam;
        public int time;
        public int x;
        public int y;

        protected List getFieldOrder() {
            return Arrays.asList("hWnd", "message", "wParam", "lParam", "time", "x", "y");
        }
    }

    static public class HMENU extends HANDLE {
        public HMENU() {
        }

        public HMENU(Pointer p) {
            super(p);
        }
    }

    static public class HWND extends HANDLE {
        public HWND() {
        }

        public HWND(Pointer p) {
            super(p);
        }
    }

    static public class Parameter extends IntegerType {
        public Parameter() {
            this(0);
        }

        public Parameter(long value) {
            super(Pointer.SIZE, value);
        }
    }

    static public class GUITHREADINFO extends Structure {
        public int cbSize = size();
        public int flags;
        public HWND hwndActive;
        public HWND hwndFocus;
        public HWND hwndCapture;
        public HWND hwndMenuOwner;
        public HWND hwndMoveSize;
        public HWND hwndCaret;
        public RECT rcCaret;

        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[]{"cbSize", "flags", "hwndActive", "hwndFocus", "hwndCapture", "hwndMenuOwner",
                    "hwndMoveSize", "hwndCaret", "rcCaret"});
        }
    }

    static public class RECT extends Structure {
        public int left;
        public int top;
        public int right;
        public int bottom;

        protected List getFieldOrder() {
            return Arrays.asList(new String[]{"left", "top", "right", "bottom"});
        }
    }

    static public class POINT extends Structure {
        public int x, y;

        protected List getFieldOrder() {
            return Arrays.asList(new String[]{"x", "y"});
        }
    }

    static public class NOTIFYICONDATA extends Structure {
        static public final int NIF_MESSAGE = 0x1;
        static public final int NIF_ICON = 0x2;
        static public final int NIF_TIP = 0x4;
        static public final int NIF_INFO = 0x10;

        public int cbSize;
        public HWND hWnd;
        public int uID;
        public int uFlags;
        public int uCallbackMessage;
        public Pointer hIcon;
        public char[] szTip = new char[64];
        public int dwState;
        public int dwStateMask;
        public char[] szInfo = new char[256];
        public int union; // {UINT uTimeout; UINT uVersion;};
        public char[] szInfoTitle = new char[64];
        public int dwInfoFlags;
        public int guidItem;
        public Pointer hBalloonIcon;

        {
            cbSize = size();
        }

        public void setTooltip(String s) {
            uFlags |= NIF_TIP;
            System.arraycopy(s.toCharArray(), 0, szTip, 0, Math.min(s.length(), szTip.length));
        }

        protected List getFieldOrder() {
            return Arrays.asList(new String[]{"cbSize", "hWnd", "uID", "uFlags", "uCallbackMessage", "hIcon", "szTip", "dwState",
                    "dwStateMask", "szInfo", "union", "szInfoTitle", "dwInfoFlags", "guidItem", "hBalloonIcon"});
        }
    }

    static public class MONITORINFO extends Structure {
        public int cbSize = size();
        public RECT rcMonitor;
        public RECT rcWork;
        public int dwFlags;

        protected List<String> getFieldOrder() {
            return Arrays.asList("cbSize", "rcMonitor", "rcWork", "dwFlags");
        }
    }
}

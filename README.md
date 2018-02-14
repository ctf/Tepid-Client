# TEPID Client

Client program to bind printers and send jobs to TEPID-server

### Build

You may look at the gradle panel for a list of valid commands.

All output will be located under `[out] = $buildDir/libs`.

You are free to mix and match commands.

Notable commands:

| Command | Description |
| --- | --- |
| `clean` | Delete `[out]` |
| `jar` | Build jar file in `[out]` |
| `tepidWindows` | Build jar and copy all files (including exe and ini) to $buildDir/libs` |
| `tepidLinux` | Build jar and copy all libs to `[out]` |
| `commandLinux` | Launch Tepid (assuming files are already built) |

### Run

Windows: `cd [out]; tepid.exe`
<br/>The error `com.jacob.com.ComFailException: Invoke of: Put_` when launching `WindowsPrinterMgmt` is usually fixed by installing "Xerox Global Print Driver PS" from the Xerox website (sometimes you may need to add a fake printer to force Windows to install the driver before it works)

Linux: `java -classpath "[out]/*" ca.mcgill.science.tepid.client.Main`
<br/>(This is the same as `commandLinux`)

### Update

* Updating tepid involves changing the java code.
* The libs used in gradle are copied automatically to the build file upon jar creation, 
so make sure that the production directory contains the same libraries
* The ini file contains the lib path (`libs`) and the classpath for `Main.java`. 
Make sure those are changed accordingly.


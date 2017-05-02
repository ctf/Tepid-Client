# TEPID Client

Client program to bind printers and send jobs to TEPID-server

### Build

The client is bundled with a jar file, and can be built with a single `buildTepid` command.
All output will be located under `$buildDir/libs`.

Note that if you wish to perform a clean build, you will need `clean build buildTepid`

### Update

* Updating tepid involves changing the java code.
* The libs used in gradle are copied automatically to the build file upon jar creation, 
so make sure that the production directory contains the same libraries
* The ini file contains the lib path (`libs`) and the classpath for `Main.java`. 
Make sure those are changed accordingly.


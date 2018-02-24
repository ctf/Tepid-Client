@echo off
IF [%PRODUCTVERSION%] == [] GOTO:EOF
"%WIX%"\bin\heat dir libs -ag -dr dir_libs -suid -cg libsDir -out libsDir.wxs
"%WIX%"\bin\candle -arch x64 libsDir.wxs
"%WIX%"\bin\candle installer.wxs
"%WIX%"\bin\light -ext WixUIExtension -cultures:en-us installer.wixobj libsDir.wixobj -b libs -out TEPID-%PRODUCTVERSION%.msi
del installer.wixobj libsDir.wixObj libsDir.wxs TEPID-%PRODUCTVERSION%.wixpdb

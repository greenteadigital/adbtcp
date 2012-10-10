Adb Tcp - Summary
-----------------
This Android application restarts the Android Debugging Bridge daemon, setting it to listen on a random tcp port and notifies the user. This allows for wireless shell access over wifi/3g/4g, and is especially useful during the build/install/run/debug cycle when developing for Android. The daemon is restarted in USB mode on exit.


Dependencies
------------
-Requires root access (uid=0).
-The app makes calls to the following external binaries: su, netstat, netcfg, setprop, stop, start. If these commands are not available on your system, or they return unexpected values, behavior is undefined.


Included...
-----------
-The source code for the app is in ./src
-An installable Android .apk is also provided. Unknown sources must be enabled on your device.
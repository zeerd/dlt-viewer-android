# Dlt-Viewer On Android

## Build (Android Studio)

Nothing to say about this. Just follow the hints of android studio itself.

## Build (Linux Terminal)

### Environment
1. Download Android SDK and NDK.
2. Unzip those packages into /opt folder.
3. Install the components.
```(base)
   $ unzip android-ndk-r16b-linux-x86_64.zip -d /opt/
   $ unzip sdk-tools-linux-3859397.zip -d /opt/android-sdk/
   $ /opt/android-sdk/tools/bin/sdkmanager "build-tools;23.0.3" "build-tools;26.0.1" "platforms;android-23" "platform-tools"
```
4. Modify the paths in the build.sh script.

### Build
1. make a key for sign
```(shell)
   $ keytool -genkeypair -validity 36500 -keystore mykey.keystore -keyalg RSA -keysize 2048
```
2. run build.sh


## Usage

There are English and Chinese support for this application.

| Screen shot                 | Description                              |
| --------------------------- | ---------------------------------------- |
| ![home](doc/home.png =100x)       | The save button at the top of the application used to save the logs shown on the screen. <br/> The save check-box at the bottom of the application used to save the dlt-format log into the download folder. The filter will not work for this function. Means all dlt logs received will be stored. |
| ![menu](doc/menu.png =100x)       | Touch Menu Button to open the menu.   |
| ![filter](doc/filter.png =100x)   | User can add a group of filters. Users can save them in different files and reloaded them anytime. |
| ![control](doc/control.png =100x) | Users could set log level with this function. <br/> Users could send inject message.  <br/> Users could use this function to jump to a given line on the home screen. |
| ![search](doc/search.png =100x)   | Users could use a keyword to search in the payload on the home screen. This keyword is not case sensitive.  <br/> Long press a search result will jump to the position of the main screen. |

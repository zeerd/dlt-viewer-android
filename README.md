# Dlt-Viewer On Android

## Build
I am not good at the android, and don't know how to use the android-studio.
So I use some special way to build this project.

### Environment
1. Download Android SDK and NDK.
2. Unzip those packages into /opt folder.
3. Install the componets.
```(base)
   $ unzip android-ndk-r16b-linux-x86_64.zip -d /opt/
   $ unzip sdk-tools-linux-3859397.zip -d /opt/android-sdk/
   $ /opt/android-sdk/tools/bin/sdkmanager "build-tools;23.0.3" "build-tools;26.0.1" "platforms;android-23" "platform-tools"
```
4. Modify the paths in the build.sh script.

### Build
1. make a key for sign
```(shell)
   $ keytool -genkeypair -validity 365 -keystore mykey.keystore -keyalg RSA -keysize 2048
```
2. run build.sh

## Refrence:

[How to make Android apps without IDE from command line](https://medium.com/@authmane512/how-to-build-an-apk-from-command-line-without-ide-7260e1e22676)
[Building an Android Command-Line Application Using the NDK Build Tools](https://software.intel.com/en-us/articles/building-an-android-command-line-application-using-the-ndk-build-tools)
[Find the files in jni-folder from dlt-daemon 2.17.0](https://github.com/GENIVI/dlt-daemon)
[Framework comes from google npk sample](https://github.com/googlesamples/android-ndk/tree/master/hello-jniCallback)
[How to add rows dynamically into table layout](https://stackoverflow.com/questions/5183968/how-to-add-rows-dynamically-into-table-layout)
[Scroll to last line of TableLayout within a ScrollView](https://stackoverflow.com/questions/3087877/scroll-to-last-line-of-tablelayout-within-a-scrollview)
[How can I create a table with borders in Android?](https://stackoverflow.com/questions/2108456/how-can-i-create-a-table-with-borders-in-android)

#!/bin/bash

set -e

AAPT="/opt/android-sdk/build-tools/23.0.3/aapt"
DX="/opt/android-sdk/build-tools/23.0.3/dx"
ZIPALIGN="/opt/android-sdk/build-tools/23.0.3/zipalign"
APKSIGNER="/opt/android-sdk/build-tools/26.0.1/apksigner" # /!\ version 26
PLATFORM="/opt/android-sdk/platforms/android-23/android.jar"

echo "Cleaning..."
rm -rf obj/*
rm -rf src/com/zeerd/dltviewer/R.java

echo "Generating R.java file..."
install -d src/com/zeerd/dltviewer/
$AAPT package -f -m -J src -M app/src/main/AndroidManifest.xml -S app/src/main/res/ -I $PLATFORM
mv src/com/zeerd/dltviewer/R.java app/src/main/java/com/zeerd/dltviewer/R.java

echo "Compiling..."
install -d obj
JAVAC="javac -d obj -classpath app/src/main/java -bootclasspath $PLATFORM -source 1.7 -target 1.7 -Xlint:deprecation"
$JAVAC app/src/main/java/com/zeerd/dltviewer/FilterActivity.java
$JAVAC app/src/main/java/com/zeerd/dltviewer/CustomExceptionHandler.java
$JAVAC app/src/main/java/com/zeerd/dltviewer/SearchActivity.java
$JAVAC app/src/main/java/com/zeerd/dltviewer/SettingActivity.java
$JAVAC app/src/main/java/com/zeerd/dltviewer/HelpActivity.java
$JAVAC app/src/main/java/com/zeerd/dltviewer/ControlActivity.java
$JAVAC app/src/main/java/com/zeerd/dltviewer/LogRow.java
$JAVAC app/src/main/java/com/zeerd/dltviewer/LogTableAdapter.java
$JAVAC app/src/main/java/com/zeerd/dltviewer/MainActivity.java
$JAVAC app/src/main/java/com/zeerd/dltviewer/R.java

echo "Translating in Dalvik bytecode..."
$DX --dex --output=classes.dex obj

echo "Compiling Dlt ..."
export NDK_PROJECT_PATH=app/src/main/
rm -rf lib
/opt/android-ndk-r16b/ndk-build
mv app/src/main/libs lib

echo "Making APK..."
install -d bin
$AAPT package -f -m -F bin/dlt-viewer.unaligned.apk -M app/src/main/AndroidManifest.xml -S app/src/main/res/ -I $PLATFORM
$AAPT add bin/dlt-viewer.unaligned.apk classes.dex
$AAPT add bin/dlt-viewer.unaligned.apk lib/arm64-v8a/libdlt-jnicallback.so
$AAPT add bin/dlt-viewer.unaligned.apk lib/armeabi-v7a/libdlt-jnicallback.so
$AAPT add bin/dlt-viewer.unaligned.apk lib/x86/libdlt-jnicallback.so
$AAPT add bin/dlt-viewer.unaligned.apk lib/x86_64/libdlt-jnicallback.so

#echo "Aligning and signing APK..."
$APKSIGNER sign --ks mykey.keystore bin/dlt-viewer.unaligned.apk
$ZIPALIGN -f 4 bin/dlt-viewer.unaligned.apk bin/dlt-viewer.apk

if [ "$1" == "test" ]; then
	echo "Launching..."
	adb install -r bin/dlt-viewer.unaligned.apk
	adb shell am start -n com.zeerd.dltviewer/.MainActivity
fi

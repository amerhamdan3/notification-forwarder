#!/bin/bash
set -e

# ---------------------------------------------------------------------------
# Portable build script for SMS Forwarder.
#
# Requirements:
#   - A JDK 17 (set JAVA_HOME or have `javac` on PATH)
#   - An Android SDK with build-tools 33.0.2 and platform android-33
#     (set ANDROID_HOME / ANDROID_SDK_ROOT, or the script tries common paths)
#
# It generates a throwaway debug keystore on first run so the APK can be
# installed via `adb install` / sideloading. This debug key is NOT suitable
# for publishing to an app store.
# ---------------------------------------------------------------------------

APP_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$APP_DIR/build"

# Resolve Android SDK
ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$ANDROID_HOME" ]; then
    for candidate in "$HOME/Android/Sdk" "$HOME/Library/Android/sdk" "$HOME/tools/android-sdk"; do
        if [ -d "$candidate" ]; then ANDROID_HOME="$candidate"; break; fi
    done
fi
if [ -z "$ANDROID_HOME" ] || [ ! -d "$ANDROID_HOME" ]; then
    echo "ERROR: Android SDK not found. Set ANDROID_HOME to your SDK path." >&2
    exit 1
fi

BUILD_TOOLS="$ANDROID_HOME/build-tools/33.0.2"
PLATFORM="$ANDROID_HOME/platforms/android-33/android.jar"
KEYSTORE="$APP_DIR/debug.keystore"

if [ -n "$JAVA_HOME" ]; then
    export PATH="$JAVA_HOME/bin:$PATH"
fi
export PATH="$BUILD_TOOLS:$PATH"

echo "=== Building SMS Forwarder APK ==="
echo "Android SDK: $ANDROID_HOME"

# Generate a debug keystore if one does not already exist
if [ ! -f "$KEYSTORE" ]; then
    echo "Generating debug keystore..."
    keytool -genkeypair \
        -keystore "$KEYSTORE" \
        -alias debug \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -storepass android -keypass android \
        -dname "CN=SMS Forwarder Debug, O=Open Source, C=US"
fi

# Clean
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/gen" "$BUILD_DIR/obj" "$BUILD_DIR/apk"

# Step 1: Generate R.java from resources
echo "Step 1: Generating R.java..."
aapt2 compile --dir "$APP_DIR/res" -o "$BUILD_DIR/resources.zip"
aapt2 link -o "$BUILD_DIR/apk/app-unsigned.apk" \
    -I "$PLATFORM" \
    --manifest "$APP_DIR/AndroidManifest.xml" \
    --java "$BUILD_DIR/gen" \
    "$BUILD_DIR/resources.zip" \
    --auto-add-overlay

# Step 2: Compile Java
echo "Step 2: Compiling Java..."
javac -source 11 -target 11 \
    -classpath "$PLATFORM" \
    -d "$BUILD_DIR/obj" \
    -sourcepath "$APP_DIR/src:$BUILD_DIR/gen" \
    "$BUILD_DIR/gen/com/smsforwarder/R.java" \
    "$APP_DIR/src/com/smsforwarder/TelegramSender.java" \
    "$APP_DIR/src/com/smsforwarder/SmsReceiver.java" \
    "$APP_DIR/src/com/smsforwarder/NotificationListener.java" \
    "$APP_DIR/src/com/smsforwarder/CallReceiver.java" \
    "$APP_DIR/src/com/smsforwarder/BootReceiver.java" \
    "$APP_DIR/src/com/smsforwarder/ForwarderService.java" \
    "$APP_DIR/src/com/smsforwarder/MainActivity.java"

# Step 3: Convert to DEX
echo "Step 3: Creating DEX..."
d8 --release \
    --lib "$PLATFORM" \
    --output "$BUILD_DIR" \
    $(find "$BUILD_DIR/obj" -name "*.class")

# Step 4: Add DEX to APK
echo "Step 4: Packaging APK..."
cd "$BUILD_DIR"
cp apk/app-unsigned.apk app.apk
zip -j app.apk classes.dex

# Step 5: Align
echo "Step 5: Aligning APK..."
zipalign -f 4 app.apk app-aligned.apk

# Step 6: Sign
echo "Step 6: Signing APK..."
apksigner sign \
    --ks "$KEYSTORE" \
    --ks-key-alias debug \
    --ks-pass pass:android \
    --key-pass pass:android \
    --out "$APP_DIR/SmsForwarder.apk" \
    app-aligned.apk

echo ""
echo "=== BUILD SUCCESS ==="
echo "APK: $APP_DIR/SmsForwarder.apk"
ls -lh "$APP_DIR/SmsForwarder.apk"

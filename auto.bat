java -jar key/signapk.jar key/platform.x509.pem key/platform.pk8 mobile/build/outputs/apk/debug/mobile-debug.apk appproxy.apk

adb shell remount.sh

adb push appproxy.apk /system/priv-app/

adb shell rm -rf /data/data/com.github.appproxy
adb shell sync


:: adb shell pm uninstall com.github.appproxy

:: adb push appproxy.apk /data/local/tmp/com.github.appproxy

:: adb shell pm install -t -r "/data/local/tmp/com.github.appproxy"

:: adb shell am start -n "com.github.appproxy/com.github.appproxy.VpnRequestActivity"
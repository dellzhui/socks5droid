java -jar key/signapk.jar key/platform.x509.pem key/platform.pk8 mobile/build/outputs/apk/debug/mobile-debug.apk appproxy.apk

adb shell pm uninstall com.github.appproxy

adb push appproxy.apk /data/local/tmp/com.github.appproxy

adb shell pm install -t -r "/data/local/tmp/com.github.appproxy"

:: adb shell am start -n "com.github.appproxy/com.github.appproxy.MainActivity"
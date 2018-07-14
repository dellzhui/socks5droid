java -jar key/signapk.jar key/platform.x509.pem key/platform.pk8 mobile/build/outputs/apk/release/mobile-armeabi-v7a-release-unsigned.apk appproxy.apk

::adb shell pm uninstall com.github.appproxy

::adb push out.apk /data/local/tmp/com.github.appproxy

::adb shell pm install -t -r "/data/local/tmp/com.github.appproxy"

::adb shell am start -n "com.github.appproxy/com.github.appproxy.MainActivity" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
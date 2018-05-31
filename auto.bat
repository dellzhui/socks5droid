java -jar key/signapk.jar key/platform.x509.pem key/platform.pk8 mobile/build/outputs/apk/debug/mobile-armeabi-v7a-debug.apk mobile-armeabi-v7a_plat.apk

::adb shell pm uninstall com.github.appproxy

::adb push out.apk /data/local/tmp/com.github.appproxy

::adb shell pm install -t -r "/data/local/tmp/com.github.appproxy"

::adb shell am start -n "com.github.appproxy/com.github.appproxy.MainActivity" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
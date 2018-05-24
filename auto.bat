java -jar key/signapk.jar key/platform.x509.pem key/platform.pk8 mobile/build/outputs/apk/debug/mobile-armeabi-v7a-debug.apk out.apk

adb shell pm uninstall com.github.shadowsocks

adb push out.apk /data/local/tmp/com.github.shadowsocks

adb shell pm install -t -r "/data/local/tmp/com.github.shadowsocks"

adb shell am start -n "com.github.shadowsocks/com.github.shadowsocks.MainActivity" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
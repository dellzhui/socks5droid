package com.github.appproxy.bg;

import android.app.ActivityManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class GetIConnectivityManager {
    private final static String TAG = "AppProxyRe";

    public boolean getInConnectivityManager(Context context, boolean enable){
        ConnectivityManager conMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Class<?> conMgrClass = null; // ConnectivityManager类
        Field iConMgrField = null; // ConnectivityManager类中的字段
        Object iConMgr = null; // IConnectivityManager类的引用
        Class<?> iConMgrClass = null; // IConnectivityManager类
        Method setPrepareVpn,setVpnPackageAuthorization;
        Method[] methods;

        try {
            // 取得ConnectivityManager类
            conMgrClass = Class.forName(conMgr.getClass().getName());
            // 取得ConnectivityManager类中的对象mService
            iConMgrField = conMgrClass.getDeclaredField("mService");
            // 设置mService可访问
            iConMgrField.setAccessible(true);
            // 取得mService的实例化类IConnectivityManager
            iConMgr = iConMgrField.get(conMgr);
            // 取得IConnectivityManager类
            iConMgrClass = Class.forName(iConMgr.getClass().getName());
            methods = iConMgrClass.getMethods();
            for (Method m: methods){
                if(m.getName().equals("prepareVpn")) {
                    m.setAccessible(true);
                    m.invoke(iConMgr,null,context.getApplicationContext().getPackageName());
                }
            }
//            setPrepareVpn = iConMgrClass.getDeclaredMethod("prepareVpn",Boolean.TYPE);
//            setVpnPackageAuthorization = iConMgrClass.getDeclaredMethod("setVpnPackageAuthorization",Boolean.TYPE);
//            setPrepareVpn.setAccessible(true);
//            setVpnPackageAuthorization.setAccessible(true);
//            setPrepareVpn.invoke(iConMgrClass,null,context.getApplicationContext().getPackageName());
//            setVpnPackageAuthorization.invoke(iConMgrClass,enable);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean openVPNAuth(String vpnPackage) {
        try {
            /*
             * 应用需要有android.permission.CONTROL_VPN权限，但是这个权限只授权给系统级应用，第三方应用无法拿到此权限，操作接口会有权限异常。
             * 另一方面，如果是系统级应用也就没必要通过反射的方式去操作。
             */
            Method prepareVpnMethod, setVpnPackageAuthMethod;
            Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
            Method getServiceMethod = serviceManagerClass.getMethod("getService", new Class[]{String.class});
            IBinder serviceManager = (IBinder)getServiceMethod.invoke(null, new Object[]{Context.CONNECTIVITY_SERVICE});
            Class<?> stubClass = Class.forName("android.net.IConnectivityManager$Stub");
            Method asInterfaceMethod = stubClass.getMethod("asInterface", new Class[]{IBinder.class});
            Object IConnectivityManager = asInterfaceMethod.invoke(null, serviceManager);
            Class<?> userHandleClass = Class.forName("android.os.UserHandle");
            Method myUserIdMethod = userHandleClass.getMethod("myUserId");
            myUserIdMethod.setAccessible(true);
            int userId = (int)myUserIdMethod.invoke(null, null);
            Class<?> IConnectivityManagerClass = Class.forName(IConnectivityManager.getClass().getName());
            // Android 2.3 没有VPN功能，因缺少3.0的在线源码资源，无法确认3.0版本是否具备VPN功能，4.0以下的版本一起屏蔽掉。
            if (Build.VERSION.SDK_INT < 14){
                return false;
                // 4.0-4.4版本
            } else if (Build.VERSION.SDK_INT >= 14 && Build.VERSION.SDK_INT < 21){
                prepareVpnMethod = IConnectivityManagerClass.getMethod("prepareVpn", String.class, String.class);
                prepareVpnMethod.setAccessible(true);
                boolean prepareSuccess = (boolean)prepareVpnMethod.invoke(IConnectivityManager, null, "com.github.appproxy");
                return prepareSuccess;
                // 5.0-5.1版本
            } else if (Build.VERSION.SDK_INT >= 21 && Build.VERSION.SDK_INT < 23){
                prepareVpnMethod = IConnectivityManagerClass.getMethod("prepareVpn", String.class, String.class);
                prepareVpnMethod.setAccessible(true);
                setVpnPackageAuthMethod = IConnectivityManagerClass.getMethod("setVpnPackageAuthorization", boolean.class);
                setVpnPackageAuthMethod.setAccessible(true);
                boolean prepareSuccess = (boolean)prepareVpnMethod.invoke(IConnectivityManager, null, vpnPackage);
                if(prepareSuccess){
                    setVpnPackageAuthMethod.invoke(IConnectivityManager, true);
                    return true;
                }
                // 6.0 开始之后的版本
            } else if (Build.VERSION.SDK_INT >= 23){
                prepareVpnMethod = IConnectivityManagerClass.getMethod("prepareVpn", String.class, String.class, int.class);
                prepareVpnMethod.setAccessible(true);
                setVpnPackageAuthMethod = IConnectivityManagerClass.getMethod("setVpnPackageAuthorization", String.class, int.class, boolean.class);
                setVpnPackageAuthMethod.setAccessible(true);
                boolean prepareSuccess = (boolean)prepareVpnMethod.invoke(IConnectivityManager, null, vpnPackage, userId);
                if(prepareSuccess){
                    setVpnPackageAuthMethod.invoke(IConnectivityManager, vpnPackage, userId, true);
                    return true;
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return false;
    }

}

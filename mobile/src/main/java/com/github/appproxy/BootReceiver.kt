/*******************************************************************************
 *                                                                             *
 *  Copyright (C) 2017 by Max Lv <max.c.lv@gmail.com>                          *
 *  Copyright (C) 2017 by Mygod Studio <contact-shadowsocks-android@mygod.be>  *
 *                                                                             *
 *  This program is free software: you can redistribute it and/or modify       *
 *  it under the terms of the GNU General Public License as published by       *
 *  the Free Software Foundation, either version 3 of the License, or          *
 *  (at your option) any later version.                                        *
 *                                                                             *
 *  This program is distributed in the hope that it will be useful,            *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 *  GNU General Public License for more details.                               *
 *                                                                             *
 *  You should have received a copy of the GNU General Public License          *
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.       *
 *                                                                             *
 *******************************************************************************/

package com.github.appproxy

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

import com.github.appproxy.App.Companion.app
import com.github.appproxy.preference.DataStore
import java.io.IOException
import com.inspur.reflect.LocalReflect
import android.net.NetworkInfo
import android.net.ConnectivityManager
import android.R.attr.action
import android.os.AsyncTask
import com.inspur.reflect.ProxyProfileInfo
import java.io.File
import khttp.get




class BootReceiver : BroadcastReceiver() {
    companion object {
		private const val TAG = "AppProxyBootReceiver"
        private const val MONITOR_TIMEOUT_MS : Long = (1000 * 2 * 60 * 60)
        // local test
        //private const val MONITOR_RQUEST_URL = "http://192.168.52.201:9002/proxy.json"
        // test
        //private const val MONITOR_RQUEST_URL = "http://172.16.189.85:8080/msis/getDynamicConfig?type=terminalProxy&authKey=d34173bf2fe34f0249247be0676e9b6d"
        // offical
        private const val MONITOR_RQUEST_URL = "http://api.ott.yun.gehua.net.cn:8080/msis/getDynamicConfig?type=terminalProxy&authKey=ca477cc0234d0d8c11b80a7af8b4f804"

        private val componentName by lazy { ComponentName(app, BootReceiver::class.java) }
        fun enabled_local_set(value: Boolean) {
            Log.e(TAG, "local_set value to " + value)
            app.packageManager.setComponentEnabledSetting(componentName,
                    if (value) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    else PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }
        var enabled: Boolean
            get() = app.packageManager.getComponentEnabledSetting(componentName) ==
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            /*set(value) = app.packageManager.setComponentEnabledSetting(componentName,
                    if (value) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
						else PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)*/
            set(value) = enabled_local_set(value)
    }

    private fun init_database(context: Context) {
        try {
            var index: Int = 0
            val timeout: Int = 10
            while(index++ < timeout) {
                val config_db_file = context.getDatabasePath("config.db")
                if (config_db_file.exists()) {
                    val local_reflect = LocalReflect()
                    Log.e(TAG, "database dir is ready")
                    if (context.getDatabasePath("profile.db").exists()) {
                        Log.e(TAG, "database is already created")
                        val info = local_reflect.GetProxyProfileInfoFromDatabase(context.getDatabasePath("profile.db").path)
                        if(info != null) {
                            Log.d(TAG, "check database succeed")
                            break
                        }
                        Log.e(TAG, "check database failed")
                    }
                    Log.e(TAG, "we will copy progile.db from other")
                    Log.e(TAG, context.applicationInfo.nativeLibraryDir)
                    if(!local_reflect.fileCopy(context.applicationInfo.nativeLibraryDir + "/profile.so", config_db_file.getParent() + "/profile.db")) {
                        if(!local_reflect.fileCopy(context.applicationInfo.nativeLibraryDir + "/libprofile.so", config_db_file.getParent() + "/profile.db")) {
                            if(!local_reflect.fileCopy("/system/priv-app/appproxy/lib/arm/profile.so", config_db_file.getParent() + "/profile.db")) {
                                local_reflect.fileCopy("/system/lib/libprofile.so", config_db_file.getParent() + "/profile.db")
                            }
                        }
                    }
                    if(!local_reflect.fileCopy(context.applicationInfo.nativeLibraryDir + "/config.so", config_db_file.getParent() + "/config.db")) {
                        if(!local_reflect.fileCopy(context.applicationInfo.nativeLibraryDir + "/libconfig.so", config_db_file.getParent() + "/config.db")) {
                            if(!local_reflect.fileCopy("/system/priv-app/appproxy/lib/arm/config.so", config_db_file.getParent() + "/config.db")) {
                                local_reflect.fileCopy("/system/lib/libconfig.so", config_db_file.getParent() + "/config.db")
                            }
                        }
                    }
                    break
                }
                Log.e(TAG, "database dir is not ready")
                Thread.sleep(1000)
            }
        } catch (ex: Exception) {
            Log.e(TAG, "init database failed")
            ex.printStackTrace()
        }
    }

    private inner class MonitorTaskClass : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String): String {
            monitor_task_loop()
            return "Executed"
        }
        override fun onPreExecute() {}
        override fun onProgressUpdate(vararg values: Void) {}
    }

    private fun monitor_task_loop() {
        Thread.sleep(1000 * 10)
        Log.e(TAG, "monitor task started")
        while(true) {
            Log.e(TAG, "monitor check")
            perform_monitor_task()
            Thread.sleep(MONITOR_TIMEOUT_MS)
        }
    }

    private fun perform_monitor_task() {
        val filePath = app.deviceContext.filesDir.path + "/proxy.json"
        val local_reflect = LocalReflect()
        var info_old: ProxyProfileInfo = ProxyProfileInfo()
        val info_new: ProxyProfileInfo

        try {
            Log.i(TAG, "input filePath is $filePath")
            val file_old = File(filePath)
            if(file_old.exists()) {
                info_old = local_reflect.GetProxyProfileInfoFromJson(file_old.readText())
            } else {
                Log.e(TAG, "${filePath} not exists")
            }
        } catch (ex: Exception) {
            Log.e(TAG, "getProxyProfileInfoFromFile failed")
            ex.printStackTrace()
        }

        try {
            Log.i(TAG, "url is " + MONITOR_RQUEST_URL)
            val json_new = get(MONITOR_RQUEST_URL).text
            Log.i(TAG, "get json_new is [$json_new]")
            info_new = local_reflect.GetProxyProfileInfoFromJson(json_new)

            if(local_reflect.isNewProxyProfileInfoAccept(info_old, info_new)) {
                Log.e(TAG, "we will update profile")
                val file_new = File(filePath)
                file_new.writeText(json_new)
                Log.e(TAG, "we will restart service")
                try {
                    app.stopService()
                    Thread.sleep(1000 * 5)
                } catch (ex: Exception) {
                    Log.e(TAG, "stopService failed")
                    ex.printStackTrace()
                }
                app.startService()
            } else {
                Log.e(TAG, "no need to update profile")
            }
        } catch (ex: Exception) {
            Log.e(TAG, "perform_monitor_task failed")
            ex.printStackTrace()
        }
    }

    private fun start_client_task(context : Context) {
        if(System.getProperty("proxy.monitor.boot") == "received" && System.getProperty("proxy.monitor.net_connect") == "received") {
            if(check_env(context)) {
                app.startService()
            }
            val proxy_monitor_status = System.getProperty("proxy.monitor.status")
            Log.e(TAG, "proxy_monitor_status is " + proxy_monitor_status)
            if(proxy_monitor_status == "running") {
                Log.e(TAG, "already started")
                return
            }
            System.setProperty("proxy.monitor.status", "running")
            Log.e(TAG, "we will start monitor task")
            MonitorTaskClass().execute("")
            return
        }
        Log.e(TAG, "we should start only after booting and network connected")
    }

    private fun update_app_list(package_name : String) {
        try {
            Log.d(TAG, "input package_name is ${package_name}")
            if("".equals(package_name)) {
                Log.e(TAG, "input NULL")
                return
            }
            val local_reflect = LocalReflect()
            val json = File(app.deviceContext.filesDir.path + "/proxy.json").readText()
            val info = local_reflect.GetProxyProfileInfoFromJson(json)
            if(info.getAppList().indexOf(package_name) != -1) {
                Log.d(TAG, "find ${package_name} in stored applist succeed, we will reload service")
                app.reloadService()
                return
            } else {
                Log.d(TAG, "can not find ${package_name} in stored applist, no need to update")
            }
        } catch (ex: Exception) {
            Log.e(TAG, "update_app_list failed")
            ex.printStackTrace()
        }

        try {
            val profile = app.currentProfile
            if(profile != null) {
                if(profile.individual.indexOf(package_name) != -1) {
                    Log.d(TAG, "find ${package_name} in currentProfile succeed, we will reload service")
                    app.reloadService()
                    return
                } else {
                    Log.d(TAG, "can not find ${package_name} in currentProfile, no need to update")
                }
            }
        } catch (ex: Exception) {
            Log.e(TAG, "update_app_list failed")
            ex.printStackTrace()
        }
    }

    private fun check_env(context : Context) : Boolean{
        val local_reflect = LocalReflect()
        val file = File(app.deviceContext.filesDir.path + "/proxy.json")
        if(file.exists()) {
            val info = local_reflect.GetProxyProfileInfoFromJson(file.readText())
            if(info != null) {
                if(!LocalReflect.isHostNameAvailable(info.serverAddr)) return false
                return true
            }
        }

        Log.e(TAG, "check from proxy.json failed")

        val info =local_reflect.GetProxyProfileInfoFromDatabase(context.getDatabasePath("profile.db").path)
        if(info == null) return false
        if(!LocalReflect.isHostNameAvailable(info.serverAddr)) return false
        return true
    }

    private fun handle_intent(context: Context, intent: Intent) {
        val action = intent.action

        if(action == Intent.ACTION_BOOT_COMPLETED) {
            init_database(context)
            //app.startService()
            System.setProperty("proxy.monitor.boot", "received")
            start_client_task(context)
            return
        }

        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            Log.d(TAG, "get CONNECTIVITY_ACTION")
            val netType = intent.getIntExtra(ConnectivityManager.EXTRA_NETWORK_TYPE, 9)
            Log.d(TAG, "netType_str is $netType")

            if(netType != ConnectivityManager.TYPE_ETHERNET) {
                Log.e(TAG, "not Ethernet")
                return
            }

            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            Log.d(TAG, "mConnectivityManager is $connectivityManager")

            val mInfo = connectivityManager.activeNetworkInfo
            Log.d(TAG, "mInfo is $mInfo")
            if (mInfo != null && mInfo.isAvailable && mInfo.type == netType) {
                val state = mInfo.detailedState
                Log.d(TAG, "state is $state")
                if(state == NetworkInfo.DetailedState.CONNECTED) {
                    System.setProperty("proxy.monitor.net_connect", "received")
                    start_client_task(context)
                }
            } else {
                Log.d(TAG, "no avaliable network")
            }
            return
        }

        if(action.equals(Intent.ACTION_PACKAGE_ADDED)) {
            val packageName = intent.data.schemeSpecificPart
            Log.d(TAG, "add new package is [" + packageName + "]")
            update_app_list(packageName)
            return
        }

        if(action.equals(Intent.ACTION_PACKAGE_CHANGED)) {
            val packageName = intent.data.schemeSpecificPart
            Log.d(TAG, "change new package is [" + packageName + "]")
            update_app_list(packageName)
            return
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.e(TAG, "intent.action is " + intent.action)
        /*val locked = when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> false
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> true // constant will be folded so no need to do version checks
            else -> return
        }
        Log.e(TAG, "DataStore.directBootAware is " + DataStore.directBootAware + ", locked is " + locked)
        if (DataStore.directBootAware == locked) app.startService()*/
        handle_intent(context, intent)
    }
}

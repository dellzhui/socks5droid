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

package com.github.shadowsocks

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

import com.github.shadowsocks.App.Companion.app
import com.github.shadowsocks.preference.DataStore
import java.io.IOException
import com.inspur.reflect.local_reflect
import android.net.NetworkInfo
import android.net.ConnectivityManager
import android.R.attr.action
import android.os.AsyncTask




class BootReceiver : BroadcastReceiver() {
    companion object {
		private const val TAG = "ShadowsocksBootReceiver"
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
            Log.e(TAG, "path is " + context.getFilesDir().getPath())
            while(index++ < timeout) {
                val config_db_file = context.getDatabasePath("config.db")
                if (config_db_file.exists()) {
                    Log.e(TAG, "database dir is ready")
                    if (context.getDatabasePath("profile.db").exists()) {
                        Log.e(TAG, "profile.db is already created")
                        break
                    }
                    Log.e(TAG, "we will copy progile.db from other")
                    val a: local_reflect = local_reflect()
                    a.fileCopy("/system/adb/databases/profile.db", config_db_file.getParent() + "/profile.db")
                    a.fileCopy("/system/adb/databases/config.db", config_db_file.getParent() + "/config.db")
                    break
                }
                Log.e(TAG, "database dir is not ready")
                Thread.sleep(1000)
            }
        } catch (ex: IOException) {
            Log.e(TAG, "init database failed")
            ex.printStackTrace()
        }
    }

    private inner class MonitorTaskClass : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String): String {
            perform_monitor_task()
            return "Executed"
        }
        override fun onPreExecute() {}
        override fun onProgressUpdate(vararg values: Void) {}
    }

    private fun perform_monitor_task() {
        Log.e(TAG, "monitor task started")
        System.setProperty("proxy.monitor.status", "running")
        Thread.sleep(1000 * 10)
        Log.e(TAG, "we will reload service")
        app.reloadService()
        Thread.sleep(1000 * 1000)
        //System.setProperty("proxy.monitor.status", "stoped")
        //Log.e(TAG, "monitor task end")
    }

    private fun start_client_task() {
        val proxy_monitor_status = System.getProperty("proxy.monitor.status")
        Log.e(TAG, "proxy_monitor_status is " + proxy_monitor_status)

        if(proxy_monitor_status == "running") {
            Log.e(TAG, "already started")
            return
        }

        if(System.getProperty("proxy.monitor.boot") == "received" && System.getProperty("proxy.monitor.net_connect") == "received") {
            Log.e(TAG, "we will start monitor task")
            MonitorTaskClass().execute("")
            return
        }
        Log.e(TAG, "we should start only after booting and network connected")
    }

    private fun handle_intent(context: Context, intent: Intent) {
        val action = intent.action

        if(action == Intent.ACTION_BOOT_COMPLETED) {
            init_database(context)
            app.startService()
            System.setProperty("proxy.monitor.boot", "received")
            start_client_task()
            return
        }

        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            Log.d(TAG, "get CONNECTIVITY_ACTION")
            val netType = intent.getIntExtra(ConnectivityManager.EXTRA_NETWORK_TYPE, 9)
            Log.d(TAG, "netType_str is $netType")

            if(netType == ConnectivityManager.TYPE_VPN) {
                Log.e(TAG, "ignore TYPE_VPN")
                return
            }

            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            Log.d(TAG, "mConnectivityManager is $connectivityManager")

            val mInfo = connectivityManager.getNetworkInfo(netType)
            Log.d(TAG, "mInfo is " + mInfo)
            if (mInfo.isAvailable()) {
                val state = mInfo.getDetailedState()
                Log.d(TAG, "state is $state")
                if(state == NetworkInfo.DetailedState.CONNECTED) {
                    System.setProperty("proxy.monitor.net_connect", "received")
                    start_client_task()
                }
            } else {
                Log.d(TAG, "no avaliable network")
            }
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

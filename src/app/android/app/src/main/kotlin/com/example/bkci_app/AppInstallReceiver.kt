package com.example.bkci_app
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.flutter.plugin.common.MethodChannel

class AppInstallReceiver(val methodChannel: MethodChannel) : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            var method: String = ""
            val packageName: String? = intent.data?.encodedSchemeSpecificPart

            if (action.equals(Intent.ACTION_PACKAGE_ADDED)) {
                println(packageName + "app installed")
                method = "bkPackageInstalled"
            } else if (action.equals(Intent.ACTION_PACKAGE_REPLACED)) {
                println(packageName + "app UPDATED")
                method = "bkPackageUpdated"
            } else if (action.equals(Intent.ACTION_PACKAGE_REMOVED)) {
                println(packageName + "app uninstalled")
                method = "bkPackageRemove"
            }
            println(method + ":" + packageName)
            if (methodChannel != null) {
                methodChannel.invokeMethod(method, packageName, null)
            }
    }
}

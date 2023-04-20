package com.devopsapp

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.content.FileProvider
import com.example.bkci_app.AppInstallReceiver
import com.tencent.itlogin.component.ITLoginAuthListener
import com.tencent.itlogin.component.ITLoginBaseActivityManager
import com.tencent.itlogin.entity.Credential
import com.tencent.itlogin.network.ITLoginError
import com.tencent.itlogin.sdk.ITLoginListener
import com.tencent.itlogin.sdk.ITLoginSDK
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.io.File

class MainActivity : FlutterActivity(), ITLoginAuthListener, ITLoginListener {

    private val channel = "flutter.itlogin"
    private var initUniLink: String? = null
    lateinit var methodChannel: MethodChannel
    lateinit var channelResult: MethodChannel.Result
    lateinit var appInstallReceiver: AppInstallReceiver
    lateinit var itloginInstance: ITLoginBaseActivityManager
    private var itloginInited: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appInstallReceiver = AppInstallReceiver(methodChannel)
        val filter = IntentFilter()
        filter.addDataScheme("package")
        filter.addAction(Intent.ACTION_PACKAGE_ADDED)
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED)

        this.registerReceiver(appInstallReceiver, filter)
        initUniLink = intent?.data?.path
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action === Intent.ACTION_VIEW && methodChannel != null) {
            val action: String? = intent?.action
            val data: String? = intent?.data?.path
            methodChannel.invokeMethod("uni_link", data, null)
        }
    }
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {

        super.configureFlutterEngine(flutterEngine)
        println("configureFlutterEngine main flutter")
        methodChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channel)
        methodChannel.setMethodCallHandler {
            // Note: this method is invoked on the main thread.
            call, result ->
            channelResult = result
            if (call.method == "checkITLogin") {
                checkITLogin()
                result.success("success")
            } else if (call.method == "logout") {
                ITLoginBaseActivityManager.getInstance().logout(this)
            } else if (call.method == "getCkey") {
                val credential: Credential = ITLoginSDK.getLoginInfo(this.applicationContext)
                println("println(credential.key);")
                println(credential.key)
                result.success(credential.key)
            } else if (call.method == "installApk") {
                installApk(call.arguments())
                result.success("success")
            } else if (call.method == "initITLogin") {
                initITLoginSDK()
                // result.success("success");
            } else if (call.method == "getInitUniLink") {
                if (initUniLink != null) {
                    result.success(initUniLink)
                    initUniLink = null
                }
            } else {
                result.notImplemented()
            }
        }
        println("intent?.data?.path")
        println(initUniLink)
    }

    fun initITLoginSDK() {
        // println("itlogin on  init");
        if (!itloginInited) {
            // println("itlogin on  inited");
            itloginInstance.onActivityResume(this)
            itloginInited = true
        }
        itloginInstance.logout(this)
    }

    fun checkITLogin() {
        if (this::itloginInstance.isInitialized) {
            itloginInstance.onActivityResume(this)
            itloginInstance.validateLogin(this)
        }
    }

    fun installApk(file: String) {
        try {
            val apkfile = File(file)
            if (!apkfile.exists()) {
                return
            }
            val intent = Intent(Intent.ACTION_VIEW)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { // 7.0+以上版本
                val apkUri: Uri = FileProvider.getUriForFile(
                    this.applicationContext,
                    this.packageName + ".fileprovider",
                    apkfile
                )
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.setDataAndType(Uri.parse("file://$file"), "application/vnd.android.package-archive")
            }
            this.applicationContext.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        // println("itlogin on  resume");
        if (!this::itloginInstance.isInitialized) {
            // println("itlogin on  resumeed");
            ITLoginBaseActivityManager.getInstance().init(this)
            ITLoginSDK.configITLogin(R.style.itloginpushstyle, "蓝盾登录", R.drawable.ic_itlogin_logo, 100)
            itloginInstance = ITLoginBaseActivityManager.getInstance()
            itloginInstance.itLoginAuthListener = this
            itloginInstance.itLoginListener = this
            itloginInstance.onActivityCreate(this)
            itloginInstance.setLogLevel(3.toByte())
        }
    }

    override fun onStop() {
        super.onStop()
        if (this::itloginInstance.isInitialized && itloginInited) {
            itloginInstance.onActivityFinish()
        }
    }

    override fun onPause() {
        super.onPause()
        if (this::itloginInstance.isInitialized && itloginInited) {
            // println("itlogin on  pause");
            itloginInstance.onActivityPause()
            itloginInited = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::itloginInstance.isInitialized && itloginInited) {
            itloginInstance.onActivityFinish()
        }
        unregisterReceiver(appInstallReceiver)
    }

    override fun finish() {
        super.finish()
        if (this::itloginInstance.isInitialized && itloginInited) {
            itloginInstance.onActivityFinish()
        }
    }

    override fun onAuthSuccess() {
//        TODO("Not yet implemented")
        println("auth success")
        handleSuccess()
    }

    fun handleSuccess() {
        val credential: Credential = ITLoginSDK.getLoginInfo(this.applicationContext)
        val sharePrefs: SharedPreferences = this.getSharedPreferences("", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharePrefs.edit()
        editor.putString("flutter.cKey", credential.key)
        editor.commit()
        sendMessageToFlutter(credential.key)
    }

    fun sendMessageToFlutter(cKey: String) {
        if (methodChannel != null) {
            methodChannel.invokeMethod("loginResult", cKey, null)
        }
    }

    override fun onAuthFailure(p0: ITLoginError?) {
//        TODO("Not yet implemented")
        println("auth fail")
    }

    override fun onLoginSuccess() {
//        TODO("Not yet implemented")
        println("login success")
        handleSuccess()
    }

    override fun onLoginFailure(p0: ITLoginError?) {
//        TODO("Not yet implemented")
        println("login failure")
    }

    override fun onFinishLogout(p0: Boolean) {
//        TODO("Not yet implemented")
        println("logout finish")
        channelResult.success("logout success")
    }

    override fun onLoginCancel() {
//        TODO("Not yet implemented")
        println("login cancel")
    }
}

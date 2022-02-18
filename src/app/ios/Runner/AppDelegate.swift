import UIKit
import Flutter
import ITLogin
import flutter_downloader


let kAppKey = "d4699d2782a6edb09a1837cfdc2df110"
let kAppID = "DevOps"

@UIApplicationMain
@objc class AppDelegate: FlutterAppDelegate, ITLoginDelegate, WXApiDelegate, TencentSessionDelegate, WWKApiDelegate {
    
    private var itloginInstance: ITLogin?
    private var loginChannel: FlutterMethodChannel?
    
    
    override func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
        ) -> Bool {
        GeneratedPluginRegistrant.register(with: self);
        FlutterDownloaderPlugin.setPluginRegistrantCallback{ registry in
            if (!registry.hasPlugin("FlutterDownloaderPlugin")) {
               FlutterDownloaderPlugin.register(with: registry.registrar(forPlugin: "FlutterDownloaderPlugin")!)
            }
        };
        
        
        
    

        let controller : FlutterViewController = window?.rootViewController as! FlutterViewController
        loginChannel = FlutterMethodChannel(name: "flutter.itlogin",
                                                binaryMessenger: controller.binaryMessenger)
        loginChannel?.setMethodCallHandler({
            (call: FlutterMethodCall, result: @escaping FlutterResult) -> Void in
            if (call.method == "getCkey") {
                return self.getCkey(result: result);
            } else if (call.method == "logout") {
                return self.logout(result: result);
            } else if (call.method == "initITLogin") {
                return self.initITLoginSDK(result: result);
            }
            
            return result(FlutterMethodNotImplemented);
        })



        return super.application(application, didFinishLaunchingWithOptions: launchOptions)
    }
    
    func registerPlugins(registry: FlutterPluginRegistry) {
        if (!registry.hasPlugin("FlutterDownloaderPlugin")) {
           FlutterDownloaderPlugin.register(with: registry.registrar(forPlugin: "FlutterDownloaderPlugin")!)
        }
    }
    
    func initITLoginSDK (result: FlutterResult) {
        // init ITLogin SDK
        if (itloginInstance == nil) {
            itloginInstance = (ITLogin.sharedInstance() as! ITLogin);
            //自定义logo和titlez
            itloginInstance!.changeLogo(UIImage(named: "loginIcon"))
            itloginInstance!.delegate = self;
            itloginInstance!.start(withAppKey: kAppKey, appId: kAppID);
            
        }
        itloginInstance?.logout();
        result(true);
    }
    
    override func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {
        if url.absoluteString.hasPrefix("itlogin-") {
            self.itloginInstance!.handleSSOURL(url);
        }
        // Process the URL.
            guard let components = NSURLComponents(url: url, resolvingAgainstBaseURL: true),
                  let albumPath = components.path else {
                    print("Invalid URL or album path missing")
                    return false
            }
        let urlPath = "/" + (components.host ?? "") + (components.path ?? "");
        
        if (url.absoluteString.hasPrefix("bkdevopsapp://")) {
            loginChannel!.invokeMethod("uni_link", arguments: urlPath);
        }
        
        if url.absoluteString.hasPrefix("wxwork-") {
            WWKApi.handleOpen(url, delegate: self);
        }
        return WXApi.handleOpen(url, delegate: self)
    }
    
    override func application(_ application: UIApplication, handleOpen url: URL) -> Bool {
        #if __QQAPI_ENABLE__
            QQApiInterface.handleOpen(url, delegate: QQAPIDemoEntry.self)
        #endif
        if true == TencentOAuth.canHandleOpen(url) {
            return TencentOAuth.handleOpen(url)
        }
        
        if url.absoluteString.hasPrefix("itlogin-") {
            self.itloginInstance!.handleSSOURL(url);
        }
       
        
        return true
    }


    override func application(_ application: UIApplication, continue userActivity: NSUserActivity, restorationHandler: @escaping ([UIUserActivityRestoring]?) -> Void) -> Bool {
        
        if let description = userActivity.webpageURL?.description {
            
            let webpageURL = userActivity.webpageURL
            let host = webpageURL?.host
            print("userActivity : \(description), \(webpageURL!.relativePath)");
            if host == "download.bkdevops.qq.com" {
                loginChannel!.invokeMethod("uni_link", arguments: webpageURL?.relativePath.dropFirst());
            }
        }
        
        WXApi.handleOpenUniversalLink(userActivity, delegate: self);
        
        return true;
    }

    @available(iOS 13.0, *)
    func scene(_ scene: UIScene, continue userActivity: NSUserActivity) {
        if let description = userActivity.webpageURL?.description {
            
            let webpageURL = userActivity.webpageURL
            let host = webpageURL?.host
            print("userActivity : \(description), \(webpageURL!.relativePath)");
            if host == "download.bkdevops.qq.com" {
                loginChannel!.invokeMethod("uni_link", arguments: webpageURL?.relativePath.dropFirst());
            }
        }
        WXApi.handleOpenUniversalLink(userActivity, delegate: self);
    }
    
    @available(iOS 9.0, *)
    override func application(_ application: UIApplication, performActionFor shortcutItem: UIApplicationShortcutItem, completionHandler: @escaping (Bool) -> Void) {
        let controller : FlutterViewController = window?.rootViewController as! FlutterViewController
        
        let channel = FlutterMethodChannel(name: "plugins.flutter.io/quick_actions", binaryMessenger: controller.binaryMessenger)
        channel.invokeMethod("launch", arguments: shortcutItem.type)
    }
    
    func on(_ resp: BaseResp?) {
        if resp is SendMessageToWXResp {
            //微信分享
            let messageResp = resp as? SendMessageToWXResp
            if messageResp?.errCode == 0 {
                print("微信分享成功！")
            } else {
                print("微信分享取消！")
            }
        }

        if resp is SendMessageToQQResp {
            //手Q分享回调
            let qqResp = resp as? SendMessageToQQResp
            if qqResp?.result == "0" {
                print("QQ分享成功！")
            } else {
                print("QQ分享取消！")
            }
        }
    }

    private func getCkey(result: FlutterResult) {
        
        let info: ITLoginInfo = itloginInstance!.getInfo();
        print("login cKey", info.credentialkey == "");
        result(info.credentialkey);
    }
    
    private func logout (result: FlutterResult) {
        itloginInstance?.logout();
        
        result(true)
    }
    
    func didValidateLoginSuccess() {
//        <#code#>
        let info: ITLoginInfo = itloginInstance!.getInfo();
        print("valid login success");
        loginChannel!.invokeMethod("loginResult", arguments: info.credentialkey);
    }
    
    func didValidateLoginFailWithError(_ error: ITLoginError!) {
//        <#code#>
        print("valid login fail");
//        iConsole.info("didValidateLoginFailWithError msg: %@, httpCode: %d, statusID: %d", error.errorMsg, error.httpStatusCode, error.errorCode)
        //如果是网络错误的情况
//        if Int(error.errorCode) == 0 {
//            //这里可以根据业务自身情况处理网络错误情况
//            let alert = UIAlertView(title: "验证登录", message: "验证登录失败，网络错误", delegate: nil, cancelButtonTitle: "OK", otherButtonTitles: "")
//            alert.show()
//        }
    }
    
    func didTokenLoginSuccess() {
//        <#code#>
        print("login success");
        let info: ITLoginInfo = itloginInstance!.getInfo();
        loginChannel!.invokeMethod("loginResult", arguments: info.credentialkey);
        
    }
    
    
    func didTokenLoginFailWithError(_ error: ITLoginError!) {
//        <#code#>
        print("token login fail");
    }
    
    func didFinishLogout() {
//        <#code#>
        print("after logout")
    }
    
    func enableSSO() -> Bool {
        return true;
    }
    
    func tencentDidLogin() {
//
    }
    
    func tencentDidNotLogin(_ cancelled: Bool) {
//
    }
    
    func tencentDidNotNetWork() {
//
    }
}

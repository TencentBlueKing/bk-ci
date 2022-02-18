//
//  ITLogin.h
//  ITLogin
//
//  Created by kavin on 2019/11/19.
//  Copyright © 2019 Tencent. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <ITLogin/ITLoginObject.h>
#import <UIKit/UIKit.h>

//! Project version number for ITLogin.
FOUNDATION_EXPORT double ITLoginVersionNumber;

//! Project version string for ITLogin.
FOUNDATION_EXPORT const unsigned char ITLoginVersionString[];

// In this header, you should import all the public headers of your framework using statements like #import <ITLogin/PublicHeader.h>


/**
 *  登录操作回调协议，分别有token登录的回调、验证登录的回调，以及登出的回调
 */
@protocol ITLoginDelegate <NSObject>

/**
 *  验证登录成功回调
 */
- (void)didValidateLoginSuccess;

/**
 *  验证登录失败回调，并返回登录失败数据信息
 */

- (void)didValidateLoginFailWithError:(ITLoginError*)error;
/**
 *  token或sso登录成功回调
 */
- (void)didTokenLoginSuccess;

/**
 *  token或sso登录失败回调，并返回登录失败数据信息
 */
- (void)didTokenLoginFailWithError:(ITLoginError*)error;

/**
 *  退出登录成功回调
 */
- (void)didFinishLogout;

///是否允许快速登录
- (BOOL)enableSSO;

@end


/**
 *  ITLogin sdk接口函数类，封装了ITLogin所需的所有接口
 */
@interface ITLogin : NSObject
/**
 *  登录操作的回调代理
 */
@property (nonatomic, assign) id<ITLoginDelegate> delegate;

/**
 *  封装ITLogin sdk接口函数的单例
 *
 *  @return 返回ITLogin接口函数的实例，供全局使用
 */
+ (id)sharedInstance;
/**
 *  设置ITLogin sdk权限使用appkey，接入sdk时需要联系申请
 *
 *  @param key    第三方产品使用ITLogin sdk的唯一权限标识
 *  @param appid  第三方产品使用ITLogin sdk的唯一id名
 */
- (void)startWithAppKey:(NSString*)key AppId:(NSString *)appid;
/**
 *  验证登录，用于用户自动登录、唤起app和使用过程中心跳验证登录的方法
 */
- (void)validateLogin;
/**
 *  退出登录，自动删除本地登录态
 */
- (void)logout;

/**
 *  对未授权的url进行授权处理，结果返回已授权的url
 *  @param toAuthUrl    待授权的url
 */
- (void) webAuthenticate:(NSString *)toAuthUrl completionHandler:(void (^)(NSString *url, ITLoginError *error))completionHandler;


/**
 *  检查url是否符合网关要求
 *  @param url    待检查的url
 */
- (BOOL) isMatchGateWay:(NSString *)url;


/**
 *  处理MOA SSO授权后传递过来的数据，需要在 application:openURL:sourceApplication:annotation:或者application:handleOpenURL中调用。
 *
 *  @param url MOA SSO授权处理后传递过来的URL
 */
- (void)handleSSOURL:(NSURL *)url;
/**
 *  获得当前登录身份信息
 *
 *  @return 返回当前用户登录的身份信息
 */
- (ITLoginInfo *)getLoginInfo;
/**
 *  自定义token登录页面logo
 *
 *  @param logo token登录页面顶部logo图标，默认展示ITLogin图标，建议自定义图片大小216*216px
 */
- (void)changeLogo:(UIImage *)logo;
/**
 *  ITLogin默认支持outlook白名单登录，如果调用此方法，则默认关闭outlook登录方式，弹出数字键盘强制token登录
 *
 */
- (void)disableOutlookLogin;

/**
 *  切换url授权机制的环境
 *
 */
- (void)debugDev:(BOOL) debugDev;

/**
*  往共享的App Group内写入自身应用的证书过期信息、版本信息
*
*  使用此方法的应用需在能力项中开启App Group，添加 group.com.tencent.inhouse.apps 这个group
*  并找Keystore-helper(证书助手) 把这个group关联到你的应用签名Profile中
*/
- (void)shareAppInfo;
@end

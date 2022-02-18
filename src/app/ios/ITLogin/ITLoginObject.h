//
//  ITLoginObject.h
//  ITLogin
//
//  Created by kavin on 2019/11/19.
//  Copyright © 2019 Tencent. All rights reserved.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/**
 *  登录错误状态码
 */
typedef enum
{
    LoginErrorCodeUnknow = 1,                  /**<  未知错误     */
    LoginErrorCodeParamsError,                 /**<  字段参数错误    */
    LoginErrorCodeAccountError,                /**<  帐号错误      */
    LoginErrorCKeyExpire,                      /**<  credentialkey已经过期或不存在，需重新token验证 */
    LoginErrorCodeAppKeyError,                 /**<  sdk appkey或appid错误或者失效    */
    LoginErrorDeviceForbid,                    /**<  设备被禁用，登录失败    */
    LoginErrorDeviceStatusChange,              /**<  设备状态被改变，需要重新token验证    */
    LoginErrorDeviceExist,                     /**<  设备已经注册给其他用户，不允许新用户登录    */
    LoginErrorDeviceError,                     /**<  设备错误    */
    LoginErrorCodeSSOAuthError,                /**<  SSO登录授权失败    */
    LoginErrorCodeDataError,                   /**<  数据处理错误    */
    LoginErrorMatchUrlError,                   /**<  需授权的url不符合网关地址样式   */
    LoginErrorGateway,                         /**<  网关中间层错误   */
} LoginErrorCode;



/**
 *  提供ITLogin登录身份信息的数据模型
 */
@interface ITLoginInfo : NSObject
/**
 *  第三方产品使用ITLogin sdk的唯一权限标识，接入sdk时需要联系申请
 */
@property (nonatomic, copy) NSString *appKey;
/**
 *  第三方产品使用ITLogin sdk分配的产品英文id名，接入sdk时需要联系申请
 */
@property (nonatomic, copy) NSString *appId;
/**
 *  当前登录用户的credentialkey登录票据
 */
@property (nonatomic, copy) NSString *credentialkey;
/**
 *  当前ITLogin sdk版本
 */
@property (nonatomic, copy) NSString *version;

@end

/**
 *  用于ITLogin登录失败状态时，返回的失败信息数据模型
 */
@interface ITLoginError : NSObject
/**
 *  http状态码
 */
@property (nonatomic, assign) NSInteger httpStatusCode;
/**
 *  登录请求返回的状态id，所有具体类型可见LoginErrorCode
 */
@property (nonatomic, assign) LoginErrorCode errorCode;
/**
 *  详细的登录错误信息
 */
@property (nonatomic, copy) NSString  *errorMsg;

@end


NS_ASSUME_NONNULL_END

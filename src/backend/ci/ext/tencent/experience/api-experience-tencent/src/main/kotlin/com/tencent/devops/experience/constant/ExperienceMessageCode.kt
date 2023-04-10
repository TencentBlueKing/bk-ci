/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.experience.constant

/**
 * 流水线微服务模块请求返回状态码
 * 返回码制定规则（0代表成功，为了兼容历史接口的成功状态都是返回0）：
 * 1、返回码总长度为7位，
 * 2、前2位数字代表系统名称（如21代表平台）
 * 3、第3位和第4位数字代表微服务模块（00：common-公共模块 01：process-流水线 02：artifactory-版本仓库 03:dispatch-分发 04：dockerhost-docker机器
 *    05:environment-环境 06：experience-版本体验 07：image-镜像 08：log-日志 09：measure-度量 10：monitoring-监控 11：notify-通知
 *    12：openapi-开放api接口 13：plugin-插件 14：quality-质量红线 15：repository-代码库 16：scm-软件配置管理 17：support-支撑服务
 *    18：ticket-证书凭据 19：project-项目管理 20：store-商店 21： auth-权限 22:sign-签名服务 23:metrics-度量服务 24：external-外部
 *    25：prebuild-预建 26:dispatcher-kubernetes 27：buildless 28: lambda 29: stream  30: worker 31: dispatcher-docker）
 * 4、最后3位数字代表具体微服务模块下返回给客户端的业务逻辑含义（如001代表系统服务繁忙，建议一个模块一类的返回码按照一定的规则制定）
 * 5、系统公共的返回码写在CommonMessageCode这个类里面，具体微服务模块的返回码写在相应模块的常量类里面
 *
 * @since: 2023-3-20
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */
object ExperienceMessageCode {

    const val EXP_GROUP_NOT_EXISTS = "2106001" // 体验：体验组({0})不存在
    const val EXP_GROUP_IS_EXISTS = "2106002" // 体验：体验组({0})已存在
    const val TOKEN_EXPIRED = "2106003" // 体验：Token过期
    const val TOKEN_NOT_EXISTS = "2106004" // 体验：Token不存在
    const val EXP_EXPIRE = "2106005" // 体验：体验已过期
    const val EXP_REMOVED = "2106006" // 体验：体验已下架
    const val EXP_FILE_NOT_FOUND = "2106007" // 体验：体验的文件不存在
    const val EXP_NOT_EXISTS = "2106008" // 体验：体验不存在
    const val OUTER_LOGIN_ERROR = "2106009" // 外部用户登录错误
    const val OUTER_ACCESS_FAILED = "2106010" // 外部用户访问失败
    const val EXPERIENCE_NEED_PERMISSION = "2106011" // 需要用户权限
    const val EXPERIENCE_NO_AVAILABLE = "2106012" // 该版本不可用，可能已被下架、已过期或被新版本覆盖，请刷新页面重试

    const val RECORD_COULD_NOT_FOUND = "2106013" //找不到该记录
    const val USER_NOT_PERMISSION = "2106014" //用户没有权限
    const val GRANT_EXPERIENCE_PERMISSION = "2106015" //请联系产品负责人：\n{0} 授予体验权限。
    const val NO_PERMISSION_QUERY_EXPERIENCE = "2106016" //没有查询该体验的权限
    const val LOGIN_IP_FREQUENTLY = "2106017" //登录IP频繁,请稍后重试
    const val LOGIN_ACCOUNT_FREQUENT = "2106018" //登录账号频繁,请稍后重试
    const val ACCOUNT_HAS_BEEN_BLOCKED = "2106019" //账号已被封禁
    const val LOGIN_EXPIRED = "2106020" //登录过期,请重新登录
    const val ACCOUNT_INFORMATION_ABNORMAL = "2106021" //账号信息异常,请重新登录
    const val UNABLE_GET_IP = "2106022" //无法获取IP , 请联系相关人员排查
    const val METADATA_NOT_EXIST = "2106023" //元数据{0}不存在
    const val EXPERIENCE_NOT_EXIST = "2106024" //体验({0})不存在
    const val FILE_NOT_EXIST = "2106025" //文件({0})不存在

    const val USER_NEED_EXP_X_PERMISSION = "2106026" //体验: 用户没有体验的{0}权限
    const val USER_NEED_EXP_GROUP_X_PERMISSION = "2106027" //体验：用户没有体验组的{0}权限
    const val EXP_META_DATA_PIPELINE_ID_NOT_EXISTS = "2106028" //体验：体验未与流水线绑定
    const val USER_NOT_IN_EXP_GROUP = "2106029" //体验：用户{0}不在体验用户名单中

    const val BK_UPDATED_SUCCESSFULLY_AND_SET = "bkUpdatedSuccessfullyAndSet" //更新成功,已置为
    const val BK_UPDATED_SUCCESSFULLY = "bkUpdatedSuccessfully" //更新成功
    const val BK_NEW_SEARCH_RECOMMENDATION_SUCCEEDED = "bkNewSearchRecommendationSucceeded" //新增搜索推荐成功
    const val BK_DELETE_SEARCH_RECOMMENDATION_SUCCEEDED = "bkDeleteSearchRecommendationSucceeded" //删除搜索推荐成功
    const val BK_CREATED_SUCCESSFULLY = "bkCreatedSuccessfully" //创建成功
    const val BK_NO_EXPERIENCE_UNDER_PROJECT = "bkNoExperienceUnderProject" //{0} 项目下无体验
    const val BK_NO_EXPERIENCE = "bkNoExperience" //无体验
    const val BK_NO_EXPERIENCE_GROUP_UNDER_PROJECT = "bkNoExperienceGroupUnderProject" //{0} 项目下无体验组
    const val BK_NO_EXPERIENCE_USER_GROUP = "bkNoExperienceUserGroup" //无体验用户组
    const val BK_NO_EXPERIENCE_USER_GROUP_UNDER_PROJECT = "bkNoExperienceUserGroupUnderProject" //{0} 项目下无体验用户组
    const val BK_USER_BOUND_DEVICE_SUCCESSFULLY = "bkUserBoundDeviceSuccessfully" //用户绑定设备成功！
    const val BK_NOT_REPEATEDLY_BIND = "bkNotRepeatedlyBind" //请勿重复绑定同台设备！
    const val BK_USER_MODIFIED_DEVICE_SUCCESSFULLY = "bkUserModifiedDeviceSuccessfully" //用户修改设备成功！
    const val BK_USER_FAILED_TO_MODIFY_DEVICE = "bkUserFailedToModifyDevice" //用户修改设备失败！
    const val BK_EXPERIENCE_IS_SUBSCRIBED = "bkExperienceIsSubscribed" //该体验已订阅，不允许重复订阅
    const val BK_INTERNAL_EXPERIENCE_SUBSCRIBED_DEFAULT = "bkInternalExperienceSubscribedDefault" //内部体验默认已订阅
    const val BK_SUBSCRIPTION_EXPERIENCE_NOT_ALLOWED = "bkSubscriptionExperienceNotAllowed" //不允许订阅内部体验
    const val BK_SUBSCRIPTION_EXPERIENCE_SUCCESSFUL = "bkSubscriptionExperienceSuccessful" //订阅体验成功！
    const val BK_PLEASE_CHANGE_CONFIGURATION = "bkPleaseChangeConfiguration" //既是公开体验又是内部体验的应用版本无法自行取消订阅,蓝盾App已不再支持同时选中两种体验范围，请尽快更改发布体验版本的配置。
    const val BK_CANNOT_BE_CANCELLED_BY_ITSELF = "bkCannotBeCancelledByItself" //内部体验默认为已订阅状态，无法自行取消。如需取消订阅，请联系产品负责人退出内部体验，退出后将不接收订阅信息。
    const val BK_INTERNAL_EXPERIENCE_CANNOT_UNSUBSCRIBED = "bkInternalExperienceCannotUnsubscribed" //内部体验不可取消订阅
    const val BK_NOT_ALLOWED_TO_CANCEL_THE_EXPERIENCE = "bkNotAllowedToCancelTheExperience" //由于没有订阅该体验，不允许取消体验
    const val BK_UNSUBSCRIBED_SUCCESSFULLY = "bkUnsubscribedSuccessfully" //取消订阅成功
    const val BK_USER_NOT_BOUND_DEVICE = "bkUserNotBoundDevice" //该用户未绑定设备
    const val BK_PLATFORM_IS_INCONSISTENT = "bkPlatformIsInconsistent" //绑定平台与包平台不一致
    const val BK_USER_NOT_EDIT_PERMISSION = "bkUserNotEditPermission" //用户在项目({0})下没有体验({0})的编辑权限
    const val BK_CONSTRUCTION_NUMBER = "bkConstructionNumber" //构建号#{0}
    const val BK_USER_NOT_EDIT_PERMISSION_GROUP = "bkUserNotEditPermissionGroup" //用户在项目({0})没有体验组({1})的编辑权限
    const val BK_HAS_BEEN_UPDATED = "bkHasBeenUpdated" //【{0}】 {1} 更新啦
    const val BK_LATEST_EXPERIENCE_VERSION_CLICK_VIEW = "bkLatestExperienceVersionClickView" //【{0}】发布了最新体验版本，蓝盾App诚邀您参与体验。点击查看>>
    const val BK_BLUE_SHIELD_VERSION_EXPERIENCE_NOTIFICATION = "bkBlueShieldVersionExperienceNotification" //【蓝盾版本体验通知】{0}邀您体验【{1}-{2}】
    const val BK_INVITES_YOU_EXPERIENCE = "bkInvitesYouExperience" //{0}邀您体验【{1}-{2}】
    const val BK_NAME = "bkName" //名称
    const val BK_VIEW = "bkView" //查看
    const val BK_LATEST_EXPERIENCE_VERSION_SHARING = "bkLatestExperienceVersionSharing" //【{0}】最新体验版本分享
    const val BK_LATEST_INVITES_YOU_EXPERIENCE = "bkLatestInvitesYouExperience" //【{0}】发布了最新体验版本，【{1}-{2}】诚邀您参与体验。
    const val BK_PC_EXPERIENCE_ADDRESS = "bkPcExperienceAddress" //\nPC体验地址
    const val BK_MOBILE_EXPERIENCE_ADDRESS = "bkMobileExperienceAddress" //\n手机体验地址
    const val BK_LATEST_EXPERIENCE_VERSION_INFO = "bkLatestExperienceVersionInfo" //【{0}】发布了最新体验版本，【{1}-{2}】诚邀您参与体验。\nPC体验地址：{3}\n手机体验地址：{4}

}

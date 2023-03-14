package com.tencent.devops.experience.constant

object ExperienceCode {
    //const val BK_WHOLE_COMPANY = "WholeCompany  " //全公司
    //const val BK_PUBLIC_EXPERIENCE = "PublicExperience  " //公开体验
    const val BK_UNABLE_TO_ACCESS = "UnableToAccess" //无法访问
    const val BK_RECORD_COULD_NOT_FOUND = "RecordCouldNotFound" //找不到该记录
    const val BK_UPDATED_SUCCESSFULLY_AND_SET = "UpdatedSuccessfullyAndSet" //更新成功,已置为{0}
    const val BK_UPDATED_SUCCESSFULLY = "UpdatedSuccessfully" //更新成功
    const val BK_NEW_SEARCH_RECOMMENDATION_SUCCEEDED = "NewSearchRecommendationSucceeded" //新增搜索推荐成功
    const val BK_DELETE_SEARCH_RECOMMENDATION_SUCCEEDED = "DeleteSearchRecommendationSucceeded" //删除搜索推荐成功
    const val BK_CREATED_SUCCESSFULLY = "CreatedSuccessfully" //创建成功
    const val BK_USER_NOT_PERMISSION = "UserNotPermission" //用户没有权限
    const val BK_NO_EXPERIENCE_UNDER_PROJECT = "NoExperienceUnderProject" //{0} 项目下无体验
    const val BK_NO_EXPERIENCE = "NoExperience" //{0}无体验
    const val BK_NO_EXPERIENCE_GROUP_UNDER_PROJECT = "NoExperienceGroupUnderProject" //{0} 项目下无体验组
    const val BK_NO_EXPERIENCE_USER_GROUP = "NoExperienceUserGroup" //{0}无体验用户组
    const val BK_NO_EXPERIENCE_USER_GROUP_UNDER_PROJECT= "NoExperienceUserGroupUnderProject" //{0} 项目下无体验用户组
    const val BK_GRANT_EXPERIENCE_PERMISSION = "GrantExperiencePermission" //请联系产品负责人：\n{0} 授予体验权限。
    const val BK_NO_PERMISSION_QUERY_EXPERIENCE = "NoPermissionQueryExperience" //没有查询该体验的权限
    const val BK_LOGIN_ERROR = "BkLoginError" //登录错误
    const val BK_LOGIN_IP_FREQUENTLY = "BkLoginIpFrequently" //登录IP频繁,请稍后重试
    const val BK_LOGIN_ACCOUNT_FREQUENT = "BkLoginAccountFrequent" //登录账号频繁,请稍后重试
    const val BK_ACCOUNT_HAS_BEEN_BLOCKED = "BkAccountHasBeenBlocked" //账号已被封禁
    const val BK_LOGIN_EXPIRED = "BkLoginExpired" //登录过期,请重新登录
    const val BK_ACCOUNT_INFORMATION_ABNORMAL = "BkAccountInformationAbnormal" //账号信息异常,请重新登录
    const val BK_UNABLE_GET_IP = "BkUnableGetIp" //无法获取IP , 请联系相关人员排查
    const val BK_USER_BOUND_DEVICE_SUCCESSFULLY = "BkUserBoundDeviceSuccessfully" //用户绑定设备成功！
    const val BK_NOT_REPEATEDLY_BIND = "BkNotRepeatedlyBind" //请勿重复绑定同台设备！
    const val BK_USER_MODIFIED_DEVICE_SUCCESSFULLY = "BkUserModifiedDeviceSuccessfully" //用户修改设备成功！
    const val BK_USER_FAILED_TO_MODIFY_DEVICE = "BkUserFailedToModifyDevice" //用户修改设备失败！
    const val BK_EXPERIENCE_IS_SUBSCRIBED = "BkExperienceIsSubscribed" //该体验已订阅，不允许重复订阅
    const val BK_INTERNAL_EXPERIENCE_SUBSCRIBED_DEFAULT = "BkInternalExperienceSubscribedDefault" //内部体验默认已订阅
    const val BK_SUBSCRIPTION_EXPERIENCE_NOT_ALLOWED = "BkSubscriptionExperienceNotAllowed" //不允许订阅内部体验
    const val BK_SUBSCRIPTION_EXPERIENCE_SUCCESSFUL = "BkSubscriptionExperienceSuccessful" //订阅体验成功！
    const val BK_PLEASE_CHANGE_CONFIGURATION = "BkPleaseChangeConfiguration" //既是公开体验又是内部体验的应用版本无法自行取消订阅,蓝盾App已不再支持同时选中两种体验范围，请尽快更改发布体验版本的配置。
    const val BK_CANNOT_BE_CANCELLED_BY_ITSELF = "BkCannotBeCancelledByItself" //内部体验默认为已订阅状态，无法自行取消。如需取消订阅，请联系产品负责人退出内部体验，退出后将不接收订阅信息。
    const val BK_INTERNAL_EXPERIENCE_CANNOT_UNSUBSCRIBED = "BkInternalExperienceCannotUnsubscribed" //内部体验不可取消订阅
    const val BK_NOT_ALLOWED_TO_CANCEL_THE_EXPERIENCE = "BkNotAllowedToCancelTheExperience" //由于没有订阅该体验，不允许取消体验
    const val BK_UNSUBSCRIBED_SUCCESSFULLY = "BkUnsubscribedSuccessfully" //取消订阅成功
    const val BK_USER_NOT_BOUND_DEVICE = "BkUserNotBoundDevice" //该用户未绑定设备
    const val BK_PLATFORM_IS_INCONSISTENT  = "BkPlatformIsInconsistent" //绑定平台与包平台不一致
    const val BK_METADATA_NOT_EXIST = "BkMetadataNotExist" //元数据{0}不存在
    const val BK_USER_NOT_EDIT_PERMISSION = "BkUserNotEditPermission" //用户在项目({0})下没有体验({0})的编辑权限
    const val BK_EXPERIENCE_NOT_EXIST = "BkExperienceNotExist" //体验({0})不存在
    const val BK_USER_NOT_EXPERIENCE_USERS = "BkUserNotExperienceUsers" //用户({0})不在体验用户名单中
    const val BK_FILE_NOT_EXIST = "BkFileNotExist" //文件({0})不存在
    const val BK_CONSTRUCTION_NUMBER = "BkConstructionNumber" //构建号#{0}
    const val BK_USERS_NOT_PERMISSION = "BkUsersNotPermission" //用户没有流水线执行权限
    const val BK_USER_NOT_EDIT_PERMISSION_GROUP = "BkUserNotEditPermissionGroup" //用户在项目({0})没有体验组({1})的编辑权限


}
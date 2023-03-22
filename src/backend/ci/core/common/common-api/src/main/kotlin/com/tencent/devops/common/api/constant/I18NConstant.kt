package com.tencent.devops.common.api.constant

object I18NConstant {
    const val BK_BLUE_SHIELD_SHARE_FILES_WITH_YOU = "BkBlueShieldShareFilesWithYou"//【蓝盾版本仓库通知】{0}与你共享{1}文件
    const val BK_BLUE_SHIELD_SHARE_AND_OTHER_FILES_WITH_YOU = "BkBlueShieldShareAndOtherFilesWithYou"//【蓝盾版本仓库通知】{0}与你共享{1}等{2}个文件
    const val BK_SHARE_FILES_PLEASE_DOWNLOAD_FILES_IN_TIME = "BkShareFilesPleaseDownloadFilesInTime"//{0}与你共享以下文件，请在有效期（{1}}天）内及时下载：
    const val BK_FILE_NAME = "BkFileName"//文件名
    const val BK_BELONG_TO_THE_PROJECT = "BkBelongToTheProject"//所属项目
    const val BK_OPERATING = "BkOperating"//操作
    const val BK_DOWNLOAD = "BkDownload"//下载
    const val BK_PUSH_FROM_BLUE_SHIELD_DEVOPS_PLATFORM = "BkPushFromBlueShieldDevopsPlatform"//来自蓝盾DevOps平台的推送
    const val BK_TABLE_CONTENTS = "BkTableContents"//表格内容
    const val BK_PLEASE_FEEL_TO_CONTACT_BLUE_SHIELD_ASSISTANT = "BkPleaseFeelToContactBlueShieldAssistant"//如有任何问题，可随时联系蓝盾助手
    const val BK_RECEIVED_THIS_EMAIL_BECAUSE_YOU_FOLLOWED_PROJECT = "BkBkReceivedThisEmailBecauseYouFollowedProject"//你收到此邮件，是因为你关注了 {0} 项目，或其它人@了你
    const val BK_ILLEGAL_PATH = "BkIllegalPath"//非法路径

    const val BK_BLUE_SHIELD_PUBLIC_BUILD_RESOURCES = "BkBlueShieldPublicBuildResources"//蓝盾公共构建资源
    const val BK_BLUE_SHIELD_PUBLIC_BUILD_RESOURCES_NEW = "BkBlueShieldPublicBuildResourcesNew"//蓝盾公共构建资源(NEW)
    const val BK_PUBLIC_DOCKER_ON_DEVNET_PHYSICAL = "BkPublicDockerOnDevnetPhysical"//公共：Docker on Devnet 物理机
    const val BK_PUBLIC_DOCKER_ON_DEVCLOUD = "BkPublicDockerOnDevcloud"//公共：Docker on DevCloud
    const val BK_PUBLIC_DOCKER_ON_BCS = "BkPublicDockerOnBcs"//公共：Docker on Bcs
    const val BK_PRIVATE_SINGLE_BUIL_MACHINE = "BkPrivateSingleBuilMachine"//私有：单构建机
    const val BK_PRIVATE_BUILD_A_CLUSTER = "BkPrivateBuildACluster"//私有：构建集群
    const val BK_PCG_PUBLIC_BUILD_RESOURCES = "BkPcgPublicBuildResources"//PCG公共构建资源
    const val BK_TENCENT_SELF_DEVELOPED_CLOUD= "BkTencentSelfDevelopedCloud"//腾讯自研云（云devnet资源）
    const val BK_CLOUD_HOSTING_WINDOWS_ON_DEVCLOUD = "BkCloudHostingWindowsOnDevcloud"//云托管：Windows on DevCloud

    const val BK_DOCKER_BUILDER_RUNS_TOO_MANY = "BkDockerBuilderRunsTooMany"//Docker构建机运行的容器太多，母机IP:{0}，容器数量: {1}
    const val BK_BUILD_ENVIRONMENT_STARTS_SUCCESSFULLY = "BkBuildEnvironmentStartsSuccessfully"//构建环境启动成功，等待Agent启动...
    const val BK_FAILED_TO_START_IMAGE_NOT_EXIST = "BkFailedToStartImageNotExist"//构建环境启动失败，镜像不存在, 镜像:{0}
    const val BK_FAILED_TO_START_ERROR_MESSAGE = "BkFailedToStartErrorMessage"//构建环境启动失败，错误信息

    const val BK_NORMAL_VERSION = "BkNormalVersion" //8核16G（普通版）
    const val BK_INTEL_XEON_SKYLAKE_PROCESSOR = "BkIntelXeonSkylakeProcessor" //2.5GHz 64核 Intel Xeon Skylake 6133处理器
    const val BK_MEMORY = "BkMemory" //32GB*12 DDR3 内存
    const val BK_SOLID_STATE_DISK = "BkSolidStateDisk" //{0}GB 固态硬盘
    const val BK_ESTIMATED_DELIVERY_TIME = "BkEstimatedDeliveryTime" //预计交付周期：{0}分钟
    const val BK_HIGH_END_VERSION = "BkHighEndVersion" //32核64G（高配版）

    const val BK_UPDATED_SUCCESSFULLY_AND_SET = "BkUpdatedSuccessfullyAndSet" //更新成功,已置为
    const val BK_UPDATED_SUCCESSFULLY = "BkUpdatedSuccessfully" //更新成功
    const val BK_NEW_SEARCH_RECOMMENDATION_SUCCEEDED = "BkNewSearchRecommendationSucceeded" //新增搜索推荐成功
    const val BK_DELETE_SEARCH_RECOMMENDATION_SUCCEEDED = "BkDeleteSearchRecommendationSucceeded" //删除搜索推荐成功
    const val BK_CREATED_SUCCESSFULLY = "BkCreatedSuccessfully" //创建成功
    const val BK_NO_EXPERIENCE_UNDER_PROJECT = "BkNoExperienceUnderProject" //{0} 项目下无体验
    const val BK_NO_EXPERIENCE = "BkNoExperience" //无体验
    const val BK_NO_EXPERIENCE_GROUP_UNDER_PROJECT = "BkNoExperienceGroupUnderProject" //{0} 项目下无体验组
    const val BK_NO_EXPERIENCE_USER_GROUP = "BkNoExperienceUserGroup" //无体验用户组
    const val BK_NO_EXPERIENCE_USER_GROUP_UNDER_PROJECT= "BkNoExperienceUserGroupUnderProject" //{0} 项目下无体验用户组
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
    const val BK_USER_NOT_EDIT_PERMISSION = "BkUserNotEditPermission" //用户在项目({0})下没有体验({0})的编辑权限

}
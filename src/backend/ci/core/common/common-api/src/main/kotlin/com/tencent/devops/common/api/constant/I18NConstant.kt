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
    const val BK_CONSTRUCTION_NUMBER = "BkConstructionNumber" //构建号#{0}
    const val BK_USER_NOT_EDIT_PERMISSION_GROUP = "BkUserNotEditPermissionGroup" //用户在项目({0})没有体验组({1})的编辑权限
    const val BK_HAS_BEEN_UPDATED = "BkHasBeenUpdated" //【{0}】 {1} 更新啦
    const val BK_LATEST_EXPERIENCE_VERSION_CLICK_VIEW = "BkLatestExperienceVersionClickView" //【{0}】发布了最新体验版本，蓝盾App诚邀您参与体验。点击查看>>
    const val BK_BLUE_SHIELD_VERSION_EXPERIENCE_NOTIFICATION = "BkBlueShieldVersionExperienceNotification" //【蓝盾版本体验通知】{0}邀您体验【{1}-{2}】
    const val BK_INVITES_YOU_EXPERIENCE = "BkInvitesYouExperience" //{0}邀您体验【{1}-{2}】
    const val BK_NAME = "BkName" //名称
    const val BK_BELONG_TO_PROJECT = "BkBelongToProject" //所属项目
    const val BK_OPERATE = "BkOperate" //操作
    const val BK_VIEW = "BkView"//查看
    const val BK_LATEST_EXPERIENCE_VERSION_SHARING = "BkLatestExperienceVersionSharing"//【{0}】最新体验版本分享
    const val BK_LATEST_INVITES_YOU_EXPERIENCE = "BkLatestInvitesYouExperience"//【{0}】发布了最新体验版本，【{1}-{2}】诚邀您参与体验。
    const val BK_PC_EXPERIENCE_ADDRESS = "BkPcExperienceAddress"//\nPC体验地址
    const val BK_MOBILE_EXPERIENCE_ADDRESS = "BkMobileExperienceAddress"//\n手机体验地址
    const val BK_LATEST_EXPERIENCE_VERSION_INFO = "BkLatestExperienceVersionInfo"//【{0}】发布了最新体验版本，【{1}-{2}】诚邀您参与体验。\nPC体验地址：{3}\n手机体验地址：{4}
    const val BK_FAILED_GET_GITHUB_ACCESS_TOKEN = "BkFailedGetGithubAccessToken"//获取Github access_token失败

    const val BK_ADD_DETECTION_TASK = "BkAddDetectionTask"//添加检测任务
    const val BK_UPDATE_DETECTION_TASK = "BkUpdateDetectionTask"//更新检测任务
    const val BK_GET_WAREHOUSE_LIST = "BkGetWarehouseList"//获取仓库列表
    const val BK_GET_SPECIFIED_BRANCH = "BkGetSpecifiedBranch"//获取指定分支
    const val BK_GET_SPECIFIED_TAG = "BkGetSpecifiedTag"//获取指定Tag
    const val BK_GET_LIST_OF_BRANCHES = "BkGetListOfBranches"//获取分支列表
    const val BK_GET_TAG_LIST = "BkGetTagList"//获取Tag列表

    const val BK_SOURCE_IMAGE = "BkSourceImage"//源镜像：{0}
    const val BK_TARGET_IMAGE = "BkTargetImage"//目标镜像：{0}:{1}
    const val BK_SUCCESSFUL_REGISTRATION_IMAGE = "BkSuccessfulRegistrationImage"//注册镜像成功
    const val BK_FAILED_REGISTER_IMAGE = "BkFailedRegisterImage"//注册镜像失败，错误信息：

    const val BK_FAILED_INSERT_DATA = "BkFailedInsertData"//蓝盾ES集群插入数据失败
    const val BK_ES_CLUSTER_RECOVERY = "BkEsClusterRecovery"//蓝盾ES集群恢复
    const val BK_FAILURE = "BkFailure"//失效
    const val BK_RECOVERY = "BkRecovery"//恢复
    const val BK_ES_CLUSTER_STATUS_ALARM_NOTIFICATION = "BkEsClusterStatusAlarmNotification"//【ES集群状态告警通知】
    const val BK_NOTIFICATION_PUSH_FROM_BKDEVOP = "BkNotificationPushFromBkdevop"//来自BKDevOps/蓝盾DevOps平台的通知推送
    const val BK_CLUSTER_NAME = "BkClusterName"//集群名称
    const val BK_STATUS = "BkStatus"//状态
    const val BK_EMPTY_DATA = "BkEmptyData"//空数据
    const val BK_LOOK_FORWARD_IT = "BkLookForwardIt"//敬请期待！
    const val BK_CONTACT_BLUE_SHIELD_ASSISTANT = "BkContactBlueShieldAssistant"//如有任何问题，可随时联系蓝盾助手。
    const val BK_HEAD_OF_BLUE_SHIELD_LOG_MANAGEMENT  = "BkHeadOfBlueShieldLogManagement"//你收到此邮件，是因为你是蓝盾日志管理负责人

    const val BK_ILLEGAL_TIMESTAMP_RANGE = "BkIllegalTimestampRange" //非法时间戳范围
    const val BK_ILLEGAL_ENTERPRISE_GROUP_ID = "BkIllegalEnterpriseGroupId" //非法事业群ID
    const val BK_INCORRECT_PASSWORD = "BkIncorrectPassword" //密码错误
    const val BK_SENT_SUCCESSFULLY = "BkSentSuccessfully" //发送成功
    const val BK_WARNING_MESSAGE_FROM_GRAFANA = "BkWarningMessageFromGrafana" //来自Grafana的预警信息
    const val BK_MONITORING_OBJEC = "BkMonitoringObjec" //监控对象：{0}，当前值为：{1}；
    const val BK_SEND_MONITORING_MESSAGES = "BkSendMonitoringMessages" //只有处于alerting告警状态的信息才发送监控消息

    const val BK_CONTROL_MESSAGE_LENGTH = "BkControlMessageLength" //...(消息长度超{0} 已截断,请控制消息长度)
    const val BK_LINE_BREAKS_WILL_ESCAPED = "BkLineBreaksWillEscaped" //(注意: 换行会被转义为\\n)
    const val BK_DESIGNATED_APPROVER_APPROVAL = "BkDesignatedApproverApproval" //指定审批人审批

    const val BK_BUILDID_NOT_FOUND = "BkBuildidNotFound" // 服务端内部异常，buildId={0}的构建未查到
    const val BK_PIPELINEID_NOT_FOUND = "BkPipelineidNotFound" // 服务端内部异常，pipelineId={0}的构建未查到

    const val BK_ETH1_NETWORK_CARD_IP_EMPTY = "BkEth1NetworkCardIpEmpty" // eth1 网卡Ip为空，因此，获取eth0的网卡ip
    const val BK_LOOPBACK_ADDRESS_OR_NIC_EMPTY = "BkLoopbackAddressOrNicEmpty" // loopback地址或网卡名称为空
    const val BK_FAILED_GET_NETWORK_CARD = "BkFailedGetNetworkCard" // 获取网卡失败

    const val BK_AGENT_NOT_INSTALLED = "BkAgentNotInstalled" // Agent未安装，请安装Agent.
    const val BK_ILLEGAL_YAML = "BkIllegalYaml" // YAML非法:
    const val BK_MANUAL_TRIGGER = "BkManualTrigger" // 手动触发
    const val BK_BUILD_TRIGGER = "BkBuildTrigger" // 构建触发
    const val BK_NO_COMPILATION_ENVIRONMENT = "BkNoCompilationEnvironment" // 无编译环境
    const val BK_TBUILD_ENVIRONMENT_LINUX = "BkTbuildEnvironmentLinux" // 构建环境-LINUX
    const val BK_SYNCHRONIZE_LOCAL_CODE = "BkSynchronizeLocalCode" // 同步本地代码

    const val BK_SUCCESSFULLY_DISTRIBUTED = "BkSuccessfullyDistributed" // 跨项目构件分发成功，共分发了{0}个文件
    const val BK_SUCCESSFULLY_FAILED = "BkSuccessfullyFailed" // 跨项目构件分发失败，
    const val BK_NO_MATCH_FILE_DISTRIBUTE = "BkNoMatchFileDistribute" // 匹配不到待分发的文件: {0}
    const val BK_START_PERFORMING_GCLOUD_OPERATION = "BkStartPerformingGcloudOperation" // 开始对文件（{0}）执行Gcloud相关操作，详情请去gcloud官方地址查看：
    const val BK_VIEW_DETAILS = "BkViewDetails" // 查看详情
    const val BK_START_UPLOAD_OPERATION = "BkStartUploadOperation" // 开始执行 \"上传动态资源版本\" 操作
    const val BK_OPERATION_PARAMETERS = "BkOperationParameters" // \"上传动态资源版本\" 操作参数：
    const val BK_QUERY_VERSION_UPLOAD  = "BkQueryVersionUpload" // 开始执行 \"查询版本上传 CDN 任务状态\" 操作\n
    const val BK_WAIT_QUERY_VERSION  = "BkWaitQueryVersion" // \"等待查询版本上传 CDN 任务状态\" 操作执行完毕: \n
    const val BK_OPERATION_COMPLETED_SUCCESSFULLY  = "BkOperationCompletedSuccessfully" // \"查询版本上传 CDN 任务状态\" 操作 成功执行完毕\n
    const val BK_FAILED_UPLOAD_FILE  = "BkFailedUploadFile" // 上传文件失败:
    const val BK_CREATE_RESOURCE_OPERATION  = "BkCreateResourceOperation" // 开始执行 \"创建资源\" 操作\n
    const val BK_CREATE_RESOURCES_OPERATION_PARAMETERS  = "BkCreateResourcesOperationParameters" // \"创建资源\" 操作参数：
    const val BK_START_RELEASE_OPERATION = "BkStartReleaseOperation" // 开始执行 \"预发布\" 操作\n
    const val BK_RESPONSE_RESULT = "BkResponseResult" // 预发布单个或多个渠道响应结果:
    const val BK_RECIPIENT_EMPTY = "BkRecipientEmpty" // 收件人为空
    const val BK_EMAIL_NOTIFICATION_CONTENT_EMPTY = "BkEmailNotificationContentEmpty" // 邮件通知内容为空
    const val BK_MESSAGE_SUBJECT_EMPTY = "BkMessageSubjectEmpty" // 邮件主题为空
    const val BK_EXPERIENCE_PATH_EMPTY = "BkExperiencePathEmpty" // 体验路径为空
    const val BK_INCORRECT_NOTIFICATION_METHOD = "BkIncorrectNotificationMethod" // 通知方式不正确
    const val BK_FILE_NOT_EXIST = "BkFileNotExist" // 文件({0})不存在
    const val BK_VERSION_EXPERIENCE_CREATED_SUCCESSFULLY = "BkVersionExperienceCreatedSuccessfully" // 版本体验({0})创建成功
    const val BK_VIEW_RESULT = "BkViewResult" // 查看结果:
    const val BK_RECEIVER_EMPTY = "BkReceiverEmpty" // Message Receivers is empty(接收人为空)
    const val BK_MESSAGE_CONTENT_EMPTY = "BkMessageContentEmpty" // Message Body is empty(消息内容为空)
    const val BK_EMPTY_TITLE = "BkEmptyTitle" // Message Title is empty(标题为空)
    const val BK_COMPUTER_VIEW_DETAILS = "BkComputerViewDetails" // {0}\n\n电脑查看详情：{1}\n手机查看详情：{2}
    const val BK_SEND_WECOM_MESSAGE = "BkSendWecomMessage" // send enterprise wechat message(发送企业微信消息):\n{0}\nto\n{1}

}
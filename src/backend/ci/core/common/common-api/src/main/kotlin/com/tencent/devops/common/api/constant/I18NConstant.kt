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
    const val BK_INVALID_NOTIFICATION_RECIPIENT = "BkInvalidNotificationRecipient" // 通知接收者不合法:
    const val BK_WECOM_NOTICE = "BkWecomNotice" // 企业微信通知内容:
    const val BK_MOBILE_VIEW_DETAILS = "BkMobileViewDetails" // {0}\n\n 手机查看详情：{1} \n 电脑查看详情：{2}
    const val BK_SEND_WECOM_CONTENT = "BkSendWecomContent" // 发送企业微信内容: ({0}) 到 {1}
    const val BK_SEND_WECOM_CONTENT_SUCCESSFULLY = "BkSendWecomContentSuccessfully" // 发送企业微信内容: ({0}) 到 {1}成功
    const val BK_SEND_WECOM_CONTENT_FAILED = "BkSendWecomContentFailed" // 发送企业微信内容: ({0}) 到 {1}失败:
    const val BK_MATCHING_FILE = "BkMatchingFile" // 匹配文件中:
    const val BK_UPLOAD_CORRESPONDING_FILE = "BkUploadCorrespondingFile" // 上传对应文件到织云成功!
    const val BK_START_UPLOADING_CORRESPONDING_FILES = "BkStartUploadingCorrespondingFiles" // 开始上传对应文件到织云...
    const val BK_PULL_GIT_WAREHOUSE_CODE = "BkPullGitWarehouseCode" // 拉取Git仓库代码
    const val BK_AUTOMATIC_EXPORT_NOT_SUPPORTED = "BkAutomaticExportNotSupported" // ### 该环境不支持自动导出，请参考 https://iwiki.woa.com/x/2ebDKw 手动配置 ###
    const val BK_BUILD_CLUSTERS_THROUGH = "BkBuildClustersThrough" // ### 可以通过 runs-on: macos-10.15 使用macOS公共构建集群。
    const val BK_NOTE_DEFAULT_XCODE_VERSION = "BkNoteDefaultXcodeVersion" // 注意默认的Xcode版本为12.2，若需自定义，请在JOB下自行执行 xcode-select 命令切换 ###
    const val BK_PLEASE_USE_STAGE_AUDIT = "BkPleaseUseStageAudit" // 人工审核插件请改用Stage审核 ###
    const val BK_PLUG_NOT_SUPPORTED = "BkPlugNotSupported" // # 注意：不支持插件【{0}({1})】的导出
    const val BK_FIND_RECOMMENDED_REPLACEMENT_PLUG = "BkFindRecommendedReplacementPlug" // 请在蓝盾研发商店查找推荐的替换插件！
    const val BK_OLD_PLUG_NOT_SUPPORT = "BkOldPlugNotSupport" // 内置老插件不支持导出，请使用市场插件 ###
    const val BK_VARIABLE_NAME_NOT_UNIQUE = "BkVariableNameNotUnique" // 变量名[{0}]来源不唯一，请修改变量名称或增加插件输出命名空间：
    const val BK_NO_RIGHT_EXPORT_PIPELINE = "BkNoRightExportPipeline" // 用户({0})无权限在工程({1})下导出流水线
    const val BK_PROJECT_ID = "BkProjectId" // # 项目ID:
    const val BK_PIPELINED_ID = "BkPipelinedId" // # 流水线ID:
    const val BK_PIPELINE_NAME = "BkPipelineName" // # 流水线名称:
    const val BK_EXPORT_TIME = "BkExportTime" // # 导出时间:
    const val BK_EXPORT_SYSTEM_CREDENTIALS = "BkExportSystemCredentials" // # 注意：不支持系统凭证(用户名、密码)的导出，请在stream项目设置下重新添加凭据：https://iwiki.woa.com/p/800638064 ！ \n
    const val BK_SENSITIVE_INFORMATION_IN_PARAMETERS = "BkSensitiveInformationInParameters" // # 注意：[插件]输入参数可能存在敏感信息，请仔细检查，谨慎分享！！！ \n
    const val BK_STREAM_NOT_SUPPORT = "BkStreamNotSupport" // # 注意：[插件]Stream不支持蓝盾老版本的插件，请在研发商店搜索新插件替换 \n
    const val BK_PARAMETERS_BE_EXPORTED = "BkParametersBeExported" // # \n# tips：部分参数导出会存在\[该字段限制导出，请手动填写]\,需要手动指定。原因有:\n
    const val BK_IDENTIFIED_SENSITIVE_INFORMATION = "BkIdentifiedSensitiveInformation" // # ①识别出为敏感信息，不支持导出\n
    const val BK_UNKNOWN_CONTEXT_EXISTS = "BkUnknownContextExists" // # ②部分字段校验格式时存在未知上下文，不支持导出\n
    const val BK_AUTOMATIC_EXPORT_NOT_SUPPORTED_IMAGE = "BkAutomaticExportNotSupportedImage" // ### 该镜像暂不支持自动导出，请参考 https://iwiki.woa.com/x/2ebDKw 手动配置 ###
    const val BK_ENTER_URL_ADDRESS_IMAGE = "BkEnterUrlAddressImage" // ###请直接填入镜像(TLinux2.2公共镜像)的URL地址，若存在鉴权请增加 credentials 字段###
    const val BK_ADMINISTRATOR = "BkAdministrator" // 管理员
    const val BK_QUICK_APPROVAL_MOA = "BkQuickApprovalMoa" // 【通过MOA快速审批】
    const val BK_QUICK_APPROVAL_PC = "BkQuickApprovalPc" // 【通过PC快速审批】
    const val BK_NOT_CONFIRMED_CAN_EXECUTED = "BkNotConfirmedCanExecuted" // 插件 {0} 尚未确认是否可以在工蜂CI执行
    const val BK_CONTACT_PLUG_DEVELOPER = "BkContactPlugDeveloper" // ，请联系插件开发者
    const val BK_CHECK_INTEGRITY_YAML = "BkCheckIntegrityYaml" // 请检查YAML的完整性，或切换为研发商店推荐的插件后再导出
    const val BK_BEE_CI_NOT_SUPPORT = "BkBeeCiNotSupport" // 工蜂CI不支持蓝盾老版本插件
    const val BK_SEARCH_STORE = "BkSearchStore" // 请在研发商店搜索新插件替换
    const val BK_NOT_SUPPORT_CURRENT_CONSTRUCTION_MACHINE = "BkNotSupportCurrentConstructionMachine" // # 注意：工蜂CI暂不支持当前类型的构建机
    const val BK_EXPORT = "BkExport" // 的导出,
    const val BK_CHECK_POOL_FIELD = "BkCheckPoolField" // 需检查JOB({0})的Pool字段
    const val BK_CONSTRUCTION_MACHINE_NOT_SUPPORTED = "BkConstructionMachineNotSupported" // # 注意：暂不支持当前类型的构建机
    const val BK_NOT_EXIST_UNDER_NEW_BUSINESS = "BkNotExistUnderNewBusiness" //# 注意：【{0}】的环境【{1}】在新业务下可能不存在，
    const val BK_CHECK_OPERATING_SYSTEM_CORRECT = "BkCheckOperatingSystemCorrect" //请手动修改成存在的环境，并检查操作系统是否正确
    const val BK_NODE_NOT_EXIST_UNDER_NEW_BUSINESS = "BkNodeNotExistUnderNewBusiness" //# 注意：【{0}】的节点【{1}】在新业务下可能不存在，
    const val BK_PLEASE_MANUALLY_MODIFY = "BkPleaseManuallyModify" // 请手动修改成存在的节点
    const val BK_ONLY_VISIBLE_PCG_BUSINESS = "BkOnlyVisiblePcgBusiness" // # 注意：【{0}】仅对PCG业务可见，请检查当前业务是否属于PCG！ \n
    const val BK_WORKER_BEE_CI_NOT_SUPPORT = "BkWorkerBeeCiNotSupport" // # 注意：[插件]工蜂CI不支持依赖蓝盾项目的服务（如凭证、节点等），
    const val BK_MODIFICATION_GUIDELINES = "BkModificationGuidelines" // 请联系插件开发者改造插件，改造指引：https://iwiki.woa.com/x/CqARHg \n
    const val BK_CREATE_SERVICE = "BkCreateService" // 创建{0}服务

    const val BK_CONTAINER_SERVICE = "BkContainerService" // 容器服务
    const val BK_FAILED_BSC_CREATE_PROJECT = "BkFailedBscCreateProject" // 调用BSC接口创建项目失败
    const val BK_FAILED_GET_PAASCC_INFORMATION = "BkFailedGetPaasccInformation" // 获取PAASCC项目信息失败

    const val BK_FILE_CANNOT_EXCEED = "BkFileCannotExceed" // 请求文件不能超过1M
    const val BK_LOCAL_WAREHOUSE_CREATION_FAILED = "BkLocalWarehouseCreationFailed" //工程({0})本地仓库创建失败
    const val BK_TRIGGER_METHOD = "BkTriggerMethod" //触发方式
    const val BK_QUALITY_RED_LINE = "BkQualityRedLine" //质量红线
    const val BK_QUALITY_RED_LINE_OUTPUT = "BkQualityRedLineOutput" //质量红线产出插件
    const val BK_METRIC = "BkMetric" //质量红线产出插件
    const val BK_RESULT = "BkResult" //结果
    const val BK_EXPECT = "BkExpect" //预期

    const val BK_SIGNING_TASK_SIGNATURE_INFORMATION = "BkSigningTaskSignatureInformation" //签名任务签名信息(resignId={0})不存在。
    const val BK_SIGNING_TASK_SIGNATURE_HISTORY  = "BkSigningTaskSignatureHistory" //签名任务签名历史(resignId=${0})不存在。
    const val BK_FAILED_CREATE_DOWNLOAD_CONNECTION  = "BkFailedCreateDownloadConnection" //创建下载连接失败(resignId={0})
    const val BK_FAILED_INSERT  = "BkFailedInsert" //插入entitlement文件({0})的keychain-access-groups失败。
    const val BK_DESCRIPTION_FILE_FOR_CERTIFICATE  = "BkDescriptionFileForCertificate" //未找到证书[{0}]对应的描述文件，返回空值


    const val BK_NO_APIGW_API = "BkNoApigwApi"//Openapi非apigw接口，不需要鉴权。
    const val BK_PRE_ENHANCEMENT = "BkPreEnhancement"//【前置增强】the method
    const val BK_PARAMETER_NAME = "BkParameterName"//参数名
    const val BK_PARAMETER_VALUE = "BkParameterValue"//参数值
    const val BK_REQUEST_TYPE_APIGWTYPE = "BkRequestTypeApigwtype"//请求类型apigwType[{0}],appCode[{1}],项目[{2}]
    const val BK_PERMISSION_FOR_PROJECT = "BkPermissionForProject"//判断！！！！请求类型apigwType[{0}],appCode[{1}],是否有项目[{2}]的权限.
    const val BK_PERMISSION_FOR_PROJECT_VERIFIED = "BkPermissionForProjectVerified"//请求类型apigwType[{0}],appCode[{1}],是否有项目[{2}]的权限【验证通过】
    const val BK_VERIFICATION_FAILED = "BkVerificationFailed"//请求类型apigwType[{0}],appCode[{1}],是否有项目[{2}]的权限【验证失败】
    const val BK_PROJECT_LIST = "BkProjectList"//项目列表

    const val BK_OTHER = "BkOther"//其他
    const val BK_PIPELINED_JOB = "BkPipelinedJob"//流水线Job
    const val BK_IMAGE_STORE_ONLINE = "BkImageStoreOnline"//容器镜像商店上线，历史镜像数据自动生成
    const val BK_OLD_VERSION_BUILD_IMAGE = "BkOldVersionBuildImage"//旧版的构建镜像，通过拷贝为构建镜像入口生成
    const val BK_AUTOMATICALLY_CONVERTED = "BkAutomaticallyConverted"//已自动转换为容器镜像商店数据，请项目管理员在研发商店工作台进行管理。
    const val BK_COPY_FOR_BUILD_IMAGE = "BkCopyForBuildImage"//旧版的构建镜像，通过蓝盾版本仓库“拷贝为构建镜像”入口生成。
    const val BK_AFTER_IMAGE_STORE_ONLINE = "BkAfterImageStoreOnline"//容器镜像商店上线后，旧版入口已下线。因历史原因，此类镜像没有办法对应到实际的镜像推送人，暂时先挂到项目管理员名下。
    const val BK_PROJECT_MANAGER_CAN_OPERATION = "BkProjectManagerCanOperation"//项目管理员可在研发商店工作台进行上架/升级/下架等操作，或者交接给实际负责人进行管理。
    const val BK_HISTORYDATA_DATA = "BkHistorydataData"//historyData数据迁移自动通过
    const val BK_WORKER_BEE_PROJECT_NOT_EXIST = "BkWorkerBeeProjectNotExist"//工蜂项目信息不存在，请检查链接
    const val BK_WORKER_BEE_PROJECT_NOT_STREAM_ENABLED = "BkWorkerBeeProjectNotStreamEnabled"//工蜂项目未开启Stream，请前往仓库的CI/CD进行配置

    const val BK_FAILED_VERIFY_AUTHORITY = "BkFailedVerifyAuthority" //授权人权限校验失败
    const val BK_STREAM_MESSAGE_NOTIFICATION = "BkStreamMessageNotification" //@Stream消息通知
    const val BK_SESSION_ID = "BkSessionId" //会话ID
    const val BK_GROUP_ID = "BkGroupId" //群ID
    const val BK_THIS_GROUP_ID = "BkThisGroupId" //本群ID='{0}'。PS:群ID可用于蓝盾平台上任意企业微信群通知。
    const val BK_NEED_SUPPLEMEN = "BkNeedSupplemen" //对接其他Git平台时需要补充
    const val BK_PULL_CODE = "BkPullCode" //拉代码


    const val BK_GROUP_CHATID = "BkGroupChatid" //本群ChatId
    const val BK_BLUE_SHIELD_DEVOPS_ROBOT = "BkBlueShieldDevopsRobot" //您好，我是蓝盾DevOps机器人，下面是平台相关链接
    const val BK_PLATFORM_ENTRANCE = "BkPlatformEntrance" //平台入口
    const val BK_DOCUMENT_ENTRY = "BkDocumentEntry" //文档入口
    const val BK_CAN_DO_FOLLOWING = "BkCanDoFollowing" //可以进行以下操作
    const val BK_QUERY_PIPELINE_LIST = "BkQueryPipelineList" //查询流水线列表
    const val BK_YOU_CAN_CLICK = "BkYouCanClick" //如有需要可以点击
    const val BK_MANUAL_CUSTOMER_SERVICE = "BkManualCustomerService" //人工客服
    const val BK_GROUP_BOUND_PROJECT = "BkGroupBoundProject" //本群已绑定【{0}】项目，如需修改请点击：
    const val BK_MODIFY_ROJECT = "BkModifyRoject" //修改项目
    const val BK_NOT_EXECUTION_PERMISSION = "BkNotExecutionPermission" //{0}暂时还没有【{1}】流水线的执行权限，请点击申请执行权限：
    const val BK_APPLICATION_ADDRESS = "BkApplicationAddress" //申请地址
    const val BK_PIPELINE_STARTED_SUCCESSFULLY = "BkPipelineStartedSuccessfully" //流水线【{0}】启动成功，{1}可以点击查看
    const val BK_PIPELINE_EXECUTION_DETAILS = "BkPipelineExecutionDetails" //流水线执行详情
    const val BK_FAILED_START_PIPELINE = "BkFailedStartPipeline" //{0}启动流水线【{1}】失败。
    const val BK_THERE_NO_ITEMS_VIEW = "BkThereNoItemsView" //在蓝盾平台DevOps中没有可以查看的项目
    const val BK_ITEMS_CAN_VIEWED = "BkItemsCanViewed" //下面是{0}在蓝盾DevOps平台中可以查看的项目
    const val BK_AUTOMATICALLY_BIND_RELEVANT_PROJECT = "BkAutomaticallyBindRelevantProject" //PS:选择项目后，本群会自动绑定相关的项目,该消息只允许{0}点击执行
    const val BK_CONSULTING_GROUP = "BkConsultingGroup" //蓝盾DevOps平台咨询群
    const val BK_PLEASE_DESCRIBE_YOUR_PROBLEM = "BkPleaseDescribeYourProblem" //请描述您的问题，并带上相关的URL地址
    const val BK_NEW_CONSULTING_GROUP_PULLED_UP = "BkNewConsultingGroupPulledUp" //已为您拉起新的咨询群，请关注会话列表。
    const val BK_NO_PIPELINE_VIEW = "BkNoPipelineView" //{0}在【{1}】项目中没有可以查看的流水线
    const val BK_FOLLOWING_PIPELINE_CAN_VIEW = "BkFollowingPipelineCanView" //下面是{0}在【{1}】项目中可以查看的流水线
    const val BK_EXECUTION = "BkExecution" //执行
    const val BK_MESSAGE_ALLOWS_CLICK = "BkMessageAllowsClick" //该消息只允许{0}点击执行。


    const val BK_CANNING_SENSITIVE_INFORMATION = "BkCanningSensitiveInformation" //开始敏感信息扫描，待排除目录
    const val BK_SENSITIVE_INFORMATION = "BkSensitiveInformation" //敏感信息扫描报告
    const val BK_NO_SENSITIVE_INFORMATION = "BkNoSensitiveInformation" //无敏感信息，无需生成报告
    const val BK_RELATIVE_PATH_KEYSTORE = "BkRelativePathKeystore" //keystore安装相对路径
    const val BK_KEYSTORE_INSTALLED_SUCCESSFULLY = "BkKeystoreInstalledSuccessfully" //Keystore安装成功
    const val BK_FAILED_UPLOAD_BUGLY_FILE = "BkFailedUploadBuglyFile" //上传bugly文件失败
    const val BK_FAILED_GET_BUILDER_INFORMATION  = "BkFailedGetBuilderInformation" //获取构建机基本信息失败
    const val BK_FAILED_GET_WORKER_BEE = "BkFailedGetWorkerBee" //获取工蜂CI项目Token失败！
    const val BK_FAILED_GET_PLUG = "BkFailedGetPlug" //获取插件执行环境信息失败
    const val BK_FAILED_UPDATE_PLUG = "BkFailedUpdatePlug" //更新插件执行环境信息失败
    const val BK_FAILED_SENSITIVE_INFORMATION = "BkFailedSensitiveInformation" //获取插件敏感信息失败
    const val BK_FAILED_ENVIRONMENT_VARIABLE_INFORMATION = "BkFailedEnvironmentVariableInformation" //获取插件开发语言相关的环境变量信息失败
    const val BK_FAILED_ADD_INFORMATION = "BkFailedAddInformation" //添加插件对接平台信息失败
    const val BK_ARCHIVE_PLUG_FILES = "BkArchivePlugFiles" //归档插件文件
    const val BK_FAILED_IOS_CERTIFICATE = "BkFailedIosCertificate" //获取IOS证书失败
    const val BK_FAILED_ANDROID_CERTIFICATE = "BkFailedAndroidCertificate" //获取Android证书失败
    const val BK_ENTERPRISE_SIGNATURE_FAILED = "BkEnterpriseSignatureFailed" //企业签名失败
}
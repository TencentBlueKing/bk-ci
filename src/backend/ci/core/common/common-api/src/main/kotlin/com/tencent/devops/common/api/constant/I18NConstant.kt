package com.tencent.devops.common.api.constant

object I18NConstant {
    //public
/*    const val BK_FILE_NAME = "bkFileName" //文件名
    const val BK_BELONG_TO_THE_PROJECT = "bkBelongToTheProject" //所属项目
    const val BK_OPERATING = "bkOperating" //操作
    const val BK_PUSH_FROM_BLUE_SHIELD_DEVOPS_PLATFORM = "bkPushFromBlueShieldDevopsPlatform" //来自蓝盾DevOps平台的推送
    const val BK_TABLE_CONTENTS = "bkTableContents" //表格内容
    const val BK_PLEASE_FEEL_TO_CONTACT_BLUE_SHIELD_ASSISTANT = "bkPleaseFeelToContactBlueShieldAssistant" //如有任何问题，可随时联系蓝盾助手
    const val BK_ETH1_NETWORK_CARD_IP_EMPTY = "bkEth1NetworkCardIpEmpty" //eth1 网卡Ip为空，因此，获取eth0的网卡ip
    const val BK_LOOPBACK_ADDRESS_OR_NIC_EMPTY = "bkLoopbackAddressOrNicEmpty" //loopback地址或网卡名称为空
    const val BK_FAILED_GET_NETWORK_CARD = "bkFailedGetNetworkCard" //获取网卡失败
    const val BK_MANUAL_TRIGGER = "bkManualTrigger" //手动触发
    const val BK_BUILD_TRIGGER = "bkBuildTrigger" //构建触发
    const val BK_VIEW_DETAILS = "bkSeeDetails" //查看详情
    const val BK_PROJECT_ID = "bkProjectId" //# 项目ID:
    const val BK_PIPELINE_NAME = "bkPipelineName" //# 流水线名称:
    const val BK_CREATE_SERVICE = "bkCreateService" //创建{0}服务
    const val BK_SESSION_ID = "bkSessionId" //会话ID
    const val BK_GROUP_ID = "bkGroupId" //群ID
    const val BK_THIS_GROUP_ID = "bkThisGroupId" //本群ID='{0}'。PS:群ID可用于蓝盾平台上任意企业微信群通知。*/

    //artifactory
/*    const val BK_BLUE_SHIELD_SHARE_FILES_WITH_YOU = "bkBlueShieldShareFilesWithYou" //【蓝盾版本仓库通知】{0}与你共享{1}文件
    const val BK_BLUE_SHIELD_SHARE_AND_OTHER_FILES_WITH_YOU = "bkBlueShieldShareAndOtherFilesWithYou" //【蓝盾版本仓库通知】{0}与你共享{1}等{2}个文件
    const val BK_SHARE_FILES_PLEASE_DOWNLOAD_FILES_IN_TIME = "bkShareFilesPleaseDownloadFilesInTime" //{0}与你共享以下文件，请在有效期（{1}}天）内及时下载：
    const val BK_DOWNLOAD = "bkDownload" //下载
    const val BK_RECEIVED_THIS_EMAIL_BECAUSE_YOU_FOLLOWED_PROJECT = "bkbkReceivedThisEmailBecauseYouFollowedProject" //你收到此邮件，是因为你关注了 {0} 项目，或其它人@了你
    const val BK_ILLEGAL_PATH = "bkIllegalPath" //非法路径*/

    //common
  /*  const val BK_CONTAINER_TIMED_OUT = "bkContainerTimedOut" //创建容器超时
    const val BK_CREATION_FAILED_EXCEPTION_INFORMATION = "bkCreationFailedExceptionInformation" //创建失败，异常信息
    const val BK_BLUE_SHIELD_PUBLIC_BUILD_RESOURCES = "bkBlueShieldPublicBuildResources" //蓝盾公共构建资源
    const val BK_BLUE_SHIELD_PUBLIC_BUILD_RESOURCES_NEW = "bkBlueShieldPublicBuildResourcesNew" //蓝盾公共构建资源(NEW)
    const val BK_PUBLIC_DOCKER_ON_DEVNET_PHYSICAL = "bkPublicDockerOnDevnetPhysical" //公共：Docker on Devnet 物理机
    const val BK_PUBLIC_DOCKER_ON_DEVCLOUD = "bkPublicDockerOnDevcloud" //公共：Docker on DevCloud
    const val BK_PUBLIC_DOCKER_ON_BCS = "bkPublicDockerOnBcs" //公共：Docker on Bcs
    const val BK_PRIVATE_SINGLE_BUIL_MACHINE = "bkPrivateSingleBuilMachine" //私有：单构建机
    const val BK_PRIVATE_BUILD_A_CLUSTER = "bkPrivateBuildACluster" //私有：构建集群
    const val BK_PCG_PUBLIC_BUILD_RESOURCES = "bkPcgPublicBuildResources" //PCG公共构建资源
    const val BK_TENCENT_SELF_DEVELOPED_CLOUD= "bkTencentSelfDevelopedCloud" //腾讯自研云（云devnet资源）
    const val BK_CLOUD_HOSTING_WINDOWS_ON_DEVCLOUD = "bkCloudHostingWindowsOnDevcloud" //云托管：Windows on DevCloud*/

    //dockerhost
   /* const val BK_DOCKER_BUILDER_RUNS_TOO_MANY = "bkDockerBuilderRunsTooMany" //Docker构建机运行的容器太多，母机IP:{0}，容器数量: {1}
    const val BK_BUILD_ENVIRONMENT_STARTS_SUCCESSFULLY = "bkBuildEnvironmentStartsSuccessfully" //构建环境启动成功，等待Agent启动...
    const val BK_FAILED_TO_START_IMAGE_NOT_EXIST = "bkFailedToStartImageNotExist" //构建环境启动失败，镜像不存在, 镜像:{0}
    const val BK_FAILED_TO_START_ERROR_MESSAGE = "bkFailedToStartErrorMessage" //构建环境启动失败，错误信息*/

    //environment
/*    const val BK_NORMAL_VERSION = "bkNormalVersion" //8核16G（普通版）
    const val BK_INTEL_XEON_SKYLAKE_PROCESSOR = "bkIntelXeonSkylakeProcessor" //2.5GHz 64核 Intel Xeon Skylake 6133处理器
    const val BK_MEMORY = "bkMemory" //32GB*12 DDR3 内存
    const val BK_SOLID_STATE_DISK = "bkSolidStateDisk" //{0}GB 固态硬盘
    const val BK_ESTIMATED_DELIVERY_TIME = "bkEstimatedDeliveryTime" //预计交付周期：{0}分钟
    const val BK_HIGH_END_VERSION = "bkHighEndVersion" //32核64G（高配版）*/

    //experience
    /*const val BK_UPDATED_SUCCESSFULLY_AND_SET = "bkUpdatedSuccessfullyAndSet" //更新成功,已置为
    const val BK_UPDATED_SUCCESSFULLY = "bkUpdatedSuccessfully" //更新成功
    const val BK_NEW_SEARCH_RECOMMENDATION_SUCCEEDED = "bkNewSearchRecommendationSucceeded" //新增搜索推荐成功
    const val BK_DELETE_SEARCH_RECOMMENDATION_SUCCEEDED = "bkDeleteSearchRecommendationSucceeded" //删除搜索推荐成功
    const val BK_CREATED_SUCCESSFULLY = "bkCreatedSuccessfully" //创建成功
    const val BK_NO_EXPERIENCE_UNDER_PROJECT = "bkNoExperienceUnderProject" //{0} 项目下无体验
    const val BK_NO_EXPERIENCE = "bkNoExperience" //无体验
    const val BK_NO_EXPERIENCE_GROUP_UNDER_PROJECT = "bkNoExperienceGroupUnderProject" //{0} 项目下无体验组
    const val BK_NO_EXPERIENCE_USER_GROUP = "bkNoExperienceUserGroup" //无体验用户组
    const val BK_NO_EXPERIENCE_USER_GROUP_UNDER_PROJECT= "bkNoExperienceUserGroupUnderProject" //{0} 项目下无体验用户组
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
    const val BK_LATEST_EXPERIENCE_VERSION_INFO = "bkLatestExperienceVersionInfo" //【{0}】发布了最新体验版本，【{1}-{2}】诚邀您参与体验。\nPC体验地址：{3}\n手机体验地址：{4}*/

    //external
    /*const val BK_FAILED_GET_GITHUB_ACCESS_TOKEN = "bkFailedGetGithubAccessToken" //获取Github access_token失败
    const val BK_ADD_DETECTION_TASK = "bkAddDetectionTask" //添加检测任务
    const val BK_UPDATE_DETECTION_TASK = "bkUpdateDetectionTask" //更新检测任务
    const val BK_GET_WAREHOUSE_LIST = "bkGetWarehouseList" //获取仓库列表
    const val BK_GET_SPECIFIED_BRANCH = "bkGetSpecifiedBranch" //获取指定分支
    const val BK_GET_SPECIFIED_TAG = "bkGetSpecifiedTag" //获取指定Tag
    const val BK_GET_LIST_OF_BRANCHES = "bkGetListOfBranches" //获取分支列表
    const val BK_GET_TAG_LIST = "bkGetTagList" //获取Tag列表*/

    //image
    /*const val BK_SOURCE_IMAGE = "bkSourceImage" //源镜像：{0}
    const val BK_TARGET_IMAGE = "bkTargetImage" //目标镜像：{0}:{1}
    const val BK_SUCCESSFUL_REGISTRATION_IMAGE = "bkSuccessfulRegistrationImage" //注册镜像成功
    const val BK_FAILED_REGISTER_IMAGE = "bkFailedRegisterImage" //注册镜像失败，错误信息：*/

    //log
  /*  const val BK_FAILED_INSERT_DATA = "bkFailedInsertData" //蓝盾ES集群插入数据失败
    const val BK_ES_CLUSTER_RECOVERY = "bkEsClusterRecovery" //蓝盾ES集群恢复
    const val BK_FAILURE = "bkFailure" //失效
    const val BK_RECOVERY = "bkRecovery" //恢复
    const val BK_ES_CLUSTER_STATUS_ALARM_NOTIFICATION = "bkEsClusterStatusAlarmNotification" //【ES集群状态告警通知】
    const val BK_NOTIFICATION_PUSH_FROM_BKDEVOP = "bkNotificationPushFrombkdevop" //来自bkDevOps/蓝盾DevOps平台的通知推送
    const val BK_CLUSTER_NAME = "bkClusterName" //集群名称
    const val BK_STATUS = "bkStatus" //状态
    const val BK_EMPTY_DATA = "bkEmptyData" //空数据
    const val BK_LOOK_FORWARD_IT = "bkLookForwardIt" //敬请期待！
    const val BK_CONTACT_BLUE_SHIELD_ASSISTANT = "bkContactBlueShieldAssistant" //如有任何问题，可随时联系蓝盾助手。
    const val BK_HEAD_OF_BLUE_SHIELD_LOG_MANAGEMENT = "bkHeadOfBlueShieldLogManagement" //你收到此邮件，是因为你是蓝盾日志管理负责人*/

    //monitoring
    /*const val BK_ILLEGAL_TIMESTAMP_RANGE = "bkIllegalTimestampRange" //非法时间戳范围
    const val BK_ILLEGAL_ENTERPRISE_GROUP_ID = "bkIllegalEnterpriseGroupId" //非法事业群ID
    const val BK_INCORRECT_PASSWORD = "bkIncorrectPassword" //密码错误
    const val BK_SENT_SUCCESSFULLY = "bkSentSuccessfully" //发送成功
    const val BK_WARNING_MESSAGE_FROM_GRAFANA = "bkWarningMessageFromGrafana" //来自Grafana的预警信息
    const val BK_MONITORING_OBJEC = "bkMonitoringObjec" //监控对象：{0}，当前值为：{1}；
    const val BK_SEND_MONITORING_MESSAGES = "bkSendMonitoringMessages" //只有处于alerting告警状态的信息才发送监控消息
*/
    //notify
   /* const val BK_CONTROL_MESSAGE_LENGTH = "bkControlMessageLength" //...(消息长度超{0} 已截断,请控制消息长度)
    const val BK_LINE_BREAKS_WILL_ESCAPED = "bkLineBreaksWillEscaped" //(注意: 换行会被转义为\\n)
    const val BK_DESIGNATED_APPROVER_APPROVAL = "bkDesignatedApproverApproval" //指定审批人审批*/

    //plugin
/*    const val BK_APP_SCAN_COMPLETED = "bkAppScanCompleted" //金刚app扫描完成
    const val BK_BUILDID_NOT_FOUND = "bkBuildidNotFound" //服务端内部异常，buildId={0}的构建未查到
    const val BK_PIPELINEID_NOT_FOUND = "bkPipelineidNotFound" //服务端内部异常，pipelineId={0}的构建未查到*/


    //prebuild
/*    const val BK_AGENT_NOT_INSTALLED = "bkAgentNotInstalled" //Agent未安装，请安装Agent.
    const val BK_ILLEGAL_YAML = "bkIllegalYaml" //YAML非法:
    const val BK_NO_COMPILATION_ENVIRONMENT = "bkNoCompilationEnvironment" //无编译环境
    const val BK_TBUILD_ENVIRONMENT_LINUX = "bkTbuildEnvironmentLinux" //构建环境-LINUX
    const val BK_SYNCHRONIZE_LOCAL_CODE = "bkSynchronizeLocalCode" //同步本地代码*/

    //process
    /*const val BK_SUCCESSFULLY_DISTRIBUTED = "bkSuccessfullyDistributed" //跨项目构件分发成功，共分发了{0}个文件
    const val BK_SUCCESSFULLY_FAILED = "bkSuccessfullyFailed" //跨项目构件分发失败，
    const val BK_NO_MATCH_FILE_DISTRIBUTE = "bkNoMatchFileDistribute" //匹配不到待分发的文件: {0}
    const val BK_START_PERFORMING_GCLOUD_OPERATION = "bkStartPerformingGcloudOperation" //开始对文件（{0}）执行Gcloud相关操作，详情请去gcloud官方地址查看：
    const val BK_START_UPLOAD_OPERATION = "bkStartUploadOperation" //开始执行 \"上传动态资源版本\" 操作
    const val BK_OPERATION_PARAMETERS = "bkOperationParameters" //\"上传动态资源版本\" 操作参数：
    const val BK_QUERY_VERSION_UPLOAD = "bkQueryVersionUpload" //开始执行 \"查询版本上传 CDN 任务状态\" 操作\n
    const val BK_WAIT_QUERY_VERSION = "bkWaitQueryVersion" //\"等待查询版本上传 CDN 任务状态\" 操作执行完毕: \n
    const val BK_OPERATION_COMPLETED_SUCCESSFULLY = "bkOperationCompletedSuccessfully" //\"查询版本上传 CDN 任务状态\" 操作 成功执行完毕\n
    const val BK_FAILED_UPLOAD_FILE = "bkFailedUploadFile" //上传文件失败:
    const val BK_CREATE_RESOURCE_OPERATION = "bkCreateResourceOperation" //开始执行 \"创建资源\" 操作\n
    const val BK_CREATE_RESOURCES_OPERATION_PARAMETERS = "bkCreateResourcesOperationParameters" //\"创建资源\" 操作参数：
    const val BK_START_RELEASE_OPERATION = "bkStartReleaseOperation" //开始执行 \"预发布\" 操作\n
    const val BK_RESPONSE_RESULT = "bkResponseResult" //预发布单个或多个渠道响应结果:
    const val BK_RECIPIENT_EMPTY = "bkRecipientEmpty" //收件人为空
    const val BK_EMAIL_NOTIFICATION_CONTENT_EMPTY = "bkEmailNotificationContentEmpty" //邮件通知内容为空
    const val BK_MESSAGE_SUBJECT_EMPTY = "bkMessageSubjectEmpty" //邮件主题为空
    const val BK_EXPERIENCE_PATH_EMPTY = "bkExperiencePathEmpty" //体验路径为空
    const val BK_INCORRECT_NOTIFICATION_METHOD = "bkIncorrectNotificationMethod" //通知方式不正确
    const val BK_FILE_NOT_EXIST = "bkFileNotExist" //文件({0})不存在
    const val BK_VERSION_EXPERIENCE_CREATED_SUCCESSFULLY = "bkVersionExperienceCreatedSuccessfully" //版本体验({0})创建成功
    const val BK_VIEW_RESULT = "bkViewResult" //查看结果:
    const val BK_RECEIVER_EMPTY = "bkReceiverEmpty" //Message Receivers is empty(接收人为空)
    const val BK_MESSAGE_CONTENT_EMPTY = "bkMessageContentEmpty" //Message Body is empty(消息内容为空)
    const val BK_EMPTY_TITLE = "bkEmptyTitle" //Message Title is empty(标题为空)
    const val BK_COMPUTER_VIEW_DETAILS = "bkComputerViewDetails" //{0}\n\n电脑查看详情：{1}\n手机查看详情：{2}
    const val BK_SEND_WECOM_MESSAGE = "bkSendWecomMessage" //send enterprise wechat message(发送企业微信消息):\n{0}\nto\n{1}
    const val BK_INVALID_NOTIFICATION_RECIPIENT = "bkInvalidNotificationRecipient" //通知接收者不合法:
    const val BK_WECOM_NOTICE = "bkWecomNotice" //企业微信通知内容:
    const val BK_SEND_WECOM_CONTENT = "bkSendWecomContent" //发送企业微信内容: ({0}) 到 {1}
    const val BK_SEND_WECOM_CONTENT_SUCCESSFULLY = "bkSendWecomContentSuccessfully" //发送企业微信内容: ({0}) 到 {1}成功
    const val BK_SEND_WECOM_CONTENT_FAILED = "bkSendWecomContentFailed" //发送企业微信内容: ({0}) 到 {1}失败:
    const val BK_MATCHING_FILE = "bkMatchingFile" //匹配文件中:
    const val BK_UPLOAD_CORRESPONDING_FILE = "bkUploadCorrespondingFile" //上传对应文件到织云成功!
    const val BK_START_UPLOADING_CORRESPONDING_FILES = "bkStartUploadingCorrespondingFiles" //开始上传对应文件到织云...
    const val BK_PULL_GIT_WAREHOUSE_CODE = "bkPullGitWarehouseCode" //拉取Git仓库代码
    const val BK_AUTOMATIC_EXPORT_NOT_SUPPORTED = "bkAutomaticExportNotSupported" //### 该环境不支持自动导出，请参考 https://iwiki.woa.com/x/2ebDKw 手动配置 ###
    const val BK_BUILD_CLUSTERS_THROUGH = "bkBuildClustersThrough" //### 可以通过 runs-on: macos-10.15 使用macOS公共构建集群。
    const val BK_NOTE_DEFAULT_XCODE_VERSION = "bkNoteDefaultXcodeVersion" //注意默认的Xcode版本为12.2，若需自定义，请在JOB下自行执行 xcode-select 命令切换 ###
    const val BK_PLEASE_USE_STAGE_AUDIT = "bkPleaseUseStageAudit" //人工审核插件请改用Stage审核 ###
    const val BK_PLUG_NOT_SUPPORTED = "bkPlugNotSupported" //# 注意：不支持插件【{0}({1})】的导出
    const val BK_FIND_RECOMMENDED_REPLACEMENT_PLUG = "bkFindRecommendedReplacementPlug" //请在蓝盾研发商店查找推荐的替换插件！
    const val BK_OLD_PLUG_NOT_SUPPORT = "bkOldPlugNotSupport" //内置老插件不支持导出，请使用市场插件 ###
    const val BK_NO_RIGHT_EXPORT_PIPELINE = "bkNoRightExportPipeline" //用户({0})无权限在工程({1})下导出流水线
    const val BK_PIPELINED_ID = "bkPipelinedId" //# 流水线ID:
    const val BK_EXPORT_TIME = "bkExportTime" //# 导出时间:
    const val BK_EXPORT_SYSTEM_CREDENTIALS = "bkExportSystemCredentials" //# 注意：不支持系统凭证(用户名、密码)的导出，请在stream项目设置下重新添加凭据：https://iwiki.woa.com/p/800638064 ！ \n
    const val BK_SENSITIVE_INFORMATION_IN_PARAMETERS = "bkSensitiveInformationInParameters" //# 注意：[插件]输入参数可能存在敏感信息，请仔细检查，谨慎分享！！！ \n
    const val BK_STREAM_NOT_SUPPORT = "bkStreamNotSupport" //# 注意：[插件]Stream不支持蓝盾老版本的插件，请在研发商店搜索新插件替换 \n
    const val BK_PARAMETERS_BE_EXPORTED = "bkParametersBeExported" //# \n# tips：部分参数导出会存在\[该字段限制导出，请手动填写]\,需要手动指定。原因有:\n
    const val BK_IDENTIFIED_SENSITIVE_INFORMATION = "bkIdentifiedSensitiveInformation" //# ①识别出为敏感信息，不支持导出\n
    const val BK_UNKNOWN_CONTEXT_EXISTS = "bkUnknownContextExists" //# ②部分字段校验格式时存在未知上下文，不支持导出\n
    const val BK_AUTOMATIC_EXPORT_NOT_SUPPORTED_IMAGE = "bkAutomaticExportNotSupportedImage" //### 该镜像暂不支持自动导出，请参考 https://iwiki.woa.com/x/2ebDKw 手动配置 ###
    const val BK_ENTER_URL_ADDRESS_IMAGE = "bkEnterUrlAddressImage" //###请直接填入镜像(TLinux2.2公共镜像)的URL地址，若存在鉴权请增加 credentials 字段###
    const val BK_ADMINISTRATOR = "bkAdministrator" //管理员
    const val BK_QUICK_APPROVAL_MOA = "bkQuickApprovalMoa" //【通过MOA快速审批】
    const val BK_QUICK_APPROVAL_PC = "bkQuickApprovalPc" //【通过PC快速审批】
    const val BK_NOT_CONFIRMED_CAN_EXECUTED = "bkNotConfirmedCanExecuted" //插件 {0} 尚未确认是否可以在工蜂CI执行
    const val BK_CONTACT_PLUG_DEVELOPER = "bkContactPlugDeveloper" //，请联系插件开发者
    const val BK_CHECK_INTEGRITY_YAML = "bkCheckIntegrityYaml" //请检查YAML的完整性，或切换为研发商店推荐的插件后再导出
    const val BK_BEE_CI_NOT_SUPPORT = "bkBeeCiNotSupport" //工蜂CI不支持蓝盾老版本插件
    const val BK_SEARCH_STORE = "bkSearchStore" //请在研发商店搜索新插件替换
    const val BK_NOT_SUPPORT_CURRENT_CONSTRUCTION_MACHINE = "bkNotSupportCurrentConstructionMachine" //# 注意：工蜂CI暂不支持当前类型的构建机
    const val BK_EXPORT = "bkExport" //的导出,
    const val BK_CHECK_POOL_FIELD = "bkCheckPoolField" //需检查JOB({0})的Pool字段
    const val BK_CONSTRUCTION_MACHINE_NOT_SUPPORTED = "bkConstructionMachineNotSupported" //# 注意：暂不支持当前类型的构建机
    const val BK_NOT_EXIST_UNDER_NEW_BUSINESS = "bkNotExistUnderNewBusiness" //# 注意：【{0}】的环境【{1}】在新业务下可能不存在，
    const val BK_CHECK_OPERATING_SYSTEM_CORRECT = "bkCheckOperatingSystemCorrect" //请手动修改成存在的环境，并检查操作系统是否正确
    const val BK_NODE_NOT_EXIST_UNDER_NEW_BUSINESS = "bkNodeNotExistUnderNewBusiness" //# 注意：【{0}】的节点【{1}】在新业务下可能不存在，
    const val BK_PLEASE_MANUALLY_MODIFY = "bkPleaseManuallyModify" //请手动修改成存在的节点
    const val BK_ONLY_VISIBLE_PCG_BUSINESS = "bkOnlyVisiblePcgBusiness" //# 注意：【{0}】仅对PCG业务可见，请检查当前业务是否属于PCG！ \n
    const val BK_WORKER_BEE_CI_NOT_SUPPORT = "bkWorkerBeeCiNotSupport" //# 注意：[插件]工蜂CI不支持依赖蓝盾项目的服务（如凭证、节点等），
    const val BK_MODIFICATION_GUIDELINES = "bkModificationGuidelines" //请联系插件开发者改造插件，改造指引：https://iwiki.woa.com/x/CqARHg \n*/

    //project
   /* const val BK_CONTAINER_SERVICE = "bkContainerService" //容器服务
    const val BK_FAILED_BSC_CREATE_PROJECT = "bkFailedBscCreateProject" //调用BSC接口创建项目失败
    const val BK_FAILED_GET_PAASCC_INFORMATION = "bkFailedGetPaasccInformation" //获取PAASCC项目信息失败*/

    //scm
    /*const val BK_FILE_CANNOT_EXCEED = "bkFileCannotExceed" //请求文件不能超过1M
    const val BK_LOCAL_WAREHOUSE_CREATION_FAILED = "bkLocalWarehouseCreationFailed" //工程({0})本地仓库创建失败
    const val BK_TRIGGER_METHOD = "bkTriggerMethod" //触发方式
    const val BK_QUALITY_RED_LINE = "bkQualityRedLine" //质量红线
    const val BK_QUALITY_RED_LINE_OUTPUT = "bkQualityRedLineOutput" //质量红线产出插件
    const val BK_RESULT = "bkResult" //结果
    const val BK_EXPECT = "bkExpect" //预期*/


    //sign
  /*  const val BK_SIGNING_TASK_SIGNATURE_INFORMATION = "bkSigningTaskSignatureInformation" //签名任务签名信息(resignId={0})不存在。
    const val BK_SIGNING_TASK_SIGNATURE_HISTORY = "bkSigningTaskSignatureHistory" //签名任务签名历史(resignId={0})不存在。
    const val BK_FAILED_CREATE_DOWNLOAD_CONNECTION = "bkFailedCreateDownloadConnection" //创建下载连接失败(resignId={0})
    const val BK_FAILED_INSERT = "bkFailedInsert" //插入entitlement文件({0})的keychain-access-groups失败。
    const val BK_DESCRIPTION_FILE_FOR_CERTIFICATE = "bkDescriptionFileForCertificate" //未找到证书[{0}]对应的描述文件，返回空值*/


    //statistics
   /* const val BK_NO_APIGW_API = "bkNoApigwApi" //Openapi非apigw接口，不需要鉴权。
    const val BK_PRE_ENHANCEMENT = "bkPreEnhancement" //【前置增强】the method
    const val BK_PARAMETER_NAME = "bkParameterName" //参数名
    const val BK_PARAMETER_VALUE = "bkParameterValue" //参数值
    const val BK_REQUEST_TYPE_APIGWTYPE = "bkRequestTypeApigwtype" //请求类型apigwType[{0}],appCode[{1}],项目[{2}]
    const val BK_PERMISSION_FOR_PROJECT = "bkPermissionForProject" //判断！！！！请求类型apigwType[{0}],appCode[{1}],是否有项目[{2}]的权限.
    const val BK_PERMISSION_FOR_PROJECT_VERIFIED = "bkPermissionForProjectVerified" //请求类型apigwType[{0}],appCode[{1}],是否有项目[{2}]的权限【验证通过】
    const val BK_VERIFICATION_FAILED = "bkVerificationFailed" //请求类型apigwType[{0}],appCode[{1}],是否有项目[{2}]的权限【验证失败】
    const val BK_PROJECT_LIST = "bkProjectList" //项目列表*/


    //store
/*    const val BK_OTHER = "bkOther" //其他
    const val BK_PIPELINED_JOB = "bkPipelinedJob" //流水线Job
    const val BK_IMAGE_STORE_ONLINE = "bkImageStoreOnline" //容器镜像商店上线，历史镜像数据自动生成
    const val BK_OLD_VERSION_BUILD_IMAGE = "bkOldVersionBuildImage" //旧版的构建镜像，通过拷贝为构建镜像入口生成
    const val BK_AUTOMATICALLY_CONVERTED = "bkAutomaticallyConverted" //已自动转换为容器镜像商店数据，请项目管理员在研发商店工作台进行管理。
    const val BK_COPY_FOR_BUILD_IMAGE = "bkCopyForBuildImage" //旧版的构建镜像，通过蓝盾版本仓库“拷贝为构建镜像”入口生成。
    const val BK_AFTER_IMAGE_STORE_ONLINE = "bkAfterImageStoreOnline" //容器镜像商店上线后，旧版入口已下线。因历史原因，此类镜像没有办法对应到实际的镜像推送人，暂时先挂到项目管理员名下。
    const val BK_PROJECT_MANAGER_CAN_OPERATION = "bkProjectManagerCanOperation" //项目管理员可在研发商店工作台进行上架/升级/下架等操作，或者交接给实际负责人进行管理。
    const val BK_HISTORYDATA_DATA = "bkHistorydataData" //historyData数据迁移自动通过
    const val BK_WORKER_BEE_PROJECT_NOT_EXIST = "bkWorkerBeeProjectNotExist" //工蜂项目信息不存在，请检查链接
    const val BK_WORKER_BEE_PROJECT_NOT_STREAM_ENABLED = "bkWorkerBeeProjectNotStreamEnabled" //工蜂项目未开启Stream，请前往仓库的CI/CD进行配置*/


    //stream
 /*   const val BK_FAILED_VERIFY_AUTHORITY = "bkFailedVerifyAuthority" //授权人权限校验失败
    const val BK_STREAM_MESSAGE_NOTIFICATION = "bkStreamMessageNotification" //@Stream消息通知
    const val BK_NEED_SUPPLEMEN = "bkNeedSupplemen" //对接其他Git平台时需要补充
    const val BK_PULL_CODE = "bkPullCode" //拉代码*/


    //support
    /*const val BK_GROUP_CHATID = "bkGroupChatid" //本群ChatId
    const val BK_BLUE_SHIELD_DEVOPS_ROBOT = "bkBlueShieldDevopsRobot" //您好，我是蓝盾DevOps机器人，下面是平台相关链接
    const val BK_PLATFORM_ENTRANCE = "bkPlatformEntrance" //平台入口
    const val BK_DOCUMENT_ENTRY = "bkDocumentEntry" //文档入口
    const val BK_CAN_DO_FOLLOWING = "bkCanDoFollowing" //可以进行以下操作
    const val BK_QUERY_PIPELINE_LIST = "bkQueryPipelineList" //查询流水线列表
    const val BK_YOU_CAN_CLICK = "bkYouCanClick" //如有需要可以点击
    const val BK_MANUAL_CUSTOMER_SERVICE = "bkManualCustomerService" //人工客服
    const val BK_GROUP_BOUND_PROJECT = "bkGroupBoundProject" //本群已绑定【{0}】项目，如需修改请点击：
    const val BK_MODIFY_ROJECT = "bkModifyRoject" //修改项目
    const val BK_NOT_EXECUTION_PERMISSION = "bkNotExecutionPermission" //{0}暂时还没有【{1}】流水线的执行权限，请点击申请执行权限：
    const val BK_APPLICATION_ADDRESS = "bkApplicationAddress" //申请地址
    const val BK_PIPELINE_STARTED_SUCCESSFULLY = "bkPipelineStartedSuccessfully" //流水线【{0}】启动成功，{1}可以点击查看
    const val BK_PIPELINE_EXECUTION_DETAILS = "bkPipelineExecutionDetails" //流水线执行详情
    const val BK_FAILED_START_PIPELINE = "bkFailedStartPipeline" //{0}启动流水线【{1}】失败。
    const val BK_THERE_NO_ITEMS_VIEW = "bkThereNoItemsView" //在蓝盾平台DevOps中没有可以查看的项目
    const val BK_ITEMS_CAN_VIEWED = "bkItemsCanViewed" //下面是{0}在蓝盾DevOps平台中可以查看的项目
    const val BK_AUTOMATICALLY_BIND_RELEVANT_PROJECT = "bkAutomaticallyBindRelevantProject" //PS:选择项目后，本群会自动绑定相关的项目,该消息只允许{0}点击执行
    const val BK_CONSULTING_GROUP = "bkConsultingGroup" //蓝盾DevOps平台咨询群
    const val BK_PLEASE_DESCRIBE_YOUR_PROBLEM = "bkPleaseDescribeYourProblem" //请描述您的问题，并带上相关的URL地址
    const val BK_NEW_CONSULTING_GROUP_PULLED_UP = "bkNewConsultingGroupPulledUp" //已为您拉起新的咨询群，请关注会话列表。
    const val BK_NO_PIPELINE_VIEW = "bkNoPipelineView" //{0}在【{1}】项目中没有可以查看的流水线
    const val BK_FOLLOWING_PIPELINE_CAN_VIEW = "bkFollowingPipelineCanView" //下面是{0}在【{1}】项目中可以查看的流水线
    const val BK_EXECUTION = "bkExecution" //执行
    const val BK_MESSAGE_ALLOWS_CLICK = "bkMessageAllowsClick" //该消息只允许{0}点击执行。*/


    //worker
    //const val BK_CANNING_SENSITIVE_INFORMATION = "bkCanningSensitiveInformation" //开始敏感信息扫描，待排除目录
    //const val BK_SENSITIVE_INFORMATION = "bkSensitiveInformation" //敏感信息扫描报告
    //const val BK_NO_SENSITIVE_INFORMATION = "bkNoSensitiveInformation" //无敏感信息，无需生成报告
    //const val BK_RELATIVE_PATH_KEYSTORE = "bkRelativePathKeystore" //keystore安装相对路径
    //const val BK_KEYSTORE_INSTALLED_SUCCESSFULLY = "bkKeystoreInstalledSuccessfully" //Keystore安装成功
    //const val BK_FAILED_UPLOAD_BUGLY_FILE = "bkFailedUploadBuglyFile" //上传bugly文件失败
    //const val BK_FAILED_GET_BUILDER_INFORMATION = "bkFailedGetBuilderInformation" //获取构建机基本信息失败
    //const val BK_FAILED_GET_WORKER_BEE = "bkFailedGetWorkerBee" //获取工蜂CI项目Token失败！
    //const val BK_FAILED_GET_PLUG = "bkFailedGetPlug" //获取插件执行环境信息失败
    //const val BK_FAILED_UPDATE_PLUG = "bkFailedUpdatePlug" //更新插件执行环境信息失败
    //const val BK_FAILED_SENSITIVE_INFORMATION = "bkFailedSensitiveInformation" //获取插件敏感信息失败
    //const val BK_FAILED_ENVIRONMENT_VARIABLE_INFORMATION = "bkFailedEnvironmentVariableInformation" //获取插件开发语言相关的环境变量信息失败
    //const val BK_FAILED_ADD_INFORMATION = "bkFailedAddInformation" //添加插件对接平台信息失败
    //const val BK_ARCHIVE_PLUG_FILES = "bkArchivePlugFiles" //归档插件文件
    //const val BK_FAILED_IOS_CERTIFICATE = "bkFailedIosCertificate" //获取IOS证书失败
    //const val BK_FAILED_ANDROID_CERTIFICATE = "bkFailedAndroidCertificate" //获取Android证书失败
    //const val BK_ENTERPRISE_SIGNATURE_FAILED = "bkEnterpriseSignatureFailed" //企业签名失败


    //dispatch-devcloud
    const val BK_FAILED_START_DEVCLOUD = "bkFailedStartDevcloud" //启动DevCloud构建容器失败，请联系devopsHelper反馈处理.
    const val BK_CONTAINER_BUILD_EXCEPTIONS = "bkContainerBuildExceptions" //容器构建异常请参考
    const val BK_DEVCLOUD_EXCEPTION = "bkDevcloudException" //第三方服务-DEVCLOUD 异常，请联系O2000排查，异常信息 -
    const val BK_INTERFACE_REQUEST_TIMEOUT = "bkInterfaceRequestTimeout" //接口请求超时
    const val BK_FAILED_CREATE_BUILD_MACHINE = "bkFailedCreateBuildMachine" //创建构建机失败，错误信息
    const val BK_SEND_REQUEST_CREATE_BUILDER_SUCCESSFULLY = "bkSendRequestCreateBuilderSuccessfully" //下发创建构建机请求成功
    const val BK_WAITING_MACHINE_START = "bkWaitingMachineStart" //等待机器启动...
    const val BK_WAIT_AGENT_START = "bkWaitAgentStart" //构建机启动成功，等待Agent启动...
    const val BK_SEND_REQUEST_START_BUILDER_SUCCESSFULLY = "bkSendRequestStartBuilderSuccessfully" //下发启动构建机请求成功
    const val BK_BUILD_MACHINE_FAILS_START = "bkBuildMachineFailsStart" //构建机启动失败，错误信息
    const val BK_NO_FREE_BUILD_MACHINE = "bkNoFreeBuildMachine" //DEVCLOUD构建机启动失败，没有空闲的构建机

}
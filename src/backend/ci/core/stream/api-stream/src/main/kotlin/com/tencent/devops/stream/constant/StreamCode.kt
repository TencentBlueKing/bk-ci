package com.tencent.devops.stream.constant

object StreamCode {
    const val BK_FAILED_VERIFY_AUTHORITY = "BkFailedVerifyAuthority" //授权人权限校验失败
    const val BK_PROJECT_ALREADY_EXISTS = "BkProjectAlreadyExists" //项目已存在
    const val BK_PROJECT_NOT_EXIST = "BkProjectNotExist" //项目不存在
    const val BK_STREAM_MESSAGE_NOTIFICATION = "BkStreamMessageNotification" //@Stream消息通知
    const val BK_SESSION_ID = "BkSessionId" //会话ID
    const val BK_GROUP_ID = "BkGroupId" //群ID
    const val BK_THIS_GROUP_ID = "BkThisGroupId" //本群ID='{0}'。PS:群ID可用于蓝盾平台上任意企业微信群通知。
    const val BK_NOT_AUTHORIZED_BY_OAUTH = "BkNotAuthorizedByOauth" //用户[{0}]尚未进行OAUTH授权，请先授权。
    const val BK_PROJECT_STREAM_NOT_ENABLED = "BkProjectStreamNotEnabled" //工蜂项目{0}未开启Stream
    const val BK_NEED_SUPPLEMEN = "BkNeedSupplemen" //对接其他Git平台时需要补充
    const val BK_VIEW_DETAILS = "BkViewDetails" //查看详情
    const val BK_NO_RECORD_MIRROR_VERSION = "BkNoRecordMirrorVersion" //没有此镜像版本记录
    const val BK_MIRROR_VERSION_NOT_AVAILABLE = "BkMirrorVersionNotAvailable" //镜像版本不可用
    const val BK_VARIABLE_NAME = "BkVariableName" //变量名称必须是英文字母、数字或下划线(_)
    const val BK_MUST_HAVE_ONE = "BkMustHaveOne" //stages, jobs, steps, extends 必须存在一个
    const val BK_STARTUP_CONFIGURATION_MISSING = "BkStartupConfigurationMissing" //启动配置缺少 rtx.v2GitUrl
    const val BK_MANUAL_TRIGGER = "BkManualTrigger" //手动触发
    const val BK_BUILD_TRIGGER = "BkBuildTrigger" //构建触发
    const val BK_PULL_CODE = "BkPullCode" //拉代码
    const val BK_GIT_CI_NO_RECOR = "BkGitCiNoRecor" //Git CI没有此镜像版本记录
    const val BK_CREATE_SERVICE = "BkCreateService" //创建{0}服务
    const val BK_PROJECT_CANNOT_OPEN_STREAM = "BkProjectCannotOpenStream" //项目无法开启Stream，请联系蓝盾助手
    const val BK_PROJECT_CANNOT_QUERIED = "BkProjectCannotQueried" //项目未开启Stream，无法查询
    const val BK_PIPELINE_NOT_EXIST_OR_DELETED = "BkPipelineNotExistOrDeleted" //流水线不存在或已删除，如有疑问请联系蓝盾助手
    const val BK_BUILD_TASK_NOT_EXIST = "BkBuildTaskNotExist" //构建任务不存在，无法重试
    const val BK_USER_NOT_PERMISSION_FOR_WORKER_BEE = "BkUserNotPermissionForWorkerBee" //用户没有工蜂项目权限，无法获取下载链接
    const val BK_INCORRECT_ID_BLUE_SHIELD_PROJECT = "BkIncorrectIdBlueShieldProject" //蓝盾项目ID不正确


}
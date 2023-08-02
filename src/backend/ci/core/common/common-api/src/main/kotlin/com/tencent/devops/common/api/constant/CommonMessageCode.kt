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

package com.tencent.devops.common.api.constant

/**
 * 流水线微服务模块请求返回状态码
 * 返回码制定规则（0代表成功，为了兼容历史接口的成功状态都是返回0）：
 * 1、返回码总长度为7位，
 * 2、前2位数字代表系统名称（如21代表平台）
 * 3、第3位和第4位数字代表微服务模块（00：common-公共模块 01：process-流水线 02：artifactory-版本仓库 03:dispatch-分发 04：dockerhost-docker机器
 *    05:environment-环境 06：experience-版本体验 07：image-镜像 08：log-日志 09：measure-度量 10：monitoring-监控 11：notify-通知
 *    12：openapi-开放api接口 13：plugin-插件 14：quality-质量红线 15：repository-代码库 16：scm-软件配置管理 17：support-支撑服务
 *    18：ticket-证书凭据 19：project-项目管理 20：store-商店 21： auth-权限 22:sign-签名服务 23:metrics-度量服务 24：external-外部
 *    25：prebuild-预建 26: dispatcher-kubernetes 27：buildless 28: lambda 29: stream  30: worker 31: dispatcher-docker
 *    32: remotedev）
 * 4、最后3位数字代表具体微服务模块下返回给客户端的业务逻辑含义（如001代表系统服务繁忙，建议一个模块一类的返回码按照一定的规则制定）remotedev
 * 5、系统公共的返回码写在CommonMessageCode这个类里面，具体微服务模块的返回码写在相应模块的常量类里面
 *
 * @since: 2023-3-20
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */
object CommonMessageCode {
    const val MSG_CODE_ROLE_PREFIX = "MSG_CODE_ROLE_PREFIX_" // 角色国际化前缀
    const val MSG_CODE_PERMISSION_PREFIX = "MSG_CODE_PERMISSION_PREFIX_" // 操作权限国际化前缀
    const val SUCCESS = "0" // 成功
    const val OAUTH_DENERD = 418 // 自定义状态码, 未进行oauth认证
    const val SYSTEM_ERROR = "2100001" // 系统内部繁忙，请稍后再试
    const val PARAMETER_IS_NULL = "2100002" // 参数{0}不能为空
    const val PARAMETER_IS_EXIST = "2100003" // 参数值{0}已经存在系统，请换一个再试
    const val PARAMETER_IS_INVALID = "2100004" // 参数值{0}为非法数据
    const val OAUTH_TOKEN_IS_INVALID = "2100005" // 无效的token，请先oauth认证
    const val PERMISSION_DENIED = "2100006" // 无权限{0}
    const val ERROR_SERVICE_NO_FOUND = "2100007" // "找不到任何有效的{0}服务提供者"
    const val ERROR_SERVICE_INVOKE_FAILURE = "2100008" // "服务调用失败：{0},uniqueId={1}"
    const val ERROR_INVALID_CONFIG = "2100009" // "配置不可用：{0},uniqueId={1}"
    const val ERROR_REST_EXCEPTION_COMMON_TIP = "2100010" // 接口访问出现异常，请联系助手或稍后再重试
    const val ERROR_CLIENT_REST_ERROR = "2100011" // 用户请求不合法，参数或方法错误，请咨询助手
    const val ERROR_PROJECT_FEATURE_NOT_ACTIVED = "2100012" // 项目[{0}]未开通该功能
    const val ERROR_INVALID_PARAM_ = "2100013" // 无效参数: {0}
    const val ERROR_NEED_PARAM_ = "2100014" // 缺少参数: {0}
    const val PARAMETER_VALIDATE_ERROR = "2100015" // {0}参数校验错误: {1}
    const val ERROR_SERVICE_NO_AUTH = "2100016" // 无访问服务的权限
    const val ERROR_QUERY_NUM_TOO_BIG = "2100017" // 查询的数量超过系统规定的值：{0}，请调整查询条件或咨询助手
    const val ERROR_QUERY_TIME_RANGE_TOO_LARGE = "2100018" // 查询的时间范围跨度最大，最长时间范围跨度不能超过{0}天
    const val ERROR_HTTP_RESPONSE_BODY_TOO_LARGE = "2100019" // http请求返回体太大
    const val PERMISSION_DENIED_FOR_APP = "2100020" // APP的无权限{0}
    const val ERROR_SENSITIVE_API_NO_AUTH = "2100021" // 无敏感API访问权限
    const val PARAMETER_LENGTH_TOO_LONG = "2100022" // 参数长度不能超过{0}个字符
    const val PARAMETER_LENGTH_TOO_SHORT = "2100023" // 参数长度不能小于{0}个字符
    const val PARAMETER_ILLEGAL_ERROR = "2100024" // {0}参数非法错误: {1}
    const val PARAMETER_EXPIRED_ERROR = "2100025" // {0}token过期错误: {1}
    const val PARAMETER_SECRET_ERROR = "2100026" // {0}密钥配置错误: {1}
    const val PARAMETER_IS_EMPTY = "2100027" // 参数不能为空
    const val ERROR_QUERY_TIME_RANGE_ERROR = "2100028" // 查询的时间范围跨度错误
    const val SERVICE_NOT_EXIST = "2100029" // 父服务不存在异常

    const val ILLEGAL_GITCI_SERVICE_IMAGE_FORMAT = "2100030" // GITCI Service镜像格式非法
    const val THIRD_PARTY_SERVICE_DEVCLOUD_EXCEPTION = "2100031" // 第三方服务-DEVCLOUD 异常，请联系8006排查，异常信息 -
    const val CREATE_CONTAINER_INTERFACE_EXCEPTION = "2100032" // 创建容器接口异常
    const val CREATE_CONTAINER_RETURNS_FAILED = "2100033" // 创建容器接口返回失败
    const val CREATE_CONTAINER_TIMED_OUT = "2100034" // 创建容器接口超时
    const val OPERATION_CONTAINER_INTERFACE_EXCEPTION = "2100035" // 操作容器接口异常
    const val OPERATION_CONTAINER_RETURNED_FAILURE = "2100036" // 操作容器接口返回失败
    const val OPERATION_CONTAINER_TIMED_OUT = "2100037" // 操作容器接口超时
    const val GET_STATUS_INTERFACE_EXCEPTION = "2100038" // 获取容器状态接口异常
    const val GET_STATUS_TIMED_OUT = "2100039" // 获取容器状态接口超时
    const val CREATE_MIRROR_INTERFACE_EXCEPTION = "2100040" // 创建镜像接口异常
    const val CREATE_MIRROR_INTERFACE_RETURNED_FAILURE = "2100041" // 创建镜像接口返回失败
    const val CREATE_MIRROR_INTERFACE_EXCEPTION_NEW = "2100042" // 创建镜像新版本接口异常
    const val NEW_MIRROR_INTERFACE_RETURNED_FAILURE = "2100043" // 创建镜像新版本接口返回失败
    const val TASK_STATUS_INTERFACE_EXCEPTION = "2100044" // 获取TASK状态接口异常
    const val TASK_STATUS_TIMED_OUT = "2100045" // 获取TASK状态接口超时
    const val GET_WEBSOCKET_INTERFACE_EXCEPTION = "2100046" // 获取websocket接口异常
    const val PARAMETER_CANNOT_EMPTY_ALL = "2100047" // 参数不能全部为空
    const val USERS_EXCEEDS_THE_LIMIT = "2100048" // 授权用户数越界:{0}
    const val FAILED_TO_QUERY_GSE_AGENT_STATUS = "2100049" // 查询 Gse Agent 状态失败
    const val FAILED_TO_GET_AGENT_STATUS = "2100050" // 获取agent状态失败
    const val FAILED_TO_GET_CMDB_NODE = "2100051" // 获取 CMDB 节点失败
    const val FAILED_TO_GET_CMDB_LIST = "2100052" // 获取CMDB列表失败
    const val STAGES_AND_STEPS_CANNOT_EXIST_BY_SIDE = "2100053" // stages和steps不能并列存在!

    const val USER_NOT_PERMISSIONS_OPERATE_PIPELINE = "2100054" // 用户({0})无权限在工程({1})下{2}流水线{3}
    const val USER_NOT_HAVE_PROJECT_PERMISSIONS = "2100055" // 用户 {0}无项目{1}权限
    const val UNABLE_GET_PIPELINE_JOB_STATUS = "2100056" // 无法获取流水线JOB状态，构建停止
    const val JOB_BUILD_STOPS = "2100057" // 流水线JOB已经不再运行，构建停止
    const val PIPELINE_NAME_OCCUPIED = "2100058" // 流水线名称已被他人使用
    const val INTERNAL_DEPENDENCY_SERVICE_EXCEPTION = "2100059" // 内部依赖服务异常
    const val PUBLIC_BUILD_RESOURCE_POOL_NOT_EXIST = "2100060" // 公共构建资源池不存在，请检查yml配置.
    const val ERROR_LANGUAGE_IS_NOT_SUPPORT = "2100061" // 该语言蓝盾目前不支持，蓝盾目前支持的语言标识为：{0}
    const val INIT_SERVICE_LIST_ERROR = "2100062" // 初始化服务列表异常问题
    const val FILE_NOT_EXIST = "2100063" // 文件{0}不存在
    const val USER_ACCESS_CHECK_FAIL = "2100064" // Gitlab access token 不正确

    const val GITLAB_TOKEN_EMPTY = "2100065" // GitLab Token为空
    const val GITLAB_HOOK_URL_EMPTY = "2100066" // GitLab hook url为空
    const val GITLAB_TOKEN_FAIL = "2100067" // GitLab Token不正确
    const val GIT_TOKEN_FAIL = "2100068" // Git Token不正确
    const val SERCRT_EMPTY = "2100069" // GIT 私钥为空
    const val GIT_SERCRT_WRONG = "2100070" // Git 私钥不对
    const val PWD_EMPTY = "2100071" // 用户密码为空
    const val USER_NAME_EMPTY = "2100072" // 用户名为空
    const val GITLAB_INVALID = "2100073" // 无效的GITLAB仓库
    const val GIT_TOKEN_WRONG = "2100074" // Git Token 不正确
    const val GIT_LOGIN_FAIL = "2100075" // Git 用户名或者密码不对
    const val GIT_TOKEN_EMPTY = "2100076" // Git Token为空
    const val GIT_HOOK_URL_EMPTY = "2100077" // Git hook url为空
    const val TGIT_LOGIN_FAIL = "2100078" // TGit 用户名或者密码不对
    const val TGIT_TOKEN_EMPTY = "2100079" // TGit Token 不正确
    const val TGIT_SECRET_WRONG = "2100080" // TGit 私钥不对
    const val SVN_SECRET_OR_PATH_ERROR = "2100081" // SVN 私钥不正确 或者 SVN 路径没有权限
    const val SVN_CREATE_HOOK_FAIL = "2100082" // 添加SVN WEB hook 失败
    const val LOCK_FAIL = "2100083" // lock失败
    const val UNLOCK_FAIL = "2100084" // unlock失败
    const val GIT_REPO_PEM_FAIL = "2100085" // 代码仓库访问未授权
    const val CALL_REPO_ERROR = "2100086" // 代码仓库访问异常
    const val P4_USERNAME_PASSWORD_FAIL = "2100087" // p4用户名密码错误
    const val PARAM_ERROR = "2100088" // 参数错误
    const val AUTH_FAIL = "2100089" // {0}认证失败
    const val ACCOUNT_NO_OPERATION_PERMISSIONS = "2100090" // 账户没有{0}的权限
    const val REPO_NOT_EXIST_OR_NO_OPERATION_PERMISSION = "2100091" // {0}仓库不存在或者是账户没有该项目{1}的权限
    const val GIT_INTERFACE_NOT_EXIST = "2100092" // {0}平台没有{1}的接口
    const val GIT_CANNOT_OPERATION = "2100093" // {0}平台{1}操作不能进行
    const val WEBHOOK_LOCK_UNLOCK_FAIL = "2100094" // unlock webhooklock失败,请确认token是否已经配置
    const val COMMIT_CHECK_ADD_FAIL = "2100095" // Commit Check添加失败，请确保该代码库的凭据关联的用户对代码库有Developer权限
    const val ADD_MR_COMMENTS_FAIL = "2100096" // 添加MR的评论失败，请确保该代码库的凭据关联的用户对代码库有Developer权限
    const val WEBHOOK_ADD_FAIL = "2100097" // Webhook添加失败，请确保该代码库的凭据关联的用户对代码库有{0}权限
    const val WEBHOOK_UPDATE_FAIL = "2100098" // Webhook更新失败，请确保该代码库的凭据关联的用户对代码库有Developer权限
    const val ENGINEERING_REPO_UNAUTHORIZED = "2100099" // 工程仓库访问未授权
    const val ENGINEERING_REPO_NOT_EXIST = "2100100" // 工程仓库不存在
    const val ENGINEERING_REPO_CALL_ERROR = "2100101" // 工程仓库访问异常
    const val NOT_MEMBER_AND_NOT_OPEN_SOURCE = "2100102" // 非项目成员且项目为非开源项目
    // 2100108
    const val USER_NO_PIPELINE_PERMISSION = "2100108" // 流水线: 用户无{0}权限
    const val SERVICE_COULD_NOT_BE_ANALYZED = "2100109" // 无法根据接口"{0}"分析所属的服务
    const val RETURNED_RESULT_COULD_NOT_BE_PARSED = "2100110" // 内部服务返回结果无法解析 status:{0} body:{1}
    const val SERVICE_PROVIDER_NOT_FOUND = "2100111" // 找不到任何有效的{0}【{1}】服务提供者
    const val ILLEGAL_JOB_TYPE = "2100112" // 非法的job类型!
    const val ERROR_YAML_FORMAT_EXCEPTION = "2100113" // {0} 中 {1} 格式有误,应为 {2}, error message:{3}
    const val ERROR_YAML_FORMAT_EXCEPTION_CHECK_STAGE_LABEL = "2100114" // 请核对Stage标签是否正确
    const val ERROR_YAML_FORMAT_EXCEPTION_LENGTH_LIMIT_EXCEEDED = "2100115" // "{0} job.id 超过长度限制64 {1}}"
    const val ERROR_YAML_FORMAT_EXCEPTION_NEED_PARAM = "2100116" // {0} 中的step必须包含uses或run或checkout!
    const val ERROR_YAML_FORMAT_EXCEPTION_SERVICE_IMAGE_FORMAT_ILLEGAL = "2100117" // STREAM Service镜像格式非法
    const val ERROR_YAML_FORMAT_EXCEPTION_STEP_ID_UNIQUENESS = "2100118" // 请确保step.id唯一性!({0})
    const val BUILD_RESOURCE_NOT_EXIST = "2100119" // {0}构建资源不存在，请检查yml配置.
    const val ERROR_YAML_FORMAT_EXCEPTION_ENV_QUANTITY_LIMIT_EXCEEDED = "2100120" // {0}配置Env数量超过100限制!
    // {0}Env单变量{1}长度超过{2}字符!({3})
    const val ERROR_YAML_FORMAT_EXCEPTION_ENV_VARIABLE_LENGTH_LIMIT_EXCEEDED = "2100121"

    const val BK_CONTAINER_TIMED_OUT = "bkContainerTimedOut" // 创建容器超时
    const val BK_CREATION_FAILED_EXCEPTION_INFORMATION = "bkCreationFailedExceptionInformation" // 创建失败，异常信息

    const val BK_FILE_NAME = "bkFileName" // 文件名
    const val BK_BELONG_TO_THE_PROJECT = "bkBelongToTheProject" // 所属项目
    const val BK_OPERATING = "bkOperating" // 操作
    const val BK_PUSH_FROM_BLUE_SHIELD_DEVOPS_PLATFORM = "bkPushFromBlueShieldDevopsPlatform" // 来自蓝盾DevOps平台的推送
    const val BK_TABLE_CONTENTS = "bkTableContents" // 表格内容
    const val BK_PLEASE_FEEL_TO_CONTACT_BLUE_SHIELD_ASSISTANT = "bkPleaseFeelToContactBlueShieldAssistant"
    // 如有任何问题，可随时联系蓝盾助手
    const val BK_ETH1_NETWORK_CARD_IP_EMPTY = "bkEth1NetworkCardIpEmpty" // eth1 网卡Ip为空，因此，获取eth0的网卡ip
    const val BK_LOOPBACK_ADDRESS_OR_NIC_EMPTY = "bkLoopbackAddressOrNicEmpty" // loopback地址或网卡名称为空
    const val BK_FAILED_GET_NETWORK_CARD = "bkFailedGetNetworkCard" // 获取网卡失败
    const val BK_MANUAL_TRIGGER = "bkManualTrigger" // 手动触发
    const val BK_BUILD_TRIGGER = "bkBuildTrigger" // 构建触发
    const val BK_VIEW_DETAILS = "bkSeeDetails" // 查看详情
    const val BK_PROJECT_ID = "bkProjectId" // # 项目ID:
    const val BK_PIPELINE_NAME = "bkPipelineName" // # 流水线名称:
    const val BK_CREATE_SERVICE = "bkCreateService" // 创建{0}服务
    const val BK_SESSION_ID = "bkSessionId" // 会话ID
    const val BK_GROUP_ID = "bkGroupId" // 群ID
    const val BK_THIS_GROUP_ID = "bkThisGroupId" // 本群ID={0}。PS:群ID可用于蓝盾平台上任意企业微信群通知。
    const val BK_MISSING_RESOURCE_DEPENDENCY = "bkMissingResourceDependency" // 依赖的资源不存在

    const val BK_REQUEST_TIMED_OUT = "bkRequestTimedOut" // 请求超时
    const val BK_QUERY_PARAM_REQUEST_ERROR = "bkQueryParamRequestError" // 查询参数请求错误
    const val BK_JSON_BAD_PARAMETERS = "bkJsonBadParameters" // JSON参数错误/Bad Parameters in json
    // 请求体内容参数错误。温馨提示：请确认{0}是否符合要求
    const val BK_REQUEST_BODY_CONTENT_PARAMETER_INCORRECT = "bkRequestBodyContentParameterIncorrect"
    const val BK_REQUESTED_RESOURCE_DOES_NOT_EXIST = "bkRequestedResourceDoesNotExist" // 请求的资源不存在
    const val BK_NOT_OAUTH_CERTIFICATION = "bkNotOauthCertification" // 你没有Oauth认证
    const val BK_QUERY_PARAM_REQUEST_EMPTY = "bkQueryParamRequestEmpty" // 请求的参数内容为空
    const val BK_QUERY_PARAM_TYPE_ERROR = "bkQueryParamTypeError" // 查询参数类型错误
    // 你没有权限进行该操作
    const val BK_NOT_HAVE_PERMISSION_PERFORM_THIS_OPERATION = "bkNotHavePermissionPerformThisOperation"
    // 访问后台数据失败，已通知产品、开发，请稍后重试
    const val BK_FAILED_ACCESS_BACKGROUND_DATA = "bkFailedAccessBackgroundData"
    // 未授权访问的资源
    const val BK_RESOURCES_THAT_NOT_AUTHORIZED_ACCESS = "bkResourcesThatNotAuthorizedAccess"
    const val BK_CODE_BASE_TRIGGERING = "bkCodeBaseTriggering" // 代码库触发
    const val BK_FAILED_START_BUILD_MACHINE = "bkFailedStartBuildMachine" // 启动构建机失败

    const val CREATE_BRANCH = "bkCreateBranch" // 创建分支
    const val DELETE_BRANCH = "bkDeleteBranch" // 删除分支

    const val GET_PROJECT_INFO = "bkGetProjectInfo" // 获取项目详情

    const val OPERATION_BRANCH = "bkOperationBranch" // 拉分支
    const val OPERATION_TAG = "bkOperationTag" // 拉标签
    const val OPERATION_ADD_WEBHOOK = "bkOperationAddWebhook" // 添加WEBHOOK
    const val OPERATION_UPDATE_WEBHOOK = "bkOperationUpdateWebhook" // 修改WEBHOOK
    const val OPERATION_LIST_WEBHOOK = "bkOperationListWebhook" // 查询WEBHOOK
    const val OPERATION_ADD_COMMIT_CHECK = "bkOperationAddCommitCheck" // 添加COMMIT CHECK
    const val OPERATION_ADD_MR_COMMENT = "bkOperationAddMrComment" // 添加MR COMMENT
    const val OPERATION_COMMIT = "bkOperationCommit" // 拉提交记录
    const val OPERATION_COMMIT_DIFF = "bkOperationCommitDiff" // 查询commit变化
    const val OPERATION_UNLOCK_HOOK_LOCK = "bkOperationUnlockHookLock" // 解锁hook锁
    const val OPERATION_MR_CHANGE = "bkOperationMrChange" // 查询合并请求的代码变更
    const val OPERATION_MR_INFO = "bkOperationMrInfo" // 查询项目合并请求
    const val OPERATION_GET_CHANGE_FILE_LIST = "bkOperationGetChangeFileList" // 查询变更文件列表
    const val OPERATION_GET_MR_COMMIT_LIST = "bkOperationGetMrCommitList" // 获取合并请求中的提交
    const val OPERATION_PROJECT_USER_INFO = "bkOperationProjectUserInfo" // 获取项目中成员信息
    const val OPERATION_TAPD_WORKITEMS = "bkOperationTapdWorkItems" // 查看绑定的TAPD单
    const val BK_USER_GROUP_CRATE_TIME = "bkUserGroupCrateTime" // {0} 用户组:{1},由{2} 创建于
    const val BK_USER_RATING_ADMIN_CRATE_TIME = "bkUserRatingAdminCrateTime" // {0} 分级管理员,由{1} 创建于
    const val BK_SECOND_LEVEL_ADMIN_CREATE = "bkSecondLevelAdminCreate" // {0} 二级管理员, 由{1} 创建于
    const val BK_SECOND_LEVEL_ADMIN_REVISE = "bkSecondLevelAdminRevise" // {0} 二级管理员, 由{1} 修改于
    // 用户 {0} 申请{1}蓝盾项目 {2} ,请审批！
    const val BK_USER_REQUESTS_THE_PROJECT = "bkUserRequestsTheProject"
    const val BK_ENV_NOT_YET_SUPPORTED = "bkEnvNotYetSupported" // 尚未支持 {0} {1}，请联系 管理员 添加对应版本

    const val BK_BUILD_ENV_TYPE = "BUILD_ENV_TYPE_" // 构建环境-
    const val BK_BUILD_ENV_TYPE_BUILDLESS = "BUILD_ENV_TYPE_BUILDLESS" // 无编译环境
    const val BK_BUILD_ENV_TYPE_BUILD_TRIGGERS = "BUILD_ENV_TYPE_BUILD_TRIGGER" // 构建触发
}

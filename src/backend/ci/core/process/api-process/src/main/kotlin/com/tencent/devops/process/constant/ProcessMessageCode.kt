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

package com.tencent.devops.process.constant

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
object ProcessMessageCode {

    // 常量标志 对应code
    const val SYSTEM_VIEW_LABEL = "CONST_PROCESS_VIEW_LABEL_SYSTEM" // "系统视图"
    const val PROJECT_VIEW_LABEL = "CONST_PROCESS_VIEW_LABEL_PROJECT" // "项目视图"
    const val PERSON_VIEW_LABEL = "CONST_PROCESS_VIEW_LABEL_PRIVATE" // "个人视图"
    const val FAVORITE_PIPELINES_LABEL = "CONST_PROCESS_VIEW_LABEL_FAVORITE" // "我的收藏"
    const val MY_PIPELINES_LABEL = "CONST_PROCESS_VIEW_LABEL_MY" // "我的流水线"
    const val ALL_PIPELINES_LABEL = "CONST_PROCESS_VIEW_LABEL_ALL" // "全部流水线"

    const val BK_NON_TIMED_TRIGGER_SKIP = "bkNonTimedTriggerSkip" // 非定时触发，直接跳过
    const val BK_FIRST_STAGE_ENV_NOT_EMPTY = "bkFirstStageEnvNotEmpty" // 第一阶段的环境不能为空
    const val BK_QUALITY_CHECK_SUCCEED = "bkQualityCheckSucceed" // 质量红线({0})检测已通过
    const val BK_QUALITY_CHECK_INTERCEPTED = "bkQualityCheckIntercepted" // 质量红线({0})检测被拦截
    const val BK_QUALITY_TO_BE_REVIEW = "bkQualityToBeReview" // 质量红线({0})待审核!审核人：{1}
    const val BK_POLLING_WAIT_FOR_QUALITY_RESULT = "bkPollingWaitForQualityResult" // 第 {0} 次轮询等待红线结果
    const val BK_QUALITY_CHECK_RESULT = "bkQualityCheckResult" // 检测红线结果
    const val BK_AUDIT_TIMEOUT = "bkAuditTimeout" // 审核超时
    const val BK_AUDIT_RESULT = "bkAuditResult" // 步骤审核结束，审核结果：[{0}]，审核人：{1}
    // 正在处理当前上报的任务, 请稍等。。。
    const val BK_PROCESSING_CURRENT_REPORTED_TASK_PLEASE_WAIT = "bkProcessingCurrentReportedTaskPleaseWait"
    // <viewId>和<viewName>不能同时为空, 填<viewName>时需同时填写参数<isProject>
    const val BK_VIEW_ID_AND_NAME_CANNOT_BE_EMPTY_TOGETHER = "bkViewIdAndNameCannotBeEmptyTogether"
    // 因【Git事件触发】插件中，MR Request Hook勾选了【MR为同源同目标分支时，等待队列只保留最新触发的任务】配置，该次构建已被新触发的构建
    const val BK_TRIGGERED_BY_GIT_EVENT_PLUGIN = "bkTriggeredByGitEventPlugin"
    const val BK_BUILD_IN_REVIEW_STATUS = "bkBuildInReviewStatus" // 项目【{0}】下的流水线【{1}】#{2} 构建处于待审核状态
    // 用户（{0}) 没有流水线({1})的执行权限
    const val BK_USER_NO_PIPELINE_EXECUTE_PERMISSIONS = "bkUserNoPipelineExecutePermissions"
    const val BK_REMOTE_CALL_SOURCE_IP = "bkRemoteCallSourceIp" // 本次远程调用的来源IP是[$sourceIp]
    const val BK_PIPELINE_NAME = "bkPipelineName" // 流水线名称
    const val BK_CREATOR = "bkCreator" // 创建人
    const val BK_TCLS_ENVIRONMENT_MESSAGE = "bkTclsEnvironmentMessage" // 获取 TCLS 环境失败，请检查用户名密码是否正确，错误信息：
    const val BK_TCLS_ENVIRONMENT = "bkTclsEnvironment" // 获取 TCLS 环境失败，请检查用户名密码是否正确
    const val BK_CONTINUE = "bkContinue" // 继续
    const val BK_OVERRULE = "bkOverrule" // 驳回
    const val BK_TRIGGER = "bkTrigger" // 触发

    const val OK = 0

    const val ERROR_BUILD_TASK_SUBPIPELINEID_NULL = "2101001" // 子流水线id不存在
    const val ERROR_BUILD_TASK_SUBPIPELINEID_NOT_EXISTS = "2101002" // 子流水线不存在
    const val ERROR_PIPELINE_VIEW_MAX_LIMIT = "2101003" // 最多允许同时保存30个视图
    const val ERROR_PIPELINE_VIEW_NOT_FOUND = "2101004" // 视图({0})不存在
    const val ERROR_PIPELINE_VIEW_HAD_EXISTS = "2101005" // 视图({0})已存在
    const val ERROR_DEL_PIPELINE_VIEW_NO_PERM = "2101006" // 用户({0})无权限删除视图({1})
    const val PIPELINE_LIST_LENGTH_LIMIT = "2101007" // 流水线列表长度不能超过100
    const val USER_NEED_PIPELINE_X_PERMISSION = "2101008" // 流水线: 用户无{0}权限
    const val ERROR_PIPELINE_CHANNEL_CODE = "2101009" // 流水线：流水线渠道来源不符合({0})
    const val ERROR_ATOM_NOT_FOUND = "2101010" // 插件不存在
    const val ILLEGAL_PIPELINE_MODEL_JSON = "2101011" // 流水线: 流水线Model不合法
    const val OPERATE_PIPELINE_FAIL = "2101012" // 流水线: 流水线出现异常:{0}
    const val ERROR_PIPELINE_NAME_EXISTS = "2101013" // 流水线: 流水线名称已被使用
    const val ERROR_PIPELINE_TEMPLATE_CAN_NOT_EDIT = "2101014" // 流水线: 模板流水线不支持编辑
    const val ERROR_PIPELINE_NAME_TOO_LONG = "2101015" // 流水线名称过长
    const val ERROR_PIPELINE_IS_EXISTS = "2101016" // 流水线: 流水线已存在
    const val ERROR_QUALITY_TASK_NOT_FOUND = "2101017" // 流水线: 质量红线拦截的任务[{0}]不存在
    const val ERROR_QUALITY_REVIEWER_NOT_MATCH = "2101018" // 流水线: 用户({0})不在审核人员名单中
    const val CANCEL_BUILD_BY_OTHER_USER = "2101019" // 流水线已经被{0}取消构建，请过{1}秒后再试
    const val ERROR_START_BUILD_FREQUENT_LIMIT = "2101020" // 流水线: 不能太频繁启动构建
    const val DENY_START_BY_MANUAL = "2101021" // 流水线: 该流水线不能手动启动
    const val DENY_START_BY_REMOTE = "2101022" // 流水线: 该流水线不能远程触发
    const val ERROR_PARAM_WEBHOOK_ID_NAME_ALL_NULL = "2101023" // Webhook 的ID和名称同时为空
    const val ERROR_RESTORE_PIPELINE_NOT_FOUND = "2101024" // 要还原的流水线不存在，可能已经被删除或还原了
    const val ERROR_PIPELINE_MODEL_NEED_JOB = "2101025" // Stage缺少Job{0}
    const val ONLY_MANAGE_CAN_OPERATE_TEMPLATE = "2101026" // 只有管理员才能操作模板
    const val PIPELINE_SETTING_NOT_EXISTS = "2101027" // 流水线设置不存在
    const val TEMPLATE_NAME_CAN_NOT_NULL = "2101028" // 模板名不能为空字符串
    const val PIPELINE_PARAM_CONSTANTS_DUPLICATE = "2101029" // 流水线变量参数和常量重名
    const val ERROR_TEMPLATE_NAME_IS_EXISTS = "2101030" // 模板名已经存在
    const val TEMPLATE_CAN_NOT_DELETE_WHEN_HAVE_INSTANCE = "2101031" // 模板还存在实例，不允许删除
    const val TEMPLATE_CAN_NOT_DELETE_WHEN_PUBLISH = "2101032" // 已关联到研发商店，请先下架再删除
    const val TEMPLATE_CAN_NOT_DELETE_WHEN_INSTALL = "2101033" // 已安装到其他项目下使用，不能删除
    const val ERROR_TEMPLATE_NOT_EXISTS = "2101034" // 模板不存在
    const val ERROR_SOURCE_TEMPLATE_NOT_EXISTS = "2101035" // 源模板不存在
    const val FAIL_TO_LIST_TEMPLATE_PARAMS = "2101036" // 列举流水线模板参数失败
    const val ONLY_ONE_TRIGGER_JOB_IN_PIPELINE = "2101037" // 流水线只能有一个触发Stage
    const val ERROR_PIPELINE_NOT_EXISTS = "2101038" // 流水线{0}不存在
    const val ERROR_PIPELINE_MODEL_NOT_EXISTS = "2101039" // 流水线的模型不存在
    const val ERROR_PIPELINE_NODEL_CONTAINER_NOT_EXISTS = "2101040" // 流水线的模型中指定构建容器{0}不存在
    const val ERROR_SAVE_PIPELINE_TIMER = "2101041" // 流水线的定时触发器保存失败
    const val ERROR_PIPELINE_JOB_NEED_TASK = "2101042" // Job需要至少有一个任务插件
    const val ERROR_DEL_PIPELINE_TIMER = "2101043" // 流水线{0}的定时触发器删除失败
    const val SVN_NOT_SUPPORT_TAG = "2101044" // SVN do not support tag
    const val ERROR_PIPELINE_REPO_ID_NULL = "2101045" // 仓库ID为空
    const val ERROR_PIPELINE_REPO_NAME_NULL = "2101046" // 仓库名为空
    const val ERROR_PIPELINE_LOCK = "2101047" // 流水线锁定
    const val ILLEGAL_TIMER_CRONTAB = "2101048" // 定时触发器的定时参数[{0}]不合法
    const val ERROR_PIPELINE_QUEUE_FULL = "2101049" // 流水线队列满
    const val USER_NO_PIPELINE_PERMISSION_UNDER_PROJECT = "2101050" // 用户({0})在工程({1})下没有流水线{2}权限
    const val ERROR_PIPELINE_DISPATCH_STORE_IMAGE_CODE_BLANK = "2101051" // 模型中使用了商店镜像，但code为空
    const val ERROR_PIPELINE_DISPATCH_STORE_IMAGE_VERSION_BLANK = "2101052" // 模型中使用了商店镜像，但version为空
    const val ERROR_PIPELINE_DISPATCH_VALUE_BLANK = "2101053" // 模型中非商店蓝盾源/第三方源的镜像value为空
    const val ERROR_PIPELINE_PARAMS_NAME_ERROR = "2101054" // 请使用英文命名流水线变量
    const val ERROR_PIPELINE_STAGE_NO_REVIEW_GROUP = "2101055" // Stage[{0}]准入配置不正确
    const val ERROR_PIPELINE_DESC_TOO_LONG = "2101056" // 流水线描述过长
    const val ILLEGAL_TIMER_INTERVAL_CRONTAB = "2101057" // 定时触发器的定时参数[{0}]不能秒级触发
    const val ERROR_PIPLEINE_INPUT = "2101058" // 流水线: 入参buildId与pipelineId不匹配
    const val MODEL_ATOMCODE_NOT_EXSIT = "2101059" // 流水线内包含插件市场不存在的插件
    const val QUERY_USER_INFO_FAIL = "2101060" // 获取用户信息失败
    const val PROJECT_NOT_EXIST = "2101061" // 项目不存在
    const val ERROR_ATOM_RUN_BUILD_ENV_INVALID = "2101062" // 流水线: 插件[{0}]不能在该环境下运行
    const val ERROR_TEMPLATE_PIPELINE_IS_INSTANCING = "2101063" // 流水线: 模板下的流水线实例{0}正在更新中，请稍后再试
    const val ERROR_FINALLY_STAGE = "2101064" // 流水线: 每个Model只能包含一个FinallyStage，并且处于最后位置
    const val ERROR_FINALLY_STAGE_JOB_CONDITION = "2101065" // 流水线: finally stage下的[{0}]Job运行条件配置错误: {1}
    const val ERROR_NORMAL_STAGE_JOB_CONDITION = "2101066" // 流水线: 普通stage下的[{0}]Job运行条件配置错误: {0}
    const val ERROR_EMPTY_JOB = "2101067" // 流水线: Model信息不完整，Stage[{0}] Job[{1}]下没有插件
    const val ERROR_PIPELINE_MODEL_TOO_LARGE = "2101068" // 流水线: 流水线模型超限，阈值为[{0}]个字符，请联系发布者
    // 流水线: 流水线下[{0}]的[{1}]数量超限，阈值为[{2}]个，请联系发布者
    const val ERROR_PIPELINE_MODEL_COMPONENT_NUM_TOO_LARGE = "2101069"
    const val ERROR_ATOM_PARAM_VALUE_TOO_LARGE = "2101070" // 流水线: 插件[{0}]的参数[{1}]值超限，阈值为[{2}]个字符，请联系发布者
    const val ERROR_PIPELINE_STAGE_REVIEW_GROUP_NO_USER = "2101072" // Stage[{0}]的审核组[{1}]没有未配置可执行人
    const val ERROR_PIPELINE_STAGE_REVIEW_GROUP_NOT_FOUND = "2101073" // Stage[{0}]的审核组ID[{1}]不存在
    const val ERROR_PIPELINE_STAGE_POSITION_NOT_FOUND = "2101074" // Stage[{0}]的准入准出标识[{1}]不正确
    const val ERROR_PIPELINE_START_WITH_ERROR = "2101075" // 流水线启动准备失败{0}
    const val ERROR_TEMPLATE_NOT_UPDATE = "2101076" // 该模板无法更新
    const val REPOSITORY_ID_AND_NAME_ARE_EMPTY = "2101077" // 仓库ID和仓库名都为空
    const val ERROR_TEMPLATE_VERSION_COUNT_EXCEEDS_LIMIT = "2101078" // 模板的版本数量不能超过{0}个
    const val FAIL_TEMPLATE_UPDATE_NUM_TOO_BIG = "2101079" // 模板实例更新数量[{0}]超过系统规定的最大值{1}，请调整参数或咨询助手
    const val ERROR_START_BUILD_PROJECT_UNENABLE = "2101080" // 项目[{0}]已禁用，不能启动
    const val ERROR_BUILD_EXPIRED_CANT_RETRY = "2101081" // 构建数据已过期，请使用rebuild进行重试/Please use rebuild
    const val ERROR_PIPELINE_STAGE_REVIEW_VARIABLES_OUT_OF_LENGTH = "2101082" // Stage审核参数{0}超出4000长度限制
    const val ERROR_PIPELINE_CAN_NOT_DELETE_WHEN_HAVE_BUILD_RECORD = "2101083" // 流水线版本还存在构建记录，不允许删除
    const val ERROR_JOB_TIME_OUT_PARAM_VAR = "2101084" // Job[{0}]的超时配置的流水线变量[{1}]值[{2}]超出合理范围[{3}](分钟)
    // Job[{0}]的Task[{1}]的超时配置的流水线变量[{2}]值[{3}]超出合理范围[{4}](分钟)
    const val ERROR_TASK_TIME_OUT_PARAM_VAR = "2101085"
    // Job[{0}]的互斥组[{1}]超时配置的流水线变量[{2}]值[{3}]超出合理范围[{4}](分钟)
    const val ERROR_JOB_MUTEX_TIME_OUT_PARAM_VAR = "2101086"
    const val GIT_INVALID = "2101087" // 无效的GIT仓库
    const val TGIT_INVALID = "2101088" // 无效的TGIT仓库
    const val SVN_INVALID = "2101089" // 无效的SVN仓库
    const val GITHUB_INVALID = "2101090" // 无效的GITHUB仓库
    const val P4_INVALID = "2101091" // 无效的p4仓库
    const val GIT_NOT_FOUND = "2101092" // 代码库{0}不存在
    const val NOT_SVN_CODE_BASE = "2101093" // 代码库({0})不是svn代码库
    const val FAIL_TO_GET_SVN_DIRECTORY = "2101094" // 获取Svn目录失败, msg:{0}
    const val ADD_PIPELINE_TIMER_TRIGGER_SAVE_FAIL = "2101095" // 添加流水线的定时触发器保存失败！可能是定时器参数过长！
    const val BUILD_QUEUE_FOR_SINGLE = "2101096" // 排队中: 当前构建正在排队中
    const val ERROR_JOB_MATRIX_YAML_CONFIG_ERROR = "2101097" // Job[{0]的矩阵YAML配置错误:
    const val ERROR_PIPELINE_ID_NOT_PROJECT_PIPELINE = "2101098" // {0} 非 {1} 流水线
    const val ERROR_NO_MATCHING_PIPELINE = "2101099" // 没有找到对应的流水线
    // 子流水线不允许循环调用,循环流水线:projectId:{0},pipelineId:{1}
    const val ERROR_SUB_PIPELINE_NOT_ALLOWED_CIRCULAR_CALL = "2101100"
    const val ERROR_MAX_PIPELINE_COUNT_PER_PROJECT = "2101101" // 该项目最多只能创建{0}条流水线
    const val ERROR_RETRY_3_FAILED = "2101102" // 重试3次失败
    const val ERROR_UPDATE_FAILED = "2101103" // 更新失败: {0}
    const val ERROR_PERMISSION_NOT_PROJECT_MANAGER = "2101104" // {0}非项目{1}管理员
    const val BUILD_QUEUE_FOR_CONCURRENCY = "2101105" // 排队中: 当前构建正在并发组({0})排队中
    const val USER_INSTALL_ATOM_CODE_IS_INVALID = "2101106" // 安装插件失败

    const val ERROR_PARUS_PIEPLINE_IS_RUNNINT = "2101107" // 暂停的流水线已开始运行
    const val ERROR_ELEMENT_TOO_LONG = "2101109" // {0} element大小越界
    const val ERROR_JOB_RUNNING = "2101110" // job非完成态，不能进行重试
    const val ERROR_TIMEOUT_IN_BUILD_QUEUE = "2101111" // 排队超时，取消运行! [{0}]
    const val ERROR_BUILD_TASK_QUALITY_OUT = "2101112" // 质量红线(准出)检测失败
    const val ERROR_TIMEOUT_IN_RUNNING = "2101113" // {0}运行达到({1})分钟，超时结束运行!
    const val ERROR_RETRY_STAGE_NOT_FAILED = "2101114" // stage非失败状态，不能进行重试
    const val ERROR_NO_PARAM_IN_JOB_CONDITION = "2101115" // 请设置Job运行的自定义变量
    const val ERROR_BACKGROUND_SERVICE_TASK_EXECUTION = "2101116" // 后台服务任务执行出错
    const val ERROR_BACKGROUND_SERVICE_RUNNING_ERROR = "2101117" // 后台服务运行出错
    const val ERROR_VIEW_NOT_FOUND_IN_PROJECT = "2101118" // 在项目 {0} 下未找到{1}视图{2}

    const val ERROR_BUILD_TASK_ENV_NAME_IS_NULL = "2101119" // 环境名未初始化
    const val ERROR_BUILD_TASK_ENV_ID_IS_NULL = "2101120" // 环境ID未初始化
    const val ERROR_BUILD_TASK_ENV_NAME_NOT_EXISTS = "2101121" // 以下这些环境名称不存在,请重新修改流水线！$noExistsEnvNames
    const val ERROR_BUILD_TASK_USER_ENV_NO_OP_PRI = "2101122" // 用户没有操作这些环境的权限！环境：$noExistsEnvNames
    // "以下这些环境id不存在,请重新修改流水线！id：
    const val ERROR_BUILD_TASK_USER_ENV_ID_NOT_EXISTS = "2101123"
    const val ERROR_BUILD_TASK_TARGETENV_TYPE_IS_NULL = "2101124" // 支持 目标环境类型: {0}

    const val ERROR_VIEW_GROUP_IS_PROJECT_NO_SAME = "2101125" // 流水线组的视图范围不一致
    const val ERROR_VIEW_EXCEED_THE_LIMIT = "2101126" // 流水线组创建太多了
    const val ERROR_VIEW_DUPLICATE_NAME = "2101127" // 流水线组名称重复
    const val ERROR_VIEW_NAME_ILLEGAL = "2101128" // 流水线组名称不合法
    const val ERROR_DUPLICATE_BUILD_RETRY_ACT = "2101129" // 当前构建正在运行中，请勿重复提交重试请求

    const val ERROR_BUILD_TASK_QUALITY_IN = "2101130" // 质量红线(准入)检测失败
    const val INCORRECT_EXCEL_FORMAT = "2101131" // Excel格式错误，或文件不存在
    const val ERROR_CALLBACK_URL_INVALID = "2101132" // 回调的url非法
    const val USER_NEED_PROJECT_X_PERMISSION = "2101133" // 用户（{0}）无（{1}）项目权限
    const val ERROR_CALLBACK_HISTORY_NOT_FOUND = "2101134" // 回调历史记录({0})不存在
    const val ERROR_CALLBACK_REPLY_FAIL = "2101135" // 回调重试失败
    const val ERROR_CALLBACK_NOT_FOUND = "2101136" // 回调记录({0})不存在
    const val ERROR_CALLBACK_SAVE_FAIL = "2101137" // 创建callback失败,失败原因:{0}

    const val ERROR_PIPELINE_TIMER_SCM_NO_CHANGE = "2101138" // 流水线定时触发时代码没有变更
    const val ERROR_PIPELINE_SUMMARY_NOT_FOUND = "2101139" // 异常：流水线的基础构建数据Summary不存在，请联系管理员
    const val ERROR_PIPELINE_IS_NOT_THE_LATEST = "2101140" // 异常：保存已拒绝，因为保存流水线时已不是最新版本
    const val ERROR_RESTART_EXSIT = "2101141" // 流水线: 待restart构建{0}已在restart中
    const val MAXIMUM_NUMBER_QUEUES_ILLEGAL = "2101142" // 最大排队数量非法
    const val ERROR_VIEW_GROUP_NO_PERMISSION = "2101143" // 没有修改流水线组权限
    const val ERROR_EXPORT_OUTPUT_CONFLICT = "2101144" // 变量名[{0}]来源不唯一，请修改变量名称或增加插件输出命名空间：{1}
    const val ERROR_PIPELINE_DEPENDON_CYCLE = "2101145" // ({0})与({1})的jobId循环依赖
    const val ERROR_PIPELINE_JOBID_EXIST = "2101146" // ({0})的jobId({1})已存在
    const val MAXIMUM_QUEUE_LENGTH_ILLEGAL = "2101147" // 最大排队时长非法
    const val BUILD_MSG_LABEL = "2101148" // 构建信息
    const val BUILD_MSG_MANUAL = "2101149" // 手动触发
    const val BUILD_MSG_TIME = "2101150" // 定时触发
    const val BUILD_MSG_REMOTE = "2101151" // 远程触发
    const val BUILD_MSG_WEBHOOK = "2101152" // webhook触发
    const val BUILD_MSG_SERVICE = "2101153" // 服务触发
    const val BUILD_MSG_PIPELINE = "2101154" // 流水线触发
    const val BUILD_MSG_DESC = "2101155" // 构建信息描述
    const val BUILD_WORKER_DEAD_ERROR = "2101156" // 其他构建进程挂掉的参考信息，自由添加方便打印到日志里
    const val BUILD_AGENT_DETAIL_LINK_ERROR = "2101157" // 构建机Agent详情链接
    const val BUILD_MSG_TRIGGERS = "2101158" // 构建触发

    // 构建进程心跳超时{0}秒
    // \n 可能原因:
    // \n 1. 构建机网络不通，检查构建机网络代理、或所在企业安全鉴权会话是否过期。
    // \n 2. 业务构建进程进程被操作系统或其他程序杀掉，需自查并降低负载后重试。
    // \n 3. 其他参考链接[Link] 其他构建进程挂掉的参考信息，自由添加方便打印卫通日志里
    // \n 4. 平台级故障导致大面积超时。
    const val BK_TIP_MESSAGE = "2101159"
    const val ERROR_GROUP_COUNT_EXCEEDS_LIMIT = "2101160" // 一个项目标签组不能超过10个
    const val ERROR_LABEL_COUNT_EXCEEDS_LIMIT = "2101161" // 同一分组下最多可添加12个标签
    const val ERROR_LABEL_NAME_TOO_LONG = "2101162" // 一个标签最多输入20个字符
    const val ERROR_NO_BUILD_EXISTS_BY_ID = "2101163" // 流水线构建[{0}]不存在
    const val ERROR_NO_PIPELINE_EXISTS_BY_ID = "2101164" // 流水线[{0}]不存在
    const val ERROR_SUBPIPELINE_CYCLE_CALL = "2101165" // 子流水线循环调用
    const val ERROR_NO_STAGE_EXISTS_BY_ID = "2101166" // 构建中Stage[{0}]不存在
    const val ERROR_STAGE_IS_NOT_PAUSED = "2101167" // 构建中Stage[{0}]未处于等待把关状态
    const val ERROR_CONDITION_EXPRESSION_PARSE = "2101168" // 执行条件表达式解析失败
    const val ERROR_TRIGGER_REVIEW_ABORT = "2101169" // 触发审核未通过
    const val ERROR_TRIGGER_NOT_UNDER_REVIEW = "2101170" // 触发不在审核状态中
    const val ERROR_GENERATE_REMOTE_TRIGGER_TOKEN_FAILED = "2101171" // 生成远程触发token失败
    const val ERROR_NO_BUILD_RECORD_FOR_CORRESPONDING_SUB_PIPELINE = "2101172" // 找不到对应子流水线的构建记录
    const val ERROR_NO_CORRESPONDING_SUB_PIPELINE = "2101173" // "找不到对应子流水线"
    const val ERROR_USER_NO_PERMISSION_GET_PIPELINE_INFO = "2101174" // 用户（{0}) 无权限获取流水线({1})信息({2})
    const val ERROR_SUB_PIPELINE_PARAM_FILTER_FAILED = "2101175" // 子流水线参数过滤失败
    const val ERROR_NO_PERMISSION_PLUGIN_IN_TEMPLATE = "2101176" // 模版下存在无权限的插件
    const val PIPELINE_ORCHESTRATIONS_NUMBER_ILLEGAL = "2101177" // 流水线编排数量非法
    const val MAXIMUM_NUMBER_CONCURRENCY_ILLEGAL = "2101178" // 最大并发数量非法
    const val PIPELINE_BUILD_HAS_ENDED_CANNOT_BE_CANCELED = "2101179" // 流水线: 流水线构建已结束，不能取消
    const val GET_PIPELINE_ATOM_INFO_NO_PERMISSION = "2101180" // 无权访问插件{0}的流水线信息，请联系组件管理员

    const val BK_SUCCESSFULLY_DISTRIBUTED = "bkSuccessfullyDistributed" // 跨项目构件分发成功，共分发了{0}个文件
    const val BK_SUCCESSFULLY_FAILED = "bkSuccessfullyFailed" // 跨项目构件分发失败，
    const val BK_NO_MATCH_FILE_DISTRIBUTE = "bkNoMatchFileDistribute" // 匹配不到待分发的文件: {0}
    // 开始对文件（{0}）执行Gcloud相关操作，详情请去gcloud官方地址查看：
    const val BK_START_PERFORMING_GCLOUD_OPERATION = "bkStartPerformingGcloudOperation"
    const val BK_START_UPLOAD_OPERATION = "bkStartUploadOperation" // 开始执行 \"上传动态资源版本\" 操作
    const val BK_OPERATION_PARAMETERS = "bkOperationParameters" // \"上传动态资源版本\" 操作参数：
    const val BK_QUERY_VERSION_UPLOAD = "bkQueryVersionUpload" // 开始执行 \"查询版本上传 CDN 任务状态\" 操作\n
    const val BK_WAIT_QUERY_VERSION = "bkWaitQueryVersion" // \"等待查询版本上传 CDN 任务状态\" 操作执行完毕: \n
    // \"查询版本上传 CDN 任务状态\" 操作 成功执行完毕\n
    const val BK_OPERATION_COMPLETED_SUCCESSFULLY = "bkOperationCompletedSuccessfully"
    const val BK_FAILED_UPLOAD_FILE = "bkFailedUploadFile" // 上传文件失败:
    const val BK_CREATE_RESOURCE_OPERATION = "bkCreateResourceOperation" // 开始执行 \"创建资源\" 操作\n
    const val BK_CREATE_RESOURCES_OPERATION_PARAMETERS = "bkCreateResourcesOperationParameters" // \"创建资源\" 操作参数：
    const val BK_START_RELEASE_OPERATION = "bkStartReleaseOperation" // 开始执行 \"预发布\" 操作\n
    const val BK_RESPONSE_RESULT = "bkResponseResult" // 预发布单个或多个渠道响应结果:
    const val BK_RECIPIENT_EMPTY = "bkRecipientEmpty" // 收件人为空
    const val BK_EMAIL_NOTIFICATION_CONTENT_EMPTY = "bkEmailNotificationContentEmpty" // 邮件通知内容为空
    const val BK_MESSAGE_SUBJECT_EMPTY = "bkMessageSubjectEmpty" // 邮件主题为空
    const val BK_EXPERIENCE_PATH_EMPTY = "bkExperiencePathEmpty" // 体验路径为空
    const val BK_INCORRECT_NOTIFICATION_METHOD = "bkIncorrectNotificationMethod" // 通知方式不正确
    // 版本体验({0})创建成功
    const val BK_VERSION_EXPERIENCE_CREATED_SUCCESSFULLY = "bkVersionExperienceCreatedSuccessfully"
    const val BK_VIEW_RESULT = "bkViewResult" // 查看结果:
    const val BK_RECEIVER_EMPTY = "bkReceiverEmpty" // Message Receivers is empty(接收人为空)
    const val BK_MESSAGE_CONTENT_EMPTY = "bkMessageContentEmpty" // Message Body is empty(消息内容为空)
    const val BK_EMPTY_TITLE = "bkEmptyTitle" // Message Title is empty(标题为空)
    const val BK_COMPUTER_VIEW_DETAILS = "bkComputerViewDetails" // {0}\n\n电脑查看详情：{1}\n手机查看详情：{2}
    // send enterprise wechat message(发送企业微信消息):\n{0}\nto\n{1}
    const val BK_SEND_WECOM_MESSAGE = "bkSendWecomMessage"
    const val BK_INVALID_NOTIFICATION_RECIPIENT = "bkInvalidNotificationRecipient" // 通知接收者不合法:
    const val BK_WECOM_NOTICE = "bkWecomNotice" // 企业微信通知内容:
    const val BK_SEND_WECOM_CONTENT = "bkSendWecomContent" // 发送企业微信内容: ({0}) 到 {1}
    const val BK_SEND_WECOM_CONTENT_SUCCESSFULLY = "bkSendWecomContentSuccessfully" // 发送企业微信内容: ({0}) 到 {1}成功
    const val BK_SEND_WECOM_CONTENT_FAILED = "bkSendWecomContentFailed" // 发送企业微信内容: ({0}) 到 {1}失败:
    const val BK_MATCHING_FILE = "bkMatchingFile" // 匹配文件中:
    const val BK_UPLOAD_CORRESPONDING_FILE = "bkUploadCorrespondingFile" // 上传对应文件到织云成功!
    const val BK_START_UPLOADING_CORRESPONDING_FILES = "bkStartUploadingCorrespondingFiles" // 开始上传对应文件到织云...
    const val BK_PULL_GIT_WAREHOUSE_CODE = "bkPullGitWarehouseCode" // 拉取Git仓库代码
    const val BK_AUTOMATIC_EXPORT_NOT_SUPPORTED = "bkAutomaticExportNotSupported"
    // ### 可以通过 runs-on: macos-10.15 使用macOS公共构建集群。
    const val BK_BUILD_CLUSTERS_THROUGH = "bkBuildClustersThrough"
    // 注意默认的Xcode版本为12.2，若需自定义，请在JOB下自行执行 xcode-select 命令切换 ###
    const val BK_NOTE_DEFAULT_XCODE_VERSION = "bkNoteDefaultXcodeVersion"
    const val BK_PLEASE_USE_STAGE_AUDIT = "bkPleaseUseStageAudit" // 人工审核插件请改用Stage审核 ###
    const val BK_PLUG_NOT_SUPPORTED = "bkPlugNotSupported" // # 注意：不支持插件【{0}({1})】的导出
    const val BK_FIND_RECOMMENDED_REPLACEMENT_PLUG = "bkFindRecommendedReplacementPlug" // 请在蓝盾研发商店查找推荐的替换插件！
    const val BK_OLD_PLUG_NOT_SUPPORT = "bkOldPlugNotSupport" // 内置老插件不支持导出，请使用市场插件 ###
    const val BK_NO_RIGHT_EXPORT_PIPELINE = "bkNoRightExportPipeline" // 用户({0})无权限在工程({1})下导出流水线
    const val BK_PIPELINED_ID = "bkPipelinedId" // # 流水线ID:
    const val BK_EXPORT_TIME = "bkExportTime" // # 导出时间:
    const val BK_EXPORT_SYSTEM_CREDENTIALS = "bkExportSystemCredentials"
    // # 注意：[插件]输入参数可能存在敏感信息，请仔细检查，谨慎分享！！！ \n
    const val BK_SENSITIVE_INFORMATION_IN_PARAMETERS = "bkSensitiveInformationInParameters"
    // # 注意：[插件]Stream不支持蓝盾老版本的插件，请在研发商店搜索新插件替换 \n
    const val BK_STREAM_NOT_SUPPORT = "bkStreamNotSupport"
    // # \n# tips：部分参数导出会存在\[该字段限制导出，请手动填写]\,需要手动指定。原因有:\n
    const val BK_PARAMETERS_BE_EXPORTED = "bkParametersBeExported"
    const val BK_IDENTIFIED_SENSITIVE_INFORMATION = "bkIdentifiedSensitiveInformation" // # ①识别出为敏感信息，不支持导出\n
    const val BK_UNKNOWN_CONTEXT_EXISTS = "bkUnknownContextExists" // # ②部分字段校验格式时存在未知上下文，不支持导出\n
    const val BK_AUTOMATIC_EXPORT_NOT_SUPPORTED_IMAGE = "bkAutomaticExportNotSupportedImage"
    // ###请直接填入镜像(TLinux2.2公共镜像)的URL地址，若存在鉴权请增加 credentials 字段###
    const val BK_ENTER_URL_ADDRESS_IMAGE = "bkEnterUrlAddressImage"
    const val BK_ADMINISTRATOR = "bkAdministrator" // 管理员
    const val BK_QUICK_APPROVAL_MOA = "bkQuickApprovalMoa" // 【通过MOA快速审批】
    const val BK_QUICK_APPROVAL_PC = "bkQuickApprovalPc" // 【通过PC快速审批】
    const val BK_NOT_CONFIRMED_CAN_EXECUTED = "bkNotConfirmedCanExecuted" // 插件 {0} 尚未确认是否可以在工蜂CI执行
    const val BK_CONTACT_PLUG_DEVELOPER = "bkContactPlugDeveloper" // ，请联系插件开发者
    const val BK_CHECK_INTEGRITY_YAML = "bkCheckIntegrityYaml" // 请检查YAML的完整性，或切换为研发商店推荐的插件后再导出
    const val BK_BEE_CI_NOT_SUPPORT = "bkBeeCiNotSupport" // 工蜂CI不支持蓝盾老版本插件
    const val BK_SEARCH_STORE = "bkSearchStore" // 请在研发商店搜索新插件替换
    // # 注意：工蜂CI暂不支持当前类型的构建机
    const val BK_NOT_SUPPORT_CURRENT_CONSTRUCTION_MACHINE = "bkNotSupportCurrentConstructionMachine"
    const val BK_EXPORT = "bkExport" // 的导出,
    const val BK_CHECK_POOL_FIELD = "bkCheckPoolField" // 需检查JOB({0})的Pool字段
    const val BK_CONSTRUCTION_MACHINE_NOT_SUPPORTED = "bkConstructionMachineNotSupported" // # 注意：暂不支持当前类型的构建机
    // # 注意：【{0}】的环境【{1}】在新业务下可能不存在，
    const val BK_NOT_EXIST_UNDER_NEW_BUSINESS = "bkNotExistUnderNewBusiness"
    // 请手动修改成存在的环境，并检查操作系统是否正确
    const val BK_CHECK_OPERATING_SYSTEM_CORRECT = "bkCheckOperatingSystemCorrect"
    // # 注意：【{0}】的节点【{1}】在新业务下可能不存在，
    const val BK_NODE_NOT_EXIST_UNDER_NEW_BUSINESS = "bkNodeNotExistUnderNewBusiness"
    const val BK_PLEASE_MANUALLY_MODIFY = "bkPleaseManuallyModify" // 请手动修改成存在的节点
    // # 注意：【{0}】仅对PCG业务可见，请检查当前业务是否属于PCG！ \n
    const val BK_ONLY_VISIBLE_PCG_BUSINESS = "bkOnlyVisiblePcgBusiness"
    // # 注意：[插件]工蜂CI不支持依赖蓝盾项目的服务（如凭证、节点等），
    const val BK_WORKER_BEE_CI_NOT_SUPPORT = "bkWorkerBeeCiNotSupport"
    const val BK_MODIFICATION_GUIDELINES = "bkModificationGuidelines"
    const val BK_BUILD_INFO = "bkBuildInfo" // 构建信息
    const val BK_DETAIL = "bkDetail" // 详情
    const val BK_BUILD_STATUS = "bkBuildStatus" // 构建状态
    const val BK_BUILD_VARIABLES = "bkBuildVariables" // 构建变量
    const val BK_BUILD_VARIABLES_VALUE = "bkBuildVariablesValue" // 构建变量的值
    const val BK_BUILD_HISTORY = "bkBuildHistory" // 构建历史
    const val BK_PENDING_APPROVAL = "bkPendingApproval" // 步骤等待审核(Pending approval)
    const val BK_REVIEWERS = "bkReviewers" // 待审核人(Reviewers)
    const val BK_REVIEWER = "bkReviewer" // 审核人(Reviewer)
    const val BK_DESCRIPTION = "bkDescription" // 审核说明(Description)
    const val BK_PARAMS = "bkParams" // 审核参数(Params)
    const val BK_AUDIT_RESULTS_APPROVE = "bkAuditResultsApprove" // 审核结果(result)：继续(Approve)
    const val BK_AUDIT_RESULTS_REJECT = "bkAuditResultsReject" // 审核结果(result)：驳回(Reject)
    const val BK_FINAL_APPROVAL = "bkFinalApproval" // 步骤审核结束(Final approval)
    const val BK_REVIEW_COMMENTS = "bkReviewComments" // 审核意见(Review comments)
    const val BK_QUEUE_TIMEOUT = "bkQueueTimeout" // 排队超时(Queue timeout)
    const val BK_JOB_QUEUE_TIMEOUT = "bkJobQueueTimeout" // Job排队超时，请检查并发配置/Queue timeout
    const val BK_TRIGGER_USER = "bkTriggerUser" // 触发人(trigger user)
    const val BK_START_USER = "bkStartUser" // 执行人(start user)
    // [自定义变量全部满足时不运行](Don‘t run it when all the custom variables are matched)
    const val BK_WHEN_THE_CUSTOM_VARIABLES_ARE_ALL_SATISFIED = "bkWhenTheCustomVariablesAreAllSatisfied"
    // [自定义变量全部满足时运行](Run it when all the custom variables are matched)
    const val BK_CUSTOM_VARIABLES_ARE_ALL_SATISFIED = "bkCustomVariablesAreAllSatisfied"
    const val BK_CHECK_TASK_RUN_CONDITION = "bkCheckTaskRunCondition" // 检查插件运行条件/Check Task Run Condition:
    const val BK_TASK_DISABLED = "bkTaskDisabled" // [插件被禁用](Task disabled) = true
    // [只有前面有任务失败时才运行](Only when a previous task has failed)
    const val BK_ONLY_WHEN_PREVIOUS_TASK_HAS_FAILED = "bkOnlyWhenPreviousTaskHasFailed"
    // [即使前面有插件运行失败也运行，除非被取消才不运行] (Even if a previous task has failed, unless the build was canceled)
    const val BK_IT_DOES_NOT_RUN_UNLESS_IT_IS_CANCELED = "bkItDoesNotRunUnlessItIsCanceled"
    // [即使前面有插件运行失败也运行，即使被取消也运行](Run even if a previous plugin failed, and run even if it was cancelled)
    const val BK_RUNS_EVEN_IF_CANCELED = "bkRunsEvenIfCanceled"
    const val BK_JOB_FAILURE_OR_CANCEL = "bkJobFailureOrCancel" // Job失败或被取消(Job failure or cancel) skip=true
    const val BK_CHECK_JOB_RUN_CONDITION = "bkCheckJobRunCondition" // 检查Job运行条件/Check Job Run Condition:
    const val BK_RELEASE_LOCK = "bkReleaseLock" // 释放互斥组锁(Release Lock)
    const val BK_GET_LOCKED = "bkGetLocked" // 获得锁定(Matched) 锁定期(Exp):
    const val BK_QUEUE_DISABLED = "bkQueueDisabled" // 未开启排队(Queue disabled)
    // 当前排队数(Queuing)[{0}], 已等待(Waiting)[{1}} seconds]
    const val BK_CURRENT_NUMBER_OF_QUEUES = "bkCurrentNumberOfQueues"
    const val BK_QUEUE_FULL = "bkQueueFull" // 队列满(Queue full)
    const val BK_ENQUEUE = "bkEnqueue" // 当前排队数(Queuing)[{0}]. 入队等待(Enqueue)
    // Job#{0}|互斥组Mutex[{1}]|
    const val BK_MUTUALLY_EXCLUSIVE_GROUPS = "bkMutuallyExclusiveGroups"
    const val BK_LOCKED = "bkLocked" // 锁定中(Running)
    const val BK_CLICK = "bkClick" // 查看
    const val BK_CURRENT = "bkCurrent" // 当前(Current)
    const val BK_PREVIOUS_STAGE_CANCEL = "bkPreviousStageCancel" // [上游 Stage 取消时](Previous Stage Cancel):
    const val BK_PREVIOUS_STAGE_FAILED = "bkPreviousStageFailed" // [上游 Stage 失败时](Previous Stage Failed):
    const val BK_PREVIOUS_STAGE_SUCCESS = "bkPreviousStageSuccess" // [上游 Stage 成功时](Previous Stage Success):
    const val BK_UNEXECUTE_TASK = "bkUnexecuteTask" // 终止构建，跳过(UnExecute Task)
    const val BK_CONDITION_INVALID = "bkConditionInvalid" // 执行条件判断失败(Condition Invalid)
    // [SystemLog]收到终止指令(UnExecute PostAction Task)
    const val BK_UNEXECUTE_POSTACTION_TASK = "bkUnexecutePostactionTask"
    const val BK_MAX_PARALLEL = "bkMaxParallel" // 并行上限/Max parallel:
    const val BK_MANUALLY_SKIPPED = "bkManuallySkipped" // 被手动跳过 Manually skipped
    const val BK_EVENT = "bkEvent" // {0}事件
    const val BK_WAREHOUSE_EVENTS = "bkWarehouseEvents" // 仓库事件
    const val BK_VM_START_ALREADY = "bkVmStartAlready" // 重复启动构建机/VM Start already:
    const val BK_CONTINUE_WHEN_ERROR = "bkContinueWhenError" // 失败自动跳过/continue when error
    const val BK_MUTEX_WAITING = "bkMutexWaiting" // 互斥中(Mutex waiting)
    const val BK_QUEUING = "bkQueuing" // 排队中(Queuing)
    const val BK_PENDING = "bkPending" // 审核中(Pending)
    const val BK_QUALITY_IN = "bkQualityIn" // 质量红线(准入)
    const val BK_QUALITY_OUT = "bkQualityOut" // 质量红线(准出)
    const val BK_BUILD_FINISHED_AND_DENY_PAUSE = "bkBuildFinishedAndDenyPause" // 构建已结束，禁止暂停请求
}

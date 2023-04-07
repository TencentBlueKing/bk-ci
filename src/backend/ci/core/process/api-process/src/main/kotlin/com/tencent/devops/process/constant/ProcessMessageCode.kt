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
 *    25：prebuild-预建 26: dispatcher-kubernetes 27：buildless 28: lambda 29: stream  30: worker 31: dispatcher-docker）
 * 4、最后3位数字代表具体微服务模块下返回给客户端的业务逻辑含义（如001代表系统服务繁忙，建议一个模块一类的返回码按照一定的规则制定）
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

    const val BK_PIPELINE_SINGLE_BUILD = "bkPipelineSingleBuild" // 当前流水线已设置为同时只能运行一个构建任务，开始排队！
    const val BK_MUTEX_GROUP_SINGLE_BUILD = "bkMutexGroupSingleBuild" // 当前互斥组[{0}]同时只能运行一个构建任务，开始排队！
    const val BK_NON_TIMED_TRIGGER_SKIP = "bkNonTimedTriggerSkip" // 非定时触发，直接跳过
    const val BK_FIRST_STAGE_ENV_NOT_EMPTY = "bkFirstStageEnvNotEmpty" // 第一阶段的环境不能为空
    const val BK_QUALITY_CHECK_SUCCEED = "bkQualityCheckSucceed" // 质量红线({0})检测已通过
    const val BK_QUALITY_CHECK_INTERCEPTED = "bkQualityCheckIntercepted" // 质量红线({0})检测被拦截
    const val BK_QUALITY_TO_BE_REVIEW = "bkQualityToBeReview" // 质量红线({0})待审核!审核人：{1}
    const val BK_POLLING_WAIT_FOR_QUALITY_RESULT = "bkPollingWaitForQualityResult" // 第 {0} 次轮询等待红线结果
    const val BK_QUALITY_CHECK_RESULT = "bkQualityCheckResult" // 检测红线结果
    const val BK_AUDIT_TIMEOUT = "bkAuditTimeout" // 审核超时
    const val BK_AUDIT_RESULT = "bkAuditResult" // 步骤审核结束，审核结果：[{0}]，审核人：{1}
    const val BK_PROCESSING_CURRENT_REPORTED_TASK_PLEASE_WAIT = "bkProcessingCurrentReportedTaskPleaseWait" // 正在处理当前上报的任务, 请稍等。。。
    const val BK_ENV_NOT_YET_SUPPORTED = "bkEnvNotYetSupported" // 尚未支持 {0} {1}，请联系 DevOps-helper 添加对应版本
    const val BK_VIEW_ID_AND_NAME_CANNOT_BE_EMPTY_TOGETHER = "bkViewIdAndNameCannotBeEmptyTogether" // <viewId>和<viewName>不能同时为空, 填<viewName>时需同时填写参数<isProject>
    const val BK_TRIGGERED_BY_GIT_EVENT_PLUGIN = "bkTriggeredByGitEventPlugin" // 因【Git事件触发】插件中，MR Request Hook勾选了【MR为同源同目标分支时，等待队列只保留最新触发的任务】配置，该次构建已被新触发的构建
    const val BK_BUILD_IN_REVIEW_STATUS = "bkBuildInReviewStatus" // 项目【{0}】下的流水线【{1}】#{2} 构建处于待审核状态
    const val BK_CHECK_THE_WEB_DATA = "bkCheckTheWebData" // 查web端数据:
    const val BK_CHECK_FILE_COUNT_AND_VERSION = "bkCheckFileCountAndVersion" // 查文件个数、版本:
    const val BK_QUERY_PIPELINE_INFO = "bkQueryPipelineInfo" // 查流水线信息:
    const val BK_QUERY_FAVORITE_PIPELINE = "bkQueryFavoritePipeline" // 查询收藏的流水线:
    const val BK_PROJECT_NO_PIPELINE = "bkProjectNoPipeline" // 项目下无流水线
    const val BK_NO_MATCHING_STARTED_PIPELINE = "bkNoMatchingStartedPipeline"//未匹配到启用流水线
    const val BK_USER_NO_PIPELINE_EXECUTE_PERMISSIONS = "bkUserNoPipelineExecutePermissions" // 用户（{0}) 没有流水线({1})的执行权限
    const val BK_REMOTE_CALL_SOURCE_IP = "bkRemoteCallSourceIp" // 本次远程调用的来源IP是[$sourceIp]
    const val BK_OPERATE_PIPELINE_FAIL = "bkOperatePipelineFail" // {0}流水线失败
    const val BK_PIPELINE_NAME = "bkPipelineName" // 流水线名称
    const val BK_CREATOR = "bkCreator" // 创建人

    const val OK = 0

    const val ERROR_BUILD_TASK_SUBPIPELINEID_NULL = "2101001" // 子流水线id不存在
    const val ERROR_BUILD_TASK_SUBPIPELINEID_NOT_EXISTS = "2101002" // 子流水线不存在
    const val ERROR_PIPELINE_VIEW_MAX_LIMIT = "2101003" // 最多允许同时保存30个视图
    const val ERROR_PIPELINE_VIEW_NOT_FOUND = "2101004" // 视图({0})不存在
    const val ERROR_PIPELINE_VIEW_HAD_EXISTS = "2101005" // 视图({0})已存在
    const val ERROR_DEL_PIPELINE_VIEW_NO_PERM = "2101006" // 用户({0})无权限删除视图({1})
    const val ERROR_EDIT_PIPELINE_VIEW_NO_PERM = "2101007" // 用户({0})无权限编辑视图({1})
    const val ERROR_ATOM_NOT_FOUND = "2101010" // 插件不存在

    const val USER_NEED_PIPELINE_X_PERMISSION = "2101008" // 流水线: 用户无{0}权限
    const val ERROR_PIPELINE_CHANNEL_CODE = "2101009" // 流水线: 指定{0}的流水线渠道来源{1}不符合{2}
    const val ILLEGAL_PIPELINE_MODEL_JSON = "2101011" // 流水线: 流水线Model不合法
    const val OPERATE_PIPELINE_FAIL = "2101012" // 流水线: 流水线出现异常:{0}
    const val ERROR_PIPELINE_NAME_EXISTS = "2101013" // 流水线: 流水线名称已被使用
    const val ERROR_PIPELINE_TEMPLATE_CAN_NOT_EDIT = "2101014" // 流水线: 模板流水线不支持编辑
    const val ERROR_PIPELINE_NAME_TOO_LONG = "2101015" // 流水线名称过长
    const val ERROR_PIPELINE_IS_EXISTS = "2101016" // 流水线: 流水线已存在
    const val ERROR_QUALITY_TASK_NOT_FOUND = "2101017" // 流水线: 质量红线拦截的任务[{0}]不存在
    const val ERROR_QUALITY_REVIEWER_NOT_MATCH = "2101018" // 流水线: 用户({0})不在审核人员名单中
    const val CANCEL_BUILD_BY_OTHER_USER = "2101019" // 流水线: 流水线已经被{0}取消构建，请过{0}秒后再试
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
    const val ERROR_PIPELINE_NOT_EXISTS = "2101038" // 流水线不存在
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
    const val ERROR_PIPELINE_AGENT_STATUS_EXCEPTION = "2101050" // 第三方构建机状态异常
    const val ERROR_PIPELINE_DISPATCH_STORE_IMAGE_CODE_BLANK = "2101051" // 模型中使用了商店镜像，但code为空
    const val ERROR_PIPELINE_DISPATCH_STORE_IMAGE_VERSION_BLANK = "2101052" // 模型中使用了商店镜像，但version为空
    const val ERROR_PIPELINE_DISPATCH_VALUE_BLANK = "2101053" // 模型中非商店蓝盾源/第三方源的镜像value为空
    const val ERROR_PIPELINE_PARAMS_NAME_ERROR = "2101054" // 请使用英文命名流水线变量
    const val ERROR_PIPELINE_STAGE_NO_REVIEW_GROUP = "2101055" // Stage[{0}]准入配置不正确
    const val ERROR_PIPELINE_DESC_TOO_LONG = "2101056" // 流水线描述过长
    const val ILLEGAL_TIMER_INTERVAL_CRONTAB = "2101057" // 定时触发器的定时参数[{0}]不能秒级触发
    const val ERROR_PIPLEINE_INPUT = "2101058" // 流水线: 入参buildId与pipelineId不匹配
    const val MODEL_ATOMCODE_NOT_EXSIT = "2101059" // 流水线内包含插件市场不存在的插件
    const val MODEL_ATOMCODE_PROJECT_NOT_INSTALL = "2101060" // 流水线内存在该项目未安装的插件:[{0}]. 请先安装插件
    const val MODEL_DEFAULT_ATOMCODE_NOT_EXSIT = "2101061" // Model内包含不存在的内置插件
    const val ERROR_ATOM_RUN_BUILD_ENV_INVALID = "2101062" // 流水线: 插件[{0}]不能在该环境下运行
    const val ERROR_TEMPLATE_PIPELINE_IS_INSTANCING = "2101063" // 流水线: 模板下的流水线实例{0}正在更新中，请稍后再试
    const val ERROR_FINALLY_STAGE = "2101064" // 流水线: 每个Model只能包含一个FinallyStage，并且处于最后位置
    const val ERROR_FINALLY_STAGE_JOB_CONDITION = "2101065" // 流水线: finally stage下的[{0}]Job运行条件配置错误: {1}
    const val ERROR_NORMAL_STAGE_JOB_CONDITION = "2101066" // 流水线: 普通stage下的[{0}]Job运行条件配置错误: {0}
    const val ERROR_EMPTY_JOB = "2101067" // 流水线: Model信息不完整，Stage[{0}] Job[{1}]下没有插件
    const val ERROR_PIPELINE_MODEL_TOO_LARGE = "2101068" // 流水线: 流水线模型超限，阈值为[{0}]个字符，请联系发布者
    const val ERROR_PIPELINE_MODEL_COMPONENT_NUM_TOO_LARGE = "2101069" // 流水线: 流水线下[{0}]的[{1}]数量超限，阈值为[{2}]个，请联系发布者
    const val ERROR_ATOM_PARAM_VALUE_TOO_LARGE = "2101070" // 流水线: 插件[{0}]的参数[{1}]值超限，阈值为[{2}]个字符，请联系发布者
    const val ERROR_PIPELINE_STAGE_REVIEW_GROUP_NO_USER = "2101072" // Stage[{0}]的审核组[{1}]没有未配置可执行人
    const val ERROR_PIPELINE_STAGE_REVIEW_GROUP_NOT_FOUND = "2101073" // Stage[{0}]的审核组ID[{1}]不存在
    const val ERROR_PIPELINE_STAGE_POSITION_NOT_FOUND = "2101074" // Stage[{0}]的准入准出标识[{1}]不正确
    const val ERROR_PIPELINE_START_WITH_ERROR = "2101075" // 流水线启动准备失败{0}
    const val ERROR_TEMPLATE_NOT_UPDATE = "2101076" // 该模板无法更新
    const val ERROR_PIPELINE_MODEL_MATRIX_YAML_CHECK_ERROR = "2101077" // matrix yaml 格式错误
    const val ERROR_TEMPLATE_VERSION_COUNT_EXCEEDS_LIMIT = "2101078" // 模板的版本数量不能超过{0}个
    const val FAIL_TEMPLATE_UPDATE_NUM_TOO_BIG = "2101079" // 模板实例更新数量[{0}]超过系统规定的最大值{1}，请调整参数或咨询助手
    const val ERROR_START_BUILD_PROJECT_UNENABLE = "2101080" // 流水线: 已禁用的项目不能启动
    const val ERROR_BUILD_EXPIRED_CANT_RETRY = "2101081" // 构建数据已过期，请使用rebuild进行重试/Please use rebuild
    const val ERROR_PIPELINE_STAGE_REVIEW_VARIABLES_OUT_OF_LENGTH = "2101082" // Stage审核参数{0}超出4000长度限制
    const val ERROR_PIPELINE_CAN_NOT_DELETE_WHEN_HAVE_BUILD_RECORD = "2101083" // 流水线版本还存在构建记录，不允许删除
    const val ERROR_JOB_TIME_OUT_PARAM_VAR = "2101084" // Job[{0}]的超时配置的流水线变量[{1}]值[{2}]超出合理范围[{3}](分钟)
    const val ERROR_TASK_TIME_OUT_PARAM_VAR = "2101085" // Job[{0}]的Task[{1}]的超时配置的流水线变量[{2}]值[{3}]超出合理范围[{4}](分钟)
    const val ERROR_JOB_MUTEX_TIME_OUT_PARAM_VAR = "2101086" // Job[{0}]的互斥组[{1}]超时配置的流水线变量[{2}]值[{3}]超出合理范围[{4}](分钟)
    const val ERROR_YAML_FORMAT_EXCEPTION_NEED_PARAM = "2101087" // {0} 中的step必须包含uses或run或checkout!
    const val ERROR_YAML_FORMAT_EXCEPTION_LENGTH_LIMIT_EXCEEDED = "2101088" // "{0} job.id 超过长度限制64 {1}}"
    const val ERROR_YAML_FORMAT_EXCEPTION = "2101089" // {0} 中 {1} 格式有误,应为 {2}, error message:${3}
    const val ERROR_YAML_FORMAT_EXCEPTION_STEP_ID_UNIQUENESS = "2101090" // 请确保step.id唯一性!({0})
    const val ERROR_YAML_FORMAT_EXCEPTION_CHECK_STAGE_LABEL = "2101091" // 请核对Stage标签是否正确
    const val ERROR_YAML_FORMAT_EXCEPTION_SERVICE_IMAGE_FORMAT_ILLEGAL = "2101092" // STREAM Service镜像格式非法
    const val ERROR_YAML_FORMAT_EXCEPTION_ENV_QUANTITY_LIMIT_EXCEEDED = "2101093" // {0}配置Env数量超过100限制!
    const val ERROR_YAML_FORMAT_EXCEPTION_ENV_VARIABLE_LENGTH_LIMIT_EXCEEDED = "2101094" // {0}Env单变量{1}长度超过{2}字符!({3})
    const val ADD_PIPELINE_TIMER_TRIGGER_SAVE_FAIL = "2101095" // 添加流水线的定时触发器保存失败！可能是定时器参数过长！
    const val ERROR_YAML_FORMAT_EXCEPTION_VARIABLE_NAME_ILLEGAL = "2101096" // 变量名称必须是英文字母、数字或下划线(_)
    const val ERROR_JOB_MATRIX_YAML_CONFIG_ERROR = "2101097" // Job[{0]的矩阵YAML配置错误:
    const val ERROR_PIPELINE_ID_NOT_PROJECT_PIPELINE = "2101098" // {0} 非 {1} 流水线
    const val ERROR_NO_MATCHING_PIPELINE = "2101099" // 没有找到对应的流水线
    const val ERROR_SUB_PIPELINE_NOT_ALLOWED_CIRCULAR_CALL = "2101100" // 子流水线不允许循环调用,循环流水线:projectId:{0},pipelineId:{1}
    const val BUILD_RESOURCE_NOT_EXIST = "2101106" // {0}构建资源不存在，请检查yml配置.

        // 通用参数错误
    const val ERROR_RETRY_3_FAILED = "2101989" // 重试3次失败
    const val ERROR_UPDATE_FAILED = "2101990" // 更新失败: {0}
    const val ERROR_NO_PUBLIC_WINDOWS_BUILDER = "2101900" // Windows暂时没有公共构建机可用，请联系持续集成助手添加
    const val ERROR_DUPLICATE_BUILD_RETRY_ACT = "2101901" // 重复的重试构建请求
    const val ERROR_NO_PARAM_IN_JOB_CONDITION = "2101902" //  请设置Job运行的自定义变量
    const val ERROR_TIMEOUT_IN_RUNNING = "2101903" //  {0}运行达到({1})分钟，超时结束运行!
    const val ERROR_TIMEOUT_IN_BUILD_QUEUE = "2101904" //  排队超时，取消运行! [{0}]
    const val ERROR_PARUS_PIEPLINE_IS_RUNNINT = "2101905" // 暂停的流水线已开始运行
    const val ERROR_ELEMENT_TOO_LONG = "2101906" // {0} element大小越界
    const val ERROR_JOB_RUNNING = "2101907" // job非完成态，不能进行重试
    const val ERROR_RETRY_STAGE_NOT_FAILED = "2101911" // stage非失败状态，不能进行重试
    const val ERROR_PULLING_LATEST_VERSION_NUMBER_EXCEPTION = "2101912" // 拉取最新版本号出现异常,重试{0}次失败
    const val ERROR_BACKGROUND_SERVICE_TASK_EXECUTION = "2101913"//后台服务任务执行出错
    const val ERROR_BACKGROUND_SERVICE_RUNNING_ERROR = "2101914" // 后台服务运行出错
    const val ERROR_VIEW_NOT_FOUND_IN_PROJECT = "2101915" // 在项目 {0} 下未找到{1}视图{2}
    const val ERROR_MAX_PIPELINE_COUNT_PER_PROJECT = "2101916" // 该项目最多只能创建{0}条流水线

    const val ERROR_NO_BUILD_EXISTS_BY_ID = "2101501" // 流水线构建[{0}]不存在
    const val ERROR_NO_PIPELINE_EXISTS_BY_ID = "2101502" // 流水线[{0}]不存在
    const val ERROR_SUBPIPELINE_CYCLE_CALL = "2101503" // 子流水线循环调用
    const val ERROR_NO_STAGE_EXISTS_BY_ID = "2101504" // 构建中Stage[{0}]不存在
    const val ERROR_STAGE_IS_NOT_PAUSED = "2101505" // 构建中Stage[{0}]未处于等待把关状态
    const val ERROR_CONDITION_EXPRESSION_PARSE = "2101506" // 执行条件表达式解析失败
    const val ERROR_TRIGGER_REVIEW_ABORT = "2101507" // 触发审核未通过
    const val ERROR_TRIGGER_NOT_UNDER_REVIEW = "2101508" // 触发不在审核状态中
    const val ERROR_GENERATE_REMOTE_TRIGGER_TOKEN_FAILED = "2101509" // 生成远程触发token失败
    const val ERROR_NO_BUILD_RECORD_FOR_CORRESPONDING_SUB_PIPELINE = "2101510" // 找不到对应子流水线的构建记录
    const val ERROR_NO_CORRESPONDING_SUB_PIPELINE = "2101511" // "找不到对应子流水线"
    const val ERROR_USER_NO_PERMISSION_GET_PIPELINE_INFO = "2101512" // 用户（{0}) 无权限获取流水线({1})信息({2})
    const val ERROR_SUB_PIPELINE_PARAM_FILTER_FAILED = "2101513" // 子流水线参数过滤失败
    const val ERROR_NO_PERMISSION_PLUGIN_IN_TEMPLATE = "2101514" // 模版下存在无权限的插件
    const val ERROR_RECORD_PARSE_FAILED = "2101515" // 解析构建记录出错
    const val MAXIMUM_NUMBER_CONCURRENCY_ILLEGAL = "2101516" // 最大并发数量非法
    const val PIPELINE_ORCHESTRATIONS_NUMBER_ILLEGAL = "2101517" // 流水线编排数量非法
    const val MAXIMUM_QUEUE_LENGTH_ILLEGAL = "2101518" // 最大排队时长非法
    const val MAXIMUM_NUMBER_QUEUES_ILLEGAL = "2101519" // 最大排队数量非法

    // 导出数据错误
    const val ERROR_EXPORT_OUTPUT_CONFLICT = "2101200" // 变量名[{0}]来源不唯一，请修改变量名称或增加插件输出命名空间：{1}

    // 构建时错误
    const val ERROR_BUILD_TASK_BCS_PARAM_BCSAPPINSTID = "2101111" // bcsAppInstId 不是 init
    const val ERROR_BUILD_TASK_BCS_PARAM_CATEGORY = "2101112" // category 不是  init
    const val ERROR_BUILD_TASK_BCS_PARAM_BCSINSTNUM = "2101113" // bcsInstNum 不是  init
    const val ERROR_BUILD_TASK_BCS_PARAM_INSTVERSIONID = "2101114" // instVersionId 不是  init
    const val ERROR_BUILD_TASK_BCS_OPERATE_FAIL = "2101115" // BCS 操作失败
    const val ERROR_BUILD_TASK_BCS_PARAM_NAMESPACE_VAR = "2101116" // instVersionId 不是 init
    const val ERROR_BUILD_TASK_BCS_PARAM_VERSIONID = "2101117" // versionId 不是 init
    const val ERROR_BUILD_TASK_BCS_PARAM_SHOW_VERSIONID = "2101118" // showVersionId 不是 init
    const val ERROR_BUILD_TASK_BCS_PARAM_INSTANCE_ENTITY = "2101119" // instanceEntity 不是 init
    const val ERROR_BUILD_TASK_BCS_CREATE_INSTANCE_FAIL = "2101120" // 创建实例失败
    const val ERROR_BUILD_TASK_ENV_NAME_IS_NULL = "2101121" // EnvName 不是 init
    const val ERROR_BUILD_TASK_ENV_ID_IS_NULL = "2101122" // EnvId 不是 init
    const val ERROR_BUILD_TASK_ENV_NAME_NOT_EXISTS = "2101123" // 以下这些环境名称不存在,请重新修改流水线！$noExistsEnvNames
    const val ERROR_BUILD_TASK_USER_ENV_NO_OP_PRI = "2101124" // 用户没有操作这些环境的权限！环境：$noExistsEnvNames
    const val ERROR_BUILD_TASK_USER_ENV_ID_NOT_EXISTS = "2101125" //  "以下这些环境id不存在,请重新修改流水线！id：$noExistsEnvIds"
    const val ERROR_BUILD_TASK_TARGETENV_TYPE_IS_NULL = "2101126" // 支持 目标环境类型: {0}
    const val ERROR_BUILD_TASK_CDN_FAIL = "2101127" // "分发CDN失败
    const val ERROR_BUILD_TASK_JOB_PUSH_FILE_FAIL = "2101128"
    const val ERROR_BUILD_TASK_IDX_FILE_NOT_EXITS = "2101129" // 索引文件不存在
    const val ERROR_BUILD_TASK_ZHIYUN_FAIL = "2101130" // 织云操作失败,织云返回错误信息：$msg
    const val ERROR_BUILD_TASK_ZHIYUN_UPGRADE_FAIL = "2101131" // 织云异步升级失败,织云返回错误信息：$msg

    const val ERROR_BUILD_TASK_ACROSS_PROJECT_PARAM_PATH = "2101132" // 这路径 不是 init
    const val ERROR_BUILD_TASK_ACROSS_PROJECT_PARAM_CUSTOMIZED = "2101133"
    const val ERROR_BUILD_TASK_ACROSS_PROJECT_PARAM_TARGETPROJECTID = "2101134"

    const val ERROR_BUILD_TASK_QUALITY_IN = "2101137" // 质量红线(准入)检测失败


    const val ERROR_BUILD_TASK_QUALITY_IN_INTERCEPT = "2101908" // 质量红线(准入)配置有误:
    const val ERROR_BUILD_TASK_QUALITY_OUT = "2101909" // 质量红线(准出)检测失败

    const val ERROR_BUILD_TASK_QUALITY_OUT_INTERCEPT = "2101910" // 质量红线(准出)配置有误：

    const val ERROR_PARAM_PROJEC_ID_NULL = "2101101" // 项目ID为空
    const val ERROR_PARAM_USER_ID_NULL = "2101102" // 用户ID为空
    const val ERROR_PARAM_PIPELINE_ID_NULL = "2101103" // 参数：流水线ID为空
    const val ERROR_PARAM_PIPELINE_NAME_TOO_LONG = "2101104" // 参数：流水线名称过长

    // 权限错误 210198开头
    const val ERROR_PERMISSION_VIEW_NEED = "2101981" // 无查看权限
    const val ERROR_PERMISSION_CREATE_NEED = "2101982" // 无写权限
    const val ERROR_PERMISSION_EXECUTE_NEED = "2101983" // 无执行权限
    const val ERROR_PERMISSION_DELETE_NEED = "2101984" // 无删除权限
    const val ERROR_PERMISSION_LIST_NEED = "2101985" // 无列表权限
    const val ERROR_PERMISSION_NOT_IN_PROJECT = "2101992" // 非项目成员
    const val ERROR_PERMISSION_NOT_PROJECT_MANAGER = "2101991" // {0}非项目{1}管理员

    // 流水线模块业务错误21011
    const val ERROR_DEL_PIPELINE_TIMER_QUARTZ = "2101107" // 流水线的定时Quartz任务删除失败

    const val ERROR_PIPELINE_DENY_RUN = "2101197" // 流水线不能执行
    const val ERROR_PIPELINE_IS_RUNNING_LOCK = "2101198" // 流水线正在运行中，锁定
    const val ERROR_PIPELINE_TIMER_SCM_NO_CHANGE = "2101190" // 流水线定时触发时代码没有变更
    const val ERROR_PIPELINE_SUMMARY_NOT_FOUND = "2101191" // 异常：流水线的基础构建数据Summary不存在，请联系管理员
    const val ERROR_PIPELINE_IS_NOT_THE_LATEST = "2101192" // 异常：保存已拒绝，因为保存流水线时已不是最新版本
    const val ERROR_RESTART_EXSIT = "2101193" // 流水线: 待restart构建{0}已在restart中

    // callback error
    const val ERROR_CALLBACK_URL_INVALID = "2101180" // 回调的url非法
    const val USER_NEED_PROJECT_X_PERMISSION = "2101181" // 用户（{0}）无（{1}）项目权限
    const val ERROR_CALLBACK_HISTORY_NOT_FOUND = "2101182" // 回调历史记录({0})不存在
    const val ERROR_CALLBACK_REPLY_FAIL = "2101183" // 回调重试失败
    const val ERROR_CALLBACK_NOT_FOUND = "2101184" // 回调记录({0})不存在
    const val ERROR_CALLBACK_SAVE_FAIL = "2101185" // 创建callback失败,失败原因:{0}

    const val ERROR_PIPELINE_DEPENDON_CYCLE = "2101301" // ({0})与({1})的jobId循环依赖
    const val ERROR_PIPELINE_JOBID_EXIST = "2101302" // ({0})的jobId({1})已存在
    const val ERROR_PIPELINE_DEPENDEON_NOT_EXIST = "2101303" // job:({0})依赖的({1})不存在

    const val BUILD_MSG_LABEL = "2101310" // 构建信息
    const val BUILD_MSG_MANUAL = "2101311" // 手动触发
    const val BUILD_MSG_TIME = "2101312" // 定时触发
    const val BUILD_MSG_REMOTE = "2101313" // 远程触发
    const val BUILD_MSG_WEBHOOK = "2101314" // webhook触发
    const val BUILD_MSG_SERVICE = "2101315" // 服务触发
    const val BUILD_MSG_PIPELINE = "2101316" // 流水线触发
    const val BUILD_MSG_DESC = "2101317" // 构建信息描述
    const val BUILD_MSG_TRIGGERS = "2101320" // 构建触发


    const val BUILD_WORKER_DEAD_ERROR = "2101318" // 其他构建进程挂掉的参考信息，自由添加方便打印卫通日志里


    const val BUILD_AGENT_DETAIL_LINK_ERROR = "2101319" // 构建机Agent详情链接


    const val ERROR_PARAM_MANUALREVIEW = "2101105" // 人工审核插件编辑时输入参数错误

    // 标签与标签组错误21014开头
    const val ERROR_GROUP_COUNT_EXCEEDS_LIMIT = "2101401" // 一个项目标签组不能超过10个
    const val ERROR_LABEL_COUNT_EXCEEDS_LIMIT = "2101402" // 同一分组下最多可添加12个标签
    const val ERROR_LABEL_NAME_TOO_LONG = "2101403" // 一个标签最多输入20个字符

    // 流水线组错误21016开头
    const val ERROR_VIEW_GROUP_NO_PERMISSION = "2101601" // 没有修改流水线组权限
    const val ERROR_VIEW_GROUP_IS_PROJECT_NO_SAME = "2101602" // 流水线组的视图范围不一致
    const val ERROR_VIEW_EXCEED_THE_LIMIT = "2101603" // 流水线组创建太多了
    const val ERROR_VIEW_DUPLICATE_NAME = "2101604" // 流水线组名称重复
    const val ERROR_VIEW_NAME_ILLEGAL = "2101605" // 流水线组名称不合法
}

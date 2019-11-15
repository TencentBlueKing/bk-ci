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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
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
 * 返回码制定规则（除开0代表成功外，为了兼容历史接口成功状态都是返回0）：
 * 1、返回码总长度为7位，
 * 2、前2位数字代表系统名称（如21代表持续集成平台）
 * 3、第3位和第4位数字代表微服务模块（00：common-公共模块 01：process-流水线 02：artifactory-版本仓库 03:dispatch-分发 04：dockerhost-docker机器
 *    05:environment-持续集成环境 06：experience-版本体验 07：image-镜像 08：log-持续集成日志 09：measure-度量 10：monitoring-监控 11：notify-通知
 *    12：openapi-开放api接口 13：plugin-插件 14：quality-质量红线 15：repository-代码库 16：scm-软件配置管理 17：support-持续集成支撑服务
 *    18：ticket-证书凭据 19：project-项目管理 20：store-商店）
 * 4、最后3位数字代表具体微服务模块下返回给客户端的业务逻辑含义（如001代表系统服务繁忙，建议一个模块一类的返回码按照一定的规则制定）
 * 5、系统公共的返回码写在CommonMessageCode这个类里面，具体微服务模块的返回码写在相应模块的常量类里面
 *
 * @since: 2018-11-09
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */
object ProcessMessageCode {

    const val OK = 0

    // 通用参数错误
    const val ERROR_RETRY_3_FAILED = 2100001
    const val ERROR_UPDATE_FAILED = 2100002 // 更新失败: {0}

    const val ERROR_NO_PUBLIC_WINDOWS_BUILDER = 2101991 // Windows暂时没有公共构建机可用，请联系持续集成助手添加
    const val ERROR_DUPLICATE_BUILD_RETRY_ACT = 2101901 // 重复的重试构建请求
    const val ERROR_NO_PARAM_IN_JOB_CONDITION = 2101902 //  请设置Job运行的自定义变量
    const val ERROR_TIMEOUT_IN_RUNNING = 2101903 //  {0}运行达到({1})分钟，超时结束运行!
    const val ERROR_TIMEOUT_IN_BUILD_QUEUE = 2101904 //  排队超时，取消运行! [{0}]

    const val ERROR_NO_BUILD_EXISTS_BY_ID = 2101100 // 流水线构建[{0}]不存在
    const val ERROR_NO_PIPELINE_EXISTS_BY_ID = 2101101 // 流水线[{0}]不存在
    const val ERROR_SUBPIPELINE_CYCLE_CALL = 2101102 // 子流水线循环调用

    const val ERROR_BUILD_TASK_SUBPIPELINEID_NULL = 2101001 // 子流水线id不存在
    const val ERROR_BUILD_TASK_SUBPIPELINEID_NOT_EXISTS = 2101002 // 子流水线不存在
    const val ERROR_ATOM_NOT_FOUND = 2101010 // 插件不存在
    const val ERROR_PIPELINE_NOT_EXISTS = 2101038 // 流水线不存在
    const val ERROR_PIPELINE_MODEL_NOT_EXISTS = 2101039 // 流水线的模型不存在
    const val ERROR_PIPELINE_NODEL_CONTAINER_NOT_EXISTS = 2101040 // 流水线的模型中指定构建容器{0}不存在
    const val ERROR_SAVE_PIPELINE_TIMER = 2101041 // 流水线的定时触发器保存失败
    const val ERROR_DEL_PIPELINE_TIMER = 2101043 // 流水线的定时触发器删除失败
    const val ERROR_PIPELINE_LOCK = 2101047 // 流水线锁定
    const val ERROR_PIPELINE_QUEUE_FULL = 2101049 // 流水线队列满
    const val ERROR_PIPELINE_AGENT_STATUS_EXCEPTION = 2101050 // 第三方构建机状态异常

    const val ERROR_BUILD_UNKNOWN_ERROR = 2101001 //
    const val ERROR_BUILD_UNKNOWN_ERROR_2 = 2101002 //
    // 构建时错误
    const val ERROR_BUILD_TASK_BCS_PARAM_BCSAPPINSTID = 2101111 // bcsAppInstId is not init
    const val ERROR_BUILD_TASK_BCS_PARAM_CATEGORY = 2101112 // category is not init
    const val ERROR_BUILD_TASK_BCS_PARAM_BCSINSTNUM = 2101113 // bcsInstNum is not init
    const val ERROR_BUILD_TASK_BCS_PARAM_INSTVERSIONID = 2101114 // instVersionId is not init
    const val ERROR_BUILD_TASK_BCS_OPERATE_FAIL = 2101115 // BCS operate failed
    const val ERROR_BUILD_TASK_BCS_PARAM_NAMESPACE_VAR = 2101116 // instVersionId is not init
    const val ERROR_BUILD_TASK_BCS_PARAM_VERSIONID = 2101117 // versionId is not init
    const val ERROR_BUILD_TASK_BCS_PARAM_SHOW_VERSIONID = 2101118  // showVersionId is not init
    const val ERROR_BUILD_TASK_BCS_PARAM_INSTANCE_ENTITY = 2101119 // instanceEntity is not init
    const val ERROR_BUILD_TASK_BCS_CREATE_INSTANCE_FAIL = 2101120 // create instance fail
    const val ERROR_BUILD_TASK_ENV_NAME_IS_NULL = 2101121 // EnvName is not init
    const val ERROR_BUILD_TASK_ENV_ID_IS_NULL = 2101122 // EnvId is not init
    const val ERROR_BUILD_TASK_ENV_NAME_NOT_EXISTS = 2101123 // 以下这些环境名称不存在,请重新修改流水线！$noExistsEnvNames
    const val ERROR_BUILD_TASK_USER_ENV_NO_OP_PRI = 2101124 // 用户没有操作这些环境的权限！环境：$noExistsEnvNames
    const val ERROR_BUILD_TASK_USER_ENV_ID_NOT_EXISTS = 2101125 //  "以下这些环境id不存在,请重新修改流水线！id：$noExistsEnvIds"
    const val ERROR_BUILD_TASK_TARGETENV_TYPE_IS_NULL = 2101126 // Unsupported targetEnvType: $targetEnvType
    const val ERROR_BUILD_TASK_CDN_FAIL = 2101127 // "分发CDN失败
    const val ERROR_BUILD_TASK_JOB_PUSH_FILE_FAIL = 2101128
    const val ERROR_BUILD_TASK_IDX_FILE_NOT_EXITS = 2101129 // "Index file not exist")
    const val ERROR_BUILD_TASK_ZHIYUN_FAIL = 2101130 // 织云操作失败,织云返回错误信息：$msg
    const val ERROR_BUILD_TASK_ZHIYUN_UPGRADE_FAIL = 2101131 // 织云异步升级失败,织云返回错误信息：$msg

    const val ERROR_BUILD_TASK_ACROSS_PROJECT_PARAM_PATH = 2101121 // The path is not init
    const val ERROR_BUILD_TASK_ACROSS_PROJECT_PARAM_CUSTOMIZED = 2101122
    const val ERROR_BUILD_TASK_ACROSS_PROJECT_PARAM_TARGETPROJECTID = 2101123

    const val ERROR_BUILD_TASK_QUALITY_IN = 2101137 // 质量红线(准入)检测失败
    const val ERROR_BUILD_TASK_QUALITY_IN_INTERCEPT = 2101908 // 质量红线(准入)配置有误：Fail to find quality gate intercept element
    const val ERROR_BUILD_TASK_QUALITY_OUT = 2101909 // 质量红线(准出)检测失败
    const val ERROR_BUILD_TASK_QUALITY_OUT_INTERCEPT = 2101910 // 质量红线(准出)配置有误：Fail to find quality gate intercept element

    const val ERROR_PARAM_PROJEC_ID_NULL = 2101101 // 项目ID为空
    const val ERROR_PARAM_USER_ID_NULL = 2101102 // 用户ID为空
    const val ERROR_PARAM_PIPELINE_ID_NULL = 2101103 // 参数：流水线ID为空
    const val ERROR_PARAM_PIPELINE_NAME_TOO_LONG = 2101104 // 参数：流水线名称过长
    const val ERROR_PARAM_PIPELINE_NAME_DUP = 2101105 // 参数：流水线名称重复

    // 权限错误 210198开头
    const val ERROR_PERMISSION_VIEW_NEED = 2101981 // 无查看权限
    const val ERROR_PERMISSION_CREATE_NEED = 2101982 // 无写权限
    const val ERROR_PERMISSION_EXECUTE_NEED = 2101983 // 无执行权限
    const val ERROR_PERMISSION_DELETE_NEED = 2101984 // 无删除权限
    const val ERROR_PERMISSION_LIST_NEED = 2101985 // 无列表权限
    const val ERROR_PERMISSION_NOT_IN_PROJECT = 2101990 // 非项目成员

    // 流水线模块业务错误21011
    const val ERROR_ADD_PIPELINE_TIMER_QUARTZ = 2101105 // 流水线的定时Quartz任务保存失败
    const val ERROR_DEL_PIPELINE_TIMER_QUARTZ = 2101107 // 流水线的定时Quartz任务删除失败

    const val ERROR_PIPELINE_DENY_RUN = 2101197 // 流水线不能执行
    const val ERROR_PIPELINE_IS_RUNNING_LOCK = 2101198 // 流水线正在运行中，锁定
    const val ERROR_PIPELINE_TIMER_SCM_NO_CHANGE = 2101190 // 流水线定时触发时代码没有变更
}

/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

package com.tencent.devops.process.engine.common

/**
 * 错误码规范：8位，  16011234
 *  16是蓝盾平台  01是流水线微服务模块   1234是错误码
 *
 * @author irwinsun
 * @version 1.0
 */
const val OK = 0
const val QUEUE = 1

// 通用参数错误 160000开头
const val ERROR_RETRY_3_FAILED = 16000001
// ============== 流水线错误
// 流水线

const val ERROR_BUILD_UNKNOWN_ERROR = 16010001 //
const val ERROR_BUILD_UNKNOWN_ERROR_2 = 16010002 //
// 构建时错误
const val ERROR_BUILD_TASK_SUBPIPELINEID_NULL = 16010101 // 子流水线id不存在
const val ERROR_BUILD_TASK_SUBPIPELINEID_NOT_EXISTS = 16010102 // 子流水线不存在
const val ERROR_BUILD_TASK_SUBPIPELINE_START_FAIL = 16010103 // 子流水线启动失败

const val ERROR_ATOM_NOT_FOUND = 16010110 // 找不到原子
const val ERROR_BUILD_TASK_BCS_PARAM_BCSAPPINSTID = 16010111 // bcsAppInstId is not init
const val ERROR_BUILD_TASK_BCS_PARAM_CATEGORY = 16010112 // category is not init
const val ERROR_BUILD_TASK_BCS_PARAM_BCSINSTNUM = 16010113 // bcsInstNum is not init
const val ERROR_BUILD_TASK_BCS_PARAM_INSTVERSIONID = 16010114 // instVersionId is not init
const val ERROR_BUILD_TASK_BCS_OPERATE_FAIL = 16010115 // BCS operate failed
const val ERROR_BUILD_TASK_BCS_PARAM_NAMESPACE_VAR = 16010116 // instVersionId is not init
const val ERROR_BUILD_TASK_BCS_PARAM_VERSIONID = 16010117 // versionId is not init
const val ERROR_BUILD_TASK_BCS_PARAM_SHOW_VERSIONID = 16010118  // showVersionId is not init
const val ERROR_BUILD_TASK_BCS_PARAM_INSTANCE_ENTITY = 16010119 // instanceEntity is not init
const val ERROR_BUILD_TASK_BCS_CREATE_INSTANCE_FAIL = 16010120 // create instance fail
const val ERROR_BUILD_TASK_ENV_NAME_IS_NULL = 16010121 // EnvName is not init
const val ERROR_BUILD_TASK_ENV_ID_IS_NULL = 16010122 // EnvId is not init
const val ERROR_BUILD_TASK_ENV_NAME_NOT_EXISTS = 16010123 // 以下这些环境名称不存在,请重新修改流水线！$noExistsEnvNames
const val ERROR_BUILD_TASK_USER_ENV_NO_OP_PRI = 16010124 // 用户没有操作这些环境的权限！环境：$noExistsEnvNames
const val ERROR_BUILD_TASK_USER_ENV_ID_NOT_EXISTS = 16010125 //  "以下这些环境id不存在,请重新修改流水线！id：$noExistsEnvIds"
const val ERROR_BUILD_TASK_TARGETENV_TYPE_IS_NULL = 16010126 // Unsupported targetEnvType: $targetEnvType
const val ERROR_BUILD_TASK_CDN_FAIL = 16010127 // "分发CDN失败
const val ERROR_BUILD_TASK_JOB_PUSH_FILE_FAIL = 16010128
const val ERROR_BUILD_TASK_IDX_FILE_NOT_EXITS = 16010129 // "Index file not exist")
const val ERROR_BUILD_TASK_ZHIYUN_FAIL = 16010130 // 织云操作失败,织云返回错误信息：$msg
const val ERROR_BUILD_TASK_ZHIYUN_UPGRADE_FAIL = 16010131 // 织云异步升级失败,织云返回错误信息：$msg

const val ERROR_BUILD_TASK_ACROSS_PROJECT_PARAM_PATH = 16010121 // The path is not init
const val ERROR_BUILD_TASK_ACROSS_PROJECT_PARAM_CUSTOMIZED = 16010122
const val ERROR_BUILD_TASK_ACROSS_PROJECT_PARAM_TARGETPROJECTID = 16010123

const val ERROR_BUILD_TASK_QUALITY_IN = 16010137 // 质量红线(准入)检测失败
const val ERROR_BUILD_TASK_QUALITY_IN_INTERCEPT = 16010908 // 质量红线(准入)配置有误：Fail to find quality gate intercept element
const val ERROR_BUILD_TASK_QUALITY_OUT = 16010909 // 质量红线(准出)检测失败
const val ERROR_BUILD_TASK_QUALITY_OUT_INTERCEPT = 16010910 // 质量红线(准出)配置有误：Fail to find quality gate intercept element

const val ERROR_PARAM_PROJEC_ID_NULL = 16010101 // 项目ID为空
const val ERROR_PARAM_USER_ID_NULL = 16010102 // 用户ID为空
const val ERROR_PARAM_PIPELINE_ID_NULL = 16010103 // 参数：流水线ID为空
const val ERROR_PARAM_PIPELINE_NAME_TOO_LONG = 16010104 // 参数：流水线名称过长
const val ERROR_PARAM_PIPELINE_NAME_DUP = 16010105 // 参数：流水线名称重复

// 权限错误 1601098开头
const val ERROR_PERMISSION_VIEW_NEED = 16010981 // 无查看权限
const val ERROR_PERMISSION_CREATE_NEED = 16010982 // 无写权限
const val ERROR_PERMISSION_EXECUTE_NEED = 16010983 // 无执行权限
const val ERROR_PERMISSION_DELETE_NEED = 16010984 // 无删除权限
const val ERROR_PERMISSION_LIST_NEED = 16010985 // 无列表权限
const val ERROR_PERMISSION_NOT_IN_PROJECT = 16010990 // 非项目成员

// 流水线模块业务错误160111
const val ERROR_PIPELINE_NOT_EXISTS = 16011101 // 流水线不存在
const val ERROR_PIPELINE_MODEL_NOT_EXISTS = 16011102 // 流水线的模型不存在
const val ERROR_PIPELINE_NODEL_CONTAINER_NOT_EXISTS = 16011103 // 流水线的模型中指定构建容器{0}不存在
const val ERROR_SAVE_PIPELINE_TIMER = 16011104 // 流水线的定时触发器保存失败
const val ERROR_ADD_PIPELINE_TIMER_QUARTZ = 16011105 // 流水线的定时Quartz任务保存失败
const val ERROR_DEL_PIPELINE_TIMER = 16011106 // 流水线的定时触发器删除失败
const val ERROR_DEL_PIPELINE_TIMER_QUARTZ = 16011107 // 流水线的定时Quartz任务删除失败

const val ERROR_PIPELINE_DENY_RUN = 16011197 // 流水线不能执行
const val ERROR_PIPELINE_IS_RUNNING_LOCK = 16011198 // 流水线正在运行中，锁定
const val ERROR_PIPELINE_LOCK = 16011199 // 流水线锁定
const val ERROR_PIPELINE_TIMER_SCM_NO_CHANGE = 16011190 // 流水线定时触发时代码没有变更
const val ERROR_PIPELINE_QUEUE_FULL = 16011191 // 流水线排队满

const val ERROR_PIPELINE_AGENT_STATUS_EXCEPTION = 1601300 // 第三方构建机状态异常

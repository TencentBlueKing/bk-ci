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

package com.tencent.devops.dispatch.kubernetes.pojo

const val BK_READY_CREATE_KUBERNETES_BUILD_MACHINE = "bkReadyCreateKubernetesBuildMachine" // 准备创建kubernetes构建机...
const val BK_READY_CREATE_BCS_BUILD_MACHINE = "bkReadyCreateBcsBuildMachine" // 准备创建BCS(蓝鲸容器平台)构建机...
// 下发创建构建机请求成功，builderName: {0} 等待机器创建...
const val BK_REQUEST_CREATE_BUILD_MACHINE_SUCCESSFUL = "bkRequestCreateBuildMachineSuccessful"
const val BK_READY_CREATE_DEVCLOUD_BUILD_MACHINE = "bkReadyCreateDevcloudBuildMachine" // 准备创建devcloud构建机...
const val BK_GET_LOGIN_DEBUG_LINK_TIMEOUT = "bkGetLoginDebugLinkTimeout" // 获取登录调试链接接口超时
const val BK_BUILD_AND_PUSH_INTERFACE_EXCEPTION = "bkBuildAndPushInterfaceException" // 构建并推送接口异常
const val BK_BUILD_AND_PUSH_INTERFACE_RETURN_FAIL = "bkBuildAndPushInterfaceReturnFail" // 构建并推送接口返回失败
const val BK_BUILD_AND_PUSH_INTERFACE_TIMEOUT = "bkBuildAndPushInterfaceTimeout" // 构建并推送接口超时
const val BK_GET_BCS_TASK_STATUS_TIMEOUT = "bkGetBcsTaskStatusTimeout" // 获取BCS TASK状态接口超时
const val BK_GET_BCS_TASK_EXECUTION_TIMEOUT = "bkGetBcsTaskExecutionTimeout" // 获取BCS任务执行超时
const val BK_GET_BCS_TASK_STATUS_ERROR = "bkGetBcsTaskStatusError" // 获取BCS TASK状态接口异常
// 下发创建构建机请求成功，等待机器创建
const val BK_DISTRIBUTE_BUILD_MACHINE_REQUEST_SUCCESS = "bkDistributeBuildMachineRequestSuccess"
// 构建机创建成功，等待机器启动...
const val BK_MACHINE_BUILD_COMPLETED_WAITING_FOR_STARTUP = "bkMachineBuildCompletedWaitingForStartup"
const val BK_BUILD_MACHINE_CREATION_FAILED = "bkBuildMachineCreationFailed" // 构建机创建失败
const val BK_CONTAINER_IS_NOT_IN_DEBUG_OR_IN_USE = "bkContainerIsNotInDebugOrInUse" // 容器没有处于debug或正在占用中
const val BK_BUILD_MACHINE_STARTUP_FAILED = "bkBuildMachineStartupFailed" // 构建机启动失败，错误信息:{0}
const val BK_INTERFACE_REQUEST_TIMEOUT = "bkInterfaceRequestTimeout" // 接口请求超时
//  "创建构建机失败，错误信息:{0}. \n容器构建异常请参考：{1}"
const val BK_BUILD_MACHINE_CREATION_FAILED_REFERENCE = "bkBuildMachineCreationFailedReference"
// 构建机启动成功，等待Agent启动...
const val BK_BUILD_MACHINE_START_SUCCESS_WAIT_AGENT_START = "bkBuildMachineStartSuccessWaitAgentStart"
// 启动{0}构建容器失败，请联系蓝盾助手反馈处理.\n容器构建异常请参考：
const val BK_START_BUILD_CONTAINER_FAIL = "bkStartBuildContainerFail"
const val BK_CONTAINER_BUILD_ERROR = "bkContainerBuildError" // {0}构建异常，请联系蓝盾助手排查，异常信息 -
// 启动BCS构建容器失败，请联系BCS(蓝鲸容器助手)反馈处理.\n容器构建异常请参考：
const val BK_START_BCS_BUILD_CONTAINER_FAIL = "bkStartBcsBuildContainerFail"
// 第三方服务-BCS 异常，请联系BCS(蓝鲸容器助手)排查，异常信息 -
const val BK_THIRD_SERVICE_BCS_BUILD_ERROR = "bkThirdServiceBcsBuildError"
const val BK_TROUBLE_SHOOTING = "bkTroubleShooting" // 第三方服务-BCS 异常，请联系BCS(蓝鲸容器助手)排查，
const val BK_MACHINE_INTERFACE_TIMEOUT = "bkMachineInterfaceTimeout" // 操作构建机接口超时
// 获取kubernetes task({0})状态接口异常
const val BK_KUBERNETES_TASK_STATUS_API_EXCEPTION = "bkKubernetesTaskStatusApiException"
const val BK_KUBERNETES_TASK_STATUS_API_TIMEOUT = "bkKubernetesTaskStatusApiTimeout" // 获取kubernetes task状态接口超时
const val BK_KUBERNETES_TASK_EXECUTE_TIMEOUT = "bkKubernetesTaskExecuteTimeout" // 获取kubernetes任务执行超时
const val BK_CREATE_WORKSPACE_ERROR = "bkCreateWorkspaceError" // 创建工作空间异常
const val BK_CREATE_WORKSPACE_API_FAIL = "bkCreateWorkspaceApiFail" // 创建工作空间接口返回失败
const val BK_CREATE_BUILD_MACHINE_TIMEOUT = "bkCreateBuildMachineTimeout" // 创建构建机接口超时
const val BK_GET_WORKSPACE_URL_ERROR = "bkGetWorkspaceUrlError" // 获取工作空间url接口异常
const val BK_GET_WORKSPACE_LINK_TIMEOUT = "bkGetWorkspaceLinkTimeout" // 获取工作空间链接接口超时
const val BK_DEVCLOUD_TASK_TIMED_OUT = "bkDevcloudTaskTimedOut" // DevCloud任务超时（10min）
const val BK_NO_CONTAINER_IS_READY_DEBUG = "bkNoContainerIsReadyDebug" // pipeline({0})没有可用的容器进行登录调试
const val BK_CONTAINER_STATUS_EXCEPTION = "bkContainerStatusException" // pipeline({0})容器状态异常，请尝试重新构建流水线
const val BK_FAIL_TO_GET_JOB_STATUS = "bkFailToGetJobStatus" // 查询Job status接口异常
const val BK_WORKSPACE_STATE_NOT_RUNNING = "bkWorkspaceStateNotRunning" // 工作空间状态非RUNNING
const val BK_CREATE_ENV_TIMEOUT = "bkCreateEnvTimeout" // 创建环境超时（10min）

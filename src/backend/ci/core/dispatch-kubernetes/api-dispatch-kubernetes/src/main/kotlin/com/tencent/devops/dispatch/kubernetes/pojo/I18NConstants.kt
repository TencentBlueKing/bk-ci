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

const val BK_READY_CREATE_KUBERNETES_BUILD_MACHINE = "bkReadyCreateKubernetesBuildMachine"// 准备创建kubernetes构建机...
const val BK_READY_CREATE_BCS_BUILD_MACHINE = "bkReadyCreateBcsBuildMachine"// 准备创建BCS(蓝鲸容器平台)构建机...

const val BK_REQUEST_CREATE_BUILD_MACHINE_SUCCESSFUL = "bkRequestCreateBuildMachineSuccessful"// 下发创建构建机请求成功，builderName: {0} 等待机器创建...




const val BK_GET_LOGIN_DEBUG_LINK_TIMEOUT = "bkGetLoginDebugLinkTimeout"// 获取登录调试链接接口超时
const val BK_BUILD_AND_PUSH_INTERFACE_EXCEPTION = "bkBuildAndPushInterfaceException"// 构建并推送接口异常
const val BK_BUILD_AND_PUSH_INTERFACE_RETURN_FAIL = "bkBuildAndPushInterfaceReturnFail"// 构建并推送接口返回失败
const val BK_BUILD_AND_PUSH_INTERFACE_TIMEOUT = "bkBuildAndPushInterfaceTimeout"// 构建并推送接口超时
const val BK_GET_BCS_TASK_STATUS_TIMEOUT = "bkGetBcsTaskStatusTimeout"// 获取BCS TASK状态接口超时
const val BK_GET_BCS_TASK_EXECUTION_TIMEOUT = "bkGetBcsTaskExecutionTimeout"// 获取BCS任务执行超时
const val BK_GET_BCS_TASK_STATUS_ERROR = "bkGetBcsTaskStatusError"// 获取BCS TASK状态接口异常
const val BK_DISTRIBUTE_BUILD_MACHINE_REQUEST_SUCCESS = "bkDistributeBuildMachineRequestSuccess"// 下发创建构建机请求成功，等待机器创建
const val BK_MACHINE_BUILD_COMPLETED_WAITING_FOR_STARTUP = "bkMachineBuildCompletedWaitingForStartup" // 构建机创建成功，等待机器启动...
const val BK_BUILD_MACHINE_CREATION_FAILED = "bkBuildMachineCreationFailed"// 构建机创建失败
const val BK_CONTAINER_IS_NOT_IN_DEBUG_OR_IN_USE = "bkContainerIsNotInDebugOrInUse"// 容器没有处于debug或正在占用中\
const val BK_BUILD_MACHINE_STARTUP_FAILED = "bkBuildMachineStartupFailed"// 构建机启动失败，错误信息:{0}
const val BK_INTERFACE_REQUEST_TIMEOUT = "bkInterfaceRequestTimeout"// 接口请求超时
const val BK_BUILD_MACHINE_CREATION_FAILED_REFERENCE = "bkBuildMachineCreationFailedReference"//  "创建构建机失败，错误信息:{0}. \n容器构建异常请参考：{1}"
const val BK_BUILD_MACHINE_START_SUCCESS_WAIT_AGENT_START  = "bkBuildMachineStartSuccessWaitAgentStart"// 构建机启动成功，等待Agent启动...

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

const val BK_READY_CREATE_KUBERNETES_BUILD_MACHINE = "BkReadyCreateKubernetesBuildMachine"
const val BK_START_KUBERNETES_BUILD_CONTAINER_FAIL = "BkStartKubernetesBuildContainerFail"
const val BK_KUBERNETES_BUILD_ERROR = "BkKubernetesBuildError"
const val BK_READY_CREATE_BCS_BUILD_MACHINE = "BkReadyCreateBcsBuildMachine"
const val BK_START_BCS_BUILD_CONTAINER_FAIL = "BkStartBcsBuildContainerFail"
const val BK_BCS_BUILD_ERROR = "BkBcsBuildError"
const val BK_REQUEST_CREATE_BUILD_MACHINE_SUCCESSFUL = "BkRequestCreateBuildMachineSuccessful"
const val BK_GET_BUILD_MACHINE_DETAILS_TIMEOUT = "BkGetBuildMachineDetailsTimeout"
const val BK_MACHINE_INTERFACE_ERROR = "BkMachineInterfaceError" // 操作构建机接口异常
const val BK_MACHINE_INTERFACE_RETURN_FAIL = "BkMachineInterfaceReturnFail"// 操作构建机接口返回失败
const val BK_MACHINE_INTERFACE_TIMEOUT = "BkMachineInterfaceTimeout"// 操作构建机接口超时
const val BK_GET_LOGIN_DEBUG_LINK_TIMEOUT = "BkGetLoginDebugLinkTimeout"// 获取登录调试链接接口超时
const val BK_BUILD_AND_PUSH_INTERFACE_EXCEPTION = "BkBuildAndPushInterfaceException"// 构建并推送接口异常
const val BK_BUILD_AND_PUSH_INTERFACE_RETURN_FAIL = "BkBuildAndPushInterfaceReturnFail"// 构建并推送接口返回失败
const val BK_BUILD_AND_PUSH_INTERFACE_TIMEOUT = "BkBuildAndPushInterfaceTimeout"// 构建并推送接口超时
const val BK_GET_BCS_TASK_STATUS_TIMEOUT = "BkGetBcsTaskStatusTimeout"// 获取BCS TASK状态接口超时
const val BK_GET_BCS_TASK_EXECUTION_TIMEOUT = "BkGetBcsTaskExecutionTimeout"// 获取BCS任务执行超时
const val BK_GET_BCS_TASK_STATUS_ERROR = "BkGetBcsTaskStatusError"// 获取BCS TASK状态接口异常
const val BK_DISTRIBUTE_BUILD_MACHINE_REQUEST_SUCCESS = "BkDistributeBuildMachineRequestSuccess"// 下发创建构建机请求成功，等待机器创建
const val BK_MACHINE_BUILD_COMPLETED_WAITING_FOR_STARTUP = "BkMachineBuildCompletedWaitingForStartup"
const val BK_BUILD_MACHINE_CREATION_FAILED = "BkBuildMachineCreationFailed"
const val BK_CONTAINER_IS_NOT_IN_DEBUG_OR_IN_USE = "BkContainerIsNotInDebugOrInUse"


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

package com.tencent.devops.dispatch.devcloud.constant

object DispatchDevcloudMessageCode {
    const val BK_NO_CONTAINER_IS_READY_DEBUG = "2133030" // pipeline({0})没有可用的容器进行登录调试
    const val BK_CONTAINER_STATUS_EXCEPTION = "2133031" // pipeline({0})容器状态异常，请尝试重新构建流水线

    const val BK_FAILED_START_DEVCLOUD = "bkFailedStartDevcloud" // 启动DevCloud构建容器失败，请联系devopsHelper反馈处理.
    const val BK_CONTAINER_BUILD_EXCEPTIONS = "bkContainerBuildExceptions" // 容器构建异常请参考
    const val BK_DEVCLOUD_EXCEPTION = "bkDevcloudException" // 第三方服务-DEVCLOUD 异常，请联系O2000排查，异常信息 -
    const val BK_INTERFACE_REQUEST_TIMEOUT = "bkInterfaceRequestTimeout" // 接口请求超时
    const val BK_FAILED_CREATE_BUILD_MACHINE = "bkFailedCreateBuildMachine" // 创建构建机失败，错误信息
    const val BK_SEND_REQUEST_CREATE_BUILDER_SUCCESSFULLY = "bkSendRequestCreateBuilderSuccessfully" // 下发创建构建机请求成功
    const val BK_WAITING_MACHINE_START = "bkWaitingMachineStart" // 等待机器启动...
    const val BK_WAIT_AGENT_START = "bkWaitAgentStart" // 构建机启动成功，等待Agent启动...
    const val BK_SEND_REQUEST_START_BUILDER_SUCCESSFULLY = "bkSendRequestStartBuilderSuccessfully" // 下发启动构建机请求成功
    const val BK_BUILD_MACHINE_FAILS_START = "bkBuildMachineFailsStart" // 构建机启动失败，错误信息{0}
    // 准备创建腾讯自研云（云devnet资源)构建机...
    const val BK_PREPARE_CREATE_TENCENT_CLOUD_BUILD_MACHINE = "bkPrepareCreateTencentBuildMachine"
    const val BK_START_MIRROR = "bkStartMirror" // 启动镜像
    const val BK_GET_WEBSOCKET_URL_FAIL = "bkGetWebsocketUrlFail" // 获取websocket url失败，错误信息:{0}
}

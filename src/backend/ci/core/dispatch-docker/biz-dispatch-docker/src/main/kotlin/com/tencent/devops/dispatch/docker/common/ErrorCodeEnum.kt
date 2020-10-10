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

package com.tencent.devops.dispatch.docker.common

enum class ErrorCodeEnum(
    val errorCode: Int,
    val formatErrorMessage: String
) {
    SYSTEM_ERROR(2127001, "Dispatcher-docker系统错误"),
    NO_IDLE_VM_ERROR(2127002, "构建机启动失败，没有空闲的构建机"),
    POOL_VM_ERROR(2127003, "容器并发池分配异常"),
    NO_SPECIAL_VM_ERROR(2127004, "Start build Docker VM failed, no available Docker VM in specialIpList"),
    NO_AVAILABLE_VM_ERROR(2127005, "Start build Docker VM failed, no available Docker VM. Please wait a moment and try again."),
    DOCKER_IP_NOT_AVAILABLE(2127006, "Docker ip is not available."),
    END_VM_ERROR(2127007, "End build Docker VM failed"),
    START_VM_FAIL(2127008, "Start build Docker VM failed"),
    RETRY_START_VM_FAIL(2127009, "Start build Docker VM failed, retry times."),
    GET_VM_STATUS_FAIL(2127010, "Get container status failed"),
    GET_CREDENTIAL_FAIL(2127011, "Get credential failed")
}
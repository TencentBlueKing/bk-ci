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

package com.tencent.devops.dispatch.kubernetes.common

import com.tencent.devops.common.api.pojo.ErrorType

enum class ErrorCodeEnum(
    val errorType: ErrorType,
    val errorCode: Int,
    val formatErrorMessage: String
) {
    SYSTEM_ERROR(ErrorType.SYSTEM, 2123001, "Dispatcher-base系统错误"),
    NO_IDLE_VM_ERROR(ErrorType.SYSTEM, 2123002, "构建机启动失败，没有空闲的构建机"),
    CREATE_JOB_LIMIT_ERROR(ErrorType.USER, 2123050, "已超过dispatch base创建Job容器上限."),

    INTERFACE_TIMEOUT(ErrorType.THIRD_PARTY, 2123018, "第三方服务异常，异常信息 - 接口请求超时"),
    START_VM_ERROR(ErrorType.THIRD_PARTY, 2123004, "第三方服务异常，异常信息 - 构建机启动失败"),
    CREATE_VM_ERROR(ErrorType.THIRD_PARTY, 2123005, "第三方服务异常，异常信息 - 构建机创建失败"),
    STOP_VM_ERROR(ErrorType.THIRD_PARTY, 2123006, "第三方服务异常，异常信息 - 构建机休眠失败"),
    DELETE_VM_ERROR(ErrorType.THIRD_PARTY, 2123007, "第三方服务异常，异常信息 - 构建机销毁失败")
}

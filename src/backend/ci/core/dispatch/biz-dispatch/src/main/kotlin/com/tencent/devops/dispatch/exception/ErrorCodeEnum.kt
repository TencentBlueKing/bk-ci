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

package com.tencent.devops.dispatch.exception

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.I18nTranslateTypeEnum
import com.tencent.devops.common.api.pojo.ErrorType

@Suppress("ALL")
enum class ErrorCodeEnum(
    @BkFieldI18n
    val errorType: ErrorType,
    val errorCode: Int,
    @BkFieldI18n(translateType = I18nTranslateTypeEnum.VALUE, reusePrefixFlag = false)
    val formatErrorMessage: String
) {
    SYSTEM_ERROR(ErrorType.SYSTEM, 2103001, "2103001"),// Dispatcher系统错误
    START_VM_FAIL(ErrorType.SYSTEM, 2103002, "2103002"),// Fail to start up after 3 retries
    VM_STATUS_ERROR(ErrorType.USER, 2103003, "2103003"),// 第三方构建机状态异常/Bad build agent status
    GET_BUILD_AGENT_ERROR(ErrorType.SYSTEM, 2103004, "2103004"),// 获取第三方构建机失败/Fail to get build agent
    FOUND_AGENT_ERROR(ErrorType.SYSTEM, 2103005, "2103005"),// 获取第三方构建机失败/Can not found agent by type
    LOAD_BUILD_AGENT_FAIL(ErrorType.USER, 2103006, "2103006"),// 获取第三方构建机失败/Load build agent fail
    VM_NODE_NULL(ErrorType.USER, 2103007, "2103007"),// 第三方构建机环境的节点为空
    GET_VM_ENV_ERROR(ErrorType.USER, 2103008, "2103008"),// 获取第三方构建机环境失败
    GET_VM_ERROR(ErrorType.USER, 2103009, "2103009"),// 获取第三方构建机失败
    JOB_QUOTA_EXCESS(ErrorType.USER, 2103010, "2103010")// JOB配额超限
}

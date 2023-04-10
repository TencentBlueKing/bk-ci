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

package com.tencent.devops.dispatch.bcs.common

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.I18nTranslateTypeEnum
import com.tencent.devops.common.api.pojo.ErrorType

enum class ErrorCodeEnum(
    @BkFieldI18n
    val errorType: ErrorType,
    val errorCode: Int,
    @BkFieldI18n(translateType = I18nTranslateTypeEnum.VALUE, reusePrefixFlag = false)
    val formatErrorMessage: String
) {
    SYSTEM_ERROR(ErrorType.SYSTEM, 2126101, "2126101"),// Dispatcher-bcs系统错误
    CREATE_VM_ERROR(ErrorType.THIRD_PARTY, 2126103, "2126103"),// 第三方服务-BCS 异常，异常信息 - 构建机创建失败
    CREATE_VM_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2126105, "2126105"),// 第三方服务-BCS 异常，异常信息 - 创建构建机接口异常
    CREATE_VM_INTERFACE_FAIL(ErrorType.THIRD_PARTY, 2126106, "2126106"),// 第三方服务-BCS 异常，异常信息 - 创建构建机接口返回失败
    OPERATE_VM_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2126107, "2126107"),// 第三方服务-BCS 异常，异常信息 - 操作构建机接口异常
    OPERATE_VM_INTERFACE_FAIL(ErrorType.THIRD_PARTY, 2126108, "2126108"),// 第三方服务-BCS 异常，异常信息 - 操作构建机接口返回失败
    VM_STATUS_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2126109, "2126109"),// 第三方服务-BCS 异常，异常信息 - 获取构建机详情接口异常
    CREATE_IMAGE_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2126110, "2126110"),// 第三方服务-BCS 异常，异常信息 - 创建镜像接口异常
    TASK_STATUS_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2126114, "2126114"),// 第三方服务-BCS 异常，异常信息 - 获取TASK状态接口异常
    WEBSOCKET_URL_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2126115, "2126115");//第三方服务-BCS 异常，异常信息 - 获取websocket接口异常
}

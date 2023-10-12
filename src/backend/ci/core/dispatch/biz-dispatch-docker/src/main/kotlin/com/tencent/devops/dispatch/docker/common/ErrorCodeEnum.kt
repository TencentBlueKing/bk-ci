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

package com.tencent.devops.dispatch.docker.common

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.web.utils.I18nUtil

@Suppress("ALL")
enum class ErrorCodeEnum(
    @BkFieldI18n
    val errorType: ErrorType,
    val errorCode: Int,
    val formatErrorMessage: String
) {
    SYSTEM_ERROR(ErrorType.SYSTEM, 2131001, "Dispatcher-docker系统错误"),
    NO_IDLE_VM_ERROR(ErrorType.SYSTEM, 2131002, "构建机启动失败，没有空闲的构建机"),
    POOL_VM_ERROR(ErrorType.SYSTEM, 2131003, "容器并发池分配异常"),
    NO_SPECIAL_VM_ERROR(
        ErrorType.SYSTEM,
        2131004,
        "Start build Docker VM failed, no available Docker VM in specialIpList"
    ),
    NO_AVAILABLE_VM_ERROR(
        ErrorType.SYSTEM,
        2131005,
        "Start build Docker VM failed, no available Docker VM. Please wait a moment and try again."
    ),
    DOCKER_IP_NOT_AVAILABLE(ErrorType.SYSTEM, 2131006, "Docker ip is not available."),
    END_VM_ERROR(ErrorType.SYSTEM, 2131007, "End build Docker VM failed"),
    START_VM_FAIL(ErrorType.SYSTEM, 2131008, "Start build Docker VM failed"),
    RETRY_START_VM_FAIL(ErrorType.USER, 2131009, "Start build Docker VM failed, retry times."),
    GET_VM_STATUS_FAIL(ErrorType.SYSTEM, 2131010, " Get container status failed"),
    GET_CREDENTIAL_FAIL(ErrorType.USER, 2131011, "Get credential failed"),
    IMAGE_ILLEGAL_EXCEPTION(
        ErrorType.USER, 2131012, "User Image illegal, not found or credential error"
    ),
    IMAGE_CHECK_LEGITIMATE_OR_RETRY(
        ErrorType.USER, 2131013, "登录调试失败,请检查镜像是否合法或重试。"
    ),
    DEBUG_CONTAINER_SHUTS_DOWN_ABNORMALLY(
        ErrorType.SYSTEM,
        2131014,
        "登录调试失败，调试容器异常关闭，请重试。"
    ),
    NO_CONTAINER_IS_READY_DEBUG(
        ErrorType.USER,
        2131015,
        "pipeline({0})没有可用的容器进行登录调试"
    ),
    LOAD_TOO_HIGH(ErrorType.SYSTEM, 2131016, "pipeline({0})当前调试容器负载过高，请稍等并重试。"),
    USER_IMAGE_NOT_INSTALLED(
        ErrorType.USER,
        2131016,
        "项目{0}未安装镜像{1}，无法使用"
    );

    fun getErrorMessage(params: Array<String>? = null): String {
        return I18nUtil.getCodeLanMessage(
            messageCode = "${this.errorCode}",
            params = params
        )
    }
}

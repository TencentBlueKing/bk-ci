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

package com.tencent.devops.dockerhost.common

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.web.utils.I18nUtil

enum class ErrorCodeEnum(
    @BkFieldI18n
    val errorType: ErrorType,
    val errorCode: Int,
    val formatErrorMessage: String
) {
    SYSTEM_ERROR(ErrorType.SYSTEM, 2104001, "dockerhost服务系统错误"),
    CREATE_CONTAINER_ERROR(ErrorType.SYSTEM, 2104002, "创建构建容器失败"),
    NO_AVAILABLE_PORT_ERROR(ErrorType.SYSTEM, 2104003, "dockerRun无可用端口"),
    NO_AUTH_PULL_IMAGE_ERROR(ErrorType.USER, 2104004, "无权限拉取镜像"),
    IMAGE_NOT_EXIST_ERROR(ErrorType.USER, 2104005, "镜像不存在"),
    DOCKER_INIT_DOWNLOAD_ERROR(ErrorType.SYSTEM, 2104006, "docker_init下载失败"),
    DOCKER_JAR_DOWNLOAD_ERROR(ErrorType.SYSTEM, 2104007, "docker.jar下载失败"),
    DOCKER_INIT_CURL_ERROR(ErrorType.SYSTEM, 2104008, "镜像curl命令异常，导致下载初始脚本出错"),
    DOCKER_INIT_JDK_ERROR(ErrorType.SYSTEM, 2104009, "镜像内JDK安装路径不存在"),
    DOCKER_INIT_ERROR(ErrorType.SYSTEM, 2104010, "初始化脚本执行失败");

    fun getErrorMessage(): String {
        return I18nUtil.getCodeLanMessage("${this.errorCode}")
    }
}

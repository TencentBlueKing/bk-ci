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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.I18nTranslateTypeEnum

/**
 * Kubernetes构建机状态信息
 * @param status 构建机状态
 * @param message 状态信息
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class KubernetesBuilderStatus(
    val status: String,
    val message: String
)

enum class KubernetesBuilderStatusEnum(
    val realName: String,
    @BkFieldI18n(
        translateType = I18nTranslateTypeEnum.VALUE,
        keyPrefixName = "kubernetesBuilderStatus",
        reusePrefixFlag = false
    )
    val message: String
) {
    READY_TO_RUN("readyToRun", "readyToRun"), // 构建机初始化状态，等待创建
    NOT_EXIST("notExist", "notExist"), // 构建机不存在，可能已被删除或者未创建
    PENDING("pending", "pending"), // 等待运行中
    RUNNING("running", "running"), // 运行中
    SUCCEEDED("succeeded", "succeeded"), // 容器运行成功退出
    FAILED("failed", "failed"), // 运行失败，容器以非0退出
    UNKNOWN("unknown", "unknown"), // 构建机状态未知
}

fun KubernetesBuilderStatus.readyToStart(): Boolean {
    return status == KubernetesBuilderStatusEnum.READY_TO_RUN.realName
}

fun KubernetesBuilderStatus.hasException(): Boolean {
    return when (status) {
        KubernetesBuilderStatusEnum.FAILED.realName,
        KubernetesBuilderStatusEnum.UNKNOWN.realName -> true

        else -> false
    }
}

fun KubernetesBuilderStatus.canReStart(): Boolean {
    return when (status) {
        KubernetesBuilderStatusEnum.READY_TO_RUN.realName, KubernetesBuilderStatusEnum.SUCCEEDED.realName -> true
        else -> false
    }
}

fun KubernetesBuilderStatus.isRunning(): Boolean {
    return when (status) {
        KubernetesBuilderStatusEnum.RUNNING.realName -> true
        else -> false
    }
}

fun KubernetesBuilderStatus.isStarting(): Boolean {
    return when (status) {
        KubernetesBuilderStatusEnum.PENDING.realName -> true
        else -> false
    }
}

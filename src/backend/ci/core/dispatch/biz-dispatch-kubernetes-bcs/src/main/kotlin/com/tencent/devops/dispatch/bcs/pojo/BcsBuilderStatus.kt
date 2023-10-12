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
 * documentation files (the "Software"),  to deal in the Software without restriction, including without limitation the
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

package com.tencent.devops.dispatch.bcs.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.web.utils.I18nUtil

/**
 * Bcs构建机状态信息
 * @param status 构建机状态
 * @param containerStatus 构建机内容器状态
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class BcsBuilderStatus(
    val status: String,
    @JsonProperty("container_status")
    val containerStatus: String
)

enum class BcsBuilderStatusEnum(
    val realName: String,
    val message: String
) {
    INIT("init", "构建机初始化状态"), // 构建机初始化状态，等待创建
    CREATING("creating", "构建机创建中"), // 构建机创建中
    ERROR("error", "构建机创建失败"), // 构建机创建失败，一般意味着输入的描述文件有错误
    CREATE_FAILED("createFailed", "构建机创建失败"), // 构建机创建失败，比如资源不够等
    START_FAILED("startFailed", "构建机启动失败"), // 构建机启动失败
    READY_TO_RUN("readyToRun", "具备了启动条件"), // 具备了启动条件
    STARTING("starting", "构建机启动中"), // 构建机启动中
    RUNNING("running", "构建机运行中"), // 构建机运行中
    STOPPING("stopping", "构建机停止中"), // 构建机停止中
    STOP_FAILED("stopFailed", "构建机停止失败，比如资源释放失败"), // 构建机停止失败，比如资源释放失败
    DELETING("deleting", "构建机删除中"), // 构建机删除中
    DELETED("deleted", "构建机删除成功"), // 构建机删除成功
    DELETE_FAILED("deleteFailed", "构建机删除失败，比如资源释放失败"), // 构建机删除失败，比如资源释放失败
    ABNORMAL_AFTER_READY("abnormalAfterReady", "构建机创建成功后，进入异常状态，一般意味着相关资源状态异常"),
    ABNORMAL_AFTER_RUNNING("abnormalAfterRunning", "构建机运行时，进入异常状态，一般意味着相关资源状态异常"),
    UNKNOWN("unknown", "构建机状态未知"), // 构建机状态未知
}

fun BcsBuilderStatus.readyToStart(): Boolean {
    return status == BcsBuilderStatusEnum.READY_TO_RUN.realName
}

fun BcsBuilderStatus.hasException(): Boolean {
    return when (status) {
        BcsBuilderStatusEnum.ERROR.realName,
        BcsBuilderStatusEnum.CREATE_FAILED.realName,
        BcsBuilderStatusEnum.START_FAILED.realName,
        BcsBuilderStatusEnum.STOP_FAILED.realName,
        BcsBuilderStatusEnum.DELETE_FAILED.realName,
        BcsBuilderStatusEnum.ABNORMAL_AFTER_READY.realName,
        BcsBuilderStatusEnum.ABNORMAL_AFTER_RUNNING.realName,
        BcsBuilderStatusEnum.UNKNOWN.realName -> true
        else -> false
    }
}

fun BcsBuilderStatus.canReStart(): Boolean {
    return when (status) {
        BcsBuilderStatusEnum.READY_TO_RUN.realName,
        BcsBuilderStatusEnum.STOP_FAILED.realName
        -> true
        else -> false
    }
}

fun BcsBuilderStatus.isRunning(): Boolean {
    return when (status) {
        BcsBuilderStatusEnum.RUNNING.realName -> true
        else -> false
    }
}

fun BcsBuilderStatus.isStarting(): Boolean {
    return when (status) {
        BcsBuilderStatusEnum.STARTING.realName -> true
        else -> false
    }
}

fun BcsBuilderStatusEnum.getMessage(): String {
    return I18nUtil.getCodeLanMessage("bcsBuilderStatus." + this.realName)
}

enum class BcsBuilderContainerStatusEnum(
    val realName: String,
    val message: String
) {
    WAITING("Waiting", "容器拉起中"), // 容器拉起中
    RUNNING("Running", "容器运行中"), // 容器运行中
    TERMINATED("Terminated", "容器已经执行完"), // 容器已经执行完
}

fun BcsBuilderContainerStatusEnum.getMessage(): String {
    return I18nUtil.getCodeLanMessage("bcsBuilderContainerStatus." + this.realName)
}

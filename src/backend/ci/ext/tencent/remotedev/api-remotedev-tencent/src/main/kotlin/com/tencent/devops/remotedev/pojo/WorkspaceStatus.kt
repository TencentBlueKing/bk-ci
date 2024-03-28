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

package com.tencent.devops.remotedev.pojo

/**
 * index 顺序不能改动，如要添加新状态，请在末尾添加。禁止直接删除某一状态字段。
 */
@Suppress("ALL")
enum class WorkspaceStatus {
    PREPARING, // 0
    RUNNING, // 1
    STOPPED, // 2
    SLEEP, // 3
    DELETED, // 4
    EXCEPTION, // 5
    STARTING, // 6
    SLEEPING, // 7
    DELETING, // 8
    DELIVERING, // 9 交付中
    DISTRIBUTING, // 10 待分配
    DELIVERING_FAILED, // 11 交付失败
    STOPPING, // 12 关机中
    RESTARTING, // 13 重启中
    MAKING_IMAGE, // 14 制作镜像中
    REBUILDING; // 14 重装系统中

    fun checkRunning() = this == RUNNING

    fun checkDeleted() = this == DELETED

    fun checkSleeping() = this == SLEEP || this == STOPPED

    fun checkException() = this == EXCEPTION

    fun checkDelivering() = this == DELIVERING || checkDeliveringFailed()

    fun checkDeliveringFailed() = this == DELIVERING_FAILED

    fun checkDistributing() = this == DISTRIBUTING

    fun workspaceInitializing() = checkDelivering()

    fun checkInUse() = !checkDeleted() && !checkException()
    fun checkInProcess() = this == RESTARTING || this == MAKING_IMAGE || this == REBUILDING
    /**
     * 当正在做某事时，不能新建任务去执行
     */
    fun notOk2doNextAction(workspaceSystemType: WorkspaceSystemType) =
        (this == PREPARING && workspaceSystemType != WorkspaceSystemType.WINDOWS_GPU) ||
            this == STARTING || this == SLEEPING || this == DELETING || this == STOPPING ||
            this == RESTARTING || this == MAKING_IMAGE || this == REBUILDING
}

@Suppress("ALL")
fun WorkspaceStatus.display(): String {
    return when (this) {
        WorkspaceStatus.PREPARING -> "准备中"
        WorkspaceStatus.RUNNING -> "运行中"
        WorkspaceStatus.STOPPED -> "已关机"
        WorkspaceStatus.SLEEP -> "已休眠"
        WorkspaceStatus.DELETED -> "已删除"
        WorkspaceStatus.EXCEPTION -> "异常"
        WorkspaceStatus.STARTING -> "启动中"
        WorkspaceStatus.SLEEPING -> "休眠中"
        WorkspaceStatus.DELETING -> "删除中"
        WorkspaceStatus.DELIVERING -> "交付中"
        WorkspaceStatus.DISTRIBUTING -> "待分配"
        WorkspaceStatus.DELIVERING_FAILED -> "交付失败"
        WorkspaceStatus.STOPPING -> "关机中"
        WorkspaceStatus.RESTARTING -> "重启中"
        WorkspaceStatus.MAKING_IMAGE -> "制作镜像中"
        WorkspaceStatus.REBUILDING -> "重装系统中"
    }
}

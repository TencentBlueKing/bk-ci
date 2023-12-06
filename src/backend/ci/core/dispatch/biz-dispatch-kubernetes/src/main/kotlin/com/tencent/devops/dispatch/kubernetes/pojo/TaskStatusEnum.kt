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

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.I18nTranslateTypeEnum

enum class TaskStatusEnum(
    val realName: String,
    @BkFieldI18n(translateType = I18nTranslateTypeEnum.VALUE, keyPrefixName = "taskStatus", reusePrefixFlag = false)
    val message: String
) {
    WAITING("waiting", "waiting"), // 任务初始化
    RUNNING("running", "running"), // 任务正在执行
    FAILED("failed", "failed"), // 任务执行失败
    SUCCEEDED("succeeded", "succeeded"), // 任务执行成功
    UNKNOWN("unknown", "unknown"), // 未知状态

    // 下面的为自定义状态，非返回
    TIME_OUT("time_out", "timeOut"); // 超时

    companion object {
        fun realNameOf(realName: String?): TaskStatusEnum? {
            if (realName.isNullOrBlank()) {
                return null
            }
            return values().firstOrNull { it.realName == realName }
        }
    }
}

fun TaskStatusEnum.isRunning() =
    this == TaskStatusEnum.RUNNING || this == TaskStatusEnum.WAITING

fun TaskStatusEnum.isSuccess() = this == TaskStatusEnum.SUCCEEDED

fun TaskStatusEnum.isFailed() = this == TaskStatusEnum.FAILED || this == TaskStatusEnum.UNKNOWN

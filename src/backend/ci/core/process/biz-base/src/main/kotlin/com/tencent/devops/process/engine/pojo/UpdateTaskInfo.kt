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

package com.tencent.devops.process.engine.pojo

import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import java.time.LocalDateTime

data class UpdateTaskInfo(
    val projectId: String, // 项目ID
    val buildId: String, // 构建ID
    val taskId: String, // 任务ID
    val executeCount: Int,
    val taskStatus: BuildStatus, // 构建状态
    var starter: String? = null, // 启动人
    var approver: String? = null, // 审批人
    var startTime: LocalDateTime? = null, // 开始时间
    var endTime: LocalDateTime? = null, // 结束时间
    var totalTime: Long? = null, // 耗费时间
    val additionalOptions: ElementAdditionalOptions? = null,
    var taskParams: Map<String, Any>? = null,
    val errorType: ErrorType? = null,
    val errorCode: Int? = null,
    val errorMsg: String? = null,
    val platformCode: String? = null, // 对接平台代码
    val platformErrorCode: Int? = null // 对接平台错误码
)

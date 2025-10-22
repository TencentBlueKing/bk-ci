/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.dispatch.pojo

import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "项目的JOB配额")
data class RunningJobVO(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "构建机类型", required = true)
    val vmType: JobQuotaVmType,
    @get:Schema(title = "项目最大并发JOB数， 默认50", required = false)
    val runningJobMax: Int,
    @get:Schema(title = "项目单JOB最大执行时间，默认8小时", required = false)
    val runningTimeJobMax: Int,
    @get:Schema(title = "项目所有JOB最大执行时间，默认40小时/月", required = false)
    val runningTimeProjectMax: Int,
//    @get:Schema(title = "工蜂CI最大并发JOB数量，默认10个", required = false)
//    val runningJobMaxGitCi: Int,
//    @get:Schema(title = "工蜂CI单JOB最大执行时间，默认8小时", required = false)
//    val runningTimeJobMaxGitCi: Int,
//    @get:Schema(title = "工蜂CI所有JOB单项目最大执行时间，默认40小时/月", required = false)
//    val runningTimeJobMaxProjectGitCi: Int,
    @get:Schema(title = "创建时间", required = false)
    val createdTime: Long?,
    @get:Schema(title = "修改时间", required = false)
    val updatedTime: Long?,
    @get:Schema(title = "操作人", required = false)
    val operator: String?
)

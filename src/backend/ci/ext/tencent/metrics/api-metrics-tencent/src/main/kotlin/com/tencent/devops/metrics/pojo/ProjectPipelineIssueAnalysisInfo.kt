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

package com.tencent.devops.metrics.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "项目流水线问题分析统计信息")
data class ProjectPipelineIssueAnalysisInfo(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "近 30 天内失败率高于 90% 的流水线数量", required = false)
    val failureRateCount: Int? = null,
    @get:Schema(title = "近 90 天内持续失败的流水线数量", required = false)
    val consecutiveFailuresCount: Int? = null,
    @get:Schema(title = "定时触发但代码无变更的流水线数量", required = false)
    val scheduledTriggerNoCodeChangeCount: Int? = null,
    @get:Schema(title = "流水线问题关联图卡ID", required = false)
    val cardId: Int? = null
)

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

package com.tencent.devops.quality.api.v2.pojo.response

import com.tencent.devops.quality.api.v2.pojo.ControlPointPosition
import com.tencent.devops.common.quality.pojo.enums.QualityOperation
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "质量红线-匹配拦截规则原子v2")
data class QualityRuleMatchTask(
    @get:Schema(title = "原子ID", required = true)
    val taskId: String,
    @get:Schema(title = "原子名称", required = true)
    val taskName: String,
    @get:Schema(title = "原子控制阶段", required = true)
    val controlStage: ControlPointPosition,
    @get:Schema(title = "规则列表", required = true)
    val ruleList: List<RuleMatchRule>,
    @get:Schema(title = "阈值列表", required = false)
    val thresholdList: List<RuleThreshold>?,
    @get:Schema(title = "审核用户列表", required = false)
    val auditUserList: Set<String>?
) {
    @Schema(title = "质量红线-拦截规则v2")
    data class RuleMatchRule(
        @get:Schema(title = "规则ID", required = true)
        val ruleHashId: String,
        @get:Schema(title = "规则名称", required = true)
        val ruleName: String,
        @get:Schema(title = "红线匹配的id", required = false)
        val gatewayId: String?
    )

    @Schema(title = "质量红线-拦截规则阈值v2")
    data class RuleThreshold(
        @get:Schema(title = "指标ID", required = true)
        val indicatorId: String,
        @get:Schema(title = "指标名称", required = true)
        val indicatorName: String,
        @get:Schema(title = "元数据DATA_ID", required = true)
        val metadataIds: List<String>,
        @get:Schema(title = "关系", required = true)
        val operation: QualityOperation,
        @get:Schema(title = "阈值值大小", required = true)
        val value: String
    )
}

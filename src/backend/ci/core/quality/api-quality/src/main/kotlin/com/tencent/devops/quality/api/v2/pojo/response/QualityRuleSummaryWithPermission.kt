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

import com.tencent.devops.quality.pojo.RulePermission
import com.tencent.devops.quality.pojo.enum.RuleRange
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "质量红线-规则简要信息v2")
data class QualityRuleSummaryWithPermission(
    @get:Schema(title = "规则HashId", required = true)
    val ruleHashId: String,
    @get:Schema(title = "规则名称", required = true)
    val name: String,
    @get:Schema(title = "控制点", required = true)
    val controlPoint: RuleSummaryControlPoint,
    @get:Schema(title = "指标列表", required = true)
    val indicatorList: List<RuleSummaryIndicator>,
    @get:Schema(title = "生效范围", required = true)
    val range: RuleRange,
    @get:Schema(title = "包含模板和流水线的生效范围（新）", required = true)
    val rangeSummary: List<RuleRangeSummary>,
    @get:Schema(title = "流水线个数", required = true)
    val pipelineCount: Int,
    @get:Schema(title = "生效流水线执次数", required = true)
    val pipelineExecuteCount: Int,
    @get:Schema(title = "拦截次数", required = true)
    val interceptTimes: Int,
    @get:Schema(title = "是否启用", required = true)
    val enable: Boolean,
    @get:Schema(title = "规则权限", required = true)
    val permissions: RulePermission,
    @get:Schema(title = "红线ID", required = true)
    val gatewayId: String?
) {
        data class RuleSummaryControlPoint(
            val hashId: String,
            val name: String,
            val cnName: String
        )
        data class RuleSummaryIndicator(
            val hashId: String,
            val name: String,
            val cnName: String,
            val operation: String,
            val threshold: String
        )
        data class RuleRangeSummary(
            val id: String,
            val name: String, // 流水线或者模板名称
            val type: String, // 类型，PIPELINE，TEMPLATE
            val lackElements: Collection<String> // 缺少的控制点
        )
}

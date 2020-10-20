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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.quality.api.v2.pojo.response

import com.tencent.devops.quality.api.v2.pojo.ControlPointPosition
import com.tencent.devops.quality.api.v2.pojo.enums.QualityOperation
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("质量红线-匹配拦截规则原子v2")
data class QualityRuleMatchTask(
    @ApiModelProperty("原子ID", required = true)
    val taskId: String,
    @ApiModelProperty("原子名称", required = true)
    val taskName: String,
    @ApiModelProperty("原子控制阶段", required = true)
    val controlStage: ControlPointPosition,
    @ApiModelProperty("规则列表", required = true)
    val ruleList: List<RuleMatchRule>,
    @ApiModelProperty("阈值列表", required = false)
    val thresholdList: List<RuleThreshold>?,
    @ApiModelProperty("审核用户列表", required = false)
    val auditUserList: Set<String>?
) {
    @ApiModel("质量红线-拦截规则v2")
    data class RuleMatchRule(
        @ApiModelProperty("规则ID", required = true)
        val ruleHashId: String,
        @ApiModelProperty("规则名称", required = true)
        val ruleName: String,
        @ApiModelProperty("红线匹配的id", required = false)
        val gatewayId: String?
    )

    @ApiModel("质量红线-拦截规则阈值v2")
    data class RuleThreshold(
        @ApiModelProperty("指标ID", required = true)
        val indicatorId: String,
        @ApiModelProperty("指标名称", required = true)
        val indicatorName: String,
        @ApiModelProperty("元数据DATA_ID", required = true)
        val metadataIds: List<String>,
        @ApiModelProperty("关系", required = true)
        val operation: QualityOperation,
        @ApiModelProperty("阈值值大小", required = true)
        val value: String
    )
}
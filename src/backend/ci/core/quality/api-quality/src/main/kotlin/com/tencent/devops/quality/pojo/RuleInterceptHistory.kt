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

package com.tencent.devops.quality.pojo

import com.tencent.devops.common.quality.pojo.QualityRuleInterceptRecord
import com.tencent.devops.common.quality.pojo.enums.RuleInterceptResult
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "质量红线-拦截记录")
data class RuleInterceptHistory(
    @get:Schema(title = "hashId 红线拦截记录在表中主键Id的哈希值，是唯一的", required = true)
    val hashId: String,
    @get:Schema(title = "项目里的序号", required = true)
    val num: Long,
    @get:Schema(title = "时间戳(秒)", required = true)
    val timestamp: Long,
    @get:Schema(title = "拦截结果", required = true)
    val interceptResult: RuleInterceptResult,
    @get:Schema(title = "规则HashId", required = true)
    val ruleHashId: String,
    @get:Schema(title = "规则名称", required = true)
    val ruleName: String,
    @get:Schema(title = "流水线ID", required = true)
    val pipelineId: String,
    @get:Schema(title = "流水线名称", required = true)
    val pipelineName: String,
    @get:Schema(title = "构建ID", required = true)
    val buildId: String,
    @get:Schema(title = "构建号", required = true)
    val buildNo: String,
    @get:Schema(title = "检查次数", required = true)
    val checkTimes: Int,
    @get:Schema(title = "描述", required = true)
    val remark: String,
    @get:Schema(title = "描述列表", required = true)
    val interceptList: List<QualityRuleInterceptRecord>? = null,
    @get:Schema(title = "流水线是否已删除", required = true)
    val pipelineIsDelete: Boolean = false,
    @get:Schema(title = "红线把关记录", required = false)
    val qualityRuleBuildHisOpt: QualityRuleBuildHisOpt? = null
)

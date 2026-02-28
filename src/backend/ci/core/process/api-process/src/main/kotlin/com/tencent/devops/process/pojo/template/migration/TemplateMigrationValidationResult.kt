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

package com.tencent.devops.process.pojo.template.migration

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 模板迁移验证结果
 *
 * 设计说明：
 * - 验证集成到迁移流程，验证失败 = 迁移失败
 * - 统计信息包含在 summary 中
 * - 差异详情存储在 discrepancies 中
 */
@Schema(title = "模板迁移验证结果")
data class TemplateMigrationValidationResult(
    @get:Schema(title = "项目ID")
    val projectId: String,
    @get:Schema(title = "是否验证成功")
    val success: Boolean,
    @get:Schema(title = "统计摘要")
    val summary: ValidationSummary,
    @get:Schema(title = "差异详情列表")
    val discrepancies: List<TemplateMigrationDiscrepancy>
) {
    /**
     * 验证统计摘要（内部类，避免与外部 ValidationSummary 冲突）
     */
    @Schema(title = "验证统计摘要")
    data class ValidationSummary(
        @get:Schema(title = "验证检查项总数")
        val total: Int,
        @get:Schema(title = "验证成功数")
        val success: Int,
        @get:Schema(title = "验证失败数")
        val failed: Int
    )
}

/**
 * 验证差异详情
 */
@Schema(title = "验证差异详情")
data class TemplateMigrationDiscrepancy(
    @get:Schema(title = "规则ID")
    val ruleId: String,
    @get:Schema(title = "规则名称")
    val ruleName: String,
    @get:Schema(title = "严重级别")
    val severity: ValidationSeverity,
    @get:Schema(title = "V1值")
    val v1Value: String?,
    @get:Schema(title = "V2值")
    val v2Value: String?,
    @get:Schema(title = "模板ID")
    val templateId: String?,
    @get:Schema(title = "版本号")
    val version: Long?,
    @get:Schema(title = "修复建议")
    val suggestion: String?
)

/**
 * 验证严重级别
 */
enum class ValidationSeverity(val value: String) {
    CRITICAL("严重"),
    HIGH("高"),
    MEDIUM("中"),
    LOW("低")
}

/**
 * 验证规则类型
 */
enum class ValidationRuleType(val value: String) {
    INTEGRITY("数据完整性"),
    MODEL("JSON模型"),
    STORE_STATUS("研发商店状态")
}

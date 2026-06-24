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

package com.tencent.devops.process.service.template.validation

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.template.MigrationStatus
import com.tencent.devops.process.dao.template.PipelineTemplateMigrationDao
import com.tencent.devops.process.pojo.template.migration.TemplateMigrationDiscrepancy
import com.tencent.devops.process.pojo.template.migration.TemplateMigrationValidationResult
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 模板迁移验证服务
 *
 * 设计原则：
 * - 验证集成到迁移流程，验证失败 = 迁移失败
 * - 只新增 1 个字段 VALIDATION_DISCREPANCIES 存储验证差异详情（JSON格式）
 * - 统计信息包含在 JSON 的 summary 中
 * - 本服务只负责执行验证并返回结果，状态更新由调用方统一处理
 */
@Service
class PipelineTemplateMigrationValidationService(
    private val dslContext: DSLContext,
    private val pipelineTemplateMigrationDao: PipelineTemplateMigrationDao,
    private val validators: List<TemplateMigrationValidator>
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTemplateMigrationValidationService::class.java)
    }

    /**
     * 执行项目级验证
     *
     * 只执行验证并返回结果，不更新数据库状态（状态更新由调用方统一处理）
     *
     * @param projectId 项目ID
     * @param autoTriggered 是否自动触发（迁移后自动调用）
     * @param validator 验证执行人
     * @return 验证结果
     */
    fun validateProject(
        projectId: String,
        autoTriggered: Boolean = false,
        validator: String? = null
    ): TemplateMigrationValidationResult {
        logger.info("Start validation for projectId=$projectId, autoTriggered=$autoTriggered, validator=$validator")

        val allDiscrepancies = mutableListOf<TemplateMigrationDiscrepancy>()
        var totalChecks = 0
        var successChecks = 0

        // 执行所有验证器
        for (validatorInstance in validators) {
            logger.info("Running validator: ${validatorInstance.getType()} for projectId=$projectId")
            val discrepancies = validatorInstance.validate(projectId)
            allDiscrepancies.addAll(discrepancies)
            totalChecks++
            if (discrepancies.isEmpty()) {
                successChecks++
            }
        }

        val hasFailures = allDiscrepancies.isNotEmpty()

        logger.info(
            "Validation completed for projectId=$projectId, hasFailures=$hasFailures, " +
                "discrepancies=${allDiscrepancies.size}"
        )

        return TemplateMigrationValidationResult(
            projectId = projectId,
            success = !hasFailures,
            summary = TemplateMigrationValidationResult.ValidationSummary(
                total = totalChecks,
                success = successChecks,
                failed = totalChecks - successChecks
            ),
            discrepancies = allDiscrepancies
        )
    }

    /**
     * 获取验证结果
     */
    fun getValidationResult(projectId: String): TemplateMigrationValidationResult? {
        val record = pipelineTemplateMigrationDao.get(dslContext, projectId)
            ?: return null

        val discrepanciesJson = pipelineTemplateMigrationDao.getValidationDiscrepancies(dslContext, projectId)
        if (discrepanciesJson.isNullOrBlank()) {
            return TemplateMigrationValidationResult(
                projectId = projectId,
                success = record.status == MigrationStatus.SUCCESS.name,
                summary = TemplateMigrationValidationResult.ValidationSummary(
                    total = 0,
                    success = 0,
                    failed = 0
                ),
                discrepancies = emptyList()
            )
        }

        return parseValidationResultJson(projectId, discrepanciesJson, record.status)
    }

    /**
     * 解析验证结果 JSON
     */
    private fun parseValidationResultJson(
        projectId: String,
        json: String,
        status: String?
    ): TemplateMigrationValidationResult {
        return try {
            val resultMap = JsonUtil.to(
                json = json,
                typeReference = object : TypeReference<Map<String, Any>>() {}
            )

            @Suppress("UNCHECKED_CAST")
            val summaryMap = resultMap["summary"] as? Map<String, Int> ?: emptyMap()
            val discrepanciesJson = JsonUtil.toJson(resultMap["discrepancies"] ?: emptyList<Any>())
            val discrepancies = JsonUtil.to(
                json = discrepanciesJson,
                typeReference = object : TypeReference<List<TemplateMigrationDiscrepancy>>() {}
            )

            TemplateMigrationValidationResult(
                projectId = projectId,
                success = status == MigrationStatus.SUCCESS.name,
                summary = TemplateMigrationValidationResult.ValidationSummary(
                    total = summaryMap["total"] ?: 0,
                    success = summaryMap["success"] ?: 0,
                    failed = summaryMap["failed"] ?: 0
                ),
                discrepancies = discrepancies
            )
        } catch (e: Exception) {
            logger.warn("Failed to parse validation result JSON for projectId=$projectId", e)
            TemplateMigrationValidationResult(
                projectId = projectId,
                success = false,
                summary = TemplateMigrationValidationResult.ValidationSummary(
                    total = 0,
                    success = 0,
                    failed = 0
                ),
                discrepancies = emptyList()
            )
        }
    }
}

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

import com.tencent.devops.common.pipeline.enums.BranchVersionAction
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.pojo.template.migration.TemplateMigrationDiscrepancy
import com.tencent.devops.process.pojo.template.migration.ValidationRuleType
import com.tencent.devops.process.pojo.template.migration.ValidationSeverity
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResourceCommonCondition
import com.tencent.devops.process.service.template.v2.PipelineTemplateInfoService
import com.tencent.devops.process.service.template.v2.PipelineTemplateResourceService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 数据完整性验证器
 * 验证模板数量、版本数量等一致性
 * 直接查询数据库获取实际数据进行对比
 */
@Component
class TemplateMigrationIntegrityValidator(
    private val dslContext: DSLContext,
    private val templateDao: TemplateDao,
    private val pipelineTemplateInfoService: PipelineTemplateInfoService,
    private val pipelineTemplateResourceService: PipelineTemplateResourceService
) : TemplateMigrationValidator {

    companion object {
        private val logger =
            LoggerFactory.getLogger(TemplateMigrationIntegrityValidator::class.java)
    }

    override fun getType(): ValidationRuleType = ValidationRuleType.INTEGRITY

    override fun validate(projectId: String): List<TemplateMigrationDiscrepancy> {
        val discrepancies = mutableListOf<TemplateMigrationDiscrepancy>()

        // 获取V1和V2的所有模板ID，供后续验证使用
        val v1TemplateIds = templateDao.listTemplateIds(dslContext, projectId).toSet()
        val v2TemplateIds = pipelineTemplateInfoService.listAllIds(projectId).toSet()

        // INT-001: 模板数量一致性（直接查询数据库）
        discrepancies.addAll(validateTemplateCount(projectId, v1TemplateIds.size, v2TemplateIds.size))

        // INT-002: 版本数量一致性
        discrepancies.addAll(validateVersionCount(projectId, v1TemplateIds))

        // INT-003: V1中存在但V2中缺失的模板（迁移遗漏）
        discrepancies.addAll(validateMissingInV2(v1TemplateIds, v2TemplateIds))

        // INT-004: V2中存在但V1中不存在的模板（孤岛数据）
        discrepancies.addAll(validateOrphanedInV2(v1TemplateIds, v2TemplateIds))

        return discrepancies
    }

    /**
     * INT-001: 验证模板数量一致性
     * 直接查询 T_TEMPLATE 和 T_PIPELINE_TEMPLATE_INFO 表对比数量
     */
    private fun validateTemplateCount(
        projectId: String,
        v1Count: Int,
        v2Count: Int
    ): List<TemplateMigrationDiscrepancy> {
        val discrepancies = mutableListOf<TemplateMigrationDiscrepancy>()

        if (v1Count != v2Count) {
            discrepancies.add(
                TemplateMigrationDiscrepancy(
                    ruleId = "INT-001",
                    ruleName = "模板数量一致性",
                    severity = ValidationSeverity.CRITICAL,
                    v1Value = v1Count.toString(),
                    v2Value = v2Count.toString(),
                    templateId = null,
                    version = null,
                    suggestion = "V1模板数量($v1Count)与V2模板数量($v2Count)不一致，" +
                        "请检查迁移是否完整"
                )
            )
        }

        logger.info(
            "Template count validation: V1=$v1Count, V2=$v2Count, projectId=$projectId"
        )
        return discrepancies
    }

    /**
     * INT-002: 验证版本数量一致性
     * 直接查询 T_TEMPLATE 和 T_PIPELINE_TEMPLATE_RESOURCE_VERSION 表
     * 对比每个模板的版本数
     *
     * 注意：约束模式模板（CONSTRAINT）需要特殊处理
     * V1 只记录一条引用关系，V2 会记录父模板的所有版本
     * 因此约束模式模板的 V2 版本数应该等于父模板（srcTemplateId）的 V1 版本数
     */
    private fun validateVersionCount(
        projectId: String,
        v1TemplateIds: Set<String>
    ): List<TemplateMigrationDiscrepancy> {
        val discrepancies = mutableListOf<TemplateMigrationDiscrepancy>()

        for (templateId in v1TemplateIds) {
            val templateInfo = pipelineTemplateInfoService.getOrNull(projectId, templateId)
            val v2VersionCount = pipelineTemplateResourceService.count(
                PipelineTemplateResourceCommonCondition(
                    projectId = projectId,
                    templateId = templateId,
                    includeDeleted = true,
                    excludeBranchAction = BranchVersionAction.INACTIVE
                )
            )
            // 约束模式模板：V2 版本数应该等于父模板的 V1 版本数
            if (templateInfo?.mode == TemplateType.CONSTRAINT) {
                val srcTemplateId = templateInfo.srcTemplateId
                val srcTemplateProjectId = templateInfo.srcTemplateProjectId
                if (srcTemplateId.isNullOrBlank() || srcTemplateProjectId.isNullOrBlank()) {
                    logger.warn(
                        "CONSTRAINT template missing srcTemplateId or srcTemplateProjectId: " +
                            "projectId=$projectId, templateId=$templateId"
                    )
                    continue
                }

                // 获取父模板的 V1 版本数
                val srcV1VersionCount = templateDao.countTemplateVersions(
                    dslContext, srcTemplateProjectId, srcTemplateId
                )
                if (srcV1VersionCount != v2VersionCount) {
                    discrepancies.add(
                        TemplateMigrationDiscrepancy(
                            ruleId = "INT-002",
                            ruleName = "版本数量一致性(约束模式)",
                            severity = ValidationSeverity.CRITICAL,
                            v1Value = "父模板[$srcTemplateId]版本数=$srcV1VersionCount",
                            v2Value = v2VersionCount.toString(),
                            templateId = templateId,
                            version = null,
                            suggestion = "约束模式模板[$templateId]的V2版本数($v2VersionCount)" +
                                "与父模板[$srcTemplateId]的V1版本数($srcV1VersionCount)不一致"
                        )
                    )
                }
                logger.info(
                    "CONSTRAINT template version validation: templateId=$templateId, " +
                        "srcTemplateId=$srcTemplateId, srcV1Versions=$srcV1VersionCount, " +
                        "v2Versions=$v2VersionCount"
                )
            } else {
                // 普通模板：V1 版本数应该等于 V2 版本数
                val v1VersionCount = templateDao.countTemplateVersions(
                    dslContext, projectId, templateId
                )

                if (v1VersionCount != v2VersionCount) {
                    discrepancies.add(
                        TemplateMigrationDiscrepancy(
                            ruleId = "INT-002",
                            ruleName = "版本数量一致性",
                            severity = ValidationSeverity.CRITICAL,
                            v1Value = v1VersionCount.toString(),
                            v2Value = v2VersionCount.toString(),
                            templateId = templateId,
                            version = null,
                            suggestion = "模板[$templateId]的V1版本数($v1VersionCount)" +
                                "与V2版本数($v2VersionCount)不一致"
                        )
                    )
                }
            }
        }

        return discrepancies
    }

    /**
     * INT-003: 验证V1中存在但V2中缺失的模板
     * 检测迁移遗漏的数据
     */
    private fun validateMissingInV2(
        v1TemplateIds: Set<String>,
        v2TemplateIds: Set<String>
    ): List<TemplateMigrationDiscrepancy> {
        val missingInV2 = v1TemplateIds - v2TemplateIds

        return missingInV2.map { templateId ->
            logger.warn("Template missing in V2: templateId={}", templateId)
            TemplateMigrationDiscrepancy(
                ruleId = "INT-003",
                ruleName = "迁移遗漏检测",
                severity = ValidationSeverity.CRITICAL,
                v1Value = "存在",
                v2Value = "缺失",
                templateId = templateId,
                version = null,
                suggestion = "模板[$templateId]在V1中存在但V2中缺失，" +
                    "可能迁移遗漏"
            )
        }
    }

    /**
     * INT-004: 验证V2中存在但V1中不存在的模板
     * 检测孤岛数据（可能是迁移过程中产生的脏数据）
     */
    private fun validateOrphanedInV2(
        v1TemplateIds: Set<String>,
        v2TemplateIds: Set<String>
    ): List<TemplateMigrationDiscrepancy> {
        val orphanedInV2 = v2TemplateIds - v1TemplateIds

        return orphanedInV2.map { templateId ->
            logger.warn("Orphaned template in V2: templateId={}", templateId)
            TemplateMigrationDiscrepancy(
                ruleId = "INT-004",
                ruleName = "孤岛数据检测",
                severity = ValidationSeverity.MEDIUM,
                v1Value = "不存在",
                v2Value = "存在",
                templateId = templateId,
                version = null,
                suggestion = "模板[$templateId]在V2中存在但V1中不存在，" +
                    "可能是孤岛数据，建议清理"
            )
        }
    }
}

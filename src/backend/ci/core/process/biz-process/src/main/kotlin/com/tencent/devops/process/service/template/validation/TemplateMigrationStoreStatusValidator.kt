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

import com.tencent.devops.common.client.Client
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.pojo.template.migration.TemplateMigrationDiscrepancy
import com.tencent.devops.process.pojo.template.migration.ValidationRuleType
import com.tencent.devops.process.pojo.template.migration.ValidationSeverity
import com.tencent.devops.process.service.template.v2.PipelineTemplateInfoService
import com.tencent.devops.store.api.template.ServiceTemplateResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 研发商店状态验证器
 * 验证 V2 表中的 STORE_STATUS 与研发商店实际状态一致
 */
@Component
class TemplateMigrationStoreStatusValidator(
    private val dslContext: DSLContext,
    private val templateDao: TemplateDao,
    private val pipelineTemplateInfoService: PipelineTemplateInfoService,
    private val client: Client
) : TemplateMigrationValidator {

    companion object {
        private val logger = LoggerFactory.getLogger(TemplateMigrationStoreStatusValidator::class.java)
    }

    override fun getType(): ValidationRuleType = ValidationRuleType.STORE_STATUS

    override fun validate(projectId: String): List<TemplateMigrationDiscrepancy> {
        val discrepancies = mutableListOf<TemplateMigrationDiscrepancy>()

        // 获取 V1 所有模板ID
        val v1TemplateIds = templateDao.listTemplateIds(dslContext, projectId)

        for (templateId in v1TemplateIds) {
            try {
                discrepancies.addAll(validateStoreStatus(projectId, templateId))
            } catch (ignore: Exception) {
                logger.error("Failed to validate store status for template $templateId", ignore)
                discrepancies.add(
                    TemplateMigrationDiscrepancy(
                        ruleId = "STR-ERR",
                        ruleName = "商店状态验证异常",
                        severity = ValidationSeverity.MEDIUM,
                        v1Value = "验证异常",
                        v2Value = ignore.message,
                        templateId = templateId,
                        version = null,
                        suggestion = "商店状态验证过程中发生异常: ${ignore.message}"
                    )
                )
            }
        }

        logger.info(
            "Store status validation completed for projectId=$projectId, found ${discrepancies.size} discrepancies"
        )
        return discrepancies
    }

    /**
     * STR-001: 验证研发商店状态一致性
     */
    private fun validateStoreStatus(
        projectId: String,
        templateId: String
    ): List<TemplateMigrationDiscrepancy> {
        val discrepancies = mutableListOf<TemplateMigrationDiscrepancy>()

        // 获取 V1 模板类型
        val v1Template = templateDao.getLatestTemplate(dslContext, projectId, templateId)

        // 约束模板（从研发商店安装）不需要验证商店状态
        if (v1Template.type == TemplateType.CONSTRAINT.name) {
            return discrepancies
        }

        // 获取 V2 模板信息
        val v2TemplateInfo = pipelineTemplateInfoService.getOrNull(projectId, templateId)
            ?: return discrepancies

        // 从研发商店获取实际状态
        val actualStoreStatus = try {
            client.get(ServiceTemplateResource::class).getMarketTemplateStatus(templateId).data
        } catch (ignore: Exception) {
            logger.warn("Failed to get market template status for templateId=$templateId", ignore)
            return discrepancies
        }

        val v2StoreStatus = v2TemplateInfo.storeStatus

        // 对比状态
        if (v2StoreStatus != actualStoreStatus) {
            discrepancies.add(
                TemplateMigrationDiscrepancy(
                    ruleId = "STR-001",
                    ruleName = "商店状态一致性",
                    severity = ValidationSeverity.HIGH,
                    v1Value = actualStoreStatus?.name ?: "未知",
                    v2Value = v2StoreStatus.name,
                    templateId = templateId,
                    version = null,
                    suggestion = "V2 表中的商店状态(${v2StoreStatus.name})与研发商店实际状态" +
                        "(${actualStoreStatus?.name})不一致"
                )
            )
        }

        return discrepancies
    }
}

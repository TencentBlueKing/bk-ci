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

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.process.engine.compatibility.BuildPropertyCompatibilityTools
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.pojo.template.migration.TemplateMigrationDiscrepancy
import com.tencent.devops.process.pojo.template.migration.ValidationRuleType
import com.tencent.devops.process.pojo.template.migration.ValidationSeverity
import com.tencent.devops.process.service.template.TemplateFacadeService
import com.tencent.devops.process.service.template.v2.PipelineTemplateResourceService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 模型验证器（精简版）
 * 仅验证有转换逻辑的字段：params 参数和构建号
 * 考虑迁移过程中的转换规则（mergeTemplateParamsIfNeeded、fixTemplateRequiredParam）
 */
@Component
class TemplateMigrationModelValidator(
    private val dslContext: DSLContext,
    private val templateDao: TemplateDao,
    private val templateFacadeService: TemplateFacadeService,
    private val pipelineTemplateResourceService: PipelineTemplateResourceService
) : TemplateMigrationValidator {

    companion object {
        private val logger = LoggerFactory.getLogger(TemplateMigrationModelValidator::class.java)
    }

    override fun getType(): ValidationRuleType = ValidationRuleType.MODEL

    override fun validate(projectId: String): List<TemplateMigrationDiscrepancy> {
        val discrepancies = mutableListOf<TemplateMigrationDiscrepancy>()

        // 获取 V1 所有模板ID
        val v1TemplateIds = templateDao.listTemplateIds(dslContext, projectId)

        for (templateId in v1TemplateIds) {
            // 获取模板所有版本
            val versions = templateFacadeService.listTemplateAllVersions(projectId, templateId, ascSort = true)
            versions.forEach { templateVersion ->
                val version = templateVersion.version
                try {
                    discrepancies.addAll(validateModelForVersion(projectId, templateId, version))
                } catch (ignore: Exception) {
                    logger.error("Failed to validate model for template $templateId version $version", ignore)
                    discrepancies.add(
                        TemplateMigrationDiscrepancy(
                            ruleId = "MDL-ERR",
                            ruleName = "模型验证异常",
                            severity = ValidationSeverity.HIGH,
                            v1Value = "验证异常",
                            v2Value = ignore.message,
                            templateId = templateId,
                            version = version,
                            suggestion = "模型验证过程中发生异常: ${ignore.message}"
                        )
                    )
                }
            }
        }

        logger.info("Model validation completed for projectId=$projectId, found ${discrepancies.size} discrepancies")
        return discrepancies
    }

    /**
     * 验证单个版本的模型（仅验证 params 和 buildNo）
     */
    private fun validateModelForVersion(
        projectId: String,
        templateId: String,
        version: Long
    ): List<TemplateMigrationDiscrepancy> {
        val discrepancies = mutableListOf<TemplateMigrationDiscrepancy>()

        // 获取 V1 模板 Model
        val v1Template = templateDao.getTemplate(dslContext = dslContext, version = version)
            ?: return discrepancies
        val v1Model = try {
            JsonUtil.to(v1Template.template, Model::class.java)
        } catch (ignore: Exception) {
            logger.warn("Failed to parse V1 model for template $templateId version $version", ignore)
            return discrepancies
        }

        // 获取 V2 对应版本
        val v2Resource = pipelineTemplateResourceService.getOrNull(
            projectId = projectId,
            templateId = templateId,
            version = version
        )

        if (v2Resource == null) {
            // V2 中不存在对应版本，由 IntegrityValidator 处理
            return discrepancies
        }

        val v2Model = v2Resource.model as? Model ?: return discrepancies

        // 转换 V1 Model（模拟迁移过程中的转换）
        val transformedV1Model = transformV1Model(v1Model)

        // MDL-001: 参数一致性验证
        discrepancies.addAll(validateParams(transformedV1Model, v2Model, templateId, version))

        // MDL-001-MERGE: 参数合并验证（验证 templateParams 是否正确合并到 params）
        discrepancies.addAll(validateTemplateParamsMerge(v1Model, v2Model, templateId, version))

        // MDL-002: 构建号一致性验证
        discrepancies.addAll(validateBuildNo(transformedV1Model, v2Model, templateId, version))

        return discrepancies
    }

    /**
     * 转换 V1 Model，模拟迁移过程中的转换规则
     */
    private fun transformV1Model(v1Model: Model): Model {
        // 深拷贝 Model
        val modelCopy = JsonUtil.to(JsonUtil.toJson(v1Model), Model::class.java)

        // 应用 mergeTemplateParamsIfNeeded 转换
        mergeTemplateParamsIfNeeded(modelCopy)

        // 应用 fixTemplateRequiredParam 转换
        fixTemplateRequiredParam(modelCopy)

        return modelCopy
    }

    /**
     * 合并模板参数到触发器参数
     * 复制自 PipelineTemplateGenerator.mergeTemplateParamsIfNeeded
     */
    private fun mergeTemplateParamsIfNeeded(model: Model) {
        val triggerContainer = model.getTriggerContainer()
        if (!triggerContainer.templateParams.isNullOrEmpty()) {
            triggerContainer.params = BuildPropertyCompatibilityTools.mergeProperties(
                from = triggerContainer.templateParams!!.map { it.copy(constant = true) },
                to = triggerContainer.params
            ).toMutableList()
            triggerContainer.templateParams = null
        }
    }

    /**
     * 修复模板参数的 required 和 asInstanceInput 属性
     * 复制自 PipelineTemplateGenerator.fixTemplateRequiredParam
     *
     * 规则（仅对 constant != true 且 asInstanceInput == null 的参数生效）：
     * - V1: required = true, asInstanceInput = null → V2: required = true, asInstanceInput = true
     * - V1: required = false, asInstanceInput = null → V2: required = true, asInstanceInput = false
     */
    private fun fixTemplateRequiredParam(model: Model) {
        val triggerContainer = model.getTriggerContainer()

        triggerContainer.params.forEach { param ->
            // 跳过模版常量和已设置 asInstanceInput 的参数
            if (param.constant == true || param.asInstanceInput != null) return@forEach
            if (param.required) {
                param.asInstanceInput = true
            } else {
                param.required = true
                param.asInstanceInput = false
            }
        }

        // 修复 buildNo
        triggerContainer.buildNo?.let { buildNo ->
            if (buildNo.asInstanceInput != null) return@let
            if (buildNo.required == true) {
                buildNo.asInstanceInput = true
            } else {
                buildNo.required = true
                buildNo.asInstanceInput = false
            }
        }
    }

    /**
     * MDL-001: 验证参数一致性
     */
    private fun validateParams(
        v1Model: Model,
        v2Model: Model,
        templateId: String,
        version: Long
    ): List<TemplateMigrationDiscrepancy> {
        val discrepancies = mutableListOf<TemplateMigrationDiscrepancy>()

        val v1Params = v1Model.getTriggerContainer().params
        val v2Params = v2Model.getTriggerContainer().params

        // 构建参数映射
        val v1ParamMap = v1Params.associateBy { it.id }
        val v2ParamMap = v2Params.associateBy { it.id }

        // 检查 V1 中存在但 V2 中不存在的参数
        v1ParamMap.keys.forEach { paramId ->
            if (!v2ParamMap.containsKey(paramId)) {
                discrepancies.add(
                    TemplateMigrationDiscrepancy(
                        ruleId = "MDL-001",
                        ruleName = "参数一致性-参数丢失",
                        severity = ValidationSeverity.HIGH,
                        v1Value = paramId,
                        v2Value = "不存在",
                        templateId = templateId,
                        version = version,
                        suggestion = "参数 $paramId 在 V2 中丢失"
                    )
                )
            }
        }

        // 检查参数属性一致性
        v1ParamMap.forEach { (paramId, v1Param) ->
            val v2Param = v2ParamMap[paramId] ?: return@forEach

            // 对于 constant = true 的参数，不验证 required/asInstanceInput
            if (v1Param.constant == true) {
                return@forEach
            }

            // 验证 required 属性
            if (v1Param.required != v2Param.required) {
                discrepancies.add(
                    TemplateMigrationDiscrepancy(
                        ruleId = "MDL-001",
                        ruleName = "参数属性一致性-required",
                        severity = ValidationSeverity.MEDIUM,
                        v1Value = "required=${v1Param.required}",
                        v2Value = "required=${v2Param.required}",
                        templateId = templateId,
                        version = version,
                        suggestion = "参数 $paramId 的 required 属性不一致"
                    )
                )
            }

            // 验证 asInstanceInput 属性
            if (v1Param.asInstanceInput != v2Param.asInstanceInput) {
                discrepancies.add(
                    TemplateMigrationDiscrepancy(
                        ruleId = "MDL-001",
                        ruleName = "参数属性一致性-asInstanceInput",
                        severity = ValidationSeverity.MEDIUM,
                        v1Value = "asInstanceInput=${v1Param.asInstanceInput}",
                        v2Value = "asInstanceInput=${v2Param.asInstanceInput}",
                        templateId = templateId,
                        version = version,
                        suggestion = "参数 $paramId 的 asInstanceInput 属性不一致"
                    )
                )
            }
        }

        return discrepancies
    }

    /**
     * MDL-001-MERGE: 验证 templateParams 合并结果
     * 验证 V1 的 templateParams 是否已正确合并到 V2 的 params 中
     */
    private fun validateTemplateParamsMerge(
        v1Model: Model,
        v2Model: Model,
        templateId: String,
        version: Long
    ): List<TemplateMigrationDiscrepancy> {
        val discrepancies = mutableListOf<TemplateMigrationDiscrepancy>()

        val v1TemplateParams = v1Model.getTriggerContainer().templateParams
        // 如果 V1 没有 templateParams，无需验证合并
        if (v1TemplateParams.isNullOrEmpty()) {
            return discrepancies
        }

        val v2Params = v2Model.getTriggerContainer().params
        val v2ParamMap = v2Params.associateBy { it.id }

        // 验证每个 templateParams 是否已合并到 params
        v1TemplateParams.forEach { templateParam ->
            val paramId = templateParam.id
            val v2Param = v2ParamMap[paramId]

            if (v2Param == null) {
                // templateParams 中的参数在 V2 params 中不存在
                discrepancies.add(
                    TemplateMigrationDiscrepancy(
                        ruleId = "MDL-001-MERGE",
                        ruleName = "参数合并验证-参数丢失",
                        severity = ValidationSeverity.HIGH,
                        v1Value = "templateParams 包含参数 $paramId",
                        v2Value = "params 中不存在",
                        templateId = templateId,
                        version = version,
                        suggestion = "templateParams 中的参数 $paramId 未合并到 params"
                    )
                )
            } else if (v2Param.constant != true) {
                // 合并后的参数应该是 constant = true
                discrepancies.add(
                    TemplateMigrationDiscrepancy(
                        ruleId = "MDL-001-MERGE",
                        ruleName = "参数合并验证-constant属性",
                        severity = ValidationSeverity.MEDIUM,
                        v1Value = "templateParams 参数 $paramId",
                        v2Value = "constant=${v2Param.constant}",
                        templateId = templateId,
                        version = version,
                        suggestion = "从 templateParams 合并的参数 $paramId 应设置 constant=true"
                    )
                )
            }
        }

        // 验证 V2 的 templateParams 应该为空（合并后置空）
        val v2TemplateParams = v2Model.getTriggerContainer().templateParams
        if (!v2TemplateParams.isNullOrEmpty()) {
            discrepancies.add(
                TemplateMigrationDiscrepancy(
                    ruleId = "MDL-001-MERGE",
                    ruleName = "参数合并验证-templateParams未清空",
                    severity = ValidationSeverity.MEDIUM,
                    v1Value = "templateParams 有 ${v1TemplateParams.size} 个参数",
                    v2Value = "templateParams 仍有 ${v2TemplateParams.size} 个参数",
                    templateId = templateId,
                    version = version,
                    suggestion = "合并后 V2 的 templateParams 应该为空"
                )
            )
        }

        return discrepancies
    }

    /**
     * MDL-002: 验证构建号一致性
     */
    private fun validateBuildNo(
        v1Model: Model,
        v2Model: Model,
        templateId: String,
        version: Long
    ): List<TemplateMigrationDiscrepancy> {
        val discrepancies = mutableListOf<TemplateMigrationDiscrepancy>()

        val v1BuildNo = v1Model.getTriggerContainer().buildNo
        val v2BuildNo = v2Model.getTriggerContainer().buildNo

        // 两者都为空，无需验证
        if (v1BuildNo == null && v2BuildNo == null) {
            return discrepancies
        }

        // 一个为空一个不为空
        if ((v1BuildNo == null) != (v2BuildNo == null)) {
            discrepancies.add(
                TemplateMigrationDiscrepancy(
                    ruleId = "MDL-002",
                    ruleName = "构建号一致性-存在性",
                    severity = ValidationSeverity.HIGH,
                    v1Value = if (v1BuildNo != null) "存在" else "不存在",
                    v2Value = if (v2BuildNo != null) "存在" else "不存在",
                    templateId = templateId,
                    version = version,
                    suggestion = "构建号配置存在性不一致"
                )
            )
            return discrepancies
        }

        // 验证构建号属性
        if (v1BuildNo != null && v2BuildNo != null) {
            if (v1BuildNo.required != v2BuildNo.required) {
                discrepancies.add(
                    TemplateMigrationDiscrepancy(
                        ruleId = "MDL-002",
                        ruleName = "构建号一致性-required",
                        severity = ValidationSeverity.MEDIUM,
                        v1Value = "required=${v1BuildNo.required}",
                        v2Value = "required=${v2BuildNo.required}",
                        templateId = templateId,
                        version = version,
                        suggestion = "构建号 required 属性不一致"
                    )
                )
            }

            if (v1BuildNo.asInstanceInput != v2BuildNo.asInstanceInput) {
                discrepancies.add(
                    TemplateMigrationDiscrepancy(
                        ruleId = "MDL-002",
                        ruleName = "构建号一致性-asInstanceInput",
                        severity = ValidationSeverity.MEDIUM,
                        v1Value = "asInstanceInput=${v1BuildNo.asInstanceInput}",
                        v2Value = "asInstanceInput=${v2BuildNo.asInstanceInput}",
                        templateId = templateId,
                        version = version,
                        suggestion = "构建号 asInstanceInput 属性不一致"
                    )
                )
            }
        }

        return discrepancies
    }
}

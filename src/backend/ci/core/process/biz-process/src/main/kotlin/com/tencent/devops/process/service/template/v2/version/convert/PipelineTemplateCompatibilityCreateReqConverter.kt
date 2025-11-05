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

package com.tencent.devops.process.service.template.v2.version.convert

import com.tencent.devops.common.pipeline.enums.PipelineStorageType
import com.tencent.devops.common.pipeline.enums.PipelineVersionAction
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.template.PipelineTemplateType
import com.tencent.devops.process.constant.PipelineTemplateConstant
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.pojo.template.v2.PTemplateModelTransferResult
import com.tencent.devops.process.pojo.template.v2.PTemplateResourceWithoutVersion
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateCompatibilityCreateReq
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInfoV2
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateVersionReq
import com.tencent.devops.process.service.template.v2.PipelineTemplateResourceService
import com.tencent.devops.process.service.template.v2.PipelineTemplateGenerator
import com.tencent.devops.process.service.template.v2.PipelineTemplateModelInitializer
import com.tencent.devops.process.service.template.v2.version.PipelineTemplateVersionCreateContext
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线模板兼容创建请求转换
 */
@Service
class PipelineTemplateCompatibilityCreateReqConverter @Autowired constructor(
    private val pipelineTemplateGenerator: PipelineTemplateGenerator,
    private val pipelineTemplateModelInitializer: PipelineTemplateModelInitializer,
    private val templateDao: TemplateDao,
    private val dslContext: DSLContext,
    private val pipelineTemplateResourceService: PipelineTemplateResourceService
) : PipelineTemplateVersionReqConverter {

    override fun support(request: PipelineTemplateVersionReq): Boolean {
        return request is PipelineTemplateCompatibilityCreateReq
    }

    override fun convert(
        userId: String,
        projectId: String,
        templateId: String?,
        version: Long?,
        request: PipelineTemplateVersionReq
    ): PipelineTemplateVersionCreateContext {
        request as PipelineTemplateCompatibilityCreateReq
        with(request) {
            logger.info("compatibility create converter:$userId|$projectId|$templateId|$version|$request")
            if (templateId == null) {
                throw IllegalArgumentException("templateId is null")
            }
            val transferResult = try {
                pipelineTemplateGenerator.transfer(
                    userId = userId,
                    projectId = projectId,
                    storageType = PipelineStorageType.MODEL,
                    templateType = PipelineTemplateType.PIPELINE,
                    templateModel = model,
                    templateSetting = setting,
                    params = model.getTriggerContainer().params,
                    yaml = null
                )
            } catch (ex: Exception) {
                logger.warn("TRANSFER_TEMPLATE_YAML_FAILED|$projectId|$templateId", ex)
                PTemplateModelTransferResult(
                    templateType = PipelineTemplateType.PIPELINE,
                    templateModel = model,
                    templateSetting = setting,
                    params = model.getTriggerContainer().params,
                    yamlWithVersion = null
                )
            }

            // v2新版本中，版本名称需要唯一：先结合v1计数推断，再以v2实时数据兜底校验
            val v2CustomVersionName = v1VersionName.let { baseName ->
                val existingCountInV1 = templateDao.countTemplateVersions(
                    dslContext = dslContext,
                    projectId = projectId,
                    templateId = templateId,
                    versionName = baseName
                )
                val preferred = if (existingCountInV1 > 0) {
                    "$baseName-${existingCountInV1 + 1}"
                } else {
                    baseName
                }
                ensureV2UniqueVersionName(projectId, templateId, preferred)
            }

            val pipelineTemplateInfo = PipelineTemplateInfoV2(
                id = templateId,
                projectId = projectId,
                name = model.name,
                desc = model.desc,
                mode = TemplateType.CUSTOMIZE,
                type = PipelineTemplateType.PIPELINE,
                enablePac = false,
                category = category,
                logoUrl = logoUrl,
                creator = userId,
                updater = userId,
                latestVersionStatus = VersionStatus.RELEASED
            )
            pipelineTemplateModelInitializer.initTemplateModel(transferResult.templateModel)
            val pTemplateResourceWithoutVersion = PTemplateResourceWithoutVersion(
                projectId = projectId,
                templateId = templateId,
                type = PipelineTemplateType.PIPELINE,
                model = transferResult.templateModel,
                params = transferResult.params,
                yaml = transferResult.yamlWithVersion?.yamlStr,
                status = VersionStatus.RELEASED,
                sortWeight = PipelineTemplateConstant.OTHER_STATUS_VERSION_SORT_WIGHT,
                srcTemplateVersion = null,
                srcTemplateProjectId = null,
                srcTemplateId = null,
                creator = userId,
                updater = userId
            )
            return PipelineTemplateVersionCreateContext(
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                v1VersionName = v1VersionName,
                customVersionName = v2CustomVersionName,
                versionAction = PipelineVersionAction.CREATE_RELEASE,
                pipelineTemplateInfo = pipelineTemplateInfo,
                pTemplateResourceWithoutVersion = pTemplateResourceWithoutVersion,
                pTemplateSettingWithoutVersion = transferResult.templateSetting
            )
        }
    }

    private fun ensureV2UniqueVersionName(
        projectId: String,
        templateId: String,
        base: String
    ): String {
        var name = base
        var index = 1
        while (
            pipelineTemplateResourceService.getLatestResource(
                projectId = projectId,
                templateId = templateId,
                status = VersionStatus.RELEASED,
                versionName = name
            ) != null
        ) {
            val m = Regex("^(.*?)(-(\\d+))?$").matchEntire(base)!!
            val plain = m.groupValues[1]
            val seed = m.groupValues.getOrNull(3)?.toIntOrNull() ?: 1
            val n = seed + index
            name = "$plain-$n"
            index++
        }
        return name
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTemplateCompatibilityCreateReqConverter::class.java)
    }
}

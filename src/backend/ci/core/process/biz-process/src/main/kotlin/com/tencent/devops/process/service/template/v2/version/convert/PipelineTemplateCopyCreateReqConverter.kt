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

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.PipelineStorageType
import com.tencent.devops.common.pipeline.enums.PipelineVersionAction
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.template.PipelineTemplateType
import com.tencent.devops.process.constant.PipelineTemplateConstant
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_TEMPLATE_LATEST_VERSION_NOT_PUBLISHED
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.pojo.template.v2.PTemplateResourceWithoutVersion
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateCopyCreateReq
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInfoV2
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateVersionReq
import com.tencent.devops.process.service.template.v2.PipelineTemplateCommonService
import com.tencent.devops.process.service.template.v2.PipelineTemplateGenerator
import com.tencent.devops.process.service.template.v2.PipelineTemplateInfoService
import com.tencent.devops.process.service.template.v2.PipelineTemplateModelInitializer
import com.tencent.devops.process.service.template.v2.PipelineTemplateResourceService
import com.tencent.devops.process.service.template.v2.PipelineTemplateSettingService
import com.tencent.devops.process.service.template.v2.version.PipelineTemplateVersionCreateContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线模板复制创建请求转换
 */
@Service
class PipelineTemplateCopyCreateReqConverter @Autowired constructor(
    private val pipelineTemplateCommonService: PipelineTemplateCommonService,
    private val pipelineTemplateGenerator: PipelineTemplateGenerator,
    private val pipelineTemplateInfoService: PipelineTemplateInfoService,
    private val pipelineTemplateResourceService: PipelineTemplateResourceService,
    private val pipelineTemplateSettingService: PipelineTemplateSettingService,
    private val pipelineTemplateModelInitializer: PipelineTemplateModelInitializer
) : PipelineTemplateVersionReqConverter {

    override fun support(request: PipelineTemplateVersionReq): Boolean {
        return request is PipelineTemplateCopyCreateReq
    }

    override fun convert(
        userId: String,
        projectId: String,
        templateId: String?,
        version: Long?,
        request: PipelineTemplateVersionReq
    ): PipelineTemplateVersionCreateContext {
        request as PipelineTemplateCopyCreateReq
        with(request) {
            val srcTemplateInfo = pipelineTemplateInfoService.get(
                projectId = projectId,
                templateId = srcTemplateId
            )
            if (srcTemplateInfo.latestVersionStatus != VersionStatus.RELEASED) {
                throw ErrorCodeException(
                    errorCode = ERROR_TEMPLATE_LATEST_VERSION_NOT_PUBLISHED,
                    params = arrayOf(srcTemplateId)
                )
            }
            val newTemplateId = pipelineTemplateGenerator.generateTemplateId()
            pipelineTemplateCommonService.checkTemplateBasicInfo(
                projectId = projectId,
                name = name
            )

            val srcTemplateResource = pipelineTemplateResourceService.get(
                projectId = projectId,
                templateId = srcTemplateId,
                version = srcTemplateVersion ?: srcTemplateInfo.releasedVersion
            )

            if (srcTemplateInfo.type == PipelineTemplateType.PIPELINE) {
                (srcTemplateResource.model as Model).name = request.name
            }

            val setting = if (copySetting) {
                val srcTemplateSetting = pipelineTemplateSettingService.get(
                    projectId = projectId,
                    templateId = srcTemplateId,
                    settingVersion = srcTemplateResource.settingVersion
                )
                srcTemplateSetting.copy(
                    pipelineId = newTemplateId,
                    projectId = projectId,
                    pipelineName = name,
                    version = PipelineTemplateConstant.INIT_VERSION,
                    creator = userId
                )
            } else {
                pipelineTemplateGenerator.getDefaultSetting(
                    type = srcTemplateResource.type,
                    projectId = projectId,
                    templateId = newTemplateId,
                    templateName = name,
                    desc = srcTemplateInfo.desc,
                    creator = userId
                )
            }

            val transferResult = pipelineTemplateGenerator.transfer(
                userId = userId,
                projectId = projectId,
                storageType = PipelineStorageType.MODEL,
                templateType = srcTemplateInfo.type,
                templateModel = srcTemplateResource.model,
                templateSetting = setting,
                params = srcTemplateResource.params,
                yaml = null,
                fallbackOnError = true
            )

            val pipelineTemplateInfo = PipelineTemplateInfoV2(
                id = newTemplateId,
                projectId = projectId,
                name = name,
                desc = srcTemplateInfo.desc,
                mode = TemplateType.CUSTOMIZE,
                category = srcTemplateInfo.category,
                type = srcTemplateInfo.type,
                logoUrl = srcTemplateInfo.logoUrl,
                enablePac = false,
                creator = userId,
                updater = userId,
                latestVersionStatus = VersionStatus.RELEASED
            )
            pipelineTemplateModelInitializer.initTemplateModel(transferResult.templateModel)
            val pTemplateResourceWithoutVersion = PTemplateResourceWithoutVersion(
                projectId = projectId,
                templateId = newTemplateId,
                type = srcTemplateResource.type,
                params = transferResult.params,
                model = transferResult.templateModel,
                yaml = transferResult.yamlWithVersion?.yamlStr ?: "",
                status = VersionStatus.RELEASED,
                creator = userId,
                updater = userId
            )
            return PipelineTemplateVersionCreateContext(
                userId = userId,
                projectId = projectId,
                templateId = newTemplateId,
                versionAction = PipelineVersionAction.CREATE_RELEASE,
                newTemplate = true,
                pipelineTemplateInfo = pipelineTemplateInfo,
                pTemplateResourceWithoutVersion = pTemplateResourceWithoutVersion,
                pTemplateSettingWithoutVersion = transferResult.templateSetting
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTemplateCopyCreateReqConverter::class.java)
    }
}

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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.enums.PipelineVersionAction
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_TEMPLATE_NOT_EXISTS
import com.tencent.devops.process.pojo.template.v2.PTemplateResourceWithoutVersion
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateDraftReleaseReq
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateVersionReq
import com.tencent.devops.process.service.template.v2.PipelineTemplateInfoService
import com.tencent.devops.process.service.template.v2.PipelineTemplateResourceService
import com.tencent.devops.process.service.template.v2.PipelineTemplateSettingService
import com.tencent.devops.process.service.template.v2.version.PipelineTemplateVersionCreateContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 发布草稿转换
 */
@Service
class PipelineTemplateDraftReleaseReqConverter @Autowired constructor(
    private val pipelineTemplateInfoService: PipelineTemplateInfoService,
    private val pipelineTemplateResourceService: PipelineTemplateResourceService,
    private val pipelineTemplateSettingService: PipelineTemplateSettingService
) : PipelineTemplateVersionReqConverter {
    override fun support(request: PipelineTemplateVersionReq): Boolean {
        return request is PipelineTemplateDraftReleaseReq
    }

    override fun convert(
        userId: String,
        projectId: String,
        templateId: String?,
        version: Long?,
        request: PipelineTemplateVersionReq
    ): PipelineTemplateVersionCreateContext {
        request as PipelineTemplateDraftReleaseReq
        with(request) {
            if (templateId == null) {
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                    params = arrayOf("templateId")
                )
            }
            if (version == null) {
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                    params = arrayOf("version")
                )
            }
            val pipelineTemplateInfo = pipelineTemplateInfoService.get(
                projectId = projectId,
                templateId = templateId
            )
            val draftResource = pipelineTemplateResourceService.get(
                projectId = projectId, templateId = templateId, version = version
            )
            if (draftResource.status != VersionStatus.COMMITTING) {
                throw ErrorCodeException(errorCode = ERROR_TEMPLATE_NOT_EXISTS)
            }
            if (enablePac) {
                if (targetAction == null) {
                    throw ErrorCodeException(
                        errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                        params = arrayOf("targetAction")
                    )
                }
                if (yamlInfo == null) {
                    throw ErrorCodeException(
                        errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                        params = arrayOf("yamlFileInfo")
                    )
                }
                if (draftResource.yaml == null) {
                    throw ErrorCodeException(
                        errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                        params = arrayOf("yaml content")
                    )
                }
            }
            val pTemplateSettingWithoutVersion = pipelineTemplateSettingService.get(
                projectId = projectId,
                templateId = templateId,
                settingVersion = draftResource.settingVersion
            )
            val pTemplateResourceWithoutVersion = PTemplateResourceWithoutVersion(draftResource).copy(
                description = description
            )
            return PipelineTemplateVersionCreateContext(
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                version = version,
                customVersionName = customVersionName,
                versionAction = PipelineVersionAction.RELEASE_DRAFT,
                pipelineTemplateInfo = pipelineTemplateInfo,
                pTemplateResourceWithoutVersion = pTemplateResourceWithoutVersion,
                pTemplateSettingWithoutVersion = pTemplateSettingWithoutVersion,
                yamlFileInfo = yamlInfo,
                enablePac = enablePac,
                targetAction = targetAction,
                branchName = targetBranch
            )
        }
    }
}

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
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.PipelineVersionAction
import com.tencent.devops.common.pipeline.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.template.UpgradeStrategyEnum
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.pojo.template.v2.PTemplateResourceWithoutVersion
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInfoV2
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateMarketCreateReq
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateVersionReq
import com.tencent.devops.process.service.template.v2.PipelineTemplateCommonService
import com.tencent.devops.process.service.template.v2.PipelineTemplateGenerator
import com.tencent.devops.process.service.template.v2.PipelineTemplateInfoService
import com.tencent.devops.process.service.template.v2.PipelineTemplateResourceService
import com.tencent.devops.process.service.template.v2.PipelineTemplateSettingService
import com.tencent.devops.process.service.template.v2.version.PipelineTemplateVersionCreateContext
import com.tencent.devops.process.service.`var`.PublicVarGroupReferManageService
import com.tencent.devops.store.api.template.ServiceTemplateResource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线模板研发商店创建请求转换
 */
@Service
class PipelineTemplateMarketCreateReqConverter @Autowired constructor(
    private val pipelineTemplateCommonService: PipelineTemplateCommonService,
    private val pipelineTemplateGenerator: PipelineTemplateGenerator,
    private val client: Client,
    private val pipelineTemplateInfoService: PipelineTemplateInfoService,
    private val pipelineTemplateResourceService: PipelineTemplateResourceService,
    private val pipelineTemplateSettingService: PipelineTemplateSettingService,
    private val publicVarGroupReferManageService: PublicVarGroupReferManageService
) : PipelineTemplateVersionReqConverter {

    override fun support(request: PipelineTemplateVersionReq): Boolean {
        return request is PipelineTemplateMarketCreateReq
    }

    override fun convert(
        userId: String,
        projectId: String,
        templateId: String?,
        version: Long?,
        request: PipelineTemplateVersionReq
    ): PipelineTemplateVersionCreateContext {
        request as PipelineTemplateMarketCreateReq
        with(request) {
            val marketTemplateDetails = client.get(ServiceTemplateResource::class).getTemplateDetailByCode(
                userId = userId,
                templateCode = marketTemplateId
            ).data ?: throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_SOURCE_TEMPLATE_NOT_EXISTS)
            val marketTemplateInfo = pipelineTemplateInfoService.get(
                projectId = marketTemplateProjectId,
                templateId = marketTemplateId
            )
            val finalMarketTemplateVersion = marketTemplateVersion
                ?: pipelineTemplateResourceService.getLatestPublishedResource(
                    projectId = marketTemplateProjectId,
                    templateId = marketTemplateId
                )?.version ?: throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_TEMPLATE_LATEST_PUBLISHED_VERSION_NOT_EXIST
                )

            val marketTemplateResource = pipelineTemplateResourceService.get(
                projectId = marketTemplateProjectId,
                templateId = marketTemplateId,
                version = finalMarketTemplateVersion
            )
            val templateName = name ?: marketTemplateDetails.templateName
            if (templateId == null) {
                pipelineTemplateCommonService.checkTemplateBasicInfo(
                    projectId = projectId,
                    name = templateName
                )
            }

            val newTemplateId = templateId ?: pipelineTemplateGenerator.generateTemplateId()
            val setting = if (copySetting) {
                val srcTemplateSetting = pipelineTemplateSettingService.get(
                    projectId = marketTemplateProjectId,
                    templateId = marketTemplateId,
                    settingVersion = marketTemplateResource.settingVersion
                )
                srcTemplateSetting.copy(
                    pipelineId = newTemplateId,
                    projectId = projectId,
                    pipelineName = templateName,
                    labels = emptyList(),
                    creator = userId
                )
            } else {
                pipelineTemplateGenerator.getDefaultSetting(
                    type = marketTemplateResource.type,
                    projectId = projectId,
                    templateId = newTemplateId,
                    creator = userId,
                    templateName = templateName,
                    desc = marketTemplateDetails.description
                )
            }

            val templateInfo = pipelineTemplateInfoService.getOrNull(
                projectId = projectId,
                templateId = newTemplateId
            )

            val pipelineTemplateInfo = PipelineTemplateInfoV2(
                id = newTemplateId,
                projectId = projectId,
                name = templateName,
                desc = marketTemplateDetails.description,
                mode = TemplateType.CONSTRAINT,
                type = marketTemplateInfo.type,
                enablePac = templateInfo?.enablePac ?: false,
                creator = userId,
                updater = userId,
                srcTemplateProjectId = marketTemplateInfo.projectId,
                srcTemplateId = marketTemplateInfo.id,
                category = marketTemplateDetails.categoryList.takeIf { it != null }?.let { categoryList ->
                    JsonUtil.toJson(categoryList.map { it.categoryCode })
                },
                logoUrl = marketTemplateDetails.logoUrl,
                latestVersionStatus = VersionStatus.RELEASED,
                upgradeStrategy = templateInfo?.upgradeStrategy ?: UpgradeStrategyEnum.AUTO,
                settingSyncStrategy = templateInfo?.upgradeStrategy ?: UpgradeStrategyEnum.AUTO
            )
            val pTemplateResourceWithoutVersion = PTemplateResourceWithoutVersion(
                projectId = projectId,
                templateId = newTemplateId,
                type = marketTemplateInfo.type,
                srcTemplateProjectId = marketTemplateInfo.projectId,
                srcTemplateId = marketTemplateInfo.id,
                srcTemplateVersion = marketTemplateResource.version,
                model = marketTemplateResource.model,
                yaml = marketTemplateResource.yaml,
                params = marketTemplateResource.params,
                creator = userId,
                status = VersionStatus.RELEASED
            )

            // 处理跨项目变量组引用，展开变量组中的变量到params
            (pTemplateResourceWithoutVersion.model as? Model)?.let { model ->
                publicVarGroupReferManageService.handleCrossProjectVarGroup(
                    projectId = marketTemplateProjectId,
                    referId = marketTemplateId,
                    referType = PublicVerGroupReferenceTypeEnum.TEMPLATE,
                    referVersion = finalMarketTemplateVersion.toInt(),
                    model = model
                )
            }

            return PipelineTemplateVersionCreateContext(
                userId = userId,
                projectId = projectId,
                templateId = newTemplateId,
                versionAction = PipelineVersionAction.CREATE_RELEASE,
                newTemplate = templateId == null,
                pipelineTemplateInfo = pipelineTemplateInfo,
                pTemplateResourceWithoutVersion = pTemplateResourceWithoutVersion,
                pTemplateSettingWithoutVersion = setting
            )
        }
    }
}

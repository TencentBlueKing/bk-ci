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

package com.tencent.devops.process.api.template.v2

import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.template.UpgradeStrategyEnum
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.engine.pojo.event.PipelineTemplateTriggerUpgradesEvent
import com.tencent.devops.process.pojo.PTemplateOrderByType
import com.tencent.devops.process.pojo.PTemplateSortType
import com.tencent.devops.process.pojo.PipelineTemplateVersionSimple
import com.tencent.devops.process.pojo.enums.TemplateSortTypeEnum
import com.tencent.devops.process.pojo.pipeline.DeployTemplateResult
import com.tencent.devops.process.pojo.template.OptionalTemplateList
import com.tencent.devops.process.pojo.template.TemplateInstanceCreate
import com.tencent.devops.process.pojo.template.TemplateInstancePage
import com.tencent.devops.process.pojo.template.TemplateInstanceUpdate
import com.tencent.devops.process.pojo.template.TemplateListModel
import com.tencent.devops.process.pojo.template.TemplateModelDetail
import com.tencent.devops.process.pojo.template.TemplateOperationRet
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.pojo.template.v2.MarketTemplateV2Request
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateDetailsResponse
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInfoResponse
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateMarketCreateReq
import com.tencent.devops.process.service.template.v2.PipelineTemplateFacadeService
import com.tencent.devops.process.service.template.v2.PipelineTemplateInfoService
import com.tencent.devops.process.service.template.v2.PipelineTemplateMarketFacadeService
import com.tencent.devops.process.service.template.v2.PipelineTemplateResourceService
import com.tencent.devops.process.service.template.v2.PipelineTemplateCompatibilityAdapter
import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum

@RestResource
class ServicePipelineTemplateV2ResourceImpl(
    private val pipelineTemplateMarketFacadeService: PipelineTemplateMarketFacadeService,
    private val pipelineTemplateInfoService: PipelineTemplateInfoService,
    private val pipelineTemplateFacadeService: PipelineTemplateFacadeService,
    private val pipelineTemplateResourceService: PipelineTemplateResourceService,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val pipelineTemplateCompatibilityAdapter: PipelineTemplateCompatibilityAdapter
) : ServicePipelineTemplateV2Resource {
    override fun handleMarketTemplatePublished(request: MarketTemplateV2Request): Result<Boolean> {
        return Result(pipelineTemplateMarketFacadeService.handleMarketTemplatePublished(request))
    }

    override fun handleMarketTemplateVersionPublished(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long
    ): Result<Boolean> {
        pipelineEventDispatcher.dispatch(
            PipelineTemplateTriggerUpgradesEvent(
                projectId = projectId,
                source = "PIPELINE_TEMPLATE_TRIGGER_UPGRADES",
                pipelineId = "",
                userId = userId,
                templateId = templateId,
                version = version
            )
        )
        return Result(true)
    }

    override fun getTemplateDetails(
        projectId: String,
        templateId: String,
        version: Long?
    ): Result<PipelineTemplateDetailsResponse> {
        return Result(
            pipelineTemplateFacadeService.getTemplateDetails(
                projectId = projectId,
                templateId = templateId,
                version = version
            )
        )
    }

    override fun checkImageReleaseStatus(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long
    ): Result<String?> {
        return pipelineTemplateMarketFacadeService.checkImageReleaseStatus(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            version = version
        )
    }

    override fun getSrcTemplateCodes(projectId: String): Result<List<String>> {
        return Result(pipelineTemplateInfoService.listSrcTemplateIds(projectId))
    }

    override fun updateStoreStatus(
        userId: String,
        projectId: String,
        templateId: String,
        storeStatus: TemplateStatusEnum,
        version: Long?
    ): Result<Boolean> {
        return Result(
            pipelineTemplateMarketFacadeService.updateStoreStatus(
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                storeStatus = storeStatus,
                version = version
            )
        )
    }

    override fun updatePublishStrategy(
        userId: String,
        templateId: String,
        strategy: UpgradeStrategyEnum
    ): Result<Boolean> {
        return Result(
            pipelineTemplateMarketFacadeService.updatePublishStrategy(
                userId = userId,
                templateId = templateId,
                strategy = strategy
            )
        )
    }

    override fun checkWhenPublishedTemplate(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long
    ): Result<Boolean> {
        return Result(
            pipelineTemplateFacadeService.checkWhenPublishedTemplate(
                projectId = projectId,
                userId = userId,
                templateId = templateId,
                version = version
            )
        )
    }

    override fun getTemplateInfo(
        userId: String,
        projectId: String,
        templateId: String
    ): Result<PipelineTemplateInfoResponse> {
        return Result(
            pipelineTemplateFacadeService.getTemplateInfo(
                userId = userId,
                projectId = projectId,
                templateId = templateId
            )
        )
    }

    override fun listLatestReleasedVersions(templateIds: List<String>): Result<List<PipelineTemplateVersionSimple>> {
        return Result(pipelineTemplateResourceService.listLatestReleasedVersions(templateIds))
    }

    override fun listPacSettings(
        templateIds: List<String>
    ): Result<Map<String, Boolean>> {
        return Result(
            pipelineTemplateInfoService.listPacSettings(
                templateIds = templateIds
            )
        )
    }

    @AuditEntry(actionId = ActionId.PIPELINE_TEMPLATE_CREATE)
    override fun createByMarket(
        userId: String,
        projectId: String,
        templateId: String?,
        request: PipelineTemplateMarketCreateReq
    ): Result<DeployTemplateResult> {
        return Result(
            pipelineTemplateFacadeService.createByMarket(
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                request = request
            )
        )
    }

    override fun getTemplate(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long?,
        versionName: String?
    ): Result<TemplateModelDetail> {
        return Result(
            pipelineTemplateCompatibilityAdapter.getTemplate(
                projectId = projectId,
                userId = userId,
                templateId = templateId,
                version = version,
                versionName = versionName
            )
        )
    }

    override fun listTemplate(
        userId: String,
        projectId: String,
        templateType: TemplateType?,
        storeFlag: Boolean?,
        orderBy: PTemplateOrderByType?,
        sort: PTemplateSortType?,
        page: Int,
        pageSize: Int
    ): Result<TemplateListModel> {
        return Result(
            pipelineTemplateCompatibilityAdapter.listTemplate(
                userId = userId,
                projectId = projectId,
                templateType = templateType,
                storeFlag = storeFlag,
                orderBy = orderBy,
                sort = sort,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun listAllTemplate(
        userId: String,
        projectId: String
    ): Result<OptionalTemplateList> {
        return Result(
            pipelineTemplateCompatibilityAdapter.listAllTemplates(
                userId = userId,
                projectId = projectId
            )
        )
    }

    override fun createTemplate(
        userId: String,
        projectId: String,
        template: Model
    ): Result<String> {
        return Result(
            pipelineTemplateCompatibilityAdapter.createTemplate(
                userId = userId,
                projectId = projectId,
                template = template
            )
        )
    }

    override fun updateTemplate(
        userId: String,
        projectId: String,
        templateId: String,
        versionName: String,
        template: Model
    ): Result<Long> {
        return Result(
            pipelineTemplateCompatibilityAdapter.updateTemplate(
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                versionName = versionName,
                template = template
            )
        )
    }

    override fun deleteTemplate(
        userId: String,
        projectId: String,
        templateId: String
    ): Result<Boolean> {
        return Result(
            pipelineTemplateCompatibilityAdapter.deleteTemplate(
                userId = userId,
                projectId = projectId,
                templateId = templateId
            )
        )
    }

    override fun deleteTemplateVersion(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long
    ): Result<Boolean> {
        return Result(
            pipelineTemplateCompatibilityAdapter.deleteVersion(
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                version = version
            )
        )
    }

    override fun createTemplateInstances(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long,
        useTemplateSettings: Boolean,
        instances: List<TemplateInstanceCreate>
    ): Result<TemplateOperationRet> {
        return Result(
            pipelineTemplateCompatibilityAdapter.createTemplateInstances(
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                version = version,
                useTemplateSettings = useTemplateSettings,
                instances = instances
            )
        )
    }

    override fun updateTemplateInstances(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long,
        useTemplateSettings: Boolean,
        instances: List<TemplateInstanceUpdate>
    ): Result<TemplateOperationRet> {
        return Result(
            pipelineTemplateCompatibilityAdapter.updateTemplateInstances(
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                version = version,
                useTemplateSettings = useTemplateSettings,
                instances = instances
            )
        )
    }

    override fun updateTemplateInstancesByName(
        userId: String,
        projectId: String,
        templateId: String,
        versionName: String,
        useTemplateSettings: Boolean,
        instances: List<TemplateInstanceUpdate>
    ): Result<TemplateOperationRet> {
        return Result(
            pipelineTemplateCompatibilityAdapter.updateTemplateInstances(
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                versionName = versionName,
                useTemplateSettings = useTemplateSettings,
                instances = instances
            )
        )
    }

    override fun listTemplateInstances(
        userId: String,
        projectId: String,
        templateId: String,
        page: Int?,
        pageSize: Int?,
        searchKey: String?,
        sortType: TemplateSortTypeEnum?,
        desc: Boolean?
    ): Result<TemplateInstancePage> {
        return Result(
            pipelineTemplateCompatibilityAdapter.listTemplateInstances(
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                page = page ?: 1,
                pageSize = pageSize ?: 20,
                searchKey = searchKey,
                sortType = sortType,
                desc = desc
            )
        )
    }
}

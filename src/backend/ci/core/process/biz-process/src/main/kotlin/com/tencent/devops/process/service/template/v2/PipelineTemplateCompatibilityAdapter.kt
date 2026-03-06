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

package com.tencent.devops.process.service.template.v2

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.TemplateRefType
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildNo
import com.tencent.devops.common.pipeline.pojo.TemplateInstanceField
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.permission.template.PipelineTemplatePermissionService
import com.tencent.devops.process.pojo.PTemplateOrderByType
import com.tencent.devops.process.pojo.PTemplateSortType
import com.tencent.devops.process.pojo.enums.TemplateSortTypeEnum
import com.tencent.devops.process.pojo.template.OptionalTemplateList
import com.tencent.devops.process.pojo.template.TemplateInstanceCreate
import com.tencent.devops.process.pojo.template.TemplateInstancePage
import com.tencent.devops.process.pojo.template.TemplateInstanceUpdate
import com.tencent.devops.process.pojo.template.TemplateListModel
import com.tencent.devops.process.pojo.template.TemplateModel
import com.tencent.devops.process.pojo.template.TemplateModelDetail
import com.tencent.devops.process.pojo.template.TemplateOperationRet
import com.tencent.devops.process.pojo.template.TemplatePipeline
import com.tencent.devops.process.pojo.template.TemplatePipelineStatus
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.pojo.template.TemplateVersion
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateCommonCondition
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInstanceReleaseInfo
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInstancesRequest
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateReleaseCreateReq
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResource
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResourceCommonCondition
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.service.template.TemplateCommonService
import com.tencent.devops.process.service.template.v2.version.PipelineTemplateVersionManager
import org.springframework.stereotype.Service

/**
 * 模板接口 V1→V2 兼容适配器。
 *
 * 将 V1 的入参/返回值类型与 V2 的类型互相转换，
 * 使调用方可以透明地从 V1 Service 切换到 V2 Service。
 *
 * 该类不会修改 V2 核心服务的任何代码。
 */
@Service
@Suppress("LongParameterList")
class PipelineTemplateCompatibilityAdapter(
    private val pipelineTemplateFacadeService: PipelineTemplateFacadeService,
    private val pipelineTemplateInstanceService: PipelineTemplateInstanceService,
    private val pipelineTemplateVersionManager: PipelineTemplateVersionManager,
    private val pipelineTemplateResourceService: PipelineTemplateResourceService,
    private val pipelineTemplateInfoService: PipelineTemplateInfoService,
    private val pipelineTemplatePermissionService: PipelineTemplatePermissionService,
    private val pipelineTemplateRelatedService: PipelineTemplateRelatedService,
    private val pipelineGroupService: PipelineGroupService,
    private val redisOperation: RedisOperation,
    private val templateCommonService: TemplateCommonService
) {

    /**
     * 获取模板详情，返回 V1 兼容的 [TemplateModelDetail]。
     * 组合 V2 的 getTemplateDetails + getTemplateVersions 构建。
     */
    fun getTemplate(
        projectId: String,
        userId: String,
        templateId: String,
        version: Long?,
        versionName: String?
    ): TemplateModelDetail {
        val templateInfo = pipelineTemplateInfoService.get(
            projectId = projectId,
            templateId = templateId
        )

        // 获取模板详情（V2）
        val resolvedVersion = resolveVersion(
            projectId = projectId,
            templateId = templateId,
            version = version,
            versionName = versionName
        )
        val details = pipelineTemplateFacadeService.getTemplateDetails(
            projectId = projectId,
            templateId = templateId,
            version = resolvedVersion
        )

        // 获取版本列表 & currentVersion / latestVersion
        val versions = buildVersionList(projectId, templateId)
        val resource = details.resource
        val currentVersion = toTemplateVersion(resource)
        val latestResource = pipelineTemplateResourceService.getLatestReleasedResource(
            projectId = projectId, templateId = templateId
        )
        val latestVersion = if (latestResource != null) {
            toTemplateVersion(latestResource)
        } else {
            currentVersion
        }

        // 获取 model & labels
        val model = details.resource.model as Model
        val groups = pipelineGroupService.getGroups(
            userId = userId,
            projectId = projectId,
            pipelineId = templateId
        )
        val labels = ArrayList<String>()
        groups.forEach { labels.addAll(it.labels) }
        model.labels = labels

        // 从 model 解析 params / templateParams
        val triggerContainer = model.getTriggerContainer()
        val params = triggerContainer.params
        val templateParams = triggerContainer.templateParams

        // 权限检查
        val hasPermission = pipelineTemplatePermissionService.checkPipelineTemplatePermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.EDIT,
            templateId = templateId
        )

        return TemplateModelDetail(
            versions = versions,
            currentVersion = currentVersion,
            latestVersion = latestVersion,
            templateName = templateInfo.name,
            description = templateInfo.desc ?: "",
            creator = templateInfo.creator,
            template = model,
            templateType = templateInfo.mode.name,
            logoUrl = templateInfo.logoUrl ?: "",
            hasPermission = hasPermission,
            params = params,
            templateParams = templateParams
        )
    }

    /**
     * 模板管理获取模板列表，返回 V1 兼容的 [TemplateListModel]。
     */
    fun listTemplate(
        userId: String,
        projectId: String,
        templateType: TemplateType?,
        storeFlag: Boolean?,
        orderBy: PTemplateOrderByType?,
        sort: PTemplateSortType?,
        page: Int,
        pageSize: Int
    ): TemplateListModel {
        val commonCondition = PipelineTemplateCommonCondition(
            projectId = projectId,
            mode = templateType,
            storeFlag = storeFlag,
            orderBy = orderBy,
            sort = sort,
            page = page,
            pageSize = pageSize
        )
        val sqlPage = pipelineTemplateFacadeService.listTemplateInfos(
            userId = userId,
            commonCondition = commonCondition
        )

        // 获取权限信息
        val hasPermission = pipelineTemplatePermissionService.checkPipelineTemplatePermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.MANAGE
        )
        val hasCreatePermission = pipelineTemplatePermissionService.checkPipelineTemplatePermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.CREATE
        )
        val enablePermManage = pipelineTemplatePermissionService.enableTemplatePermissionManage(projectId)

        // 批量查询哪些模板有待升级的实例
        val templateIds = sqlPage.records.map { it.id }.toSet()
        val upgradeTemplateIds = pipelineTemplateRelatedService
            .listTemplateIdsWithInstance2Upgrade(
                projectId = projectId,
                templateIds = templateIds
            )

        // 转换 PipelineTemplateListResponse → TemplateModel
        val models = sqlPage.records.map { resp ->
            TemplateModel(
                name = resp.name,
                templateId = resp.id,
                version = resp.releasedVersion,
                versionName = resp.releasedVersionName ?: "",
                templateType = resp.mode.name,
                templateTypeDesc = TemplateType.getTemplateTypeDesc(
                    resp.mode.name
                ),
                logoUrl = resp.logoUrl ?: "",
                storeFlag = resp.storeFlag,
                associateCodes = emptyList(),
                associatePipelines = emptyList(),
                hasInstance2Upgrade =
                    resp.id in upgradeTemplateIds,
                hasPermission = resp.canEdit ?: hasPermission,
                canView = resp.canView,
                canEdit = resp.canEdit,
                canDelete = resp.canDelete,
                creator = resp.creator,
                updateTime = resp.updateTime ?: 0L
            )
        }

        return TemplateListModel(
            projectId = projectId,
            hasPermission = hasPermission,
            models = models,
            count = sqlPage.count.toInt(),
            hasCreatePermission = hasCreatePermission,
            enableTemplatePermissionManage = enablePermManage
        )
    }

    /**
     * 创建流水线时获取全部模板列表，V2 直接透传。
     */
    fun listAllTemplates(
        userId: String,
        projectId: String
    ): OptionalTemplateList {
        return pipelineTemplateFacadeService.listAllTemplates(
            userId = userId,
            projectId = projectId
        )
    }

    /**
     * 创建模板，返回 templateId。
     */
    fun createTemplate(
        userId: String,
        projectId: String,
        template: Model
    ): String {
        checkTemplateMigrateStatus(projectId)
        val templateId = UUIDUtil.generate()
        pipelineTemplatePermissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.CREATE
        )
        val setting = templateCommonService.getDefaultSetting(
            projectId = projectId,
            templateId = templateId,
            templateName = template.name,
            creator = userId
        )
        val request = PipelineTemplateReleaseCreateReq(
            model = template,
            setting = setting
        )
        val result = pipelineTemplateVersionManager.deployTemplate(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            request = request
        )
        return result.templateId
    }

    /**
     * 更新模板，返回最新版本号。
     */
    fun updateTemplate(
        userId: String,
        projectId: String,
        templateId: String,
        versionName: String,
        template: Model
    ): Long {
        checkTemplateMigrateStatus(projectId)
        pipelineTemplatePermissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            permission = AuthPermission.EDIT
        )
        val templateInfo = pipelineTemplateInfoService.get(
            projectId = projectId,
            templateId = templateId
        )
        val request = PipelineTemplateReleaseCreateReq(
            model = template,
            setting = PipelineSetting(
                projectId = projectId,
                pipelineId = templateId,
                pipelineName = templateInfo.name,
                desc = template.desc ?: "",
                pipelineAsCodeSettings = null,
                creator = userId,
                updater = userId
            ),
            versionName = versionName,
            category = templateInfo.category,
            logoUrl = templateInfo.logoUrl
        )
        val result = pipelineTemplateVersionManager.deployTemplate(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            request = request
        )
        return result.version
    }

    /**
     * 删除模板，V2 直接透传。
     */
    fun deleteTemplate(
        userId: String,
        projectId: String,
        templateId: String
    ): Boolean {
        checkTemplateMigrateStatus(projectId)
        pipelineTemplatePermissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            permission = AuthPermission.DELETE
        )
        return pipelineTemplateFacadeService.deleteTemplate(
            userId = userId,
            projectId = projectId,
            templateId = templateId
        )
    }

    /**
     * 删除模板版本，V2 返回 Unit，包装为 Boolean。
     */
    fun deleteVersion(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long
    ): Boolean {
        checkTemplateMigrateStatus(projectId)
        pipelineTemplatePermissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            permission = AuthPermission.DELETE
        )
        pipelineTemplateVersionManager.deleteVersion(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            version = version
        )
        return true
    }

    /**
     * 批量创建模板实例（同步），返回 [TemplateOperationRet]。
     */
    fun createTemplateInstances(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long,
        useTemplateSettings: Boolean,
        instances: List<TemplateInstanceCreate>
    ): TemplateOperationRet {
        val request = buildInstancesRequest(
            projectId = projectId,
            templateId = templateId,
            version = version,
            useTemplateSettings = useTemplateSettings,
            instances = instances.map {
                InstanceAdaptInfo(
                    pipelineId = "",
                    pipelineName = it.pipelineName,
                    buildNo = it.buildNo,
                    param = it.param
                )
            }
        )
        return pipelineTemplateInstanceService.createTemplateInstances(
            projectId = projectId,
            userId = userId,
            templateId = templateId,
            version = version,
            request = request
        )
    }

    /**
     * 批量更新模板实例（同步，按 versionName），返回 [TemplateOperationRet]。
     */
    fun updateTemplateInstances(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long? = null,
        versionName: String? = null,
        useTemplateSettings: Boolean,
        instances: List<TemplateInstanceUpdate>
    ): TemplateOperationRet {
        val version = resolveVersion(
            projectId = projectId,
            templateId = templateId,
            version = version,
            versionName = versionName
        ) ?: pipelineTemplateResourceService.getLatestReleasedResource(
            projectId = projectId,
            templateId = templateId
        )!!.version
        val request = buildInstancesRequest(
            projectId = projectId,
            templateId = templateId,
            version = version,
            useTemplateSettings = useTemplateSettings,
            instances = instances.map {
                InstanceAdaptInfo(
                    pipelineId = it.pipelineId,
                    pipelineName = it.pipelineName,
                    buildNo = it.buildNo,
                    param = it.param,
                    resetBuildNo = it.resetBuildNo
                )
            }
        )
        return pipelineTemplateInstanceService.updateTemplateInstances(
            projectId = projectId,
            userId = userId,
            templateId = templateId,
            version = version,
            request = request
        )
    }

    /**
     * 查询模板实例列表，返回 V1 兼容的 [TemplateInstancePage]。
     */
    fun listTemplateInstances(
        userId: String,
        projectId: String,
        templateId: String,
        page: Int,
        pageSize: Int,
        searchKey: String?,
        sortType: TemplateSortTypeEnum?,
        desc: Boolean?
    ): TemplateInstancePage {
        val sqlPage = pipelineTemplateInstanceService.listTemplateInstances(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            pipelineName = searchKey,
            updater = null,
            status = null,
            templateVersion = null,
            repoHashId = null,
            page = page,
            pageSize = pageSize,
            sortType = sortType,
            sortDesc = desc ?: true
        )

        val latestResource = pipelineTemplateResourceService.getLatestReleasedResource(
            projectId = projectId, templateId = templateId
        )
        val latestVersion = if (latestResource != null) {
            toTemplateVersion(latestResource)
        } else {
            TemplateVersion(
                version = 0,
                versionName = "",
                updateTime = 0,
                createTime = 0,
                creator = ""
            )
        }

        // 转换 PipelineTemplateRelatedResp → TemplatePipeline
        val instances = sqlPage.records.map { resp ->
            TemplatePipeline(
                templateId = resp.templateId,
                versionName = resp.fromTemplateVersionName,
                version = resp.fromTemplateVersion,
                pipelineId = resp.pipelineId,
                pipelineName = resp.pipelineName,
                updateTime = resp.updateTime,
                hasPermission = resp.canEdit,
                status = resp.status
                    ?: TemplatePipelineStatus.UPDATED,
                instanceErrorInfo = resp.instanceErrorInfo
            )
        }

        // 与 V1 保持一致：查询是否有创建模板实例的权限
        val hasCreateTemplateInstancePerm = pipelineTemplatePermissionService.hasCreateTemplateInstancePermission(
            userId = userId,
            projectId = projectId
        )

        return TemplateInstancePage(
            projectId = projectId,
            templateId = templateId,
            instances = instances,
            latestVersion = latestVersion,
            count = sqlPage.count.toInt(),
            page = page,
            pageSize = pageSize,
            hasCreateTemplateInstancePerm = hasCreateTemplateInstancePerm
        )
    }

    // ====================== 私有辅助方法 ======================

    /**
     * 将 [PipelineTemplateResource] 转换为 [TemplateVersion]。
     */
    private fun toTemplateVersion(resource: PipelineTemplateResource): TemplateVersion {
        return TemplateVersion(
            version = resource.version,
            versionName = resource.versionName ?: "",
            updateTime = resource.updateTime ?: 0L,
            createTime = resource.createdTime ?: 0L,
            creator = resource.creator
        )
    }

    /**
     * 构建 getTemplate 的版本列表。
     */
    private fun buildVersionList(
        projectId: String,
        templateId: String
    ): List<TemplateVersion> {
        val page = pipelineTemplateFacadeService.getTemplateVersions(
            projectId = projectId,
            templateId = templateId,
            commonCondition = PipelineTemplateResourceCommonCondition(
                projectId = projectId,
                templateId = templateId,
                status = VersionStatus.RELEASED
            )
        )
        return page.records.map { record ->
            TemplateVersion(
                version = record.version.toLong(),
                versionName = record.versionName,
                createTime = record.createTime,
                updateTime = record.updateTime ?: record.createTime,
                creator = record.creator
            )
        }
    }

    /**
     * 根据 version 或 versionName 解析出实际的版本号。
     */
    private fun resolveVersion(
        projectId: String,
        templateId: String,
        version: Long?,
        versionName: String?
    ): Long? {
        if (version != null) return version
        if (!versionName.isNullOrBlank()) {
            val condition = PipelineTemplateResourceCommonCondition(
                projectId = projectId,
                templateId = templateId,
                version = version,
                versionName = versionName,
                status = VersionStatus.RELEASED
            )
            val resource = pipelineTemplateResourceService.get(condition)
            return resource.version
        }
        return null
    }

    /**
     * 构建 V2 实例化请求体，自动补充 overrideTemplateField。
     */
    private fun buildInstancesRequest(
        projectId: String,
        templateId: String,
        version: Long,
        useTemplateSettings: Boolean,
        instances: List<InstanceAdaptInfo>
    ): PipelineTemplateInstancesRequest {
        val templateResource = pipelineTemplateResourceService.get(
            projectId = projectId,
            templateId = templateId,
            version = version
        )
        val model = templateResource.model as Model
        val defaultOverrideField = TemplateInstanceField.initFromTemplate(model)

        val instanceReleaseInfos = instances.map { inst ->
            PipelineTemplateInstanceReleaseInfo(
                pipelineId = inst.pipelineId,
                pipelineName = inst.pipelineName,
                buildNo = inst.buildNo,
                param = inst.param,
                triggerConfigs = null,
                overrideTemplateField = defaultOverrideField,
                resetBuildNo = inst.resetBuildNo
            )
        }

        return PipelineTemplateInstancesRequest(
            templateRefType = TemplateRefType.ID,
            templateRef = null,
            useTemplateSettings = useTemplateSettings,
            enablePac = false,
            description = null,
            targetAction = null,
            repoHashId = null,
            targetBranch = null,
            instanceReleaseInfos = instanceReleaseInfos
        )
    }

    /**
     * 实例化适配信息，统一 create 和 update 的差异。
     */
    private data class InstanceAdaptInfo(
        val pipelineId: String,
        val pipelineName: String,
        val buildNo: BuildNo?,
        val param: List<BuildFormProperty>? = null,
        val resetBuildNo: Boolean? = null
    )

    private fun checkTemplateMigrateStatus(projectId: String) {
        if (redisOperation.isMember(TEMPLATE_MIGRATE_REDIS_KEY, projectId)) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_TEMPLATE_MIGRATING
            )
        }
    }

    companion object {
        private const val TEMPLATE_MIGRATE_REDIS_KEY = "pipeline:template:migrate"
    }
}

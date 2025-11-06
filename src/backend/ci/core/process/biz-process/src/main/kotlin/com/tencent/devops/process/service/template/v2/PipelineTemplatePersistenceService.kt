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

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.BranchVersionAction
import com.tencent.devops.common.pipeline.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.process.constant.PipelineTemplateConstant
import com.tencent.devops.process.dao.yaml.PipelineYamlInfoDao
import com.tencent.devops.process.dao.yaml.PipelineYamlVersionDao
import com.tencent.devops.process.engine.dao.template.TemplatePipelineDao
import com.tencent.devops.process.enums.OperationLogType
import com.tencent.devops.process.permission.template.PipelineTemplatePermissionService
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.pojo.template.v2.PTemplateResourceOnlyVersion
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateCommonCondition
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInfoUpdateInfo
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateRelatedCommonCondition
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResource
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResourceCommonCondition
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResourceUpdateInfo
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateSettingCommonCondition
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateSettingUpdateInfo
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupReferDTO
import com.tencent.devops.process.service.template.v2.version.PipelineTemplateVersionCreateContext
import com.tencent.devops.process.service.template.v2.version.processor.PTemplateVersionCreatePostProcessor
import com.tencent.devops.process.service.`var`.PublicVarGroupReferInfoService
import com.tencent.devops.store.api.common.ServiceStoreResource
import com.tencent.devops.store.api.template.ServiceTemplateResource
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 负责流水线模版持久化业务逻辑
 */
@Service
class PipelineTemplatePersistenceService @Autowired constructor(
    private val pipelineTemplateInfoService: PipelineTemplateInfoService,
    private val pipelineTemplateResourceService: PipelineTemplateResourceService,
    private val pipelineTemplateSettingService: PipelineTemplateSettingService,
    private val pipelineTemplatePermissionService: PipelineTemplatePermissionService,
    private val dslContext: DSLContext,
    private val pipelineTemplateRelatedService: PipelineTemplateRelatedService,
    private val client: Client,
    private val templatePipelineDao: TemplatePipelineDao,
    private val versionCreatePostProcessors: List<PTemplateVersionCreatePostProcessor>,
    private val pipelineYamlInfoDao: PipelineYamlInfoDao,
    private val pipelineYamlVersionDao: PipelineYamlVersionDao,
    private val publicVarGroupReferInfoService: PublicVarGroupReferInfoService
) {

    /**
     * 初始化创建流水线模版和权限（草稿/分支/正式）
     */
    fun initializeTemplate(
        context: PipelineTemplateVersionCreateContext,
        resourceOnlyVersion: PTemplateResourceOnlyVersion
    ) {
        with(context) {
            val versionStatus = pTemplateResourceWithoutVersion.status
            val branchAction = versionStatus.takeIf {
                it == VersionStatus.BRANCH
            }?.let { BranchVersionAction.ACTIVE }
            // 正式版本和分支版本需要有发布时间
            val releaseTime = versionStatus.takeIf {
                it == VersionStatus.RELEASED || it == VersionStatus.BRANCH
            }?.let { LocalDateTime.now().timestampmilli() }

            if (pTemplateResourceWithoutVersion.model is Model) {
                (pTemplateResourceWithoutVersion.model as Model).latestVersion = resourceOnlyVersion.version.toInt()
            }
            val pipelineTemplateResource = PipelineTemplateResource(
                pTemplateResourceWithoutVersion = pTemplateResourceWithoutVersion,
                pTemplateResourceOnlyVersion = resourceOnlyVersion
            ).copy(
                branchAction = branchAction,
                releaseTime = releaseTime
            )
            val pipelineTemplateSetting = pTemplateSettingWithoutVersion.copy(
                version = resourceOnlyVersion.settingVersion
            )
            val newPipelineTemplateInfo = pipelineTemplateInfo.copy(
                releasedVersion = resourceOnlyVersion.version,
                releasedVersionName = resourceOnlyVersion.versionName,
                releasedSettingVersion = resourceOnlyVersion.settingVersion,
                latestVersionStatus = pTemplateResourceWithoutVersion.status
            )
            operationLogType = OperationLogType.fetchType(pipelineTemplateResource.status)
            operationLogParams = resourceOnlyVersion.versionName ?: ""

            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                pipelineTemplateInfoService.createOrUpdate(
                    transactionContext = transactionContext,
                    pipelineTemplateInfo = newPipelineTemplateInfo
                )
                pipelineTemplateResourceService.create(
                    transactionContext = transactionContext,
                    pipelineTemplateResource = pipelineTemplateResource
                )
                pipelineTemplateSettingService.createOrUpdate(
                    transactionContext = transactionContext,
                    pipelineTemplateSetting = pipelineTemplateSetting
                )
                pipelineTemplatePermissionService.createResource(
                    userId = pipelineTemplateInfo.creator,
                    projectId = pipelineTemplateInfo.projectId,
                    templateId = pipelineTemplateInfo.id,
                    templateName = pipelineTemplateInfo.name
                )
                postProcessInTransactionVersionCreate(
                    transactionContext = transactionContext,
                    context = context,
                    pipelineTemplateResource = pipelineTemplateResource,
                    pipelineTemplateSetting = pipelineTemplateSetting
                )
            }
            postProcessAfterVersionCreate(
                context = context,
                pipelineTemplateResource = pipelineTemplateResource,
                pipelineTemplateSetting = pipelineTemplateSetting
            )
        }
    }

    fun createReleaseVersion(
        context: PipelineTemplateVersionCreateContext,
        resourceOnlyVersion: PTemplateResourceOnlyVersion
    ) {
        with(context) {
            if (pTemplateResourceWithoutVersion.model is Model) {
                (pTemplateResourceWithoutVersion.model as Model).latestVersion = resourceOnlyVersion.version.toInt()
            }
            val pipelineTemplateResource = PipelineTemplateResource(
                pTemplateResourceWithoutVersion = pTemplateResourceWithoutVersion,
                pTemplateResourceOnlyVersion = resourceOnlyVersion
            ).copy(
                releaseTime = LocalDateTime.now().timestampmilli()
            )
            val pipelineTemplateSetting = pTemplateSettingWithoutVersion.copy(
                version = resourceOnlyVersion.settingVersion
            )
            operationLogType = OperationLogType.RELEASE_MASTER_VERSION
            operationLogParams = resourceOnlyVersion.versionName!!

            val pipelineTemplateInfoUpdateInfo = PipelineTemplateInfoUpdateInfo(
                name = pipelineTemplateSetting.pipelineName,
                desc = pipelineTemplateSetting.desc,
                releasedVersion = resourceOnlyVersion.version,
                releasedVersionName = resourceOnlyVersion.versionName,
                releasedSettingVersion = resourceOnlyVersion.settingVersion,
                latestVersionStatus = VersionStatus.RELEASED,
                updater = userId
            )
            val pipelineTemplateCommonCondition = PipelineTemplateCommonCondition(
                projectId = projectId,
                templateId = templateId
            )
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                pipelineTemplateInfoService.update(
                    transactionContext = transactionContext,
                    record = pipelineTemplateInfoUpdateInfo,
                    commonCondition = pipelineTemplateCommonCondition
                )
                pipelineTemplateResourceService.create(
                    transactionContext = transactionContext,
                    pipelineTemplateResource = pipelineTemplateResource
                )
                pipelineTemplateSettingService.createOrUpdate(
                    transactionContext = transactionContext,
                    pipelineTemplateSetting = pipelineTemplateSetting
                )
                pipelineTemplatePermissionService.modifyResource(
                    userId = userId,
                    projectId = projectId,
                    templateId = templateId,
                    templateName = pipelineTemplateSetting.pipelineName
                )
                postProcessInTransactionVersionCreate(
                    transactionContext = transactionContext,
                    context = context,
                    pipelineTemplateResource = pipelineTemplateResource,
                    pipelineTemplateSetting = pipelineTemplateSetting
                )
            }
            postProcessAfterVersionCreate(
                context = context,
                pipelineTemplateResource = pipelineTemplateResource,
                pipelineTemplateSetting = pipelineTemplateSetting
            )
        }
    }

    /**
     * 创建正式版本
     */
    fun createReleaseVersion(
        userId: String,
        templateResource: PipelineTemplateResource,
        templateSetting: PipelineSetting,
        syncPermission: Boolean? = true
    ) {
        val pipelineTemplateInfoUpdateInfo = PipelineTemplateInfoUpdateInfo(
            name = templateSetting.pipelineName,
            desc = templateSetting.desc,
            releasedVersion = templateResource.version,
            releasedVersionName = templateResource.versionName,
            releasedSettingVersion = templateResource.settingVersion,
            latestVersionStatus = VersionStatus.RELEASED,
            updater = userId
        )
        val pipelineTemplateCommonCondition = PipelineTemplateCommonCondition(
            projectId = templateResource.projectId,
            templateId = templateResource.templateId
        )
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            pipelineTemplateInfoService.update(
                transactionContext = context,
                record = pipelineTemplateInfoUpdateInfo,
                commonCondition = pipelineTemplateCommonCondition
            )
            pipelineTemplateResourceService.create(
                transactionContext = context,
                pipelineTemplateResource = templateResource
            )
            pipelineTemplateSettingService.createOrUpdate(
                transactionContext = context,
                pipelineTemplateSetting = templateSetting
            )
            if (syncPermission == true) {
                pipelineTemplatePermissionService.modifyResource(
                    userId = userId,
                    projectId = templateResource.projectId,
                    templateId = templateResource.templateId,
                    templateName = templateSetting.pipelineName
                )
            }
        }
    }

    fun createDraftVersion(
        context: PipelineTemplateVersionCreateContext,
        resourceOnlyVersion: PTemplateResourceOnlyVersion
    ) {
        with(context) {
            if (pTemplateResourceWithoutVersion.model is Model) {
                (pTemplateResourceWithoutVersion.model as Model).latestVersion = resourceOnlyVersion.version.toInt()
            }
            val pipelineTemplateResource = PipelineTemplateResource(
                pTemplateResourceWithoutVersion = pTemplateResourceWithoutVersion,
                pTemplateResourceOnlyVersion = resourceOnlyVersion
            )
            val pipelineTemplateSetting = pTemplateSettingWithoutVersion.copy(
                version = resourceOnlyVersion.settingVersion
            )
            operationLogType = OperationLogType.CREATE_DRAFT_VERSION
            operationLogParams = resourceOnlyVersion.baseVersionName ?: ""

            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                pipelineTemplateResourceService.create(
                    transactionContext = transactionContext,
                    pipelineTemplateResource = pipelineTemplateResource
                )
                pipelineTemplateSettingService.createOrUpdate(
                    transactionContext = transactionContext,
                    pipelineTemplateSetting = pipelineTemplateSetting
                )
                postProcessInTransactionVersionCreate(
                    transactionContext = transactionContext,
                    context = context,
                    pipelineTemplateResource = pipelineTemplateResource,
                    pipelineTemplateSetting = pipelineTemplateSetting
                )
            }
            postProcessAfterVersionCreate(
                context = context,
                pipelineTemplateResource = pipelineTemplateResource,
                pipelineTemplateSetting = pipelineTemplateSetting
            )

            (pTemplateResourceWithoutVersion.model as? Model)?.let {
                publicVarGroupReferInfoService.handleVarGroupReferBus(
                    PublicVarGroupReferDTO(
                        userId = userId,
                        projectId = projectId,
                        model = it,
                        referId = templateId,
                        referType = PublicVerGroupReferenceTypeEnum.TEMPLATE,
                        referName = pipelineTemplateInfo.name,
                        referVersion = pipelineTemplateResource.version.toInt(),
                        referVersionName = pipelineTemplateResource.versionName ?: ""
                    )
                )
            }
        }
    }

    fun createBranchVersion(
        context: PipelineTemplateVersionCreateContext,
        resourceOnlyVersion: PTemplateResourceOnlyVersion
    ) {
        with(context) {
            if (pTemplateResourceWithoutVersion.model is Model) {
                (pTemplateResourceWithoutVersion.model as Model).latestVersion = resourceOnlyVersion.version.toInt()
            }
            val pipelineTemplateResource = PipelineTemplateResource(
                pTemplateResourceWithoutVersion = pTemplateResourceWithoutVersion,
                pTemplateResourceOnlyVersion = resourceOnlyVersion
            ).copy(
                releaseTime = LocalDateTime.now().timestampmilli()
            )
            val pipelineTemplateSetting = pTemplateSettingWithoutVersion.copy(
                version = resourceOnlyVersion.settingVersion
            )
            val inactiveBranchUpdateInfo = PipelineTemplateResourceUpdateInfo(
                branchAction = BranchVersionAction.INACTIVE
            )
            val inactiveBranchCondition = PipelineTemplateResourceCommonCondition(
                projectId = projectId,
                templateId = templateId,
                versionName = pipelineTemplateResource.versionName,
                branchAction = BranchVersionAction.ACTIVE
            )
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                // 创建分支版本,需要把原来的活跃的分支置为非活跃
                val cnt = pipelineTemplateResourceService.update(
                    transactionContext = transactionContext,
                    record = inactiveBranchUpdateInfo,
                    commonCondition = inactiveBranchCondition
                )
                if (cnt > 0) {
                    operationLogType = OperationLogType.UPDATE_BRANCH_VERSION
                    operationLogParams = resourceOnlyVersion.versionName ?: ""
                } else {
                    operationLogType = OperationLogType.CREATE_BRANCH_VERSION
                    operationLogParams = resourceOnlyVersion.versionName ?: ""
                }
                pipelineTemplateResourceService.create(
                    transactionContext = transactionContext,
                    pipelineTemplateResource = pipelineTemplateResource.copy(
                        branchAction = BranchVersionAction.ACTIVE
                    )
                )
                pipelineTemplateSettingService.createOrUpdate(
                    transactionContext = transactionContext,
                    pipelineTemplateSetting = pipelineTemplateSetting
                )
                postProcessInTransactionVersionCreate(
                    transactionContext = transactionContext,
                    context = context,
                    pipelineTemplateResource = pipelineTemplateResource,
                    pipelineTemplateSetting = pipelineTemplateSetting
                )
            }
            postProcessAfterVersionCreate(
                context = context,
                pipelineTemplateResource = pipelineTemplateResource,
                pipelineTemplateSetting = pipelineTemplateSetting
            )
        }
    }

    fun updateDraftVersion(
        context: PipelineTemplateVersionCreateContext,
        resourceOnlyVersion: PTemplateResourceOnlyVersion
    ) {
        with(context) {
            operationLogType = OperationLogType.UPDATE_DRAFT_VERSION
            if (pTemplateResourceWithoutVersion.model is Model) {
                (pTemplateResourceWithoutVersion.model as Model).latestVersion = resourceOnlyVersion.version.toInt()
            }
            val pipelineTemplateResource = PipelineTemplateResource(
                pTemplateResourceWithoutVersion = pTemplateResourceWithoutVersion,
                pTemplateResourceOnlyVersion = resourceOnlyVersion
            )
            val pipelineTemplateSetting = pTemplateSettingWithoutVersion.copy(
                version = resourceOnlyVersion.settingVersion
            )
            val templateResourceUpdateInfo = PipelineTemplateResourceUpdateInfo(
                params = pipelineTemplateResource.params,
                model = pipelineTemplateResource.model,
                yaml = pipelineTemplateResource.yaml,
                baseVersion = pipelineTemplateResource.baseVersion,
                baseVersionName = pipelineTemplateResource.baseVersionName,
                updater = userId,
                sortWeight = PipelineTemplateConstant.COMMITTING_STATUS_VERSION_SORT_WIGHT
            )
            val templateResourceCondition = PipelineTemplateResourceCommonCondition(
                projectId = projectId,
                templateId = templateId,
                version = resourceOnlyVersion.version
            )
            val templateSettingUpdateInfo = PipelineTemplateSettingUpdateInfo(
                userId = userId,
                pipelineSetting = pipelineTemplateSetting
            )
            val templateSettingCondition = PipelineTemplateSettingCommonCondition(
                projectId = projectId,
                templateId = templateId,
                settingVersion = resourceOnlyVersion.settingVersion
            )
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                pipelineTemplateResourceService.update(
                    transactionContext = transactionContext,
                    record = templateResourceUpdateInfo,
                    commonCondition = templateResourceCondition
                )
                pipelineTemplateSettingService.update(
                    transactionContext = transactionContext,
                    record = templateSettingUpdateInfo,
                    commonCondition = templateSettingCondition
                )
                postProcessInTransactionVersionCreate(
                    transactionContext = transactionContext,
                    context = context,
                    pipelineTemplateResource = pipelineTemplateResource,
                    pipelineTemplateSetting = pipelineTemplateSetting
                )
            }
            postProcessAfterVersionCreate(
                context = context,
                pipelineTemplateResource = pipelineTemplateResource,
                pipelineTemplateSetting = pipelineTemplateSetting
            )
        }
    }

    /**
     * 将草稿版本发布为正式版本
     */
    fun releaseDraft2ReleaseVersion(
        context: PipelineTemplateVersionCreateContext,
        resourceOnlyVersion: PTemplateResourceOnlyVersion
    ) {
        with(context) {
            operationLogType = OperationLogType.RELEASE_MASTER_VERSION
            operationLogParams = resourceOnlyVersion.versionName!!

            val pipelineTemplateResource = PipelineTemplateResource(
                pTemplateResourceWithoutVersion = pTemplateResourceWithoutVersion,
                pTemplateResourceOnlyVersion = resourceOnlyVersion
            )
            val pipelineTemplateSetting = pTemplateSettingWithoutVersion.copy(
                version = resourceOnlyVersion.settingVersion
            )
            val templateInfoUpdateInfo = PipelineTemplateInfoUpdateInfo(
                name = pipelineTemplateSetting.pipelineName,
                desc = pipelineTemplateSetting.desc,
                releasedVersion = resourceOnlyVersion.version,
                releasedVersionName = resourceOnlyVersion.versionName,
                releasedSettingVersion = resourceOnlyVersion.settingVersion,
                latestVersionStatus = VersionStatus.RELEASED,
                enablePac = enablePac,
                updater = userId
            )
            val pipelineTemplateCommonCondition = PipelineTemplateCommonCondition(
                projectId = projectId,
                templateId = templateId
            )
            val templateResourceUpdateInfo = PipelineTemplateResourceUpdateInfo(
                versionName = resourceOnlyVersion.versionName,
                settingVersionNum = resourceOnlyVersion.settingVersionNum,
                versionNum = resourceOnlyVersion.versionNum,
                pipelineVersion = resourceOnlyVersion.pipelineVersion,
                triggerVersion = resourceOnlyVersion.triggerVersion,
                releaseTime = LocalDateTime.now(),
                status = VersionStatus.RELEASED,
                description = pipelineTemplateResource.description,
                sortWeight = PipelineTemplateConstant.OTHER_STATUS_VERSION_SORT_WIGHT,
                updater = userId
            )
            val templateResourceCondition = PipelineTemplateResourceCommonCondition(
                projectId = projectId,
                templateId = templateId,
                version = resourceOnlyVersion.version
            )
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                pipelineTemplateInfoService.update(
                    transactionContext = transactionContext,
                    record = templateInfoUpdateInfo,
                    commonCondition = pipelineTemplateCommonCondition
                )
                pipelineTemplateResourceService.update(
                    transactionContext = transactionContext,
                    record = templateResourceUpdateInfo,
                    commonCondition = templateResourceCondition
                )
                pipelineTemplatePermissionService.modifyResource(
                    userId = userId,
                    projectId = projectId,
                    templateId = templateId,
                    templateName = pipelineTemplateSetting.pipelineName
                )
                postProcessInTransactionVersionCreate(
                    transactionContext = transactionContext,
                    context = context,
                    pipelineTemplateResource = pipelineTemplateResource,
                    pipelineTemplateSetting = pipelineTemplateSetting
                )
            }
            postProcessAfterVersionCreate(
                context = context,
                pipelineTemplateResource = pipelineTemplateResource,
                pipelineTemplateSetting = pipelineTemplateSetting
            )
        }
    }

    fun releaseDraft2BranchVersion(
        context: PipelineTemplateVersionCreateContext,
        resourceOnlyVersion: PTemplateResourceOnlyVersion
    ) {
        with(context) {
            val pipelineTemplateResource = PipelineTemplateResource(
                pTemplateResourceWithoutVersion = pTemplateResourceWithoutVersion,
                pTemplateResourceOnlyVersion = resourceOnlyVersion
            )
            val pipelineTemplateSetting = pTemplateSettingWithoutVersion.copy(
                version = resourceOnlyVersion.settingVersion
            )
            val pipelineTemplateInfoUpdateInfo = PipelineTemplateInfoUpdateInfo(
                enablePac = true,
                updater = userId
            )
            val pipelineTemplateCommonCondition = PipelineTemplateCommonCondition(
                projectId = projectId,
                templateId = templateId
            )
            val inactiveBranchUpdateInfo = PipelineTemplateResourceUpdateInfo(
                branchAction = BranchVersionAction.INACTIVE
            )
            val inactiveBranchCondition = PipelineTemplateResourceCommonCondition(
                projectId = projectId,
                templateId = templateId,
                versionName = resourceOnlyVersion.versionName,
                branchAction = BranchVersionAction.ACTIVE
            )
            val templateResourceUpdateInfo = PipelineTemplateResourceUpdateInfo(
                versionName = resourceOnlyVersion.versionName,
                status = VersionStatus.BRANCH,
                branchAction = BranchVersionAction.ACTIVE,
                description = pipelineTemplateResource.description,
                updater = userId,
                sortWeight = PipelineTemplateConstant.OTHER_STATUS_VERSION_SORT_WIGHT,
                releaseTime = LocalDateTime.now()
            )
            val templateResourceCondition = PipelineTemplateResourceCommonCondition(
                projectId = projectId,
                templateId = templateId,
                version = version
            )
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                if (pipelineTemplateInfo.enablePac != enablePac) {
                    pipelineTemplateInfoService.update(
                        transactionContext = transactionContext,
                        record = pipelineTemplateInfoUpdateInfo,
                        commonCondition = pipelineTemplateCommonCondition
                    )
                }
                // 创建分支版本,需要把原来的活跃的分支置为非活跃
                val cnt = pipelineTemplateResourceService.update(
                    transactionContext = transactionContext,
                    record = inactiveBranchUpdateInfo,
                    commonCondition = inactiveBranchCondition
                )
                if (cnt > 0) {
                    operationLogType = OperationLogType.UPDATE_BRANCH_VERSION
                    operationLogParams = resourceOnlyVersion.versionName ?: ""
                } else {
                    operationLogType = OperationLogType.CREATE_BRANCH_VERSION
                    operationLogParams = resourceOnlyVersion.versionName ?: ""
                }
                pipelineTemplateResourceService.update(
                    transactionContext = transactionContext,
                    record = templateResourceUpdateInfo,
                    commonCondition = templateResourceCondition
                )
                postProcessInTransactionVersionCreate(
                    transactionContext = transactionContext,
                    context = context,
                    pipelineTemplateResource = pipelineTemplateResource,
                    pipelineTemplateSetting = pipelineTemplateSetting
                )
            }
            postProcessAfterVersionCreate(
                context = context,
                pipelineTemplateResource = pipelineTemplateResource,
                pipelineTemplateSetting = pipelineTemplateSetting
            )
        }
    }

    fun deleteVersion(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long
    ) {
        val updateInfo = PipelineTemplateResourceUpdateInfo(
            status = VersionStatus.DELETE
        )
        val condition = PipelineTemplateResourceCommonCondition(
            projectId = projectId,
            templateId = templateId,
            version = version
        )
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            pipelineTemplateResourceService.update(
                transactionContext = transactionContext,
                record = updateInfo,
                commonCondition = condition
            )
            templatePipelineDao.deleteByVersion(
                dslContext = transactionContext,
                projectId = projectId,
                templateId = templateId,
                version = version
            )
            client.get(ServiceTemplateResource::class).deleteMarketPublishedVersions(
                templateCode = templateId,
                versions = listOf(version)
            )
        }
    }

    fun deleteTemplateAllVersions(
        projectId: String,
        templateId: String
    ) {
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val templateInfo = pipelineTemplateInfoService.get(templateId)
            pipelineTemplateRelatedService.delete(
                transactionContext = context,
                condition = PipelineTemplateRelatedCommonCondition(
                    projectId = projectId,
                    templateId = templateId
                )
            )
            pipelineTemplateInfoService.delete(
                transactionContext = context,
                commonCondition = PipelineTemplateCommonCondition(
                    projectId = projectId,
                    templateId = templateId
                )
            )
            pipelineTemplateResourceService.delete(
                transactionContext = context,
                commonCondition = PipelineTemplateResourceCommonCondition(
                    projectId = projectId,
                    templateId = templateId,
                    includeDeleted = true
                )
            )
            pipelineTemplateSettingService.delete(
                transactionContext = context,
                commonCondition = PipelineTemplateSettingCommonCondition(
                    projectId = projectId,
                    templateId = templateId
                )
            )
            pipelineYamlInfoDao.deleteByPipelineId(context, projectId, templateId)
            pipelineYamlVersionDao.deleteByPipelineId(context, projectId, templateId)
            if (templateInfo.mode == TemplateType.CONSTRAINT) {
                client.get(ServiceStoreResource::class).uninstall(
                    storeCode = templateInfo.srcTemplateId!!,
                    storeType = StoreTypeEnum.TEMPLATE,
                    projectCode = templateInfo.projectId
                )
                client.get(ServiceTemplateResource::class).deleteTemplateInstallHistory(templateId)
            } else {
                client.get(ServiceTemplateResource::class).deleteMarketPublishedHistory(templateId)
            }
            pipelineTemplatePermissionService.deleteResource(
                projectId = projectId,
                templateId = templateId
            )
        }
    }

    fun inactiveBranchVersion(
        projectId: String,
        templateId: String,
        branch: String
    ) {
        val inactiveBranchUpdateInfo = PipelineTemplateResourceUpdateInfo(
            branchAction = BranchVersionAction.INACTIVE
        )
        val inactiveBranchCondition = PipelineTemplateResourceCommonCondition(
            projectId = projectId,
            templateId = templateId,
            versionName = branch,
            branchAction = BranchVersionAction.ACTIVE
        )
        pipelineTemplateResourceService.update(
            record = inactiveBranchUpdateInfo,
            commonCondition = inactiveBranchCondition
        )
    }

    fun transformTemplateToCustom(
        userId: String,
        projectId: String,
        templateId: String
    ) {
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val templateInfo = pipelineTemplateInfoService.get(
                projectId = projectId,
                templateId = templateId
            )
            val newTemplateInfo = templateInfo.copy(
                mode = TemplateType.CUSTOMIZE,
                srcTemplateId = null,
                srcTemplateProjectId = null,
                upgradeStrategy = null,
                settingSyncStrategy = null,
                updater = userId,
                updateTime = LocalDateTime.now().timestampmilli()
            )
            pipelineTemplateInfoService.delete(
                transactionContext = context,
                commonCondition = PipelineTemplateCommonCondition(
                    projectId = projectId,
                    templateId = templateId
                )
            )
            pipelineTemplateInfoService.createOrUpdate(
                transactionContext = context,
                pipelineTemplateInfo = newTemplateInfo
            )
            pipelineTemplateResourceService.transformTemplateToCustom(
                transactionContext = context,
                projectId = projectId,
                templateId = templateId
            )
            client.get(ServiceStoreResource::class).uninstall(
                storeCode = templateInfo.srcTemplateId!!,
                storeType = StoreTypeEnum.TEMPLATE,
                projectCode = templateInfo.projectId
            )
        }
    }

    private fun postProcessBeforeVersionCreate(
        context: PipelineTemplateVersionCreateContext,
        pipelineTemplateResource: PipelineTemplateResource,
        pipelineTemplateSetting: PipelineSetting
    ) {
        versionCreatePostProcessors.forEach {
            it.postProcessBeforeVersionCreate(
                context = context,
                pipelineTemplateResource = pipelineTemplateResource,
                pipelineTemplateSetting = pipelineTemplateSetting
            )
        }
    }

    private fun postProcessInTransactionVersionCreate(
        transactionContext: DSLContext,
        context: PipelineTemplateVersionCreateContext,
        pipelineTemplateResource: PipelineTemplateResource,
        pipelineTemplateSetting: PipelineSetting
    ) {
        versionCreatePostProcessors.forEach {
            it.postProcessInTransactionVersionCreate(
                transactionContext = transactionContext,
                context = context,
                pipelineTemplateResource = pipelineTemplateResource,
                pipelineTemplateSetting = pipelineTemplateSetting
            )
        }
    }

    private fun postProcessAfterVersionCreate(
        context: PipelineTemplateVersionCreateContext,
        pipelineTemplateResource: PipelineTemplateResource,
        pipelineTemplateSetting: PipelineSetting
    ) {
        versionCreatePostProcessors.forEach {
            it.postProcessAfterVersionCreate(
                context = context,
                pipelineTemplateResource = pipelineTemplateResource,
                pipelineTemplateSetting = pipelineTemplateSetting
            )
        }
    }
}

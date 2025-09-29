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
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.common.pipeline.enums.PipelineStorageType
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.template.MigrationStatus
import com.tencent.devops.common.pipeline.template.PipelineTemplateType
import com.tencent.devops.common.pipeline.template.UpgradeStrategyEnum
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.process.tables.records.TTemplateRecord
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.dao.template.PipelineTemplateMigrationDao
import com.tencent.devops.process.engine.dao.PipelineResourceDao
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.engine.dao.template.TemplatePipelineDao
import com.tencent.devops.common.pipeline.enums.TemplateRefType
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.pojo.template.TemplateVersion
import com.tencent.devops.process.pojo.template.v2.PTemplateModelTransferResult
import com.tencent.devops.process.pojo.template.v2.PTemplatePipelineVersion
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInfoV2
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateRelated
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateRelatedCommonCondition
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResource
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResourceCommonCondition
import com.tencent.devops.process.service.template.TemplateFacadeService
import com.tencent.devops.process.utils.PipelineVersionUtils
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.store.api.template.ServiceTemplateResource
import com.tencent.devops.store.pojo.template.TemplatePublishedVersionInfo
import com.tencent.devops.store.pojo.template.TemplateVersionInstallHistoryInfo
import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.Executors

@Service
class PipelineTemplateMigrateService(
    val templateDao: TemplateDao,
    val dslContext: DSLContext,
    val templateFacadeService: TemplateFacadeService,
    val pipelineTemplatePersistenceService: PipelineTemplatePersistenceService,
    val pipelineSettingDao: PipelineSettingDao,
    val pipelineTemplateGenerator: PipelineTemplateGenerator,
    val pipelineTemplateResourceService: PipelineTemplateResourceService,
    val pipelineTemplateSettingService: PipelineTemplateSettingService,
    val pipelineTemplateInfoService: PipelineTemplateInfoService,
    val templatePipelineDao: TemplatePipelineDao,
    val redisOperation: RedisOperation,
    val client: Client,
    val pipelineTemplateMigrationDao: PipelineTemplateMigrationDao,
    val pipelineResourceDao: PipelineResourceDao,
    val pipelineTemplatePipelineVersionService: PipelineTemplatePipelineVersionService,
    val pipelineTemplateRelatedService: PipelineTemplateRelatedService
) {
    fun migrateTemplatesByCondition(projectConditionDTO: ProjectConditionDTO) {
        logger.info("start to migrate Templates by condition|$projectConditionDTO")
        val traceId = MDC.get(TraceTag.BIZID)
        var offset = 0
        val limit = PageUtil.MAX_PAGE_SIZE / 2
        do {
            val projectCodes = client.get(ServiceProjectResource::class).listProjectsByCondition(
                projectConditionDTO = projectConditionDTO,
                limit = limit,
                offset = offset
            ).data ?: break
            projectCodes.forEach {
                migrateProjectTemplateExecutorService.execute {
                    MDC.put(TraceTag.BIZID, traceId)
                    migrateTemplates(it.englishName)
                }
            }
            offset += limit
        } while (projectCodes.size == limit)
    }

    fun migratePublicTemplates() {
        migrateTemplates("")
    }

    /**
     * 迁移一个项目下的所有模板（协调函数）。
     *
     * 主要职责：
     * 1. 控制迁移任务的生命周期（开始、进行中、结束）。
     * 2. 协调核心迁移、数据清理和结果上报的流程。
     * 3. 通过顶层 try-catch 保证任务状态最终会被更新，避免卡在“进行中”。
     */
    fun migrateTemplates(projectId: String) {
        logger.info("Start to migrate project templates for projectId: {}", projectId)

        // 1. 前置检查：如果任务已在进行中或者不存在模板，则直接跳过
        val migrationRecord = pipelineTemplateMigrationDao.get(dslContext, projectId)
        if (migrationRecord?.status == MigrationStatus.IN_PROGRESS.name) {
            logger.warn("Migration for projectId {} is already in progress. Skipping.", projectId)
            return
        }
        val templateCount = templateDao.countTemplate(dslContext, projectId)
        if (templateCount == 0) {
            logger.warn("The template does not exist under project {}. Skipping.", projectId)
            return
        }

        val startTime = LocalDateTime.now()
        // 2. 标记任务开始
        pipelineTemplateMigrationDao.create(
            dslContext = dslContext,
            projectId = projectId,
            status = MigrationStatus.IN_PROGRESS
        )

        var result: MigrationResult? = null
        var cleanupStats: CleanupStats? = null

        try {
            // 3. 执行核心迁移逻辑
            result = runTemplateMigration(projectId)

            // 4. 清理孤立数据
            cleanupStats = cleanupOrphanedTemplates(projectId, result.allTemplateIds)
        } catch (ex: Exception) {
            // 捕获迁移或清理过程中的任何意外异常，确保能记录失败状态
            logger.error(
                "A critical error occurred during migration for projectId: {}. Error: {}",
                projectId, ex.message, ex
            )
        } finally {
            // 5. 记录最终结果，无论成功、部分失败还是严重失败
            recordFinalMigrationStatus(projectId, startTime, result, cleanupStats)
        }
    }

    /**
     * [辅助函数 1] - 执行实际的模板迁移循环。
     * @return 返回一个包含所有迁移结果的 MigrationResult 对象。
     */
    private fun runTemplateMigration(projectId: String): MigrationResult {
        val allTemplateIds = mutableSetOf<String>()
        val successfulIds = mutableListOf<String>()
        val failedItems = mutableListOf<FailedMigrationItem>()

        var offset = 0
        val limit = PageUtil.MAX_PAGE_SIZE / 2
        do {
            val templateIdsBatch = templateDao.list(
                dslContext = dslContext,
                projectId = projectId,
                limit = limit,
                offset = offset
            )
            logger.info("Processing batch of {} templates for projectId: {}", templateIdsBatch.size, projectId)

            templateIdsBatch.forEach { templateId ->
                try {
                    migrateTemplate(templateId = templateId, projectId = projectId)
                    successfulIds.add(templateId)
                } catch (errorCodeException: ErrorCodeException) {
                    val errorMessage = I18nUtil.getCodeLanMessage(
                        messageCode = errorCodeException.errorCode,
                        params = errorCodeException.params,
                        defaultMessage = errorCodeException.defaultMessage
                    )
                    logger.warn(
                        "Failed to migrate templateId {} in projectId {}: {}",
                        templateId, projectId, errorMessage
                    )
                    failedItems.add(FailedMigrationItem(templateId, errorMessage))
                } catch (ex: Exception) {
                    logger.warn(
                        "Failed to migrate templateId {} in projectId {}: {}",
                        templateId, projectId, ex.message
                    )
                    failedItems.add(FailedMigrationItem(templateId, ex.message))
                }
            }
            allTemplateIds.addAll(templateIdsBatch)
            offset += limit
        } while (templateIdsBatch.size == limit)

        return MigrationResult(allTemplateIds, successfulIds, failedItems)
    }

    /**
     * [辅助函数 2] - 清理在新表中存在但在旧表中不存在的孤立模板。
     * @param v1AllTemplateIds 从旧表中获取的所有模板ID。
     * @return 返回清理前后的数量统计。
     */
    private fun cleanupOrphanedTemplates(projectId: String, v1AllTemplateIds: Set<String>): CleanupStats {
        val v2AllTemplateIds = pipelineTemplateInfoService.listAllIds(projectId)
        val deleteRecords = v2AllTemplateIds.filterNot { it in v1AllTemplateIds }

        if (deleteRecords.isNotEmpty()) {
            logger.warn("Found {} orphaned templates to delete for projectId: {}", deleteRecords.size, projectId)
            deleteRecords.forEach { templateId ->
                try {
                    pipelineTemplatePersistenceService.deleteTemplateAllVersions(
                        projectId = projectId,
                        templateId = templateId
                    )
                } catch (ex: Exception) {
                    logger.error(
                        "Failed to delete orphaned templateId {} for projectId {}: {}",
                        templateId, projectId, ex.message
                    )
                }
            }
        }

        return CleanupStats(v2AllTemplateIds.size, v2AllTemplateIds.size - deleteRecords.size)
    }

    /**
     * [辅助函数 3] - 将最终的迁移结果更新到数据库。
     */
    private fun recordFinalMigrationStatus(
        projectId: String,
        startTime: LocalDateTime,
        result: MigrationResult?,
        cleanupStats: CleanupStats?
    ) {
        val totalTime = LocalDateTime.now().timestampmilli() - startTime.timestampmilli()

        // 如果 result 为 null，说明在核心流程中发生了严重错误
        val isSuccess = result != null && result.failedItems.isEmpty()
        val status = if (isSuccess) MigrationStatus.SUCCESS else MigrationStatus.FAILED

        val errorMessage = result?.failedItems?.takeIf { it.isNotEmpty() }?.let { JsonUtil.toJson(it) }

        val beforeCount = result?.allTemplateIds?.size ?: 0
        val afterCount = cleanupStats?.countAfterCleanup
            ?: pipelineTemplateInfoService.listAllIds(projectId).size // 降级方案

        pipelineTemplateMigrationDao.update(
            dslContext = dslContext,
            projectId = projectId,
            status = status,
            errorMessage = errorMessage,
            totalTime = totalTime,
            beforeTemplateCount = beforeCount,
            afterTemplateCount = afterCount
        )
        logger.info(
            "Migration for projectId {} finished with status: {}. Total time: {}ms. Before: {}, After: {}",
            projectId, status, totalTime, beforeCount, afterCount
        )
    }

    /**
     * 主协调函数，负责迁移单个模板的所有版本。
     * 原始的 migrateTemplate 函数被重构为这个更高阶的协调者。
     */
    fun migrateTemplate(projectId: String, templateId: String) {
        val lock = PipelineTemplateModelLock(redisOperation = redisOperation, templateId = templateId)
        val startEpoch = System.currentTimeMillis()
        try {
            lock.lock()
            logger.info("Migrate template started, projectId={}, templateId={}", projectId, templateId)
            // 步骤 1: 准备所有需要的数据，并创建上下文对象
            val context = setupMigrationContext(projectId, templateId)
            // 步骤 2: 遍历所有旧版本，并逐一迁移
            migrateAllTemplateVersions(context)
            // 步骤 3: 迁移模板和流水线的版本关联关系
            createTemplatePipelineVersion(context)
            // 步骤 4: 迁移完成后，创建最终的 info 记录并清理可能存在的脏数据
            finalizeMigration(context)
            logger.info("Migrate template finished successfully, projectId={}, templateId={}", projectId, templateId)
        } finally {
            lock.unlock()
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms to migrate template, projectId={}, " +
                    "templateId={}", projectId, templateId
            )
        }
    }

    private fun createTemplatePipelineVersion(context: MigrationContext) {
        with(context) {
            relatedPipelines.forEach { relatedPipelineInfo ->
                val pipelineResource = pipelineResourceDao.getReleaseVersionResource(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = relatedPipelineInfo.pipelineId
                ) ?: return@forEach
                val triggerContainer = pipelineResource.model.getTriggerContainer()

                pipelineTemplatePipelineVersionService.createOrUpdate(
                    record = PTemplatePipelineVersion(
                        projectId = projectId,
                        pipelineId = relatedPipelineInfo.pipelineId,
                        pipelineVersion = pipelineResource.version,
                        pipelineVersionName = pipelineResource.versionName ?: "",
                        instanceType = PipelineInstanceTypeEnum.CONSTRAINT,
                        buildNo = triggerContainer.buildNo,
                        params = triggerContainer.params,
                        refType = TemplateRefType.ID,
                        inputTemplateId = templateId,
                        inputTemplateVersionName = relatedPipelineInfo.versionName,
                        templateId = templateId,
                        templateVersion = relatedPipelineInfo.version,
                        templateVersionName = relatedPipelineInfo.versionName,
                        creator = relatedPipelineInfo.creator,
                        updater = relatedPipelineInfo.updater
                    )
                )
            }
        }
    }

    /**
     * [辅助函数 1] - 准备工作：获取所有必要数据并组装成上下文对象。
     */
    private fun setupMigrationContext(projectId: String, templateId: String): MigrationContext {
        val latestTemplate = templateDao.getLatestTemplate(dslContext, projectId, templateId)
        val setting = pipelineSettingDao.getSetting(dslContext, projectId, latestTemplate.id)
            ?: throw ErrorCodeException(errorCode = ProcessMessageCode.PIPELINE_SETTING_NOT_EXISTS)

        val templateVersionInfos = getTemplateVersions(latestTemplate)
        val srcTemplateProjectId = getSrcTemplateProjectId(latestTemplate)

        val marketTemplateStatus = client.get(ServiceTemplateResource::class).getMarketTemplateStatus(templateId).data!!

        val isConstraint = latestTemplate.type == TemplateType.CONSTRAINT.name
        val hasBeenPublished = !isConstraint && marketTemplateStatus != TemplateStatusEnum.NEVER_PUBLISHED &&
            marketTemplateStatus != TemplateStatusEnum.INIT

        val relatedPipelines = pipelineTemplateRelatedService.list(
            condition = PipelineTemplateRelatedCommonCondition(
                projectId = projectId,
                templateId = templateId,
                instanceType = PipelineInstanceTypeEnum.CONSTRAINT,
                deleted = false
            )
        )

        logger.info(
            "Migration context setup complete for templateId={}: isConstraint={}, marketTemplateStatus={}, " +
                "hasBeenPublished={}, versionsToMigrate={}", templateId, isConstraint, marketTemplateStatus,
            hasBeenPublished, templateVersionInfos.size
        )

        return MigrationContext(
            projectId = projectId,
            templateId = templateId,
            latestTemplate = latestTemplate,
            setting = setting,
            templateVersionInfos = templateVersionInfos,
            srcTemplateProjectId = srcTemplateProjectId,
            marketTemplateStatus = marketTemplateStatus,
            hasBeenPublished = hasBeenPublished,
            isConstraint = isConstraint,
            relatedPipelines = relatedPipelines
        )
    }

    /**
     * [辅助函数 2] - 核心执行：遍历所有版本并调用单个版本的迁移逻辑。
     */
    private fun migrateAllTemplateVersions(context: MigrationContext) {
        val versionCounters = VersionCounters(pipelineVersion = 1, triggerVersion = 1)

        context.templateVersionInfos.forEachIndexed { index, versionInfo ->
            migrateSingleVersion(context, versionInfo, versionSequence = index + 1, versionCounters)
        }
    }

    /**
     * [辅助函数 3] - 迁移单个版本（原循环体内的主要逻辑）。
     */
    private fun migrateSingleVersion(
        context: MigrationContext,
        versionInfo: TemplateVersion,
        versionSequence: Int,
        counters: VersionCounters
    ) {
        val currentProjectId = context.srcTemplateProjectId ?: context.projectId
        val currentTemplate = templateDao.getTemplate(dslContext, currentProjectId, versionInfo.version)
            ?: if (context.isConstraint) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PARENTS_TEMPLATE_NOT_EXISTS,
                    params = arrayOf(context.latestTemplate.srcTemplateId)
                )
            } else {
                throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_TEMPLATE_NOT_EXISTS)
            }

        // 步骤 3.1: 计算新版本的 pipelineVersion 和 triggerVersion
        calculateNextVersions(context, currentTemplate, currentProjectId, versionSequence, counters)

        // 步骤 3.2: 执行模型转换
        val modelTransferResult = performModelTransfer(context, currentTemplate, versionSequence, versionInfo)

        // 步骤 3.3: 根据转换结果创建新的 PipelineTemplateResource
        val currentTemplateModel = JsonUtil.to(currentTemplate.template, Model::class.java)
        val pipelineTemplateResource = createPipelineTemplateResource(
            versionInfo = versionInfo,
            latestTemplate = context.latestTemplate,
            currentTemplate = currentTemplate,
            versionSequence = versionSequence,
            pipelineVersion = counters.pipelineVersion,
            triggerVersion = counters.triggerVersion,
            params = currentTemplateModel.getTriggerContainer().params,
            modelTransferResult = modelTransferResult,
            marketTemplateStatus = context.marketTemplateStatus
        )

        // 步骤 3.4: 持久化新创建的版本
        pipelineTemplatePersistenceService.createReleaseVersion(
            userId = versionInfo.creator,
            templateResource = pipelineTemplateResource,
            templateSetting = modelTransferResult.templateSetting,
            syncPermission = false
        )

        // 步骤 3.5: 根据模板类型记录安装或发布历史
        recordHistoryIfApplicable(context, pipelineTemplateResource, versionInfo)
    }

    /**
     * [辅助函数 3.1] - 计算版本号。
     */
    private fun calculateNextVersions(
        context: MigrationContext,
        currentTemplate: TTemplateRecord,
        currentProjectId: String,
        versionSequence: Int,
        counters: VersionCounters
    ) {
        if (versionSequence > 1) {
            val previousVersionInfo = context.templateVersionInfos[versionSequence - 1]
            val previousTemplate = templateDao.getTemplate(dslContext, currentProjectId, previousVersionInfo.version)
                ?: throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_TEMPLATE_NOT_EXISTS)

            val currentTemplateModel = JsonUtil.to(currentTemplate.template, Model::class.java)
            val previousTemplateModel = JsonUtil.to(previousTemplate.template, Model::class.java)

            counters.pipelineVersion = PipelineVersionUtils.getPipelineVersion(
                currVersion = counters.pipelineVersion,
                originTemplateModel = previousTemplateModel,
                newTemplateModel = currentTemplateModel,
                originParams = previousTemplateModel.getTriggerContainer().params,
                newParams = currentTemplateModel.getTriggerContainer().params
            )
            counters.triggerVersion = PipelineVersionUtils.getTriggerVersion(
                currVersion = counters.triggerVersion,
                originModel = previousTemplateModel,
                newModel = currentTemplateModel
            )
        }
    }

    /**
     * [辅助函数 3.2] - 执行模型转换，失败则抛出异常。
     */
    private fun performModelTransfer(
        context: MigrationContext,
        currentTemplate: TTemplateRecord,
        versionSequence: Int,
        versionInfo: TemplateVersion
    ): PTemplateModelTransferResult {
        val currentSetting = context.setting.copy(
            version = versionSequence,
            creator = versionInfo.creator,
            createdTime = versionInfo.updateTime,
            updateTime = versionInfo.updateTime
        )
        val currentTemplateModel = JsonUtil.to(currentTemplate.template, Model::class.java)
        val currentTemplateParams = currentTemplateModel.getTriggerContainer().params

        return try {
            logger.debug("model Transfer model: {} ", JsonUtil.toJson(currentTemplateModel))
            logger.debug("model Transfer setting: {}", JsonUtil.toJson(currentSetting))
            pipelineTemplateGenerator.transfer(
                userId = context.latestTemplate.creator,
                projectId = context.latestTemplate.projectId,
                storageType = PipelineStorageType.MODEL,
                templateType = PipelineTemplateType.PIPELINE,
                templateModel = currentTemplateModel,
                templateSetting = currentSetting,
                params = currentTemplateParams,
                yaml = null
            )
        } catch (ex: Exception) {
            logger.warn(
                "Model transfer failed for templateId={}, version={}: {}",
                context.templateId, currentTemplate.version, ex.message, ex
            )
            PTemplateModelTransferResult(
                templateType = PipelineTemplateType.PIPELINE,
                templateModel = currentTemplateModel,
                templateSetting = currentSetting,
                yamlWithVersion = null
            )
        }
    }

    /**
     * [辅助函数 3.5] - 根据条件记录历史。
     */
    private fun recordHistoryIfApplicable(
        context: MigrationContext,
        resource: PipelineTemplateResource,
        versionInfo: TemplateVersion
    ) {
        // 如果是约束模板（从研发商店安装），记录安装历史
        if (context.isConstraint) {
            val srcTemplateVersion = resource.srcTemplateVersion!!
            val srcTemplateResource = templateDao.getTemplate(
                dslContext = dslContext,
                projectId = context.srcTemplateProjectId!!,
                version = srcTemplateVersion
            ) ?: throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_TEMPLATE_NOT_EXISTS)

            client.get(ServiceTemplateResource::class).createTemplateInstallHistory(
                TemplateVersionInstallHistoryInfo(
                    srcMarketTemplateProjectCode = context.srcTemplateProjectId,
                    srcMarketTemplateCode = resource.srcTemplateId!!,
                    projectCode = resource.projectId,
                    templateCode = resource.templateId,
                    version = srcTemplateResource.version,
                    versionName = versionInfo.versionName,
                    number = resource.number,
                    createTime = resource.releaseTime
                )
            )
        }

        // 如果是自定义模板且已上架过，记录发布的版本历史
        if (context.hasBeenPublished) {
            client.get(ServiceTemplateResource::class).createMarketTemplatePublishedVersion(
                TemplatePublishedVersionInfo(
                    projectCode = resource.projectId,
                    templateCode = context.templateId,
                    version = resource.version,
                    versionName = versionInfo.versionName,
                    number = resource.number,
                    published = context.marketTemplateStatus == TemplateStatusEnum.RELEASED,
                    creator = versionInfo.creator,
                    updater = versionInfo.creator,
                    createTime = resource.createdTime,
                    updateTime = resource.updateTime
                )
            )
        }
    }

    /**
     * [辅助函数 4] - 收尾工作：创建最终的Info记录并清理脏数据。
     */
    private fun finalizeMigration(context: MigrationContext) {
        pipelineTemplateInfoService.createOrUpdate(
            pipelineTemplateInfo = createPipelineTemplateInfo(
                marketTemplateStatus = context.marketTemplateStatus,
                latestTemplate = context.latestTemplate,
                templateName = context.setting.pipelineName
            )
        )
        cleanupOrphanedVersions(context)
    }

    /**
     * [辅助函数 4.1] - 清理在新表中存在但在旧表中不存在的“脏”版本。
     */
    private fun cleanupOrphanedVersions(context: MigrationContext) {
        val v1TemplateVersions = context.templateVersionInfos.map { it.version }.toSet()

        val v2TemplateVersions = pipelineTemplateResourceService.getTemplateVersions(
            PipelineTemplateResourceCommonCondition(
                projectId = context.projectId,
                templateId = context.templateId,
                status = VersionStatus.RELEASED
            )
        )

        val deletedVersions = v2TemplateVersions.mapNotNull { resource ->
            (if (context.isConstraint) resource.srcTemplateVersion else resource.version)?.toLong()
        }.filterNot { it in v1TemplateVersions }.takeIf { it.isNotEmpty() }

        deletedVersions?.let { versionsToDelete ->
            logger.warn("Found orphaned versions to delete for templateId={}: {}", context.templateId, versionsToDelete)

            // 警告：下面的 client 调用在 DB 事务中，如果 client 调用失败，DB 事务不会回滚。
            // 理想情况下，应先执行 DB 事务，成功后再执行 client 调用，并处理 client 调用失败的情况（如记录日志或放入重试队列）。
            // 这里暂时保持原逻辑，但加上日志警告。
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)

                pipelineTemplateResourceService.delete(
                    transactionContext = transactionContext,
                    commonCondition = PipelineTemplateResourceCommonCondition(
                        projectId = context.projectId,
                        templateId = context.templateId,
                        srcTemplateVersions = if (context.isConstraint) versionsToDelete else null,
                        versions = if (context.isConstraint) null else versionsToDelete,
                        status = VersionStatus.RELEASED,
                        includeDeleted = true
                    )
                )

                pipelineTemplateSettingService.pruneLatestVersions(
                    transactionContext = transactionContext,
                    projectId = context.projectId,
                    templateId = context.templateId,
                    limit = versionsToDelete.size
                )
            }

            // 将 Client 调用移出 DB 事务，以避免分布式事务问题
            try {
                when {
                    context.isConstraint -> {
                        client.get(ServiceTemplateResource::class).deleteTemplateInstallHistoryVersions(
                            srcTemplateCode = context.latestTemplate.srcTemplateId!!,
                            templateCode = context.latestTemplate.id,
                            versions = versionsToDelete
                        )
                    }

                    context.hasBeenPublished -> {
                        client.get(ServiceTemplateResource::class).deleteMarketPublishedVersions(
                            templateCode = context.latestTemplate.id,
                            versions = versionsToDelete
                        )
                    }

                    else -> {}
                }
            } catch (ex: Exception) {
                // 外部服务调用失败，只记录日志，不影响主流程成功状态
                logger.error(
                    "Failed to delete history from remote service for templateId={}, versions={}. " +
                        "This might require manual cleanup. Error: {}", context.templateId, versionsToDelete, ex.message
                )
            }
        }
    }

    fun getTemplateVersions(latestTemplate: TTemplateRecord): List<TemplateVersion> {
        return if (latestTemplate.type == TemplateType.CONSTRAINT.name) {
            val srcLatestTemplate = try {
                templateDao.getLatestTemplate(
                    dslContext = dslContext,
                    templateId = latestTemplate.srcTemplateId
                )
            } catch (ex: ErrorCodeException) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PARENTS_TEMPLATE_NOT_EXISTS,
                    params = arrayOf(latestTemplate.srcTemplateId)
                )
            }
            templateFacadeService.listTemplateAllVersions(
                projectId = srcLatestTemplate.projectId,
                templateId = srcLatestTemplate.id,
                ascSort = true
            ).map {
                it.copy(
                    createTime = latestTemplate.createdTime.timestampmilli(),
                    updateTime = latestTemplate.updateTime.timestampmilli(),
                    creator = latestTemplate.creator
                )
            }
        } else {
            templateFacadeService.listTemplateAllVersions(
                projectId = latestTemplate.projectId,
                templateId = latestTemplate.id,
                ascSort = true
            )
        }
    }

    fun getSrcTemplateProjectId(latestTemplate: TTemplateRecord): String? {
        return takeIf { latestTemplate.type == TemplateType.CONSTRAINT.name }?.let {
            templateDao.getLatestTemplate(
                dslContext = dslContext,
                templateId = latestTemplate.srcTemplateId
            ).projectId
        }
    }

    fun createPipelineTemplateResource(
        versionInfo: TemplateVersion,
        latestTemplate: TTemplateRecord,
        currentTemplate: TTemplateRecord,
        params: List<BuildFormProperty>,
        modelTransferResult: PTemplateModelTransferResult,
        versionSequence: Int,
        pipelineVersion: Int,
        triggerVersion: Int,
        marketTemplateStatus: TemplateStatusEnum
    ): PipelineTemplateResource {
        val isConstraint = latestTemplate.type == TemplateType.CONSTRAINT.name
        val (srcTemplateProjectId, srcTemplateVersion, srcTemplateId) =
            currentTemplate.takeIf { isConstraint }?.let {
                Triple(it.projectId, it.version, it.id)
            } ?: Triple(null, null, null)

        val version = if (isConstraint) {
            pipelineTemplateResourceService.getOrNull(
                commonCondition = PipelineTemplateResourceCommonCondition(
                    projectId = latestTemplate.projectId,
                    templateId = latestTemplate.id,
                    srcTemplateVersion = srcTemplateVersion
                )
            )?.version ?: pipelineTemplateGenerator.generateTemplateVersion()
        } else {
            currentTemplate.version
        }

        return PipelineTemplateResource(
            projectId = latestTemplate.projectId,
            templateId = latestTemplate.id,
            type = PipelineTemplateType.PIPELINE,
            settingVersion = versionSequence,
            version = version,
            storeStatus = marketTemplateStatus,
            number = versionSequence,
            versionName = versionInfo.versionName,
            versionNum = versionSequence,
            settingVersionNum = versionSequence,
            pipelineVersion = pipelineVersion,
            triggerVersion = triggerVersion,
            srcTemplateProjectId = srcTemplateProjectId,
            srcTemplateId = srcTemplateId,
            srcTemplateVersion = srcTemplateVersion,
            params = params,
            model = modelTransferResult.templateModel,
            yaml = modelTransferResult.yamlWithVersion?.yamlStr,
            status = VersionStatus.RELEASED,
            description = currentTemplate.desc,
            sortWeight = 0,
            creator = versionInfo.creator,
            updater = versionInfo.creator,
            releaseTime = versionInfo.updateTime,
            createdTime = versionInfo.createTime,
            updateTime = versionInfo.updateTime
        )
    }

    fun createPipelineTemplateInfo(
        marketTemplateStatus: TemplateStatusEnum,
        latestTemplate: TTemplateRecord,
        templateName: String
    ): PipelineTemplateInfoV2 {
        val latestReleasedResource = pipelineTemplateResourceService.getLatestReleasedResource(
            projectId = latestTemplate.projectId,
            templateId = latestTemplate.id
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_TEMPLATE_LATEST_RELEASED_VERSION_NOT_EXIST
        )
        val instanceSize = templatePipelineDao.countByVersionFeat(
            dslContext = dslContext,
            projectId = latestTemplate.projectId,
            templateId = latestTemplate.id,
            instanceType = TemplateType.CONSTRAINT.name
        )
        logger.info("template instance count {}|{}|{}", latestTemplate.projectId, latestTemplate.id, instanceSize)
        val isConstraint = latestTemplate.type == TemplateType.CONSTRAINT.name
        // 新版本中，storeFlag表示为是否已经上架研发商店。
        val storeFlag = !isConstraint && marketTemplateStatus == TemplateStatusEnum.RELEASED
        // 如果模板已经上传研发商店，发布策略默认为自动
        val publishStrategy = if (storeFlag) UpgradeStrategyEnum.AUTO else null
        val strategy = if (isConstraint) UpgradeStrategyEnum.AUTO else null
        val templateInfo = pipelineTemplateInfoService.getOrNull(
            projectId = latestTemplate.id,
            templateId = latestTemplate.id
        )
        return PipelineTemplateInfoV2(
            id = latestTemplate.id,
            projectId = latestTemplate.projectId,
            name = templateName,
            desc = latestTemplate.desc,
            mode = TemplateType.valueOf(latestTemplate.type),
            category = latestTemplate.category,
            type = PipelineTemplateType.PIPELINE,
            logoUrl = latestTemplate.logoUrl,
            enablePac = templateInfo?.enablePac ?: false,
            releasedVersion = latestReleasedResource.version,
            releasedVersionName = latestReleasedResource.versionName,
            releasedSettingVersion = latestReleasedResource.settingVersion,
            latestVersionStatus = VersionStatus.RELEASED,
            storeStatus = marketTemplateStatus,
            srcTemplateId = latestReleasedResource.srcTemplateId,
            srcTemplateProjectId = latestReleasedResource.srcTemplateProjectId,
            instancePipelineCount = instanceSize,
            publishStrategy = publishStrategy,
            upgradeStrategy = strategy,
            settingSyncStrategy = strategy,
            creator = latestTemplate.creator,
            updater = latestTemplate.creator,
            createdTime = latestTemplate.createdTime.timestampmilli(),
            updateTime = latestTemplate.updateTime.timestampmilli()
        )
    }

    // 数据类 1: 用于在迁移流程中传递共享的上下文信息
    private data class MigrationContext(
        val projectId: String,
        val templateId: String,
        val latestTemplate: TTemplateRecord,
        val setting: PipelineSetting,
        val templateVersionInfos: List<TemplateVersion>,
        val srcTemplateProjectId: String?,
        val marketTemplateStatus: TemplateStatusEnum,
        val hasBeenPublished: Boolean,
        val isConstraint: Boolean,
        val relatedPipelines: List<PipelineTemplateRelated>
    )

    // 数据类 2: 用于在循环中跟踪和更新版本号
    private data class VersionCounters(
        var pipelineVersion: Int,
        var triggerVersion: Int
    )

    /**
     * 封装迁移结果的数据类。
     */
    private data class MigrationResult(
        val allTemplateIds: Set<String>,
        val successfulIds: List<String>,
        val failedItems: List<FailedMigrationItem>
    )

    /**
     * 封装失败项信息的数据类，比简单的字符串更具结构性。
     */
    private data class FailedMigrationItem(
        val templateId: String,
        val errorMessage: String?
    )

    /**
     * 封装清理结果的数据类。
     */
    private data class CleanupStats(
        val countBeforeCleanup: Int,
        val countAfterCleanup: Int
    )

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTemplateMigrateService::class.java)
        private val migrateProjectTemplateExecutorService = Executors.newFixedThreadPool(5)
    }
}

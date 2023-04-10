/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.pipeline.type.devcloud.PublicDevCloudDispathcType
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.BK_ADMINISTRATOR
import com.tencent.devops.process.dao.PipelineTemplateTransferHistoryDao
import com.tencent.devops.process.dao.PipelineTransferHistoryDao
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.pipeline.TransferDispatchType
import com.tencent.devops.process.pojo.pipeline.TransferTemplateDispatchType
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.service.template.TemplateFacadeService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.Executors
import javax.annotation.PostConstruct
import javax.ws.rs.core.Response

@Service
class PipelineTransferService @Autowired constructor(
    private val pipelineListFacadeService: PipelineListFacadeService,
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val redisOperation: RedisOperation,
    private val pipelineTransferHistoryDao: PipelineTransferHistoryDao,
    private val dslContext: DSLContext,
    private val templateFacadeService: TemplateFacadeService,
    private val pipelineTemplateTransferHistoryDao: PipelineTemplateTransferHistoryDao
) {

    @Value("\${transfer.targetImage:tlinux_ci,3.*}")
    private lateinit var targetImageAndVersion: String

    private var defaultImage: String = "tlinux_ci"
    private var defaultImageVersion: String = "3.*"

    private val executePool = Executors.newFixedThreadPool(1)

    @PostConstruct
    fun init() {
        defaultImage = targetImageAndVersion.split(",")[0]
        defaultImageVersion = targetImageAndVersion.split(",")[1]
        logger.info("targetImageAndVersion=$targetImageAndVersion, image=$defaultImage, defaultImageVersion=$defaultImageVersion")
    }

    fun transfer(userId: String, transferDispatchType: TransferDispatchType): Boolean {
        val projectId = transferDispatchType.projectId
        if (!pipelinePermissionService.isProjectUser(userId = userId, projectId = projectId, group = BkAuthGroup.MANAGER)) {
            val defaultMessage = MessageUtil.getMessageByLocale(
                messageCode = BK_ADMINISTRATOR,
                language = I18nUtil.getLanguage(userId)
            )
            val permissionMsg = I18nUtil.getCodeLanMessage(
                messageCode = "${CommonMessageCode.MSG_CODE_ROLE_PREFIX}${BkAuthGroup.MANAGER.value}",
                language = I18nUtil.getLanguage(userId),
                defaultMessage = defaultMessage
            )
            throw ErrorCodeException(
                statusCode = Response.Status.FORBIDDEN.statusCode,
                errorCode = ProcessMessageCode.USER_NEED_PIPELINE_X_PERMISSION,
                defaultMessage = defaultMessage,
                params = arrayOf(permissionMsg)
            )
        }
        executePool.submit {
            val lock = RedisLock(redisOperation, "Transfer_Lock_${transferDispatchType.projectId}", 6000L)
            if (!lock.tryLock()) {
                return@submit
            }
            try {

                logger.info("Transfer_START|[$projectId]|userId=$userId|$transferDispatchType")
                var offset = 0
                val limit = 50
                val channel = transferDispatchType.channelCode
                do {
                    val pipelinesPage = pipelineListFacadeService.getPipelinePage(projectId = projectId, limit = limit, offset = offset)
                    logger.info("Transfer_PipelinePage|[$projectId]|userId=$userId|offset=$offset|page=${pipelinesPage.page}|totalPages=${pipelinesPage.totalPages}")
                    pipelinesPage.records.forEach { pipeline ->
                        if (channel == "ALL" || pipeline.channelCode.name == channel) {
                            val watcher = Watcher("transfer_$projectId")
                            val model = pipelineRepositoryService.getModel(
                                projectId = projectId,
                                pipelineId = pipeline.pipelineId,
                                version = pipeline.version
                            ) ?: return@forEach
                            val stages = transferStages(
                                id = pipeline.pipelineId, model = model,
                                projectId = projectId,
                                sourceDispatchType = transferDispatchType.sourceDispatchType,
                                targetDispatchType = transferDispatchType.targetDispatchType
                            )
                            try {
                                if (stages != null && stages.isNotEmpty()) {
                                    pipelineRepositoryService.deployPipeline(
                                        model = model.copy(stages = stages),
                                        projectId = projectId,
                                        signPipelineId = pipeline.pipelineId,
                                        userId = pipeline.lastModifyUser,
                                        channelCode = pipeline.channelCode,
                                        create = false
                                    )

                                    pipelineTransferHistoryDao.save(
                                        dslContext = dslContext,
                                        projectId = projectId,
                                        pipelineId = pipeline.pipelineId,
                                        userId = userId,
                                        sourceVersion = pipeline.version,
                                        targetVersion = pipeline.version + 1,
                                        log = "SUCCESS"
                                    )

                                    logger.info(watcher.shortSummary())
                                }
                            } catch (e: Exception) {
                                logger.warn("Transfer_Pipeline deploy fail:", e)
                            }
                        }
                    }
                    offset += limit
                } while (pipelinesPage.records.size == limit)
            } finally {
                lock.unlock()
                logger.info("Transfer_END|[$projectId]|userId=$userId|$transferDispatchType")
            }
        }
        return true
    }

    private fun transferStages(id: String, model: Model, projectId: String, sourceDispatchType: String, targetDispatchType: String): List<Stage>? {
        var change = false
        val newStages = mutableListOf<Stage>()
        model.stages.forEach stage@{ s ->
            var jobChange = false
            val jobs = mutableListOf<Container>()
            s.containers.forEach job@{ job ->
                if (job !is VMBuildContainer) {
                    jobs.add(job)
                    return@job
                }
                var newJob: VMBuildContainer? = job

                if (job.dispatchType != null) {
                    if (job.dispatchType!!.buildType().name == sourceDispatchType) {
                        newJob = transferJob(id = id, job = job,
                            projectId = projectId, targetDispatchType = targetDispatchType, sourceDispatchType = sourceDispatchType)
                    }
                } else if (job.dockerBuildVersion != null && sourceDispatchType == BuildType.DOCKER.name) { // 仅针对旧版Linux的镜像
                    newJob = transferJob(id = id, job = job,
                        projectId = projectId, targetDispatchType = targetDispatchType, sourceDispatchType = sourceDispatchType)
                }

                if (newJob == null) {

                    logger.warn("Transfer_DispatchType[$projectId]|$id|newJob=null|oldJob=${job.dispatchType}")
                    newJob = job
                }

                if (!jobChange && job != newJob) {
                    jobChange = true
                }
                jobs.add(newJob)
            }
            if (jobChange) {
                change = true
                newStages.add(s.copy(containers = jobs))
            } else {
                newStages.add(s)
            }
        }
        return if (change) newStages else null
    }

    private fun transferJob(id: String, job: VMBuildContainer, projectId: String, sourceDispatchType: String, targetDispatchType: String): VMBuildContainer? {

        // DevCloud迁移
        if (targetDispatchType != BuildType.PUBLIC_DEVCLOUD.name) {
            return null
        }
        val oldDispatchType = job.dispatchType
        var imageType = ImageType.BKSTORE
        val imageCode: String
        val imageName: String
        var imageVersion = "1.*"
        var credentialId = ""
        var credentialProject = ""
        val value: String
        // 最老的默认镜像

        if (oldDispatchType == null) {
            if (job.dockerBuildVersion == null) {
                logger.warn("Transfer_OldDispatchType[$projectId]|$id|job#${job.id}_${job.name}|db=${job.dockerBuildVersion}")
                return null
            }
            logger.info("Transfer_OldDispatchType[$projectId]|$id|job#${job.id}_${job.name}|db=${job.dockerBuildVersion}")
            when (job.dockerBuildVersion) {
                "tlinux2.2", "tlinux1.2" -> {
                    imageCode = defaultImage
                    imageName = defaultImage
                    imageVersion = defaultImageVersion
                    value = imageCode
                }
                else -> {
                    imageType = ImageType.THIRD
                    imageCode = job.dockerBuildVersion!!
                    imageName = job.dockerBuildVersion!!
                    value = imageCode
                }
            }
        } else {
            // 源已经与目标相同，不迁移
            if (oldDispatchType.buildType().name == targetDispatchType) {
                return null
            }
            // 源不相同，不迁移
            if (oldDispatchType.buildType().name != sourceDispatchType) {
                return null
            }

            logger.info("Transfer_DispatchType[$projectId]|$id|job#${job.id}_${job.name}|od=$oldDispatchType")
            val dockerDispatchType = oldDispatchType as DockerDispatchType
            // 最老的蓝盾镜像，1年前未更新过的蓝盾官方最早出品的tlinux1.2/tlinux2.2 转换为tlinux_ci
            if (dockerDispatchType.imageType == ImageType.BKDEVOPS) {
                imageType = ImageType.BKSTORE
                imageCode = defaultImage
                imageName = defaultImage
                imageVersion = defaultImageVersion
                value = imageCode
            } else {
                imageType = dockerDispatchType.imageType!!
                imageCode = dockerDispatchType.imageCode!!
                imageName = dockerDispatchType.imageName!!
                imageVersion = dockerDispatchType.imageVersion!!
                value = dockerDispatchType.value
            }
            credentialId = dockerDispatchType.credentialId!!
            credentialProject = dockerDispatchType.credentialProject!!
        }

        val dispatchType = PublicDevCloudDispathcType(
            imageCode = imageCode,
            imageName = imageName,
            imageVersion = imageVersion,
            imageType = imageType,
            performanceConfigId = "0",
            image = value,
            credentialId = credentialId,
            credentialProject = credentialProject
        )
        return job.copy(dispatchType = dispatchType)
    }

    fun rollBackTransferDispatchType(userId: String, transferDispatchType: TransferDispatchType): Boolean {
        val projectId = transferDispatchType.projectId
        if (!pipelinePermissionService.isProjectUser(userId = userId, projectId = projectId, group = BkAuthGroup.MANAGER)) {
            val defaultMessage = MessageUtil.getMessageByLocale(
                messageCode = BK_ADMINISTRATOR,
                language = I18nUtil.getLanguage(userId)
            )
            val permissionMsg = I18nUtil.getCodeLanMessage(
                messageCode = "${CommonMessageCode.MSG_CODE_ROLE_PREFIX}${BkAuthGroup.MANAGER.value}",
                language = I18nUtil.getLanguage(userId),
                defaultMessage = defaultMessage
            )
            throw ErrorCodeException(
                statusCode = Response.Status.FORBIDDEN.statusCode,
                errorCode = ProcessMessageCode.USER_NEED_PIPELINE_X_PERMISSION,
                defaultMessage = defaultMessage,
                params = arrayOf(permissionMsg)
            )
        }
        executePool.submit {
            val lock = RedisLock(redisOperation, "Transfer_Lock_${transferDispatchType.projectId}", 6000L)
            if (!lock.tryLock()) {
                return@submit
            }
            try {
                var offset = 0
                val limit = 50
                do {
                    logger.info("RollBackTransfer_START|[$projectId]|userId=$userId|$transferDispatchType")
                    val pipelineIds = transferDispatchType.pipelineIds
                    val list = pipelineTransferHistoryDao.list(
                        dslContext = dslContext,
                        projectId = projectId,
                        pipelineIds = pipelineIds,
                        offset = offset,
                        limit = limit
                    )
                    list.forEach {
                        if (it.log.startsWith("SUCCESS")) {
                            val sourceModel = pipelineRepositoryService.getModel(
                                projectId = projectId,
                                pipelineId = it.pipelineId,
                                version = it.sourceVersion
                            ) ?: return@forEach
                            val pipelineInfo = pipelineRepositoryService.getPipelineInfo(
                                projectId = projectId,
                                pipelineId = it.pipelineId
                            ) ?: return@forEach
                            try {
                                pipelineRepositoryService.deployPipeline(
                                    model = sourceModel,
                                    projectId = projectId,
                                    signPipelineId = it.pipelineId,
                                    userId = pipelineInfo.lastModifyUser,
                                    channelCode = pipelineInfo.channelCode,
                                    create = false
                                )

                                pipelineTransferHistoryDao.save(
                                    dslContext = dslContext,
                                    projectId = projectId,
                                    pipelineId = pipelineInfo.pipelineId,
                                    userId = userId,
                                    sourceVersion = it.sourceVersion,
                                    targetVersion = pipelineInfo.version + 1,
                                    log = "ROLLBACK ${it.log}"
                                )
                            } catch (e: Exception) {
                                logger.warn("RollBack Transfer_Pipeline deploy fail:", e)
                            }
                        }
                    }
                    offset += limit
                } while (list.size == limit)
            } finally {
                lock.unlock()
                logger.info("RollBackTransfer_END|[$projectId]|userId=$userId|$transferDispatchType")
            }
        }
        return true
    }

    fun transferTemplate(userId: String, transferDispatchType: TransferTemplateDispatchType): Boolean {
        val projectId = transferDispatchType.projectId
        if (!pipelinePermissionService.isProjectUser(userId = userId, projectId = projectId, group = BkAuthGroup.MANAGER)) {
            val defaultMessage = MessageUtil.getMessageByLocale(
                messageCode = BK_ADMINISTRATOR,
                language = I18nUtil.getLanguage(userId)
            )
            val permissionMsg = I18nUtil.getCodeLanMessage(
                messageCode = "${CommonMessageCode.MSG_CODE_ROLE_PREFIX}${BkAuthGroup.MANAGER.value}",
                language = I18nUtil.getLanguage(userId),
                defaultMessage = defaultMessage
            )
            throw ErrorCodeException(
                statusCode = Response.Status.FORBIDDEN.statusCode,
                errorCode = ProcessMessageCode.USER_NEED_PIPELINE_X_PERMISSION,
                defaultMessage = defaultMessage,
                params = arrayOf(permissionMsg)
            )
        }

        executePool.submit {
            val lock = RedisLock(redisOperation, "Transfer_LockTemplate_${transferDispatchType.projectId}", 6000L)
            if (!lock.tryLock()) {
                return@submit
            }
            try {

                logger.info("TransferTemplate_START|[$projectId]|userId=$userId|$transferDispatchType")
                var page = 1
                val pageSize = 50
                do {
                    val templates = templateFacadeService.listTemplate(projectId, userId, templateType = TemplateType.CUSTOMIZE,
                        storeFlag = transferDispatchType.storeFlag, page = page, pageSize = pageSize)
                    logger.info("Transfer_TemplatePage|[$projectId]|userId=$userId|page=$page|page=$pageSize|total=${templates.count}")
                    templates.models.forEach {
                        val template = templateFacadeService.getTemplate(projectId = projectId, userId = userId, templateId = it.templateId, version = it.version)
                        val watcher = Watcher("transferTemplate_$projectId")
                        val model = template.template
                        val stages = transferStages(id = it.templateId, model = model, projectId = projectId,
                            sourceDispatchType = transferDispatchType.sourceDispatchType,
                            targetDispatchType = transferDispatchType.targetDispatchType
                        )
                        try {
                            if (stages != null && stages.isNotEmpty()) {
                                templateFacadeService.updateTemplate(
                                    projectId = projectId,
                                    userId = template.creator,
                                    templateId = it.templateId,
                                    versionName = it.versionName,
                                    template = model.copy(stages = stages)
                                )

                                pipelineTemplateTransferHistoryDao.save(
                                    dslContext = dslContext,
                                    projectId = projectId,
                                    templateId = it.templateId,
                                    userId = userId,
                                    sourceVersion = it.version,
                                    targetVersion = it.version + 1,
                                    log = "SUCCESS ${it.versionName}"
                                )

                                logger.info(watcher.shortSummary())
                            }
                        } catch (e: Exception) {
                            logger.warn("TransferTemplate updateTemplate fail:", e)
                        }
                    }
                    page++
                } while (templates.models.size == pageSize)
            } finally {
                lock.unlock()
                logger.info("TransferTemplate_END|[$projectId]|userId=$userId|$transferDispatchType")
            }
        }

        return true
    }

    fun rollBackTransferTemplateDispatchType(userId: String, transferDispatchType: TransferTemplateDispatchType): Boolean {
        val projectId = transferDispatchType.projectId
        if (!pipelinePermissionService.isProjectUser(userId = userId, projectId = projectId, group = BkAuthGroup.MANAGER)) {
            val defaultMessage = MessageUtil.getMessageByLocale(
                messageCode = BK_ADMINISTRATOR,
                language = I18nUtil.getLanguage(userId)
            )
            val permissionMsg = I18nUtil.getCodeLanMessage(
                messageCode = "${CommonMessageCode.MSG_CODE_ROLE_PREFIX}${BkAuthGroup.MANAGER.value}",
                language = I18nUtil.getLanguage(userId),
                defaultMessage = defaultMessage
            )
            throw ErrorCodeException(
                statusCode = Response.Status.FORBIDDEN.statusCode,
                errorCode = ProcessMessageCode.USER_NEED_PIPELINE_X_PERMISSION,
                defaultMessage = defaultMessage,
                params = arrayOf(permissionMsg)
            )
        }

        executePool.submit {
            val lock = RedisLock(redisOperation, "TransferTemplate_Lock_${transferDispatchType.projectId}", 6000L)
            if (!lock.tryLock()) {
                return@submit
            }
            try {
                var offset = 0
                val limit = 50
                do {
                    logger.info("RollBackTransferTemplate_START|[$projectId]|userId=$userId|$transferDispatchType")
                    val templateIds = transferDispatchType.templateIds
                    val list = pipelineTemplateTransferHistoryDao.list(dslContext = dslContext, projectId = projectId, templateIds = templateIds, offset = offset, limit = limit)
                    list.forEach {
                        if (it.log.startsWith("SUCCESS")) {

                            val template = templateFacadeService.getTemplate(projectId = projectId, userId = userId, templateId = it.templateId, version = it.sourceVersion)

                            val split = it.log.split(" ")
                            val versionName = if (split.size == 2) split[1] else template.currentVersion.versionName
                            try {
                                templateFacadeService.updateTemplate(
                                    projectId = projectId,
                                    userId = template.creator,
                                    templateId = it.templateId,
                                    versionName = versionName,
                                    template = template.template
                                )

                                pipelineTemplateTransferHistoryDao.save(
                                    dslContext = dslContext,
                                    projectId = projectId,
                                    templateId = it.templateId,
                                    userId = userId,
                                    sourceVersion = it.sourceVersion,
                                    targetVersion = it.targetVersion + 1,
                                    log = "ROLLBACK ${it.log}"
                                )
                            } catch (e: Exception) {
                                logger.warn("RollBack TransferTemplate updateTemplate fail:", e)
                            }
                        }
                    }
                    offset += limit
                } while (list.size == limit)
            } finally {
                lock.unlock()
                logger.info("RollBackTransferTemplate_END|[$projectId]|userId=$userId|$transferDispatchType")
            }
        }
        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTransferService::class.java)
    }
}

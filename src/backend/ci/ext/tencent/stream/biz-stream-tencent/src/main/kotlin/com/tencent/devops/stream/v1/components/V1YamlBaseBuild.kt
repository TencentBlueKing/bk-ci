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

package com.tencent.devops.stream.v1.components

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.webhook.enums.code.StreamGitObjectKind
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource
import com.tencent.devops.store.pojo.atom.InstallAtomReq
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.v1.client.V1ScmClient
import com.tencent.devops.stream.v1.dao.V1GitPipelineResourceDao
import com.tencent.devops.stream.v1.dao.V1GitRequestEventBuildDao
import com.tencent.devops.stream.v1.pojo.V1GitCITriggerLock
import com.tencent.devops.stream.v1.pojo.V1GitRepositoryConf
import com.tencent.devops.stream.v1.pojo.V1GitRequestEvent
import com.tencent.devops.stream.v1.pojo.enums.V1GitCICommitCheckState
import com.tencent.devops.stream.v1.pojo.isFork
import com.tencent.devops.stream.v1.service.V1GitCIEventService
import com.tencent.devops.stream.v1.service.V1StreamPipelineBranchService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
abstract class V1YamlBaseBuild<T> @Autowired constructor(
    private val client: Client,
    private val scmClient: V1ScmClient,
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val gitPipelineResourceDao: V1GitPipelineResourceDao,
    private val gitRequestEventBuildDao: V1GitRequestEventBuildDao,
    private val gitCIEventSaveService: V1GitCIEventService,
    private val streamPipelineBranchService: V1StreamPipelineBranchService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(V1YamlBaseBuild::class.java)
    }

    private val channelCode = ChannelCode.GIT

    abstract fun gitStartBuild(
        pipeline: StreamTriggerPipeline,
        event: V1GitRequestEvent,
        yaml: T,
        gitBuildId: Long
    ): BuildId?

    fun startBuild(
        pipeline: StreamTriggerPipeline,
        event: V1GitRequestEvent,
        gitProjectConf: V1GitRepositoryConf,
        model: Model,
        gitBuildId: Long
    ): BuildId? {
        val processClient = client.get(ServicePipelineResource::class)
        if (pipeline.pipelineId.isBlank()) {
            // 直接新建
            logger.info("create new gitBuildId:$gitBuildId, pipeline: $pipeline")

            pipeline.pipelineId = processClient.create(
                event.userId,
                gitProjectConf.projectCode!!,
                model,
                channelCode
            ).data!!.id
            gitPipelineResourceDao.createPipeline(
                dslContext = dslContext,
                gitProjectId = gitProjectConf.gitProjectId,
                pipeline = pipeline,
                version = null
            )
        } else if (needReCreate(processClient, event, gitProjectConf, pipeline)) {
            val oldPipelineId = pipeline.pipelineId
            // 先删除已有数据
            logger.info("recreate gitBuildId:$gitBuildId, pipeline: $pipeline")
            try {
                gitPipelineResourceDao.deleteByPipelineId(dslContext, oldPipelineId)
                processClient.delete(event.userId, gitProjectConf.projectCode!!, oldPipelineId, channelCode)
            } catch (e: Exception) {
                logger.warn("V1YamlBaseBuild|gitBuildId|$gitBuildId|pipeline|$pipeline|error", e)
            }
            // 再次新建
            pipeline.pipelineId = processClient.create(
                event.userId,
                gitProjectConf.projectCode!!,
                model,
                channelCode
            ).data!!.id
            gitPipelineResourceDao.createPipeline(
                dslContext = dslContext,
                gitProjectId = gitProjectConf.gitProjectId,
                pipeline = pipeline,
                version = null
            )
            // 对于需要删了重建的，删除旧的流水线-分支记录
            streamPipelineBranchService.deleteBranch(
                gitProjectId = gitProjectConf.gitProjectId,
                pipelineId = oldPipelineId,
                branch = null
            )
        } else if (pipeline.pipelineId.isNotBlank()) {
            // 已有的流水线需要更新下Stream这里的状态
            logger.info("update gitPipeline gitBuildId:$gitBuildId, pipeline: $pipeline")
            gitPipelineResourceDao.updatePipeline(
                dslContext = dslContext,
                gitProjectId = gitProjectConf.gitProjectId,
                pipelineId = pipeline.pipelineId,
                displayName = pipeline.displayName,
                version = null
            )
        }

        // 修改流水线并启动构建，需要加锁保证事务性
        try {
            logger.info(
                "GitCI Build start, gitProjectId[${gitProjectConf.gitProjectId}], " +
                    "pipelineId[${pipeline.pipelineId}], gitBuildId[$gitBuildId]"
            )
            val buildId = startupPipelineBuild(
                processClient,
                model,
                event,
                gitProjectConf,
                pipeline.pipelineId
            )
            logger.info(
                "GitCI Build success, gitProjectId[${gitProjectConf.gitProjectId}], " +
                    "pipelineId[${pipeline.pipelineId}], gitBuildId[$gitBuildId], buildId[$buildId]"
            )
            gitPipelineResourceDao.updatePipelineBuildInfo(dslContext, pipeline, buildId, null)
            gitRequestEventBuildDao.update(dslContext, gitBuildId, pipeline.pipelineId, buildId, null)
            // 成功构建的添加 流水线-分支 记录
            if (!event.isFork() &&
                (
                    event.objectKind == StreamGitObjectKind.PUSH.value ||
                        event.objectKind == StreamGitObjectKind.MERGE_REQUEST.value
                    )
            ) {
                streamPipelineBranchService.saveOrUpdate(
                    gitProjectId = gitProjectConf.gitProjectId,
                    pipelineId = pipeline.pipelineId,
                    branch = event.branch
                )
            }
            // 推送启动构建消息,当人工触发时不推送构建消息
            if (event.objectKind != StreamGitObjectKind.OBJECT_KIND_MANUAL) {
                scmClient.pushCommitCheck(
                    commitId = event.commitId,
                    description = event.description ?: "",
                    mergeRequestId = event.mergeRequestId ?: 0L,
                    pipelineId = pipeline.pipelineId,
                    buildId = buildId,
                    userId = event.userId,
                    status = V1GitCICommitCheckState.PENDING,
                    context = "${pipeline.displayName}(${pipeline.filePath})",
                    gitProjectConf = gitProjectConf
                )
            }
            return BuildId(buildId)
        } catch (e: Exception) {
            logger.warn(
                "V1YamlBaseBuild|startBuild|GitCI Build failed|gitProjectId|${gitProjectConf.gitProjectId}|" +
                    "pipelineId|${pipeline.pipelineId}|gitBuildId|$gitBuildId",
                e
            )
            val build = gitRequestEventBuildDao.getByGitBuildId(dslContext, gitBuildId)
            gitCIEventSaveService.saveRunNotBuildEvent(
                userId = event.userId,
                eventId = event.id!!,
                pipelineId = pipeline.pipelineId,
                pipelineName = pipeline.displayName,
                filePath = pipeline.filePath,
                originYaml = build?.originYaml,
                normalizedYaml = build?.normalizedYaml,
                reason = TriggerReason.PIPELINE_RUN_ERROR.name,
                reasonDetail = e.message ?: TriggerReason.PIPELINE_RUN_ERROR.detail,
                gitProjectId = event.gitProjectId,
                // V1不发送通知
                sendCommitCheck = false,
                commitCheckBlock = false,
                version = null,
                branch = event.branch
            )
            if (build != null) gitRequestEventBuildDao.removeBuild(dslContext, gitBuildId)
        }

        return null
    }

    private fun startupPipelineBuild(
        processClient: ServicePipelineResource,
        model: Model,
        event: V1GitRequestEvent,
        gitProjectConf: V1GitRepositoryConf,
        pipelineId: String
    ): String {
        val triggerLock = V1GitCITriggerLock(redisOperation, gitProjectConf.gitProjectId, pipelineId)
        try {
            triggerLock.lock()
            processClient.edit(event.userId, gitProjectConf.projectCode!!, pipelineId, model, channelCode)
            return client.get(ServiceBuildResource::class).manualStartup(
                userId = event.userId,
                projectId = gitProjectConf.projectCode!!,
                pipelineId = pipelineId,
                values = mapOf(),
                channelCode = channelCode
            ).data!!.id
        } finally {
            triggerLock.unlock()
        }
    }

    private fun needReCreate(
        processClient: ServicePipelineResource,
        event: V1GitRequestEvent,
        gitProjectConf: V1GitRepositoryConf,
        pipeline: StreamTriggerPipeline
    ): Boolean {
        try {
            val response = processClient.get(
                event.userId,
                gitProjectConf.projectCode!!,
                pipeline.pipelineId,
                channelCode
            )
            if (response.isNotOk()) {
                logger.warn("get pipeline failed, msg: ${response.message}")
                return true
            }
        } catch (e: Exception) {
            logger.warn(
                "get pipeline failed, pipelineId: ${pipeline.pipelineId}, " +
                    "projectCode: ${gitProjectConf.projectCode}, error msg: ${e.message}"
            )
            return true
        }
        return false
    }

    fun installMarketAtom(gitProjectConf: V1GitRepositoryConf, userId: String, atomCode: String) {
        val projectCodes = ArrayList<String>()
        projectCodes.add(gitProjectConf.projectCode!!)
        try {
            client.get(ServiceMarketAtomResource::class).installAtom(
                userId = userId,
                channelCode = channelCode,
                installAtomReq = InstallAtomReq(projectCodes, atomCode)
            )
        } catch (e: Throwable) {
            logger.warn("install atom($atomCode) failed, exception:", e)
            // 可能之前安装过，继续执行不退出
        }
    }
}

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

package com.tencent.devops.stream.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.model.stream.tables.records.TGitPipelineResourceRecord
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.pojo.setting.PipelineModelAndSetting
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.constant.StreamConstant
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.dao.GitRequestEventNotBuildDao
import com.tencent.devops.stream.pojo.AllPathPair
import com.tencent.devops.stream.pojo.StreamGitPipelineDir
import com.tencent.devops.stream.pojo.StreamGitProjectPipeline
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StreamPipelineService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val gitPipelineResourceDao: GitPipelineResourceDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitRequestEventNotBuildDao: GitRequestEventNotBuildDao,
    private val redisOperation: RedisOperation,
    private val websocketService: StreamWebsocketService,
    private val streamGitTransferService: StreamGitTransferService,
    private val streamBasicSettingService: StreamBasicSettingService,
    private val gitConfig: StreamGitConfig
) {
    companion object {
        private val logger = LoggerFactory.getLogger(StreamPipelineService::class.java)
        private val channelCode = ChannelCode.GIT
        private const val CIDir = ".ci/"
        private const val ymlVersion = "v2.0"
    }

    fun getPipelineList(
        userId: String,
        gitProjectId: Long,
        keyword: String?,
        page: Int?,
        pageSize: Int?,
        filePath: String?
    ): Page<StreamGitProjectPipeline> {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 10
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val pipelines = gitPipelineResourceDao.getPageByGitProjectId(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            keyword = keyword,
            offset = limit.offset,
            limit = limit.limit,
            filePath = filePath
        )
        if (pipelines.isEmpty()) return Page(
            count = 0L,
            page = pageNotNull,
            pageSize = pageSizeNotNull,
            totalPages = 0,
            records = emptyList()
        )
        val count = gitPipelineResourceDao.getPipelineCount(dslContext, gitProjectId)
        // 获取流水线最后一次构建分支
        val pipelineBranchMap = getPipelineLastBuildBranch(gitProjectId, pipelines.map { it.pipelineId }.toSet())
        val basicSetting = streamBasicSettingService.getStreamConf(gitProjectId)
        return Page(
            count = count.toLong(),
            page = pageNotNull,
            pageSize = pageSizeNotNull,
            totalPages = PageUtil.calTotalPage(pageSizeNotNull, count.toLong()),
            records = pipelines.map {
                StreamGitProjectPipeline(
                    gitProjectId = gitProjectId,
                    pipelineId = it.pipelineId,
                    filePath = it.filePath,
                    displayName = it.displayName,
                    enabled = it.enabled,
                    creator = it.creator,
                    latestBuildInfo = null,
                    latestBuildBranch = pipelineBranchMap[it.pipelineId] ?: "master",
                    yamlLink = genYamlLink(
                        pathWithNamespace = basicSetting?.pathWithNamespace,
                        pipelineBranch = pipelineBranchMap[it.pipelineId],
                        filePath = it.filePath
                    )
                )
            }
        )
    }

    private fun genYamlLink(pathWithNamespace: String?, pipelineBranch: String?, filePath: String): String {
        val branch = pipelineBranch ?: "master"
        return "${gitConfig.gitUrl}/$pathWithNamespace/blob/$branch/$filePath"
    }

    fun getPipelineDirList(
        userId: String,
        gitProjectId: Long,
        pipelineId: String?
    ): StreamGitPipelineDir {
        val allPipeline = gitPipelineResourceDao.getDirListByGitProjectId(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            pipelineId = null
        )
        return StreamGitPipelineDir(
            currentPath = allPipeline.find { it.value2() == pipelineId }?.value1(),
            allPath = allPipeline.map { it.value1() }.distinct().mapNotNull {
                if (it == CIDir) return@mapNotNull null
                AllPathPair(path = it, name = it.removePrefix(CIDir).removeSuffix("/"))
            }
        )
    }

    fun getPipelineListWithoutHistory(
        userId: String,
        gitProjectId: Long,
        keyword: String?,
        page: Int?,
        pageSize: Int?
    ): List<StreamGitProjectPipeline> {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 10
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val pipelines = gitPipelineResourceDao.getPageByGitProjectId(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            keyword = keyword,
            offset = limit.offset,
            limit = limit.limit
        )
        if (pipelines.isEmpty()) {
            return emptyList()
        }
        return pipelines.map {
            StreamGitProjectPipeline(
                gitProjectId = gitProjectId,
                pipelineId = it.pipelineId,
                filePath = it.filePath,
                displayName = it.displayName,
                enabled = it.enabled,
                creator = it.creator,
                latestBuildInfo = null,
                latestBuildBranch = null
            )
        }
    }

    fun getPipelineById(
        pipelineId: String
    ): StreamGitProjectPipeline? {
        logger.info("get pipeline: $pipelineId")
        val pipeline = gitPipelineResourceDao.getPipelinesInIds(
            dslContext = dslContext,
            gitProjectId = null,
            pipelineIds = listOf(pipelineId)
        ).getOrNull(0) ?: return null
        return StreamGitProjectPipeline(
            gitProjectId = pipeline.gitProjectId,
            pipelineId = pipeline.pipelineId,
            filePath = pipeline.filePath,
            displayName = pipeline.displayName,
            enabled = pipeline.enabled,
            creator = pipeline.creator,
            latestBuildInfo = null,
            latestBuildBranch = null
        )
    }

    fun enablePipeline(
        userId: String,
        gitProjectId: Long,
        pipelineId: String,
        enabled: Boolean
    ): Boolean {
        // 关闭流水线时同时关闭其定时触发任务
        val lock = getLock(gitProjectId = gitProjectId, pipelineId = pipelineId)
        try {
            lock.lock()
            val processClient = client.get(ServicePipelineResource::class)
            val model = getModel(processClient, userId, gitProjectId, pipelineId) ?: return false
            model.stages.first()
                .containers.first()
                .elements.filter { it.getClassType() == "timerTrigger" }
                .forEach { it.additionalOptions?.enable = enabled }
            val edited = saveModel(processClient, userId, gitProjectId, pipelineId, model)
            logger.info(
                "gitProjectId: $gitProjectId enable pipeline[$pipelineId] to $enabled" +
                    ", edit timerTrigger with $edited"
            )
            websocketService.pushPipelineWebSocket(gitProjectId.toString(), pipelineId, userId)
            return gitPipelineResourceDao.enablePipelineById(
                dslContext = dslContext,
                pipelineId = pipelineId,
                enabled = enabled
            ) == 1
        } catch (e: Exception) {
            logger.error("gitProjectId: $gitProjectId enable pipeline[$pipelineId] to $enabled error ${e.message}")
            return false
        } finally {
            lock.unlock()
        }
    }

    fun getYamlByPipeline(
        gitProjectId: Long,
        pipelineId: String,
        ref: String
    ): String? {
        logger.info("get yaml by pipelineId:($pipelineId), ref: $ref")
        val conf = streamBasicSettingService.getStreamConf(gitProjectId) ?: return null

        val filePath =
            gitPipelineResourceDao.getPipelineById(dslContext, gitProjectId, pipelineId)?.filePath ?: return null

        return streamGitTransferService.getYamlContent(
            gitProjectId = gitProjectId.toString(),
            fileName = filePath,
            ref = ref,
            userId = conf.enableUserId
        )
    }

    fun savePipeline(
        pipeline: StreamTriggerPipeline,
        userId: String,
        gitProjectId: Long,
        projectCode: String,
        modelAndSetting: PipelineModelAndSetting,
        updateLastModifyUser: Boolean
    ) {
        val processClient = client.get(ServicePipelineResource::class)
        if (pipeline.pipelineId.isBlank()) {
            // 直接新建
            logger.info("create newpipeline: $pipeline")

            pipeline.pipelineId = processClient.create(
                userId = userId,
                projectId = projectCode,
                pipeline = modelAndSetting.model,
                channelCode = channelCode
            ).data!!.id
            gitPipelineResourceDao.createPipeline(
                dslContext = dslContext,
                gitProjectId = gitProjectId,
                pipeline = pipeline.toGitPipeline(),
                version = ymlVersion
            )
            websocketService.pushPipelineWebSocket(
                projectId = projectCode,
                pipelineId = pipeline.pipelineId,
                userId = userId
            )
        }
        processClient.saveSetting(
            userId = userId,
            projectId = projectCode,
            pipelineId = pipeline.pipelineId,
            setting = modelAndSetting.setting.copy(
                projectId = projectCode,
                pipelineId = pipeline.pipelineId,
                pipelineName = modelAndSetting.model.name
            ),
            updateLastModifyUser = updateLastModifyUser
        )
    }

    fun getPipelineByFile(
        gitProjectId: Long,
        filePath: String
    ): TGitPipelineResourceRecord? {
        return gitPipelineResourceDao.getPipelineByFile(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            filePath = filePath
        )
    }

    private fun getPipelineLastBuildBranch(
        gitProjectId: Long,
        pipelineIds: Set<String>
    ): Map<String, String> {
        val conf = streamBasicSettingService.getStreamConf(gitProjectId) ?: return emptyMap()
        var branch: String? = null
        val result = mutableMapOf<String, String>()
        val pipelineBuild = gitRequestEventBuildDao.getPipelinesLastBuild(dslContext, gitProjectId, pipelineIds)
            ?.associate { it.pipelineId to it.branch }
        // 获取没有构建记录的流水线的未构建成功的分支
        val noBuildPipelines = (pipelineIds - pipelineBuild?.keys).map { it.toString() }.toSet()
        val pipelineNoBuild: Map<String, String>? = if (noBuildPipelines.isEmpty()) {
            emptyMap()
        } else {
            gitRequestEventNotBuildDao.getPipelinesLastBuild(dslContext, gitProjectId, noBuildPipelines)
                ?.associate { it.pipelineId to it.branch }
        }
        pipelineIds.forEach { pipelineId ->
            if (!pipelineBuild?.get(pipelineId).isNullOrBlank()) {
                result[pipelineId] = pipelineBuild?.get(pipelineId).toString()
                return@forEach
            }
            if (!pipelineNoBuild?.get(pipelineId).isNullOrBlank()) {
                result[pipelineId] = pipelineNoBuild?.get(pipelineId).toString()
                return@forEach
            }
            // 构建记录和未构建记录都没得，就去拿默认分支
            if (branch.isNullOrBlank()) {
                branch = streamGitTransferService.getGitProjectInfo(
                    gitProjectId = gitProjectId.toString(),
                    userId = conf.enableUserId
                )?.defaultBranch ?: "master"
                result[pipelineId] = branch!!
            } else {
                result[pipelineId] = branch!!
            }
        }
        return result
    }

    private fun getLock(gitProjectId: Long, pipelineId: String): RedisLock {
        return RedisLock(
            redisOperation = redisOperation,
            lockKey = "STREAM_PIPELINE_ENABLE_LOCK_${gitProjectId}_$pipelineId",
            expiredTimeInSeconds = 60L
        )
    }

    private fun getModel(
        processClient: ServicePipelineResource,
        userId: String,
        gitProjectId: Long,
        pipelineId: String
    ): Model? {
        try {
            val response = processClient.get(
                userId = userId,
                projectId = "${StreamConstant.DEVOPS_PROJECT_PREFIX}$gitProjectId",
                pipelineId = pipelineId,
                channelCode = channelCode
            )
            if (response.isNotOk()) {
                logger.error("get pipeline failed, msg: ${response.message}")
                return null
            }
            return response.data
        } catch (e: Exception) {
            logger.error(
                "get pipeline failed, pipelineId: " +
                    "$pipelineId, projectCode: $gitProjectId, error msg: ${e.message}"
            )
            return null
        }
    }

    private fun saveModel(
        processClient: ServicePipelineResource,
        userId: String,
        gitProjectId: Long,
        pipelineId: String,
        model: Model
    ): Boolean? {
        try {
            val response = processClient.edit(
                userId = userId,
                projectId = "${StreamConstant.DEVOPS_PROJECT_PREFIX}$gitProjectId",
                pipelineId = pipelineId,
                pipeline = model,
                channelCode = channelCode
            )
            if (response.isNotOk()) {
                logger.error("edit pipeline failed, msg: ${response.message}")
                return null
            }
            return response.data
        } catch (e: Exception) {
            logger.error(
                "edit pipeline failed, pipelineId: " +
                    "$pipelineId, projectCode: $gitProjectId, error msg: ${e.message}"
            )
            return null
        }
    }
}

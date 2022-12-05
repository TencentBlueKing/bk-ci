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

package com.tencent.devops.stream.trigger.parsers

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.StreamPipelineTriggerDao
import com.tencent.devops.stream.service.StreamPipelineBranchService
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.stream.trigger.service.RepoTriggerEventService
import com.tencent.devops.stream.trigger.service.StreamEventService
import com.tencent.devops.stream.util.GitCommonUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class PipelineDelete @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val client: Client,
    private val gitPipelineResourceDao: GitPipelineResourceDao,
    private val streamPipelineTriggerDao: StreamPipelineTriggerDao,
    private val streamPipelineBranchService: StreamPipelineBranchService,
    private val streamEventService: StreamEventService,
    private val repoTriggerEventService: RepoTriggerEventService,
    private val streamGitConfig: StreamGitConfig
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineDelete::class.java)
        private val channelCode = ChannelCode.GIT
    }

    /**
     * push请求时涉及到删除yml文件的操作
     * 所有向远程库的请求最后都会为push，所以针对push删除即可
     * push请求  - 检索当前流水线的存在分支，如果源分支分支在流水线存在分支中唯一，删除流水线
     * 因为源分支已经删除文件，所以后面执行时不会触发构建
     */
    fun checkAndDeletePipeline(
        action: BaseAction,
        path2PipelineExists: Map<String, StreamTriggerPipeline>,
        deleteYamlFiles: List<String>?
    ) {
        if (deleteYamlFiles.isNullOrEmpty()) {
            return
        }

        deleteYamlFiles.forEach { filePath ->
            val existPipeline = path2PipelineExists[filePath] ?: return@forEach
            val pipelineId = existPipeline.pipelineId
            delete(
                action = action,
                gitProjectId = existPipeline.gitProjectId,
                pipelineId = pipelineId,
                filePath = filePath
            )
        }
    }

    fun delete(
        action: BaseAction,
        gitProjectId: String,
        pipelineId: String,
        filePath: String?
    ) {
        val processClient = client.get(ServicePipelineResource::class)
        // 先删除后查询的过程需要加锁
        val redisLock = RedisLock(
            redisOperation,
            "STREAM_DELETE_PIPELINE_$pipelineId",
            60L
        )
        try {
            redisLock.lock()
            streamPipelineBranchService.deleteBranch(
                gitProjectId = gitProjectId.toLong(),
                pipelineId = pipelineId,
                branch = action.data.eventCommon.branch
            )

            // 删除分支流水线记录时同步删除掉触发器缓存
            streamPipelineTriggerDao.deleteTrigger(
                dslContext = dslContext,
                projectId = GitCommonUtils.getCiProjectId(gitProjectId.toLong(), streamGitConfig.getScmType()),
                pipelineId = pipelineId,
                branch = action.data.eventCommon.branch
            )

            repoTriggerEventService.deleteRepoTriggerEvent(pipelineId)

            val isFileEmpty =
                if (null != filePath && action.data.eventCommon.branch != action.data.context.defaultBranch) {
                    checkFileEmpty(
                        action,
                        gitProjectId = gitProjectId,
                        filePath = filePath
                    )
                } else {
                    true
                }
            if (isFileEmpty &&
                !streamPipelineBranchService.hasBranchExist(gitProjectId.toLong(), pipelineId)
            ) {
                logger.info(
                    "event: ${action.data.context.requestEventId}" +
                        "delete file $filePath with pipeline: $pipelineId "
                )
                gitPipelineResourceDao.deleteByPipelineId(dslContext, pipelineId)
                val pipelineInfoResult = processClient.getPipelineInfo(
                    projectId = GitCommonUtils.getCiProjectId(gitProjectId.toLong(), streamGitConfig.getScmType()),
                    pipelineId = pipelineId,
                    channelCode = channelCode
                )
                if (pipelineInfoResult.data != null) {
                    processClient.delete(
                        userId = action.data.getUserId(),
                        projectId = GitCommonUtils.getCiProjectId(
                            gitProjectId.toLong(),
                            streamGitConfig.getScmType()
                        ),
                        pipelineId = pipelineId,
                        channelCode = channelCode
                    )
                }
                // 删除相关的构建记录
                streamEventService.deletePipelineBuildHistory(setOf(pipelineId))
            }
        } finally {
            redisLock.unlock()
        }
    }

    private fun checkFileEmpty(
        action: BaseAction,
        gitProjectId: String,
        filePath: String
    ): Boolean {
        val fileList = try {
            action.api.getFileTree(
                cred = action.getGitCred(),
                gitProjectId = action.getGitProjectIdOrName(gitProjectId),
                path = if (filePath.contains("/")) {
                    filePath.substring(0, filePath.lastIndexOf("/"))
                } else {
                    filePath
                },
                ref = action.data.context.defaultBranch,
                recursive = true,
                retry = ApiRequestRetryInfo(true)
            )
        } catch (ignored: Throwable) {
            logger.info("checkFileEmpty get file error , .ci/ may be delete ${ignored.message}")
            listOf()
        }

        fileList.forEach {
            if (it.name == filePath.substring(filePath.lastIndexOf("/") + 1)) {
                return false
            }
        }
        return true
    }
}

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
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.service.StreamPipelineBranchService
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.service.StreamEventService
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
    private val streamPipelineBranchService: StreamPipelineBranchService,
    private val streamEventService: StreamEventService
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
            delete(action, pipelineId, filePath)
        }
    }

    fun delete(
        action: BaseAction,
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
                gitRequestEvent.gitProjectId,
                pipelineId,
                gitRequestEvent.branch
            )

            val isFileEmpty = if (null != filePath) {
                checkFileEmpty(
                    gitToken = gitToken,
                    gitProjectId = gitRequestEvent.gitProjectId,
                    filePath = filePath
                )
            } else {
                true
            }
            if (isFileEmpty &&
                !streamPipelineBranchService.hasBranchExist(gitRequestEvent.gitProjectId, pipelineId)
            ) {
                logger.info("event: ${gitRequestEvent.id} delete file: $filePath with pipeline: $pipelineId ")
                gitPipelineResourceDao.deleteByPipelineId(dslContext, pipelineId)
                val pipelineInfoResult = processClient.getPipelineInfo(
                    projectId = gitProjectConf.projectCode!!,
                    pipelineId = pipelineId,
                    channelCode = channelCode
                )
                if (pipelineInfoResult.data != null) {
                    processClient.delete(
                        gitRequestEvent.userId, gitProjectConf.projectCode!!, pipelineId,
                        channelCode
                    )
                }
                // 删除相关的构建记录
                gitCIEventService.deletePipelineBuildHistory(setOf(pipelineId))
            }
        } finally {
            redisLock.unlock()
        }
    }

    private fun checkFileEmpty(
        gitToken: String,
        gitProjectId: Long,
        filePath: String
    ): Boolean {
        val fileList = scmService.getFileTreeFromGitWithDefaultBranch(
            gitToken = gitToken,
            gitProjectId = gitProjectId,
            filePath = filePath,
            recursive = true
        )

        fileList.forEach {
            if (it.name == filePath.substring(filePath.lastIndexOf("/") + 1)) {
                return false
            }
        }
        return true
    }
}

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

import com.tencent.devops.stream.dao.StreamPipelineBranchDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class StreamAsyncService @Autowired constructor(
    private val dslContext: DSLContext,
    private val streamPipelineBranchDao: StreamPipelineBranchDao,
    private val scmService: StreamScmService
) {
    companion object {
        private const val pageSize = 1000L
        private val logger = LoggerFactory.getLogger(StreamAsyncService::class.java)
    }

    @Async("pipelineBranchCheckExecutor")
    fun checkPipelineBranch(gitProjectId: Long?, pipelineId: String?) {
        logger.info("StreamAsyncService|checkPipelineBranch|start")
        if (gitProjectId == null) {
            var realMaxId = streamPipelineBranchDao.getMaxGitProjectId(dslContext)
            val minId = streamPipelineBranchDao.getMinGitProjectId(dslContext)
            while (realMaxId > minId) {
                if (streamPipelineBranchDao.isGitProjectExist(dslContext, realMaxId)) {
                    checkBranch(realMaxId, null)
                }
                realMaxId--
            }
        } else {
            checkBranch(gitProjectId, pipelineId)
        }
        logger.info("StreamAsyncService|checkPipelineBranch|end")
    }

    private fun checkBranch(gitProjectId: Long, pipelineId: String?) {
        val branches = if (pipelineId != null) {
            streamPipelineBranchDao.getBranches(dslContext, gitProjectId, pipelineId, pageSize)
        } else {
            streamPipelineBranchDao.getBranches(dslContext, gitProjectId, null, pageSize)
        }.toMutableSet()
        if (branches.isEmpty()) {
            return
        }

        var flag = true
        var page = 1
        val token = try {
            scmService.getToken(gitProjectId.toString())
        } catch (e: Throwable) {
            logger.warn("StreamAsyncService|checkBranch|get token error|gitProjectId|$gitProjectId|error| ${e.message}")
            return
        }.accessToken

        while (flag) {
            // 分页查出工蜂现有的分支
            val gitBranches = try {
                scmService.getProjectBranchesRetry(
                    token = token,
                    gitProjectId.toString(),
                    page = page,
                    pageSize = 100
                )
            } catch (e: Throwable) {
                logger.warn("StreamAsyncService|checkBranch|get branches error|projectId|$gitProjectId |${e.message}")
                return
            }?.toSet()
            if (gitBranches.isNullOrEmpty() && page == 1) {
                return
            }

            val realGitBranches = gitBranches ?: emptySet()
            // 如果查询的结果是最后一页姐结束循环，不然继续循环
            if (realGitBranches.size < 100) {
                flag = false
            } else {
                page++
            }
            // 在工蜂存在的分支不进入下次筛选
            val iterator = branches.iterator()
            while (iterator.hasNext()) {
                val branch = iterator.next()
                if (branch in realGitBranches) {
                    iterator.remove()
                }
            }
            // 遍历结束后删除掉所有剩余的
            if (!flag) {
                streamPipelineBranchDao.deleteBranches(dslContext, gitProjectId, branches)
            }
        }
    }
}

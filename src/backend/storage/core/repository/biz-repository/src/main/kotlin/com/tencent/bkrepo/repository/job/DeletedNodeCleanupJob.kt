/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.repository.job

import com.tencent.bkrepo.common.service.log.LoggerHolder
import com.tencent.bkrepo.repository.config.RepositoryProperties
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.dao.RepositoryDao
import com.tencent.bkrepo.repository.model.TNode
import com.tencent.bkrepo.repository.model.TRepository
import com.tencent.bkrepo.repository.service.file.FileReferenceService
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.data.domain.PageRequest
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * 清理被标记为删除的node，同时减少文件引用
 */
@Component
class DeletedNodeCleanupJob(
    private val nodeDao: NodeDao,
    private val repositoryDao: RepositoryDao,
    private val fileReferenceService: FileReferenceService,
    private val repositoryProperties: RepositoryProperties
) {

    @Scheduled(cron = "0 0 2/6 * * ?") // 2点开始，6小时执行一次
    @SchedulerLock(name = "DeletedNodeCleanupJob", lockAtMostFor = "PT6H")
    fun cleanup() {
        logger.info("Starting to clean up deleted nodes.")
        val reserveDays = repositoryProperties.deletedNodeReserveDays
        if (reserveDays < 0) {
            logger.info("Reserve days[$reserveDays] for deleted nodes is less than 0, skip cleaning up.")
            return
        }
        var totalCleanupCount = 0L
        var fileCleanupCount = 0L
        var folderCleanupCount = 0L
        val startTimeMillis = System.currentTimeMillis()
        val expireDate = LocalDateTime.now().minusDays(reserveDays)

        repositoryDao.findAll().forEach { repo ->
            handleRepo(repo, expireDate).let {
                totalCleanupCount += it.totalCleanupCount
                fileCleanupCount += it.fileCleanupCount
                folderCleanupCount += it.folderCleanupCount
            }
        }
        val elapseTimeMillis = System.currentTimeMillis() - startTimeMillis
        logger.info(
            "[$totalCleanupCount] nodes has been clean up, file[$fileCleanupCount], folder[$folderCleanupCount]" +
                ", elapse [$elapseTimeMillis] ms totally."
        )
    }

    private fun handleRepo(repo: TRepository, expireDate: LocalDateTime): CleanupResult {
        val result = CleanupResult()
        val criteria = where(TNode::projectId).isEqualTo(repo.projectId)
            .and(TNode::repoName).isEqualTo(repo.name)
            .and(TNode::deleted).lt(expireDate)
        val query = Query.query(criteria).with(PageRequest.of(0, PAGE_SIZE))
        var deletedNodeList = nodeDao.find(query)
        while (deletedNodeList.isNotEmpty()) {
            logger.info("Retrieved [${deletedNodeList.size}] deleted records from ${repo.projectId}/${repo.name}")
            deletedNodeList.forEach { node ->
                cleanUpNode(repo, node)
                if (node.folder) {
                    FileReferenceCleanupJob
                    result.folderCleanupCount += 1
                } else {
                    result.fileCleanupCount += 1
                }
            }
            result.totalCleanupCount += deletedNodeList.size
            deletedNodeList = nodeDao.find(query)
        }
        return result
    }

    private fun cleanUpNode(repo: TRepository, node: TNode) {
        var fileReferenceChanged = false
        try {
            val nodeQuery = Query.query(
                where(TNode::projectId).isEqualTo(node.projectId)
                    .and(TNode::repoName).isEqualTo(node.repoName)
                    .and(TNode::fullPath).isEqualTo(node.fullPath)
                    .and(TNode::deleted).isEqualTo(node.deleted)
            )
            nodeDao.remove(nodeQuery)
            if (!node.folder) {
                fileReferenceChanged = fileReferenceService.decrement(node, repo)
            }
        } catch (ignored: Exception) {
            logger.error("Clean up deleted node[$node] failed.", ignored)
            if (fileReferenceChanged) {
                fileReferenceService.increment(node, repo)
            }
        }
    }

    data class CleanupResult(
        var folderCleanupCount: Int = 0,
        var fileCleanupCount: Int = 0,
        var totalCleanupCount: Int = 0
    )

    companion object {
        private val logger = LoggerHolder.jobLogger
        private const val PAGE_SIZE = 1000
    }
}

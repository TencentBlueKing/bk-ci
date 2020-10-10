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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.repository.job

import com.tencent.bkrepo.common.service.log.LoggerHolder
import com.tencent.bkrepo.repository.config.RepositoryProperties
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.dao.repository.RepoRepository
import com.tencent.bkrepo.repository.model.TNode
import com.tencent.bkrepo.repository.service.FileReferenceService
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * 清理被标记为删除的node，同时减少文件引用
 */
@Component
class DeletedNodeCleanupJob {

    @Autowired
    private lateinit var nodeDao: NodeDao

    @Autowired
    private lateinit var repoRepository: RepoRepository

    @Autowired
    private lateinit var fileReferenceService: FileReferenceService

    @Autowired
    private lateinit var repositoryProperties: RepositoryProperties

    @Scheduled(cron = "0 0 1/3 * * ?")
    @SchedulerLock(name = "DeletedNodeCleanupJob", lockAtMostFor = "PT1H")
    fun cleanUp() {
        logger.info("Starting to clean up deleted nodes.")
        if (repositoryProperties.deletedNodeReserveDays >= 0) {
            var totalCleanupCount = 0L
            var fileCleanupCount = 0L
            var folderCleanupCount = 0L
            val startTimeMillis = System.currentTimeMillis()
            val expireDate = LocalDateTime.now().minusDays(repositoryProperties.deletedNodeReserveDays)

            repoRepository.findAll().forEach { repo ->
                val query = Query.query(
                    Criteria.where(TNode::projectId.name).`is`(repo.projectId)
                        .and(TNode::repoName.name).`is`(repo.name)
                        .and(TNode::deleted.name).lt(expireDate)
                ).with(PageRequest.of(0, 1000))
                var deletedNodeList = nodeDao.find(query)
                while (deletedNodeList.isNotEmpty()) {
                    logger.info("Retrieved [${deletedNodeList.size}] deleted records to be clean up.")
                    deletedNodeList.forEach { node ->
                        var fileReferenceChange = false
                        try {
                            if (node.folder) {
                                folderCleanupCount += 1
                            } else {
                                fileReferenceChange = fileReferenceService.decrement(node, repo)
                                fileCleanupCount += 1
                            }
                            val nodeQuery = Query.query(
                                Criteria.where(TNode::projectId.name).`is`(node.projectId)
                                    .and(TNode::repoName.name).`is`(node.repoName)
                                    .and(TNode::fullPath.name).`is`(node.fullPath)
                                    .and(TNode::deleted.name).`is`(node.deleted)
                            )
                            nodeDao.remove(nodeQuery)
                        } catch (ignored: Exception) {
                            logger.error("Clean up deleted node[$node] failed.", ignored)
                            if (fileReferenceChange) {
                                fileReferenceService.increment(node, repo)
                            }
                        } finally {
                            totalCleanupCount += 1
                        }
                    }
                    deletedNodeList = nodeDao.find(query)
                }
            }
            val elapseTimeMillis = System.currentTimeMillis() - startTimeMillis
            logger.info(
                "[$totalCleanupCount] nodes has been clean up, file[$fileCleanupCount], folder[$folderCleanupCount]" +
                    ", elapse [$elapseTimeMillis] ms totally."
            )
        } else {
            logger.info("Reserve days[${repositoryProperties.deletedNodeReserveDays}] for deleted nodes is less than 0, skip cleaning up.")
        }
    }

    companion object {
        private val logger = LoggerHolder.jobLogger
    }
}

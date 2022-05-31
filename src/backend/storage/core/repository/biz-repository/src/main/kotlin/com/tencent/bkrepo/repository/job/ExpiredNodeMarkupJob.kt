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
import com.tencent.bkrepo.repository.constant.SHARDING_COUNT
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.job.base.CenterNodeJob
import com.tencent.bkrepo.repository.model.TNode
import com.tencent.bkrepo.repository.service.node.NodeService
import org.springframework.data.domain.PageRequest
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime

/**
 * 标记已过期的节点为已删除
 */
@Component
class ExpiredNodeMarkupJob(
    private val nodeDao: NodeDao,
    private val nodeService: NodeService
) : CenterNodeJob() {

    @Scheduled(cron = "0 0 0/6 * * ?") // 凌晨开始，6小时执行一次
    override fun start() {
        super.start()
    }

    override fun run() {
        var markupCount = 0L
        val query = Query.query(Criteria.where(TNode::expireDate.name).lt(LocalDateTime.now()))
        val mongoTemplate = nodeDao.determineMongoTemplate()
        for (sequence in 0 until SHARDING_COUNT) {
            val collectionName = nodeDao.parseSequenceToCollectionName(sequence)
            var page = 0
            query.with(PageRequest.of(page, PAGE_SIZE))
            var deletedNodeList = mongoTemplate.find(query, TNode::class.java, collectionName)
            while (deletedNodeList.isNotEmpty()) {
                logger.info("Retrieved [${deletedNodeList.size}] expired records to be clean up.")
                deletedNodeList.forEach {
                    nodeService.deleteByPath(it.projectId, it.repoName, it.fullPath, it.lastModifiedBy)
                    markupCount += 1
                }
                page += 1
                query.with(PageRequest.of(page, PAGE_SIZE))
                deletedNodeList = mongoTemplate.find(query, TNode::class.java, collectionName)
            }
        }
        logger.info("[$markupCount] nodes were marked up with deleted status.")
    }

    override fun getLockAtMostFor(): Duration = Duration.ofDays(1)

    companion object {
        private val logger = LoggerHolder.jobLogger
        private const val PAGE_SIZE = 1000
    }
}

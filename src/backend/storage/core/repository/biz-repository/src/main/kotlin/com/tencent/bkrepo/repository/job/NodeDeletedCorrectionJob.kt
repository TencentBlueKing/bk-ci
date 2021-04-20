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
import com.tencent.bkrepo.repository.model.TNode
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Component

/**
 * deleted = null -> deleted = 0 的文件
 */
@Component
class NodeDeletedCorrectionJob(
    private val nodeDao: NodeDao
) {

    fun correct() {
        logger.info("Starting to correct node deleted value.")
        var matchedCount = 0L
        var modifiedCount = 0L
        val startTimeMillis = System.currentTimeMillis()
        val mongoTemplate = nodeDao.determineMongoTemplate()
        val query = Query.query(Criteria.where(TNode::deleted.name).`is`(null))
        val update = Update.update(TNode::deleted.name, 0)
        for (sequence in 0 until SHARDING_COUNT) {
            val collectionName = nodeDao.parseSequenceToCollectionName(sequence)
            val updateResult = mongoTemplate.updateMulti(query, update, collectionName)
            matchedCount += updateResult.matchedCount
            modifiedCount += updateResult.modifiedCount
        }
        val elapseTimeMillis = System.currentTimeMillis() - startTimeMillis
        logger.info(
            "matchedCount: $matchedCount, modifiedCount: $modifiedCount, elapse [$elapseTimeMillis] ms totally."
        )
    }

    companion object {
        private val logger = LoggerHolder.jobLogger
    }
}

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

package com.tencent.bkrepo.repository.service

import com.tencent.bkrepo.common.artifact.event.base.EventType
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.common.mongo.dao.util.sharding.MonthRangeShardingUtils
import com.tencent.bkrepo.common.operate.service.dao.OperateLogDao
import com.tencent.bkrepo.common.operate.service.model.TOperateLog
import com.tencent.bkrepo.repository.UT_PROJECT_ID
import com.tencent.bkrepo.repository.UT_REPO_NAME
import com.tencent.bkrepo.repository.UT_USER
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.where
import java.time.LocalDateTime
import java.util.Random
import kotlin.math.abs

@DisplayName("操作日志测试")
@DataMongoTest
@Import(
    OperateLogDao::class
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OperateLogServiceTest @Autowired constructor(
    private val operateLogDao: OperateLogDao,
    private val mongoTemplate: MongoTemplate
): ServiceBaseTest() {

    @BeforeAll
    fun beforeAll() {
        insertOperateLog()
    }

    @AfterAll
    fun afterAll() {
        deleteOperateLog()
    }

    @Test
    fun pageQueryTest() {
        val startDateTime = LocalDateTime.now().minusDays(100)
        val endDateTime = LocalDateTime.now()
        var date = endDateTime.toLocalDate()
        val query = Query(where(TOperateLog::createdDate).gte(startDateTime).lte(endDateTime))
        var pageNumber = 1
        val pageSize = 10
        var count: Int
        do {
            val pageRequest = Pages.ofRequest(pageNumber, pageSize)
            val logs = operateLogDao.find(query.with(pageRequest))
            count = logs.size
            logger.info("count: $count, date: $date")
            if (pageNumber < 11) {
                Assertions.assertEquals(pageSize, count)
                Assertions.assertEquals(date, logs.first().createdDate.toLocalDate())
            }
            pageNumber ++
            date = logs.firstOrNull()?.createdDate?.toLocalDate()?.minusDays(pageSize.toLong())
        } while (count == pageSize)
    }

    private fun insertOperateLog() {
        val sequences = mutableSetOf<Int>()
        for (i in 0..99) {
            val createdDate = LocalDateTime.now().minusDays(i.toLong())
            val sequence = MonthRangeShardingUtils.shardingSequenceFor(createdDate, -1)
            sequences.add(sequence)
            val log = TOperateLog(
                createdDate = createdDate,
                type = EventType.values()[abs(Random().nextInt() % EventType.values().size)],
                projectId = UT_PROJECT_ID,
                repoName = UT_REPO_NAME,
                resourceKey = RandomStringUtils.randomAlphabetic(5),
                userId = UT_USER,
                clientAddress = "127.0.0.1",
                description = emptyMap()
            )
            operateLogDao.insert(log)
        }
        Assertions.assertTrue(sequences.size == 4)
    }

    private fun deleteOperateLog() {
        val query = Query(where(
            TOperateLog::createdDate).gte(LocalDateTime.now().minusDays(100)).lte(LocalDateTime.now()))
        val collectionNames = operateLogDao.determineCollectionNames(query)
        collectionNames.forEach {
            mongoTemplate.dropCollection(it)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OperateLogServiceTest::class.java)
    }
}

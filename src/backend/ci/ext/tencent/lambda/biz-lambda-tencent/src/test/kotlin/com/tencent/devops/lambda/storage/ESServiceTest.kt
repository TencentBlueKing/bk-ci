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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.tencent.devops.lambda.storage

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockito_kotlin.mock
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.es.ESClient
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.lambda.dao.LambdaBuildIndexDao
import org.elasticsearch.client.transport.TransportClient
import org.jooq.DSLContext
import org.junit.Assert.assertEquals
import org.junit.Test
import org.springframework.data.redis.core.RedisTemplate
import java.time.LocalDateTime

class ESServiceTest {

    private val client: TransportClient = mock()
    private val esClient: ESClient = ESClient("esClient", client, false)
    private val redisTemplate: RedisTemplate<String, String> = mock()
    private val redisOperation: RedisOperation = RedisOperation(redisTemplate)
    private val objectMapper: ObjectMapper = mock()
    private val dslContext: DSLContext = mock()
    private val lambdaBuildIndexDao: LambdaBuildIndexDao = mock()
    private val indexService = IndexService(dslContext, lambdaBuildIndexDao, redisOperation)
    private val esService: ESService = ESService(
        esClient = esClient,
        redisOperation = redisOperation,
        indexService = indexService,
        objectMapper = objectMapper
    )

    @Test
    fun testDate() {
        val begin = LocalDateTime.now().minusDays(10).timestamp()
        val end = System.currentTimeMillis() / 1000
        val index = esService.getIndex(begin, end)
        assertEquals(11, index.size)
    }
}
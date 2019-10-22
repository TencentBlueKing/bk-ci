package com.tencent.devops.lambda.storage

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockito_kotlin.mock
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.lambda.dao.BuildIndexDao
import org.elasticsearch.client.transport.TransportClient
import org.jooq.DSLContext
import org.junit.Assert.assertEquals
import org.junit.Test
import org.springframework.data.redis.core.RedisTemplate
import java.time.LocalDateTime

class ESServiceTest {

    private val client: TransportClient = mock()
    private val redisTemplate: RedisTemplate<String, String> = mock()
    private val redisOperation: RedisOperation = RedisOperation(redisTemplate)
    private val objectMapper: ObjectMapper = mock()
    private val dslContext: DSLContext = mock()
    private val buildIndexDao: BuildIndexDao = mock()
    private val indexService = IndexService(dslContext, buildIndexDao, redisOperation)
    private val esService: ESService = ESService(client, redisOperation, indexService, objectMapper)
    @Test
    fun testDate() {
        val begin = LocalDateTime.now().minusDays(10).timestamp()
        val end = System.currentTimeMillis() / 1000
        val index = esService.getIndex(begin, end)
        assertEquals(11, index.size)
    }
}
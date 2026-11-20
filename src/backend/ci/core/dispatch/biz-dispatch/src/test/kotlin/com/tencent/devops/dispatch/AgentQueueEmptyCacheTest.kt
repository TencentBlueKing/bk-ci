package com.tencent.devops.dispatch

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.pojo.AgentResult
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.dao.ThirdPartyAgentBuildDao
import com.tencent.devops.dispatch.pojo.thirdpartyagent.AskEnable
import com.tencent.devops.dispatch.pojo.thirdpartyagent.BuildJobType
import com.tencent.devops.dispatch.pojo.thirdpartyagent.ThirdPartyAskInfo
import com.tencent.devops.dispatch.service.ThirdPartyAgentService
import com.tencent.devops.dispatch.utils.redis.AgentAskCache
import com.tencent.devops.environment.api.thirdpartyagent.ServiceThirdPartyAgentResource
import com.tencent.devops.environment.pojo.thirdpartyagent.AskHeartbeatResponse
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.ws.rs.NotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.test.util.ReflectionTestUtils
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService

class AgentQueueEmptyCacheTest {
    private val redis = mockk<RedisOperation>()
    private val cache = AgentAskCache(redis, 20)
    private val markers = ConcurrentHashMap<String, Pair<String, Long>>()
    private var nowSeconds = 0L

    @BeforeEach
    fun prepare() {
        every { redis.get(any()) } answers {
            markers[firstArg<String>()]?.takeIf { (_, expiresAt) -> nowSeconds < expiresAt }?.first
        }
        every { redis.set(any(), any(), any<Long>()) } answers {
            markers[firstArg()] = secondArg<String>() to (nowSeconds + thirdArg<Long>())
        }
        every { redis.delete(any<String>()) } answers { markers.remove(firstArg<String>()) != null }
    }

    @Test
    fun springUsesTheDefaultTtlForStringMarkers() {
        AnnotationConfigApplicationContext().use { context ->
            context.beanFactory.registerSingleton("redisOperation", redis)
            context.register(AgentAskCache::class.java)
            context.refresh()
            context.getBean(AgentAskCache::class.java).rememberEmpty("a", BuildJobType.ALL)
            verify { redis.set(any(), "ALL", 20L) }
        }
    }

    @Test
    fun knownEmptySkipsOnlyCompatiblePolls() {
        assertFalse(cache.isBuildEmpty("a", BuildJobType.ALL))
        for (type in listOf(BuildJobType.BINARY, BuildJobType.DOCKER, BuildJobType.ALL)) {
            cache.rememberEmpty("a", type)
            for (poll in BuildJobType.values()) {
                val expected = poll != BuildJobType.NONE && (type == BuildJobType.ALL || type == poll)
                assertEquals(expected, cache.isBuildEmpty("a", poll), "$type / $poll")
            }
        }
        assertFalse(cache.isBuildEmpty("b", BuildJobType.ALL))
        cache.invalidateBuild("a")
        assertFalse(cache.isBuildEmpty("a", BuildJobType.ALL))
    }

    @Test
    fun lateEmptyResultExpiresEvenWhenPollsKeepHittingIt() {
        // 模拟回队清缓存后，之前的查空结果才写入。后续轮询不会延长这次写入的 TTL。
        cache.invalidateBuild("a")
        nowSeconds = 3
        cache.rememberEmpty("a", BuildJobType.ALL)
        for (time in listOf(8L, 13L, 22L)) {
            nowSeconds = time
            assertTrue(cache.isBuildEmpty("a", BuildJobType.ALL))
        }
        nowSeconds = 23
        assertFalse(cache.isBuildEmpty("a", BuildJobType.ALL))
        verify(exactly = 1) { redis.set(any(), "ALL", 20L) }
    }

    @Test
    fun disabledInstanceStillInvalidatesOtherInstancesMarkers() {
        cache.rememberEmpty("a", BuildJobType.ALL)
        val disabled = AgentAskCache(redis, 0)
        assertFalse(disabled.isBuildEmpty("a", BuildJobType.ALL))
        disabled.rememberEmpty("a", BuildJobType.DOCKER)
        cache.rememberEmpty("a", BuildJobType.NONE)
        verify(exactly = 0) { redis.get(any()) }
        verify(exactly = 1) { redis.set(any(), any(), any<Long>()) }
        disabled.invalidateBuild("a")
        assertFalse(cache.isBuildEmpty("a", BuildJobType.ALL))
    }

    @Test
    fun redisFailuresLeaveTheQueueUsable() {
        every { redis.get(any()) } throws IllegalStateException("offline")
        every { redis.set(any(), any(), any<Long>()) } throws IllegalStateException("offline")
        every { redis.delete(any<String>()) } throws IllegalStateException("offline")
        assertFalse(cache.isBuildEmpty("a", BuildJobType.ALL))
        cache.rememberEmpty("a", BuildJobType.ALL)
        cache.invalidateBuild("a")
    }

    @Test
    fun askSkipsOnlyTheBuildLookupOnEmptyHits() {
        val client = mockk<Client>()
        val environment = mockk<ServiceThirdPartyAgentResource>()
        val dao = mockk<ThirdPartyAgentBuildDao>()
        val agent = mockk<ThirdPartyAgent>()
        val heartbeat = mockk<AskHeartbeatResponse>()
        val request = ThirdPartyAskInfo(AskEnable("ALL", false, false, false), mockk(), null)
        val service = ThirdPartyAgentService(
            mockk(), cache, mockk(), client, redis, dao, mockk(), mockk(), mockk(), mockk()
        )
        every { client.get(ServiceThirdPartyAgentResource::class) } returns environment
        every { environment.getAgentById("p", "a") } returns AgentResult(AgentStatus.IMPORT_OK, agent)
        every { agent.secretKey } returns "current"
        every { agent.status } returns AgentStatus.IMPORT_OK
        every { heartbeat.agentStatus } returns AgentStatus.IMPORT_OK.name
        every { environment.newHeartbeat("p", "a", "current", request.heartbeat) } returns Result(heartbeat)
        every { redis.getKeyByRedisName(any()) } answers { firstArg() }
        every { redis.setNxEx(any(), any(), any(), true) } returns true
        every { redis.execute<Long>(any(), any(), *anyVararg(), isRedisLock = true) } returns 1L
        every { dao.fetchOneQueueBuild(any(), "a", BuildJobType.ALL) } returns null

        try {
            // 三次 ask 只有首次需要查 agent 和队列，后两次通过心跳分别验证节点删除和密钥失效仍能反映在响应中。
            assertEquals(AgentStatus.IMPORT_OK, service.ask("p", "a", "current", request).agentStatus)
            every { heartbeat.agentStatus } returns AgentStatus.DELETE.name
            val deleted = service.ask("p", "a", "current", request)
            assertEquals(AgentStatus.DELETE, deleted.agentStatus)
            assertNull(deleted.data?.build)
            every { environment.newHeartbeat("p", "a", "revoked", request.heartbeat) } throws
                IllegalStateException("invalid credential")
            assertTrue(service.ask("p", "a", "revoked", request).isNotOk())
            verify(exactly = 3) { environment.newHeartbeat("p", "a", any(), request.heartbeat) }
            verify(exactly = 1) { environment.getAgentById("p", "a") }
            verify(exactly = 1) { dao.fetchOneQueueBuild(any(), "a", BuildJobType.ALL) }

            // 独立领取接口没有心跳，即使缓存命中也校验密钥。
            assertThrows(NotFoundException::class.java) { service.startBuild("p", "a", "revoked", BuildJobType.ALL) }
            every { environment.getAgentById("p", "a") } returns AgentResult(AgentStatus.DELETE, null)
            assertEquals(AgentStatus.DELETE, service.startBuild("p", "a", "current", BuildJobType.ALL).agentStatus)
            verify(exactly = 3) { environment.getAgentById("p", "a") }

            // 模拟入队清除缓存。此时应重新查 agent，发现节点已删除就结束领取，避免把节点删除误记为队列查空。
            cache.invalidateBuild("a")
            service.ask("p", "a", "current", request)
            verify(exactly = 4) { environment.getAgentById("p", "a") }
            verify(exactly = 1) { dao.fetchOneQueueBuild(any(), "a", BuildJobType.ALL) }
            assertFalse(cache.isBuildEmpty("a", BuildJobType.ALL))
        } finally {
            (ReflectionTestUtils.getField(service, "askExecutor") as ExecutorService).shutdownNow()
        }
    }
}

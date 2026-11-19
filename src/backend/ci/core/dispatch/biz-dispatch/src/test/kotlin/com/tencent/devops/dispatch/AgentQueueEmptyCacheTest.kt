package com.tencent.devops.dispatch

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.pojo.AgentResult
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisAutoConfiguration
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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.ws.rs.NotFoundException
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.test.util.ReflectionTestUtils
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService

class AgentQueueEmptyCacheTest {
    private val redis = mockk<RedisOperation>()
    private val cache = AgentAskCache(redis, 20)
    private val hashes = ConcurrentHashMap<String, ConcurrentHashMap<String, String>>()

    @BeforeEach
    fun prepare() {
        every { redis.getKeyByRedisName(any()) } answers { firstArg() }
        every { redis.hget(any(), any()) } answers { hashes[firstArg<String>()]?.get(secondArg<String>()) }
        every { redis.hdelete(any(), any<String>()) } answers {
            hashes[firstArg<String>()]?.remove(secondArg<String>())
            Unit
        }
        // 模拟 Redis 对一次脚本的原子执行，测试可以在真实查询中插入入队或第二次探测。
        every { redis.execute<Long>(any(), any(), *anyVararg()) } answers {
            val key = secondArg<List<String>>().single()
            val args = arg<Array<Any?>>(2)
            val script = firstArg<RedisScript<Long>>().scriptAsString
            when {
                "'PTTL'" in script -> {
                    hashes.computeIfAbsent(key) { ConcurrentHashMap() }[args[0] as String] = args[1] as String
                    1L
                }
                "'HGET'" in script -> {
                    val replaced = hashes[key]?.replace(args[0] as String, args[1] as String, args[2] as String)
                    if (replaced == true) 1L else 0L
                }
                else -> error("Unexpected Redis script")
            }
        }
    }

    @Test
    fun springWiresHashFieldsAndScriptArgumentsWithIdenticalBytes() {
        AnnotationConfigApplicationContext().use { context ->
            context.beanFactory.registerSingleton("redisConnectionFactory", mockk<RedisConnectionFactory>())
            context.beanFactory.registerSingleton("meterRegistry", SimpleMeterRegistry())
            context.register(RedisAutoConfiguration::class.java, AgentAskCache::class.java)
            context.refresh()
            val operation = ReflectionTestUtils.getField(context.getBean(AgentAskCache::class.java), "redis")
            assertSame(context.getBean("redisStringHashOperation"), operation)
            val template = ReflectionTestUtils.getField(operation!!, "masterRedisTemplate") as RedisTemplate<*, *>
            // 脚本参数、Hash 字段和 Hash 值分别经不同 serializer；三者均需和 Lua 的原始字节互通。
            for (serializer in listOf(template.valueSerializer, template.hashKeySerializer, template.hashValueSerializer)) {
                @Suppress("UNCHECKED_CAST")
                val strings = serializer as RedisSerializer<String>
                for (value in listOf("build", "123456:ALL")) {
                    assertArrayEquals(value.toByteArray(), strings.serialize(value))
                }
            }
        }
    }

    @Test
    fun knownEmptySkipsOnlyCompatiblePolls() {
        assertFalse(cache.isBuildEmpty("a", BuildJobType.ALL))
        assertNull(cache.queryAndRememberEmpty<String>("a", BuildJobType.BINARY) { null })
        assertTrue(cache.isBuildEmpty("a", BuildJobType.BINARY))
        assertFalse(cache.isBuildEmpty("a", BuildJobType.DOCKER))
        assertFalse(cache.isBuildEmpty("a", BuildJobType.ALL))
        assertEquals("docker", cache.queryAndRememberEmpty("a", BuildJobType.DOCKER) { "docker" })
        assertNull(cache.queryAndRememberEmpty<String>("a", BuildJobType.ALL) { null })
        assertTrue(cache.isBuildEmpty("a", BuildJobType.DOCKER))
        assertFalse(cache.isBuildEmpty("b", BuildJobType.ALL))
        cache.invalidateBuild("a")
        assertFalse(cache.isBuildEmpty("a", BuildJobType.ALL))
    }

    @Test
    fun staleEmptyQueryCannotOverwriteEnqueueOrAnotherProbe() {
        // 外层 ALL 查询最后才返回空。期间任务入队，另一次 Docker 轮询已经领到它并换掉了 UUID。
        // 如果外层还能写回 ALL，接下来的轮询就会错误地跳过所有任务类型的查询。
        cache.queryAndRememberEmpty<String>("a", BuildJobType.ALL) {
            cache.invalidateBuild("a")
            assertEquals("job", cache.queryAndRememberEmpty("a", BuildJobType.DOCKER) { "job" })
            null
        }
        assertFalse(cache.isBuildEmpty("a", BuildJobType.ALL))
    }

    @Test
    fun cacheFailureAndDisabledCacheFallBackToTheClaim() {
        every { redis.hget(any(), any()) } throws IllegalStateException("offline")
        every { redis.execute<Long>(any(), any(), *anyVararg()) } throws IllegalStateException("offline")
        assertFalse(cache.isBuildEmpty("a", BuildJobType.ALL))
        assertEquals("job", cache.queryAndRememberEmpty("a", BuildJobType.ALL) { "job" })
        val disabled = AgentAskCache(mockk(), 0)
        assertFalse(disabled.isBuildEmpty("a", BuildJobType.ALL))
        assertEquals("job", disabled.queryAndRememberEmpty("a", BuildJobType.ALL) { "job" })
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

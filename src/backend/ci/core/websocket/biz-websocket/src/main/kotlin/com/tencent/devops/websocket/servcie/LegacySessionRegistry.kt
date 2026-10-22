package com.tencent.devops.websocket.servcie

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.utils.WsRedisUtils
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.data.redis.core.script.DefaultRedisScript
import java.util.UUID
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

@Component
class LegacySessionRegistry(private val redisOperation: RedisOperation) {
    private data class Connection(
        val userId: String,
        val socket: WebSocketSession,
        val token: String = UUID.randomUUID().toString(),
        @Volatile var lastIndexRefresh: Long = 0,
        @Volatile var page: String? = null
    )

    private val connections = ConcurrentHashMap<String, Connection>()

    fun connected(sessionId: String, userId: String, socket: WebSocketSession) {
        var previous: Connection? = null
        withSessionLock(sessionId) {
            val owner = WsRedisUtils.getUserBySession(redisOperation, sessionId)
            require(owner == null || owner == userId) { "Session belongs to another user" }
            val connection = Connection(userId, socket)
            previous = connections.put(sessionId, connection)
            redisOperation.set(ownerKey(sessionId), connection.token, LEASE_SECONDS, true)
            renew(sessionId, connection)
        }
        previous?.socket?.takeIf { it !== socket && it.isOpen }?.close(CloseStatus.NORMAL)
    }

    fun disconnected(sessionId: String, socketId: String) {
        val connection = connections[sessionId] ?: return
        if (connection.socket.id != socketId || !connections.remove(sessionId, connection)) return
        withSessionLock(sessionId) {
            if (redisOperation.get(ownerKey(sessionId)) == connection.token) {
                clean(sessionId, connection.userId)
            }
        }
    }

    fun isLocal(sessionId: String): Boolean = connections[sessionId]?.socket?.isOpen == true

    fun isManaged(sessionId: String): Boolean = redisOperation.get(ownerKey(sessionId)) != null

    fun isOwner(sessionId: String, userId: String, socketId: String? = null): Boolean {
        val connection = connections[sessionId] ?: return false
        return connection.userId == userId && connection.socket.isOpen &&
            (socketId == null || connection.socket.id == socketId)
    }

    fun pageChanged(sessionId: String, page: String?) {
        connections[sessionId]?.page = page
    }

    @Scheduled(fixedDelay = 30000)
    fun refreshConnections() {
        connections.forEach { (sessionId, connection) ->
            try {
                if (!connection.socket.isOpen) {
                    disconnected(sessionId, connection.socket.id)
                } else {
                    val renewed = redisOperation.execute(
                        RENEW_SCRIPT, listOf(ownerKey(sessionId)), connection.token, LEASE_SECONDS.toString()
                    ) == 1L
                    if (!renewed) {
                        connection.socket.close(CloseStatus.SERVICE_RESTARTED)
                    } else {
                        redisOperation.zadd(EXPIRY_KEY, sessionId,
                            (System.currentTimeMillis() + LEASE_SECONDS * 1000).toDouble())
                        if (System.currentTimeMillis() - connection.lastIndexRefresh > 120000) {
                            withSessionLock(sessionId) {
                                if (connections[sessionId] === connection &&
                                    redisOperation.get(ownerKey(sessionId)) == connection.token
                                ) renew(sessionId, connection)
                            }
                        }
                    }
                }
            } catch (exception: Exception) {
                logger.warn("Unable to renew legacy websocket session", exception)
            }
        }
        expireConnections()
    }

    fun expireConnections() {
        val now = System.currentTimeMillis().toDouble()
        try {
            redisOperation.zrange(EXPIRY_KEY, 0, 255)?.forEach { sessionId ->
                val score = redisOperation.zscore(EXPIRY_KEY, sessionId) ?: return@forEach
                if (score > now) return@forEach
                withSessionLock(sessionId) {
                    val deadline = redisOperation.zscore(EXPIRY_KEY, sessionId) ?: return@withSessionLock
                    if (deadline <= now && !isManaged(sessionId)) {
                        clean(sessionId, WsRedisUtils.getUserBySession(redisOperation, sessionId))
                    }
                }
            }
        } catch (exception: Exception) {
            logger.warn("Unable to expire legacy websocket sessions", exception)
        }
    }

    private fun renew(sessionId: String, connection: Connection) {
        redisOperation.expire(ownerKey(sessionId), LEASE_SECONDS)
        redisOperation.zadd(EXPIRY_KEY, sessionId, (System.currentTimeMillis() + LEASE_SECONDS * 1000).toDouble())
        WsRedisUtils.writeSessionIdByRedis(redisOperation, connection.userId, sessionId)
        connection.page?.let { page ->
            WsRedisUtils.refreshSessionPage(redisOperation, sessionId, page)
            WsRedisUtils.refreshPageSession(redisOperation, sessionId, page)
        }
        connection.lastIndexRefresh = System.currentTimeMillis()
    }

    private fun clean(sessionId: String, userId: String?) {
        WsRedisUtils.getPageFromSessionPageBySession(redisOperation, sessionId)?.let { page ->
            WsRedisUtils.cleanPageSessionBySessionId(redisOperation, page, sessionId)
        }
        WsRedisUtils.cleanSessionPageBySessionId(redisOperation, sessionId)
        if (userId != null) WsRedisUtils.cleanUserSessionBySessionId(redisOperation, userId, sessionId)
        WsRedisUtils.deleteUserSessionBySession(redisOperation, sessionId)
        redisOperation.delete(ownerKey(sessionId))
        redisOperation.zremove(EXPIRY_KEY, sessionId)
    }

    private fun withSessionLock(sessionId: String, action: () -> Unit) {
        RedisLock(redisOperation, "websocket:changeStatus:$sessionId", 10L).use { lock ->
            lock.lock()
            action()
        }
    }

    companion object {
        const val SESSION_ATTRIBUTE = "bk.websocket.session"
        const val USER_ATTRIBUTE = "bk.websocket.user"
        private const val LEASE_SECONDS = 180L
        private const val EXPIRY_KEY = "WebSocket:legacy:expiry:v2"
        private val RENEW_SCRIPT = DefaultRedisScript(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
                "return redis.call('EXPIRE', KEYS[1], ARGV[2]) else return 0 end",
            Long::class.java
        )
        private val logger = LoggerFactory.getLogger(LegacySessionRegistry::class.java)
        fun ownerKey(sessionId: String) = "WebSocket:legacy:owner:v2:$sessionId"
    }
}

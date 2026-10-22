package com.tencent.devops.websocket.v2

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORIGINAL_HOST
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.websocket.v2.WsInvalidation
import com.tencent.devops.common.websocket.v2.WsTopic
import com.tencent.devops.common.websocket.v2.WsV2Properties
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.CopyOnWriteArrayList

class WsV2Test {
    @Test
    fun `topics validate resource identifiers and attempts`() {
        assertNotNull(WsTopic.parse("ps:project:p-1"))
        assertNotNull(WsTopic.parse("pr:project:p-1:b-1:latest"))
        assertEquals(2, WsTopic.parse("pr:project:p-1:b-1:attempt:2")?.executeCount)
        listOf("ps:p:*", "ps:p:p:extra", "pr:p:p:b:attempt:0", "pr:p:p:b:attempt:01",
            "pr:p:p:b:attempt:-1", "pr:p:p:b:attempt:2147483648", "notify:other:user").forEach {
            assertNull(WsTopic.parse(it), it)
        }
        val latest = WsTopic.parse("pr:p:p:b:latest")!!
        val attempt = WsTopic.parse("pr:p:p:b:attempt:2")!!
        assertEquals(1, latest.routingKeys().intersect(attempt.routingKeys()).size)
        assertFalse(WsTopic.routingKey("ps:p:a") == WsTopic.routingKey("ps:p:b"))
    }

    @Test
    fun `handshake requires identity and explicit valid origin`() {
        val properties = WsV2Properties().apply { allowedOrigins = listOf("https://ci.example.test") }
        val interceptor = WsV2HandshakeInterceptor(properties)
        val headers = HttpHeaders()
        val request = mockk<ServerHttpRequest>()
        every { request.headers } returns headers
        val response = mockk<ServerHttpResponse>(relaxed = true)
        val handler = mockk<WebSocketHandler>()
        assertFalse(interceptor.beforeHandshake(request, response, handler, mutableMapOf()))
        headers.set(AUTH_HEADER_DEVOPS_USER_ID, "test-user")
        headers.origin = "https://untrusted.example.test"
        assertFalse(interceptor.beforeHandshake(request, response, handler, mutableMapOf()))
        headers.origin = "https://ci.example.test"
        val attributes = mutableMapOf<String, Any>()
        assertTrue(interceptor.beforeHandshake(request, response, handler, attributes))
        assertEquals("test-user", attributes[WsV2Handler.USER_ATTRIBUTE])
    }

    @Test
    fun `empty origins follow the forwarded public host`() {
        val properties = WsV2Properties()
        val interceptor = WsV2HandshakeInterceptor(properties)
        val headers = HttpHeaders()
        val request = mockk<ServerHttpRequest>()
        every { request.headers } returns headers
        val response = mockk<ServerHttpResponse>(relaxed = true)
        val handler = mockk<WebSocketHandler>()
        headers.set(AUTH_HEADER_DEVOPS_USER_ID, "test-user")
        headers.origin = "https://ci.example.test"
        headers.set("X-Forwarded-Proto", "https,http")
        headers.set("X-Forwarded-Host", "ci.example.test,proxy.internal")
        assertTrue(interceptor.beforeHandshake(request, response, handler, mutableMapOf()))
        headers.origin = "https://other.example.test"
        assertFalse(interceptor.beforeHandshake(request, response, handler, mutableMapOf()))
        properties.allowedOrigins = listOf("")
        headers.origin = "https://ci.example.test"
        assertTrue(interceptor.beforeHandshake(request, response, handler, mutableMapOf()))
        headers.set("X-Forwarded-Proto", "http")
        headers.set("X-Forwarded-Host", "ci.example.test:80")
        assertTrue(interceptor.beforeHandshake(request, response, handler, mutableMapOf()))
        headers.set(AUTH_HEADER_DEVOPS_ORIGINAL_HOST, "ci.example.test")
        headers.set("X-Forwarded-Host", "proxy.internal")
        assertTrue(interceptor.beforeHandshake(request, response, handler, mutableMapOf()))
        headers.origin = "https://ci.example.test:8443"
        assertFalse(interceptor.beforeHandshake(request, response, handler, mutableMapOf()))
    }

    @Test
    fun `subscriptions isolate generations and forbidden topics and release bindings`() {
        val broker = FakeBroker()
        val properties = WsV2Properties().apply { mergeWindowMs = 10 }
        val handler = WsV2Handler(properties, broker, WsTopicAuthorizer { _, topics ->
            topics.filter { it.pipelineId != "denied" }.map { it.value }.toSet()
        })
        val socket = mockk<WebSocketSession>(relaxed = true)
        val messages = CopyOnWriteArrayList<String>()
        every { socket.id } returns "connection"
        every { socket.isOpen } returns true
        every { socket.attributes } returns mutableMapOf(WsV2Handler.USER_ATTRIBUTE to "test-user")
        every { socket.sendMessage(any()) } answers {
            messages.add((firstArg<WebSocketMessage<*>>() as TextMessage).payload)
            Unit
        }
        try {
            handler.afterConnectionEstablished(socket)
            handler.handleMessage(socket, TextMessage("""{"v":2,"op":"sub","requestId":"r1","generation":"g1","topics":["ps:p:allowed","ps:p:denied"]}"""))
            await { messages.any { it.contains("sub_ack") } }
            val ack = messages.map { JsonUtil.getObjectMapper(false).readTree(it) }.first { it.path("op").asText() == "sub_ack" }
            assertEquals(listOf("ps:p:allowed"), ack.path("accepted").map { it.asText() })
            assertEquals("FORBIDDEN", ack.path("rejected").path("ps:p:denied").asText())
            assertEquals(1, broker.keys.size)
            handler.handleMessage(socket, TextMessage("""{"v":2,"op":"unsub","requestId":"old","generation":"stale","topics":["ps:p:allowed"]}"""))
            await { messages.any { it.contains("unsub_ack") } }
            assertEquals(1, broker.keys.size)
            broker.message(WsTopic.routingKey("ps:p:allowed"), WsInvalidation("ps:p:allowed"))
            await { messages.any { it.contains("invalidate") } }
            handler.afterConnectionClosed(socket, CloseStatus.NORMAL)
            assertTrue(broker.keys.isEmpty())
        } finally {
            handler.shutdown()
        }
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 3_000_000_000
        while (!condition() && System.nanoTime() < deadline) Thread.sleep(5)
        assertTrue(condition(), "Timed out waiting for websocket state")
    }

    private class FakeBroker : WsSubscriptionBroker {
        override val ready = true
        val keys = mutableSetOf<String>()
        var message: (String, WsInvalidation) -> Unit = { _, _ -> }
        override fun bind(keys: Set<String>) { this.keys.addAll(keys) }
        override fun unbind(keys: Set<String>) { this.keys.removeAll(keys) }
        override fun listen(onMessage: (String, WsInvalidation) -> Unit, onReset: () -> Unit) { message = onMessage }
    }
}

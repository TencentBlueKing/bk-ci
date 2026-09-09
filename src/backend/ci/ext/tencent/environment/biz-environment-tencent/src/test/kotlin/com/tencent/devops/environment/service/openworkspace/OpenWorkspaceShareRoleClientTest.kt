package com.tencent.devops.environment.service.openworkspace

import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.environment.pojo.workspace.WorkspaceShareRoleQueryItem
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OpenWorkspaceShareRoleClientTest {

    private lateinit var client: OpenWorkspaceShareRoleClient

    @BeforeEach
    fun setUp() {
        client = OpenWorkspaceShareRoleClient(
            baseUrl = "https://cds-sz.bkdevops.qq.com",
            appId = "test-app",
            secret = "test-secret"
        )
        mockkObject(OkhttpUtils)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `empty items return empty list without http`() {
        val result = client.queryShareAccessRoles(emptyList())

        assertEquals(emptyList<Any>(), result)
        verify(exactly = 0) { OkhttpUtils.doHttp(any()) }
    }

    @Test
    fun `status 0 parses roles including null`() {
        val parsed = client.parseResponse(
            """
            {
              "status": 0,
              "data": [
                {"userId": "alice", "workspaceName": "ws-1", "role": "admin"},
                {"userId": "bob", "workspaceName": "ws-1", "role": null}
              ]
            }
            """.trimIndent()
        )

        assertEquals(2, parsed?.size)
        assertEquals("admin", parsed?.get(0)?.role)
        assertEquals("alice", parsed?.get(0)?.userId)
        assertEquals("ws-1", parsed?.get(0)?.workspaceName)
        assertEquals("bob", parsed?.get(1)?.userId)
        assertNull(parsed?.get(1)?.role)
    }

    @Test
    fun `status not 0 is fail closed`() {
        val parsed = client.parseResponse(
            """{"status": 2100001, "message": "没有权限请求该接口"}"""
        )

        assertNull(parsed)
    }

    @Test
    fun `http error is fail closed`() {
        every { OkhttpUtils.doHttp(any()) } returns buildErrorResponse(500, "Internal Server Error")

        val result = client.queryShareAccessRoles(
            listOf(WorkspaceShareRoleQueryItem(userId = "alice", workspaceName = "ws-1"))
        )

        assertNull(result)
    }

    @Test
    fun `http success returns parsed roles`() {
        every { OkhttpUtils.doHttp(any()) } returns buildOkResponse(
            """
            {
              "status": 0,
              "data": [
                {"userId": "alice", "workspaceName": "ws-1", "role": "collaborator"}
              ]
            }
            """.trimIndent()
        )

        val result = client.queryShareAccessRoles(
            listOf(WorkspaceShareRoleQueryItem(userId = "alice", workspaceName = "ws-1"))
        )

        assertEquals(1, result?.size)
        assertEquals("collaborator", result?.first()?.role)
        assertTrue(result != null)
    }

    private fun buildOkResponse(body: String): Response {
        return Response.Builder()
            .code(200)
            .message("OK")
            .protocol(Protocol.HTTP_1_1)
            .request(Request.Builder().url("https://cds-sz.bkdevops.qq.com/ai/api/open/workspace/query").build())
            .body(body.toResponseBody(null))
            .build()
    }

    private fun buildErrorResponse(code: Int, message: String): Response {
        return Response.Builder()
            .code(code)
            .message(message)
            .protocol(Protocol.HTTP_1_1)
            .request(Request.Builder().url("https://cds-sz.bkdevops.qq.com/ai/api/open/workspace/query").build())
            .body("".toResponseBody(null))
            .build()
    }
}

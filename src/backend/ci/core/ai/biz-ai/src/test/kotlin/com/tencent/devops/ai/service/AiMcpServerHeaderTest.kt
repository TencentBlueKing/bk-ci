/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.ai.service

import com.tencent.devops.ai.pojo.AiMcpServerInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AiMcpServerHeaderTest {

    @Test
    fun `existing headers without placeholder remain unchanged`() {
        val configured = """{"Authorization":"Bearer existing-pat"}"""

        val headers = AiMcpServerService.resolveHeaders(config(configured), "tester")

        assertEquals(mapOf("Authorization" to "Bearer existing-pat"), headers)
    }

    @Test
    fun `explicit user placeholder is replaced in nested gateway json`() {
        val headers = AiMcpServerService.resolveHeaders(
            config(
                """{"X-Bkapi-Authorization":"{\"bk_app_code\":\"bk-ci\",\"bk_app_secret\":\"secret\",""" +
                    """\"bk_username\":\"{{userId}}\"}"}"""
            ),
            "tester"
        )

        assertEquals(
            """{"bk_app_code":"bk-ci","bk_app_secret":"secret","bk_username":"tester"}""",
            headers["X-Bkapi-Authorization"]
        )
    }

    @Test
    fun `placeholder config fails closed when user is unavailable`() {
        listOf(null, "", "   ", "unknown").forEach { operator ->
            assertThrows(IllegalStateException::class.java) {
                AiMcpServerService.resolveHeaders(
                    config("""{"X-User":"{{userId}}"}"""),
                    operator
                )
            }
        }
    }

    @Test
    fun `operator is sanitized before interpolation`() {
        val headers = AiMcpServerService.resolveHeaders(
            config("""{"X-User":"{{userId}}"}"""),
            "tester\nforged"
        )

        assertEquals("tester forged", headers["X-User"])
    }

    @Test
    fun `empty configuration remains empty`() {
        val headers = AiMcpServerService.resolveHeaders(config(null), "tester")

        assertEquals(emptyMap<String, String>(), headers)
    }

    private fun config(headers: String?) = AiMcpServerInfo(
        id = "id",
        scope = "SYSTEM",
        userId = null,
        serverName = "codecc",
        serverUrl = "https://example.com/mcp",
        transportType = "SSE",
        headers = headers,
        bindAgent = "codecc_agent",
        enabled = true,
        createdTime = 0L,
        updatedTime = 0L
    )
}

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

package com.tencent.devops.process.engine.utils

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReviewNotifyPlaceholderTest {

    @Test
    fun replacesPipelineVariablesAndKeepsUnknownTokens() {
        val params = mutableMapOf(
            "reviewDesc" to "用户【\${managerUserId}】申请【\${managerNames}】，" +
                "有效期\${{ variables.timeout }}分钟，备注\${{ci.remark}}，未知\${{not.exist}}",
            "reviewParams" to """
                [
                  {"key":"BK_CI_BUILD_REMARK","value":"${'$'}{{ci.remark}}","chineseName":"备注","desc":"过期${'$'}{{variables.timeout}}"},
                  {"key":"miss","value":"${'$'}{{missing.var}}","chineseName":"未定义${'$'}{noSuchVar}"}
                ]
            """.trimIndent(),
            "reviewUrl" to "http://example.com/\${{ci.remark}}"
        )
        ReviewNotifyPlaceholder.replace(
            params,
            mapOf(
                "managerUserId" to "user-a",
                "managerNames" to "reporter",
                "timeout" to "30",
                "BK_CI_BUILD_REMARK" to "发布备注"
            )
        )
        assertEquals(
            "用户【user-a】申请【reporter】，有效期30分钟，备注发布备注，未知\${{not.exist}}",
            params["reviewDesc"]
        )
        assertEquals("http://example.com/\${{ci.remark}}", params["reviewUrl"])
        val parsed = JsonUtil.to(params["reviewParams"]!!, object : TypeReference<List<Map<String, Any?>>>() {})
        assertEquals("发布备注", parsed[0]["value"])
        assertEquals("过期30", parsed[0]["desc"])
        assertEquals("\${{missing.var}}", parsed[1]["value"])
        assertEquals("未定义\${noSuchVar}", parsed[1]["chineseName"])
        assertTrue(params["reviewParams"]!!.contains("发布备注"))
    }
}

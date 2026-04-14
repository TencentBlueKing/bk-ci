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

package com.tencent.devops.gpt.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.gpt.service.config.AiCreativeBotConfig
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 蓝鲸 AI Creative Bot 客户端
 * 通过 APIGW 调用智能体生成流水线摘要，支持 SSE 流式和非流式响应
 */
@Service
class AiCreativeBotClient @Autowired constructor(
    private val aiCreativeBotConfig: AiCreativeBotConfig
) {

    /**
     * 调用 AI Creative Bot 生成流水线摘要
     * 使用非流式方式收集完整响应，从 SSE 事件中提取 TEXT_MESSAGE_CONTENT 的 delta 拼接为最终摘要
     *
     * @param userId 操作用户，用于设置 X-BKAIDEV-USER 请求头
     * @param pipelineModelJson 流水线完整编排 JSON
     * @return AI 生成的摘要文本，若调用失败则返回 null
     */
    fun generatePipelineSummary(userId: String, pipelineModelJson: String): String? {
        try {
            val prompt = buildPrompt(pipelineModelJson)
            val requestBody = buildRequestBody(prompt)
            return callApiAndExtractSummary(userId, requestBody)
        } catch (e: Exception) {
            logger.error("Failed to call AI Creative Bot for pipeline summary", e)
            return null
        }
    }

    private fun buildPrompt(pipelineModelJson: String): String {
        return """请为以下 CI/CD 流水线编排生成一段简洁的中文摘要，流水线编排JSON：
$pipelineModelJson"""
    }

    private fun buildRequestBody(prompt: String): String {
        val objectMapper = ObjectMapper()
        val body = mapOf(
            "input" to prompt,
            "chat_history" to emptyList<Any>(),
            "execute_kwargs" to mapOf(
                "stream" to true
            )
        )
        return objectMapper.writeValueAsString(body)
    }

    /**
     * 发起 HTTP 请求并从 SSE 流式响应中提取摘要文本
     *
     * @param userId 操作用户，用于 X-BKAIDEV-USER 请求头
     * @param requestBody 请求体 JSON
     */
    private fun callApiAndExtractSummary(userId: String, requestBody: String): String? {
        val objectMapper = ObjectMapper()
        val connection = createConnection(objectMapper, userId)
        try {
            sendRequestBody(connection, requestBody)
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                logErrorResponse(connection, responseCode)
                return null
            }
            return parseSseStream(connection.inputStream, objectMapper)
        } finally {
            connection.disconnect()
        }
    }

    private fun createConnection(objectMapper: ObjectMapper, userId: String): HttpURLConnection {
        val url = URL(aiCreativeBotConfig.apiUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty(
            "X-Bkapi-Authorization",
            objectMapper.writeValueAsString(
                mapOf(
                    "bk_app_code" to aiCreativeBotConfig.appCode,
                    "bk_app_secret" to aiCreativeBotConfig.appSecret
                )
            )
        )
        connection.setRequestProperty("X-BKAIDEV-USER", userId)
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        return connection
    }

    private fun sendRequestBody(connection: HttpURLConnection, requestBody: String) {
        connection.outputStream.use { os ->
            os.write(requestBody.toByteArray(Charsets.UTF_8))
            os.flush()
        }
    }

    private fun logErrorResponse(connection: HttpURLConnection, responseCode: Int) {
        val errorBody = connection.errorStream?.let {
            BufferedReader(InputStreamReader(it)).readText()
        }
        logger.error("AI Creative Bot API returned HTTP $responseCode, error: $errorBody")
    }

    private fun parseSseStream(inputStream: InputStream, objectMapper: ObjectMapper): String? {
        val summaryBuilder = StringBuilder()
        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val result = processSseLine(objectMapper, line!!)
                if (result == null) return null
                if (result.isNotEmpty()) summaryBuilder.append(result)
            }
        }
        val summary = summaryBuilder.toString().trim()
        return if (summary.isNotEmpty()) summary else null
    }

    /**
     * @return delta 文本内容；空字符串表示跳过该行；null 表示遇到错误应终止
     */
    private fun processSseLine(objectMapper: ObjectMapper, line: String): String? {
        val trimmedLine = line.trim()
        if (!trimmedLine.startsWith("data:")) return ""
        val jsonStr = trimmedLine.removePrefix("data:").trim()
        if (jsonStr.isEmpty()) return ""

        return try {
            val eventNode = objectMapper.readTree(jsonStr)
            when (eventNode.get("type")?.asText()) {
                "TEXT_MESSAGE_CONTENT" -> eventNode.get("delta")?.asText() ?: ""
                "RUN_ERROR" -> {
                    logger.error("AI Creative Bot run error: ${eventNode.get("message")?.asText()}")
                    null
                }
                else -> ""
            }
        } catch (ignored: Exception) {
            logger.debug("Skip unparseable SSE line: $jsonStr")
            ""
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AiCreativeBotClient::class.java)
        private const val CONNECT_TIMEOUT_MS = 30_000
        private const val READ_TIMEOUT_MS = 120_000
    }
}

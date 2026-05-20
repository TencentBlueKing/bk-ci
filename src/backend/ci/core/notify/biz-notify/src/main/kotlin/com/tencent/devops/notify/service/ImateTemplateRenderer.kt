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

package com.tencent.devops.notify.service

import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

/**
 * IMate 模板渲染器（创作流）。
 *
 * - 模板源：classpath `templates/imate/{templateCode}.html`，由 stream 后台维护和发版；
 * - 渲染：`{{var}}` 形式占位符做字符串替换，缺失变量替换为空字符串；
 * - 缓存：模板文件读后常驻进程内存（模板数量少且只在发版时变化，不需要 TTL/驱逐）；
 * - 业务方在 IMate 后台不再需要约定模板编码，bk-ci 这边发的就是已渲染好的最终 HTML。
 */
@Component
class ImateTemplateRenderer {

    private val templateCache: MutableMap<String, String> = ConcurrentHashMap()
    private val missingMarker: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /**
     * 加载并渲染 [templateCode] 对应的模板。
     * 模板加载失败时返回 null（调用方决定是 silent fallback 还是直接放弃发送）。
     */
    fun render(templateCode: String, params: Map<String, String?>): String? {
        val raw = loadTemplate(templateCode) ?: return null
        return PLACEHOLDER_REGEX.replace(raw) { mr ->
            val key = mr.groupValues[1].trim()
            params[key].orEmpty()
        }
    }

    private fun loadTemplate(templateCode: String): String? {
        templateCache[templateCode]?.let { return it }
        if (templateCode in missingMarker) return null

        // templateCode 仅允许字母、数字、下划线，避免 path traversal
        if (!SAFE_NAME_REGEX.matches(templateCode)) {
            logger.warn("[IMATE_TPL] illegal templateCode=$templateCode, refuse to load")
            missingMarker.add(templateCode)
            return null
        }
        val resource = ClassPathResource("templates/imate/$templateCode.html")
        if (!resource.exists()) {
            logger.info("[IMATE_TPL] template not found templateCode=$templateCode")
            missingMarker.add(templateCode)
            return null
        }
        val raw = try {
            resource.inputStream.use { it.readBytes().toString(StandardCharsets.UTF_8) }
        } catch (e: Exception) {
            logger.warn("[IMATE_TPL] read failed templateCode=$templateCode", e)
            return null
        }
        templateCache[templateCode] = raw
        return raw
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ImateTemplateRenderer::class.java)
        private val PLACEHOLDER_REGEX = Regex("""\{\{([^{}]+)}}""")
        private val SAFE_NAME_REGEX = Regex("""[A-Za-z0-9_]+""")
    }
}

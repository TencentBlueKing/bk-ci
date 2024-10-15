/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.devops.common.api.util

import java.util.regex.Matcher
import java.util.regex.Pattern

object ReplacementUtils {

    fun replace(command: String, replacement: KeyReplacement): String {
        return replace(command, replacement, emptyMap())
    }

    fun replace(
        command: String,
        replacement: KeyReplacement,
        contextMap: Map<String, String>? = emptyMap()
    ): String {
        if (command.isBlank()) {
            return command
        }
        val sb = StringBuilder()

        val lines = command.lines()
        lines.forEachIndexed { index, line ->
            // 忽略注释
            val template = if (line.trim().startsWith("#")) {
                line
            } else {
                parseTemplate(line, replacement, contextMap)
            }
            sb.append(template)
            if (index != lines.size - 1) {
                sb.append("\n")
            }
        }
        return sb.toString()
    }

    private fun parseTemplate(
        command: String,
        replacement: KeyReplacement,
        contextMap: Map<String, String>?,
        depth: Int = 1
    ): String {
        if (depth < 0) {
            return command
        }
        val matcher = tPattern.matcher(command)
        val buff = StringBuffer()
        while (matcher.find()) {
            val key = (matcher.group("single") ?: matcher.group("double")).trim()
            var value = replacement.getReplacement(key) ?: contextMap?.get(key)
            if (value == null) {
                value = matcher.group()
            } else {
                if (depth > 0 && tPattern.matcher(value).find()) {
                    value = parseTemplate(value, replacement, contextMap, depth = depth - 1)
                }
            }
            matcher.appendReplacement(buff, Matcher.quoteReplacement(value))
        }
        matcher.appendTail(buff)
        return buff.toString()
    }

    private val tPattern = Pattern.compile("(\\$[{](?<single>[^$^{}]+)})|(\\$[{]{2}(?<double>[^$^{}]+)[}]{2})")
}

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

package com.tencent.devops.common.api.util

import java.util.regex.Matcher
import java.util.regex.Pattern

@Suppress("LongParameterList")
object EnvUtils {

    fun parseEnv(
        command: String?,
        data: Map<String, String>,
        replaceWithEmpty: Boolean = false,
        isEscape: Boolean = false
    ): String {
        return parseEnv(command, data, replaceWithEmpty, isEscape, emptyMap())
    }

    fun parseEnv(
        command: String?,
        data: Map<String, String>,
        replaceWithEmpty: Boolean = false,
        isEscape: Boolean = false,
        contextMap: Map<String, String> = emptyMap()
    ): String {
        if (command.isNullOrBlank()) {
            return command ?: ""
        }
        return parseTokenTwice(command, data, contextMap, replaceWithEmpty, isEscape)
    }

    private fun parseTokenTwice(
        command: String,
        data: Map<String, String>,
        contextMap: Map<String, String>?,
        replaceWithEmpty: Boolean = false,
        isEscape: Boolean = false,
        depth: Int = 1
    ): String {
        if (depth < 0) {
            return command
        }
        val matcher = tPattern.matcher(command)
        val buff = StringBuffer()
        while (matcher.find()) {
            val key = (matcher.group("single") ?: matcher.group("double")).trim()
            var value = data[key] ?: contextMap?.get(key)
            if (value == null) {
                value = if (!replaceWithEmpty) matcher.group() else ""
            } else {
                if (depth > 0 && tPattern.matcher(value).find()) {
                    value = parseTokenTwice(value, data, contextMap, replaceWithEmpty, isEscape, depth = depth - 1)
                } else if (isEscape) {
                    value = escapeSpecialWord(value)
                }
            }
            matcher.appendReplacement(buff, Matcher.quoteReplacement(value))
        }
        matcher.appendTail(buff)
        return buff.toString()
    }

    private val tPattern = Pattern.compile("(\\$[{](?<single>[^$^{}]+)})|(\\$[{]{2}(?<double>[^$^{}]+)[}]{2})")

    private fun escapeSpecialWord(keyword: String): String {
        var replaceWord = keyword
        if (keyword.isNotBlank()) {
            val wordList = listOf("\\", "\"")
            wordList.forEach {
                if (replaceWord.contains(it)) {
                    replaceWord = replaceWord.replace(it, "\\" + it)
                }
            }
        }
        return replaceWord
    }
}

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

/**
 * 注意：这是腾讯特供版本，与开源版完全不一样，因被用户历史流水线绑架，用户会使用${{a}}的嵌套写法，问题有:
 * 1、代码较恶心，变量名存在与{}混用情况。 比如 ${{sfsf} 变量名会是 {sfsf
 *
 * 2、 二次替换 只能 对应处理 一直是双括号或者一直是单括号 ，不支持两级变量单双括号混排
 */
@Suppress("MagicNumber")
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
                // 先处理${{}} 双花括号的情况
                val lineTmp = parseWithDoubleCurlyBraces(line, replacement, contextMap)
                // 再处理${} 单个花括号的情况
                parseTemplate(lineTmp, replacement, contextMap)
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
        contextMap: Map<String, String>? = emptyMap(),
        depth: Int = 1
    ): String {
        if (command.isBlank()) {
            return command
        }
        val newValue = StringBuilder()
        var index = 0
        while (index < command.length) {
            val c = command[index]
            if (c == '$' && (index + 1) < command.length && command[index + 1] == '{') {
                val inside = StringBuilder()
                index = parseVariable(command, index + 2, inside, replacement, contextMap, depth)
                newValue.append(inside)
            } else {
                newValue.append(c)
                index++
            }
        }
        return newValue.toString()
    }

    private fun parseWithDoubleCurlyBraces(
        command: String,
        replacement: KeyReplacement,
        contextMap: Map<String, String>? = emptyMap(),
        depth: Int = 1
    ): String {
        if (command.isBlank()) {
            return command
        }
        val newValue = StringBuilder()
        var index = 0
        while (index < command.length) {
            val c = command[index]
            if (checkPrefix(c, index, command)) {
                val inside = StringBuilder()
                index = parseVariableWithDoubleCurlyBraces(command, index + 3, inside, replacement, contextMap, depth)
                newValue.append(inside)
            } else {
                newValue.append(c)
                index++
            }
        }
        return newValue.toString()
    }

    @Suppress("NestedBlockDepth", "LongParameterList")
    private fun parseVariable(
        command: String,
        start: Int,
        newValue: StringBuilder,
        replacement: KeyReplacement,
        contextMap: Map<String, String>? = emptyMap(),
        depth: Int = 1
    ): Int {
        val token = StringBuilder()
        var index = start
        while (index < command.length) {
            val c = command[index]
            if (c == '$' && (index + 1) < command.length && command[index + 1] == '{') {
                val inside = StringBuilder()
                index = parseVariable(command, index + 2, inside, replacement, contextMap, depth)
                token.append(inside)
            } else if (c == '}') {
                var tokenValue: String? = getVariable(token.toString(), replacement, false)
                if (tokenValue == "\${$token}") {
                    tokenValue = contextMap?.get(token.toString())
                }

                if (tokenValue == null) {
                    tokenValue = "\${$token}"
                } else {
                    // 去掉tokenValue.startsWith("\${")是考虑存在XXX_${{xxxx}}这种有前缀的情况
                    if (depth > 0) {
                        tokenValue = parseTemplate(tokenValue, replacement, contextMap, depth - 1)
                    }
                }
                newValue.append(tokenValue)
                return index + 1
            } else {
                token.append(c)
                index++
            }
        }
        newValue.append("\${").append(token)
        return index
    }

    @Suppress("NestedBlockDepth", "LongParameterList")
    private fun parseVariableWithDoubleCurlyBraces(
        command: String,
        start: Int,
        newValue: StringBuilder,
        replacement: KeyReplacement,
        contextMap: Map<String, String>? = emptyMap(),
        depth: Int = 1
    ): Int {
        val token = StringBuilder()
        var index = start

        while (index < command.length) {
            val c = command[index]
            if (checkPrefix(c, index, command)) {
                val inside = StringBuilder()
                index = parseVariableWithDoubleCurlyBraces(command, index + 3, inside, replacement, contextMap, depth)
                token.append(inside)
            } else if (c == '}' && index + 1 < command.length && command[index + 1] == '}') {
                val tokenStr = token.toString().trim()
                var tokenValue: String? = getVariable(token.toString().trim(), replacement, true)
                if (tokenValue == "\${{$tokenStr}}") {
                    tokenValue = contextMap?.get(tokenStr)
                }

                if (tokenValue == null) {
                    tokenValue = "\${{$token}}"
                } else {
                    // 去掉tokenValue.startsWith是考虑有xxx_${{xxx}前缀的情况
                    if (depth > 0) {
                        tokenValue = parseWithDoubleCurlyBraces(tokenValue, replacement, contextMap, depth - 1)
                    }
                }
                newValue.append(tokenValue)
                return index + 2
            } else {
                token.append(c)
                index++
            }
        }
        newValue.append("\${{").append(token)
        return index
    }

    private fun checkPrefix(c: Char, index: Int, command: String) =
        c == '$' && (index + 2) < command.length && command[index + 1] == '{' && command[index + 2] == '{'

    private fun getVariable(key: String, replacement: KeyReplacement, doubleCurlyBraces: Boolean) =
        replacement.getReplacement(key)
            ?: if (doubleCurlyBraces) {
                "\${{$key}}"
            } else {
                "\${$key}"
            }
}

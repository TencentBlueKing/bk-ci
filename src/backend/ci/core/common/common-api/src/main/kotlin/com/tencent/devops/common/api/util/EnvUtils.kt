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
 * 注意：这是腾讯特供版本，与开源版完全不一样，被用户历史流水线绑架，用户会使用${{a}}的嵌套写法，问题有:
 * 1、代码较恶心，变量名存在与{}混用情况。 比如 ${{xxx} 变量名会是 {xxx
 *
 * 2、 二次替换 只能 对应处理 一直是双括号或者一直是单括号 ，不支持两级变量单双括号混排
 *
 * 3、replaceWithEmpty = true 会将找不到变量的置空，对于像${{xxx}}，会先被单引号模板语法识别到 ${{xxx} 并替换为空格，无法正常识别 ${{}}
 */
@Suppress("MagicNumber")
object EnvUtils {
    fun parseEnv(
        command: String?,
        data: Map<String, String>,
        replaceWithEmpty: Boolean = false,
        isEscape: Boolean = false
    ): String {
        return parseEnv(
            command = command,
            data = data,
            replaceWithEmpty = replaceWithEmpty,
            isEscape = isEscape,
            contextMap = emptyMap()
        )
    }

    fun parseEnv(
        command: String?,
        data: Map<String, String>,
        replaceWithEmpty: Boolean = false,
        isEscape: Boolean = false,
        contextMap: Map<String, String>? = emptyMap()
    ): String {
        if (command.isNullOrBlank()) {
            return command ?: ""
        }
        // 先处理${{}} 双花括号的情况
        val value = parseWithDoubleCurlyBraces(command, data, isEscape, contextMap, depth = 1)
        // 再处理${} 单个花括号的情况
        return parseWithSingleCurlyBraces(value, data, replaceWithEmpty, isEscape, contextMap, depth = 1)
    }

    private fun parseWithDoubleCurlyBraces(
        value: String,
        data: Map<String, String>,
        escape: Boolean = false,
        contextMap: Map<String, String>? = emptyMap(),
        depth: Int = 1
    ): String {
        val newValue = StringBuilder()
        var index = 0
        while (index < value.length) {
            val c = value[index]
            if (checkPrefix(c, index, value)) {
                val inside = StringBuilder()
                index = parseVariableWithDoubleCurlyBraces(value, index + 3, inside, data, contextMap, escape, depth)
                if (escape) {
                    newValue.append(escapeSpecialWord(inside.toString()))
                } else {
                    newValue.append(inside)
                }
            } else {
                newValue.append(c)
                index++
            }
        }
        return newValue.toString()
    }

    @Suppress("NestedBlockDepth", "LongParameterList")
    private fun parseWithSingleCurlyBraces(
        command: String,
        data: Map<String, String>,
        replaceWithEmpty: Boolean,
        isEscape: Boolean,
        contextMap: Map<String, String>? = emptyMap(),
        depth: Int = 1
    ): String {
        val newValue = StringBuilder()
        var index = 0
        while (index < command.length) {
            val c = command[index]
            if (c == '$' && (index + 1) < command.length && command[index + 1] == '{') {
                val inside = StringBuilder()
                index = parseVariable(command, index + 2, inside, data, replaceWithEmpty, contextMap, isEscape, depth)
                if (isEscape) {
                    // 将动态参数值里面的特殊字符转义
                    newValue.append(escapeSpecialWord(inside.toString()))
                } else {
                    newValue.append(inside)
                }
            } else {
                newValue.append(c)
                index++
            }
        }
        return newValue.toString()
    }

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

    @Suppress("NestedBlockDepth", "LongParameterList")
    private fun parseVariable(
        command: String,
        start: Int,
        newValue: StringBuilder,
        data: Map<String, String>,
        replaceWithEmpty: Boolean = false,
        contextMap: Map<String, String>? = emptyMap(),
        isEscape: Boolean = false,
        depth: Int = 1
    ): Int {
        val token = StringBuilder()
        var index = start
        while (index < command.length) {
            val c = command[index]
            if (c == '$' && (index + 1) < command.length && command[index + 1] == '{') {
                val inside = StringBuilder()
                index = parseVariable(command, index + 2, inside, data, replaceWithEmpty, contextMap, isEscape, depth)
                token.append(inside)
            } else if (c == '}') {
                val value = data[token.toString()] ?: contextMap?.get(token.toString())
                if (value != null) {
                    // 去掉value.startsWith("\${")是考虑以xxx_${{xxx}}前缀的情况
                    if (depth > 0) {
                        newValue.append(
                            parseWithSingleCurlyBraces(
                                command = value,
                                data = data,
                                replaceWithEmpty = replaceWithEmpty,
                                isEscape = isEscape,
                                contextMap = contextMap,
                                depth = depth - 1
                            )
                        )
                    } else {
                        newValue.append(value)
                    }
                } else if (!replaceWithEmpty) {
                    newValue.append("\${$token}")
                }

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
        data: Map<String, String>,
        contextMap: Map<String, String>? = emptyMap(),
        escape: Boolean = false,
        depth: Int = 1
    ): Int {
        val token = StringBuilder()
        var index = start
        while (index < command.length) {
            val c = command[index]
            if (checkPrefix(c, index, command)) {
                val inside = StringBuilder()
                index = parseVariableWithDoubleCurlyBraces(command, index + 3, inside, data, contextMap, escape, depth)
                token.append(inside)
            } else if (c == '}' && index + 1 < command.length && command[index + 1] == '}') {
                val tokenStr = token.toString().trim()
                val value = data[tokenStr] ?: contextMap?.get(tokenStr)
                if (value != null) {
                    // 去掉value.startsWith是考虑xxx_${{xxx}}前缀的情况
                    if (depth > 0) {
                        newValue.append(parseWithDoubleCurlyBraces(value, data, escape, contextMap, depth - 1))
                    } else {
                        newValue.append(value)
                    }
                } else {
                    newValue.append("\${{$token}}")
                }
                return index + 2
            } else {
                token.append(c)
                index++
            }
        }
        newValue.append("\${{").append(token)
        return index
    }

    private fun checkPrefix(c: Char, index: Int, value: String) =
        c == '$' && (index + 2) < value.length && value[index + 1] == '{' && value[index + 2] == '{'
}

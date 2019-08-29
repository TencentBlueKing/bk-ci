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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.api.util

object EnvUtils {
    fun parseEnv(command: String, data: Map<String, String>, replaceWithEmpty: Boolean = false): String {
        if (command.isBlank()) {
            return command
        }
        val newValue = StringBuilder()
        var index = 0
        while (index < command.length) {
            val c = command[index]
            if (c == '$' && (index + 1) < command.length && command[index + 1] == '{') {
                val inside = StringBuilder()
                index = parseVariable(command, index + 2, inside, data, replaceWithEmpty)
                // 将动态参数值里面的双引号转义
                newValue.append(inside.toString().replace("\"", "\\\""))
            } else {
                newValue.append(c)
                index++
            }
        }
        return newValue.toString()
    }

    private fun parseVariable(
        command: String,
        start: Int,
        newValue: StringBuilder,
        data: Map<String, String>,
        replaceWithEmpty: Boolean = false
    ): Int {
        val token = StringBuilder()
        var index = start
        while (index < command.length) {
            val c = command[index]
            if (c == '$' && (index + 1) < command.length && command[index + 1] == '{') {
                val inside = StringBuilder()
                index = parseVariable(command, index + 2, inside, data, replaceWithEmpty)
                token.append(inside)
            } else if (c == '}') {
                val value = getVariable(data, token.toString()) ?: if (replaceWithEmpty) {
                    ""
                } else {
                    "\${$token}"
                }

                newValue.append(value)
                return index + 1
            } else {
                token.append(c)
                index++
            }
        }
        newValue.append("\${").append(token)
        return index
    }

    private fun getVariable(data: Map<String, String>, key: String) = if (data[key] != null) {
        data[key]!!
    } else {
        null
    }
}

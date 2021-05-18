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

object ReplacementUtils {

    fun replace(command: String, replacement: KeyReplacement): String {
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
                parseTemplate(line, replacement)
            }
            sb.append(template)
            if (index != lines.size - 1) {
                sb.append("\n")
            }
        }
        return sb.toString()
    }

    private fun parseTemplate(command: String, replacement: KeyReplacement): String {
        if (command.isBlank()) {
            return command
        }
        val newValue = StringBuilder()
        var index = 0
        while (index < command.length) {
            val c = command[index]
            if (c == '$' && (index + 1) < command.length && command[index + 1] == '{') {
                val inside = StringBuilder()
                index = parseVariable(command, index + 2, inside, replacement)
                newValue.append(inside)
            } else {
                newValue.append(c)
                index++
            }
        }
        return newValue.toString()
    }

    private fun parseVariable(command: String, start: Int, newValue: StringBuilder, replacement: KeyReplacement): Int {
        val token = StringBuilder()
        var index = start
        while (index < command.length) {
            val c = command[index]
            if (c == '$' && (index + 1) < command.length && command[index + 1] == '{') {
                val inside = StringBuilder()
                index = parseVariable(command, index + 2, inside, replacement)
                token.append(inside)
            } else if (c == '}') {
                val tokenValue = getVariable(token.toString(), replacement) ?: "\${$token}"
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

    private fun getVariable(key: String, replacement: KeyReplacement) = replacement.getReplacement(key)

    interface KeyReplacement {
        fun getReplacement(key: String): String?
    }
}
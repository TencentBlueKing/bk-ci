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

object TemplateFastReplaceUtils {

    private const val DCBS = 3
    private const val SCBS = 2

    fun replaceTemplate(templateString: String?, replacer: (t: String) -> String?): String {
        if (templateString.isNullOrBlank()) {
            return templateString ?: ""
        }
        // 先处理 ${} 单花括号模板语法（旧的实现）
        val first = singleCurlyBraces(template = templateString, replacer = replacer)
        // 再处理${{}} 双花括号模板语法
        return doubleCurlyBraces(template = first, replacer = replacer)
    }

    private fun singleCurlyBraces(template: String, replacer: (t: String) -> String?): String {
        val newValue = StringBuilder()
        var index = 0
        while (index < template.length) {
            val c = template[index]
            if (c == '$' && (index + 1) < template.length && template[index + 1] == '{') {
                val inside = StringBuilder()
                index = reSingleCBS(template = template, start = index + SCBS, newValue = inside, replacer = replacer)
                newValue.append(inside)
            } else {
                newValue.append(c)
                index++
            }
        }
        return newValue.toString()
    }

    private fun reSingleCBS(
        template: String,
        start: Int,
        newValue: StringBuilder,
        replacer: (t: String) -> String?
    ): Int {
        val token = StringBuilder()
        var index = start
        while (index < template.length) {
            val c = template[index]
            if (c == '$' && (index + 1) < template.length && template[index + 1] == '{') {
                val inside = StringBuilder()
                index = reSingleCBS(template = template, start = index + SCBS, newValue = inside, replacer = replacer)
                token.append(inside)
            } else if (c == '}') {
                newValue.append(replacer(token.trim().toString()) ?: "\${$token}")
                return index + 1
            } else { // to do: 此处存在优化空间，后续再处理，先保持与原来EnvUtils的逻辑不动
                token.append(c)
                index++
            }
        }
        newValue.append("\${").append(token)
        return index
    }

    private fun doubleCurlyBraces(template: String, replacer: (t: String) -> String?): String {
        val newValue = StringBuilder()
        var index = 0
        while (index < template.length) {
            val c = template[index]
            if (checkPrefix(c, index, template)) {
                val inside = StringBuilder()
                index = reDoubleCBS(template = template, start = index + DCBS, newValue = inside, replacer = replacer)
                newValue.append(inside)
            } else {
                newValue.append(c)
                index++
            }
        }
        return newValue.toString()
    }

    private fun reDoubleCBS(
        template: String,
        start: Int,
        newValue: StringBuilder,
        replacer: (t: String) -> String?
    ): Int {
        val token = StringBuilder()
        var index = start
        while (index < template.length) {
            val c = template[index]
            if (checkPrefix(c, index, template)) {
                val inside = StringBuilder()
                index = reDoubleCBS(template = template, start = index + DCBS, newValue = inside, replacer = replacer)
                token.append(inside)
            } else if (c == '}' && index + 1 < template.length && template[index + 1] == '}') {
                newValue.append(replacer(token.trim().toString()) ?: "\${{$token}}")
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

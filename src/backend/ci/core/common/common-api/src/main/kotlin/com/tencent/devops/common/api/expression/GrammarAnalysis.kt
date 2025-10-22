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

package com.tencent.devops.common.api.expression

import java.lang.Exception
import java.lang.IllegalArgumentException
import java.util.ArrayDeque

@Suppress("ALL")
class GrammarAnalysis(private val items: List<Word>) {

    private var index = 0

    private var sym: Word = items[index]

    private val parentStack = ArrayDeque<String>()

    fun analysis(): Boolean {
        try {
            condition()
        } catch (e: Exception) {
            if (e is GrammarAnalysisFinishedException) {
                return true
            }
            throw e
        }
        return false
    }

    private fun accept(s: String): Boolean {
        if (s == sym.symbol) {
            nextSym()
            return true
        }
        return false
    }

    private fun expect(s: String): Boolean {
        if (accept(s)) {
            return true
        }
        error("expect: $s  but now [index: $index symbol: ${sym.symbol}] is unexpected symbol")
        return false
    }

    private fun factor() {
        when {
            accept("ident") -> {
            }
            accept("number") -> {
            }
            accept("(") -> {
                parentStack.push("(")
                condition()
                expect(")")
            }
            else -> {
                error("factor: [index: $index symbol: ${sym.symbol}] is not ident")
                nextSym()
            }
        }
    }

    private fun condition() {
        accept("!")
        factor()
        if (sym.symbol == ")") {
            if (parentStack.isEmpty()) {
                error("condition: [index: $index symbol: ${sym.symbol}] is extra need '(' ")
            } else {
                parentStack.pop()
            }
        } else if (sym.symbol == "==" || sym.symbol == "!=" || sym.symbol == "<" || sym.symbol == "<=" || sym
                .symbol == ">" || sym.symbol == ">=" || sym.symbol == "||" || sym.symbol == "&&"
        ) {
            nextSym()
            condition()
        } else {
            error("condition: [index: $index symbol: ${sym.symbol}] is invalid operator")
            nextSym()
        }
    }

    private fun nextSym() {
//        println("index: $index   symbol: ${sym.symbol}")
        index++
        if (index >= items.size) {
            if (parentStack.isNotEmpty()) {
                error("condition:  need ')' ")
            }
            throw GrammarAnalysisFinishedException("success")
        }
        sym = items[index]
    }

    private fun error(message: String) {
        throw IllegalArgumentException(message)
    }
}

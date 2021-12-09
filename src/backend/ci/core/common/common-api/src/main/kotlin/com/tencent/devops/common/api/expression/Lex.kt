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

package com.tencent.devops.common.api.expression

@Suppress("ALL")
class Lex(var str: MutableList<Char>) {

    var pos = 0
    var syn = -1
    var state = 0

    val chars = setOf('.', '\'', '[', ']', '_', '-', '+', '*', '/', ',')

    fun getToken(): List<Word> {
        val results = mutableListOf<Word>()

        val token = mutableListOf<Char>()

        var ch = getNext()

        while (ch != null) {
            while (state != 100) {
                when (state) {
                    0 -> {
//                        if (ch == '+') state = 1
//                        else if (ch == '-') state = 2
//                        else if (ch == '*') state = 3
//                        else if (ch == '/') state = 4
                        if (ch == '<') state = 5
                        else if (ch == '>') state = 6
                        else if (ch == '=') state = 7
//                        else if (ch == ';') state = 8
                        else if (ch == '!') state = 9
//                        else if (ch == '[') state = 10
//                        else if (ch == ']') state = 11
                        else if (ch == '(') state = 12
                        else if (ch == ')') state = 13
//                        else if (ch == '{') state = 14
//                        else if (ch == '}') state = 15
//                        else if (ch == '"') state = 16
                        else if (ch == '&') state = 17
                        else if (ch == '|') state = 18
//                        else if (ch == '.') state = 19
                        else if (isLetter(ch!!)) state = 40
//                        else if (isNum(ch)) state = 50
                        else if (ch == ' ' || ch == '\t' || ch == '\n') state = 100
                        else state = 99 // 异常
                    }
                    // 匹配到 <
                    5 -> {
                        token.add(ch!!)
                        ch = getNext()
                        if (ch == '=') {
                            state = 24
                        } else {
                            back()
                            state = 100
                            syn = 12
                        }
                    }
                    // 匹配到 <=
                    24 -> {
                        token.add(ch!!)
                        state = 100
                        syn = 23
                    }
                    // 匹配到 >
                    6 -> {
                        token.add(ch!!)
                        ch = getNext()
                        if (ch == '=') {
                            state = 25
                        } else {
                            back()
                            state = 100
                            syn = 13
                        }
                    }
                    // 匹配到 <=
                    25 -> {
                        token.add(ch!!)
                        state = 100
                        syn = 24
                    }
                    // 匹配到 =
                    7 -> {
                        token.add(ch!!)
                        ch = getNext()
                        if (ch == '=') {
                            state = 26
                        } else {
                            back()
                            state = 100
                            syn = 14
                        }
                    }
                    // 匹配到 ==
                    26 -> {
                        token.add(ch!!)
                        state = 100
                        syn = 25
                    }
                    // 匹配到 !
                    9 -> {
                        token.add(ch!!)
                        ch = getNext()
                        if (ch == '=') {
                            state = 27
                        } else {
                            back()
                            state = 100
                            syn = 15
                        }
                    }
                    // 匹配到 !=
                    27 -> {
                        token.add(ch!!)
                        state = 100
                        syn = 26
                    }
                    // 匹配到 (
                    12 -> {
                        token.add(ch!!)
                        state = 100
                        syn = 19
                    }
                    // 匹配到 )
                    13 -> {
                        token.add(ch!!)
                        state = 100
                        syn = 20
                    }
                    // 匹配到 &
                    17 -> {
                        token.add(ch!!)
                        ch = getNext()
                        if (ch == '&') {
                            token.add(ch)
                            ch = getNext()
                            state = 100
                            syn = 21
                        } else {
                            state = 99
                        }
                    }
                    // 匹配到 |
                    18 -> {
                        token.add(ch!!)
                        ch = getNext()
                        if (ch == '|') {
                            token.add(ch)
                            ch = getNext()
                            state = 100
                            syn = 22
                        } else {
                            state = 99
                        }
                    }
                    // 匹配到字母
                    40 -> {
                        token.add(ch!!)
                        ch = getNext()
                        if (ch == null) {
                            state = 100
                            syn = 27
                        } else {
                            // 向前看一位还是字母
                            if (isLetter(ch!!)) {
                                state = 40
                            } else {
                                back()
                                state = 100
                                syn = 27
                            }
                        }
                    }
                    // 匹配中出错
                    99 -> {
//                        println("error: { $ch } in index: $pos")
                        ch = getNext()
                        if (ch == null) {
                            state = 100
                            syn = 27
                        } else {
                            while (ch!! != ' ') {
                                ch = getNext()
                                if (ch == null) {
                                    break
                                }
                            }
                            back()
                            state = 100
                            syn = -1
                        }
                    }
                }
            }
            if (state == 100 && syn != -1) {
                when (syn) {
                    0, 1, 2, 3, 4, 5, 6, 7 -> {
                        results.add(Word(token.joinToString(""), "RESERVED WORD"))
                    }
                    27 -> {
                        results.add(Word(token.joinToString(""), "ident"))
                    }
                    31, 32 -> {
                    }
                    else -> {
                        results.add(Word(token.joinToString(""), token.joinToString("")))
                    }
                }
                token.clear()

                state = 0
                syn = -1
            }
            if (state == 100) {
                state = 0
            }
            ch = getNext()
        }
        return results
    }

    private fun getNext(): Char? {
        return if (pos < str.size) {
            str[pos++]
        } else {
            null
        }
    }

    private fun isLetter(c: Char): Boolean {
        return (c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c in chars)
    }

    private fun back() {
        pos -= 1
    }
}

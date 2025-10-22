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

import java.lang.IllegalArgumentException

@Suppress("ALL")
class SemanticAnalysis(items: List<Word>) {

    private val symbolLevelTable = mapOf(
        1 to listOf("!"),
        2 to listOf("==", "!=", ">", ">=", "<", "<="),
        3 to listOf("||", "&&")
    )

    var index = 0

    var itemList = items.toMutableList()

    fun analysis(): Boolean {
        // 先找括号,可能存在同级的括号，所以使用iterator将所有括号打平
        var iterator = itemList.iterator()
        while (iterator.hasNext()) {
            val word = iterator.next()
            if (word.symbol == "(") {
                parent(index)
                iterator = itemList.iterator()
            }
        }
        // 打平后统一分析
        return analysis(itemList.toMutableList()).toBoolean()
    }

    // 针对括号内的进行分析，令其最后只剩一个bool
    private fun analysis(leftIndex: Int, rightIndex: Int) {
        if (leftIndex == rightIndex || leftIndex > rightIndex || leftIndex + 1 == rightIndex) {
            return
        }
        val templist = itemList.subList(leftIndex + 1, rightIndex).toMutableList()
        val result = analysis(templist)
        itemList[leftIndex] = Word(result, "ident")

        val iterator = itemList.iterator()
        var index = 0
        while (iterator.hasNext()) {
            iterator.next()
            if (index in (leftIndex + 1)..rightIndex) {
                iterator.remove()
            }
            index++
        }
    }

    // 对已经打平的表达式根据符号优先级分析
    private fun analysis(list: MutableList<Word>): String {
        var iterator = list.iterator()
        var index = 0
        while (iterator.hasNext()) {
            val word = iterator.next()
            if (word.symbol == "!") {
                list[index] = Word(calculate("!", list[index + 1].str), "ident")
                if (iterator.hasNext()) {
                    iterator.next()
                    iterator.remove()
                }
            }
            index++
        }

        iterator = list.iterator()
        index = 0
        while (iterator.hasNext()) {
            val word = iterator.next()
            if (symbolLevelTable[2]!!.contains(word.symbol)) {
                list[index - 1] = Word(calculate(list[index - 1].str, word.symbol, list[index + 1].str), "ident")
                iterator.remove()
                index--
                if (iterator.hasNext()) {
                    iterator.next()
                    iterator.remove()
                    index--
                }
                // 每次计算删除后从当前的列表的头重新遍历
                iterator = list.iterator()
                index = -1
            }
            index++
        }

        iterator = list.iterator()
        index = 0
        while (iterator.hasNext()) {
            val word = iterator.next()
            if (symbolLevelTable[3]!!.contains(word.symbol)) {
                list[index - 1] = Word(calculate(list[index - 1].str, word.symbol, list[index + 1].str), "ident")
                iterator.remove()
                index--
                if (iterator.hasNext()) {
                    iterator.next()
                    iterator.remove()
                    index--
                }
                // 每次计算删除后从当前的列表的头重新遍历
                iterator = list.iterator()
                index = -1
            }
            index++
        }

        return list[0].str
    }

    // 找出所有的括号的子列表进行打平
    private fun parent(index: Int) {
        val leftIndex = index
        var rightIndex = index

        for (i in (leftIndex + 1) until itemList.size) {
            // 递归后i的值不刷新所以可能会溢出
            if (i >= itemList.size - 1) {
                break
            }
            if (itemList[i].symbol == "(") {
                parent(i)
            }
        }

        for (i in leftIndex until itemList.size) {
            // 只找距离最近的 ')'
            if (itemList[i].symbol == ")") {
                rightIndex = i
                break
            }
        }
        analysis(leftIndex, rightIndex)
    }

    // 计算二元表达式
    private fun calculate(symbol: String, str: String): String {
        when (symbol) {
            "!" -> {
                return (!getBool(replaceVal(str))).toString()
            }
            else -> {
                throw IllegalArgumentException("SemanticAnalysis error Symbol: $symbol")
            }
        }
    }

    // 计算三元表达式
    private fun calculate(str: String, symbol: String, str2: String): String {
        val left = replaceVal(str)
        val right = replaceVal(str2)
        when (symbol) {
            "==" -> {
                return (left == right).toString()
            }
            "!=" -> {
                return (left != right).toString()
            }
            ">" -> {
                return (getNumber(left) > getNumber(right)).toString()
            }
            ">=" -> {
                return (getNumber(left) >= getNumber(right)).toString()
            }
            "<" -> {
                return (getNumber(left) < getNumber(right)).toString()
            }
            "<=" -> {
                return (getNumber(left) <= getNumber(right)).toString()
            }
            "||" -> {
                return (getBool(left) || getBool(right)).toString()
            }
            "&&" -> {
                return (getBool(left) && getBool(right)).toString()
            }
            else -> {
                throw IllegalArgumentException("SemanticAnalysis error Symbol: $symbol")
            }
        }
    }

    private fun replaceVal(str: String): String {
        // todo: 替换为后台保存的数值
        return str
    }

    private fun getNumber(str: String): Int {
        try {
            return str.toInt()
        } catch (e: Exception) {
            throw IllegalArgumentException("SemanticAnalysis: need Number ident")
        }
    }

    private fun getBool(str: String): Boolean {
        return when (str) {
            "true" -> {
                true
            }
            "false" -> {
                false
            }
            else -> {
                throw IllegalArgumentException("SemanticAnalysis: need Boolean ident")
            }
        }
    }
}

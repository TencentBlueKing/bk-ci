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

package com.tencent.devops.common.pipeline

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.ObjectReplaceEnvVarUtil
import com.tencent.devops.common.expression.ExecutionContext
import com.tencent.devops.common.expression.ExpressionParseException
import com.tencent.devops.common.expression.ExpressionParser
import com.tencent.devops.common.expression.context.ContextValueNode
import com.tencent.devops.common.expression.context.DictionaryContextData
import com.tencent.devops.common.expression.context.PipelineContextData
import com.tencent.devops.common.expression.context.RuntimeDictionaryContextData
import com.tencent.devops.common.expression.context.RuntimeNamedValue
import com.tencent.devops.common.expression.expression.ExpressionOutput
import com.tencent.devops.common.expression.expression.IFunctionInfo
import com.tencent.devops.common.expression.expression.sdk.NamedValueInfo
import org.apache.tools.ant.filters.StringInputStream
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader

@Suppress(
    "LoopWithTooManyJumpStatements",
    "ComplexCondition",
    "ComplexMethod",
    "NestedBlockDepth",
    "ReturnCount",
    "LongParameterList"
)
object EnvReplacementParser {

    private val logger = LoggerFactory.getLogger(EnvReplacementParser::class.java)

    /**
     * 根据环境变量map进行object处理并保持原类型
     * @param value 等待进行环境变量替换的对象，可以是任意类型
     * @param contextMap 环境变量map值
     * @param contextPair 自定义表达式计算上下文（如果指定则不使用表达式替换或默认替换逻辑）
     * @param onlyExpression 只进行表达式替换（若指定了自定义替换逻辑此字段无效，为false）
     * @param functions 用户自定义的拓展用函数
     * @param output 表达式计算时输出
     */
    fun parse(
        value: String?,
        contextMap: Map<String, String>,
        onlyExpression: Boolean? = false,
        contextPair: Pair<ExecutionContext, List<NamedValueInfo>>? = null,
        functions: Iterable<IFunctionInfo>? = null,
        output: ExpressionOutput? = null
    ): String {
        if (value.isNullOrBlank()) return ""
        return if (onlyExpression == true) {
            try {
                val (context, nameValues) = contextPair
                    ?: getCustomExecutionContextByMap(contextMap)
                    ?: return value
                parseExpression(
                    value = value,
                    context = context,
                    nameValues = nameValues,
                    functions = functions,
                    output = output
                )
            } catch (ignore: Throwable) {
                logger.warn("[$value]|EnvReplacementParser expression invalid: ", ignore)
                value
            }
        } else {
            ObjectReplaceEnvVarUtil.replaceEnvVar(value, contextMap).let {
                JsonUtil.toJson(it, false)
            }
        }
    }

    fun getCustomExecutionContextByMap(
        variables: Map<String, String>,
        extendNamedValueMap: List<RuntimeNamedValue>? = null
    ): Pair<ExecutionContext, List<NamedValueInfo>>? {
        try {
            val context = ExecutionContext(DictionaryContextData())
            val nameValue = mutableListOf<NamedValueInfo>()
            extendNamedValueMap?.forEach { namedValue ->
                nameValue.add(NamedValueInfo(namedValue.key, ContextValueNode()))
                context.expressionValues.add(
                    namedValue.key,
                    RuntimeDictionaryContextData(namedValue)
                )
            }
            ExpressionParser.fillContextByMap(variables, context, nameValue)
            return Pair(context, nameValue)
        } catch (ignore: Throwable) {
            logger.warn("EnvReplacementParser context invalid: $variables", ignore)
            return null
        }
    }

    private fun parseExpression(
        value: String,
        nameValues: List<NamedValueInfo>,
        context: ExecutionContext,
        functions: Iterable<IFunctionInfo>? = null,
        output: ExpressionOutput? = null
    ): String {
        val strReader = InputStreamReader(StringInputStream(value))
        val bufferReader = BufferedReader(strReader)
        val newValue = StringBuilder()
        try {
            var line = bufferReader.readLine()
            while (line != null) {
                // 跳过空行和注释行
                val blocks = findExpressions(line)
                if (line.isBlank() || blocks.isEmpty()) {
                    newValue.append(line).append("\n")
                    line = bufferReader.readLine()
                    continue
                }
                val onceResult = parseExpressionLine(
                    value = line,
                    blocks = blocks,
                    context = context,
                    nameValues = nameValues,
                    functions = functions,
                    output = output
                )

                val newLine = findExpressions(onceResult).let {
                    if (it.isEmpty()) {
                        onceResult
                    } else {
                        parseExpressionLine(
                            value = onceResult,
                            blocks = it,
                            context = context,
                            nameValues = nameValues,
                            functions = functions,
                            output = output
                        )
                    }
                }
                newValue.append(newLine).append("\n")
                line = bufferReader.readLine()
            }
        } finally {
            strReader.close()
            bufferReader.close()
        }
        return newValue.toString().removeSuffix("\n")
    }

    /**
     * 解析表达式，根据 findExpressions 寻找的括号优先级进行解析
     */
    private fun parseExpressionLine(
        value: String,
        blocks: List<List<ExpressionBlock>>,
        nameValues: List<NamedValueInfo>,
        context: ExecutionContext,
        functions: Iterable<IFunctionInfo>? = null,
        output: ExpressionOutput? = null
    ): String {
        var chars = value.toList()
        blocks.forEachIndexed nextBlockLevel@{ blockLevel, blocksInLevel ->
            blocksInLevel.forEachIndexed nextBlock@{ blockI, block ->
                // 表达式因为含有 ${{ }} 所以起始向后推3位，末尾往前推两位
                val expression = chars.joinToString("").substring(block.startIndex + 3, block.endIndex - 1)

                var result = try {
                    ExpressionParser.createTree(expression, null, nameValues, functions)!!
                        .evaluate(null, context, null, output).value.let {
                            if (it is PipelineContextData) it.fetchValue() else it
                        }?.let {
                            JsonUtil.toJson(it, false)
                        } ?: ""
                } catch (ignore: ExpressionParseException) {
                    return@nextBlock
                }

                if ((blockLevel + 1 < blocks.size) &&
                    !(
                        (block.startIndex - 1 >= 0 && chars[block.startIndex - 1] == '.') ||
                            (block.endIndex + 1 < chars.size && chars[block.endIndex + 1] == '.')
                        )
                ) {
                    result = "'$result'"
                }

                val charList = result.toList()

                // 将替换后的表达式嵌入原本的line
                val startSub = if (block.startIndex - 1 < 0) {
                    listOf()
                } else {
                    chars.slice(0 until block.startIndex)
                }
                val endSub = if (block.endIndex + 1 >= chars.size) {
                    listOf()
                } else {
                    chars.slice(block.endIndex + 1 until chars.size)
                }
                chars = startSub + charList + endSub

                // 将替换后的字符查传递给后边的括号位数
                val diffNum = charList.size - (block.endIndex - block.startIndex + 1)
                blocks.forEachIndexed { i, bl ->
                    bl.forEachIndexed level@{ j, b ->
                        if (i <= blockLevel && j <= blockI) {
                            return@level
                        }
                        if (blocks[i][j].startIndex > block.endIndex) {
                            blocks[i][j].startIndex += diffNum
                        }
                        if (blocks[i][j].endIndex > block.endIndex) {
                            blocks[i][j].endIndex += diffNum
                        }
                    }
                }
            }
        }

        return chars.joinToString("")
    }

    /**
     * 寻找语句中包含 ${{}}的表达式的位置，返回成对的位置坐标，并根据优先级排序
     * 优先级算法目前暂定为 从里到外，从左到右
     * @param levelMax 返回的最大层数，从深到浅。默认为2层
     * 例如: 替换顺序如数字所示 ${{ 4  ${{ 2 ${{ 1 }} }} ${{ 3 }} }}
     * @return [ 层数次序 [ 括号 ] ]  [[1], [2, 3], [4]]]]
     */
    private fun findExpressions(condition: String, levelMax: Int = 2): List<List<ExpressionBlock>> {
        val stack = ArrayDeque<Int>()
        var index = 0
        val chars = condition.toCharArray()
        val levelMap = mutableMapOf<Int, MutableList<ExpressionBlock>>()
        while (index < chars.size) {
            if (index + 2 < chars.size && chars[index] == '$' && chars[index + 1] == '{' && chars[index + 2] == '{'
            ) {
                stack.addLast(index)
                index += 3
                continue
            }

            if (index + 1 < chars.size && chars[index] == '}' && chars[index + 1] == '}'
            ) {
                val start = stack.removeLastOrNull()
                if (start != null) {
                    // 栈里剩下几个前括号，这个完整括号的优先级就是多少
                    val level = stack.size + 1
                    if (levelMap.containsKey(level)) {
                        levelMap[level]!!.add(ExpressionBlock(start, index + 1))
                    } else {
                        levelMap[level] = mutableListOf(ExpressionBlock(start, index + 1))
                    }
                }
                index += 2
                continue
            }

            index++
        }

        if (levelMap.isEmpty()) {
            return listOf()
        }

        val result = mutableListOf<MutableList<ExpressionBlock>>()
        var max = 0
        var listIndex = 0
        run end@{
            levelMap.keys.sortedDescending().forEach result@{ level ->
                val blocks = levelMap[level] ?: return@result
                blocks.sortBy { it.startIndex }
                blocks.forEach { block ->
                    if (result.size < listIndex + 1) {
                        result.add(mutableListOf(block))
                    } else {
                        result[listIndex].add(block)
                    }
                }
                listIndex++
                max++
                if (max == levelMax) {
                    return@end
                }
            }
        }
        return result
    }

    /**
     * 表达式括号项 ${{ }}
     * @param startIndex 括号开始位置即 $ 位置
     * @param endIndex 括号结束位置即最后一个 } 位置
     */
    data class ExpressionBlock(
        var startIndex: Int,
        var endIndex: Int
    )
}

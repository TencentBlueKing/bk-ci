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
import com.tencent.devops.common.api.util.KeyReplacement
import com.tencent.devops.common.api.util.ObjectReplaceEnvVarUtil
import com.tencent.devops.common.expression.ExecutionContext
import com.tencent.devops.common.expression.ExpressionParseException
import com.tencent.devops.common.expression.ExpressionParser
import com.tencent.devops.common.expression.SubNameValueEvaluateResult
import com.tencent.devops.common.expression.SubNameValueResultType
import com.tencent.devops.common.expression.context.ContextValueNode
import com.tencent.devops.common.expression.context.DictionaryContextData
import com.tencent.devops.common.expression.context.RuntimeDictionaryContextData
import com.tencent.devops.common.expression.context.RuntimeNamedValue
import com.tencent.devops.common.expression.expression.sdk.NamedValueInfo
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

object EnvReplacementParser {

    private val logger = LoggerFactory.getLogger(EnvReplacementParser::class.java)

    /**
     * 根据环境变量map进行object处理并保持原类型
     * @param obj 等待进行环境变量替换的对象，可以是任意类型
     * @param contextMap 环境变量map值
     * @param executionPair 自定义替换逻辑（如果指定则不使用表达式替换或默认替换逻辑）
     * @param onlyExpression 只进行表达式替换（若指定了自定义替换逻辑此字段无效，为false）
     */
    fun <T: String?> parse(
        obj: T,
        contextMap: Map<String, String>,
        onlyExpression: Boolean? = false,
        executionPair: Pair<ExecutionContext, List<NamedValueInfo>>
    ): T {
        return if (onlyExpression == true) {
            // #7115 如果出现无法表达式解析则保持原文
            object : KeyReplacement {
                override fun getReplacement(key: String): String? {
                    return try {
                        ExpressionParser.evaluateByMap(key, contextMap, true)?.let {
                            JsonUtil.toJson(it, false)
                        }
                    } catch (ignore: ExpressionParseException) {
                        logger.warn("[$onlyExpression] Expression evaluation failed: ", ignore)
                        null
                    }
                }
            }
        } else {
            ObjectReplaceEnvVarUtil.replaceEnvVar(
                obj, contextMap,
                object : KeyReplacement {
                    override fun getReplacement(key: String) = contextMap[key]
                }
            ) as T
        }
    }

    fun getCustomReplacementByMap(
        variables: Map<String, String>,
        extendNamedValueMap: List<RuntimeNamedValue>? = null
    ): Pair<ExecutionContext, List<NamedValueInfo>> {
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
    }


    fun parseParameterValue(
        value: String,
        nameValues: List<NamedValueInfo>,
        context: ExecutionContext
    ): String {
        val strReader = InputStreamReader(StringInputStream(value))
        val bufferReader = BufferedReader(strReader)
        val newValue = StringBuilder()
        try {
            var line = bufferReader.readLine()
            while (line != null) {
                // 跳过空行和注释行，如果一行除空格外最左是 # 那一定是注释
                if (line.isBlank() || line.trimStart().startsWith("#")) {
                    newValue.append(line).append("\n")
                    line = bufferReader.readLine()
                    continue
                }

                val lineString = line.trim().replace("\\s".toRegex(), "")

                val condition = line
                val blocks = findExpressions(condition)
                if (blocks.isEmpty()) {
                    newValue.append(line).append("\n")
                    line = bufferReader.readLine()
                    continue
                }

                val newLine = parseExpression(
                    value = condition,
                    blocks = blocks,
                    nameValues = nameValues,
                    context = context
                )
                newValue.append(newLine).append("\n")
                line = bufferReader.readLine()
            }
        } finally {
            try {
                strReader.close()
                bufferReader.close()
            } catch (ignore: IOException) {
            }
        }

        return newValue.toString()
    }

    /**
     * 解析表达式，根据 findExpressions 寻找的括号优先级进行解析
     */
    private fun parseExpression(
        value: String,
        blocks: List<List<ExpressionBlock>>,
        nameValues: List<NamedValueInfo>,
        context: ExecutionContext
    ): String {
        var lineChars = value.toList()
        blocks.forEachIndexed { blockLevel, blocksInLevel ->
            blocksInLevel.forEachIndexed { blockI, block ->
                // 表达式因为含有 ${{ }} 所以起始向后推3位，末尾往前推两位
                val expression = lineChars.joinToString("").substring(block.startIndex + 3, block.endIndex - 1)

                val result = expressionEvaluate(
                    expression = expression,
                    nameValues = nameValues,
                    context = context
                )

                // 格式化返回值
                val (res, needFormatArr) = formatResult(
                    blockLevel = blockLevel,
                    blocks = blocks,
                    block = block,
                    lineChars = lineChars,
                    evaluateResult = result
                )

                // 去掉前后的可能的引号
                if (needFormatArr) {
                    if (block.startIndex - 1 >= 0 && lineChars[block.startIndex - 1] == '"') {
                        block.startIndex = block.startIndex - 1
                    }
                    if (block.endIndex + 1 < lineChars.size && lineChars[block.endIndex + 1] == '"') {
                        block.endIndex = block.endIndex + 1
                    }
                }

                // 将替换后的表达式嵌入原本的line
                val startSub = if (block.startIndex - 1 < 0) {
                    listOf()
                } else {
                    lineChars.slice(0 until block.startIndex)
                }
                val endSub = if (block.endIndex + 1 >= lineChars.size) {
                    listOf()
                } else {
                    lineChars.slice(block.endIndex + 1 until lineChars.size)
                }
                lineChars = startSub + res + endSub

                // 将替换后的字符查传递给后边的括号位数
                val diffNum = res.size - (block.endIndex - block.startIndex + 1)
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

        return lineChars.joinToString("")
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
     * 格式化表达式计算的返回值
     * @return <格式化结果, 是否需要格式化列表>
     */
    private fun formatResult(
        blockLevel: Int,
        blocks: List<List<ExpressionBlock>>,
        block: ExpressionBlock,
        lineChars: List<Char>,
        evaluateResult: SubNameValueEvaluateResult
    ): Pair<List<Char>, Boolean> {
        if (!evaluateResult.isComplete) {
            return Pair(evaluateResult.value.toList(), false)
        }

        // ScriptUtils.formatYaml会将所有的带上 "" 但换时数组不需要"" 所以为数组去掉可能的额外的""
        // 需要去掉额外""的情况只可能出现只替换了一次列表的情景，即作为参数 var_a: "{{ parameters.xxx }}"
        // 需要将给表达式的转义 \" 转回 "
        if (evaluateResult.type == SubNameValueResultType.ARRAY) {
            return Pair(evaluateResult.value.replace("\\\"", "\"").toList(), (blocks.size == 1))
        }

        // 对于还有下一层的表达式，其替换出来的string需要加上 '' 方便后续第二层使用
        // 例外: 当string前或后存在 . 时，说明是用来做索引，不加 ''
        var result = evaluateResult.value
        if ((blockLevel + 1 < blocks.size && evaluateResult.type == SubNameValueResultType.STRING) &&
            !(
                (block.startIndex - 1 >= 0 && lineChars[block.startIndex - 1] == '.') ||
                    (block.endIndex + 1 < lineChars.size && lineChars[block.endIndex + 1] == '.')
                )
        ) {
            result = "'$result'"
        }

        // 对于字符传可能用的非转义的 \n \s 改为转义后的
        result = StringEscapeUtils.escapeJava(result)

        return Pair(result.toList(), false)
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

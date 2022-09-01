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

package com.tencent.devops.process.yaml.v2.parsers.template

import com.fasterxml.jackson.databind.JsonNode
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.expression.DistinguishType
import com.tencent.devops.common.expression.ExecutionContext
import com.tencent.devops.common.expression.ExpressionParser
import com.tencent.devops.common.expression.SubNameValueEvaluateInfo
import com.tencent.devops.common.expression.context.ArrayContextData
import com.tencent.devops.common.expression.context.BooleanContextData
import com.tencent.devops.common.expression.context.ContextValueNode
import com.tencent.devops.common.expression.context.DictionaryContextData
import com.tencent.devops.common.expression.context.NumberContextData
import com.tencent.devops.common.expression.context.PipelineContextData
import com.tencent.devops.common.expression.context.StringContextData
import com.tencent.devops.common.expression.expression.sdk.NamedValueInfo
import com.tencent.devops.process.yaml.v2.enums.TemplateType
import com.tencent.devops.process.yaml.v2.exception.YamlFormatException
import com.tencent.devops.process.yaml.v2.exception.YamlTemplateException
import com.tencent.devops.process.yaml.v2.models.Repositories
import com.tencent.devops.process.yaml.v2.parameter.Parameters
import com.tencent.devops.process.yaml.v2.parameter.ParametersType
import com.tencent.devops.process.yaml.v2.parsers.template.models.NoReplaceTemplate
import com.tencent.devops.process.yaml.v2.utils.ScriptYmlUtils
import org.apache.commons.text.StringEscapeUtils
import org.apache.tools.ant.filters.StringInputStream
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

@Suppress("ALL")
object TemplateYamlUtil {

    private val logger = LoggerFactory.getLogger(TemplateYamlUtil::class.java)

    // 检查是否具有重复的ID，job，variable中使用
    fun checkDuplicateKey(
        filePath: String,
        keys: Set<String>,
        newKeys: Set<String>,
        toPath: String? = null
    ): Boolean {
        val interSet = newKeys intersect keys
        return if (interSet.isEmpty() || (interSet.size == 1 && interSet.last() == Constants.TEMPLATE_KEY)) {
            true
        } else {
            if (toPath == null) {
                throw error(
                    Constants.TEMPLATE_ROOT_ID_DUPLICATE.format(
                        filePath,
                        interSet.filter { it != Constants.TEMPLATE_KEY }
                    )
                )
            } else {
                throw error(
                    Constants.TEMPLATE_ID_DUPLICATE.format(
                        interSet.filter { it != Constants.TEMPLATE_KEY },
                        filePath,
                        toPath
                    )
                )
            }
        }
    }

    // 校验当前模板的远程库信息，每个文件只可以使用当前文件下引用的远程库
    fun <T> checkAndGetRepo(
        fromPath: String,
        repoName: String,
        templateType: TemplateType,
        templateLib: TemplateLibrary<T>,
        nowRepo: Repositories?,
        toRepo: Repositories?
    ): Repositories {
        val repos = YamlObjects.getObjectFromYaml<NoReplaceTemplate>(
            path = fromPath,
            template = templateLib.getTemplate(
                path = fromPath,
                templateType = templateType,
                nowRepo = nowRepo,
                toRepo = toRepo
            )
        ).resources?.repositories

        repos?.forEach {
            if (it.name == repoName) {
                return it
            }
        }

        throw YamlFormatException(Constants.REPO_NOT_FOUND_ERROR.format(fromPath, repoName))
    }

    /**
     * 为模板中的变量赋值
     * @param fromPath 来自哪个文件
     * @param path 读取的哪个模板文件
     * @param template 被读取的模板文件内容
     * @param templateParameters 被读取的模板文件自带的参数
     * @param parameters 引用模板文件时传入的参数
     */
    fun parseTemplateParameters(
        fromPath: String,
        path: String,
        template: String,
        templateParameters: MutableList<Parameters>?,
        parameters: Map<String, Any?>?
    ): String {
        if (templateParameters.isNullOrEmpty()) {
            return template
        }

        // 模板替换 先替换调用模板传入的参数，再替换模板的默认参数
        templateParameters.forEachIndexed { index, param ->
            if (param.name.contains(".")) {
                logger.error("PARAMETERS|NAME|WARNING|${param.name}")
                throw error(
                    Constants.PARAMETER_FORMAT_ERROR.format(path, "parameter name ${param.name} not allow contains '.'")
                )
            }

            if (parameters == null) {
                return@forEachIndexed
            }

            val valueName = param.name
            val newValue = parameters[param.name]
            if (!parameters.keys.contains(valueName)) {
                return@forEachIndexed
            }

            if (!param.values.isNullOrEmpty() && !param.values.contains(newValue)) {
                throw error(
                    Constants.VALUE_NOT_IN_ENUM.format(
                        fromPath,
                        path,
                        valueName,
                        newValue,
                        param.values.joinToString(",")
                    )
                )
            } else {
                templateParameters[index] = param.copy(default = newValue)
            }
        }

        // 拼接表达式变量
        val expNameValues = mutableListOf<NamedValueInfo>().apply {
            add(NamedValueInfo("parameters", ContextValueNode()))
        }
        val expContext = ExecutionContext(expressionValues = DictionaryContextData())
        val parameterContext = DictionaryContextData()
        expContext.expressionValues.add("parameters", parameterContext)
        templateParameters.filter { it.default != null }.forEach { param ->
            when (param.type.toLowerCase()) {
                ParametersType.ARRAY.value -> {
                    if (param.default !is Iterable<*>) {
                        throw error(
                            Constants.PARAMETER_FORMAT_ERROR.format(
                                path, "parameter ${param.name} type is ${param.type} but value not"
                            )
                        )
                    }
                    val arr = fromJsonToArrayContext(path, param.name, param.default)
                    parameterContext.add(param.name, arr)
                }

                ParametersType.BOOLEAN.value, ParametersType.NUMBER.value, ParametersType.STRING.value -> {
                    parameterContext.add(param.name, nativeTypeToContext(param.default!!))
                }

                else -> throw error(
                    Constants.PARAMETER_FORMAT_ERROR.format(
                        path, "parameter ${param.name} type ${param.type} not support"
                    )
                )
            }
        }

        return parseParameterValue(path, template, expNameValues, expContext)
    }

    // 因为array的里面可能嵌套array所以先转成json再转成array
    fun fromJsonToArrayContext(path: String, parameterName: String, value: Iterable<*>): ArrayContextData {
        val jsonTree = try {
            JsonUtil.getObjectMapper().readTree(JsonUtil.toJson(value))
        } catch (e: Throwable) {
            throw error(
                Constants.PARAMETER_FORMAT_ERROR.format(
                    path, "array parameter $parameterName value [$value] can't to json."
                )
            )
        }
        if (!jsonTree.isArray) {
            throw error(
                Constants.PARAMETER_FORMAT_ERROR.format(
                    path, "array parameter $parameterName value  [$value] json type [${jsonTree.nodeType}] not array."
                )
            )
        }
        return initByJsonTree(jsonTree, null, null) as ArrayContextData
    }

    private fun initByJsonTree(node: JsonNode, context: PipelineContextData?, nodeName: String?): PipelineContextData {
        if (node.isValueNode) {
            return context.addNode(node, nodeName)
        }

        var ctx: PipelineContextData? = context

        if (node.isObject) {
            val fields = node.fields()
            ctx = context ?: DictionaryContextData()
            if (!fields.hasNext()) {
                return ctx
            }
            val newContext = when {
                context == null -> {
                    ctx
                }

                ctx is ArrayContextData -> {
                    val c = DictionaryContextData()
                    ctx.add(c)
                    c
                }

                ctx is DictionaryContextData -> {
                    val c = DictionaryContextData()
                    ctx.add(nodeName!!, c)
                    c
                }

                else -> return ctx
            }
            while (fields.hasNext()) {
                val entry = fields.next()
                initByJsonTree(entry.value, newContext, entry.key)
            }
        }

        if (node.isArray) {
            val iter = node.iterator()
            ctx = context ?: ArrayContextData()
            if (!iter.hasNext()) {
                return ctx
            }
            val newContext = when {
                context == null -> {
                    ctx
                }

                ctx is ArrayContextData -> {
                    val c = ArrayContextData()
                    ctx.add(c)
                    c
                }

                ctx is DictionaryContextData -> {
                    val c = ArrayContextData()
                    ctx.add(nodeName!!, c)
                    c
                }

                else -> return ctx
            }
            while (iter.hasNext()) {
                val entry = iter.next()
                initByJsonTree(entry, newContext, null)
            }
        }

        return ctx ?: StringContextData(node.toString())
    }

    private fun PipelineContextData?.addNode(node: JsonNode, nodeName: String?): PipelineContextData {
        val value = when {
            node.isBoolean -> BooleanContextData(node.booleanValue())
            node.isNumber -> NumberContextData(node.doubleValue())
            node.isNull -> StringContextData("")
            node.isTextual -> StringContextData(node.textValue())
            else -> StringContextData(node.toString())
        }
        if (this == null) {
            return value
        }
        if (this is ArrayContextData) {
            this.add(value)
        }
        if (this is DictionaryContextData) {
            this.add(nodeName!!, value)
        }
        return this
    }

    fun parseParameterValue(
        path: String,
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
                // 跳过注释行，如果一行除空格外最左是 # 那一定是注释
                if (line.trimStart().startsWith("#")) {
                    newValue.append(line).append("\n")
                    line = bufferReader.readLine()
                    continue
                }

                val lineString = line.trim().replace("\\s".toRegex(), "")
                // if 表达式替换
                if (lineString.startsWith("if:") || lineString.startsWith("-if:")) {
                    val ifPrefix = line.substring(0 until line.indexOfFirst { it == ':' } + 1)
                    val condition = line.removePrefix(ifPrefix).trim().removeSurrounding("\"")
                    // 去掉花括号 这里 replace 因为yaml倒了好几手之后会出现在语句中的 \" 字符，使得正常的包含 " 的json无法替换
                    val expression = condition.replace("\${{", "").replace("}}", "")
                        .replace("\\\"", "\"")
                    val (evaluateResult, _) = expressionEvaluate(
                        path = path,
                        expression = expression,
                        needBrackets = false,
                        distinguishTypes = null,
                        nameValues = nameValues,
                        context = context
                    )
                    newValue.append("$ifPrefix \"${evaluateResult}\"").append("\n")
                    line = bufferReader.readLine()
                    continue
                }

                val expIndexList = findExpressions(line).toMutableList()
                if (expIndexList.isEmpty()) {
                    newValue.append(line).append("\n")
                    line = bufferReader.readLine()
                    continue
                }

                var newLine = StringBuilder()
                for (i in expIndexList.indices) {
                    // 通过奇偶来判断，奇数代表当前index前是表达式字段，偶数代表当前index前是原字段
                    val isOdd = (i and 1) != 0

                    // 偶数需要将在他之前的原始字段加上
                    if (!isOdd) {
                        val startIndex = if (i == 0) {
                            0
                        } else {
                            expIndexList[i - 1] + 1
                        }
                        val endIndex = expIndexList[i]
                        newLine.append(line.substring(startIndex, endIndex))
                        continue
                    }

                    // 奇数需要将在他前的表达式替换后加上，同时如果是最后一位，需要加上末尾字段
                    val startIndex = expIndexList[i - 1]
                    val endIndex = expIndexList[i] + 1

                    // 表达式因为含有 ${{ }} 所以起始向后推3位，末尾往前推两位
                    // 这里 replace 因为yaml倒了好几手之后会出现在语句中的 \" 字符，使得正常的包含 " 的json无法替换
                    val expression = line.substring(startIndex + 3, endIndex - 2).replace("\\\"", "\"")

                    val (result, isComplete) = expressionEvaluate(
                        path = path,
                        expression = expression,
                        needBrackets = true,
                        distinguishTypes = setOf(DistinguishType.ARRAY),
                        nameValues = nameValues,
                        context = context
                    )

                    // ScriptUtils.formatYaml会将所有的带上 "" 但换时数组不需要"" 所以为数组去掉可能的额外的""
                    // 需要去掉额外""的情况只可能出现只替换了一次列表的情景，即作为参数 var_a: "{{ parameters.xxx }}"
                    var needFormatArr = false
                    val res = if (result.startsWith("'") && result.endsWith("'")) {
                        needFormatArr = (expIndexList.size == 2)
                        result.removeSurrounding("'", "'")
                    } else {
                        if (isComplete) {
                            // 对于字符传可能用的非转义的 \n \s 改为转义后的
                            StringEscapeUtils.escapeJava(result)
                        } else {
                            result
                        }
                    }
                    // 去掉上一个字段末尾可能的引号
                    if (needFormatArr) {
                        newLine = StringBuilder(newLine.removeSuffix("\""))
                    }

                    newLine.append(res)

                    // 对于奇数可能是最后一位，需要加上末尾
                    val isLineEnd = (i + 1) == expIndexList.size
                    var lineEndStartIndex = 0

                    // 将下一位的起始去掉可能的引号
                    if (needFormatArr && expIndexList[i] + 1 < line.length && line[expIndexList[i] + 1] == '"') {
                        if (!isLineEnd) {
                            expIndexList[i] = expIndexList[i] + 1
                        } else {
                            lineEndStartIndex = expIndexList[i] + 2
                        }
                    }

                    if (!isLineEnd) {
                        continue
                    }

                    // 对于奇数可能是最后一位，需要加上末尾
                    lineEndStartIndex = if (lineEndStartIndex == 0) {
                        expIndexList[i] + 1
                    } else {
                        lineEndStartIndex
                    }
                    if (lineEndStartIndex < line.length) {
                        newLine.append(line.substring(lineEndStartIndex))
                    }
                }
                newValue.append(newLine.toString()).append("\n")
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

    fun expressionEvaluate(
        path: String,
        expression: String,
        needBrackets: Boolean,
        distinguishTypes: Set<DistinguishType>?,
        nameValues: List<NamedValueInfo>,
        context: ExecutionContext
    ): Pair<String, Boolean> {
        val subInfo = SubNameValueEvaluateInfo(distinguishTypes = distinguishTypes)
        val (result, isComplete) = try {
            ExpressionParser.createSubNameValueEvaluateTree(
                expression, null, nameValues, null, subInfo
            )?.subNameValueEvaluate(null, context, null, subInfo)
                ?: throw YamlTemplateException("create evaluate tree is null")
        } catch (e: Throwable) {
            throw error(Constants.EXPRESSION_EVALUATE_ERROR.format(path, expression, e.message))
        }
        if (isComplete) {
            return Pair(result.trim(), true)
        }
        if (needBrackets) {
            return Pair("\${{ ${result.trim()} }}", false)
        }
        return Pair(result.trim(), false)
    }

    // 寻找语句中包含 ${{}}的表达式的位置，返回成对的位置坐标
    // 例如: 返回 [1,3,4,8] 指的是1到3 4到8为表达式
    fun findExpressions(condition: String): List<Int> {
        var index = 0
        val chars = condition.toCharArray()
        var findLast = true
        var findFirst = false
        val result = mutableListOf<Int>()
        while (index < chars.size) {
            if (findLast &&
                index + 2 < chars.size &&
                chars[index] == '$' && chars[index + 1] == '{' && chars[index + 2] == '{'
            ) {
                findFirst = true
                findLast = false
                result.add(index)
                index += 3
                continue
            }

            if (findFirst &&
                index + 1 < chars.size &&
                chars[index] == '}' && chars[index + 1] == '}'
            ) {
                findFirst = false
                findLast = true
                result.add(index + 1)
                index += 2
                continue
            }

            index++
        }
        // 如果是奇数，说明只找到了开头没找到结尾，直接舍弃
        if (result.size != 0 && (result.size and 1) != 0) {
            result.removeLast()
        }

        return result
    }

    private fun nativeTypeToContext(value: Any): PipelineContextData {
        return when (value) {
            is Char, is String -> StringContextData(value.toString())
            is Number -> NumberContextData(value.toDouble())
            is Boolean -> BooleanContextData(value)
            else -> StringContextData(value.toString())
        }
    }

    private fun error(content: String) = YamlFormatException(content)

    /**
     * 为模板中的变量赋值(旧版本只是为了兼容，非必要不要使用)
     * @param fromPath 来自哪个文件
     * @param path 读取的哪个模板文件
     * @param template 被读取的模板文件内容
     * @param templateParameters 被读取的模板文件自带的参数
     * @param parameters 引用模板文件时传入的参数
     */
    @Deprecated("旧版本，只是为了兼容，非必要不要使用")
    fun parseTemplateParametersOld(
        fromPath: String,
        path: String,
        template: String,
        templateParameters: MutableList<Parameters>?,
        parameters: Map<String, Any?>?
    ): String {
        if (!templateParameters.isNullOrEmpty()) {
            templateParameters.forEachIndexed { index, param ->
                if (parameters != null) {
                    val valueName = param.name

                    if (param.name.contains(".")) {
                        logger.info("PARAMETERS|NAME|WARNING|${param.name}")
                    }

                    val newValue = parameters[param.name]
                    if (parameters.keys.contains(valueName)) {
                        if (!param.values.isNullOrEmpty() && !param.values!!.contains(newValue)) {
                            kotlin.error(
                                Constants.VALUE_NOT_IN_ENUM.format(
                                    fromPath,
                                    valueName,
                                    newValue,
                                    param.values.joinToString(",")
                                )
                            )
                        } else {
                            templateParameters[index] = param.copy(default = newValue)
                        }
                    }
                }
            }
        } else {
            return template
        }
        // 模板替换 先替换调用模板传入的参数，再替换模板的默认参数
        val parametersListMap = templateParameters.filter {
            it.default != null && it.type == ParametersType.ARRAY.value
        }.associate {
            "parameters.${it.name}" to it.default
        }
        val parametersStringMap = templateParameters.filter { it.default != null }.associate {
            "parameters.${it.name}" to if (it.default == null) {
                null
            } else {
                it.default.toString()
            }
        }
        val replacedList = ScriptYmlUtils.parseParameterValue(template, parametersListMap, ParametersType.ARRAY)
        return ScriptYmlUtils.parseParameterValue(replacedList, parametersStringMap, ParametersType.STRING)
    }
}

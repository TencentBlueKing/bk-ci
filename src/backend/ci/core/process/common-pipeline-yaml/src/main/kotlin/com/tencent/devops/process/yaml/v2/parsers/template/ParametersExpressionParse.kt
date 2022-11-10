package com.tencent.devops.process.yaml.v2.parsers.template

import com.fasterxml.jackson.databind.JsonNode
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.expression.ExecutionContext
import com.tencent.devops.common.expression.ExpressionParser
import com.tencent.devops.common.expression.SubNameValueEvaluateInfo
import com.tencent.devops.common.expression.SubNameValueEvaluateResult
import com.tencent.devops.common.expression.SubNameValueResultType
import com.tencent.devops.common.expression.context.ArrayContextData
import com.tencent.devops.common.expression.context.BooleanContextData
import com.tencent.devops.common.expression.context.ContextValueNode
import com.tencent.devops.common.expression.context.DictionaryContextData
import com.tencent.devops.common.expression.context.ExpressionContextData
import com.tencent.devops.common.expression.context.NumberContextData
import com.tencent.devops.common.expression.context.PipelineContextData
import com.tencent.devops.common.expression.context.StringContextData
import com.tencent.devops.common.expression.expression.FunctionInfo
import com.tencent.devops.common.expression.expression.sdk.NamedValueInfo
import com.tencent.devops.common.expression.expression.specialFuctions.hashFiles.HashFilesFunction
import com.tencent.devops.process.yaml.v2.exception.YamlFormatException
import com.tencent.devops.process.yaml.v2.exception.YamlTemplateException
import com.tencent.devops.process.yaml.v2.parameter.Parameters
import com.tencent.devops.process.yaml.v2.parameter.ParametersType
import com.tencent.devops.process.yaml.v2.parsers.template.models.ExpressionBlock
import org.apache.commons.text.StringEscapeUtils
import org.apache.tools.ant.filters.StringInputStream
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * 模板参数表达式解析相关
 */
@Suppress("ALL")
object ParametersExpressionParse {
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

                ParametersType.BOOLEAN.value, ParametersType.NUMBER.value -> {
                    parameterContext.add(param.name, nativeTypeToContext(param.default!!))
                }

                ParametersType.STRING.value -> {
                    val value = param.default!!.toString()
                    // 针对插入表达式单独处理
                    if (value.trim().startsWith("\${{") && value.trim().endsWith("}}")) {
                        parameterContext.add(param.name, ExpressionContextData(value.trim()))
                    } else {
                        parameterContext.add(param.name, nativeTypeToContext(param.default))
                    }
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
                // 跳过空行和注释行，如果一行除空格外最左是 # 那一定是注释
                if (line.isBlank() || line.trimStart().startsWith("#")) {
                    newValue.append(line).append("\n")
                    line = bufferReader.readLine()
                    continue
                }

                val lineString = line.trim().replace("\\s".toRegex(), "")
                // if 表达式替换
                if (lineString.startsWith("if:") || lineString.startsWith("-if:")) {
                    val ifPrefix = line.substring(0 until line.indexOfFirst { it == ':' } + 1)

                    var condition = line.removePrefix(ifPrefix).trim().removeSurrounding("\"")
                    // if如果没有需要预先添加一个双括号，方便后面的替换
                    condition = "\${{$condition}}"

                    val blocks = findExpressions(condition)

                    val newLine = parseExpression(
                        line = condition,
                        blocks = blocks,
                        path = path,
                        needBrackets = false,
                        nameValues = nameValues,
                        context = context
                    )
                    newLine.removePrefix("\${{")
                    newLine.removeSuffix("}}")

                    newValue.append("$ifPrefix \"${newLine}\"").append("\n")
                    line = bufferReader.readLine()
                    continue
                }

                val condition = line
                val blocks = findExpressions(condition)
                if (blocks.isEmpty()) {
                    newValue.append(line).append("\n")
                    line = bufferReader.readLine()
                    continue
                }

                val newLine = parseExpression(
                    line = condition,
                    blocks = blocks,
                    path = path,
                    needBrackets = true,
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
     * 寻找语句中包含 ${{}}的表达式的位置，返回成对的位置坐标，并根据优先级排序
     * 优先级算法目前暂定为 从里到外，从左到右
     * @param levelMax 返回的最大层数，从深到浅。默认为2层
     * 例如: 替换顺序如数字所示 ${{ 4  ${{ 2 ${{ 1 }} }} ${{ 3 }} }}
     * @return [ 层数次序 [ 括号 ] ]  [[1], [2, 3], [4]]]]
     */
    fun findExpressions(condition: String, levelMax: Int = 2): List<List<ExpressionBlock>> {
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
     * 解析表达式，根据 findExpressions 寻找的括号优先级进行解析
     */
    fun parseExpression(
        line: String,
        blocks: List<List<ExpressionBlock>>,
        path: String,
        needBrackets: Boolean,
        nameValues: List<NamedValueInfo>,
        context: ExecutionContext
    ): String {
        var lineChars = line.toList()
        blocks.forEachIndexed { blockLevel, blocksInLevel ->
            blocksInLevel.forEachIndexed { blockI, block ->
                // 表达式因为含有 ${{ }} 所以起始向后推3位，末尾往前推两位
                val expression = lineChars.joinToString("").substring(block.startIndex + 3, block.endIndex - 1)

                val result = expressionEvaluate(
                    path = path,
                    expression = expression,
                    needBrackets = needBrackets,
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

    private val functionList = listOf(
        FunctionInfo(
            HashFilesFunction.name,
            1,
            Byte.MAX_VALUE.toInt(),
            HashFilesFunction()
        )
    )

    fun expressionEvaluate(
        path: String,
        expression: String,
        needBrackets: Boolean,
        nameValues: List<NamedValueInfo>,
        context: ExecutionContext
    ): SubNameValueEvaluateResult {
        val subInfo = SubNameValueEvaluateInfo()
        val (value, isComplete, type) = try {
            ExpressionParser.createSubNameValueEvaluateTree(
                expression, null, nameValues, functionList, subInfo
            )?.subNameValueEvaluate(null, context, null, subInfo, null)
                ?: throw YamlTemplateException("create evaluate tree is null")
        } catch (e: Throwable) {
            throw error(Constants.EXPRESSION_EVALUATE_ERROR.format(path, expression, e.message))
        }
        if (isComplete) {
            return SubNameValueEvaluateResult(value.trim(), true, type)
        }
        if (needBrackets) {
            return SubNameValueEvaluateResult("\${{ ${value.trim()} }}", false, type)
        }
        return SubNameValueEvaluateResult(value.trim(), false, type)
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

    private fun nativeTypeToContext(value: Any): PipelineContextData {
        return when (value) {
            is Char, is String -> StringContextData(value.toString())
            is Number -> NumberContextData(value.toDouble())
            is Boolean -> BooleanContextData(value)
            else -> StringContextData(value.toString())
        }
    }

    private fun error(content: String) = YamlFormatException(content)
}

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
package com.tencent.devops.openapi.service.doc

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.util.FileUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.BK_ALL_MODEL_DATA
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.BK_APPLICATION_STATE_REQUIRED
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.BK_BODY_PARAMETER
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.BK_CURL_PROMPT
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.BK_DEFAULT_VALUE
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.BK_DISCRIMINATOR_ILLUSTRATE
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.BK_ERROR_PROMPT
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.BK_HAVE_TO
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.BK_HEADER_PARAMETER
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.BK_HTTP_CODE
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.BK_ILLUSTRATE
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.BK_INPUT_PARAMETER_DESCRIPTION
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.BK_MUST_BE
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.BK_NO
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.BK_OBJECT_PROPERTY_ILLUSTRATE
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.BK_PARAM_ILLUSTRATE
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.BK_PARAM_NAME
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.BK_PARAM_TYPE
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.BK_PATH_PARAMETER
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.BK_PAYLOAD_REQUEST_SAMPLE
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.BK_POLYMORPHIC_CLASS_IMPLEMENTATION
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.BK_POLYMORPHISM_MODEL_ILLUSTRATE
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.BK_QUERY_PARAMETER
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.BK_REQUEST_METHOD
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.BK_REQUEST_SAMPLE
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.BK_RESOURCE_DESCRIPTION
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.BK_RESPONSE_PARAMETER
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.BK_RETURNS_THE_SAMPLE
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.BK_THE_FIELD_IS_READ_ONLY
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.BK_USER_NAME
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.BK_YES
import com.tencent.devops.openapi.pojo.SwaggerDocParameterInfo
import com.tencent.devops.openapi.pojo.SwaggerDocResponse
import com.tencent.devops.openapi.utils.markdown.Code
import com.tencent.devops.openapi.utils.markdown.Link
import com.tencent.devops.openapi.utils.markdown.MarkdownElement
import com.tencent.devops.openapi.utils.markdown.Table
import com.tencent.devops.openapi.utils.markdown.TableRow
import com.tencent.devops.openapi.utils.markdown.Text
import io.swagger.v3.oas.integration.SwaggerConfiguration
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.MapSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.servers.Server
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
@Suppress("ComplexMethod")
class DocumentService {
    @Value("\${spring.application.desc:#{null}}")
    private val applicationDesc: String = "DevOps openapi Service"

    @Value("\${spring.application.version:#{null}}")
    private val applicationVersion: String = "4.0"

    @Value("\${spring.application.packageName:#{null}}")
    private val packageName: String = "com.tencent.devops.openapi"

    @Value("\${spring.application.name:#{null}}")
    private val service: String = "openapi"

    private val onLoadTable = mutableMapOf<String, Table>()

    private val definitions = mutableMapOf<String, Schema<*>>()

    private lateinit var polymorphismMap: Map<String, Map<String, String>>

    /**
     * swagger生成 markdown 文档。然后归档
     */
    @Suppress("NestedBlockDepth", "LongMethod")
    fun docInit(
        checkMetaData: Boolean,
        checkMDData: Boolean,
        polymorphism: Map<String, Map<String, String>> = emptyMap(),
        outputPath: String? = null,
        parametersInfo: Map<String, Map<String, SwaggerDocParameterInfo>>? = null
    ): Map<String, SwaggerDocResponse> {
        val response = mutableMapOf<String, SwaggerDocResponse>()
        val swagger = loadSwagger()
        definitions.putAll(swagger.components.schemas)
        polymorphismMap = polymorphism
        loadAllDefinitions(parametersInfo)
        swagger.paths.forEach { (path, body) ->
            body.parameters
            // 遍历并生成每一path 不同 HttpMethod 下的文档
            body.readOperationsMap().forEach { (httpMethod, operation) ->
                val loadMarkdown = mutableListOf<MarkdownElement>()
                // 该path 需要组装的model
                val onLoadModel = mutableListOf<String>()
                // 该path 已组装的model
                val loadedModel = mutableListOf<String>()
//                loadMarkdown.add(Text(level = 1, body = "资源文档: ${operation.tags}", key = "resource_documentation"))
                loadMarkdown.add(Text(level = 3, body = getI18n(BK_REQUEST_METHOD), key = "request_method"))
                loadMarkdown.add(Text(level = 4, body = "$httpMethod $path", key = "http_method_path"))
                loadMarkdown.add(Text(level = 3, body = getI18n(BK_RESOURCE_DESCRIPTION), key = "resource_description"))
                loadMarkdown.add(Text(level = 4, body = operation.summary ?: "", key = "summary"))
                loadMarkdown.add(
                    Text(level = 3, body = getI18n(BK_INPUT_PARAMETER_DESCRIPTION), key = "input_parameter_description")
                )
                loadMarkdown.add(Text(level = 4, body = getI18n(BK_PATH_PARAMETER), key = "path_parameter_title"))
                loadMarkdown.add(
                    cacheOrLoad({
                        Table(
                            header = TableRow(
                                getI18n(BK_PARAM_NAME),
                                getI18n(BK_PARAM_TYPE),
                                getI18n(BK_HAVE_TO),
                                getI18n(BK_PARAM_ILLUSTRATE),
                                getI18n(BK_DEFAULT_VALUE)
                            ),
                            rows = parseParameters(operation.parameters.filterIsInstance<Parameter>()),
                            key = "path_parameter"
                        ).checkLoadModel(onLoadModel)
                    }, path + httpMethod + "path")
                )
                loadMarkdown.add(Text(4, getI18n(BK_QUERY_PARAMETER), "query_parameter_title"))
                loadMarkdown.add(
                    cacheOrLoad({
                        Table(
                            header = TableRow(
                                getI18n(BK_PARAM_NAME),
                                getI18n(BK_PARAM_TYPE),
                                getI18n(BK_HAVE_TO),
                                getI18n(BK_PARAM_ILLUSTRATE),
                                getI18n(BK_DEFAULT_VALUE)
                            ),
                            rows = parseParameters(operation.parameters.filterIsInstance<Parameter>()),
                            "query_parameter"
                        ).checkLoadModel(onLoadModel)
                    }, path + httpMethod + "query")
                )
                loadMarkdown.add(Text(4, getI18n(BK_HEADER_PARAMETER), "header_parameter_title"))
                loadMarkdown.add(
                    cacheOrLoad({
                        Table(
                            header = TableRow(
                                getI18n(BK_PARAM_NAME),
                                getI18n(BK_PARAM_TYPE),
                                getI18n(BK_HAVE_TO),
                                getI18n(BK_PARAM_ILLUSTRATE),
                                getI18n(BK_DEFAULT_VALUE)
                            ),
                            rows = parseParameters(operation.parameters.filterIsInstance<Parameter>()),
                            "header_parameter"
                        ).checkLoadModel(onLoadModel)
                            .setRow(
                                AUTH_HEADER_USER_ID,
                                "string",
                                getI18n(BK_APPLICATION_STATE_REQUIRED),
                                getI18n(BK_USER_NAME),
                                "{X-DEVOPS-UID}"
                            )
                            .setRow("Content-Type", "string", getI18n(BK_YES), "", "application/json")
                            .removeRow(AUTH_HEADER_DEVOPS_APP_CODE)
                    }, path + httpMethod + "header")
                )
                loadMarkdown.add(Text(4, getI18n(BK_BODY_PARAMETER), "body_parameter_title"))
                loadMarkdown.add(
                    cacheOrLoad({
                        Table(
                            header = TableRow(
                                getI18n(BK_PARAM_NAME),
                                getI18n(BK_PARAM_TYPE),
                                getI18n(BK_HAVE_TO),
                                getI18n(BK_PARAM_ILLUSTRATE),
                                getI18n(BK_DEFAULT_VALUE)
                            ),
                            rows = parseParameters(operation.parameters),
                            "body_parameter"
                        ).checkLoadModel(onLoadModel)
                    }, path + httpMethod + "body")
                )
                loadMarkdown.add(Text(4, getI18n(BK_RESPONSE_PARAMETER), "response_parameter_title"))
                loadMarkdown.add(
                    cacheOrLoad({
                        Table(
                            header = TableRow(getI18n(BK_HTTP_CODE), getI18n(BK_PARAM_TYPE), getI18n(BK_ILLUSTRATE)),
                            rows = parseResponse(operation.responses),
                            "response_parameter"
                        ).checkLoadModel(onLoadModel)
                    }, path + httpMethod + "response")
                )
                // payload 样例
                loadMarkdown.addAll(
                    parsePayloadExample(
                        operation.parameters.filterIsInstance<RequestBody>()
                    )
                )
                loadMarkdown.add(
                    Text(3, "Curl ${getI18n(BK_REQUEST_SAMPLE)}", "curl_request_sample_title")
                )
                loadMarkdown.add(
                    Code(
                        "Json",
                        parseCurlExample(
                            httpMethod = httpMethod.name,
                            query = cacheOrLoad({ null }, path + httpMethod + "query"),
                            header = cacheOrLoad({ null }, path + httpMethod + "header")
                        ),
                        "curl_request_sample"
                    )
                )
                // 组装请求样例
                loadMarkdown.addAll(
                    parseRequestExampleJson(
                        httpMethod.name,
                        operation.parameters.filterIsInstance<RequestBody>()
                    )
                )
                // 组装返回样例
                loadMarkdown.addAll(
                    parseResponseExampleJson(
                        operation.responses
                    )
                )
                loadMarkdown.add(Text(3, getI18n(BK_ALL_MODEL_DATA), "all_model_data"))
                // 组装所有已使用的模型
                loadMarkdown.addAll(parseAllModel(onLoadModel, loadedModel))
                operation.tags.forEach { tag ->
                    response[tag] = SwaggerDocResponse(
                        path = path,
                        httpMethod = httpMethod.name,
                        markdown = if (checkMDData) loadMarkdown.joinToString(separator = "") else null,
                        metaData = if (checkMetaData) loadMarkdown else null
                    )
                    if (!outputPath.isNullOrBlank()) {
                        FileUtil.outFile(outputPath, "$tag.md", loadMarkdown.joinToString(separator = ""))
                    }
                }
            }
        }
        if (!outputPath.isNullOrBlank()) {
            FileUtil.outFile(outputPath, "all.json", JsonUtil.toJson(response))
        }
        onLoadTable.clear()
        definitions.clear()
        return response
    }

    private fun cacheOrLoad(func: () -> Table?, key: String): Table {
        val cache = onLoadTable[key]
        if (cache != null) {
            return cache
        }
        val load = func() ?: return Table()
        onLoadTable[key] = load
        return load
    }

    private fun loadAllDefinitions(parametersInfo: Map<String, Map<String, SwaggerDocParameterInfo>>? = null) {
        definitions.forEach { (key, model) ->
            cacheOrLoad({
                val tableRows = mutableListOf<TableRow>()
                loadModelDefinitions(model, tableRows)
                tableRows.forEach { table ->
                    val reflectInfo = parametersInfo?.get(key)?.get(table.columns[0])
                    if (reflectInfo != null) {
                        val column = table.columns.toMutableList()
                        column[2] = if (reflectInfo.markedNullable.not()) getI18n(BK_YES) else getI18n(BK_NO)
                        column[4] = if (reflectInfo.markedNullable) reflectInfo.defaultValue ?: "" else column[4]
                        table.columns = column
                    }
                }
                Table(
                    header = TableRow(
                        getI18n(BK_PARAM_NAME),
                        getI18n(BK_PARAM_TYPE),
                        getI18n(BK_HAVE_TO),
                        getI18n(BK_PARAM_ILLUSTRATE),
                        getI18n(BK_DEFAULT_VALUE)
                    ),
                    rows = tableRows,
                    key = "model_$key"
                )
            }, key)
        }
    }

    private fun parseAllModel(
        modelList: List<String>,
        loadedModel: MutableList<String>
    ): List<MarkdownElement> {
        val markdownElement = mutableListOf<MarkdownElement>()
        modelList.forEach {
            if (it in loadedModel) return@forEach
            val onLoadModel = mutableListOf<String>()
            val model = cacheOrLoad({
                val tableRows = mutableListOf<TableRow>()
                definitions[it]?.let { model -> loadModelDefinitions(model, tableRows) }
                Table(
                    header = TableRow(
                        getI18n(BK_PARAM_NAME),
                        getI18n(BK_PARAM_TYPE),
                        getI18n(BK_HAVE_TO),
                        getI18n(BK_PARAM_ILLUSTRATE),
                        getI18n(BK_DEFAULT_VALUE)
                    ),
                    rows = tableRows,
                    key = "model_$it"
                )
            }, it).apply {
                if (it in polymorphismMap) {
                    this.setRow(
                        (definitions[it] as Schema).discriminator.toString(),
                        "string",
                        getI18n(BK_YES),
                        getI18n(BK_DISCRIMINATOR_ILLUSTRATE, arrayOf("${polymorphismMap[it]?.keys}")),
                        ""
                    )
                }
            }.checkLoadModel(onLoadModel)
            markdownElement.add(Text(level = 4, body = it, key = "model_${it}_title"))
            markdownElement.add(model)
            loadedModel.add(it)

            // 多态类展示
            polymorphismMap[it]?.forEach { (child, value) ->
                val discriminator = (definitions[it] as Schema).discriminator.toString()
                val childModel = cacheOrLoad({ null }, child)
                    .setRow(
                        discriminator,
                        "string",
                        getI18n(BK_MUST_BE, arrayOf(value)),
                        getI18n(BK_POLYMORPHIC_CLASS_IMPLEMENTATION),
                        value
                    ).checkLoadModel(onLoadModel)
                markdownElement.add(Text(4, child, "polymorphism_model_${child}_title"))
                markdownElement.add(
                    Text(
                        level = 0, body = getI18n(BK_POLYMORPHISM_MODEL_ILLUSTRATE, arrayOf(it, discriminator, value)),
                        key = "polymorphism_model_$child"
                    )
                )
                markdownElement.add(Text(level = 0, body = "", key = ""))
                markdownElement.add(childModel)
                loadedModel.add(child)
            }

            if (onLoadModel.isNotEmpty()) {
                markdownElement.addAll(parseAllModel(onLoadModel, loadedModel))
            }
        }
        return markdownElement
    }

    private fun parseResponseExampleJson(responses: Map<String, ApiResponse>): List<MarkdownElement> {
        val markdownElement = mutableListOf<MarkdownElement>()
        responses.forEach { (httpStatus, response) ->
            val loadJson = mutableMapOf<String, Any>()
            loadModelJson(response.content.values.first().schema, loadJson)
            markdownElement.add(
                Text(
                    level = 3,
                    body = "$httpStatus $BK_RETURNS_THE_SAMPLE",
                    key = "${httpStatus}_return_example_title"
                )
            )
            markdownElement.add(
                Code(language = "Json", body = JsonUtil.toJson(loadJson), key = "${httpStatus}_return_example")
            )
        }
        return markdownElement
    }

    private fun parsePayloadExample(body: List<RequestBody>): List<MarkdownElement> {
        val examples = body.getOrNull(0)?.content?.values?.first()?.examples
        if (examples.isNullOrEmpty()) return emptyList()
        val res = mutableListOf<MarkdownElement>()
        res.add(Text(level = 3, body = "Request Payload 举例", key = "Payload_request_sample_title"))
        res.add(
            Text(
                level = 0,
                body = getI18n(BK_ERROR_PROMPT),
                key = "Payload_request_sample_explain"
            )
        )
        examples.forEach { (texPlain, jsonSimple) ->
            res.add(
                Text(
                    level = 4,
                    body = getI18n(BK_PAYLOAD_REQUEST_SAMPLE, arrayOf(texPlain)),
                    key = "Payload_request_sample_title_$texPlain"
                )
            )
            val jsonString = try {
                JsonUtil.toJson(jsonSimple)
            } catch (e: Throwable) {
                jsonSimple.toString()
            }
            res.add(Code(language = "Json", body = jsonString, key = "Payload_request_sample_json_$texPlain"))
        }
        return res
    }

    private fun parseRequestExampleJson(httpMethod: String, body: List<RequestBody>): List<MarkdownElement> {
        if (body.isEmpty()) return emptyList()
        val schema = body[0].content.values.first().schema
        val outJson: Any = if (StringUtils.isNotBlank(schema.`$ref`)) {
            val loadJson = mutableMapOf<String, Any>()
            loadModelJson(schema, loadJson)
            loadJson
        } else when (schema) {
            is ComposedSchema -> {
                val loadJson = mutableMapOf<String, Any>()
                schema.allOf?.forEach {
                    loadModelJson(it, loadJson)
                }
                loadJson
            }

            is ArraySchema -> {
                val loadJson = mutableListOf<Any>()
                loadJson.add(loadPropertyJson(schema.items))
                loadJson
            }

            is Schema<*> -> {
                val loadJson = mutableMapOf<String, Any>()
                schema.properties?.forEach { (key, property) ->
                    loadJson[key] = loadPropertyJson(property)
                }
                loadJson
            }

            else -> {
                emptyMap<String, String>()
            }
        }
        return listOf(
            Text(
                level = 3,
                body = "$httpMethod ${getI18n(BK_REQUEST_SAMPLE)}",
                key = "${httpMethod}_request_sample_title"
            ),
            Code(language = "Json", body = JsonUtil.toJson(outJson), key = "${httpMethod}_request_sample")
        )
    }

    private fun parseCurlExample(httpMethod: String, query: Table, header: Table): String {
        val queryString = query.rows.takeIf { it.isNotEmpty() }?.joinToString(prefix = "?", separator = "&") {
            "${it.columns[0]}={${it.columns[0]}}"
        } ?: ""
        val headerString = header.rows.takeIf { it.isNotEmpty() }?.joinToString(prefix = "\\\n", separator = "\\\n") {
            "-H '${it.columns[0]}: ${it.columns[4]}' "
        } ?: ""
        return "curl -X ${httpMethod.toUpperCase()} ${getI18n(BK_CURL_PROMPT, arrayOf(queryString))} $headerString"
    }

    private fun parseResponse(responses: Map<String, ApiResponse>): List<TableRow> {
        val tableRow = mutableListOf<TableRow>()
        responses.forEach { (httpStatus, response) ->
            val schema = response.content.values.first().schema
            tableRow.addNoRepeat(
                TableRow(httpStatus, loadModelType(schema), response.description)
            )
        }
        return tableRow
    }

    private fun parseParameters(parameters: List<Parameter>): List<TableRow> {
        val tableRow = mutableListOf<TableRow>()
        parameters.forEach {
            if (StringUtils.isNotBlank(it.`$ref`)) {
                tableRow.addNoRepeat(
                    TableRow(
                        it.name,
                        Link(it.`$ref`, '#' + it.`$ref`).toString(),
                        if (it.required) getI18n(BK_YES) else getI18n(BK_NO),
                        it.description,
                        ""
                    )
                )
            } else {
                tableRow.addNoRepeat(
                    TableRow(
                        it.name,
                        loadSerializableParameter(it),
                        if (it.required) getI18n(BK_YES) else getI18n(BK_NO),
                        it.description,
                        it.schema.default?.toString() ?: ""
                    )
                )
            }
        }
        return tableRow
    }

    private fun loadSwagger(): OpenAPI {
        val bean = SwaggerConfiguration().apply {
            openAPI = OpenAPI()
                .info(Info().title(applicationDesc).version(applicationVersion))
                .addServersItem(Server().url("/$service/api"))
            resourcePackages = setOf(packageName)
        }
        return bean.openAPI
    }

    private fun loadSerializableParameter(parameter: Parameter): String {
        return when (val schema = parameter.schema) {
            is StringSchema -> {
                val enum = schema.enum
                if (enum.isNullOrEmpty()) {
                    "String"
                } else {
                    val str = enum.toEnumString()
                    "ENUM($str)"
                }
            }

            is ArraySchema -> {
                "List<" + loadPropertyType(schema.items) + ">"
            }

            is NumberSchema -> {
                when (schema.format) {
                    "int32" -> "Int"
                    "int64" -> "Long"
                    else -> "integer"
                }
            }

            else -> schema.type
        }
    }

    private fun loadModelDefinitions(
        model: Schema<*>,
        tableRow: MutableList<TableRow>
    ) {
        if (StringUtils.isNotBlank(model.`$ref`)) {
            tableRow.addAll(
                cacheOrLoad(
                    {
                        val table = mutableListOf<TableRow>()
                        definitions[model.`$ref`]?.let {
                            loadModelDefinitions(it, table)
                        }
                        Table(
                            header = TableRow(
                                getI18n(BK_PARAM_NAME),
                                getI18n(BK_PARAM_TYPE),
                                getI18n(BK_HAVE_TO),
                                getI18n(BK_PARAM_ILLUSTRATE),
                                getI18n(BK_DEFAULT_VALUE)
                            ),
                            rows = table,
                            key = "model_${model.`$ref`}"
                        )
                    }, model.`$ref`
                ).rows
            )
        }
        when (model) {
            is ComposedSchema -> {
                model.allOf?.forEach {
                    loadModelDefinitions(it, tableRow)
                }
            }

            else -> {
                model.properties?.forEach { (key, property) ->
                    tableRow.addNoRepeat(
                        TableRow(
                            key,
                            loadPropertyType(property),
                            if (!property.nullable) getI18n(BK_YES) else getI18n(BK_NO),
                            loadDescriptionInfo(property),
                            loadPropertyDefault(property)
                        )
                    )
                }
            }

        }
    }

    private fun loadDescriptionInfo(property: Schema<*>?): String {
        if (property == null) return ""
        val res = StringBuffer()
        if (property.readOnly == true) {
            res.append(getI18n(BK_THE_FIELD_IS_READ_ONLY))
        }
        res.append(property.description ?: "")
        return res.toString()
    }

    private fun loadModelType(model: Schema<*>?): String {
        if (model == null) return ""
        if (StringUtils.isNotBlank(model.`$ref`)) {
            return Link(model.`$ref`, '#' + model.`$ref`).toString()
        }
        return when (model) {
            is ArraySchema -> {
                "List<" + loadPropertyType(model.items) + ">"
            }

            else -> {
                "Map<String, " + model.additionalProperties + ">"
            }
        }
    }

    private fun loadModelJson(model: Schema<*>?, loadJson: MutableMap<String, Any>) {
        if (model == null) return
        if (StringUtils.isNotBlank(model.`$ref`)) {
            definitions[model.`$ref`]?.let { loadModelJson(it, loadJson) }
        }
        when (model) {
            is ComposedSchema -> {
                model.allOf?.forEach {
                    loadModelJson(it, loadJson)
                }
            }

            else -> {
                if (model.discriminator != null) {
                    loadJson[model.discriminator.toString()] = "string"
                }
                model.properties?.forEach { (key, property) ->
                    loadJson[key] = loadPropertyJson(property)
                }
            }
        }
    }

    private fun loadPropertyJson(property: Schema<*>): Any {
        if (StringUtils.isNotBlank(property.`$ref`)) {
            val loadJson = mutableMapOf<String, Any>()
            definitions[property.`$ref`]?.let { loadModelJson(it, loadJson) }
            return loadJson
        }
        return when (property) {
            // swagger无法获取到map的key类型
            is MapSchema -> {
                mapOf("string" to property.additionalProperties)
            }

            is ObjectSchema -> {
                getI18n(BK_OBJECT_PROPERTY_ILLUSTRATE)
            }

            is ArraySchema -> {
                listOf(loadPropertyJson(property.items))
            }

            is StringSchema -> {
                if (property.enum == null) {
                    property.type
                } else {
                    "enum"
                }
            }

            is BooleanSchema -> false
            else -> {
                val result = when (property.default) {
                    is Int -> 0
                    is Long -> 0L
                    is Double -> 0.0
                    is Float -> 0f
                    else -> property.type
                }
                result
            }
        }
    }

    private fun loadPropertyDefault(property: Schema<*>?): String? {
        if (property == null || property.default == null) return null
        return property.default.toString()
    }

    private fun loadPropertyType(property: Schema<*>?): String {
        if (property == null) return ""
        if (StringUtils.isNotBlank(property.`$ref`)) {
            return Link(property.`$ref`, '#' + property.`$ref`).toString()
        }
        return when (property) {
            // swagger无法获取到map的key类型
            is MapSchema -> {
                "Map<String, " + property.additionalProperties + ">"
            }

            is ObjectSchema -> {
                "Any"
            }

            is ArraySchema -> {
                "List<" + loadPropertyType(property.items) + ">"
            }

            is StringSchema -> {
                if (property.enum.isNullOrEmpty()) {
                    property.type
                } else {
                    "ENUM(" + property.enum.toEnumString() + ")"
                }
            }

            else -> {
                property.type
            }
        }
    }

    private fun <T> List<T>.toEnumString(): String {
        var result = ""
        this.forEachIndexed { index, any ->
            if (index == this.size - 1) {
                result = "$result$any"
                return@forEachIndexed
            }
            result = "$result$any, "
        }
        return result
    }

    private fun MutableList<TableRow>.addNoRepeat(table: TableRow) {
        val row = this.find { it.columns[0] == table.columns[0] }
        if (row != null) {
            this.remove(row)
        }
        this.add(table)
    }

    private fun getI18n(code: String, params: Array<String>? = null): String {
        return MessageUtil.getMessageByLocale(
            messageCode = code,
            language = "zh_CN",
            params = params
        )
    }
}

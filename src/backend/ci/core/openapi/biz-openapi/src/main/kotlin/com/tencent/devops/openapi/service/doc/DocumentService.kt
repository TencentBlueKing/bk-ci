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
import io.swagger.jaxrs.config.BeanConfig
import io.swagger.models.ArrayModel
import io.swagger.models.ComposedModel
import io.swagger.models.Model
import io.swagger.models.ModelImpl
import io.swagger.models.RefModel
import io.swagger.models.Response
import io.swagger.models.Swagger
import io.swagger.models.parameters.AbstractSerializableParameter
import io.swagger.models.parameters.BodyParameter
import io.swagger.models.parameters.HeaderParameter
import io.swagger.models.parameters.Parameter
import io.swagger.models.parameters.PathParameter
import io.swagger.models.parameters.QueryParameter
import io.swagger.models.parameters.RefParameter
import io.swagger.models.parameters.SerializableParameter
import io.swagger.models.properties.ArrayProperty
import io.swagger.models.properties.BooleanProperty
import io.swagger.models.properties.DoubleProperty
import io.swagger.models.properties.FloatProperty
import io.swagger.models.properties.IntegerProperty
import io.swagger.models.properties.LongProperty
import io.swagger.models.properties.MapProperty
import io.swagger.models.properties.ObjectProperty
import io.swagger.models.properties.PasswordProperty
import io.swagger.models.properties.Property
import io.swagger.models.properties.RefProperty
import io.swagger.models.properties.StringProperty
import io.swagger.models.properties.UUIDProperty
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

    private val definitions = mutableMapOf<String, Model>()

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
        definitions.putAll(swagger.definitions)
        polymorphismMap = polymorphism
        loadAllDefinitions(parametersInfo)
        swagger.paths.forEach { (path, body) ->
            // 遍历并生成每一path 不同 HttpMethod 下的文档
            body.operationMap.forEach { (httpMethod, operation) ->
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
                            rows = parseParameters(operation.parameters.filterIsInstance<PathParameter>()),
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
                            rows = parseParameters(operation.parameters.filterIsInstance<QueryParameter>()),
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
                            rows = parseParameters(operation.parameters.filterIsInstance<HeaderParameter>()),
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
                            rows = parseParameters(operation.parameters.filterIsInstance<BodyParameter>()),
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
                        operation.parameters.filterIsInstance<BodyParameter>()
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
                        operation.parameters.filterIsInstance<BodyParameter>()
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
                        (definitions[it] as ModelImpl).discriminator,
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
                val discriminator = (definitions[it] as ModelImpl).discriminator
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

    private fun parseResponseExampleJson(responses: Map<String, Response>): List<MarkdownElement> {
        val markdownElement = mutableListOf<MarkdownElement>()
        responses.forEach { (httpStatus, response) ->
            val loadJson = mutableMapOf<String, Any>()
            loadModelJson(response.responseSchema, loadJson)
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

    private fun parsePayloadExample(body: List<BodyParameter>): List<MarkdownElement> {
        if (body.getOrNull(0)?.examples?.isEmpty() != false) return emptyList()
        val res = mutableListOf<MarkdownElement>()
        res.add(Text(level = 3, body = "Request Payload 举例", key = "Payload_request_sample_title"))
        res.add(
            Text(
                level = 0,
                body = getI18n(BK_ERROR_PROMPT),
                key = "Payload_request_sample_explain"
            )
        )
        body[0].examples.forEach { (texplain, jsonSimple) ->
            res.add(
                Text(
                    level = 4,
                    body = getI18n(BK_PAYLOAD_REQUEST_SAMPLE, arrayOf(texplain)),
                    key = "Payload_request_sample_title_$texplain"
                )
            )
            val jsonString = try {
                JsonUtil.toJson(JsonUtil.to(jsonSimple))
            } catch (e: Throwable) {
                jsonSimple
            }
            res.add(Code(language = "Json", body = jsonString, key = "Payload_request_sample_json_$texplain"))
        }
        return res
    }

    private fun parseRequestExampleJson(httpMethod: String, body: List<BodyParameter>): List<MarkdownElement> {
        if (body.isEmpty()) return emptyList()
        val schema = body[0].schema
        val outJson: Any = when (schema) {
            is ComposedModel -> {
                val loadJson = mutableMapOf<String, Any>()
                schema.allOf?.forEach {
                    loadModelJson(it, loadJson)
                }
                loadJson
            }
            is ModelImpl -> {
                val loadJson = mutableMapOf<String, Any>()
                schema.properties?.forEach { (key, property) ->
                    loadJson[key] = loadPropertyJson(property)
                }
                loadJson
            }
            is RefModel -> {
                val loadJson = mutableMapOf<String, Any>()
                loadModelJson(schema, loadJson)
                loadJson
            }
            is ArrayModel -> {
                val loadJson = mutableListOf<Any>()
                loadJson.add(loadPropertyJson(schema.items))
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

    private fun parseResponse(responses: Map<String, Response>): List<TableRow> {
        val tableRow = mutableListOf<TableRow>()
        responses.forEach { (httpStatus, response) ->
            tableRow.addNoRepeat(
                TableRow(httpStatus, loadModelType(response.responseSchema), response.description)
            )
        }
        return tableRow
    }

    private fun parseParameters(parameters: List<Parameter>): List<TableRow> {
        val tableRow = mutableListOf<TableRow>()
        parameters.forEach {
            when (it) {
                is BodyParameter -> {
                    tableRow.addNoRepeat(
                        TableRow(
                            it.name,
                            loadModelType(it.schema),
                            if (it.required) getI18n(BK_YES) else getI18n(BK_NO),
                            it.description,
                            ""
                        )
                    )
                }
                is AbstractSerializableParameter<*> -> {
                    tableRow.addNoRepeat(
                        TableRow(
                            it.name,
                            loadSerializableParameter(it),
                            if (it.required) getI18n(BK_YES) else getI18n(BK_NO),
                            it.description,
                            it.defaultValue
                        )
                    )
                }
//                is PathParameter -> {}
//                is QueryParameter -> {}
                is RefParameter -> {
                    tableRow.addNoRepeat(
                        TableRow(
                            it.name,
                            Link(it.originalRef, '#' + it.originalRef).toString(),
                            if (it.required) getI18n(BK_YES) else getI18n(BK_NO),
                            it.description,
                            ""
                        )
                    )
                }
                else -> {}
            }
        }
        return tableRow
    }

    private fun loadSwagger(): Swagger {
        val bean = BeanConfig().apply {
            title = applicationDesc
            version = applicationVersion
            resourcePackage = packageName
            scan = true
            basePath = "/$service/api"
        }
        return bean.swagger
    }

    private fun loadSerializableParameter(parameter: SerializableParameter): String {
        return when (parameter.type) {
            "string" -> {
                val enum = parameter.enumValue
                if (enum.isNullOrEmpty()) {
                    parameter.type
                } else {
                    val str = enum.toEnumString()
                    "ENUM($str)"
                }
            }
            "array" -> {
                "List<" + loadPropertyType(parameter.items) + ">"
            }
            "integer" -> {
                when (parameter.format) {
                    "int32" -> "Int"
                    "int64" -> "Long"
                    else -> "integer"
                }
            }
            else -> parameter.type
        }
    }

    private fun loadModelDefinitions(
        model: Model,
        tableRow: MutableList<TableRow>
    ) {
        when (model) {
            is ComposedModel -> {
                model.allOf?.forEach {
                    loadModelDefinitions(it, tableRow)
                }
//
//                // 初始化多态类
//                if (model.parent is RefModel && model.child is ModelImpl) {
//                    val ref = model.parent as RefModel
//                    val impl = model.child as ModelImpl
//                    polymorphismMap[ref.originalRef].apply {
//                        if (this == null) {
//                            polymorphismMap[ref.originalRef] = mutableListOf(impl.name)
//                        } else {
//                            this.addNoRepeat(impl.name)
//                        }
//                    }
//                }
            }

            is ModelImpl -> {
                model.properties?.forEach { (key, property) ->
                    tableRow.addNoRepeat(
                        TableRow(
                            key,
                            loadPropertyType(property),
                            if (property.required) getI18n(BK_YES) else getI18n(BK_NO),
                            loadDescriptionInfo(property),
                            loadPropertyDefault(property)
                        )
                    )
                }
            }

            is RefModel -> {
                tableRow.addAll(
                    cacheOrLoad(
                        {
                            val table = mutableListOf<TableRow>()
                            definitions[model.originalRef]?.let {
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
                                key = "model_${model.originalRef}"
                            )
                        }, model.originalRef
                    ).rows
                )
            }
            else -> {}
        }
    }

    private fun loadDescriptionInfo(property: Property?): String {
        if (property == null) return ""
        val res = StringBuffer()
        if (property.readOnly == true) {
            res.append(getI18n(BK_THE_FIELD_IS_READ_ONLY))
        }
        res.append(property.description ?: "")
        return res.toString()
    }

    private fun loadModelType(model: Model?): String {
        if (model == null) return ""
        return when (model) {
//            is ComposedModel -> {}
            is ModelImpl -> {
                "Map<String, " + loadPropertyType(model.additionalProperties) + ">"
            }
            is RefModel -> {
                Link(model.originalRef, '#' + model.originalRef).toString()
            }
            is ArrayModel -> {
                "List<" + loadPropertyType(model.items) + ">"
            }
            else -> {
                "parse error"
            }
        }
    }

    private fun loadModelJson(model: Model?, loadJson: MutableMap<String, Any>) {
        if (model == null) return
        when (model) {
            is ComposedModel -> {
                model.allOf?.forEach {
                    loadModelJson(it, loadJson)
                }
            }
            is ModelImpl -> {
                if (model.discriminator != null) {
                    loadJson[model.discriminator] = "string"
                }
                model.properties?.forEach { (key, property) ->
                    loadJson[key] = loadPropertyJson(property)
                }
            }
            is RefModel -> {
                definitions[model.originalRef]?.let { loadModelJson(it, loadJson) }
            }
            else -> {}
        }
    }

    private fun loadPropertyJson(property: Property): Any {
        return when (property) {
            is RefProperty -> {
                val loadJson = mutableMapOf<String, Any>()
                definitions[property.originalRef]?.let { loadModelJson(it, loadJson) }
                loadJson
            }
            // swagger无法获取到map的key类型
            is MapProperty -> {
                mapOf("string" to loadPropertyJson(property.additionalProperties))
            }
            is ObjectProperty -> {
                getI18n(BK_OBJECT_PROPERTY_ILLUSTRATE)
            }
            is ArrayProperty -> {
                listOf(loadPropertyJson(property.items))
            }
            is StringProperty -> {
                if (property.enum == null) {
                    property.type
                } else {
                    "enum"
                }
            }
            is BooleanProperty -> false
            is IntegerProperty -> 0
            is LongProperty -> 0L
            is DoubleProperty -> 0.0
            is FloatProperty -> 0f
            else -> {
                property.type
            }
        }
    }

    private fun loadPropertyDefault(property: Property?): String? {
        if (property == null) return null
        return when (property) {
            is BooleanProperty -> {
                property.default?.toString()
            }
            is DoubleProperty -> {
                property.default?.toString()
            }
            is FloatProperty -> {
                property.default?.toString()
            }
            is IntegerProperty -> {
                property.default?.toString()
            }
            is LongProperty -> {
                property.default?.toString()
            }
            is StringProperty -> {
                property.default?.toString()
            }
            is PasswordProperty -> {
                property.default?.toString()
            }
            is UUIDProperty -> {
                property.default?.toString()
            }
            else -> {
                null
            }
        }
    }

    private fun loadPropertyType(property: Property?): String {
        if (property == null) return ""
        return when (property) {
            is RefProperty -> {
                Link(property.originalRef, '#' + property.originalRef).toString()
            }
            // swagger无法获取到map的key类型
            is MapProperty -> {
                "Map<String, " + loadPropertyType(property.additionalProperties) + ">"
            }
            is ObjectProperty -> {
                "Any"
            }
            is ArrayProperty -> {
                "List<" + loadPropertyType(property.items) + ">"
            }
            is StringProperty -> {
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

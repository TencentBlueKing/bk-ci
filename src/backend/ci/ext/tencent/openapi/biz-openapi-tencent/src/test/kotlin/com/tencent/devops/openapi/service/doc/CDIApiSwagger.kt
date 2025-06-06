package com.tencent.devops.openapi.service.doc

import com.tencent.devops.common.web.JerseyConfig
import io.swagger.v3.core.util.Json
import io.swagger.v3.core.util.ReflectionUtils
import io.swagger.v3.jaxrs2.Reader
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.integration.SwaggerConfiguration
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.parameters.Parameter
import java.io.File
import java.lang.reflect.Method
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [JerseyConfig::class])
class CDIApiSwagger {

    private val parametersMap = mapOf(
        "need_bk_ticket" to Parameter().apply {
            `in`("header")
            name("bk_ticket")
            description("蓝鲸统一登陆鉴权")
            required(true)
            schema(StringSchema())
        }
    )

    @Suppress("NestedBlockDepth")
    private fun loadSwagger(): OpenAPI {
        val bean = SwaggerConfiguration()
            .resourcePackages(setOf("com.tencent.devops"))
            .readAllResources(true)
        val res = JaxrsOpenApiContextBuilder<JaxrsOpenApiContextBuilder<*>>()
            .openApiConfiguration(bean)
            .buildContext(true)
        res.setOpenApiReader(
            object : Reader() {
                /*魔改isOperationHidden，以实现白名单输出接口的功能。swagger本身只提供了黑名单，没提供白名单。*/
                override fun isOperationHidden(method: Method): Boolean {
                    val apiOperation = ReflectionUtils.getAnnotation(
                        method,
                        Operation::class.java
                    )
                    return !(apiOperation != null && apiOperation.tags.contains("REMOTE_DEV_SDK"))
                }
            }.apply {
                setConfiguration(res.openApiConfiguration)
            }
        )
        val openApi = res.read()

        val modifiedPaths = Paths()
        // 遍历原始路径并添加前缀
        openApi.paths.forEach { (path, pathItem) ->
            val operations = pathItem.readOperations()
            val first = operations.firstOrNull() ?: return@forEach
            val prefix = first.tags.find {
                it.startsWith("prefix:")
            }?.substringAfter("prefix:") ?: ""
            modifiedPaths.addPathItem("$prefix$path", pathItem)

            operations.forEach { operation ->
                val tags = operation.tags
                parametersMap.keys.forEach { key ->
                    if (tags.contains(key)) {
                        operation.addParametersItem(parametersMap[key])
                    }
                }
                /*重置tags*/
                operation.tags(emptyList())
            }
        }
        openApi.paths = modifiedPaths
        openApi.tags(emptyList())
        openApi.info = Info().apply {
            title = "云桌面应用CDI接口"
            description = "这是提供给云研发应用所使用的CDI SDK工具"
            version = "1.0.0"
        }
        return openApi
    }

    @Test
    fun `test loadSwagger and export to JSON`() {
        val openAPI = loadSwagger()

        val json = Json.pretty(openAPI)
        val outputFile = File("cdi_swagger.json")
        outputFile.writeText(json)

        assert(outputFile.exists())
        assert(outputFile.length() > 0)
    }
}

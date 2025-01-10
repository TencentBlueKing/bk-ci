package com.tencent.devops.openapi.service.doc

import com.tencent.devops.common.web.JerseyConfig
import io.swagger.v3.core.util.Json
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder
import io.swagger.v3.oas.integration.SwaggerConfiguration
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.info.Info
import java.io.File
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [JerseyConfig::class])
class CDIApiSwagger {

    private val packageName: String = "com.tencent.devops.remotedev.api.cdi"

    private fun loadSwagger(): OpenAPI {
        val bean = SwaggerConfiguration()
            .resourcePackages(setOf(packageName))
            .readAllResources(true)
        val res = JaxrsOpenApiContextBuilder<JaxrsOpenApiContextBuilder<*>>()
            .openApiConfiguration(bean)
            .buildContext(true)
        val openApi = res.read()

        val modifiedPaths = Paths()
        // 遍历原始路径并添加前缀
        openApi.paths.forEach { (path, pathItem) ->
            val operation = pathItem.readOperations().firstOrNull() ?: return@forEach
            val prefix = when {
                operation.tags.contains("CDI_REMOTE_DEV") -> "/remotedev/api"
                else -> ""
            }
            modifiedPaths.addPathItem("$prefix$path", pathItem)
        }
        openApi.paths = modifiedPaths
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

        val outputFile = File("swagger.json")
        outputFile.writeText(json)

        assert(outputFile.exists())
        assert(outputFile.length() > 0)
    }
}

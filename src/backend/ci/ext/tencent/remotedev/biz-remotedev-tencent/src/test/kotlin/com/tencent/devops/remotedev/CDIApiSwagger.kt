package com.tencent.devops.remotedev

import com.tencent.devops.common.web.JerseyConfig
import io.swagger.v3.core.util.Json
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder
import io.swagger.v3.oas.integration.SwaggerConfiguration
import io.swagger.v3.oas.models.OpenAPI
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
        return res.read()
    }

    @Test
    fun test() {
        val load = loadSwagger()
        println(load)
    }

    @Test
    fun `test loadSwagger and export to JSON`() {
        // 调用 loadSwagger 函数
        val openAPI = loadSwagger()

        // 将 OpenAPI 对象转换为 JSON 字符串
        val json = Json.pretty(openAPI)

        // 导出 JSON 到文件
        val outputFile = File("swagger.json")
        outputFile.writeText(json)

        // 这里可以添加更多的断言来验证 JSON 内容
        // 例如，检查文件是否存在，文件大小等
        assert(outputFile.exists())
        assert(outputFile.length() > 0)
    }
}

package com.tencent.devops.openapi.service.doc

import com.tencent.devops.common.api.util.FileUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.web.JerseyConfig
import com.tencent.devops.openapi.BkApigwApi
import com.tencent.devops.openapi.service.doc.apigw.APIGWDefinitionV20
import com.tencent.devops.openapi.service.doc.apigw.APIGWDefinitionV20.AuthConfig
import com.tencent.devops.openapi.service.doc.apigw.APIGWResourcesV20
import io.swagger.v3.core.util.ReflectionUtils
import io.swagger.v3.jaxrs2.Reader
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.integration.SwaggerConfiguration
import io.swagger.v3.oas.models.OpenAPI
import java.lang.reflect.Method
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [JerseyConfig::class, DocumentService::class])
class APIGWSwagger @Autowired constructor(
    private val document: DocumentService
) {

    private val prefix = listOf("/{apigwType}/v3", "/{apigwType}/v4")
    private val allVersion = listOf("v3", "v4")
    private val bkApigwApiCache = mutableMapOf<String, BkApigwApi>()

    @Suppress("NestedBlockDepth")
    private fun loadSwagger(version: List<String> = allVersion): OpenAPI {
        val bean = SwaggerConfiguration()
            .resourcePackages(setOf("com.tencent.devops"))
            .readAllResources(true)

        val res = JaxrsOpenApiContextBuilder<JaxrsOpenApiContextBuilder<*>>()
            .openApiConfiguration(bean)
            .buildContext(true)
        res.setOpenApiReader(
            object : Reader() {

                private fun cache(method: Method, classApiOperation: BkApigwApi) {
                    val operation = ReflectionUtils.getAnnotation(
                        method,
                        Operation::class.java
                    )
                    operation?.tags?.forEach {
                        bkApigwApiCache[it] = classApiOperation
                    }
                }

                /*魔改isOperationHidden，以实现白名单输出接口的功能。swagger本身只提供了黑名单，没提供白名单。*/
                override fun isOperationHidden(method: Method): Boolean {
                    val methodApiOperation = ReflectionUtils.getAnnotation(
                        method,
                        BkApigwApi::class.java
                    )
                    if (methodApiOperation != null && methodApiOperation.version in version) {
                        cache(method, methodApiOperation)
                        return false
                    }

                    val classApiOperation = ReflectionUtils.getAnnotation(
                        method.declaringClass,
                        BkApigwApi::class.java
                    )
                    return !(classApiOperation != null && classApiOperation.version in version)
                }
            }.apply {
                setConfiguration(res.openApiConfiguration)
            }
        )
        return res.read()
    }

    private fun extraPath(path: String) = prefix.indexOfFirst { path.startsWith(it) }.let {
        path.substring(prefix[it].length) to allVersion[it]
    }

    private fun userApigwPath(version: String?, tail: String) =
        "${version?.let { "/$it" } ?: ""}/apigw-user$tail"

    private fun appApigwPath(version: String?, tail: String) =
        "${version?.let { "/$it" } ?: ""}/apigw-app$tail"

    private fun userApiPath(version: String?, tail: String) =
        "/openapi/api/apigw-user${version?.let { "/$it" } ?: ""}$tail"

    private fun appApiPath(version: String?, tail: String) =
        "/openapi/api/apigw-app${version?.let { "/$it" } ?: ""}$tail"

    private fun userApi(tags: List<String>, version: String?) =
        tags.find { it.startsWith("${version?.let { "${version}_" } ?: ""}user") }

    private fun appApi(tags: List<String>, version: String?) =
        tags.find { it.startsWith("${version?.let { "${version}_" } ?: ""}app") }

    private fun userDefinitionV20(
        version: String?,
        id: String,
        description: String,
        method: String,
        tail: String
    ) = APIGWDefinitionV20(
        operationId = id,
        description = description,
        tags = listOf("${version?.uppercase()}用户态"),
        resource = APIGWDefinitionV20.Resource(
            allowApplyPermission = true,
            authConfig = AuthConfig(
                appVerifiedRequired = false,
                resourcePermissionRequired = false,
                userVerifiedRequired = true
            ),
            backend = APIGWDefinitionV20.Backend(
                matchSubpath = false,
                method = method,
                name = "backend-1",
                path = userApiPath(version, tail),
                timeout = 30
            ),
            enableWebsocket = false,
            isPublic = true,
            matchSubpath = false
        )
    )

    private fun appDefinitionV20(
        version: String?,
        id: String,
        description: String,
        method: String,
        tail: String
    ) = APIGWDefinitionV20(
        operationId = id,
        description = description,
        tags = listOf("${version?.uppercase()}应用态"),
        resource = APIGWDefinitionV20.Resource(
            allowApplyPermission = true,
            authConfig = AuthConfig(
                appVerifiedRequired = true,
                resourcePermissionRequired = true,
                userVerifiedRequired = false
            ),
            backend = APIGWDefinitionV20.Backend(
                matchSubpath = false,
                method = method,
                name = "backend-1",
                path = appApiPath(version, tail),
                timeout = 30
            ),
            enableWebsocket = false,
            isPublic = true,
            matchSubpath = false
        )
    )

    @Test
    fun `init apigw resources v20 only v4`() {
        val openAPI = loadSwagger(version = listOf("v4"))
        initResources(openAPI, "build/apigw-only-v4")
        initDoc(openAPI, "build/apigw-only-v4")
    }

    @Test
    fun `init apigw resources v20`() {
        val openAPI = loadSwagger()
        initResources(openAPI, "build/apigw-v3-v4")
        initDoc(openAPI, "build/apigw-v3-v4")
    }

    private fun initDoc(openAPI: OpenAPI, path: String) {
        val config = ConfigurationBuilder()
        config.addUrls(ClasspathHelper.forPackage("com.tencent.devops"))
        config.setExpandSuperTypes(true)
        config.setScanners(Scanners.TypesAnnotated)
        val reflections = Reflections(config)

        document.docInit(
            checkMetaData = false,
            checkMDData = true,
            polymorphism = DocumentService.getAllSubType(reflections),
            swagger = openAPI,
            outputPath = "$path/bk_apigw_docs_devops/zh",
            parametersInfo = DocumentService.getAllApiModelInfo(reflections)
        )
    }

    private fun initResources(openAPI: OpenAPI, path: String) {
        val resource = APIGWResourcesV20()
        openAPI.paths.forEach { (path, pathItem) ->
            val (tail, version) = extraPath(path)
            pathItem.readOperationsMap().forEach { (httpMethod, operation) ->
                val method = httpMethod.name.lowercase()
                val userApi = userApi(operation.tags, version)
                if (userApi != null) {
                    val inPath = resource.paths.getOrPut(
                        userApigwPath(
                            version = version,
                            tail = bkApigwApiCache[userApi]?.apigwPathTail?.ifEmpty { null } ?: tail)
                    ) {
                        mutableMapOf()
                    }
                    inPath[method] = userDefinitionV20(
                        version = version,
                        id = userApi,
                        description = operation.summary ?: "",
                        method = method,
                        tail = tail
                    )
                }
                val appApi = appApi(operation.tags, version)
                if (appApi != null) {
                    val inPath = resource.paths.getOrPut(
                        appApigwPath(
                            version = version,
                            tail = bkApigwApiCache[appApi]?.apigwPathTail?.ifEmpty { null } ?: tail)
                    ) {
                        mutableMapOf()
                    }
                    inPath[method] = appDefinitionV20(
                        version = version,
                        id = appApi,
                        description = operation.summary ?: "",
                        method = method,
                        tail = tail
                    )
                }
            }
        }
        FileUtil.outFile(path, "bk_apigw_resources_devops.yaml", YamlUtil.toYaml(resource))
    }
}

package com.tencent.devops.openapi.service.doc.apigw

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.TreeMap

data class APIGWResourcesV20(
    @JsonProperty("info")
    val info: Info = Info(),
    @JsonProperty("openapi")
    val openapi: String = "3.0.1",
    @JsonProperty("servers")
    val servers: List<Server> = listOf(Server()),
    @JsonProperty("paths")
    val paths: TreeMap<String, MutableMap<String, APIGWDefinitionV20>> = TreeMap()
) {
    data class Info(
        @JsonProperty("description")
        val description: String = "",
        @JsonProperty("title")
        val title: String = "API Gateway Resources",
        @JsonProperty("version")
        val version: String = "2.0"
    )

    data class Server(
        @JsonProperty("url")
        val url: String = "/"
    )
}

data class APIGWDefinitionV20(
    @JsonProperty("operationId")
    val operationId: String,
    @JsonProperty("description")
    val description: String,
    @JsonProperty("tags")
    val tags: List<String>,
    @JsonProperty("x-bk-apigateway-resource")
    val resource: Resource
) {

    data class Resource(
        @JsonProperty("allowApplyPermission")
        val allowApplyPermission: Boolean,
        @JsonProperty("authConfig")
        val authConfig: AuthConfig,
        @JsonProperty("backend")
        val backend: Backend,
        @JsonProperty("enableWebsocket")
        val enableWebsocket: Boolean,
        @JsonProperty("isPublic")
        val isPublic: Boolean,
        @JsonProperty("matchSubpath")
        val matchSubpath: Boolean
    )

    data class AuthConfig(
        @JsonProperty("appVerifiedRequired")
        val appVerifiedRequired: Boolean,
        @JsonProperty("resourcePermissionRequired")
        val resourcePermissionRequired: Boolean,
        @JsonProperty("userVerifiedRequired")
        val userVerifiedRequired: Boolean
    )

    data class Backend(
        @JsonProperty("matchSubpath")
        val matchSubpath: Boolean,
        @JsonProperty("method")
        val method: String,
        @JsonProperty("name")
        val name: String,
        @JsonProperty("path")
        val path: String,
        @JsonProperty("timeout")
        val timeout: Int
    )
}

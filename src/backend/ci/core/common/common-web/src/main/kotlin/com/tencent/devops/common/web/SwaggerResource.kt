package com.tencent.devops.common.web

import io.swagger.jaxrs.listing.ApiListingResource
import io.swagger.models.HttpMethod
import javax.servlet.ServletConfig
import javax.ws.rs.core.Application
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

class SwaggerResource : ApiListingResource() {

    private val allowOrigin = listOf("*")
    private val allowMethods = HttpMethod.values().toList()
    private val allowHeaders = listOf(
        "Content-Type",
        "api_key",
        "Authorization"
    )

    override fun getListing(
        app: Application?,
        sc: ServletConfig?,
        headers: HttpHeaders?,
        uriInfo: UriInfo?,
        type: String?
    ): Response {
        val response = super.getListing(app, sc, headers, uriInfo, type)
        val responseHeaders = response.headers
        responseHeaders["Access-Control-Allow-Origin"] = allowOrigin
        responseHeaders["Access-Control-Allow-Methods"] = allowMethods
        responseHeaders["Access-Control-Allow-Headers"] = allowHeaders
        return response
    }
}

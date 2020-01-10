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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.tencent.devops.openapi.filter

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.RequestFilter
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.ERROR_OPENAPI_JWT_PARSE_FAIL
import com.tencent.devops.openapi.utils.ApiGatewayPubFile
import io.jsonwebtoken.Jwts
import net.sf.json.JSONObject
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.provider.JCERSAPublicKey
import org.bouncycastle.openssl.PEMReader
import org.bouncycastle.openssl.PasswordFinder
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.security.Security
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.container.PreMatching
import javax.ws.rs.core.Response
import javax.ws.rs.ext.Provider

@Provider
@PreMatching
@RequestFilter
class ApiFilter : ContainerRequestFilter {

    private val excludeVeritfyPath = listOf("swagger.json", "external/service/versionInfo")

    fun verifyJWT(requestContext: ContainerRequestContext): Boolean {
        val bkApiJwt = requestContext.getHeaderString("X-Bkapi-JWT")
        val apigwtType = requestContext.getHeaderString("X-DEVOPS-APIGW-TYPE")
        if (bkApiJwt.isNullOrBlank()) {
            logger.error("Request bk api jwt is empty for ${requestContext.request}")
            requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST)
                .entity("Request bkapi jwt is empty.")
                .build())
            return false
        }

        val uriPath = requestContext.uriInfo.requestUri.path
        val apiType = if (uriPath.startsWith("/api/apigw-app")) "apigw-app" else "apigw-user"

        logger.info("Get the bkApiJwt header, X-Bkapi-JWT：$bkApiJwt")
        val jwt = parseJwt(bkApiJwt, apigwtType)
        logger.info("Get the parse bkApiJwt($jwt)")

        // 验证应用身份信息
        if (jwt.has("app")) {
            val app = jwt.getJSONObject("app")
            // 应用身份登录
            if (app.has("app_code")) {
                val appCode = app.getString("app_code")
                val verified = app.get("verified") as Boolean
                if (appCode.isNullOrEmpty() || !verified) {
                    return false
                }
            }
        }
        // 在验证应用身份信息
        if (jwt.has("user")) {
            // 先做app的验证再做
            val user = jwt.getJSONObject("user")
            // 用户身份登录
            if (user.has("username")) {
                val username = user.getString("username")
                val verified = user.get("verified") as Boolean
                // 名字为空或者没有通过认证的时候，直接失败
                if (username.isNotBlank() && verified) {
                    // 将头部置空
                    requestContext.headers[AUTH_HEADER_DEVOPS_USER_ID]?.set(0, null)
                    if (requestContext.headers[AUTH_HEADER_DEVOPS_USER_ID] != null) {
                        requestContext.headers[AUTH_HEADER_DEVOPS_USER_ID]?.set(0, username)
                    } else {
                        requestContext.headers.add(AUTH_HEADER_DEVOPS_USER_ID, username)
                    }
                } else if (apiType == "apigw-user") {
                    requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST)
                        .entity("Request don't has user's access_token.")
                        .build())
                    return false
                }
            }
        }
        return true
    }

    override fun filter(requestContext: ContainerRequestContext) {
        val path = requestContext.uriInfo?.path
        if (!path.isNullOrBlank()) {
            if (excludeVeritfyPath.contains(path)) {
                logger.info("The path($path) already exclude")
                return
            }
        }
        val valid = verifyJWT(requestContext)
        // 验证通过
        if (!valid) {
            requestContext.abortWith(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity("Devops OpenAPI Auth fail：user or app auth fail.")
                    .build()
            )
            return
        }
    }

    private fun parseJwt(bkApiJwt: String, apigwtType: String?): JSONObject {
        var reader: PEMReader? = null
        try {
            val key = if (!apigwtType.isNullOrEmpty() && apigwtType == "outer") {
                SpringContextUtil.getBean(ApiGatewayPubFile::class.java).getPubOuter().toByteArray()
            } else {
                SpringContextUtil.getBean(ApiGatewayPubFile::class.java).getPubInner().toByteArray()
            }
            Security.addProvider(BouncyCastleProvider())
            val bais = ByteArrayInputStream(key)
            reader = PEMReader(InputStreamReader(bais), PasswordFinder { "".toCharArray() })
            val keyPair = reader.readObject() as JCERSAPublicKey
            val jwtParser = Jwts.parser().setSigningKey(keyPair)
            val parse = jwtParser.parse(bkApiJwt)
            logger.info("Get the parse body(${parse.body}) and header(${parse.header})")
            return JSONObject.fromObject(parse.body)
        } catch (e: Exception) {
            logger.error("Parse jwt failed.", e)
            throw ErrorCodeException(
                errorCode = ERROR_OPENAPI_JWT_PARSE_FAIL,
                defaultMessage = "Parse jwt failed",
                params = arrayOf(bkApiJwt)
            )
        } finally {
            reader?.close()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApiFilter::class.java)
    }
}
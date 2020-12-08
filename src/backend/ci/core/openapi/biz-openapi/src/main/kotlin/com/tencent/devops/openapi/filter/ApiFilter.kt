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

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_SECRET
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.RequestFilter
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.ERROR_OPENAPI_JWT_PARSE_FAIL
import com.tencent.devops.openapi.utils.ApiGatewayPubFile
import com.tencent.devops.openapi.utils.ApiGatewayUtil
import io.jsonwebtoken.Jwts
import net.sf.json.JSONObject
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.provider.JCERSAPublicKey
import org.bouncycastle.openssl.PEMReader
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
class ApiFilter(
    private val apiGatewayUtil: ApiGatewayUtil
) : ContainerRequestFilter {

    companion object {
        private val logger = LoggerFactory.getLogger(ApiFilter::class.java)
        private const val appCodeHeader = "app_code"
        private const val appSecHeader = "app_secret"
        private const val jwtHeader = "X-Bkapi-JWT"
        private const val apigwSourceHeader = "X-DEVOPS-APIGW-TYPE"
    }

    enum class ApiType(val startContextPath: String) {
        DEFAULT("/api/apigw/"),
        USER("/api/apigw-user/"),
        APP("/api/apigw-app/");

        companion object {
            fun parseType(path: String): ApiType? {
                values().forEach { type ->
                    if (path.contains(other = type.startContextPath, ignoreCase = true)) {
                        return type
                    }
                }
                return null
            }
        }
    }

    override fun filter(requestContext: ContainerRequestContext) {

        // path为为空的时候，直接退出
        val path = requestContext.uriInfo.requestUri.path
        // 判断是否需要处理apigw
        val apiType = ApiType.parseType(path) ?: return

        logger.info("FILTER| url=$path")
        if (!apiGatewayUtil.isAuth()) {
            // 将query中的app_code和app_secret设置成头部
            setupHeader(requestContext)
        } else {
            // 验证通过
            if (!verifyJWT(requestContext, apiType)) {
                requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST).entity("Devops OpenAPI Auth fail：user or app auth fail.").build())
                return
            }
        }
    }

    private fun setupHeader(requestContext: ContainerRequestContext) {
        requestContext.uriInfo?.pathParameters?.forEach { pathParam ->
            if (pathParam.key == appCodeHeader && pathParam.value.isNotEmpty()) {
                requestContext.headers[AUTH_HEADER_DEVOPS_APP_CODE]?.set(0, null)
                if (requestContext.headers[AUTH_HEADER_DEVOPS_APP_CODE] != null) {
                    requestContext.headers[AUTH_HEADER_DEVOPS_APP_CODE]?.set(0, pathParam.value[0])
                } else {
                    requestContext.headers.add(AUTH_HEADER_DEVOPS_APP_CODE, pathParam.value[0])
                }
            } else if (pathParam.key == appSecHeader && pathParam.value.isNotEmpty()) {
                requestContext.headers[AUTH_HEADER_DEVOPS_APP_SECRET]?.set(0, null)
                if (requestContext.headers[AUTH_HEADER_DEVOPS_APP_SECRET] != null) {
                    requestContext.headers[AUTH_HEADER_DEVOPS_APP_SECRET]?.set(0, pathParam.value[0])
                } else {
                    requestContext.headers.add(AUTH_HEADER_DEVOPS_APP_CODE, pathParam.value[0])
                }
            }
        }
    }

    fun verifyJWT(requestContext: ContainerRequestContext, apiType: ApiType): Boolean {
        val bkApiJwt = requestContext.getHeaderString(jwtHeader)
        if (bkApiJwt.isNullOrBlank()) {
            logger.error("Request bk api jwt is empty for ${requestContext.request}")
            requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST).entity("Request bkapi jwt is empty.").build())
            return false
        }

        val apigwSource = requestContext.getHeaderString(apigwSourceHeader)
        logger.debug("Get the bkApiJwt header, X-Bkapi-JWT：{}", bkApiJwt)
        val jwt = parseJwt(bkApiJwt, apigwSource)
        logger.debug("Get the parse bkApiJwt({})", jwt)

        // 验证应用身份信息
        if (jwt.has("app")) {
            val app = jwt.getJSONObject("app")
            // 应用身份登录
            if (app.has(appCodeHeader)) {
                val appCode = app.getString(appCodeHeader)
                val verified = app.get("verified") as Boolean
                if (apiType == ApiType.APP && (appCode.isNullOrEmpty() || !verified)) {
                    return false
                } else {
                    if (!appCode.isNullOrBlank()) {
                        // 将appCode头部置空
                        requestContext.headers[AUTH_HEADER_DEVOPS_APP_CODE]?.set(0, null)
                        if (requestContext.headers[AUTH_HEADER_DEVOPS_APP_CODE] != null) {
                            requestContext.headers[AUTH_HEADER_DEVOPS_APP_CODE]?.set(0, appCode)
                        } else {
                            requestContext.headers.add(AUTH_HEADER_DEVOPS_APP_CODE, appCode)
                        }
                    }
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
                    // 将user头部置空
                    requestContext.headers[AUTH_HEADER_DEVOPS_USER_ID]?.set(0, null)
                    if (requestContext.headers[AUTH_HEADER_DEVOPS_USER_ID] != null) {
                        requestContext.headers[AUTH_HEADER_DEVOPS_USER_ID]?.set(0, username)
                    } else {
                        requestContext.headers.add(AUTH_HEADER_DEVOPS_USER_ID, username)
                    }
                } else if (apiType == ApiType.USER) {
                    requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST).entity("Request don't has user's access_token.").build())
                    return false
                }
            }
        }
        return true
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
            reader = PEMReader(InputStreamReader(bais)) { "".toCharArray() }
            val keyPair = reader.readObject() as JCERSAPublicKey
            val jwtParser = Jwts.parser().setSigningKey(keyPair)
            val parse = jwtParser.parse(bkApiJwt)
            logger.info("Get the parse body(${parse.body}) and header(${parse.header})")
            return JSONObject.fromObject(parse.body)
        } catch (e: Exception) {
            logger.error("Parse jwt failed.", e)
            throw ErrorCodeException(errorCode = ERROR_OPENAPI_JWT_PARSE_FAIL, defaultMessage = "Parse jwt failed", params = arrayOf(bkApiJwt))
        } finally {
            reader?.close()
        }
    }
}

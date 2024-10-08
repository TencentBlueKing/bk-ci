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
package com.tencent.devops.openapi.filter.manager.impl

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.ERROR_OPENAPI_JWT_PARSE_FAIL
import com.tencent.devops.openapi.filter.manager.ApiFilterFlowState
import com.tencent.devops.openapi.filter.manager.ApiFilterManager
import com.tencent.devops.openapi.filter.manager.FilterContext
import com.tencent.devops.openapi.utils.ApiGatewayPubFile
import com.tencent.devops.openapi.utils.ApiGatewayUtil
import io.jsonwebtoken.Jwts
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.security.Security
import javax.ws.rs.core.Response
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.provider.JCERSAPublicKey
import org.bouncycastle.openssl.PEMReader
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TencentApigwApiFilter(
    private val apiGatewayUtil: ApiGatewayUtil
) : ApiFilterManager {

    companion object {
        private val logger = LoggerFactory.getLogger(TencentApigwApiFilter::class.java)
        private const val appCodeHeader = "app_code"
        private const val jwtHeader = "X-Bkapi-JWT"
        private const val apigwSourceHeader = "X-DEVOPS-APIGW-TYPE"
    }

    /*返回true时执行check逻辑*/
    override fun canExecute(requestContext: FilterContext): Boolean {
        if (!apiGatewayUtil.isAuth()) return false
        requestContext.needCheckPermissions = true
        return !requestContext.requestContext.getHeaderString(jwtHeader).isNullOrBlank()
    }

    @Suppress("UNCHECKED_CAST", "ComplexMethod", "NestedBlockDepth", "ReturnCount")
    override fun verify(requestContext: FilterContext): ApiFilterFlowState {
        val bkApiJwt = requestContext.requestContext.getHeaderString(jwtHeader)
        if (bkApiJwt.isNullOrBlank()) {
            logger.error("Request bk api jwt is empty for ${requestContext.requestContext.request}")
            requestContext.requestContext.abortWith(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity("Request bkapi jwt is empty.")
                    .build()
            )
            return ApiFilterFlowState.BREAK
        }

        val apigwSource = requestContext.requestContext.getHeaderString(apigwSourceHeader)
        val jwt = parseJwt(bkApiJwt, apigwSource)
        logger.debug("Get the bkApiJwt header|X-Bkapi-JWT={}|jwt={}", bkApiJwt, jwt)

        // 验证应用身份信息
        if (jwt.contains("app")) {
            val app = jwt["app"] as Map<String, Any>
            // 应用身份登录
            if (app.contains(appCodeHeader)) {
                val appCode = app[appCodeHeader]?.toString()
                val verified = app["verified"].toString().toBoolean()
                if (requestContext.apiType == ApiPathFilter.ApiType.APP && (appCode.isNullOrEmpty() || !verified)) {
                    requestContext.requestContext.abortWith(
                        Response.status(Response.Status.BAD_REQUEST)
                            .entity("Devops OpenAPI Auth fail：app auth fail.")
                            .build()
                    )
                    return ApiFilterFlowState.BREAK
                } else {
                    if (!appCode.isNullOrBlank()) {
                        // 将appCode头部置空
                        requestContext.requestContext.headers[AUTH_HEADER_DEVOPS_APP_CODE]?.set(0, null)
                        if (requestContext.requestContext.headers[AUTH_HEADER_DEVOPS_APP_CODE] != null) {
                            requestContext.requestContext.headers[AUTH_HEADER_DEVOPS_APP_CODE]?.set(0, appCode)
                        } else {
                            requestContext.requestContext.headers.add(AUTH_HEADER_DEVOPS_APP_CODE, appCode)
                        }
                    }
                }
            }
        }
        // 在验证应用身份信息
        if (jwt.contains("user")) {
            // 先做app的验证再做
            val user = jwt["user"] as Map<String, Any>
            // 用户身份登录
            if (user.contains("username")) {
                val username = user["username"]?.toString() ?: ""
                val verified = user["verified"].toString().toBoolean()
                // 名字为空或者没有通过认证的时候，直接失败
                if (username.isNotBlank() && verified) {
                    // 将user头部置空
                    requestContext.requestContext.headers[AUTH_HEADER_DEVOPS_USER_ID]?.set(0, null)
                    if (requestContext.requestContext.headers[AUTH_HEADER_DEVOPS_USER_ID] != null) {
                        requestContext.requestContext.headers[AUTH_HEADER_DEVOPS_USER_ID]?.set(0, username)
                    } else {
                        requestContext.requestContext.headers.add(AUTH_HEADER_DEVOPS_USER_ID, username)
                    }
                } else if (requestContext.apiType == ApiPathFilter.ApiType.USER) {
                    requestContext.requestContext.abortWith(
                        Response.status(Response.Status.BAD_REQUEST)
                            .entity("Request don't has user's access_token.")
                            .build()
                    )
                    return ApiFilterFlowState.BREAK
                }
            }
        }
        return ApiFilterFlowState.AUTHORIZED
    }

    private fun parseJwt(bkApiJwt: String, apigwtType: String?): Map<String, Any> {
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
            return JsonUtil.toMap(parse.body)
        } catch (ignored: Exception) {
            logger.error("BKSystemErrorMonitor| Parse jwt failed.", ignored)
            throw ErrorCodeException(
                errorCode = ERROR_OPENAPI_JWT_PARSE_FAIL,
                defaultMessage = "Parse jwt failed",
                params = arrayOf(bkApiJwt)
            )
        } finally {
            reader?.close()
        }
    }
}

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

    private val excludeVeritfyPath =
        listOf("/api/apigw/", "/api/apigw-user/", "/api/apigw-app/")

    override fun filter(requestContext: ContainerRequestContext) {

        // path为为空的时候，直接退出
        val path = requestContext.uriInfo.requestUri.path
        logger.info("uriInfo uriInfo[$path]")
        // 判断是否需要处理apigw
        var pass = true
        excludeVeritfyPath.forEach {
            if (path.startsWith(it)) {
                pass = false
            }
        }
        if (pass) {
            return
        }

        // 将query中的app_code和app_secret设置成头部
        val pathparam = requestContext.getUriInfo().pathParameters
        pathparam.forEach {
            if(it.key == "app_code" && it.value.isNotEmpty()) {
                requestContext.headers[AUTH_HEADER_DEVOPS_APP_CODE]?.set(0, null)
                if (requestContext.headers[AUTH_HEADER_DEVOPS_APP_CODE] != null) {
                    requestContext.headers[AUTH_HEADER_DEVOPS_APP_CODE]?.set(0, it.value[0])
                } else {
                    requestContext.headers.add(AUTH_HEADER_DEVOPS_APP_CODE, it.value[0])
                }
            }
            if(it.key == "app_secret" && it.value.isNotEmpty()) {
                requestContext.headers[AUTH_HEADER_DEVOPS_APP_SECRET]?.set(0, null)
                if (requestContext.headers[AUTH_HEADER_DEVOPS_APP_SECRET] != null) {
                    requestContext.headers[AUTH_HEADER_DEVOPS_APP_SECRET]?.set(0, it.value[0])
                } else {
                    requestContext.headers.add(AUTH_HEADER_DEVOPS_APP_CODE, it.value[0])
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApiFilter::class.java)
    }
}
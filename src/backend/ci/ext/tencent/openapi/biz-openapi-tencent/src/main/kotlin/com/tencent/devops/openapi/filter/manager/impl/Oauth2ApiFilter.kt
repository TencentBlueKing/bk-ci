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

import com.tencent.devops.auth.api.oauth2.Oauth2ServiceEndpointResource
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_OAUTH2_AUTHORIZATION
import com.tencent.devops.common.api.auth.AUTH_HEADER_OAUTH2_CLIENT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_OAUTH2_CLIENT_SECRET
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.openapi.filter.manager.ApiFilterFlowState
import com.tencent.devops.openapi.filter.manager.ApiFilterManager
import com.tencent.devops.openapi.filter.manager.FilterContext
import com.tencent.devops.openapi.utils.ApiGatewayUtil
import javax.ws.rs.core.Response
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class Oauth2ApiFilter(
    private val apiGatewayUtil: ApiGatewayUtil,
    private val client: Client
) : ApiFilterManager {

    companion object {
        private val logger = LoggerFactory.getLogger(Oauth2ApiFilter::class.java)
        private const val REMOTE_EXCEPTION_CODE = 500
    }

    /*返回true时执行check逻辑*/
    override fun canExecute(requestContext: FilterContext): Boolean {
        if (!apiGatewayUtil.isAuth()) return false
        return requestContext.apiType != ApiPathFilter.ApiType.USER &&
            !requestContext.requestContext.headers.getFirst(AUTH_HEADER_OAUTH2_AUTHORIZATION).isNullOrBlank()
    }

    override fun verify(requestContext: FilterContext): ApiFilterFlowState {
        val oauth2AccessToken = requestContext.requestContext.headers.getFirst(AUTH_HEADER_OAUTH2_AUTHORIZATION)
        val clientId = requestContext.requestContext.headers.getFirst(AUTH_HEADER_OAUTH2_CLIENT_ID)
        val clientSecret = requestContext.requestContext.headers.getFirst(AUTH_HEADER_OAUTH2_CLIENT_SECRET)
        if (clientId == null || clientSecret == null) {
            requestContext.requestContext.abortWith(
                Response.status(Response.Status.OK)
                    .entity(
                        Result(
                            status = AuthMessageCode.ERROR_CLIENT_NOT_EXIST.toInt(),
                            message = "The client id or client secret cannot be empty!",
                            data = null
                        )
                    ).build()
            )
            return ApiFilterFlowState.BREAK
        }
        try {
            val username = client.get(Oauth2ServiceEndpointResource::class).verifyAccessToken(
                clientId = clientId,
                clientSecret = clientSecret,
                accessToken = oauth2AccessToken
            ).data
            requestContext.requestContext.headers.putSingle(AUTH_HEADER_DEVOPS_USER_ID, username)
        } catch (ex: ErrorCodeException) {
            requestContext.requestContext.abortWith(
                Response.status(Response.Status.OK)
                    .entity(
                        Result(
                            status = ex.errorCode.toInt(),
                            message = ex.defaultMessage,
                            data = null
                        )
                    ).build()
            )
            return ApiFilterFlowState.BREAK
        } catch (ignore: RemoteServiceException) {
            requestContext.requestContext.abortWith(
                Response.status(Response.Status.OK)
                    .entity(
                        Result(
                            status = ignore.errorCode ?: REMOTE_EXCEPTION_CODE,
                            message = ignore.errorMessage,
                            data = null
                        )
                    ).build()
            )
            return ApiFilterFlowState.BREAK
        } catch (ignore: Exception) {
            requestContext.requestContext.abortWith(
                Response.status(Response.Status.OK)
                    .entity(
                        Result(
                            status = REMOTE_EXCEPTION_CODE,
                            message = ignore.message,
                            data = null
                        )
                    ).build()
            )
            return ApiFilterFlowState.BREAK
        }
        return ApiFilterFlowState.AUTHORIZED
    }
}

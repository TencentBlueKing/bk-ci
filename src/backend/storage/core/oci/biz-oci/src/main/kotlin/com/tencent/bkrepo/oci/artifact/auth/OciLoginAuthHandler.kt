/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.oci.artifact.auth

import com.tencent.bkrepo.common.api.constant.HttpHeaders
import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.security.exception.AuthenticationException
import com.tencent.bkrepo.common.security.http.basic.BasicAuthHandler
import com.tencent.bkrepo.common.security.manager.AuthenticationManager
import com.tencent.bkrepo.oci.constant.DOCKER_API_VERSION
import com.tencent.bkrepo.oci.constant.DOCKER_HEADER_API_VERSION
import com.tencent.bkrepo.oci.constant.OCI_FILTER_ENDPOINT
import com.tencent.bkrepo.oci.constant.UNAUTHORIZED_CODE
import com.tencent.bkrepo.oci.constant.UNAUTHORIZED_DESCRIPTION
import com.tencent.bkrepo.oci.constant.UNAUTHORIZED_MESSAGE
import com.tencent.bkrepo.oci.pojo.response.OciErrorResponse
import com.tencent.bkrepo.oci.pojo.response.OciResponse
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * oci registry login handler
 */
class OciLoginAuthHandler(
    authenticationManager: AuthenticationManager
) : BasicAuthHandler(authenticationManager) {

    /**
     * login to a registry (with manual password entry)
     */
    override fun getLoginEndpoint() = OCI_FILTER_ENDPOINT
    override fun getLoginMethod() = HttpMethod.GET.name

    override fun onAuthenticateSuccess(request: HttpServletRequest, response: HttpServletResponse, userId: String) {
        response.status = HttpStatus.OK.value
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write("{}")
    }

    override fun onAuthenticateFailed(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authenticationException: AuthenticationException
    ) {
        response.status = HttpStatus.UNAUTHORIZED.value
        response.contentType = MediaTypes.APPLICATION_JSON
        response.addHeader(DOCKER_HEADER_API_VERSION, DOCKER_API_VERSION)
        response.addHeader(
            HttpHeaders.WWW_AUTHENTICATE,
            AUTH_CHALLENGE_SERVICE_SCOPE.format(request.remoteHost, REGISTRY_SERVICE, SCOPE_STR)
        )
        val ociAuthResponse = OciResponse.errorResponse(
            OciErrorResponse(UNAUTHORIZED_MESSAGE, UNAUTHORIZED_CODE, UNAUTHORIZED_DESCRIPTION)
        )
        response.writer.write(ociAuthResponse.toJsonString())
        response.writer.flush()
    }

    companion object {
        const val AUTH_CHALLENGE_SERVICE_SCOPE = "Basic realm=\"%s\",service=\"%s\",scope=\"%s\""
        const val REGISTRY_SERVICE = "bkrepo"
        const val SCOPE_STR = "repository:*/*/tb:push,pull"
    }
}

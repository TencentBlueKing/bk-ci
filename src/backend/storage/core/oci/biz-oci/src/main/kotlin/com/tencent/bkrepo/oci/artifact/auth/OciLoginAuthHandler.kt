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

import com.tencent.bkrepo.common.api.constant.ANONYMOUS_USER
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.security.exception.AuthenticationException
import com.tencent.bkrepo.common.security.http.basic.BasicAuthHandler
import com.tencent.bkrepo.common.security.http.jwt.JwtAuthProperties
import com.tencent.bkrepo.common.security.manager.AuthenticationManager
import com.tencent.bkrepo.common.security.util.JwtUtils
import com.tencent.bkrepo.oci.constant.DOCKER_API_VERSION
import com.tencent.bkrepo.oci.constant.DOCKER_HEADER_API_VERSION
import com.tencent.bkrepo.oci.constant.OCI_API_SUFFIX
import com.tencent.bkrepo.oci.constant.OCI_FILTER_ENDPOINT
import com.tencent.bkrepo.oci.util.TimeUtil
import io.undertow.servlet.spec.HttpServletRequestImpl
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType

/**
 * oci registry login handler
 */
class OciLoginAuthHandler(
    authenticationManager: AuthenticationManager,
    private val jwtProperties: JwtAuthProperties
) : BasicAuthHandler(authenticationManager) {

    private val signingKey = JwtUtils.createSigningKey(jwtProperties.secretKey)

    /**
     * login to a registry (with manual password entry)
     */
    override fun getLoginEndpoint() = OCI_FILTER_ENDPOINT + OCI_API_SUFFIX

    override fun onAuthenticateSuccess(request: HttpServletRequest, response: HttpServletResponse, userId: String) {
        val token = JwtUtils.generateToken(signingKey, jwtProperties.expiration, userId)
        val issuedAt = TimeUtil.getGMTTime()
        val tokenUrl = AUTH_CHALLENGE_TOKEN.format(token, token, issuedAt)
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.setHeader(DOCKER_HEADER_API_VERSION, DOCKER_API_VERSION)
        response.writer.print(tokenUrl)
        response.writer.flush()
        super.onAuthenticateSuccess(request, response, userId)
    }

    override fun onAuthenticateFailed(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authenticationException: AuthenticationException
    ) {
        parseRepositoryId(request, response, authenticationException)
    }

    private fun parseRepositoryId(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authenticationException: AuthenticationException
    ) {
        try {
            val params = (request as HttpServletRequestImpl).queryParameters
            // 从scope中解析对应的projectId与repoName, scope=repository:XXX/XXX/php:pull
            val scope = params?.get("scope")?.first ?: throw authenticationException
            val scopeValues = scope.split(":")
            val values = scopeValues[1].split("/")
            val repositoryId = ArtifactContextHolder.RepositoryId(values[0], values[1])
            val repo = ArtifactContextHolder.getRepoDetail(repositoryId)
            // 针对仓库类型的为public的，允许下载。
            if (repo.public) {
                logger.info("empty user pull,push ,change to  [$this]")
                return onAuthenticateSuccess(request, response, ANONYMOUS_USER)
            }
        } catch (e: Exception) {
            throw authenticationException
        }
        throw authenticationException
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OciLoginAuthHandler::class.java)
        const val AUTH_CHALLENGE_SERVICE_SCOPE = "Bearer realm=\"%s\",service=\"%s\",scope=\"%s\""
        const val AUTH_CHALLENGE_TOKEN = "{\"token\": \"%s\", \"access_token\": \"%s\",\"issued_at\": \"%s\"}"
        const val REGISTRY_SERVICE = "bkrepo"
        const val SCOPE_STR = "repository:*/*/tb:push,pull"
        const val OCI_UNAUTHED_BODY =
            "{\"errors\":[{\"code\":\"UNAUTHORIZED\",\"message\":\"access to the " +
                "requested resource is not authorized\",\"detail\"" +
                ":[{\"Type\":\"repository\",\"Name\":\"samalba/my-app\",\"Action\":\"pull\"}," +
                "{\"Type\":\"repository\",\"Name\":\"samalba/my-app\",\"Action\":\"push\"}]}]}"
    }
}

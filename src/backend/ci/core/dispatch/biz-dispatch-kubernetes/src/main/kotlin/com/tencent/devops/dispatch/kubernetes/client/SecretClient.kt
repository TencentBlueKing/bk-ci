/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.dispatch.kubernetes.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesResult
import com.tencent.devops.dispatch.kubernetes.pojo.base.KubernetesRepo
import com.tencent.devops.dispatch.kubernetes.pojo.common.ErrorCodeEnum
import io.fabric8.kubernetes.api.model.Secret
import io.fabric8.kubernetes.api.model.SecretBuilder
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.apache.commons.codec.binary.Base64
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class SecretClient @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val clientCommon: KubernetesClientCommon
) {

    companion object {
        private val logger = LoggerFactory.getLogger(SecretClient::class.java)
    }

    fun createSecret(
        userId: String,
        namespace: String,
        secret: Secret
    ): KubernetesResult<String> {
        val url = "/api/namespace/$namespace/secrets"
        val body = JsonUtil.toJson(secret)
        logger.info("Create secret request url: $url, body: $body")
        val request = clientCommon.microBaseRequest(url).post(
            RequestBody.create(
                "application/json; charset=utf-8".toMediaTypeOrNull(),
                body
            )
        ).build()
        val responseBody = OkhttpUtils.doHttp(request).body!!.string()
        logger.info("Create secret response: ${JsonUtil.toJson(responseBody)}")
        return JsonUtil.getObjectMapper().readValue(responseBody)
    }

    fun getSecretByName(
        userId: String,
        namespace: String,
        secretName: String
    ): KubernetesResult<Secret> {
        val url = "/api/namespace/$namespace/secrets/$secretName"
        val request = clientCommon.microBaseRequest(url).get().build()
        logger.info("Get secret: $secretName request url: $url, userId: $userId")
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            logger.info("Get secret: $secretName response: $responseContent")
            if (!response.isSuccessful) {
                throw BuildFailureException(
                    errorType = ErrorCodeEnum.BCS_SYSTEM_ERROR.errorType,
                    errorCode = ErrorCodeEnum.BCS_SYSTEM_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.BCS_SYSTEM_ERROR.getErrorMessage(),
                    errorMessage = "Fail to get secret,http response code: ${response.code}"
                )
            }
            return objectMapper.readValue(responseContent)
        }
    }

    fun deleteSecretByName(
        userId: String,
        namespace: String,
        secretName: String
    ): KubernetesResult<String> {
        val url = "/api/namespace/$namespace/secrets/$secretName"
        val request = clientCommon.microBaseRequest(url).delete().build()
        logger.info("Delete secret: $secretName request url: $url, userId: $userId")
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            logger.info("Delete secret: $secretName response: $responseContent")
            if (!response.isSuccessful) {
                throw BuildFailureException(
                    errorType = ErrorCodeEnum.BCS_SYSTEM_ERROR.errorType,
                    errorCode = ErrorCodeEnum.BCS_SYSTEM_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.BCS_SYSTEM_ERROR.getErrorMessage(),
                    errorMessage = "Fail to delete secret,http response code: ${response.code}"
                )
            }
            return objectMapper.readValue(responseContent)
        }
    }

    /**
     * 创建k8s拉取镜像secret
     * @param namespaceName 命名空间名称
     * @param secretName 秘钥名称
     * @param kubernetesRepoInfo k8s仓库信息
     */
    fun createImagePullSecret(
        userId: String,
        secretName: String,
        namespaceName: String,
        kubernetesRepoInfo: KubernetesRepo
    ) {
        var secret = getSecretByName(userId, namespaceName, secretName).data
        logger.info("the secret is: $secret")
        if (secret == null) {
            val secretData: HashMap<String, String> = HashMap(1)
            val basicAuth = String(
                Base64.encodeBase64("${kubernetesRepoInfo.username}:${kubernetesRepoInfo.password}".toByteArray())
            )
            var dockerCfg = String.format(
                "{ " +
                    " \"auths\": { " +
                    "  \"%s\": { " +
                    "   \"username\": \"%s\", " +
                    "   \"password\": \"%s\", " +
                    "   \"email\": \"%s\", " +
                    "   \"auth\": \"%s\" " +
                    "  } " +
                    " } " +
                    "}",
                kubernetesRepoInfo.registryUrl,
                kubernetesRepoInfo.username,
                kubernetesRepoInfo.password,
                kubernetesRepoInfo.email,
                basicAuth
            )
            dockerCfg = String(Base64.encodeBase64(dockerCfg.toByteArray(Charsets.UTF_8)), Charsets.UTF_8)
            secretData[".dockerconfigjson"] = dockerCfg
            secret = SecretBuilder()
                .withNewMetadata()
                .withName(secretName)
                .withNamespace(namespaceName)
                .endMetadata()
                .addToData(secretData)
                .withType("kubernetes.io/dockerconfigjson")
                .build()
            createSecret(userId, namespaceName, secret)
            logger.info("create new secret: $secret")
        }
    }
}

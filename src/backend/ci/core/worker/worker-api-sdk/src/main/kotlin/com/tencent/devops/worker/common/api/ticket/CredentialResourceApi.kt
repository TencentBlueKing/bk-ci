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

package com.tencent.devops.worker.common.api.ticket

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.sdk.enums.HttpMethod
import com.tencent.devops.common.util.ApiSignUtil
import com.tencent.devops.ticket.pojo.CredentialInfo
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import com.tencent.devops.worker.common.constants.WorkerMessageCode.GET_CREDENTIAL_FAILED
import com.tencent.devops.worker.common.env.AgentEnv

class CredentialResourceApi : AbstractBuildResourceApi(), CredentialSDKApi {

    override fun get(credentialId: String, publicKey: String, signToken: String): Result<CredentialInfo> {
        val path = "/ms/ticket/api/build/credentials/$credentialId?publicKey=${encode(publicKey)}"
        val signHeaders = if (signToken.isNotBlank()) {
            ApiSignUtil.generateSignHeader(
                method = HttpMethod.GET.name,
                url = "/api/build/credentials/$credentialId?publicKey=${encode(publicKey)}",
                token = signToken
            )
        } else {
            emptyMap()
        }
        val request = buildGet(path, signHeaders)
        val responseContent = request(
            request,
            MessageUtil.getMessageByLocale(GET_CREDENTIAL_FAILED, AgentEnv.getLocaleLanguage())
        )
        return objectMapper.readValue(responseContent)
    }

    override fun getAcrossProject(
        targetProjectId: String,
        credentialId: String,
        publicKey: String,
        signToken: String
    ): Result<CredentialInfo> {
        val path = "/ms/ticket/api/build/credentials/$credentialId/across" +
            "?publicKey=${encode(publicKey)}&targetProjectId=$targetProjectId"
        val signHeaders = if (signToken.isNotBlank()) {
            ApiSignUtil.generateSignHeader(
                method = HttpMethod.GET.name,
                url = "/api/build/credentials/$credentialId?publicKey=${encode(publicKey)}",
                token = signToken
            )
        } else {
            emptyMap()
        }
        val request = buildGet(path, signHeaders)
        val responseContent = request(
            request,
            MessageUtil.getMessageByLocale(GET_CREDENTIAL_FAILED, AgentEnv.getLocaleLanguage())
        )
        return objectMapper.readValue(responseContent)
    }
}

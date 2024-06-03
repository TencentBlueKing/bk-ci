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

package com.tencent.devops.stream.util

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.ticket.api.ServiceCredentialResource
import com.tencent.devops.ticket.pojo.Credential
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.slf4j.LoggerFactory
import java.util.Base64

object CommonCredentialUtils {

    private val logger = LoggerFactory.getLogger(CommonCredentialUtils::class.java)

    @Suppress("ComplexMethod")
    fun getCredential(
        client: Client,
        projectId: String,
        credentialId: String,
        typeCheck: List<CredentialType>,
        acrossProject: Boolean = false
    ): Credential {
        val pair = DHUtil.initKey()
        val encoder = Base64.getEncoder()
        val decoder = Base64.getDecoder()
        val credentialResult = client.get(ServiceCredentialResource::class).get(
            projectId, credentialId,
            encoder.encodeToString(pair.publicKey)
        )
        if (credentialResult.isNotOk() || credentialResult.data == null) {
            logger.warn(
                "CommonCredentialUtils|getCredential|$credentialId|$projectId|msg=${credentialResult.message}"
            )
            throw RuntimeException("Fail to get the credential($credentialId) of project($projectId)")
        }

        val credential = credentialResult.data!!
        if (credential.credentialType !in typeCheck) {
            logger.warn("CommonCredentialUtils|getCredential|$typeCheck|${credential.credentialType.name}")
            throw ParamBlankException(
                "Fail to get the credential($credentialId) in ${credential.credentialType.name} " +
                    "of project($projectId), only support (${typeCheck.joinToString { it.name }})"
            )
        }

        if (acrossProject && !credential.allowAcrossProject) {
            logger.warn("CommonCredentialUtils|getCredential|$projectId|$credentialId")
            throw RuntimeException("Fail to get the credential($credentialId) of project($projectId)")
        }

        val v1 = String(
            DHUtil.decrypt(
                decoder.decode(credential.v1),
                decoder.decode(credential.publicKey),
                pair.privateKey
            )
        )

        val v2 = if (credential.v2 != null && credential.v2!!.isNotEmpty()) {
            String(
                DHUtil.decrypt(
                    decoder.decode(credential.v2),
                    decoder.decode(credential.publicKey),
                    pair.privateKey
                )
            )
        } else null

        val v3 = if (credential.v3 != null && credential.v3!!.isNotEmpty()) {
            String(
                DHUtil.decrypt(
                    decoder.decode(credential.v3),
                    decoder.decode(credential.publicKey),
                    pair.privateKey
                )
            )
        } else null

        val v4 = if (credential.v4 != null && credential.v4!!.isNotEmpty()) {
            String(
                DHUtil.decrypt(
                    decoder.decode(credential.v4),
                    decoder.decode(credential.publicKey),
                    pair.privateKey
                )
            )
        } else null
        return Credential(
            credentialId = credentialId,
            credentialType = credential.credentialType,
            v1 = v1,
            v2 = v2,
            v3 = v3,
            v4 = v4
        )
    }
}

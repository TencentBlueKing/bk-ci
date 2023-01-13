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

package com.tencent.devops.repository.service

import com.tencent.devops.common.api.constant.RepositoryMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.DHKeyPair
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.credential.EmptyCredentialInfo
import com.tencent.devops.repository.pojo.credential.RepoCredentialInfo
import com.tencent.devops.repository.pojo.credential.SshCredentialInfo
import com.tencent.devops.repository.pojo.credential.UserNamePasswordCredentialInfo
import com.tencent.devops.ticket.api.ServiceCredentialResource
import com.tencent.devops.ticket.pojo.CredentialInfo
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Base64

@Service
class CredentialService @Autowired constructor(
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(CredentialService::class.java)
    }

    fun getCredential(projectId: String, repository: Repository): List<String> {
        val pair = DHUtil.initKey()
        val encoder = Base64.getEncoder()
        val result = client.get(ServiceCredentialResource::class)
            .get(projectId, repository.credentialId, encoder.encodeToString(pair.publicKey))
        if (result.isNotOk() || result.data == null) {
            throw ErrorCodeException(errorCode = RepositoryMessageCode.GET_TICKET_FAIL)
        }

        val credential = result.data!!
        logger.info("Get the credential($credential)")
        return buildCredentialList(credential, pair)
    }

    /**
     * 获取凭证基础信息
     */
    fun getCredentialInfo(projectId: String, repository: Repository): RepoCredentialInfo {
        val pair = DHUtil.initKey()
        val encoder = Base64.getEncoder()
        val result = client.get(ServiceCredentialResource::class)
            .get(projectId, repository.credentialId, encoder.encodeToString(pair.publicKey))
        if (result.isNotOk() || result.data == null) {
            throw ErrorCodeException(errorCode = RepositoryMessageCode.GET_TICKET_FAIL)
        }
        val credential: CredentialInfo = result.data!!
        logger.info("Get the credential($credential)")
        return buildRepoCredentialInfo(credential, credential.credentialType,pair)
    }

    /**
     * 构建凭证信息集合
     */
    private fun buildCredentialList(
        credential: CredentialInfo,
        pair: DHKeyPair
    ): List<String> {
        val list = ArrayList<String>()
        list.add(decode(credential.v1, credential.publicKey, pair.privateKey))
        if (!credential.v2.isNullOrEmpty()) {
            list.add(decode(credential.v2!!, credential.publicKey, pair.privateKey))
            if (!credential.v3.isNullOrEmpty()) {
                list.add(decode(credential.v3!!, credential.publicKey, pair.privateKey))
                if (!credential.v4.isNullOrEmpty()) {
                    list.add(decode(credential.v4!!, credential.publicKey, pair.privateKey))
                }
            }
        }
        return list
    }

    private fun decode(encode: String, publicKey: String, privateKey: ByteArray): String {
        val decoder = Base64.getDecoder()
        return String(DHUtil.decrypt(decoder.decode(encode), decoder.decode(publicKey), privateKey))
    }

    /**
     * 构建授权信息实体
     */
    private fun buildRepoCredentialInfo(
        credentialInfo: CredentialInfo,
        credentialType: CredentialType,
        pair: DHKeyPair
    ): RepoCredentialInfo {
        return when (credentialType) {
            CredentialType.USERNAME_PASSWORD -> {
                if (credentialInfo.v1.isNullOrEmpty()) {
                    throw OperationException(
                        message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.USER_NAME_EMPTY)
                    )
                }
                if (credentialInfo.v2.isNullOrBlank()) {
                    throw OperationException(
                        message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.PWD_EMPTY)
                    )
                }
                UserNamePasswordCredentialInfo(
                    token = StringUtils.EMPTY,
                    username = decode(credentialInfo.v1, credentialInfo.publicKey, pair.privateKey),
                    password = decode(credentialInfo.v2!!, credentialInfo.publicKey, pair.privateKey)
                )
            }
            CredentialType.TOKEN_USERNAME_PASSWORD -> {
                if (credentialInfo.v2.isNullOrBlank()) {
                    throw OperationException(
                        message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.USER_NAME_EMPTY)
                    )
                }
                if (credentialInfo.v3.isNullOrBlank()) {
                    throw OperationException(
                        message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.PWD_EMPTY)
                    )
                }
                UserNamePasswordCredentialInfo(
                    token = decode(credentialInfo.v1, credentialInfo.publicKey, pair.privateKey),
                    username = decode(credentialInfo.v2!!, credentialInfo.publicKey, pair.privateKey),
                    password = decode(credentialInfo.v3!!, credentialInfo.publicKey, pair.privateKey)
                )
            }
            CredentialType.SSH_PRIVATEKEY -> {
                if (credentialInfo.v1.isNullOrEmpty()) {
                    throw OperationException(
                        message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.USER_SECRET_EMPTY)
                    )
                }
                SshCredentialInfo(
                    token = StringUtils.EMPTY,
                    privateKey = decode(credentialInfo.v1, credentialInfo.publicKey, pair.privateKey),
                    passPhrase = decode(credentialInfo.v2!!, credentialInfo.publicKey, pair.privateKey),
                    username = StringUtils.EMPTY,
                    password = StringUtils.EMPTY
                )
            }
            CredentialType.TOKEN_SSH_PRIVATEKEY -> {
                if (credentialInfo.v2.isNullOrBlank()) {
                    throw OperationException(
                        message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.USER_NAME_EMPTY)
                    )
                }
                SshCredentialInfo(
                    token = decode(credentialInfo.v1, credentialInfo.publicKey, pair.privateKey),
                    privateKey = decode(credentialInfo.v2!!, credentialInfo.publicKey, pair.privateKey),
                    passPhrase = decode(credentialInfo.v3!!, credentialInfo.publicKey, pair.privateKey),
                    username = StringUtils.EMPTY,
                    password = StringUtils.EMPTY
                )
            }
            else -> {
                EmptyCredentialInfo(token = StringUtils.EMPTY)
            }
        }
    }
}

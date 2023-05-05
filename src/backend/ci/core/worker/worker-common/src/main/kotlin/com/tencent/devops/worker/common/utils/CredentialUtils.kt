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

package com.tencent.devops.worker.common.utils

import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DHKeyPair
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.api.util.KeyReplacement
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.ReplacementUtils
import com.tencent.devops.common.expression.context.DictionaryContextData
import com.tencent.devops.common.expression.context.PipelineContextData
import com.tencent.devops.common.expression.context.RuntimeNamedValue
import com.tencent.devops.common.expression.context.StringContextData
import com.tencent.devops.ticket.pojo.CredentialInfo
import com.tencent.devops.ticket.pojo.enums.CredentialType
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.ticket.CredentialSDKApi
import com.tencent.devops.worker.common.constants.WorkerMessageCode
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.service.SensitiveValueService
import java.util.Base64
import org.slf4j.LoggerFactory

/**
 * This util is to get the credential from core
 * It use DH encrypt and decrypt
 */
@Suppress("ALL")
object CredentialUtils {

    private val sdkApi = ApiFactory.create(CredentialSDKApi::class)
    var signToken = ""

    fun getCredential(
        credentialId: String,
        showErrorLog: Boolean = true,
        acrossProjectId: String? = null
    ): List<String> {
        return getCredentialWithType(credentialId, showErrorLog, acrossProjectId).first
    }

    fun getCredentialWithType(
        credentialId: String,
        showErrorLog: Boolean = true,
        acrossProjectId: String? = null
    ): Pair<List<String>, CredentialType> {
        if (credentialId.trim().isEmpty()) {
            throw TaskExecuteException(
                errorCode = ErrorCode.USER_INPUT_INVAILD,
                errorType = ErrorType.USER,
                errorMsg = "The credential Id is empty"
            )
        }
        try {
            val pair = DHUtil.initKey()
            val result = requestCredential(credentialId, pair, acrossProjectId)
            val credential = result.data!!
            return Pair(getDecodedCredentialList(credential, pair), credential.credentialType)
        } catch (ignored: Exception) {
            logger.warn("Fail to get the credential($credentialId), $ignored")
            if (showErrorLog) {
                LoggerService.addErrorLine("Fail to get the credential($credentialId)， cause：${ignored.message}")
            }
            throw ignored
        }
    }

    @Deprecated("保留原处理变量值逻辑，后续替换凭据建议使用表达式实现")
    fun String.parseCredentialValue(
        context: Map<String, String>? = null,
        acrossProjectId: String? = null
    ) = ReplacementUtils.replace(
        this,
        object : KeyReplacement {
            override fun getReplacement(key: String): String? {
                // 支持嵌套的二次替换
                context?.get(key)?.let { return it }
                // 如果不是凭据上下文则直接返回原value值
                return getCredentialContextValue(key, acrossProjectId)
            }
        },
        context
    )

    class CredentialRuntimeNamedValue(
        override val key: String = "settings",
        private val targetProjectId: String? = null
    ) : RuntimeNamedValue {
        override fun getValue(key: String): PipelineContextData? {
            return DictionaryContextData().apply {
                try {
                    val pair = DHUtil.initKey()
                    val credentialInfo = requestCredential(key, pair, targetProjectId).data!!
                    val credentialList = getDecodedCredentialList(credentialInfo, pair)
                    val keyMap = CredentialType.Companion.getKeyMap(credentialInfo.credentialType.name)
                    logger.info(
                        "[$key]|CredentialRuntimeNamedValue|credentialInfo=$credentialInfo|" +
                            "credentialList=$credentialList|keyMap=$keyMap"
                    )
                    credentialList.forEachIndexed { index, credential ->
                        val token = keyMap["v${index + 1}"] ?: return@forEachIndexed
                        add(token, StringContextData(credential))
                    }
                } catch (ignore: Throwable) {
                    logger.warn("[$key]|Expression get credential value: ", ignore)
                    return null
                }
            }
        }
    }

    private fun requestCredential(
        credentialId: String,
        pair: DHKeyPair,
        acrossProjectId: String?
    ): Result<CredentialInfo> {
        val encoder = Base64.getEncoder()
        logger.info("Start to get the credential($credentialId|$acrossProjectId)")

        val result = sdkApi.get(
            credentialId = credentialId,
            publicKey = encoder.encodeToString(pair.publicKey),
            signToken = signToken
        )
        if (result.isOk() && result.data != null) {
            return result
        }
        // 当前项目取不到查看是否有跨项目凭证
        if (!acrossProjectId.isNullOrBlank()) {
            val acrossResult =
                sdkApi.getAcrossProject(
                    targetProjectId = acrossProjectId,
                    credentialId = credentialId,
                    publicKey = encoder.encodeToString(pair.publicKey),
                    signToken = signToken
                )
            if (acrossResult.isNotOk() || acrossResult.data == null) {
                logger.error(
                    "Fail to get the across project($acrossProjectId) " +
                        "credential($credentialId) because of ${result.message}"
                )
                throw TaskExecuteException(
                    errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND,
                    errorType = ErrorType.USER,
                    errorMsg = result.message!!
                )
            }
            return acrossResult
        }

        logger.error("Fail to get the credential($credentialId) because of ${result.message}")
        throw TaskExecuteException(
            errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND,
            errorType = ErrorType.USER,
            errorMsg = result.message!!
        )
    }

    fun getCredentialContextValue(key: String, acrossProjectId: String? = null): String? {
        val ticketId = getCredentialKey(key)
        if (ticketId == key) {
            return null
        }

        return try {
            val valueTypePair = getCredentialWithType(ticketId, false, acrossProjectId)
            val value = getCredentialValue(valueTypePair.first, valueTypePair.second, key)
            logger.info("get credential context value, key: $key acrossProjectId: $acrossProjectId")
            value
        } catch (ignore: Exception) {
            logger.warn(
                MessageUtil.getMessageByLocale(
                    WorkerMessageCode.CREDENTIAL_ID_NOT_EXIST,
                    AgentEnv.getLocaleLanguage(),
                    arrayOf(ticketId)
                ),
                ignore.message
            )
            null
        }
    }

    private fun getCredentialKey(key: String): String {
        // 参考CredentialType
        return if (key.startsWith("settings.") && (
            key.endsWith(".password") ||
                key.endsWith(".access_token") ||
                key.endsWith(".username") ||
                key.endsWith(".secretKey") ||
                key.endsWith(".appId") ||
                key.endsWith(".privateKey") ||
                key.endsWith(".passphrase") ||
                key.endsWith(".token") ||
                key.endsWith(".cosappId") ||
                key.endsWith(".secretId") ||
                key.endsWith(".region")
            )
        ) {
            key.substringAfter("settings.").substringBeforeLast(".")
        } else {
            key
        }
    }

    private fun getCredentialValue(valueList: List<String>, type: CredentialType, key: String): String? {
        if (valueList.isEmpty()) {
            return null
        }

        when (type) {
            CredentialType.PASSWORD -> {
                if (key.endsWith(".password")) {
                    return valueList[0]
                }
            }
            CredentialType.ACCESSTOKEN -> {
                if (key.endsWith(".access_token")) {
                    return valueList[0]
                }
            }
            CredentialType.USERNAME_PASSWORD -> {
                if (valueList.size >= 2) {
                    if (key.endsWith(".username")) {
                        return valueList[0]
                    }
                    if (key.endsWith(".password")) {
                        return valueList[1]
                    }
                }
            }
            CredentialType.SECRETKEY -> {
                if (key.endsWith(".secretKey")) {
                    return valueList[0]
                }
            }
            CredentialType.APPID_SECRETKEY -> {
                if (valueList.size >= 2) {
                    if (key.endsWith(".appId")) {
                        return valueList[0]
                    }
                    if (key.endsWith(".secretKey")) {
                        return valueList[1]
                    }
                }
            }
            CredentialType.SSH_PRIVATEKEY -> {
                if (valueList.size == 1) {
                    if (key.endsWith(".privateKey")) {
                        return valueList[0]
                    }
                }
                if (valueList.size >= 2) {
                    if (key.endsWith(".privateKey")) {
                        return valueList[0]
                    }
                    if (key.endsWith(".passphrase")) {
                        return valueList[1]
                    }
                }
            }
            CredentialType.TOKEN_SSH_PRIVATEKEY -> {
                if (valueList.size == 2) {
                    if (key.endsWith(".token")) {
                        return valueList[0]
                    }
                    if (key.endsWith(".privateKey")) {
                        return valueList[1]
                    }
                }
                if (valueList.size >= 3) {
                    if (key.endsWith(".token")) {
                        return valueList[0]
                    }
                    if (key.endsWith(".privateKey")) {
                        return valueList[1]
                    }
                    if (key.endsWith(".passphrase")) {
                        return valueList[2]
                    }
                }
            }
            CredentialType.TOKEN_USERNAME_PASSWORD -> {
                if (valueList.size >= 3) {
                    if (key.endsWith(".token")) {
                        return valueList[0]
                    }
                    if (key.endsWith(".username")) {
                        return valueList[1]
                    }
                    if (key.endsWith(".password")) {
                        return valueList[2]
                    }
                }
            }
            CredentialType.COS_APPID_SECRETID_SECRETKEY_REGION -> {
                if (valueList.size >= 4) {
                    if (key.endsWith(".cosappId")) {
                        return valueList[0]
                    }
                    if (key.endsWith(".secretId")) {
                        return valueList[1]
                    }
                    if (key.endsWith(".secretKey")) {
                        return valueList[2]
                    }
                    if (key.endsWith(".region")) {
                        return valueList[3]
                    }
                }
            }
            CredentialType.MULTI_LINE_PASSWORD -> {
                if (valueList.isNotEmpty() && key.endsWith(".password")) {
                    return valueList[0]
                }
            }
        }
        return null
    }

    private fun decode(encode: String, publicKey: String, privateKey: ByteArray): String {
        val decoder = Base64.getDecoder()
        return String(DHUtil.decrypt(decoder.decode(encode), decoder.decode(publicKey), privateKey))
    }

    private fun getDecodedCredentialList(
        credential: CredentialInfo,
        pair: DHKeyPair
    ): List<String> {
        val list = mutableListOf<String>()

        list.add(decode(credential.v1, credential.publicKey, pair.privateKey))
        credential.v2?.let { list.add(decode(it, credential.publicKey, pair.privateKey)) }
        credential.v3?.let { list.add(decode(it, credential.publicKey, pair.privateKey)) }
        credential.v4?.let { list.add(decode(it, credential.publicKey, pair.privateKey)) }

        // #4732 日志脱敏，被请求过的凭据除用户名外统一过滤
        val sensitiveList = mutableListOf<String>()
        when (credential.credentialType) {
            CredentialType.USERNAME_PASSWORD -> {
                // 只获取密码，不获取v1用户名
                credential.v2?.let { sensitiveList.add(decode(it, credential.publicKey, pair.privateKey)) }
            }
            CredentialType.TOKEN_USERNAME_PASSWORD -> {
                // 只获取密码和token，不获取v2用户名
                sensitiveList.add(decode(credential.v1, credential.publicKey, pair.privateKey))
                credential.v3?.let { sensitiveList.add(decode(it, credential.publicKey, pair.privateKey)) }
            }
            else -> {
                sensitiveList.addAll(list)
            }
        }
        SensitiveValueService.addSensitiveValues(sensitiveList)
        return list
    }

    private val logger = LoggerFactory.getLogger(CredentialUtils::class.java)
}

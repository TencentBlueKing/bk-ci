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

package com.tencent.devops.dispatch.kubernetes.utils

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.dispatch.kubernetes.common.ErrorCodeEnum
import com.tencent.devops.ticket.api.ServiceCredentialResource
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.slf4j.LoggerFactory
import java.util.Base64

@SuppressWarnings("NestedBlockDepth")
object CommonUtils {

    private val logger = LoggerFactory.getLogger(CommonUtils::class.java)

    //    private const val dockerHubUrl = "https://index.docker.io/v1/"
    private const val dockerHubUrl = ""

    /**
     * 转换镜像全名为各个属性
     */
    fun parseImage(imageNameInput: String): Triple<String, String, String> {
        val imageNameStr = imageNameInput.removePrefix("http://").removePrefix("https://")
        val imageNames = imageNameStr.split(":")
        if (imageNames.size == 1) {
            val str = imageNameStr.split("/")
            return if (str.size == 1) {
                Triple(dockerHubUrl, imageNameStr, "latest")
            } else {
                Triple(str[0], imageNameStr.substringAfter(str[0] + "/"), "latest")
            }
        } else if (imageNames.size == 2) {
            val str = imageNameStr.split("/")
            when {
                str.size == 1 -> return Triple(dockerHubUrl, imageNames[0], imageNames[1])
                str.size >= 2 -> return if (str[0].contains(":")) {
                    Triple(str[0], imageNameStr.substringAfter(str[0] + "/"), "latest")
                } else {
                    if (str.last().contains(":")) {
                        val nameTag = str.last().split(":")
                        Triple(
                            str[0],
                            imageNameStr.substringAfter(str[0] + "/").substringBefore(":" + nameTag[1]),
                            nameTag[1]
                        )
                    } else {
                        Triple(str[0], str.last(), "latest")
                    }
                }
                else -> {
                    logger.error("image name invalid: $imageNameStr")
                    throw Exception("image name invalid.")
                }
            }
        } else if (imageNames.size == 3) {
            val str = imageNameStr.split("/")
            if (str.size >= 2) {
                val tail = imageNameStr.removePrefix(str[0] + "/")
                val nameAndTag = tail.split(":")
                if (nameAndTag.size != 2) {
                    logger.error("image name invalid: $imageNameStr")
                    throw Exception("image name invalid.")
                }
                return Triple(str[0], nameAndTag[0], nameAndTag[1])
            } else {
                logger.error("image name invalid: $imageNameStr")
                throw Exception("image name invalid.")
            }
        } else {
            logger.error("image name invalid: $imageNameStr")
            throw Exception("image name invalid.")
        }
    }

    /**
     * 获取凭证
     */
    fun getCredential(
        client: Client,
        projectId: String,
        credentialId: String,
        type: CredentialType
    ): MutableMap<String, String> {
        val pair = DHUtil.initKey()
        val encoder = Base64.getEncoder()
        val decoder = Base64.getDecoder()
        try {
            val credentialResult = client.get(ServiceCredentialResource::class).get(
                projectId, credentialId,
                encoder.encodeToString(pair.publicKey)
            )
            if (credentialResult.isNotOk() || credentialResult.data == null) {
                throw TaskExecuteException(
                    errorCode = ErrorCode.SYSTEM_SERVICE_ERROR,
                    errorType = ErrorType.SYSTEM,
                    errorMsg = "Fail to get the credential($credentialId) of project($projectId)"
                )
            }

            val credential = credentialResult.data!!
            if (type != credential.credentialType) {
                logger.error("CredentialId is invalid, expect:${type.name}, but real:${credential.credentialType.name}")
                throw ParamBlankException("Fail to get the credential($credentialId) of project($projectId)")
            }

            val ticketMap = mutableMapOf<String, String>()
            val v1 = String(
                DHUtil.decrypt(
                    decoder.decode(credential.v1),
                    decoder.decode(credential.publicKey),
                    pair.privateKey
                )
            )
            ticketMap["v1"] = v1

            if (credential.v2 != null && credential.v2!!.isNotEmpty()) {
                val v2 = String(
                    DHUtil.decrypt(
                        decoder.decode(credential.v2),
                        decoder.decode(credential.publicKey),
                        pair.privateKey
                    )
                )
                ticketMap["v2"] = v2
            }

            if (credential.v3 != null && credential.v3!!.isNotEmpty()) {
                val v3 = String(
                    DHUtil.decrypt(
                        decoder.decode(credential.v3),
                        decoder.decode(credential.publicKey),
                        pair.privateKey
                    )
                )
                ticketMap["v3"] = v3
            }

            if (credential.v4 != null && credential.v4!!.isNotEmpty()) {
                val v4 = String(
                    DHUtil.decrypt(
                        decoder.decode(credential.v4),
                        decoder.decode(credential.publicKey),
                        pair.privateKey
                    )
                )
                ticketMap["v4"] = v4
            }

            return ticketMap
        } catch (e: Exception) {
            throw BuildFailureException(
                errorType = ErrorCodeEnum.GET_CREDENTIAL_FAIL.errorType,
                errorCode = ErrorCodeEnum.GET_CREDENTIAL_FAIL.errorCode,
                formatErrorMessage = ErrorCodeEnum.GET_CREDENTIAL_FAIL.formatErrorMessage,
                errorMessage = ErrorCodeEnum.GET_CREDENTIAL_FAIL.formatErrorMessage
            )
        }
    }

    /**
     * 抛出异常
     */
    fun onFailure(errorType: ErrorType, errorCode: Int, formatErrorMessage: String, message: String) {
        throw BuildFailureException(errorType, errorCode, formatErrorMessage, message)
    }

    /**
     * 生成异常
     */
    fun buildFailureException(errorCodeEnum: ErrorCodeEnum, message: String): BuildFailureException {
        return BuildFailureException(
            errorCodeEnum.errorType,
            errorCodeEnum.errorCode,
            errorCodeEnum.formatErrorMessage,
            message
        )
    }

//    fun generatePwd(): String {
//        val secretSeed = arrayOf(
//            "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
//            "abcdefghijklmnopqrstuvwxyz",
//            "0123456789",
//            "[()~!@#%&-+=_"
//        )
//
//        val random = Random()
//        val buf = StringBuffer()
//        for (i in 0 until 15) {
//            val num = random.nextInt(secretSeed[i / 4].length)
//            buf.append(secretSeed[i / 4][num])
//        }
//        return buf.toString()
//    }
}

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

package com.tencent.devops.common.gcloud.utils

import com.tencent.devops.common.gcloud.api.pojo.ActionParam
import com.tencent.devops.common.gcloud.api.pojo.CommonParam
import com.tencent.devops.common.gcloud.api.pojo.ModuleParam
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import java.util.TreeMap
import java.util.Random
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object GcloudUtil {
    private val logger = LoggerFactory.getLogger(GcloudUtil::class.java)

    fun getRequestUriWithSignature(
        host: String,
        fileHost: String,
        customParam: Map<String, Any>,
        commonParam: CommonParam,
        moduleParam: ModuleParam,
        actionParam: ActionParam
    ): String {
        val paramMap = TreeMap<String, Any>()
        paramMap.putAll(customParam)
        paramMap["gameid"] = commonParam.gameId
        paramMap["ts"] = System.currentTimeMillis().toString()
        paramMap["nonce"] = Random().nextInt(Int.MAX_VALUE).toString()
        paramMap["accessid"] = commonParam.accessId

        val uri = "/v1/openapi/${moduleParam.name.toLowerCase()}/${actionParam.name}"
        var reqUri = if (moduleParam == ModuleParam.FILE) "http://$fileHost$uri?" else "http://$host$uri?"
        var sig = "$uri?"

        var isFirst = true
        paramMap.forEach {
            if (isFirst) {
                isFirst = false
            } else {
                sig += "&"
                reqUri += "&"
            }
            sig += "${it.key}=${it.value}"
            reqUri += "${it.key}=${URLEncoder.encode(it.value.toString(), "UTF-8")}"
        }
        logger.info(sig)
        sig = hmacSha1Encode(commonParam.accessKey, sig)
        sig = sig.replace("+", "-")
        sig = sig.replace("/", "_")

        reqUri += "&signature=$sig"
        logger.info("Request to gcloud, url:$reqUri")

        return reqUri
    }

    private fun hmacSha1Encode(key: String, message: String): String {
        val keySpec = SecretKeySpec(
                key.toByteArray(),
                "HmacSHA1")

        val mac = Mac.getInstance("HmacSHA1")
        mac.init(keySpec)
        val rawHmac = mac.doFinal(message.toByteArray())

        return Base64.getEncoder().encodeToString(rawHmac)
    }
}

package com.tencent.devops.common.util

import com.tencent.devops.common.api.auth.SIGN_HEADER_NONCE
import com.tencent.devops.common.api.auth.SIGN_HEADER_TIMESTAMP
import com.tencent.devops.common.api.auth.SING_HEADER_SIGNATURE
import com.tencent.devops.common.api.util.UUIDUtil
import org.apache.commons.codec.digest.HmacAlgorithms
import org.apache.commons.codec.digest.HmacUtils

object ApiSignUtil {

    fun generateSignHeader(method: String, url: String, token: String): Map<String, String> {
        val timestamp = System.currentTimeMillis()
        val nonce = UUIDUtil.generate()
        val signature = signToRequest(
            method = method,
            url = url,
            timestamp = timestamp.toString(),
            nonce = nonce,
            token = token
        )
        return mapOf(
            SIGN_HEADER_TIMESTAMP to timestamp.toString(),
            SIGN_HEADER_NONCE to nonce,
            SING_HEADER_SIGNATURE to signature
        )
    }

    fun signToRequest(
        method: String,
        url: String,
        timestamp: String,
        nonce: String,
        token: String
    ): String {
        val message = "${method}\n${url}\n${timestamp}\n${nonce}\n"
        return HmacUtils(HmacAlgorithms.HMAC_SHA_256, token).hmacHex(message)
    }
}

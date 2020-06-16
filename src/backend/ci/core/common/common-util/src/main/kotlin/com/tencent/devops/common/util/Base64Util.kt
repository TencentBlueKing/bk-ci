package com.tencent.devops.common.util

import org.apache.commons.codec.binary.Base64
import org.apache.commons.lang3.StringUtils
import java.nio.charset.StandardCharsets

object Base64Util {
    /**
     * BASE64 解码字符，返回解码后的字符
     *
     * @param content
     * @return
     */
    fun base64DecodeContentToStr(content: String?): String? {
        return if (StringUtils.isEmpty(content)) {
            null
        } else try {
            String(
                Base64.decodeBase64(content),
                StandardCharsets.UTF_8
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * BASE64 编码字符，返回编码后的字符
     *
     * @param content
     * @return
     */
    fun base64EncodeContentToStr(content: String): String? {
        return if (StringUtils.isEmpty(content)) {
            null
        } else try {
            Base64.encodeBase64String(content.toByteArray(StandardCharsets.UTF_8))
        } catch (e: Exception) {
            null
        }
    }

    /**
     * BASE64 编码字节流，返回编码后的字符
     *
     * @param byteContent
     * @return
     */
    fun base64EncodeContentToStr(byteContent: ByteArray?): String? {
        return if (byteContent == null || byteContent.size == 0) {
            null
        } else try {
            Base64.encodeBase64String(byteContent)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * BASE64 解码字符，返回解码后的字节流
     *
     * @param content
     * @return
     */
    fun base64DecodeToByte(content: String?): ByteArray? {
        return if (StringUtils.isEmpty(content)) {
            ByteArray(0)
        } else try {
            Base64.decodeBase64(content)
        } catch (e: Exception) {
            null
        }
    }
}

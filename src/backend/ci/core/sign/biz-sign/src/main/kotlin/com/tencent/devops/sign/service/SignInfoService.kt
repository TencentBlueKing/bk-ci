package com.tencent.devops.sign.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.sign.api.constant.SignMessageCode
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.impl.SignServiceImpl
import org.jolokia.util.Base64Util
import org.slf4j.LoggerFactory
import java.io.File

interface SignInfoService {
    /*
    * 检查IpaSignInfo信息，并补齐默认值，如果返回null则表示IpaSignInfo的值不合法
    * */
    fun check(info: IpaSignInfo): IpaSignInfo

    fun save(resignId: String, ipaSignInfoHeader: String, info: IpaSignInfo)

    fun decodeIpaSignInfo(ipaSignInfoHeader: String): IpaSignInfo {
        try {
            val ipaSignInfoHeaderDecode = String(Base64Util.decode(ipaSignInfoHeader))
            val objectMapper = ObjectMapper()
            return objectMapper.readValue(ipaSignInfoHeaderDecode, IpaSignInfo::class.java)
        } catch (e: Exception) {
            logger.error("解析签名信息失败：$e")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_PARSE_SIGN_INFO_HEADER, defaultMessage = "解析签名信息失败")
        }
    }

    fun encodeIpaSignInfo(ipaSignInfo: IpaSignInfo): String {
        try {
            val objectMapper = ObjectMapper()
            val ipaSignInfoJson =objectMapper.writeValueAsString(ipaSignInfo)
            return Base64Util.encode(ipaSignInfoJson.toByteArray())
        } catch (e: Exception) {
            logger.error("编码签名信息失败：$e")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_ENCODE_SIGN_INFO, defaultMessage = "编码签名信息失败")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SignInfoService::class.java)
    }
}
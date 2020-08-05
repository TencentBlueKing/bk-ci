package com.tencent.devops.sign.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.sign.SignMessageCode
import com.tencent.devops.sign.pojo.IosProfile
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class KeystoreService {

    @Value("\${keystore.url:#{null}}")
    val keyStoreUrl: String? = null

    fun getHost(): String {
        return HomeHostUtil.getHost(keyStoreUrl!!)
    }

    fun getInHouseCertList(appId: String): Result<List<IosProfile?>> {
        logger.info("getInHouseCertList from KeyStore with appId:$appId")
        OkhttpUtils.doGet(getKeystoreUrl(appId)).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("[${getHost()}|$appId] Fail to get ios Certs from keystore with response [${response.code()}|${response.message()}|$responseContent]")
                throw OperationException("[${getHost()}|$appId]| Fail to get ios Certs from keystore")
            }
            logger.info("response: $responseContent")
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
            val code = responseData["code"] as Int
            if (0 != code) {
                val message = responseData["msg"] as String
                logger.warn("[${getHost()}|$appId]|getInHouseCertList|return error [${response.code()}|$message|$responseContent]")
                throw ErrorCodeException(
                    errorCode = SignMessageCode.KEYSTORE_RESOURCE_NOT_EXISTS,
                    defaultMessage = message,
                    params = arrayOf(appId)
                )
            }
            return Result(responseData["data"] as List<IosProfile>)
        }
    }

    private fun getKeystoreUrl(appId: String): String =
        "${getHost()}/api/auth/getInHouseCertList?appId=$appId"

    companion object {
        private val logger = LoggerFactory.getLogger(KeystoreService::class.java)
    }
}
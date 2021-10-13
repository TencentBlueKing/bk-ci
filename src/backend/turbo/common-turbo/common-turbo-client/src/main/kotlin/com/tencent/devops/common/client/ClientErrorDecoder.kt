package com.tencent.devops.common.client

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.ClientException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.utils.jackson.JsonUtils
import feign.Response
import feign.codec.ErrorDecoder
import org.springframework.stereotype.Service
import java.io.IOException

/**
 *
 * Powered By Tencent
 */
@Service
class ClientErrorDecoder : ErrorDecoder {
    override fun decode(methodKey: String, response: Response): Exception {
        // 首先判断返回结果是否能被序列化
        val responseStream = response.body().asInputStream()
        val result: Result<*>
        try {
            result = JsonUtils.objectMapper.readValue(responseStream)
        } catch (e: IOException) {
            return ClientException("内部服务返回结果无法解析")
        }
        if (response.status() == OperationException.statusCode) {
            throw OperationException(result.message ?: "")
        }
        return RemoteServiceException(result.message ?: "", response.status())
    }
}

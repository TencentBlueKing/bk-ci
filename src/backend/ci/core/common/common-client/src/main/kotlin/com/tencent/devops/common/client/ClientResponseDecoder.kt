package com.tencent.devops.common.client

import com.fasterxml.jackson.databind.ObjectMapper
import feign.Response
import feign.codec.DecodeException
import feign.jackson.JacksonDecoder
import java.io.IOException
import java.lang.reflect.Type
import jakarta.ws.rs.core.Response as JakartaResponse

/**
 * 自定义解析 Feign Decoder
 */
class ClientResponseDecoder(mapper: ObjectMapper) : JacksonDecoder(mapper) {

    override fun decode(response: Response, type: Type): Any {
        // 检查目标类型是否为 jakarta.ws.rs.core.Response
        if (type == JakartaResponse::class.java) {
            return try {
                // 构建 Jakarta Response
                buildJakartaResponse(response)
            } catch (e: IOException) {
                throw DecodeException(
                    response.status(),
                    "Failed to decode response: ${e.message}",
                    response.request(),
                    e
                )
            }
        }

        return super.decode(response, type)
    }

    /**
     * 将 Feign Response 转换为 Jakarta Response
     */
    private fun buildJakartaResponse(feignResponse: Response): JakartaResponse {
        val responseBuilder = JakartaResponse.status(feignResponse.status())

        // 添加响应头
        feignResponse.headers().forEach { (name, values) ->
            values.forEach { value ->
                responseBuilder.header(name, value)
            }
        }

        // 添加响应体
        val entity = feignResponse.body()?.let { body ->
            try {
                // 读取响应体内容
                body.asInputStream().use { inputStream ->
                    inputStream.readBytes()
                }
            } catch (e: IOException) {
                throw IOException("Failed to read response body", e)
            }
        }

        if (entity != null) {
            responseBuilder.entity(entity)
        }

        return responseBuilder.build()
    }
}

package com.tencent.devops.common.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.RuntimeJsonMappingException
import feign.Response
import feign.codec.Decoder
import java.io.BufferedReader
import java.io.IOException
import java.lang.reflect.Type

/**
 * @see feign.jackson.JacksonDecoder
 * 相比JacksonDecoder解决了状态码为404时，返回结果为null的情况,需要在Feign构建时增加Feign.builder().decode404(),开启404状态解析
 */
class ClientJacksonDecoder constructor(private val mapper: ObjectMapper) : Decoder {

    override fun decode(response: Response, type: Type): Any? {
        if (response.body() == null) return null
        var reader = response.body().asReader()
        if (!reader.markSupported()) {
            reader = BufferedReader(reader, 1)
        }
        try {
            reader.mark(1)
            if (reader.read() == -1) {
                return null
            }
            reader.reset()
            return mapper.readValue(reader, mapper.constructType(type))
        } catch (e: RuntimeJsonMappingException) {
            if (e.cause != null && e.cause is IOException) {
                throw IOException::class.java.cast(e.cause)
            }
            throw e
        }
    }
}
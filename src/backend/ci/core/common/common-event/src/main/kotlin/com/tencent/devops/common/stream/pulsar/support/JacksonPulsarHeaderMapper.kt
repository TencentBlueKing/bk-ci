/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.devops.common.stream.pulsar.support

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.stream.pulsar.constant.Serialization
import org.slf4j.LoggerFactory
import org.springframework.messaging.MessageHeaders
import org.springframework.util.ClassUtils
import java.io.IOException
import java.util.Objects

/**
 * jackson header mapper for Pulsar. Header types are added to a special header
 * {@link #JSON_TYPES}.
 */
@Suppress("ComplexMethod")
class JacksonPulsarHeaderMapper(
    private val objectMapper: ObjectMapper,
    serialization: Serialization = Serialization.BYTE
) : AbstractPulsarHeaderMapper(serialization) {

    private val trustedPackages: MutableSet<String> = LinkedHashSet(
        DEFAULT_TRUSTED_PACKAGES
    )

    override fun fromHeaders(headers: MessageHeaders): Map<String, String> {
        val target = mutableMapOf<String, String>()
        val jsonHeaders = mutableMapOf<String, String>()
        headers.forEach { key: String, value: Any ->
            if (!matches(key)) {
                return@forEach
            }
            if (value is String) {
                target[key] = value
            } else {
                try {
                    val className = value.javaClass.name
                    target[key] = objectMapper.writeValueAsString(value)
                    jsonHeaders[key] = className
                } catch (e: Exception) {
                    logger.debug(
                        "Could not map " + key + " with type " +
                            value.javaClass.name,
                        e
                    )
                }
            }
        }
        if (jsonHeaders.isNotEmpty()) {
            try {
                target[JSON_TYPES] = objectMapper.writeValueAsString(jsonHeaders)
            } catch (e: IllegalStateException) {
                logger.error("Could not add json types header", e)
            } catch (e: JsonProcessingException) {
                logger.error("Could not add json types header", e)
            }
        }
        return target
    }

    override fun toHeaders(source: Map<String, String>): MessageHeaders {
        val target = mutableMapOf<String, Any>()
        val jsonTypes = decodeJsonTypes(source)
        source.forEach { (key: String, value: String) ->
            if (!matches(key) || key == JSON_TYPES) {
                return@forEach
            }
            if (jsonTypes.containsKey(key)) {
                target[key] = getHeaderTarget(
                    key = key,
                    value = value,
                    jsonTypes = jsonTypes
                )
            } else {
                target[key] = value
            }
        }
        return MessageHeaders(target)
    }

    private fun getHeaderTarget(key: String, value: String, jsonTypes: Map<String, String>): Any {
        var type: Class<*> = Any::class.java
        val requestedType = jsonTypes[key]
        val trusted = trusted(requestedType)
        if (trusted) {
            try {
                type = ClassUtils.forName(requestedType!!, null)
            } catch (e: Exception) {
                logger.error("Could not load class for header: $key", e)
            }
        }
        return if (trusted) {
            try {
                decodeValue(value, type)
            } catch (e: IOException) {
                logger.error(
                    "Could not decode json type: " + value +
                        " for key: " + key,
                    e
                )
                value
            }
        } else {
            NonTrustedHeaderType(value, requestedType)
        }
    }

    /**
     * @param packagesToTrust the packages to trust.
     * @see .addTrustedPackages
     */
    fun addTrustedPackages(vararg packagesToTrust: String) {
        if (Objects.nonNull(packagesToTrust)) {
            addTrustedPackages(listOf(*packagesToTrust))
        }
    }

    /**
     * Add packages to the trusted packages list (default `java.util, java.lang`)
     * used when constructing objects from JSON. If any of the supplied packages is
     * `"*"`, all packages are trusted. If a class for a non-trusted package is
     * encountered, the header is returned to the application with value of type
     * [NonTrustedHeaderType].
     * @param packagesToTrust the packages to trust.
     */
    private fun addTrustedPackages(packagesToTrust: Collection<String>) {
        for (whiteList in packagesToTrust) {
            if ("*" == whiteList) {
                trustedPackages.clear()
                break
            } else {
                trustedPackages.add(whiteList)
            }
        }
    }

    fun getTrustedPackages(): Set<String> {
        return trustedPackages
    }

    fun getObjectMapper(): ObjectMapper {
        return objectMapper
    }

    @Throws(IOException::class, LinkageError::class)
    private fun decodeValue(jsonString: String, type: Class<*>): Any {
        var value = objectMapper.readValue(jsonString, type)
        if (type == NonTrustedHeaderType::class.java) {
            // Upstream NTHT propagated; may be trusted here...
            val nth = value as NonTrustedHeaderType
            if (trusted(nth.untrustedType)) {
                try {
                    value = objectMapper.readValue(
                        nth.headerValue,
                        ClassUtils.forName(nth.untrustedType!!, null)
                    )
                } catch (e: Exception) {
                    logger.error("Could not decode header: $nth", e)
                }
            }
        }
        return value
    }

    private fun decodeJsonTypes(source: Map<String, String>): Map<String, String> {
        if (source.containsKey(JSON_TYPES)) {
            val value = source[JSON_TYPES]
            try {
                return objectMapper.readValue(value, object : TypeReference<Map<String, String>>() {})
            } catch (e: IOException) {
                logger.error("Could not decode json types: $value", e)
            }
        }
        return emptyMap()
    }

    private fun trusted(requestedType: String?): Boolean {
        if (requestedType == NonTrustedHeaderType::class.java.name) {
            return true
        }
        if (trustedPackages.isNotEmpty()) {
            val lastDot = requestedType!!.lastIndexOf('.')
            if (lastDot < 0) {
                return false
            }
            val packageName = requestedType.substring(0, lastDot)
            for (trustedPackage in trustedPackages) {
                if (packageName == trustedPackage || packageName.startsWith("$trustedPackage.")) {
                    return true
                }
            }
            return false
        }
        return true
    }
    companion object {
        private val logger = LoggerFactory.getLogger(JacksonPulsarHeaderMapper::class.java)

        val DEFAULT_TRUSTED_PACKAGES = listOf("java.lang", "java.net", "java.util", "org.springframework.util")

        /**
         * Header name for java types of other headers.
         */
        const val JSON_TYPES = "spring_json_header_types"
    }
}

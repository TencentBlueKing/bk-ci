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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.tencent.devops.common.api.annotation.ServiceInterface
import com.tencent.devops.common.api.exception.ClientException
import com.tencent.devops.common.client.ms.MicroServiceTarget
import com.tencent.devops.common.service.utils.SpringContextUtil
import feign.Feign
import feign.RequestInterceptor
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import feign.jaxrs.JAXRSContract
import feign.okhttp.OkHttpClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.consul.discovery.ConsulDiscoveryClient
import org.springframework.context.annotation.DependsOn
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

/**
 *
 * Powered By Tencent
 */
@Component
@DependsOn("springContextUtil")
class Client @Autowired constructor(
    private val consulClient: ConsulDiscoveryClient?,
    private val clientErrorDecoder: ClientErrorDecoder,
    objectMapper: ObjectMapper
) {

    companion object {
        private val logger = LoggerFactory.getLogger(Client::class.java)
        private const val readWriteTimeoutSeconds = 15L
        private const val connectTimeoutSeconds = 5L
    }

    private val beanCaches: LoadingCache<KClass<*>, *> = CacheBuilder.newBuilder()
        .maximumSize(1000).build(object : CacheLoader<KClass<*>, Any>() {
            override fun load(p0: KClass<*>): Any {
                return getImpl(p0)
            }
        })

    private val interfaces = ConcurrentHashMap<KClass<*>, String>()
    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(readWriteTimeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(readWriteTimeoutSeconds, TimeUnit.SECONDS)
        .build()

    private val feignClient = OkHttpClient(okHttpClient)
    private val jaxRsContract = JAXRSContract()
    private val jacksonDecoder = JacksonDecoder(objectMapper)
    private val jacksonEncoder = JacksonEncoder(objectMapper)

    @Value("\${spring.cloud.consul.discovery.tags:#{null}}")
    private val tag: String? = null

    @Value("\${spring.cloud.consul.discovery.service-name:#{null}}")
    private val assemblyServiceName: String? = null

    fun <T : Any> get(clz: KClass<T>): T {
        return get(clz, "")
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(clz: KClass<T>, suffix: String): T {
        return try {
            beanCaches.get(clz) as T
        } catch (ignored: Throwable) {
            getImpl(clz)
        }
    }

    fun <T : Any> getImpl(clz: KClass<T>): T {
        try {
            return SpringContextUtil.getBean(clz.java)
        } catch (ignored: Exception) {
            logger.info("[$clz]|try to proxy by feign: ${ignored.message}")
        }
        val requestInterceptor = SpringContextUtil.getBean(RequestInterceptor::class.java) // 获取为feign定义的拦截器
        return Feign.builder()
            .client(feignClient)
            .errorDecoder(clientErrorDecoder)
            .encoder(jacksonEncoder)
            .decoder(jacksonDecoder)
            .contract(jaxRsContract)
            .requestInterceptor(requestInterceptor)
            .target(MicroServiceTarget(findServiceName(clz), clz.java, consulClient!!, tag))
    }

    fun getServiceUrl(clz: KClass<*>): String {
        return MicroServiceTarget(findServiceName(clz), clz.java, consulClient!!, tag).url()
    }

    private fun findServiceName(clz: KClass<*>): String {
        // 单体结构，不分微服务的方式
        if (!assemblyServiceName.isNullOrBlank()) {
            return assemblyServiceName!!
        }
        return interfaces.getOrPut(clz) {
            val serviceInterface = AnnotationUtils.findAnnotation(clz.java, ServiceInterface::class.java)
            if (serviceInterface != null && serviceInterface.value.isNotBlank()) {
                serviceInterface.value
            } else {
                val packageName = clz.qualifiedName.toString()
                val regex = Regex("""com.tencent.devops.([a-z]+).api.([a-zA-Z]+)""")
                val matches = regex.find(packageName) ?: throw ClientException("无法根据接口\"$packageName\"分析所属的服务")
                matches.groupValues[1]
            }
        }
    }
}
/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
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
import com.netflix.client.ClientException
import com.tencent.devops.common.api.annotation.ServiceInterface
import com.tencent.devops.common.client.ms.DevopsServiceTarget
import com.tencent.devops.common.client.ms.MicroServiceTarget
import com.tencent.devops.common.client.pojo.AllProperties
import com.tencent.devops.common.service.utils.SpringContextUtil
import feign.*
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import feign.jaxrs.JAXRSContract
import feign.okhttp.OkHttpClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.consul.discovery.ConsulDiscoveryClient
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

@Component
class Client @Autowired constructor(
        private val consulClient: ConsulDiscoveryClient,
        private val clientErrorDecoder: ClientErrorDecoder,
        private val allProperties: AllProperties,
        objectMapper: ObjectMapper
) {

    private val interfaces = ConcurrentHashMap<KClass<*>, String>()

    private val okHttpClient = okhttp3.OkHttpClient.Builder()
            .connectTimeout(5L, TimeUnit.SECONDS)
            .readTimeout(60L, TimeUnit.SECONDS)
            .writeTimeout(60L, TimeUnit.SECONDS)
            .build()

    private val longRunClient = OkHttpClient(
            okhttp3.OkHttpClient.Builder()
                    .connectTimeout(10L, TimeUnit.SECONDS)
                    .readTimeout(30L, TimeUnit.MINUTES)
                    .writeTimeout(30L, TimeUnit.MINUTES)
                    .build()
    )

    private val feignClient = OkHttpClient(okHttpClient)
    private val jaxRsContract = JAXRSContract()
    private val jacksonDecoder = JacksonDecoder(objectMapper)
    private val jacksonEncoder = JacksonEncoder(objectMapper)

    @Value("\${spring.cloud.consul.discovery.tags:#{null}}")
    private val tag: String? = null

    @Value("\${service-suffix:#{null}}")
    private val suffix : String? = null

    companion object {

        const val codeccPackagePath = """com.tencent.bk.codecc.([a-z]+).api.([a-zA-Z]+)"""

        const val devopsPackagePath = """com.tencent.devops.([a-z]+).api.([a-zA-Z]+)"""
    }


    fun <T : Any> getDevopsService(clz: Class<T>): T {
        return Feign.builder()
                .client(feignClient)
                .errorDecoder(clientErrorDecoder)
                .encoder(jacksonEncoder)
                .decoder(jacksonDecoder)
                .contract(jaxRsContract)
                .options(Request.Options(10000, 30000))
                .requestInterceptor(SpringContextUtil.getBean(RequestInterceptor::class.java, "devopsRequestInterceptor"))
                .target(DevopsServiceTarget(findServiceName(clz.kotlin), clz, allProperties.devopsDevUrl
                        ?: ""))
        // 获取为feign定义的拦截器

    }

    fun <T : Any> get(clz: Class<T>): T = get(clz.kotlin)

    fun <T : Any> get(clz: KClass<T>): T {
        return Feign.builder()
                .client(feignClient)
                .errorDecoder(clientErrorDecoder)
                .encoder(jacksonEncoder)
                .decoder(jacksonDecoder)
                .contract(jaxRsContract)
                .options(Request.Options(10000, 30000))// 10秒连接 30秒收数据
                .requestInterceptor(SpringContextUtil.getBean(RequestInterceptor::class.java, "normalRequestInterceptor")) // 获取为feign定义的拦截器
                .target(MicroServiceTarget("${findServiceName(clz)}$suffix", clz.java, consulClient, tag))
    }

    fun <T : Any> getWithoutRetry(clz: KClass<T>): T {
        return Feign.builder()
                .client(longRunClient)
                .errorDecoder(clientErrorDecoder)
                .encoder(jacksonEncoder)
                .decoder(jacksonDecoder)
                .contract(jaxRsContract)
                .requestInterceptor(SpringContextUtil.getBean(RequestInterceptor::class.java, "normalRequestInterceptor")) // 获取为feign定义的拦截器
                .options(Request.Options(10 * 1000, 30 * 60 * 1000))
                .retryer(object : Retryer {
                    override fun clone(): Retryer {
                        return this
                    }

                    override fun continueOrPropagate(e: RetryableException) {
                        throw e
                    }
                })
                .target(MicroServiceTarget(findServiceName(clz), clz.java, consulClient, tag))
    }

    private fun findServiceName(clz: KClass<*>): String {
        return interfaces.getOrPut(clz) {
            val serviceInterface = AnnotationUtils.findAnnotation(clz.java, ServiceInterface::class.java)
            if (serviceInterface != null) {
                serviceInterface.value
            } else {
                val packageName = clz.qualifiedName.toString()
                val codeccRegex = Regex(codeccPackagePath)
                val devopsRegex = Regex(devopsPackagePath)
                val matches = codeccRegex.find(packageName) ?: devopsRegex.find(packageName) ?: throw ClientException("无法根据接口[$packageName]分析所属的服务")
                matches.groupValues[1]
            }
        }
    }

}

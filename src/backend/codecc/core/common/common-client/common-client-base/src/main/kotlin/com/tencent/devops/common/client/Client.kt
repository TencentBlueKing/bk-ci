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

package com.tencent.devops.common.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.annotation.ServiceInterface
import com.tencent.devops.common.client.ms.DevopsServiceTarget
import com.tencent.devops.common.client.pojo.AllProperties
import com.tencent.devops.common.client.proxy.DevopsProxy
import com.tencent.devops.common.exception.ClientException
import com.tencent.devops.common.service.utils.SpringContextUtil
import feign.Feign
import feign.Request
import feign.RequestInterceptor
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import feign.jaxrs.JAXRSContract
import feign.okhttp.OkHttpClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.AnnotationUtils
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

abstract class Client constructor(
        protected open val clientErrorDecoder: ClientErrorDecoder,
        protected open val allProperties: AllProperties,
        protected val objectMapper: ObjectMapper
) {

    @Value("\${server.prefix:codecc:#{null}}")
    private val serverPrefix: String? = null

    @Value("\${service.suffix.codecc:#{null}}")
    private val serviceSuffix: String? = null

    private val interfaces = ConcurrentHashMap<KClass<*>, String>()

    private val okHttpClient = okhttp3.OkHttpClient.Builder()
            .connectTimeout(5L, TimeUnit.SECONDS)
            .readTimeout(60L, TimeUnit.SECONDS)
            .writeTimeout(60L, TimeUnit.SECONDS)
            .build()

    protected val longRunClient = OkHttpClient(
            okhttp3.OkHttpClient.Builder()
                    .connectTimeout(10L, TimeUnit.SECONDS)
                    .readTimeout(30L, TimeUnit.MINUTES)
                    .writeTimeout(30L, TimeUnit.MINUTES)
                    .build()
    )

    protected val feignClient = OkHttpClient(okHttpClient)
    protected val jaxRsContract = JAXRSContract()
    protected val jacksonDecoder = JacksonDecoder(objectMapper)
    protected val jacksonEncoder = JacksonEncoder(objectMapper)

    @Value("\${codecc.quartz.tag:\${spring.cloud.consul.discovery.tags}}")
    private val tag: String? = null

    companion object {

        const val codeccPackagePath = """com.tencent.bk.codecc.([a-z]+).api.([a-zA-Z]+)"""

        const val devopsPackagePath = """com.tencent.devops.([a-z]+).api.([a-zA-Z]+)"""
    }

    fun <T : Any> getDevopsService(clz: Class<T>): T {
        // 获取为feign定义的拦截器
        val feignProxy = Feign.builder()
            .client(feignClient)
            .errorDecoder(clientErrorDecoder)
            .encoder(jacksonEncoder)
            .decoder(jacksonDecoder)
            .contract(jaxRsContract)
            .options(Request.Options(10000, 30000))
            .requestInterceptor(SpringContextUtil.getBean(RequestInterceptor::class.java,
                "devopsRequestInterceptor"))
            .target(DevopsServiceTarget(findServiceName(clz.kotlin,"",""),  clz, allProperties.devopsDevUrl
                ?: ""))
        val devopsProxy = DevopsProxy(feignProxy, clz)
        return clz.cast(Proxy.newProxyInstance(feignProxy.javaClass.classLoader,
            feignProxy.javaClass.interfaces, devopsProxy))
    }

    fun <T : Any> getDevopsService(clz: Class<T>, projectId: String): T {
        // 获取为feign定义的拦截器
        DevopsProxy.projectIdThreadLocal.set(projectId)
        val feignProxy = Feign.builder()
            .client(feignClient)
            .errorDecoder(clientErrorDecoder)
            .encoder(jacksonEncoder)
            .decoder(jacksonDecoder)
            .contract(jaxRsContract)
            .options(Request.Options(10000, 30000))
            .requestInterceptor(SpringContextUtil.getBean(
                RequestInterceptor::class.java, "devopsRequestInterceptor"))
            .target(DevopsServiceTarget(findServiceName(clz.kotlin,"", ""),  clz,
                allProperties.devopsDevUrl
                    ?: ""))
        val devopsProxy = DevopsProxy(feignProxy, clz)
        return clz.cast(
            Proxy.newProxyInstance(feignProxy.javaClass.classLoader, feignProxy.javaClass.interfaces, devopsProxy))
    }

    fun <T : Any> get(clz: Class<T>): T = get(clz.kotlin)

    abstract fun <T : Any> get(clz: KClass<T>): T

    abstract fun <T : Any> getNoneUrlPrefix(clz: Class<T>): T

    abstract fun <T : Any> getWithoutRetry(clz: KClass<T>): T

    protected fun findServiceName(clz: KClass<*>): String {
        return findServiceName(clz, serverPrefix, serviceSuffix)
    }

    protected fun findServiceName(clz: KClass<*>, prefix: String?, suffix: String?): String {
        val serviceName = interfaces.getOrPut(clz) {
            val serviceInterface = AnnotationUtils.findAnnotation(clz.java, ServiceInterface::class.java)
            if (serviceInterface != null) {
                serviceInterface.value
            } else {
                val packageName = clz.qualifiedName.toString()
                val codeccRegex = Regex(codeccPackagePath)
                val devopsRegex = Regex(devopsPackagePath)
                val matches = codeccRegex.find(packageName) ?: devopsRegex.find(packageName)
                ?: throw ClientException("无法根据接口[$packageName]分析所属的服务")
                matches.groupValues[1]
            }
        }
        return if (prefix.isNullOrBlank() && suffix.isNullOrBlank()) {
            serviceName
        } else if (suffix.isNullOrBlank()) {
            "$prefix$serviceName"
        } else {
            "$serviceName$suffix"
        }
    }

}

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
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.tencent.devops.common.api.annotation.ServiceInterface
import com.tencent.devops.common.api.constant.CommonMessageCode.SERVICE_COULD_NOT_BE_ANALYZED
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.client.ms.MicroServiceTarget
import com.tencent.devops.common.client.pojo.enums.GatewayType
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.KubernetesUtils
import com.tencent.devops.common.service.utils.SpringContextUtil
import feign.Contract
import feign.Feign
import feign.MethodMetadata
import feign.Request
import feign.RequestInterceptor
import feign.RetryableException
import feign.Retryer
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import feign.jaxrs.JAXRSContract
import feign.okhttp.OkHttpClient
import feign.spring.SpringContract
import java.lang.reflect.Method
import java.security.cert.CertificateException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClient
import org.springframework.context.annotation.DependsOn
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

/**
 *
 * Powered By Tencent
 */
@Suppress("UNUSED")
@Component
@DependsOn("springContextUtil")
class Client @Autowired constructor(
    private val compositeDiscoveryClient: CompositeDiscoveryClient?,
    private val clientErrorDecoder: ClientErrorDecoder,
    private val commonConfig: CommonConfig,
    private val bkTag: BkTag,
    objectMapper: ObjectMapper
) {

    companion object {
        private val logger = LoggerFactory.getLogger(Client::class.java)
        private const val readWriteTimeoutSeconds = 15L
        private const val connectTimeoutSeconds = 5L
        private const val CACHE_SIZE = 1000L
        private val longTimeOptions = Request.Options(10L, TimeUnit.SECONDS, 30L, TimeUnit.MINUTES, true)
    }

    private val beanCaches: LoadingCache<KClass<*>, *> = Caffeine.newBuilder()
        .maximumSize(CACHE_SIZE).build { key -> getImpl(key) }

    private val interfaces = ConcurrentHashMap<KClass<*>, String>()

    private val trustAllCerts: Array<TrustManager> = arrayOf(object : X509TrustManager {
        @Throws(CertificateException::class)
        override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) = Unit

        @Throws(CertificateException::class)
        override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) = Unit

        override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
            return arrayOf()
        }
    })

    private fun sslSocketFactory(): SSLSocketFactory {
        try {
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            return sslContext.socketFactory
        } catch (ignored: Exception) {
            throw RemoteServiceException(ignored.message!!)
        }
    }

    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(readWriteTimeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(readWriteTimeoutSeconds, TimeUnit.SECONDS)
        .sslSocketFactory(sslSocketFactory(), trustAllCerts[0] as X509TrustManager)
        .hostnameVerifier { _, _ -> true }
        .build()

    private val longRunClient = OkHttpClient(
        okhttp3.OkHttpClient.Builder()
            .connectTimeout(10L, TimeUnit.SECONDS)
            .readTimeout(30L, TimeUnit.MINUTES)
            .writeTimeout(30L, TimeUnit.MINUTES)
            .sslSocketFactory(sslSocketFactory(), trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    )

    private val feignClient = OkHttpClient(okHttpClient)
    private val clientContract = ClientContract()
    private val springContract = SpringContract()
    private val jacksonDecoder = JacksonDecoder(objectMapper)
    private val jacksonEncoder = JacksonEncoder(objectMapper)

    @Value("\${spring.cloud.consul.discovery.service-name:#{null}}")
    private val assemblyServiceName: String? = null

    @Value("\${service-suffix:#{null}}")
    private val serviceSuffix: String? = null

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

    fun <T : Any> getSpringMvc(clz: KClass<T>): T {
        return get(clz, "", springContract)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(clz: KClass<T>, suffix: String, contract: Contract = clientContract): T {
        return try {
            beanCaches.get(clz) as T
        } catch (ignored: Throwable) {
            getImpl(clz, contract)
        }
    }

    fun <T : Any> getWithoutRetry(clz: KClass<T>): T {
        val requestInterceptor = SpringContextUtil.getBean(RequestInterceptor::class.java) // 获取为feign定义的拦截器
        return Feign.builder()
            .client(longRunClient)
            .errorDecoder(clientErrorDecoder)
            .encoder(jacksonEncoder)
            .decoder(jacksonDecoder)
            .contract(clientContract)
            .requestInterceptor(requestInterceptor)
            .options(longTimeOptions) // 可复用常量对象不重复创建
            .retryer(WithoutRetry()) // 优化重复创建的匿名类
            .target(
                MicroServiceTarget(
                    serviceName = findServiceName(clz),
                    type = clz.java,
                    compositeDiscoveryClient = compositeDiscoveryClient!!,
                    bkTag = bkTag
                )
            )
    }

    fun <T : Any> getExternalServiceWithoutRetry(serviceName: String, clz: KClass<T>): T {
        val requestInterceptor = SpringContextUtil.getBean(RequestInterceptor::class.java) // 获取为feign定义的拦截器

        return Feign.builder()
            .client(longRunClient)
            .errorDecoder(clientErrorDecoder)
            .encoder(jacksonEncoder)
            .decoder(jacksonDecoder)
            .contract(clientContract)
            .requestInterceptor(requestInterceptor)
            .options(longTimeOptions) // 可复用常量对象不重复创建
            .retryer(WithoutRetry()) // 优化重复创建的匿名类
            .target(
                MicroServiceTarget(
                    serviceName = serviceName,
                    type = clz.java,
                    compositeDiscoveryClient = compositeDiscoveryClient!!,
                    bkTag = bkTag
                )
            )
    }

    /**
     * 通过网关访问微服务接口
     *
     */
    fun <T : Any> getGateway(clz: KClass<T>, gatewayType: GatewayType = GatewayType.IDC): T {
        // 从网关访问去掉后缀，否则会变成 /process-devops/api/service/piplines 导致访问失败
        val serviceName = findServiceName(clz).removeSuffix(serviceSuffix ?: "")
        val requestInterceptor = SpringContextUtil.getBean(RequestInterceptor::class.java) // 获取为feign定义的拦截器
        return Feign.builder()
            .client(feignClient)
            .errorDecoder(clientErrorDecoder)
            .encoder(jacksonEncoder)
            .decoder(jacksonDecoder)
            .contract(clientContract)
            .requestInterceptor(requestInterceptor)
            .target(clz.java, buildGatewayUrl(path = "/$serviceName/api", gatewayType = gatewayType))
    }

    // devnet区域的，只能直接通过ip访问
    fun <T : Any> getScm(clz: KClass<T>): T {
        // 从网关访问去掉后缀，否则会变成 /process-devops/api/service/piplines 导致访问失败
        val serviceName = findServiceName(clz).removeSuffix(serviceSuffix ?: "")
        // 获取为feign定义的拦截器
        val requestInterceptor = SpringContextUtil.getBeansWithClass(RequestInterceptor::class.java)
        return Feign.builder()
            .client(feignClient)
            .errorDecoder(clientErrorDecoder)
            .encoder(jacksonEncoder)
            .decoder(jacksonDecoder)
            .contract(clientContract)
            .requestInterceptors(requestInterceptor)
            .target(clz.java, buildGatewayUrl(path = "/$serviceName/api", gatewayType = GatewayType.IDC_PROXY))
    }

    /**
     * 支持对spring MVC注解的解析
     */
    fun <T : Any> getImpl(clz: KClass<T>, contract: Contract = clientContract): T {
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
            .contract(contract)
            .requestInterceptor(requestInterceptor)
            .retryer(HttpGetRetry()) // 优化重复创建的匿名类
            .target(
                MicroServiceTarget(
                    serviceName = findServiceName(clz),
                    type = clz.java,
                    compositeDiscoveryClient = compositeDiscoveryClient!!,
                    bkTag = bkTag
                )
            )
    }

    fun getServiceUrl(clz: KClass<*>): String {
        return MicroServiceTarget(
            serviceName = findServiceName(clz),
            type = clz.java,
            compositeDiscoveryClient = compositeDiscoveryClient!!,
            bkTag = bkTag
        ).url()
    }

    private fun findServiceName(clz: KClass<*>): String {
        // 单体结构，不分微服务的方式
        if (!assemblyServiceName.isNullOrBlank()) {
            return assemblyServiceName
        }
        val serviceName = interfaces.getOrPut(clz) {
            val serviceInterface = AnnotationUtils.findAnnotation(clz.java, ServiceInterface::class.java)
            if (serviceInterface != null && serviceInterface.value.isNotBlank()) {
                serviceInterface.value
            } else {
                val packageName = clz.qualifiedName.toString()
                val regex = Regex("""com.tencent.devops.([a-z]+).api.([a-zA-Z]+)""")
                val matches = regex.find(packageName)
                    ?: throw ErrorCodeException(
                        errorCode = SERVICE_COULD_NOT_BE_ANALYZED,
                        params = arrayOf(packageName)
                    )
                matches.groupValues[1]
            }
        }

        return if (serviceSuffix.isNullOrBlank() || KubernetesUtils.inContainer()) {
            serviceName
        } else {
            "$serviceName$serviceSuffix"
        }
    }

    private fun buildGatewayUrl(path: String, gatewayType: GatewayType = GatewayType.IDC): String {

        return if (path.startsWith("http://") || path.startsWith("https://")) {
            path
        } else {

            val gateway = when (gatewayType) {
                GatewayType.DEVNET_PROXY -> commonConfig.devopsDevnetProxyGateway!!
                GatewayType.DEVNET -> commonConfig.devopsDevnetGateway!!
                GatewayType.IDC -> commonConfig.devopsIdcGateway!!
                GatewayType.IDC_PROXY -> commonConfig.devopsIdcProxyGateway!!
                GatewayType.OSS -> commonConfig.devopsOssGateway!!
                GatewayType.OSS_PROXY -> commonConfig.devopsOssProxyGateway!!
            }
            if (gateway.startsWith("http://") || gateway.startsWith("https://")) {
                "$gateway/${path.removePrefix("/")}"
            } else {
                "http://$gateway/${path.removePrefix("/")}"
            }
        }
    }

    private class WithoutRetry : Retryer.Default() {
        override fun clone(): Retryer = this

        override fun continueOrPropagate(e: RetryableException): Unit = throw e
    }

    private class HttpGetRetry : Retryer.Default() {
        override fun clone(): Retryer = this

        override fun continueOrPropagate(e: RetryableException) {
            if (e.method() != Request.HttpMethod.GET) {
                throw e
            } else {
                super.continueOrPropagate(e)
            }
        }
    }
}

class ClientContract : JAXRSContract() {
    override fun parseAndValidateMetadata(targetType: Class<*>?, method: Method?): MethodMetadata {
        val parseAndValidateMetadata = super.parseAndValidateMetadata(targetType, method)
        parseAndValidateMetadata.template().decodeSlash(false)
        return parseAndValidateMetadata
    }
}

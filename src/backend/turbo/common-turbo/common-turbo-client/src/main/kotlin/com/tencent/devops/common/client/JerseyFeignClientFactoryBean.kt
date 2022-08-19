package com.tencent.devops.common.client

import com.tencent.devops.common.api.annotation.ServiceInterface
import com.tencent.devops.common.api.exception.ClientException
import com.tencent.devops.common.client.ms.MicroServiceTarget
import com.tencent.devops.utils.jackson.JsonUtils
import feign.Feign
import feign.Request
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import feign.jaxrs.JAXRSContract
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.consul.discovery.ConsulDiscoveryClient
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.core.annotation.AnnotationUtils
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

@Suppress("EmptyFunctionBlock", "MaxLineLength")
class JerseyFeignClientFactoryBean(
    private val type: Class<*>,
    private val tag: String
) : FactoryBean<Any>, InitializingBean, ApplicationContextAware {

    companion object {
        const val codeccPackagePath =
            """com.tencent.bk.codecc.([a-z]+).api.([a-zA-Z]+)"""
        const val devopsPackagePath =
            """com.tencent.devops.([a-z]+).api.([a-zA-Z]+)"""
    }

    @Value("\${service-suffix:#{null}}")
    private val serviceSuffix: String? = null

    private var applicationContext: ApplicationContext? = null

    private val interfaces = ConcurrentHashMap<KClass<*>, String>()

    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(5L, TimeUnit.SECONDS)
        .readTimeout(60L, TimeUnit.SECONDS)
        .writeTimeout(60L, TimeUnit.SECONDS)
        .build()

    private val feignClient = feign.okhttp.OkHttpClient(okHttpClient)
    private val jaxRsContract = JAXRSContract()
    private val jacksonDecoder = JacksonDecoder(JsonUtils.objectMapper)
    private val jacksonEncoder = JacksonEncoder(JsonUtils.objectMapper)
    private val clientErrorDecoder = ClientErrorDecoder()



    override fun getObject(): Any? {
        return Feign.builder()
            .client(feignClient)
            .errorDecoder(clientErrorDecoder)
            .encoder(jacksonEncoder)
            .decoder(jacksonDecoder)
            .contract(jaxRsContract)
            .options(Request.Options(10000L, TimeUnit.MILLISECONDS, 30000, TimeUnit.MILLISECONDS, true))
//            .requestInterceptor(applicationContext!!.getBean(RequestInterceptor::class.java, "devopsRequestInterceptor"))
            .target(MicroServiceTarget(findServiceName(type.kotlin), type, applicationContext!!.getBean(ConsulDiscoveryClient::class.java), tag))
    }

    override fun getObjectType(): Class<*> {
        return this.type
    }

    override fun afterPropertiesSet() {
    }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }

    private fun findServiceName(clz: KClass<*>): String {
        val serviceName = interfaces.getOrPut(clz) {
            val serviceInterface = AnnotationUtils.findAnnotation(clz.java, ServiceInterface::class.java)
            if (serviceInterface != null) {
                serviceInterface.value
            } else {
                val packageName = clz.qualifiedName.toString()
                val codeccRegex = Regex(codeccPackagePath)
                val devopsRegex = Regex(devopsPackagePath)
                val matches = codeccRegex.find(packageName) ?: devopsRegex.find(packageName)
                ?: throw ClientException("can not find service according to package [$packageName]")
                matches.groupValues[1]
            }
        }

        return if (serviceSuffix.isNullOrBlank()) {
            serviceName
        } else {
            "$serviceName$serviceSuffix"
        }
    }
}

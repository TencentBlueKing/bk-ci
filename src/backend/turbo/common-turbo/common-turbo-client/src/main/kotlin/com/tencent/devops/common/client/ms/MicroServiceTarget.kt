package com.tencent.devops.common.client.ms

import com.tencent.devops.common.api.exception.ClientException
import feign.Request
import feign.RequestTemplate
import org.slf4j.LoggerFactory
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.consul.discovery.ConsulDiscoveryClient
import org.springframework.cloud.consul.discovery.ConsulServiceInstance
import java.util.concurrent.ConcurrentHashMap

@Suppress("NestedBlockDepth")
class MicroServiceTarget<T> constructor(
    private val serviceName: String,
    private val type: Class<T>,
    private val consulClient: ConsulDiscoveryClient,
    private val tag: String?
) : FeignTarget<T> {

    private val logger = LoggerFactory.getLogger(MicroServiceTarget::class.java)
//    private val errorInfo =
//        MessageCodeUtil.generateResponseDataObject<String>(ERROR_SERVICE_NO_FOUND, arrayOf(serviceName))

    private val usedInstance = ConcurrentHashMap<String, ServiceInstance>()

    private fun choose(serviceName: String): ServiceInstance {

        val instances = consulClient.getInstances(serviceName)
            ?: throw ClientException("can not find[$serviceName]provider")
        if (instances.isEmpty()) {
            throw ClientException("can not find[$serviceName]provider")
        }
        val matchTagInstances = ArrayList<ServiceInstance>()

        instances.forEach { serviceInstance ->
            /*if (serviceInstance.metadata.isEmpty())
                return@forEach*/
            if (serviceInstance is ConsulServiceInstance) {
                if (serviceInstance.tags.contains(tag)) {
                    if (!usedInstance.contains(serviceInstance.url())) {
                        matchTagInstances.add(serviceInstance)
                    }
                }
            }
        }

        // 如果为空，则将之前用过的实例重新加入选择
        if (matchTagInstances.isEmpty() && usedInstance.isNotEmpty()) {
            matchTagInstances.addAll(usedInstance.values)
        }

        if (matchTagInstances.isEmpty()) {
            throw ClientException("can not find[$serviceName]provider")
        } else if (matchTagInstances.size > 1) {
            matchTagInstances.shuffle()
        }

        usedInstance[matchTagInstances[0].url()] = matchTagInstances[0]
        return matchTagInstances[0]
    }

    override fun apply(input: RequestTemplate?): Request {
        if (input!!.url().indexOf("http") != 0) {
            input.target(url())
        }
        return input.request()
    }

    override fun url() = choose(serviceName).url()

    override fun type() = type

    override fun name() = serviceName

    private fun ServiceInstance.url() = "${if (isSecure) "https" else "http"}://$host:$port/api"
}

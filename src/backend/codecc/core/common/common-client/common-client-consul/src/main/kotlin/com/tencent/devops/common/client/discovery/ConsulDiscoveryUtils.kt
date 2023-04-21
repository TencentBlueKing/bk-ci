package com.tencent.devops.common.client.discovery

import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.serviceregistry.Registration
import org.springframework.cloud.consul.discovery.ConsulServiceInstance
import org.springframework.cloud.consul.serviceregistry.ConsulRegistration

class ConsulDiscoveryUtils constructor(
    private val registration: Registration
) : DiscoveryUtils {

    override fun getInstanceTags(instance: ServiceInstance): List<String> {
        return if (instance is ConsulServiceInstance) {
            instance.tags
        } else {
            instance.metadata.values.toList()
        }
    }

    override fun getRegistrationTags(instance: ServiceInstance): List<String> {
        return if (instance is ConsulRegistration) {
            instance.service.tags
        } else {
            instance.metadata.values.toList()
        }
    }

    override fun getRegistration(): ServiceInstance {
        return registration
    }
}
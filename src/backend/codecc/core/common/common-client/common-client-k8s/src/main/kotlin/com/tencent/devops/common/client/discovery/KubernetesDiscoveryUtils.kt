package com.tencent.devops.common.client.discovery

import com.tencent.devops.common.service.Profile
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.discovery.DiscoveryClient
import java.net.InetAddress

class KubernetesDiscoveryUtils constructor(
    private val discoveryClient: DiscoveryClient,
    private val profile: Profile
): DiscoveryUtils {

    override fun getInstanceTags(instance: ServiceInstance): List<String> {
        return instance.metadata.values.toList()
    }

    override fun getRegistrationTags(instance: ServiceInstance): List<String> {
        return instance.metadata.values.toList()
    }

    override fun getRegistration(): ServiceInstance {
        val instances = discoveryClient.getInstances(profile.getApplicationName()) ?: emptyList()
        val ip = InetAddress.getLocalHost().hostAddress
        val localInstance = instances.firstOrNull { instance -> instance.host == ip }
        return localInstance!!
    }
}
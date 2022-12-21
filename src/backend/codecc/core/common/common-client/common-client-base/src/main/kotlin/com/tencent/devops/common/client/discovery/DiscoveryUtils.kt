package com.tencent.devops.common.client.discovery

import org.springframework.cloud.client.ServiceInstance

interface DiscoveryUtils {

    fun getInstanceTags(instance : ServiceInstance) : List<String>

    fun getRegistrationTags(instance : ServiceInstance) : List<String>

    fun getRegistration() : ServiceInstance
}
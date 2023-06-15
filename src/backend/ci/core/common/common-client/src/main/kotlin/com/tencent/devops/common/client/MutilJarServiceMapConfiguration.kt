package com.tencent.devops.common.client

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "mutiljar.service.map")
class MutilJarServiceMapConfiguration {
    val propertiesMap: Map<String, String> = mutableMapOf()
}

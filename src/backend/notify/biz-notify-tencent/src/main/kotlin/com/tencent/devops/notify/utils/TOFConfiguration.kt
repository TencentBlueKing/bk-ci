package com.tencent.devops.notify.utils

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "tof")
class TOFConfiguration {
    private var defaultSystem: Map<String, String>? = null
    private var optionSystems: Map<String, Map<String, String>>? = null

    fun getDefaultSystem(): Map<String, String>? {
        return defaultSystem
    }

    fun setDefaultSystem(defaultSystem: Map<String, String>) {
        this.defaultSystem = defaultSystem
    }

    fun getOptionSystems(): Map<String, Map<String, String>>? {
        return optionSystems
    }

    fun setOptionSystems(optionSystems: Map<String, Map<String, String>>) {
        this.optionSystems = optionSystems
    }

    fun getConfigurations(sysId: String?): Map<String, String>? {
        if (sysId == null || sysId === "" || !optionSystems!!.containsKey(sysId)) {
            return defaultSystem
        }
        return if (optionSystems == null) null else optionSystems!!.get(sysId)
    }
}

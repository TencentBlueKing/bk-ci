package com.tencent.devops.dispatch.utils

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.devops.dispatch.pojo.Machine
import com.vmware.vim25.mo.ServiceInstance
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.concurrent.TimeUnit

object VMUtils {

    private val cache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build<Machine/*machine ip*/, ServiceInstance>(
                    object : CacheLoader<Machine, ServiceInstance>() {
                        override fun load(machine: Machine) = _getService(machine)
                    }
            )

    fun getService(machine: Machine): ServiceInstance? {
        return cache.get(machine)
    }

    fun invalid(machine: Machine) {
        cache.invalidate(machine)
    }

    @Synchronized private fun _getService(machine: Machine): ServiceInstance? {
        try {
            return ServiceInstance(URL("https://${machine.ip}/sdk"),
                    machine.username,
                    machine.password, true)
        } catch (e: Exception) {
            logger.warn("Fail to connect to ${machine.ip} with username(${machine.username}/${machine.password})",
                    e)
        }
        return null
    }
    private val logger = LoggerFactory.getLogger(VMUtils::class.java)
}

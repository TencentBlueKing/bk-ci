package com.tencent.devops.common.quartz.simpl

import org.quartz.SchedulerException
import org.quartz.spi.InstanceIdGenerator
import java.net.InetAddress

class TurboInstanceIdGenerator : InstanceIdGenerator {
    override fun generateInstanceId(): String {
        return try {
            "Turbo${InetAddress.getLocalHost().hostAddress.replace(".", "")}"
        } catch (e: Exception) {
            throw SchedulerException("get turbo instance id fail!")
        }
    }
}

package com.tencent.devops.common.misc.utils

import com.tencent.devops.common.api.pojo.agent.NodeStatus

object BcsVmStatusUtils {
    fun parseBcsVmStatus(vmStatus: String): NodeStatus {
        return when (vmStatus.toLowerCase()) {
            "starting" -> NodeStatus.STARTING
            "running" -> NodeStatus.NORMAL
            "failed" -> NodeStatus.ABNORMAL
            "lost" -> NodeStatus.LOST
            else -> NodeStatus.UNKNOWN
        }
    }
}
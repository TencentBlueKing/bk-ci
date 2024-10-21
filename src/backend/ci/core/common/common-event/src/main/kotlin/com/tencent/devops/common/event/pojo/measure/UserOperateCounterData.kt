package com.tencent.devops.common.event.pojo.measure

import java.util.concurrent.ConcurrentHashMap

class UserOperateCounterData {
    private val userOperationCountMap: ConcurrentHashMap<String, Int> = ConcurrentHashMap()

    fun increment(projectUserOperateMetricsKey: String) {
        userOperationCountMap.merge(projectUserOperateMetricsKey, 1, Integer::sum)
    }

    fun getCount(projectUserOperateMetricsKey: String): Int {
        return userOperationCountMap.getOrDefault(projectUserOperateMetricsKey, 0)
    }

    fun reset() {
        userOperationCountMap.clear()
    }

    fun getUserOperationCountMap(): Map<String, Int> {
        return userOperationCountMap
    }
}

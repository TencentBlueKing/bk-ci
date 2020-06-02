package com.tencent.bk.codecc.quartz.jmx

import com.tencent.bk.codecc.quartz.core.CustomSchedulerManager
import org.springframework.jmx.export.annotation.ManagedAttribute
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Component

@Component
@ManagedResource(
    objectName = "com.tencent.bk.codecc.quartz:type=sharding", description = "quartz sharding info"
)
class ShardingMBean {

    @ManagedAttribute
    fun getCurrentShardNum(): Int {
        return CustomSchedulerManager.shardingStrategy.getShardingStrategy().getShardingResult()?.currentShard?.shardNum
            ?: -1
    }

    @ManagedAttribute
    fun getCurrentShardListSize(): Int {
        return CustomSchedulerManager.shardingStrategy.getShardingStrategy().getShardingResult()?.currentShardCount
            ?: -1
    }

    @ManagedAttribute
    fun getCurrentNodeNum(): Int {
        return CustomSchedulerManager.shardingStrategy.getShardingStrategy().getShardingResult()?.currentNode?.nodeNum
            ?: -1
    }

    @ManagedAttribute
    fun getCurrentNodeListSize(): Int {
        return CustomSchedulerManager.shardingStrategy.getShardingStrategy().getShardingResult()?.currentNodeCount ?: -1
    }
}
package com.tencent.bk.codecc.quartz.strategy.sharding

import com.tencent.bk.codecc.quartz.pojo.ShardingResult
import com.tencent.devops.common.client.discovery.DiscoveryUtils
import org.springframework.cloud.client.ServiceInstance

abstract class AbstractShardingStrategy {

    private var previousShardingResult: ShardingResult? = null

    abstract fun shardInstances(
        instanceList: List<ServiceInstance>,
        discoveryUtils : DiscoveryUtils
    ): ShardingResult

    fun setPreviousShardingResultIfNull(shardingResult: ShardingResult) {
        if (null == previousShardingResult) {
            synchronized(this) {
                if (null == previousShardingResult) {
                    previousShardingResult = shardingResult
                }
            }
        }
    }

    fun setPreviousShardingResult(shardingResult: ShardingResult) {
        previousShardingResult = shardingResult
    }

    fun getShardingResult(): ShardingResult? {
        return previousShardingResult
    }
}
package com.tencent.bk.codecc.quartz.strategy.sharding.impl

import com.tencent.bk.codecc.quartz.pojo.NodeInfo
import com.tencent.bk.codecc.quartz.pojo.ShardInfo
import com.tencent.bk.codecc.quartz.pojo.ShardingResult
import com.tencent.bk.codecc.quartz.strategy.sharding.AbstractShardingStrategy
import org.springframework.cloud.client.ServiceInstance

class StandaloneShardingStrategy : AbstractShardingStrategy() {

    override fun shardInstances(instanceList: List<ServiceInstance>, localInstance: ServiceInstance): ShardingResult {
        val shardList = instanceList.mapIndexed { index, serviceInstance ->
            ShardInfo(
                shardNum = index + 1,
                tag = "${serviceInstance.host}:${serviceInstance.port}",
                nodeList = listOf(
                    NodeInfo(
                        nodeNum = 1,
                        serviceId = serviceInstance.serviceId,
                        host = serviceInstance.host,
                        port = serviceInstance.port
                    )
                )
            )
        }

        val currentShard = shardList.find { it.tag == "${localInstance.host}:${localInstance.port}" }!!
        val currentShardCount = shardList.size
        val currentNode = currentShard.nodeList[0]
        val currentNodeCount = 1
        return ShardingResult(
            currentShard = currentShard,
            currentShardCount = currentShardCount,
            currentNode = currentNode,
            currentNodeCount = currentNodeCount,
            shardList = shardList
        )
    }
}
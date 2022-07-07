package com.tencent.bk.codecc.quartz.strategy.sharding.impl

import com.tencent.bk.codecc.quartz.pojo.NodeInfo
import com.tencent.bk.codecc.quartz.pojo.ShardInfo
import com.tencent.bk.codecc.quartz.pojo.ShardingResult
import com.tencent.bk.codecc.quartz.strategy.sharding.AbstractShardingStrategy
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.consul.discovery.ConsulServiceInstance
import org.springframework.cloud.consul.serviceregistry.ConsulRegistration

class AscendShardingStrategy : AbstractShardingStrategy() {

    override fun shardInstances(instanceList: List<ServiceInstance>, localInstance: ServiceInstance): ShardingResult {
        //获取分片map
        val shardMap = instanceList.groupBy { originInstances ->
            if (originInstances is ConsulServiceInstance) {
                originInstances.tags.firstOrNull() ?: ""
            } else {
                originInstances.metadata.values.firstOrNull() ?: ""
            }
        }.entries.sortedBy { it.key }
        //获取分片总数
        val currentShardCount = shardMap.size
        //处理每个分片节点信息
        val shardList = shardMap.mapIndexed { index, entry ->
            ShardInfo(
                shardNum = index + 1,
                tag = entry.key,
                nodeList = entry.value.sortedBy { "${it.host}.${it.port}" }.mapIndexed { index, serviceInstance ->
                    NodeInfo(
                        nodeNum = index + 1,
                        serviceId = serviceInstance.serviceId,
                        host = serviceInstance.host,
                        port = serviceInstance.port
                    )
                })
        }
        //获取当前分片
        val currentShard = shardList.find {
            if (localInstance is ConsulRegistration) {
                it.tag == (localInstance.service.tags.firstOrNull() ?: "")
            } else {
                it.tag == (localInstance.metadata.values.firstOrNull() ?: "")
            }
        }!!
        //获取节点数
        val currentNode =
            shardList[currentShard.shardNum - 1].nodeList.find { it.host == localInstance.host && it.port == localInstance.port }!!
        //获取节点总数
        val currentNodeCount = shardList[currentShard.shardNum - 1].nodeList.size
        return ShardingResult(currentShard, currentShardCount, currentNode, currentNodeCount, shardList)
    }
}
package com.tencent.bk.codecc.quartz.pojo

data class ShardingResultVO(
        val currentShard: ShardInfoVO,
        var currentShardCount: Int,
        var currentNode: NodeInfoVO,
        var currentNodeCount: Int,
        var shardList: List<ShardInfoVO>
)
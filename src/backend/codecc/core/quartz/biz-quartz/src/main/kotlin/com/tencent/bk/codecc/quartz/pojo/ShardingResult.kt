package com.tencent.bk.codecc.quartz.pojo

data class ShardingResult(
    var currentShard: ShardInfo,
    var currentShardCount: Int,
    var currentNode: NodeInfo,
    var currentNodeCount: Int,
    var shardList: List<ShardInfo>
)
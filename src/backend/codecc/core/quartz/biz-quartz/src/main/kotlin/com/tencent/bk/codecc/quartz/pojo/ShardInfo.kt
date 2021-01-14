package com.tencent.bk.codecc.quartz.pojo

data class ShardInfo(
    val shardNum: Int,
    val tag: String,
    var nodeList: List<NodeInfo>
)
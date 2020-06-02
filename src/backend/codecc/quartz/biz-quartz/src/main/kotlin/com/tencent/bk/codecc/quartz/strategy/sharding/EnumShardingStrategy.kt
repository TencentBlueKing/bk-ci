package com.tencent.bk.codecc.quartz.strategy.sharding

import com.tencent.bk.codecc.quartz.strategy.sharding.impl.AscendShardingStrategy
import com.tencent.bk.codecc.quartz.strategy.sharding.impl.StandaloneShardingStrategy

enum class EnumShardingStrategy(private val shardingStrategy: AbstractShardingStrategy) {
    ASCEND(AscendShardingStrategy()),
    STANDALONE(StandaloneShardingStrategy());

    fun getShardingStrategy(): AbstractShardingStrategy {
        return shardingStrategy
    }
}
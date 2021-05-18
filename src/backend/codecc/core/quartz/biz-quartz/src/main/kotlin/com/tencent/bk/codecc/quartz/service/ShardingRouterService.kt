package com.tencent.bk.codecc.quartz.service

import com.tencent.bk.codecc.quartz.model.JobInstanceEntity
import com.tencent.bk.codecc.quartz.pojo.JobInstancesChangeInfo
import com.tencent.bk.codecc.quartz.pojo.ShardingResult
import com.tencent.bk.codecc.quartz.strategy.router.EnumRouterStrategy
import com.tencent.bk.codecc.quartz.strategy.sharding.EnumShardingStrategy

interface ShardingRouterService {

    fun initSharding(enumShardingStrategy: EnumShardingStrategy = EnumShardingStrategy.STANDALONE): ShardingResult

    fun initJobInstance(shardingResult: ShardingResult,
                        jobInstances: List<JobInstanceEntity>,
                        enumRouterStrategy: EnumRouterStrategy = EnumRouterStrategy.CONSISTENT_HASH): List<JobInstanceEntity>

    fun judgeCurrentShardJob(jobInstance: JobInstanceEntity,
                             enumShardingStrategy: EnumShardingStrategy = EnumShardingStrategy.STANDALONE,
                             enumRouterStrategy: EnumRouterStrategy = EnumRouterStrategy.CONSISTENT_HASH): Boolean

    fun reShardAndReRouter(enumShardingStrategy: EnumShardingStrategy,
                           enumRouterStrategy: EnumRouterStrategy): JobInstancesChangeInfo

}
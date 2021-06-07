package com.tencent.bk.codecc.quartz.strategy.router

import com.tencent.bk.codecc.quartz.model.JobInstanceEntity
import com.tencent.bk.codecc.quartz.pojo.OperationType
import com.tencent.bk.codecc.quartz.pojo.ShardInfo
import com.tencent.bk.codecc.quartz.pojo.ShardingResult

abstract class AbstractRouterStrategy {

    abstract fun initJobInstances(
        shardingResult: ShardingResult?,
        jobInstances: List<JobInstanceEntity>
    ): List<JobInstanceEntity>

    abstract fun rescheduleJobInstances(
        shardInfo: ShardInfo, shardingResult: ShardingResult,
        operType: OperationType, currentJobInstances: List<JobInstanceEntity>,
        totalJobInstances: List<JobInstanceEntity>
    ): List<JobInstanceEntity>

    abstract fun addOrRemoveJobInstance(
        shardingResult: ShardingResult?,
        jobInstance: JobInstanceEntity
    ): Boolean
}

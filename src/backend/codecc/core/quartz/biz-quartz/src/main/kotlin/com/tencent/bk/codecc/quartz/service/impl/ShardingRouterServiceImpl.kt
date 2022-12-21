package com.tencent.bk.codecc.quartz.service.impl

import com.tencent.bk.codecc.quartz.core.CustomSchedulerManager
import com.tencent.bk.codecc.quartz.model.JobInstanceEntity
import com.tencent.bk.codecc.quartz.pojo.JobInstancesChangeInfo
import com.tencent.bk.codecc.quartz.pojo.OperationType
import com.tencent.bk.codecc.quartz.pojo.ShardingResult
import com.tencent.bk.codecc.quartz.service.JobManageService
import com.tencent.bk.codecc.quartz.service.ShardingRouterService
import com.tencent.bk.codecc.quartz.strategy.router.EnumRouterStrategy
import com.tencent.bk.codecc.quartz.strategy.sharding.EnumShardingStrategy
import com.tencent.devops.common.client.discovery.DiscoveryUtils
import com.tencent.devops.common.service.Profile
import org.quartz.Scheduler
import org.quartz.impl.matchers.GroupMatcher
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.stereotype.Service

@Service
class ShardingRouterServiceImpl @Autowired constructor(
    private val discoveryClient: DiscoveryClient,
    private val profile: Profile,
    private val scheduler : Scheduler,
    private val jobManageService: JobManageService,
    private val discoveryUtils: DiscoveryUtils
) : ShardingRouterService {

    companion object {
        private val logger = LoggerFactory.getLogger(ShardingRouterServiceImpl::class.java)
    }

    /**
     * 服务启动时初始化
     */
    override fun initSharding(enumShardingStrategy: EnumShardingStrategy): ShardingResult {
        val serviceName = profile.getApplicationName()
        //取该服务名的所有服务实例
        val instances =
            discoveryClient.getInstances(serviceName)
        //取本地服务
        logger.info("successfully get instance list and local instance!")
        //按照特定分片算法计算分片信息
        val shardingResult = enumShardingStrategy.getShardingStrategy().shardInstances(
            instances, discoveryUtils
        )
        logger.info("shard info: ${shardingResult.currentShard}, node info: ${shardingResult.currentNode}")
        //缓存分片信息
        enumShardingStrategy.getShardingStrategy().setPreviousShardingResultIfNull(shardingResult)
        return shardingResult
    }


    /**
     * job实例初始化
     */
    override fun initJobInstance(
        shardingResult: ShardingResult,
        jobInstances: List<JobInstanceEntity>,
        enumRouterStrategy: EnumRouterStrategy
    ): List<JobInstanceEntity> {
        return enumRouterStrategy.getRouterStrategy().initJobInstances(
            shardingResult,
            jobInstances
        )
    }

    /**
     * 判断新来的job是否是属于该节点的
     */
    override fun judgeCurrentShardJob(
        jobInstance: JobInstanceEntity,
        enumShardingStrategy: EnumShardingStrategy,
        enumRouterStrategy: EnumRouterStrategy
    ): Boolean {
        return enumRouterStrategy.getRouterStrategy().addOrRemoveJobInstance(
            enumShardingStrategy.getShardingStrategy().getShardingResult(),
            jobInstance
        )
    }

    /**
     * 重新分片
     */
    override fun reShardAndReRouter(
        enumShardingStrategy: EnumShardingStrategy,
        enumRouterStrategy: EnumRouterStrategy
    ): JobInstancesChangeInfo {
        val serviceName = profile.getApplicationName()
        //取该服务名的所有服务实例
        val instances =
            discoveryClient.getInstances(serviceName)
        //取本地服务
        val oldShardingResult = enumShardingStrategy.getShardingStrategy().getShardingResult()!!
        val newShardingResult = enumShardingStrategy.getShardingStrategy().shardInstances(
            instances, discoveryUtils
        )
        val jobsNeedToAdd = mutableListOf<JobInstanceEntity>()
        val jobsNeedToRemove = mutableListOf<JobInstanceEntity>()
        var shardChangeFlag = 0

        var currentJobInstances = emptyList<JobInstanceEntity>()
        var totalJobInstances = emptyList<JobInstanceEntity>()

        //先算减少的
        oldShardingResult.shardList.forEach { oldShard ->
            val newQualifiedShard = newShardingResult.shardList.find { newShard -> newShard.tag == oldShard.tag }
            if (null == newQualifiedShard) {
                if(totalJobInstances.isNullOrEmpty()){
                    totalJobInstances = jobManageService.findCachedJobs()
                }
                if(currentJobInstances.isNullOrEmpty()){
                    val currentJobList = scheduler.getJobKeys(GroupMatcher.groupEquals(CustomSchedulerManager.jobGroup)).map { it.name }
                    currentJobInstances = totalJobInstances.filter { currentJobList.contains(it.jobName) }
                }
                jobsNeedToAdd.addAll(
                    enumRouterStrategy.getRouterStrategy().rescheduleJobInstances(
                        oldShard, oldShardingResult, OperationType.REMOVE,
                        currentJobInstances, totalJobInstances
                    )
                )
                shardChangeFlag = 1
                //只要shard或节点有变化，都要重新赋值shard信息
            } else {
                if (oldShard.nodeList.size != newQualifiedShard.nodeList.size) {
                    shardChangeFlag = 1
                } else {
                    for (i in 0 until oldShard.nodeList.size) {
                        if (oldShard.nodeList[i].host != newQualifiedShard.nodeList[i].host ||
                            oldShard.nodeList[i].port != newQualifiedShard.nodeList[i].port
                        ) {
                            shardChangeFlag = 1
                            break
                        }
                    }
                }
            }
        }
        //再算增加的
        newShardingResult.shardList.forEach { newShard ->
            if (null == oldShardingResult.shardList.find { oldShard -> oldShard.tag == newShard.tag }) {
                if(totalJobInstances.isNullOrEmpty()){
                    totalJobInstances = jobManageService.findCachedJobs()
                }
                if(currentJobInstances.isNullOrEmpty()){
                    val currentJobList = scheduler.getJobKeys(GroupMatcher.groupEquals(CustomSchedulerManager.jobGroup)).map { it.name }
                    currentJobInstances = totalJobInstances.filter { currentJobList.contains(it.jobName) }
                }
                jobsNeedToRemove.addAll(
                    enumRouterStrategy.getRouterStrategy().rescheduleJobInstances(
                        newShard, oldShardingResult, OperationType.ADD,
                        currentJobInstances, totalJobInstances
                    )
                )
                shardChangeFlag = 1
            }

        }
        logger.info(
            "re-shard and re-router finish! new shard info: $newShardingResult, " +
                    "jobs to add num: ${jobsNeedToAdd.size}, jobs to remove num: ${jobsNeedToRemove.size}"
        )
        if (shardChangeFlag == 1) {
            logger.info("sharding result has changed!")
            enumShardingStrategy.getShardingStrategy().setPreviousShardingResult(newShardingResult)
        }
        return JobInstancesChangeInfo(jobsNeedToAdd, jobsNeedToRemove)
    }
}

package com.tencent.bk.codecc.quartz.core

import com.tencent.bk.codecc.quartz.model.JobCompensateEntity
import com.tencent.bk.codecc.quartz.service.JobManageService
import com.tencent.devops.common.constant.RedisKeyConstants
import com.tencent.devops.common.redis.lock.RedisLock
import org.apache.commons.lang.time.DateFormatUtils
import org.quartz.JobExecutionContext
import org.quartz.Trigger
import org.quartz.TriggerListener
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate

class ShardingListener @Autowired constructor(
    private val redisTemplate: RedisTemplate<String, String>,
    private val jobManageService: JobManageService
) : TriggerListener {

    companion object {
        private val logger = LoggerFactory.getLogger(ShardingListener::class.java)
    }

    override fun triggerFired(trigger: Trigger, context: JobExecutionContext) {
    }

    override fun getName(): String {
        return "ShardingListener"
    }

    override fun vetoJobExecution(trigger: Trigger, context: JobExecutionContext): Boolean {
        //竞争分布式锁，如果没获得锁，则不执行job
        val jobName = context.jobDetail.key.name
        //确认时间用nextScheduledTime,如果用当前的话，会因为延时而导致分布式锁不齐
        val nextFireTime = DateFormatUtils.format(context.nextFireTime, "yyyyMMddHHmmss")
        val redisLock = RedisLock(redisTemplate, "quartz:cluster:platform:$jobName:$nextFireTime", 20)
        return if (redisLock.tryLock()) {
            //保存执行情况
            logger.info("job begin, job name: $jobName, trigger scheduled fire time is: $nextFireTime")
            //todo 放入的值要注意
            redisTemplate.opsForHash<String, String>().put(
                "${RedisKeyConstants.BK_JOB_CLUSTER_RECORD}${CustomSchedulerManager.shardingStrategy.getShardingStrategy().getShardingResult()!!.currentShard.tag}",
                "${jobName}_$nextFireTime", "1"
            )
            false
        } else {
            true
        }
    }

    override fun triggerComplete(
        trigger: Trigger,
        context: JobExecutionContext,
        triggerInstructionCode: Trigger.CompletedExecutionInstruction
    ) {
        val jobName = context.jobDetail.key.name
        val nextFireTime = DateFormatUtils.format(context.nextFireTime, "yyyyMMddHHmmss")
        logger.info("job finish, job name: $jobName, trigger fire time is: $nextFireTime")
        redisTemplate.opsForHash<String, String>().delete(
            "${RedisKeyConstants.BK_JOB_CLUSTER_RECORD}${CustomSchedulerManager.shardingStrategy.getShardingStrategy().getShardingResult()!!.currentShard.tag}",
            "${jobName}_$nextFireTime"
        )
    }

    override fun triggerMisfired(trigger: Trigger) {
        logger.info("job is misfired! job name: ${trigger.jobKey.name}")
        val jobName = trigger.jobKey.name
        val nextFireTime = DateFormatUtils.format(trigger.nextFireTime, "yyyyMMddHHmmss")
        val jobCompensateEntity = JobCompensateEntity(
            jobName,
            CustomSchedulerManager.shardingStrategy.getShardingStrategy().getShardingResult()!!.currentShard.tag,
            nextFireTime,
            false
        )
        jobManageService.saveJobCompensate(jobCompensateEntity)
    }
}
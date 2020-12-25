package com.tencent.bk.codecc.quartz.job

import com.tencent.bk.codecc.quartz.core.CustomSchedulerManager
import com.tencent.bk.codecc.quartz.jmx.JobStatisticMBean
import com.tencent.bk.codecc.quartz.pojo.QuartzJobContext
import com.tencent.bk.codecc.quartz.service.impl.ShardingRouterServiceImpl
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.util.JsonUtil
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory
import org.springframework.beans.BeansException
import org.springframework.beans.factory.annotation.Autowired

class ShardingJob : Job {

    @Autowired
    private lateinit var jobStatisticMBean: JobStatisticMBean

    companion object {
        private val logger = LoggerFactory.getLogger(ShardingRouterServiceImpl::class.java)
    }

    override fun execute(context: JobExecutionContext) {
        val jobParam = context.jobDetail.jobDataMap
        val quartzJobContext = JsonUtil.mapTo(jobParam, QuartzJobContext::class.java)
        quartzJobContext.scheduledFireTime = context.scheduledFireTime
        quartzJobContext.jobName = context.trigger.jobKey.name
        quartzJobContext.shardNum =
            CustomSchedulerManager.shardingStrategy.getShardingStrategy().getShardingResult()?.currentShard?.shardNum
                ?: -1
        quartzJobContext.nodeNum =
            CustomSchedulerManager.shardingStrategy.getShardingStrategy().getShardingResult()?.currentNode?.nodeNum
                ?: -1
        var success = false
        try {
            val scheduleTask = SpringContextUtil.getBean(IScheduleTask::class.java, quartzJobContext.beanName)
            scheduleTask.executeTask(quartzJobContext)
            success = true
        } catch (e1: BeansException) {
            logger.error("get spring bean fail! bean name: ${quartzJobContext.beanName}, error message: ${e1.message}")
            return
        } catch (e2: Exception) {
            logger.error("execute job task fail! bean name: ${quartzJobContext.beanName}, error message: ${e2.message}")
            return
        } finally {
            jobStatisticMBean.executeStatistic(success)
        }
    }
}
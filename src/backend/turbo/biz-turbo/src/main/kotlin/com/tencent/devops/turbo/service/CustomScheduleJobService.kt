package com.tencent.devops.turbo.service

import com.tencent.devops.common.api.exception.TurboException
import com.tencent.devops.common.api.exception.code.SUB_CLASS_CHECK_ERROR
import com.tencent.devops.turbo.pojo.CustomScheduleJobModel
import org.quartz.CronScheduleBuilder
import org.quartz.Job
import org.quartz.JobBuilder
import org.quartz.JobDataMap
import org.quartz.JobKey
import org.quartz.Scheduler
import org.quartz.TriggerBuilder
import org.quartz.TriggerKey
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CustomScheduleJobService @Autowired constructor(
    private val scheduler: Scheduler
) {
    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
        private const val triggerGroup = "customTriggerGroup"
        private const val jobGroup = "customJobGroup"
    }

    /**
     * 新增自定义计划任务
     */
    fun customScheduledJobAdd(customScheduleJobModel: CustomScheduleJobModel): Boolean {
        logger.info("$customScheduleJobModel")
        val triggerName = customScheduleJobModel.triggerName
        val cronExpression = customScheduleJobModel.cronExpression

        var trigger = TriggerBuilder.newTrigger().withIdentity(
            triggerName, triggerGroup
        ).withSchedule(
            // 以当前时间为触发频率立刻触发一次执行, 然后按照cron频率依次执行
            CronScheduleBuilder.cronSchedule(cronExpression).withMisfireHandlingInstructionFireAndProceed()
        ).build()

        val jobKey = JobKey.jobKey(customScheduleJobModel.jobName, jobGroup)

        // 执行计划任务的类名
        val jobClassName = Class.forName(customScheduleJobModel.jobClassName)

        // 判断jobClassName类是否继承与Job
        if (!Job::class.java.isAssignableFrom(jobClassName)) {
            throw TurboException(SUB_CLASS_CHECK_ERROR, "ERROR:$jobClassName is not assignable from Job!")
        }
        val className = jobClassName.asSubclass(Job::class.java)

        val jobBuilder = JobBuilder.newJob(className).withIdentity(jobKey)
        // 检查是否有Job入参参数
        if (!customScheduleJobModel.jobDataMap.isNullOrEmpty()) {
            jobBuilder.usingJobData(JobDataMap(customScheduleJobModel.jobDataMap))
        }
        val jobDetail = jobBuilder.build()

        val triggerKey = TriggerKey.triggerKey(triggerName, triggerGroup)
        try {
            scheduler.scheduleJob(jobDetail, trigger)
        } catch (e: Exception) {
            logger.warn("schedule $triggerName job fail with scheduler exception!")
            trigger = trigger.triggerBuilder.withIdentity(triggerKey).withSchedule(
                CronScheduleBuilder.cronSchedule(cronExpression)
            ).build()
            scheduler.rescheduleJob(triggerKey, trigger)
        } catch (e: Exception) {
            logger.error("schedule $triggerName job fail with exception!")
            scheduler.deleteJob(jobKey)
            throw TurboException(errorMessage = "add schedule fail!")
        }
        return true
    }

    /**
     * 触发job立即执行
     */
    fun trigger(jobName: String): String? {
        return try {
            val jobKey = JobKey.jobKey(jobName, jobGroup)
            scheduler.triggerJob(jobKey)
            "trigger job [] successfully"
        } catch (e: Exception) {
            e.message
        }
    }
}

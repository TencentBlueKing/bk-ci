/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.dispatch.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.dao.JobQuotaProjectRunTimeDao
import com.tencent.devops.dispatch.dao.RunningJobsDao
import com.tencent.devops.dispatch.exception.ErrorCodeEnum
import com.tencent.devops.dispatch.pojo.JobQuotaHistory
import com.tencent.devops.dispatch.pojo.JobQuotaStatus
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.dispatch.utils.JobQuotaProjectLock
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.engine.common.VMUtils
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class JobQuotaBusinessService @Autowired constructor(
    private val jobQuotaManagerService: JobQuotaManagerService,
    private val runningJobsDao: RunningJobsDao,
    private val jobQuotaProjectRunTimeDao: JobQuotaProjectRunTimeDao,
    private val dslContext: DSLContext,
    private val client: Client,
    private val jobQuotaInterface: JobQuotaInterface,
    private val buildLogPrinter: BuildLogPrinter,
    private val redisOperation: RedisOperation
) {

    /**
     * job启动时检查配额并记录
     */
    fun checkAndAddRunningJob(
        projectId: String,
        vmType: JobQuotaVmType,
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        containerId: String,
        containerHashId: String?
    ): Boolean {
        LOG.info("$projectId|$buildId|$vmSeqId|$executeCount|${vmType.name} >>> start check job quota.")

        var result: Boolean
        val jobQuotaProjectLock = JobQuotaProjectLock(redisOperation, projectId)
        try {
            jobQuotaProjectLock.lock()
            result = checkJobQuotaBase(
                projectId = projectId,
                buildId = buildId,
                vmSeqId = vmSeqId,
                containerId = containerId,
                containerHashId = containerHashId,
                executeCount = executeCount,
                vmType = vmType
            )

            // 如果配额没有超限，则记录一条running job
            if (result) {
                LOG.info("$projectId|$buildId|$vmSeqId|$executeCount|${vmType.name} >>> start add running job.")
                runningJobsDao.insert(dslContext, projectId, vmType, buildId, vmSeqId, executeCount)
            }
        } finally {
            jobQuotaProjectLock.unlock()
        }

        checkSystemWarn(vmType)

        return result
    }

    /**
     * job或agent结束时删除，并记录时间
     */
    fun deleteRunningJob(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        executeCount: Int
    ) {
        LOG.info("$projectId|$buildId|$vmSeqId|$executeCount >>> update agent startTime.")
        jobAgentFinish(projectId, pipelineId, buildId, vmSeqId, executeCount)
    }

    /**
     * agent成功启动时更新
     */
    fun updateAgentStartTime(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        executeCount: Int
    ) {
        LOG.info("$projectId|$buildId|$vmSeqId|$executeCount >>> update agent startTime.")
        runningJobsDao.updateAgentStartTime(dslContext, buildId, vmSeqId, executeCount)
    }

    /**
     * agent结束时更新
     */
    fun updateRunningTime(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        executeCount: Int
    ) {
        LOG.info("$projectId|$buildId|$vmSeqId|$executeCount >>> update agent running time.")
        jobAgentFinish(projectId, pipelineId, buildId, vmSeqId, executeCount)
    }

    private fun getProjectRunningJobStatus(projectId: String, vmType: JobQuotaVmType): JobQuotaStatus {
        val jobQuota = jobQuotaManagerService.getProjectQuota(projectId, vmType)
        val runningJobCount = runningJobsDao.getProjectRunningJobCount(dslContext, projectId, vmType).toLong()
        val threshold = jobQuotaManagerService.getSystemQuota(vmType)
        val runningJobTime = getProjectRunningJobTime(projectId, vmType)

        return JobQuotaStatus(
            jobQuota = jobQuota.runningJobMax,
            runningJobCount = runningJobCount,
            jobThreshold = threshold.projectRunningJobThreshold,
            timeQuota = jobQuota.runningTimeProjectMax.toLong(),
            runningJobTime = runningJobTime,
            timeThreshold = threshold.projectRunningTimeThreshold
        )
    }

    /**
     * 获取项目当月所有JOB已运行的时间，返回已运行的时间
     */
    private fun getProjectRunningJobTime(projectId: String, vmType: JobQuotaVmType): Long {
        if (vmType == JobQuotaVmType.ALL) {
            // 所有运行中的耗时
            var runningTotalTime = 0L
            JobQuotaVmType.values().filter { it != JobQuotaVmType.ALL }.forEach { type ->
                val runningJobs = runningJobsDao.getProjectRunningJobs(dslContext, projectId, type)
                runningJobs.filter { it?.agentStartTime != null }.forEach {
                    val duration: Duration = Duration.between(it!!.agentStartTime, LocalDateTime.now())
                    runningTotalTime += duration.toMillis()
                }
            }

            // 所有已经结束的耗时
            val finishRunJobTime = getRedisStringSerializerOperation().hget(getProjectMonthRunningTimeKey(),
                getProjectRunningTimeKey(projectId))
            runningTotalTime += (finishRunJobTime ?: "0").toLong()

            return runningTotalTime
        } else {
            // 运行中的耗时
            val runningJobs = runningJobsDao.getProjectRunningJobs(dslContext, projectId, vmType)
            var runningTotalTime = 0L
            runningJobs.filter { it?.agentStartTime != null }.forEach {
                val duration: Duration = Duration.between(it!!.agentStartTime, LocalDateTime.now())
                runningTotalTime += duration.toMillis()
            }

            // 所有已经结束的耗时
            val finishRunJobTime = getRedisStringSerializerOperation().hget(
                getProjectMonthRunningTimeKey(),
                getProjectVmTypeRunningTimeKey(projectId, vmType)
            )
            runningTotalTime += (finishRunJobTime ?: "0").toLong()

            return runningTotalTime
        }
    }

    private fun checkJobQuotaBase(
        projectId: String,
        buildId: String,
        vmSeqId: String,
        containerId: String,
        containerHashId: String?,
        executeCount: Int?,
        vmType: JobQuotaVmType
    ): Boolean {
        val jobStatus = getProjectRunningJobStatus(projectId, vmType)

        with(jobStatus) {
            if (runningJobCount >= jobQuota) {
                buildLogPrinter.addYellowLine(
                    buildId = buildId,
                    message = ErrorCodeEnum.JOB_NUM_REACHED_MAX_QUOTA.getErrorMessage(
                        params = arrayOf(vmType.displayName, "$runningJobCount", "$jobQuota"),
                        language = I18nUtil.getDefaultLocaleLanguage()
                    ),
                    tag = VMUtils.genStartVMTaskId(containerId),
                    jobId = containerHashId,
                    executeCount = executeCount ?: 1
                )
                return false
            }

            if (runningJobCount * 100 / jobQuota >= jobThreshold) {
                buildLogPrinter.addYellowLine(
                    buildId = buildId,
                    message = ErrorCodeEnum.JOB_NUM_EXCEED_ALARM_THRESHOLD.getErrorMessage(
                        params = arrayOf(
                            vmType.displayName,
                            "$runningJobCount",
                            "$jobQuota",
                            normalizePercentage(jobThreshold.toDouble()),
                            normalizePercentage(runningJobCount * 100.0 / jobQuota)
                        ),
                        language = I18nUtil.getDefaultLocaleLanguage()
                    ),
                    tag = VMUtils.genStartVMTaskId(containerId),
                    jobId = containerHashId,
                    executeCount = executeCount ?: 1
                )
            }

            if (runningJobTime >= timeQuota * 60 * 60) {
/*                buildLogPrinter.addRedLine(
                    buildId = buildId,
                    message = "当前项目下本月已执行的【${vmType.displayName}】JOB时间达到配额最大值，已执行JOB时间：" +
                            "${String.format("%.2f", runningJobTime / 60 / 60)}小时, 配额: ${timeQuota}小时",
                    tag = VMUtils.genStartVMTaskId(containerId),
                    jobId = containerHashId,
                    executeCount = executeCount ?: 1
                )*/
                LOG.info("$projectId|$buildId|$vmSeqId|$executeCount|${vmType.name} running jobTime reach project " +
                        "timeLimits, now running jobTime: $runningJobTime, timeQuota: ${timeQuota * 60 * 60}")
            }

            if ((runningJobTime * 100) / (timeQuota * 60 * 60) >= timeThreshold) {
/*                buildLogPrinter.addYellowLine(
                    buildId = buildId,
                    message = "前项目下本月已执行的【${vmType.displayName}】JOB时间已经超过告警阈值，已执行JOB时间：" +
                            "${String.format("%.2f", runningJobTime / 60 / 60)}小时, 配额: ${timeQuota}小时，" +
                            "告警阈值：${normalizePercentage(timeThreshold.toDouble())}%，当前已经使用：" +
                            "${normalizePercentage((runningJobTime * 100.0) / (timeQuota * 60 * 60))}%",
                    tag = VMUtils.genStartVMTaskId(containerId),
                    jobId = containerHashId,
                    executeCount = executeCount ?: 1
                )*/
                LOG.info("$projectId|$buildId|$vmSeqId|$executeCount|${vmType.name} running jobTime reach project " +
                        "timeThreshold, now running jobTime: $runningJobTime, " +
                        "timeQuota: ${timeQuota * 60 * 60}, timeThreshold: $timeThreshold")
            }

            return true
        }
    }

    private fun jobAgentFinish(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        executeCount: Int
    ) {
        val redisLock = RedisLock(
            redisOperation = getRedisStringSerializerOperation(),
            lockKey = "$JOB_END_LOCK_KEY$buildId-$vmSeqId-$executeCount",
            expiredTimeInSeconds = 60L
        )
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                val runningJobsRecord = runningJobsDao.getAgentRunningJobs(dslContext, buildId, vmSeqId, executeCount)
                if (runningJobsRecord != null && runningJobsRecord.agentStartTime != null) {
                    val duration: Duration = Duration.between(runningJobsRecord!!.agentStartTime, LocalDateTime.now())

                    // 保存构建时间，单位秒
                    incProjectJobRunningTime(
                        projectId = projectId,
                        vmType = JobQuotaVmType.parse(runningJobsRecord.vmType),
                        time = duration.toMillis() / 1000
                    )
                    LOG.info("$projectId|$buildId|$vmSeqId|${JobQuotaVmType.parse(runningJobsRecord.vmType)} >> " +
                            "Finish time: increase ${duration.toMillis() / 1000} seconds. >>>")

                    // 保存构建记录
                    jobQuotaInterface.saveJobQuotaHistory(
                        JobQuotaHistory(
                            projectId = projectId,
                            pipelineId = pipelineId,
                            buildId = buildId,
                            vmSeqId = vmSeqId,
                            executeCount = executeCount,
                            vmType = runningJobsRecord.vmType,
                            createTime = runningJobsRecord.createdTime.format(dateTimeFormatter),
                            agentStartTime = runningJobsRecord.agentStartTime.format(dateTimeFormatter),
                            agentFinishTime = LocalDateTime.now().format(dateTimeFormatter)
                        ))
                }
            } else {
                LOG.info("$projectId|$buildId|$vmSeqId|$executeCount >> DeleteRunningJob get lock failed, not run>>>")
            }
        } catch (e: Throwable) {
            LOG.error("$projectId|$buildId|$vmSeqId|$executeCount >> Job agent finish exception:", e)
        } finally {
            try {
                runningJobsDao.delete(dslContext, buildId, vmSeqId, executeCount)
            } catch (e: Throwable) {
                LOG.error("$projectId|$buildId|$vmSeqId|$executeCount >> DeleteRunningJob exception:", e)
            }
            redisLock.unlock()
        }
    }

    private fun checkSystemWarn(vmType: JobQuotaVmType) {
        try {
            val jobQuota = jobQuotaManagerService.getSystemQuota(vmType)
            val runningJobCount = runningJobsDao.getSystemRunningJobCount(dslContext, vmType).toLong()

            val runningJobMaxSystem = jobQuota.runningJobMaxSystem
            val systemRunningJobThreshold = jobQuota.systemRunningJobThreshold
            val jobMaxLock = getRedisStringSerializerOperation().get(WARN_TIME_SYSTEM_JOB_MAX_LOCK_KEY)
            if (jobMaxLock != null) {
                if (runningJobCount < runningJobMaxSystem) {
                    getRedisStringSerializerOperation().delete(WARN_TIME_SYSTEM_JOB_MAX_LOCK_KEY)
                }
                return
            } else {
                if (runningJobCount >= runningJobMaxSystem) {
                    getRedisStringSerializerOperation().set(
                        key = WARN_TIME_SYSTEM_JOB_MAX_LOCK_KEY,
                        value = WARN_TIME_LOCK_VALUE,
                        expiredInSecond = 86400
                    )
                    LOG.info("System running job count reach max, running jobs: $runningJobCount, " +
                            "quota: $runningJobMaxSystem")
                    return
                }
            }

            val thresholdLock = getRedisStringSerializerOperation().get(WARN_TIME_SYSTEM_THRESHOLD_LOCK_KEY)
            if (thresholdLock != null) {
                if (runningJobCount * 100 / runningJobMaxSystem < systemRunningJobThreshold) {
                    getRedisStringSerializerOperation().delete(WARN_TIME_SYSTEM_THRESHOLD_LOCK_KEY)
                }
                return
            } else {
                if (runningJobCount * 100 / runningJobMaxSystem >= systemRunningJobThreshold) {
                    getRedisStringSerializerOperation().set(
                        key = WARN_TIME_SYSTEM_THRESHOLD_LOCK_KEY,
                        value = WARN_TIME_LOCK_VALUE,
                        expiredInSecond = 86400
                    )
                    LOG.info("System running job count reach threshold: $runningJobCount, " +
                            "quota: $runningJobMaxSystem, threshold: $systemRunningJobThreshold send alert.")
                    return
                }
            }
        } catch (e: Exception) {
            LOG.error("Check System warning error.", e)
        }
    }

    private fun normalizePercentage(value: Double): String {
        return when {
            value >= 100.0 -> {
                "100.00"
            }
            value <= 0 -> {
                "0.00"
            }
            else -> {
                String.format("%.2f", value)
            }
        }
    }

    /**
     * 清理超过7天的job记录，防止一直占用
     */
    @Scheduled(cron = "0 0 1 * * ?")
    fun clearTimeOutJobRecord() {
        LOG.info("start to clear timeout job record")
        val redisLock = RedisLock(getRedisStringSerializerOperation(), TIMER_OUT_LOCK_KEY, 60L)
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                LOG.info("<<< Clear Pipeline Quota Start >>>")
                doClear()
            } else {
                LOG.info("<<< Clear Pipeline Quota Job Has Running, Do Not Start>>>")
            }
        } catch (e: Throwable) {
            LOG.error("Clear pipeline quota exception:", e)
        } finally {
            redisLock.unlock()
        }
    }

    fun restoreProjectJobTime(projectId: String?, vmType: JobQuotaVmType) {
        if (projectId == null && vmType != JobQuotaVmType.ALL) {
            // 直接删除当月的hash主key
            getRedisStringSerializerOperation().delete(getProjectMonthRunningTimeKey())
            return
        }
        if (projectId != null && vmType != JobQuotaVmType.ALL) { // restore project with vmType
            restoreWithVmType(projectId, vmType)
            return
        }
    }

    private fun restoreWithVmType(project: String, vmType: JobQuotaVmType) {
        val time = getRedisStringSerializerOperation().hget(
            getProjectMonthRunningTimeKey(),
            getProjectVmTypeRunningTimeKey(project, vmType)) ?: "0"
        val totalTime = getRedisStringSerializerOperation().hget(
            getProjectMonthRunningTimeKey(),
            getProjectRunningTimeKey(project)) ?: "0"
        val reduiceTime = (totalTime.toLong() - time.toLong())
        getRedisStringSerializerOperation().hset(
            key = getProjectMonthRunningTimeKey(),
            hashKey = getProjectRunningTimeKey(project),
            values = if (reduiceTime < 0) {
                "0"
            } else {
                reduiceTime.toString()
            }
        )
        getRedisStringSerializerOperation().hset(
            key = getProjectMonthRunningTimeKey(),
            hashKey = getProjectVmTypeRunningTimeKey(project, vmType),
            values = "0"
        )
    }

    /**
     * 每月1号凌晨0点删除上个月的redis key
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    fun restoreTimeMonthly() {
        LOG.info("start to clear time monthly")
        val redisLock = RedisLock(getRedisStringSerializerOperation(), TIMER_RESTORE_LOCK_KEY, 60L)
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                LOG.info("<<< Restore time monthly Start >>>")
                val lastMonth = LocalDateTime.now().minusMonths(1).month.name
                getRedisStringSerializerOperation().delete(getProjectMonthRunningTimeKey(lastMonth))
            } else {
                LOG.info("<<< Restore time monthly Has Running, Do Not Start>>>")
            }
        } catch (e: Throwable) {
            LOG.error("Restore time monthly exception:", e)
        } finally {
            redisLock.unlock()
        }
    }

    private fun doClear() {
        val timeoutJobs = runningJobsDao.getTimeoutRunningJobs(dslContext, TIMEOUT_DAYS)
        if (timeoutJobs.isNotEmpty) {
            timeoutJobs.filterNotNull().forEach {
                LOG.info("delete timeout running job: ${it.projectId}|${it.buildId}|${it.vmSeqId}")
            }
        }
        runningJobsDao.clearTimeoutRunningJobs(dslContext, TIMEOUT_DAYS)
        LOG.info("finish to clear timeout jobs, total:${timeoutJobs.size}")

        LOG.info("Check pipeline running.")
        val runningJobs = runningJobsDao.getTimeoutRunningJobs(dslContext, CHECK_RUNNING_DAYS)
        if (runningJobs.isNullOrEmpty()) {
            return
        }

        try {
            runningJobs.filterNotNull().forEach {
                val isRunning = client.get(ServicePipelineResource::class).isRunning(projectId = it.projectId,
                    buildId = it.buildId,
                    channelCode = ChannelCode.BS).data
                    ?: false
                if (!isRunning) {
                    runningJobsDao.delete(
                        dslContext = dslContext,
                        buildId = it.buildId,
                        vmSeqId = it.vmSeqId,
                        executeCount = it.executeCount
                    )
                    LOG.info("${it.buildId}|${it.vmSeqId} Pipeline not running, " +
                            "but runningJob history not deleted.")
                }
            }
        } catch (e: Throwable) {
            LOG.error("Check pipeline running failed, msg: ${e.message}")
        }
    }

    private fun incProjectJobRunningTime(projectId: String, vmType: JobQuotaVmType?, time: Long) {
        if (vmType == null) {
            LOG.warn("incProjectJobRunningTime, vmType is null. projectId: $projectId")
            return
        }

        getRedisStringSerializerOperation().hIncrBy(
            key = getProjectMonthRunningTimeKey(),
            hashKey = getProjectVmTypeRunningTimeKey(projectId, vmType),
            delta = time
        )
        getRedisStringSerializerOperation().hIncrBy(
            key = getProjectMonthRunningTimeKey(),
            hashKey = getProjectRunningTimeKey(projectId),
            delta = time
        )
    }

    private fun getProjectMonthRunningTimeKey(month: String? = null): String {
        val currentMonth = month ?: LocalDateTime.now().month.name
        return "$PROJECT_RUNNING_TIME_KEY_PREFIX$currentMonth"
    }

    private fun getProjectVmTypeRunningTimeKey(projectId: String, vmType: JobQuotaVmType): String {
        return "$PROJECT_RUNNING_TIME_KEY_PREFIX${projectId}_${vmType.name}"
    }

    private fun getProjectRunningTimeKey(projectId: String): String {
        return "$PROJECT_RUNNING_TIME_KEY_PREFIX$projectId"
    }

    fun statistics(limit: Int?, offset: Int?): Map<String, Any> {
        val result = mutableMapOf<String, List<ProjectVmTypeTime>>()
        JobQuotaVmType.values().filter { it != JobQuotaVmType.ALL }.forEach { type ->
            val records = jobQuotaProjectRunTimeDao.listByType(dslContext, type, limit ?: 100, offset ?: 0)
                .filterNotNull().map { ProjectVmTypeTime(it.projectId, JobQuotaVmType.parse(it.vmType), it.runTime) }
            result[type.displayName] = records
        }
        return result
    }

    private fun getRedisStringSerializerOperation(): RedisOperation {
        return SpringContextUtil.getBean(RedisOperation::class.java, "redisStringHashOperation")
    }

    companion object {
        private const val TIMER_OUT_LOCK_KEY = "job_quota_business_time_out_lock"
        private const val TIMER_RESTORE_LOCK_KEY = "job_quota_business_time_restore_lock"
        private const val TIMER_COUNT_TIME_LOCK_KEY = "job_quota_project_run_time_count_lock"
        private const val JOB_END_LOCK_KEY = "job_quota_business_redis_job_end_lock_"
        private const val PROJECT_RUNNING_TIME_KEY_PREFIX = "project_running_time_key_" // 项目当月已运行时间前缀
        // 系统当月已运行JOB数量KEY, 告警使用
        private const val WARN_TIME_SYSTEM_JOB_MAX_LOCK_KEY = "job_quota_warning_system_max_lock_key"
        // 项目当月已运行JOB数量告警前缀
        private const val WARN_TIME_SYSTEM_THRESHOLD_LOCK_KEY = "job_quota_warning_system_threshold_lock_key"
        // 系统当月已运行JOB数量阈值KEY，告警使用
        private const val WARN_TIME_PROJECT_JOB_MAX_LOCK_KEY_PREFIX = "job_quota_warning_project_max_lock_key_"
        private const val WARN_TIME_PROJECT_JOB_THRESHOLD_LOCK_KEY_PREFIX =
            "job_quota_warning_project_threshold_lock_key_" // 项目当月已运行JOB数量阈值告警前缀
        private const val WARN_TIME_PROJECT_TIME_MAX_LOCK_KEY_PREFIX =
            "time_quota_warning_project_max_lock_key_" // 项目当月已运行时间告警前缀
        private const val WARN_TIME_PROJECT_TIME_THRESHOLD_LOCK_KEY_PREFIX =
            "time_quota_warning_project_threshold_lock_key_" // 项目当月已运行时间阈值告警前缀
        private const val WARN_TIME_LOCK_VALUE = "job_quota_warning_lock_value" // VALUE值，标志位
        private const val TIMEOUT_DAYS = 7L
        private const val CHECK_RUNNING_DAYS = 1L

        private val LOG = LoggerFactory.getLogger(JobQuotaBusinessService::class.java)
        private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ProjectVmTypeTime(
        val projectId: String,
        val vmType: JobQuotaVmType?,
        val runningTime: Long
    ) : Comparable<ProjectVmTypeTime> {
        override fun compareTo(other: ProjectVmTypeTime): Int {
            return other.runningTime.compareTo(this.runningTime)
        }
    }
}

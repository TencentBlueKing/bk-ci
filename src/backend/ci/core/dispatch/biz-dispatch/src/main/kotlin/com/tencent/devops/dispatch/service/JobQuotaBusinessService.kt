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
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.Profile
import com.tencent.devops.dispatch.dao.JobQuotaProjectRunTimeDao
import com.tencent.devops.dispatch.dao.RunningJobsDao
import com.tencent.devops.dispatch.pojo.JobQuotaStatus
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import com.tencent.devops.notify.pojo.RtxNotifyMessage
import com.tencent.devops.process.api.service.ServicePipelineResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

@Service
@Suppress("ALL")
class JobQuotaBusinessService @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val jobQuotaManagerService: JobQuotaManagerService,
    private val runningJobsDao: RunningJobsDao,
    private val jobQuotaProjectRunTimeDao: JobQuotaProjectRunTimeDao,
    private val dslContext: DSLContext,
    private val client: Client,
    private val profile: Profile
) {
/*    @Value("\${dispatch.jobQuota.systemAlertReceiver:#{null}}")
    private val systemAlertReceiver: String? = null

    @Value("\${dispatch.jobQuota.enable}")
    private val jobQuotaEnable: Boolean = false*/

    /**
     * job启动时记录
     */
    fun insertRunningJob(projectId: String, vmType: JobQuotaVmType, buildId: String, vmSeqId: String) {
        runningJobsDao.insert(dslContext, projectId, vmType, buildId, vmSeqId)
        redisOperation.sadd(QUOTA_PROJECT_ALL_KEY, projectId) // 所有项目集合
        // checkWarning(projectId, vmType)
    }

    /**
     * job或agent结束时删除，并记录时间
     * vmSeqId可能为空
     */
    fun deleteRunningJob(projectId: String, buildId: String, vmSeqId: String?) {
        jobAgentFinish(projectId, buildId, vmSeqId)
    }

    /**
     * agent成功启动时更新
     */
    fun updateAgentStartTime(projectId: String, buildId: String, vmSeqId: String) {
        runningJobsDao.updateAgentStartTime(dslContext, projectId, buildId, vmSeqId)
    }

    /**
     * agent结束时更新
     */
    fun updateRunningTime(projectId: String, buildId: String, vmSeqId: String) {
        jobAgentFinish(projectId, buildId, vmSeqId)
    }

/*    *//**
     * 获取项目运行中的job数量，返回运行的JOB数量
     *//*
    fun getProjectRunningJobCount(projectId: String, vmType: JobQuotaVmType): Long {
        return runningJobsDao.getProjectRunningJobCount(dslContext, projectId, vmType).toLong()
    }

    *//**
     * 获取系统运行中的job数量（非gitci），返回运行的JOB数量
     *//*
    fun getSystemRunningJobCount(vmType: JobQuotaVmType): Long {
        return runningJobsDao.getSystemRunningJobCount(dslContext, vmType).toLong()
    }

    *//**
     * 获取系统运行中的job数量（gitci），返回运行的JOB数量
     *//*
    fun getSystemGitCiRunningJobCount(vmType: JobQuotaVmType): Long {
        return runningJobsDao.getSystemGitCiRunningJobCount(dslContext, vmType).toLong()
    }*/

    /**
     * 获取项目当月所有JOB已运行的时间，返回已运行的时间
     */
    fun getProjectRunningJobTime(projectId: String, vmType: JobQuotaVmType): Long {
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
            val finishRunJobTime = redisOperation.hget(getProjectMonthRunningTimeKey(), getProjectRunningTimeKey(projectId))
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
            val finishRunJobTime = redisOperation.hget(
                getProjectMonthRunningTimeKey(),
                getProjectVmTypeRunningTimeKey(projectId, vmType)
            )
            runningTotalTime += (finishRunJobTime ?: "0").toLong()

            return runningTotalTime
        }
    }

    fun getProjectRunningJobStatus(projectId: String, vmType: JobQuotaVmType): JobQuotaStatus {
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

/*    fun checkWarning(projectId: String, vmType: JobQuotaVmType) {
        try {
            checkSystemWarn(vmType)
            checkProjectWarn(projectId, vmType)
        } catch (e: Throwable) {
            LOG.error("Send alert exception:", e)
        }
    }*/

    private fun jobAgentFinish(projectId: String, buildId: String, vmSeqId: String?) {
        val redisLock = RedisLock(redisOperation, JOB_END_LOCK_KEY + projectId, 60L)
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                val runningJobs = runningJobsDao.getAgentRunningJobs(dslContext, projectId, buildId, vmSeqId)
                runningJobs.filter { it?.agentStartTime != null && it.vmType != null }.forEach {
                    val duration: Duration = Duration.between(it!!.agentStartTime, LocalDateTime.now())

                    // 保存构建时间，单位秒
                    incProjectJobRunningTime(projectId, JobQuotaVmType.parse(it.vmType), duration.toMillis() / 1000)
                    LOG.info("$projectId|$buildId|$vmSeqId|${JobQuotaVmType.parse(it.vmType)} >> Finish time: " +
                            "increase ${duration.toHours()} hours. >>>")
                }
            } else {
                LOG.info("$projectId|$buildId|$vmSeqId >> DeleteRunningJob get lock failed, not run>>>")
            }
        } catch (e: Throwable) {
            LOG.error("$projectId|$buildId|$vmSeqId >> Job agent finish exception:", e)
        } finally {
            try {
                runningJobsDao.delete(dslContext, projectId, buildId, vmSeqId)
            } catch (e: Throwable) {
                LOG.error("$projectId|$buildId|$vmSeqId >> DeleteRunningJob exception:", e)
            }
            redisLock.unlock()
        }
    }

/*    private fun checkJobQuotaImpl(
        vmType: JobQuotaVmType,
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        containerId: String,
        containerHashId: String?,
        executeCount: Int?,
        buildLogPrinter: BuildLogPrinter
    ): Boolean {
        val jobStatus = getProjectRunningJobStatus(projectId, vmType)
        // 判断运行中JOB数量是否超出配额
        with(jobStatus) {
            if (runningJobCount >= jobQuota) {
                LOG.warn("checkJobQuota|count:$runningJobCount|quota:$jobQuota|stop it.($pipelineId|$buildId|$vmSeqId)")
                buildLogPrinter.addRedLine(
                    buildId = buildId,
                    message = "当前项目下正在执行的【${vmType.displayName}】JOB数量已经达到配额最大值，" +
                        "正在执行JOB数量：$runningJobCount, 配额: $jobQuota",
                    tag = VMUtils.genStartVMTaskId(containerId),
                    jobId = containerHashId,
                    executeCount = executeCount ?: 1
                )
                return !jobQuotaEnable
            }

            if (runningJobCount * 100 / jobQuota >= jobThreshold) {
                buildLogPrinter.addYellowLine(
                    buildId = buildId,
                    message = "当前项目下正在执行的【${vmType.displayName}】JOB数量已经超过告警阈值，" +
                        "正在执行JOB数量：$runningJobCount，配额：$jobQuota，" +
                        "告警阈值：${normalizePercentage(jobThreshold.toDouble())}%，" +
                        "当前已经使用：${normalizePercentage(runningJobCount * 100.0 / jobQuota)}%",
                    tag = VMUtils.genStartVMTaskId(containerId),
                    jobId = containerHashId,
                    executeCount = executeCount ?: 1
                )
            }

            if (runningJobTime >= timeQuota * 60 * 60 * 1000) {
                LOG.warn("checkJobQuota|totalTime:$runningJobTime(s)|quota:$timeQuota(h)" +
                    "|stop it.($pipelineId|$buildId|$vmSeqId)")
                buildLogPrinter.addRedLine(
                    buildId = buildId,
                    message = "当前项目下本月已执行的【${vmType.displayName}】JOB时间达到配额最大值" +
                        "，已执行JOB时间：${String.format("%.2f", runningJobTime / 1000.0 / 60 / 60)}小时, 配额: ${timeQuota}小时",
                    tag = VMUtils.genStartVMTaskId(containerId),
                    jobId = containerHashId,
                    executeCount = executeCount ?: 1
                )
                return !jobQuotaEnable
            }

            if ((runningJobTime * 100) / (timeQuota * 60 * 60 * 1000) >= timeThreshold) {
                buildLogPrinter.addYellowLine(
                    buildId = buildId,
                    message = "前项目下本月已执行的【${vmType.displayName}】JOB时间已经超过告警阈值，" +
                        "已执行JOB时间：${String.format("%.2f", runningJobTime / 1000.0 / 60 / 60)}小时, " +
                        "配额: ${timeQuota}小时，" +
                        "告警阈值：${normalizePercentage(timeThreshold.toDouble())}%，" +
                        "当前已经使用：${normalizePercentage((runningJobTime * 100.0) / (timeQuota * 60 * 60 * 1000))}%",
                    tag = VMUtils.genStartVMTaskId(containerId),
                    jobId = containerHashId,
                    executeCount = executeCount ?: 1
                )
            }
            return true
        }
    }*/

/*    private fun checkProjectWarn(projectId: String, vmType: JobQuotaVmType) {
        val jobQuotaStatus = getProjectRunningJobStatus(projectId, vmType)
        with(jobQuotaStatus) {
            val userList = try {
                client.get(ServiceUserResource::class).getProjectUserRoles(projectId, BkAuthGroup.MANAGER).data
                    ?: emptyList()
            } catch (e: Throwable) {
//                logger.error("getProjectUserRoles exception,", e.message)
                emptyList<String>()
            }
            if (!checkProjectJobMax(projectId, vmType, userList)) {
                checkProjectJobThreshold(projectId, vmType, userList)
            }
            if (!checkProjectJobTime(projectId, vmType, userList)) {
                checkProjectJobTimeThreshold(projectId, vmType, userList)
            }
        }
    }

    private fun JobQuotaStatus.checkProjectJobTimeThreshold(
        projectId: String,
        vmType: JobQuotaVmType,
        userList: List<String>
    ) {
        val timeLock = redisOperation.get(WARN_TIME_PROJECT_TIME_THRESHOLD_LOCK_KEY_PREFIX + projectId)
        if (timeLock != null) {
            if (runningJobCount < jobQuota) {
                redisOperation.delete(WARN_TIME_PROJECT_TIME_THRESHOLD_LOCK_KEY_PREFIX + projectId)
            }
        } else {
            if ((runningJobTime * 100) / (timeQuota * 60 * 60 * 1000) >= timeThreshold) {
                redisOperation.set(key = WARN_TIME_PROJECT_TIME_THRESHOLD_LOCK_KEY_PREFIX + projectId,
                    value = WARN_TIME_LOCK_VALUE,
                    expiredInSecond = 86400)
                LOG.warn("Running job total time:$runningJobTime(s), quota: $timeQuota(h), " +
                    "timeThreshold: $timeThreshold, warning to project master.")
                val msg = "当前项目【$projectId】【${vmType.displayName}】" +
                    "类型的Job当月总执行时长：${String.format("%.2f", runningJobTime / 1000.0 / 60 / 60)}小时，" +
                    "已达到阈值(${timeQuota}小时)的" +
                    "${normalizePercentage((runningJobTime * 100.0) / (timeQuota * 60 * 60 * 1000))}%，" +
                    "请调整流水线，合理控制Job执行时间!"
                sendAlert(msg, userList.toSet())
                LOG.warn(msg)
            }
        }
    }

    private fun JobQuotaStatus.checkProjectJobTime(
        projectId: String,
        vmType: JobQuotaVmType,
        userList: List<String>
    ): Boolean {
        val timeLock = redisOperation.get(WARN_TIME_PROJECT_TIME_MAX_LOCK_KEY_PREFIX + projectId)
        if (timeLock != null) {
            if (runningJobCount < jobQuota) {
                redisOperation.delete(WARN_TIME_PROJECT_TIME_MAX_LOCK_KEY_PREFIX + projectId)
            }
            return false
        } else {
            if (runningJobTime >= timeQuota * 60 * 60 * 1000) {
                redisOperation.set(WARN_TIME_PROJECT_TIME_MAX_LOCK_KEY_PREFIX + projectId, WARN_TIME_LOCK_VALUE, 86400)
                LOG.warn("Running job total time:$runningJobTime(s), quota: $timeQuota(h), warning to project master.")
                val msg = "当前项目【$projectId】【${vmType.displayName}】类型的Job当月总执行时长：" +
                    "${String.format("%.2f", runningJobTime / 1000.0 / 60 / 60)}小时，" +
                    "已达到阈值(${timeQuota}小时)的100%，请调整流水线，合理控制Job执行时间!"
                sendAlert(msg, userList.toSet())
                LOG.warn(msg)
                return true
            }
        }
        return false
    }

    private fun JobQuotaStatus.checkProjectJobThreshold(
        projectId: String,
        vmType: JobQuotaVmType,
        userList: List<String>
    ) {
        val thresholdLock = redisOperation.get(WARN_TIME_PROJECT_JOB_THRESHOLD_LOCK_KEY_PREFIX + projectId)
        if (thresholdLock != null) {
            if (runningJobCount < jobQuota) {
                redisOperation.delete(WARN_TIME_PROJECT_JOB_THRESHOLD_LOCK_KEY_PREFIX + projectId)
            }
        } else {
            if (runningJobCount * 100 / jobQuota >= jobThreshold) {
                redisOperation.set(
                    key = WARN_TIME_PROJECT_JOB_THRESHOLD_LOCK_KEY_PREFIX + projectId,
                    value = WARN_TIME_LOCK_VALUE,
                    expiredInSecond = 86400)
                LOG.warn("Running job count:$runningJobCount, quota: $jobQuota, " +
                    "threshold: $jobThreshold, warning to project master.")
                val msg = "当前项目【$projectId】【${vmType.displayName}】类型的Job最大并发数$runningJobCount，" +
                    "已达到阈值($jobQuota)的${normalizePercentage(runningJobCount * 100.0 / jobQuota)}%，" +
                    "请调整流水线，合理控制Job并发数!"
                sendAlert(msg, userList.toSet())
                LOG.warn(msg)
            }
        }
    }

    private fun JobQuotaStatus.checkProjectJobMax(
        projectId: String,
        vmType: JobQuotaVmType,
        userList: List<String>
    ): Boolean {
        val jobMaxLock = redisOperation.get(WARN_TIME_PROJECT_JOB_MAX_LOCK_KEY_PREFIX + projectId)
        if (jobMaxLock != null) {
            if (runningJobCount < jobQuota) {
                redisOperation.delete(WARN_TIME_PROJECT_JOB_MAX_LOCK_KEY_PREFIX + projectId)
            }
            return false
        } else {
            if (runningJobCount >= jobQuota) {
                redisOperation.set(
                    key = WARN_TIME_PROJECT_JOB_MAX_LOCK_KEY_PREFIX + projectId,
                    value = WARN_TIME_LOCK_VALUE,
                    expiredInSecond = 86400)
                LOG.warn("Running job count:$runningJobCount, quota: $jobQuota, warning to project master.")
                val msg = "当前项目【$projectId】【${vmType.displayName}】类型的Job最大并发数$runningJobCount，" +
                    "已达到阈值($jobQuota)的100%，请调整流水线，合理控制Job并发数!"
                sendAlert(msg, userList.toSet())
                LOG.warn(msg)
                return true
            }
        }
        return false
    }

    private fun checkSystemWarn(vmType: JobQuotaVmType) {
        val jobQuota = jobQuotaManagerService.getSystemQuota(vmType)
        val runningJobCount = runningJobsDao.getSystemRunningJobCount(dslContext, vmType).toLong()
        val runningJobCountGitCi = runningJobsDao.getSystemGitCiRunningJobCount(dslContext, vmType).toLong()

        if (checkSystem(runningJobCount = runningJobCount,
                runningJobMaxSystem = jobQuota.runningJobMaxSystem,
                systemRunningJobThreshold = jobQuota.systemRunningJobThreshold,
                vmType = vmType)) {
            return
        }
        if (checkSystem(runningJobCount = runningJobCountGitCi,
                runningJobMaxSystem = jobQuota.runningJobMaxGitCiSystem,
                systemRunningJobThreshold = jobQuota.systemRunningJobThreshold,
                vmType = vmType)) {
            return
        }
    }

    private fun checkSystem(
        runningJobCount: Long,
        runningJobMaxSystem: Int,
        systemRunningJobThreshold: Int,
        vmType: JobQuotaVmType
    ): Boolean {
        val jobMaxLock = redisOperation.get(WARN_TIME_SYSTEM_JOB_MAX_LOCK_KEY)
        if (jobMaxLock != null) {
            if (runningJobCount < runningJobMaxSystem) {
                redisOperation.delete(WARN_TIME_SYSTEM_JOB_MAX_LOCK_KEY)
            }
            return true
        } else {
            if (runningJobCount >= runningJobMaxSystem) {
                redisOperation.set(WARN_TIME_SYSTEM_JOB_MAX_LOCK_KEY, WARN_TIME_LOCK_VALUE, 86400)
                LOG.warn("System running job count reach max, running jobs: $runningJobCount, " +
                    "quota: $runningJobMaxSystem")
                val msg = "蓝盾当前【${vmType.displayName}】Job并发数为$runningJobCount，已达到100%，请关注。"
                sendAlert(msg, (systemAlertReceiver ?: "").split(",", ";").toSet())

                return true
            }
        }

        val thresholdLock = redisOperation.get(WARN_TIME_SYSTEM_THRESHOLD_LOCK_KEY)
        if (thresholdLock != null) {
            if (runningJobCount * 100 / runningJobMaxSystem < systemRunningJobThreshold) {
                redisOperation.delete(WARN_TIME_SYSTEM_THRESHOLD_LOCK_KEY)
            }
            return true
        } else {
            if (runningJobCount * 100 / runningJobMaxSystem >= systemRunningJobThreshold) {
                redisOperation.set(WARN_TIME_SYSTEM_THRESHOLD_LOCK_KEY, WARN_TIME_LOCK_VALUE, 86400)
                LOG.warn("System running job count reach threshold: $runningJobCount, " +
                    "quota: $runningJobMaxSystem, threshold: $systemRunningJobThreshold send alert.")
                val msg = "蓝盾当前【${vmType.displayName}】Job并发数为$runningJobCount，已达到$systemRunningJobThreshold%，" +
                    "请关注！详情：正在执行JOB数量：$runningJobCount, 阈值：$runningJobMaxSystem, " +
                    "告警阈值：$systemRunningJobThreshold%, " +
                    "当前使用达到${normalizePercentage(runningJobCount * 100.0 / runningJobMaxSystem)}%"
                sendAlert(msg, (systemAlertReceiver ?: "").split(",", ";").toSet())

                return true
            }
        }

        return false
    }*/

    private fun sendAlert(msg: String, receiverUsers: Set<String>) {
        if (receiverUsers.isEmpty()) return
        val envStr = when {
            profile.isProd() -> "生产环境"
            profile.isTest() -> "测试环境"
            else -> "开发环境"
        }
        val rtxMessage = RtxNotifyMessage().apply {
            addAllReceivers(receiverUsers)
            title = "【蓝盾devops系统告警】"
            body = "[$envStr]$msg"
        }
        val emailMessage = EmailNotifyMessage().apply {
            addAllReceivers(receiverUsers)
            title = "【蓝盾devops系统告警】"
            body = "[$envStr]$msg"
        }

//        val notifyCli = client.get(ServiceNotifyResource::class)
//        notifyCli.sendRtxNotify(rtxMessage)
//        notifyCli.sendEmailNotify(emailMessage)
        LOG.info("alert send: ${rtxMessage.body}")
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
        val redisLock = RedisLock(redisOperation, TIMER_OUT_LOCK_KEY, 60L)
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
        if (projectId == null && vmType != JobQuotaVmType.ALL) { // restore all project with vmType
            val projectSet = redisOperation.getSetMembers(QUOTA_PROJECT_ALL_KEY)
            if (null != projectSet && projectSet.isNotEmpty()) {
                projectSet.forEach { project ->
                    restoreWithVmType(project, vmType)
                }
            }
            return
        }
        if (projectId != null && vmType != JobQuotaVmType.ALL) { // restore project with vmType
            restoreWithVmType(projectId, vmType)
            return
        }
    }

    private fun restoreWithVmType(project: String, vmType: JobQuotaVmType) {
        val time = redisOperation.hget(
            getProjectMonthRunningTimeKey(),
            getProjectVmTypeRunningTimeKey(project, vmType)) ?: "0"
        val totalTime = redisOperation.hget(
            getProjectMonthRunningTimeKey(),
            getProjectRunningTimeKey(project)) ?: "0"
        val reduiceTime = (totalTime.toLong() - time.toLong())
        redisOperation.hset(getProjectMonthRunningTimeKey(), getProjectRunningTimeKey(project), if (reduiceTime < 0) {
            "0"
        } else {
            reduiceTime.toString()
        })
        redisOperation.hset(getProjectMonthRunningTimeKey(), getProjectVmTypeRunningTimeKey(project, vmType), "0")
    }

    /**
     * 每月1号凌晨0点删除上个月的redis key
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    fun restoreTimeMonthly() {
        LOG.info("start to clear time monthly")
        val redisLock = RedisLock(redisOperation, TIMER_RESTORE_LOCK_KEY, 60L)
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                LOG.info("<<< Restore time monthly Start >>>")
                val lastMonth = LocalDateTime.now().minusMonths(1).month.name
                redisOperation.delete(getProjectMonthRunningTimeKey(lastMonth))
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
        if (runningJobs.isNotEmpty) {
            try {
                runningJobs.filterNotNull().forEach {
                    val isRunning = client.get(ServicePipelineResource::class).isRunning(projectId = it.projectId,
                        buildId = it.buildId,
                        channelCode = ChannelCode.BS).data
                        ?: false
                    if (!isRunning) {
                        runningJobsDao.delete(
                            dslContext = dslContext,
                            projectId = it.projectId,
                            buildId = it.buildId,
                            vmSeqId = it.vmSeqId)
                        LOG.info("${it.buildId}|${it.vmSeqId} Pipeline not running, but runningJob history not deleted.")
                    }
                }
            } catch (e: Throwable) {
                LOG.error("Check pipeline running failed, msg: ${e.message}")
            }
        }
    }

    private fun incProjectJobRunningTime(projectId: String, vmType: JobQuotaVmType?, time: Long) {
        if (vmType == null) {
            LOG.warn("incProjectJobRunningTime, vmType is null. projectId: $projectId")
            return
        }
        //
        redisOperation.hIncrBy(getProjectMonthRunningTimeKey(), getProjectVmTypeRunningTimeKey(projectId, vmType), time)
        redisOperation.hIncrBy(getProjectMonthRunningTimeKey(), getProjectRunningTimeKey(projectId), time)
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
        statistics()

        val result = mutableMapOf<String, List<ProjectVmTypeTime>>()
        JobQuotaVmType.values().filter { it != JobQuotaVmType.ALL }.forEach { type ->
            val records = jobQuotaProjectRunTimeDao.listByType(dslContext, type, limit ?: 100, offset ?: 0)
                .filterNotNull().map { ProjectVmTypeTime(it.projectId, JobQuotaVmType.parse(it.vmType), it.runTime) }
            result[type.displayName] = records
        }
        return result
    }

    @Scheduled(cron = "0 0 2 * * ?")
    fun statistics() {
//        LOG.info("Count project run time into db.")
//        val projectSet = redisOperation.getSetMembers(QUOTA_PROJECT_ALL_KEY)
//        if (null != projectSet && projectSet.isNotEmpty()) {
//            val redisLock = RedisLock(redisOperation, TIMER_COUNT_TIME_LOCK_KEY, 600L)
//            try {
//                val lockSuccess = redisLock.tryLock()
//                if (lockSuccess) {
//                    LOG.info("<<< Count project run time into db start >>>")
//                    JobQuotaVmType.values().filter { it != JobQuotaVmType.ALL }.forEach { type ->
//                        projectSet.forEach { project ->
//                            jobQuotaProjectRunTimeDao.add(
//                                dslContext = dslContext,
//                                projectId = project,
//                                jobQuotaVmType = type,
//                                runTime = (redisOperation.get(getProjectVmTypeRunningTimeKey(project, type))
//                                    ?: "0").toLong()
//                            )
//                        }
//                    }
//                } else {
//                    LOG.info("<<< Count project run time into db Has Running, Do Not Start>>>")
//                }
//            } catch (e: Throwable) {
//                LOG.error("Count project run time into db exception:", e)
//            } finally {
//                redisLock.unlock()
//            }
//        }
    }

    companion object {
        private const val TIMER_OUT_LOCK_KEY = "job_quota_business_time_out_lock"
        private const val TIMER_RESTORE_LOCK_KEY = "job_quota_business_time_restore_lock"
        private const val TIMER_COUNT_TIME_LOCK_KEY = "job_quota_project_run_time_count_lock"
        private const val JOB_END_LOCK_KEY = "job_quota_business_redis_job_end_lock_"
        private const val PROJECT_RUNNING_TIME_KEY_PREFIX = "project_running_time_key_" // 项目当月已运行时间前缀
        private const val WARN_TIME_SYSTEM_JOB_MAX_LOCK_KEY = "job_quota_warning_system_max_lock_key" // 系统当月已运行JOB数量KEY, 告警使用
        private const val WARN_TIME_SYSTEM_THRESHOLD_LOCK_KEY = "job_quota_warning_system_threshold_lock_key" // 系统当月已运行JOB数量阈值KEY，告警使用
        private const val WARN_TIME_PROJECT_JOB_MAX_LOCK_KEY_PREFIX = "job_quota_warning_project_max_lock_key_" // 项目当月已运行JOB数量告警前缀
        private const val WARN_TIME_PROJECT_JOB_THRESHOLD_LOCK_KEY_PREFIX = "job_quota_warning_project_threshold_lock_key_" // 项目当月已运行JOB数量阈值告警前缀
        private const val WARN_TIME_PROJECT_TIME_MAX_LOCK_KEY_PREFIX = "time_quota_warning_project_max_lock_key_" // 项目当月已运行时间告警前缀
        private const val WARN_TIME_PROJECT_TIME_THRESHOLD_LOCK_KEY_PREFIX = "time_quota_warning_project_threshold_lock_key_" // 项目当月已运行时间阈值告警前缀
        private const val WARN_TIME_LOCK_VALUE = "job_quota_warning_lock_value" // VALUE值，标志位
        private const val TIMEOUT_DAYS = 7L
        private const val CHECK_RUNNING_DAYS = 1L
        private const val QUOTA_PROJECT_ALL_KEY = "project_time_quota_all_key"
        private val LOG = LoggerFactory.getLogger(JobQuotaBusinessService::class.java)
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

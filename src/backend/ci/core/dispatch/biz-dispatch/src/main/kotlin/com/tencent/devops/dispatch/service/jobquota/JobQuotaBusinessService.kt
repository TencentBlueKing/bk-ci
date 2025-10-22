/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.dispatch.service.jobquota

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.dao.JobQuotaProjectRunTimeDao
import com.tencent.devops.dispatch.dao.RunningJobsDao
import com.tencent.devops.dispatch.exception.ErrorCodeEnum
import com.tencent.devops.dispatch.pojo.JobConcurrencyHistory
import com.tencent.devops.dispatch.pojo.JobQuotaHistory
import com.tencent.devops.dispatch.pojo.JobQuotaStatus
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.dispatch.utils.redis.JobQuotaRedisUtils
import com.tencent.devops.process.engine.common.VMUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class JobQuotaBusinessService @Autowired constructor(
    private val jobQuotaManagerService: JobQuotaManagerService,
    private val runningJobsDao: RunningJobsDao,
    private val jobQuotaProjectRunTimeDao: JobQuotaProjectRunTimeDao,
    private val dslContext: DSLContext,
    private val jobQuotaInterface: JobQuotaInterface,
    private val buildLogPrinter: BuildLogPrinter,
    private val jobQuotaRedisUtils: JobQuotaRedisUtils
) {

    /**
     * job启动时检查配额并记录
     */
    fun checkAndAddRunningJob(
        projectId: String,
        jobType: JobQuotaVmType,
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        containerId: String,
        containerHashId: String?,
        channelCode: String = ChannelCode.BS.name
    ): Boolean {
        val result: Boolean
        val jobQuotaProjectLock = jobQuotaRedisUtils.getJobQuotaProjectLock(projectId, jobType)
        try {
            jobQuotaProjectLock.lock()
            result = checkJobQuotaBase(
                projectId = projectId,
                buildId = buildId,
                containerId = containerId,
                containerHashId = containerHashId,
                executeCount = executeCount,
                vmType = jobType,
                channelCode = channelCode
            )

            // 如果配额没有超限，则记录一条running job
            if (result) {
                runningJobsDao.insert(dslContext, projectId, jobType, buildId, vmSeqId, executeCount, channelCode)
            }
        } finally {
            jobQuotaProjectLock.unlock()
        }

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

    private fun getProjectRunningJobStatus(
        projectId: String,
        vmType: JobQuotaVmType,
        channelCode: String
    ): JobQuotaStatus {
        val jobQuota = jobQuotaManagerService.getProjectQuota(projectId, vmType, channelCode)
        val runningJobCount = runningJobsDao.getProjectRunningJobCount(dslContext, projectId, vmType, channelCode)
        val threshold = jobQuotaManagerService.getSystemQuota(vmType, channelCode)
        // 目前暂时不用关注构建总耗时，先注释
        // val runningJobTime = getProjectRunningJobTime(projectId, vmType, channelCode)

        return JobQuotaStatus(
            jobQuota = jobQuota.runningJobMax,
            runningJobCount = runningJobCount,
            jobThreshold = threshold.projectRunningJobThreshold,
            timeQuota = jobQuota.runningTimeProjectMax.toLong(),
            runningJobTime = 0,
            timeThreshold = threshold.projectRunningTimeThreshold
        )
    }

    /**
     * 获取项目当月所有JOB已运行的时间，返回已运行的时间
     */
    private fun getProjectRunningJobTime(
        projectId: String,
        vmType: JobQuotaVmType,
        channelCode: String
    ): Long {
        if (vmType == JobQuotaVmType.ALL) {
            // 所有运行中的耗时
            var runningTotalTime = 0L
            JobQuotaVmType.values().filter { it != JobQuotaVmType.ALL }.forEach { type ->
                val runningJobs = runningJobsDao.getProjectRunningJobs(dslContext, projectId, type, channelCode)
                runningJobs.filter { it?.agentStartTime != null }.forEach {
                    val duration: Duration = Duration.between(it!!.agentStartTime, LocalDateTime.now())
                    runningTotalTime += duration.toMillis()
                }
            }

            // 所有已经结束的耗时
            val finishRunJobTime = jobQuotaRedisUtils.getProjectRunJobTime(projectId)
            runningTotalTime += (finishRunJobTime ?: "0").toLong()

            return runningTotalTime
        } else {
            // 运行中的耗时
            val runningJobs = runningJobsDao.getProjectRunningJobs(dslContext, projectId, vmType, channelCode)
            var runningTotalTime = 0L
            runningJobs.filter { it?.agentStartTime != null }.forEach {
                val duration: Duration = Duration.between(it!!.agentStartTime, LocalDateTime.now())
                runningTotalTime += duration.toMillis()
            }

            // 所有已经结束的耗时
            val finishRunJobTime = jobQuotaRedisUtils.getProjectRunJobTypeTime(projectId, vmType, channelCode)
            runningTotalTime += (finishRunJobTime ?: "0").toLong()

            return runningTotalTime
        }
    }

    private fun checkJobQuotaBase(
        projectId: String,
        buildId: String,
        containerId: String,
        containerHashId: String?,
        executeCount: Int?,
        vmType: JobQuotaVmType,
        channelCode: String
    ): Boolean {
        val jobStatus = getProjectRunningJobStatus(projectId, vmType, channelCode)

        with(jobStatus) {
            // 记录一次job并发数据
            saveJobConcurrency(
                projectId = projectId,
                runningJobCount = jobStatus.runningJobCount,
                jobQuotaVmType = vmType,
                channelCode = channelCode
            )

            if (runningJobCount >= jobQuota) {
                buildLogPrinter.addYellowLine(
                    buildId = buildId,
                    message = ErrorCodeEnum.JOB_NUM_REACHED_MAX_QUOTA.getErrorMessage(
                        params = arrayOf(vmType.displayName, "$runningJobCount", "$jobQuota"),
                        language = I18nUtil.getDefaultLocaleLanguage()
                    ),
                    tag = VMUtils.genStartVMTaskId(containerId),
                    containerHashId = containerHashId,
                    executeCount = executeCount ?: 1,
                    jobId = null,
                    stepId = VMUtils.genStartVMTaskId(containerId)
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
                    containerHashId = containerHashId,
                    executeCount = executeCount ?: 1,
                    jobId = null,
                    stepId = VMUtils.genStartVMTaskId(containerId)
                )
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
        val redisLock = jobQuotaRedisUtils.getJobAgentFinishLock(buildId, vmSeqId, executeCount)
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                val runningJobsRecord = runningJobsDao.getAgentRunningJobs(dslContext, buildId, vmSeqId, executeCount)
                if (runningJobsRecord != null && runningJobsRecord.agentStartTime != null) {
                    val duration: Duration = Duration.between(runningJobsRecord.agentStartTime, LocalDateTime.now())

                    // 保存构建时间，单位秒
                    jobQuotaRedisUtils.incProjectJobRunningTime(
                        projectId = projectId,
                        jobType = JobQuotaVmType.parse(runningJobsRecord.vmType),
                        costTime = duration.toMillis() / 1000,
                        agentStartTime = runningJobsRecord.agentStartTime,
                        channelCode = runningJobsRecord.channelCode
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
                            channelCode = runningJobsRecord.channelCode,
                            createTime = runningJobsRecord.createdTime.format(dateTimeFormatter),
                            agentStartTime = runningJobsRecord.agentStartTime.format(dateTimeFormatter),
                            agentFinishTime = LocalDateTime.now().format(dateTimeFormatter),
                            costTime = duration.toMillis() / 1000
                        )
                    )
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

    private fun saveJobConcurrency(
        projectId: String,
        runningJobCount: Int,
        jobQuotaVmType: JobQuotaVmType,
        channelCode: String
    ) {
        // 刷新redis缓存数据
        jobQuotaRedisUtils.saveJobConcurrency(projectId, runningJobCount, jobQuotaVmType, channelCode)

        jobQuotaInterface.saveJobConcurrency(
            JobConcurrencyHistory(
                projectId = projectId,
                jobConcurrency = runningJobCount + 1,
                jobQuotaVmType = jobQuotaVmType,
                channelCode = channelCode,
                createTime = LocalDateTime.now().format(dateTimeFormatter)
            )
        )
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

    fun statistics(limit: Int?, offset: Int?): Map<String, Any> {
        val result = mutableMapOf<String, List<ProjectVmTypeTime>>()
        JobQuotaVmType.values().filter { it != JobQuotaVmType.ALL }.forEach { type ->
            val records = jobQuotaProjectRunTimeDao.listByType(dslContext, type, limit ?: 100, offset ?: 0)
                .filterNotNull().map { ProjectVmTypeTime(it.projectId, JobQuotaVmType.parse(it.vmType), it.runTime) }
            result[type.displayName] = records
        }
        return result
    }

    companion object {
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

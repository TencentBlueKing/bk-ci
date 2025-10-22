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
package com.tencent.devops.dispatch.utils.redis

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.dispatch.service.jobquota.JobQuotaBusinessService
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.Cursor
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class JobQuotaRedisUtils {
    fun getJobQuotaProjectLock(projectId: String, jobType: JobQuotaVmType): RedisLock {
        return RedisLock(getRedisStringSerializerOperation(), "$JOB_PROJECT_LOCK_KEY$projectId${jobType.name}", 60L)
    }

    fun getJobStatisticsLock(): RedisLock {
        return RedisLock(getRedisStringSerializerOperation(), JOB_STATISTICS_LOCK_KEY, 300L)
    }

    fun getTimeoutJobLock(): RedisLock {
        return RedisLock(getRedisStringSerializerOperation(), TIMER_OUT_LOCK_KEY, 60L)
    }

    fun getJobAgentFinishLock(
        buildId: String,
        vmSeqId: String,
        executeCount: Int
    ): RedisLock {
        return RedisLock(
            redisOperation = getRedisStringSerializerOperation(),
            lockKey = "$JOB_END_LOCK_KEY$buildId-$vmSeqId-$executeCount",
            expiredTimeInSeconds = 60L
        )
    }

    fun getProjectRunJobTime(projectId: String): String? {
        return getRedisStringSerializerOperation().hget(getDayJobRunningTimeKey(),
            getProjectRunningTimeKey(projectId))
    }

    fun getProjectRunJobTypeTime(
        projectId: String,
        jobType: JobQuotaVmType,
        channelCode: String
    ): String? {
        return getRedisStringSerializerOperation().hget(
            getDayJobRunningTimeKey(),
            getProjectJobTypeRunningTimeKey(projectId, jobType, channelCode))
    }

    fun getLastDayProjectRunJobTypeTime(
        projectId: String,
        jobType: JobQuotaVmType,
        channelCode: String
    ): String? {
        val lastWeekDay = LocalDateTime.now().minusDays(1).dayOfWeek
        return getRedisStringSerializerOperation().hget(
            getDayJobRunningTimeKey(lastWeekDay.name),
            getProjectJobTypeRunningTimeKey(projectId, jobType, channelCode))
    }

    fun getLastDayAllProjectConcurrency(): Cursor<MutableMap.MutableEntry<String, String>> {
        val lastWeekDay = LocalDateTime.now().minusDays(1).dayOfWeek
        return getRedisStringSerializerOperation().hscan(getDayJobConcurrencyKey(lastWeekDay.name))
    }

    fun saveJobConcurrency(
        projectId: String,
        runningJobCount: Int,
        jobQuotaVmType: JobQuotaVmType,
        channelCode: String
    ) {
        // 刷新redis缓存数据
        val dayJobConcurrencyKey = getDayJobConcurrencyKey()
        val projectDayJobConcurrencyKey = getProjectJobTypeConcurrencyKey(projectId, jobQuotaVmType, channelCode)

        val maxConcurrency = getRedisStringSerializerOperation().hget(
            dayJobConcurrencyKey,
            projectDayJobConcurrencyKey
        )?.toLongOrNull()

        if (maxConcurrency == null || maxConcurrency < (runningJobCount + 1)) {
            getRedisStringSerializerOperation().hset(
                dayJobConcurrencyKey,
                projectDayJobConcurrencyKey,
                (runningJobCount + 1).toString()
            )
        }
    }

    fun restoreProjectJobTime(
        projectId: String?,
        vmType: JobQuotaVmType,
        channelCode: String = ChannelCode.BS.name
    ) {
        if (projectId == null && vmType != JobQuotaVmType.ALL) {
            // 直接删除当月的hash主key
            getRedisStringSerializerOperation().delete(getDayJobRunningTimeKey())
            return
        }
        if (projectId != null && vmType != JobQuotaVmType.ALL) { // restore project with vmType
            restoreWithVmType(projectId, vmType, channelCode)
            return
        }
    }

    private fun restoreWithVmType(project: String, vmType: JobQuotaVmType, channelCode: String) {
        val time = getRedisStringSerializerOperation().hget(
            getDayJobRunningTimeKey(),
            getProjectJobTypeRunningTimeKey(project, vmType, channelCode)) ?: "0"
        val totalTime = getRedisStringSerializerOperation().hget(
            getDayJobRunningTimeKey(),
            getProjectRunningTimeKey(project)) ?: "0"
        val reduiceTime = (totalTime.toLong() - time.toLong())
        getRedisStringSerializerOperation().hset(
            key = getDayJobRunningTimeKey(),
            hashKey = getProjectRunningTimeKey(project),
            values = if (reduiceTime < 0) {
                "0"
            } else {
                reduiceTime.toString()
            }
        )
        getRedisStringSerializerOperation().hset(
            key = getDayJobRunningTimeKey(),
            hashKey = getProjectJobTypeRunningTimeKey(project, vmType, channelCode),
            values = "0"
        )
    }

    fun incProjectJobRunningTime(
        projectId: String,
        jobType: JobQuotaVmType?,
        costTime: Long,
        agentStartTime: LocalDateTime,
        channelCode: String
    ) {
        if (jobType == null) {
            LOG.warn("incProjectJobRunningTime, vmType is null. projectId: $projectId")
            return
        }

        // 判断如果是跨天的构建任务，并发数加一
        if (agentStartTime.dayOfYear != LocalDateTime.now().dayOfYear) {
            saveJobConcurrency(projectId, 0, jobType, channelCode)
        }

        getRedisStringSerializerOperation().hIncrBy(
            key = getDayJobRunningTimeKey(),
            hashKey = getProjectJobTypeRunningTimeKey(projectId, jobType, channelCode),
            delta = costTime
        )

        // 注释按月统计时间
        /*getRedisStringSerializerOperation().hIncrBy(
            key = getProjectMonthRunningTimeKey(),
            hashKey = getProjectRunningTimeKey(projectId),
            delta = time
        )*/
    }

    fun parseProjectJobTypeRunningTimeKey(key: String): Pair<String, String> {
        val array = key.removePrefix(PROJECT_RUNNING_TIME_KEY_PREFIX).split(":", limit = 2)
        return Pair(array[0], array[1])
    }

    fun parseProjectJobTypeConcurrencyKey(key: String): Triple<String, String, String> {
        val array = key.removePrefix(PROJECT_JOB_CONCURRENCY_KEY_PREFIX).split(":", limit = 3)
        return Triple(array[0], array[1], array[2])
    }

    /**
     * 删除前一天的hashKey
     */
    fun deleteLastDayJobKey() {
        val lastWeekDay = LocalDateTime.now().minusDays(1).dayOfWeek
        getRedisStringSerializerOperation().delete(getDayJobRunningTimeKey(lastWeekDay.name))
        getRedisStringSerializerOperation().delete(getDayJobConcurrencyKey(lastWeekDay.name))
    }

    /**
     * 给hashKey设置过期时间
     */
    fun setDayJobKeyExpire() {
        getRedisStringSerializerOperation().expire(getDayJobRunningTimeKey(), DAY_HASH_KEY_EXPIRE_TIME)
        getRedisStringSerializerOperation().expire(getDayJobConcurrencyKey(), DAY_HASH_KEY_EXPIRE_TIME)
    }

    /************************** KEY ***************************/

    private fun getDayJobRunningTimeKey(day: String? = null): String {
        val currentDay = day ?: LocalDateTime.now().dayOfWeek.name
        return "$PROJECT_RUNNING_TIME_KEY_PREFIX$currentDay"
    }

    private fun getProjectJobTypeRunningTimeKey(
        projectId: String,
        vmType: JobQuotaVmType,
        channelCode: String
    ): String {
        return "$PROJECT_RUNNING_TIME_KEY_PREFIX$projectId:${vmType.name}:$channelCode"
    }

    private fun getProjectRunningTimeKey(projectId: String): String {
        return "$PROJECT_RUNNING_TIME_KEY_PREFIX$projectId"
    }

    private fun getDayJobConcurrencyKey(day: String? = null): String {
        val currentDay = day ?: LocalDateTime.now().dayOfWeek.name
        return "$PROJECT_JOB_CONCURRENCY_KEY_PREFIX$currentDay"
    }

    private fun getProjectJobTypeConcurrencyKey(
        projectId: String,
        vmType: JobQuotaVmType,
        channelCode: String
    ): String {
        return "$PROJECT_JOB_CONCURRENCY_KEY_PREFIX$projectId:${vmType.name}:$channelCode"
    }

    private fun getRedisStringSerializerOperation(): RedisOperation {
        return SpringContextUtil.getBean(RedisOperation::class.java, "redisStringHashOperation")
    }

    companion object {
        private const val JOB_PROJECT_LOCK_KEY = "job_project_quota_"
        private const val JOB_STATISTICS_LOCK_KEY = "job_statistics_lock"
        private const val TIMER_OUT_LOCK_KEY = "job_quota_business_time_out_lock"
        private const val TIMER_RESTORE_LOCK_KEY = "job_quota_business_time_restore_lock"
        private const val TIMER_COUNT_TIME_LOCK_KEY = "job_quota_project_run_time_count_lock"
        private const val JOB_END_LOCK_KEY = "job_quota_business_redis_job_end_lock_"
        private const val PROJECT_RUNNING_TIME_KEY_PREFIX = "dispatch:project_running_time_key:" // 项目当天已运行时间前缀
        private const val PROJECT_JOB_CONCURRENCY_KEY_PREFIX = "dispatch:project_job_concurrency_key:" // 项目当天最大并发job前缀

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

        private const val DAY_HASH_KEY_EXPIRE_TIME = 3600 * 25L

        private val LOG = LoggerFactory.getLogger(JobQuotaBusinessService::class.java)
    }
}

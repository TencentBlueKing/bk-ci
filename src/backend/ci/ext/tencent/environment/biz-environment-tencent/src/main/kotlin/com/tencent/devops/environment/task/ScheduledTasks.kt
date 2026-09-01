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

package com.tencent.devops.environment.task

import com.tencent.devops.environment.service.RedisLockService
import com.tencent.devops.environment.service.sync.UpdateCmdbNodeService
import com.tencent.devops.environment.service.sync.UpdateGseAgentInfoService
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

/**
 * 后台自动调度执行的任务汇总，默认开启
 *
 * 通过配置 environment.schedule.enabled=false 可禁用所有定时任务。
 * 背景：多个集群（可能共享同一个 MySQL 和 Redis 实例，但各集群配置了不同的
 * spring.redis.name，导致 Redis 分布式锁的 key 带有不同前缀，
 * 无法跨集群互斥。而这些定时任务都在操作同一个 MySQL 的 T_NODE / T_PROJECT_JOB 表，并且会调用 CMDB、CC、
 * GSE 等外部 API，多集群并行执行会导致重复写表和重复调用外部 API。因此需要通过配置开关确保只在一个集群中启用。
 */
@Service("ScheduledTasks")
@Primary
@ConditionalOnProperty(
    prefix = "environment.schedule",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class ScheduledTasks @Autowired constructor(
    private val updateGseAgentInfoService: UpdateGseAgentInfoService,
    private val redisLockService: RedisLockService,
    private val clearExpiredJobTask: ClearExpiredJobTask,
    private val updateCmdbNodeService: UpdateCmdbNodeService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ScheduledTasks::class.java)
        private const val SCHEDULED_CHECK_NODES_TIMEOUT_LOCK_KEY = "scheduled_check_nodes_timeout_lock"
        private const val SCHEDULED_UPDATE_GSE_AGENT_TIMEOUT_LOCK_KEY = "scheduled_update_gse_agent_timeout_lock"
        private const val CLEAR_EXPIRED_JOB_TASK_TIMEOUT_LOCK_KEY = "clear_expired_job_task_timeout_lock"
    }

    @PostConstruct
    fun init() {
        logger.info("[environment ScheduledTasks]Register success.")
    }

    /**
     * 清理T_PROJECT_JOB表的过期数据（job任务过期时间：1个月）
     * cron：每天执行一次
     */
    @Scheduled(cron = "0 19 19 * * ?")
    fun scheduledClearExpiredJobTask() {
        redisLockService.taskWithRedisLock(
            CLEAR_EXPIRED_JOB_TASK_TIMEOUT_LOCK_KEY,
            clearExpiredJobTask::execute
        )
    }

    /**
     * 后台定时更新机器的CMDB信息（状态与属性等）
     * cron：默认每10分钟执行一次，0 7/10 * * * ?
     * 因为需要调用外部API，所以各环境定时时间错开防止接口429
     */
    @Scheduled(cron = "\${environment.schedule.updateCmdbNode.cron:0 7/10 * * * ?}")
    fun scheduledUpdateCmdbNodeInfo() {
        redisLockService.taskWithRedisLock(
            SCHEDULED_CHECK_NODES_TIMEOUT_LOCK_KEY,
            updateCmdbNodeService::updateCmdbNodeInfo
        )
    }

    /**
     * 定时任务：gse agent状态/版本 轮询 + 差量更新
     * 条件：NODE_TYPE为"部署"的，查询该节点的agent安装状态以及版本，并对比差异更新。
     * 分组执行，每次遍历1000条记录。
     * cron：每10分钟执行一次。
     */
    @Scheduled(cron = "0 8/10 * * * ?")
    fun updateGseAgentStatusAndVersionPeriodically() {
        redisLockService.taskWithRedisLock(
            SCHEDULED_UPDATE_GSE_AGENT_TIMEOUT_LOCK_KEY,
            updateGseAgentInfoService::updateGseAgentStatusAndVersion
        )
    }
}

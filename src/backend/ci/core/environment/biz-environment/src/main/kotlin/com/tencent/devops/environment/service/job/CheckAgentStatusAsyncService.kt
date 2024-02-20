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

package com.tencent.devops.environment.service.job

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.job.agentreq.QueryAgentTaskStatusReq
import com.tencent.devops.environment.resources.job.UserJobResourceImpl
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.ws.rs.core.Response

@Service("CheckAgentStatusAsyncService")
class CheckAgentStatusAsyncService @Autowired constructor(
    private val userJobResourceImpl: UserJobResourceImpl,
    private val dslContext: DSLContext,
    private val nodeDao: NodeDao,
    val redisOperation: RedisOperation
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CheckAgentStatusAsyncService::class.java)

        private const val DEFAULT_PAGE = 20
        private const val DEFAULT_PAGE_SIZE = 20
        private const val MAXIMUM_RETRY_TIMES = 20
        private const val INITIAL_DELAY = 1000L // unit: ms
        private const val TASK_PERIOD = 15000L // unit: ms

        private const val AGENT_INSTALL_NORMAL = "SUCCESS"
        private val agentTaskEndStatusList = listOf(
            "FAILED", "SUCCESS", "PART_FAILED", "TERMINATED", "REMOVED", "FILTERED", "IGNORED"
        ) // 两种正在执行状态："PENDING"(等待), "RUNNING"（正在执行）

        private const val CHECK_NODE_STATUS_TIMEOUT_LOCK_KEY = "check_node_status_timeout_lock"
        private const val EXPIRATION_TIME_OF_THE_LOCK = 200L
    }

    /**
     * 安装agent状态轮询
     * 发起安装任务后，轮询安装状态：安装中 - NODE_STATUS: RUNNING
     * 执行定时轮询任务，每隔 30000ms 检查任务状态，如果结束（成功/失败）则停止轮询。
     */
    @Async("checkAgentStatus")
    fun checkAgentStatus(userId: String, projectId: String, jobId: Int?, ipList: List<String>?) {
        val redisLock = RedisLock(redisOperation, CHECK_NODE_STATUS_TIMEOUT_LOCK_KEY, EXPIRATION_TIME_OF_THE_LOCK)
        redisLock.takeIf { it.tryLock() }.run {
            try {
                if (null == jobId) {
                    throw CustomException(
                        Response.Status.INTERNAL_SERVER_ERROR,
                        "Empty job id."
                    )
                }
                if (null == ipList) return
                val executor = Executors.newSingleThreadScheduledExecutor()
                val runningIpList = ipList.toMutableList()
                nodeDao.updateNodeStatusByNodeIp(dslContext, ipList, NodeStatus.RUNNING.name)
                val task = object : Runnable {
                    var count = 0
                    override fun run() {
                        val queryAgentTaskStatusReq = QueryAgentTaskStatusReq(
                            page = DEFAULT_PAGE, pageSize = DEFAULT_PAGE_SIZE
                        )
                        val queryAgentTaskStatusRes = userJobResourceImpl.queryAgentTaskStatus(
                            userId, projectId, jobId, queryAgentTaskStatusReq
                        )
                        queryAgentTaskStatusRes.data?.list?.filter {
                            it.ip in runningIpList
                        }?.map {
                            if (it.status in agentTaskEndStatusList) { // agent安装结束(成功/失败)
                                val nodeStatus =
                                    if (AGENT_INSTALL_NORMAL == it.status) NodeStatus.NORMAL.name
                                    else NodeStatus.ABNORMAL.name
                                nodeDao.updateNodeStatusByNodeIp(dslContext, listOf(it.ip), nodeStatus)
                                runningIpList.remove(it.ip)
                            }
                        }
                        if (runningIpList.isEmpty()) {
                            logger.info("Agent install task is complete.")
                            executor.shutdown()
                        } else if (count > MAXIMUM_RETRY_TIMES) {
                            logger.info("Agent install task is partially complete. Abnormal ip: $runningIpList")
                            nodeDao.updateNodeStatusByNodeIp(dslContext, runningIpList, NodeStatus.ABNORMAL.name)
                            executor.shutdown()
                        } else {
                            if (logger.isDebugEnabled) logger.debug("Agent install task running...")
                            count++
                        }
                    }
                }
                executor.scheduleAtFixedRate(task, INITIAL_DELAY, TASK_PERIOD, TimeUnit.MILLISECONDS)
            } finally {
                redisLock.unlock()
            }
        }
    }
}
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

package com.tencent.devops.misc.service.process

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.db.pojo.MIGRATING_SHARDING_DSL_CONTEXT
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.misc.dao.process.ProcessDao
import com.tencent.devops.model.process.tables.TPipelineBuildHistory
import com.tencent.devops.process.api.service.ServiceBuildResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import javax.annotation.Resource

@Service
class ProcessDbMigrateService @Autowired constructor(
    private val dslContext: DSLContext,
    @Resource(name = MIGRATING_SHARDING_DSL_CONTEXT) private val migratingShardingDslContext: DSLContext,
    private val processDao: ProcessDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ProcessDbMigrateService::class.java)
        private const val DEFAULT_THREAD_NUM = 10
        private const val DEFAULT_PAGE_SIZE = 20
    }

    fun migrateProjectData(
        userId: String,
        projectId: String,
        cancelFlag: Boolean = false
    ): Boolean {
        logger.info("migrateProjectData params:[$userId|$projectId]")
        // 查询项目下流水线数量
        val pipelineNum = processDao.getPipelineNumByProjectId(dslContext, projectId)
        // 根据流水线数量计算线程数量
        val threadNum = if (pipelineNum < DEFAULT_THREAD_NUM) {
            pipelineNum
        } else {
            DEFAULT_THREAD_NUM
        }
        // 根据线程数量创建线程池
        val executor = Executors.newFixedThreadPool(threadNum)
        // 根据线程数量创建信号量
        val semaphore = Semaphore(threadNum)
        // 根据流水线数量创建计数器
        val doneSignal = CountDownLatch(pipelineNum)
        var minPipelineInfoId = processDao.getMinPipelineInfoIdByProjectId(dslContext, projectId)
        do {
            val pipelineIdList = processDao.getPipelineIdListByProjectId(
                dslContext = dslContext,
                projectId = projectId,
                minId = minPipelineInfoId,
                limit = DEFAULT_PAGE_SIZE.toLong()
            )?.map { it.getValue(0).toString() }
            if (!pipelineIdList.isNullOrEmpty()) {
                // 重置minId的值
                minPipelineInfoId = (processDao.getPipelineInfoByPipelineId(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineIdList[pipelineIdList.size - 1]
                )?.id ?: 0L) + 1
            }
            pipelineIdList?.forEach { pipelineId ->
                // executor.submit()

            }
        } while (pipelineIdList?.size == DEFAULT_PAGE_SIZE)
        return true
    }

    class MigratePipelineDataTask constructor(
        private val projectId: String,
        private val pipelineId: String,
        private val cancelFlag: Boolean,
        private val semaphore: Semaphore,
        private val doneSignal: CountDownLatch,
        private val dslContext: DSLContext,
        private val processDao: ProcessDao
    ) : Runnable {
        override fun run() {
            // 获取是否允许执行的信号量
            semaphore.acquire()
            logger.info("migrateProjectData project[$projectId],pipeline[$pipelineId] start..............")
            try {
                if (cancelFlag) {
                    handleUnFinishPipelines()
                }
            } finally {
                // 业务逻辑执行完成后释放信号量
                semaphore.release()
                // 业务逻辑执行完后计数器减1
                doneSignal.countDown()
            }
        }

        private fun handleUnFinishPipelines() {
            // 查看项目下是否还有未结束的构建
            val unFinishStatusList = listOf(
                BuildStatus.QUEUE,
                BuildStatus.QUEUE_CACHE,
                BuildStatus.RUNNING
            )
            var offset = 0
            do {
                // 查询未结束的构建记录
                val historyInfoRecords = processDao.getHistoryInfoList(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    offset = offset,
                    limit = DEFAULT_PAGE_SIZE,
                    statusList = unFinishStatusList
                )
                val tPipelineBuildHistory = TPipelineBuildHistory.T_PIPELINE_BUILD_HISTORY
                historyInfoRecords?.forEach { historyInfoRecord ->
                    val buildId = historyInfoRecord[tPipelineBuildHistory.BUILD_ID]
                    val channel = historyInfoRecord[tPipelineBuildHistory.CHANNEL]
                    val startUser = historyInfoRecord[tPipelineBuildHistory.START_USER]
                    val client = SpringContextUtil.getBean(Client::class.java)
                    client.get(ServiceBuildResource::class).manualShutdown(
                        userId = startUser,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        channelCode = ChannelCode.getChannel(channel) ?: ChannelCode.BS
                    )
                }
                offset += DEFAULT_PAGE_SIZE
            } while (historyInfoRecords?.size == DEFAULT_PAGE_SIZE)
        }
    }
}

package com.tencent.devops.misc.task

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.misc.pojo.process.MigratePipelineDataParam
import com.tencent.devops.model.process.tables.TPipelineBuildHistory
import com.tencent.devops.process.api.service.ServiceBuildResource
import org.slf4j.LoggerFactory

class MigratePipelineDataTask constructor(
    private val migratePipelineDataParam: MigratePipelineDataParam
) : Runnable {

    companion object {
        private val logger = LoggerFactory.getLogger(MigratePipelineDataTask::class.java)
        private const val DEFAULT_PAGE_SIZE = 20
        private const val RETRY_NUM = 3
    }
        override fun run() {
            val semaphore = migratePipelineDataParam.semaphore
            val projectId = migratePipelineDataParam.projectId
            val pipelineId = migratePipelineDataParam.pipelineId
            val cancelFlag = migratePipelineDataParam.cancelFlag
            val doneSignal = migratePipelineDataParam.doneSignal
            // 获取是否允许执行的信号量
            semaphore.acquire()
            logger.info("migrateProjectData project[$projectId],pipeline[$pipelineId] start..............")
            try {
                if (cancelFlag) {
                    // 取消未结束的构建
                    handleUnFinishPipelines(RETRY_NUM)
                }
                // 开始迁移流水线的数据
            } finally {
                // 业务逻辑执行完成后释放信号量
                semaphore.release()
                // 业务逻辑执行完后计数器减1
                doneSignal.countDown()
            }
        }

        private fun handleUnFinishPipelines(retryNum: Int) {
            // 查看项目下是否还有未结束的构建
            val unFinishStatusList = listOf(
                BuildStatus.QUEUE,
                BuildStatus.QUEUE_CACHE,
                BuildStatus.RUNNING
            )
            val projectId = migratePipelineDataParam.projectId
            val pipelineId = migratePipelineDataParam.pipelineId
            var offset = 0
            var retryFlag = false
            do {
                // 查询未结束的构建记录
                val historyInfoRecords = migratePipelineDataParam.processDao.getHistoryInfoList(
                    dslContext = migratePipelineDataParam.dslContext,
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
                    try {
                        val shutdownResult = client.get(ServiceBuildResource::class).manualShutdown(
                            userId = startUser,
                            projectId = projectId,
                            pipelineId = pipelineId,
                            buildId = buildId,
                            channelCode = ChannelCode.getChannel(channel) ?: ChannelCode.BS
                        )
                        if (shutdownResult.isNotOk()) {
                            logger.warn("porject[$projectId]-pipelineId[$pipelineId]-buildId[$buildId] cancel fail")
                            retryFlag = true
                        }
                    } catch (ignored: Throwable) {
                        logger.warn(
                            "porject[$projectId]-pipelineId[$pipelineId]-buildId[$buildId] cancel fail",
                            ignored
                        )
                        retryFlag = true
                    }
                }
                offset += DEFAULT_PAGE_SIZE
            } while (historyInfoRecords?.size == DEFAULT_PAGE_SIZE)
            if (retryFlag) {
                if (retryNum > 0) {
                    Thread.sleep(25000)
                    // 重试取消动作
                    handleUnFinishPipelines(retryNum - 1)
                }
            }
        }
    }

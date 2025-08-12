package com.tencent.devops.misc.task

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.utils.BkApiUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.misc.dao.process.ProcessDao
import com.tencent.devops.misc.factory.MigrationStrategyFactory
import com.tencent.devops.misc.pojo.constant.MiscMessageCode
import com.tencent.devops.misc.pojo.process.MigratePipelineDataParam
import com.tencent.devops.misc.pojo.process.MigrationContext
import com.tencent.devops.model.process.tables.TPipelineBuildHistory
import com.tencent.devops.process.api.service.ServiceBuildResource
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory

class PipelineMigrationTask constructor(
    private val migratePipelineDataParam: MigratePipelineDataParam
) : Runnable {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineMigrationTask::class.java)
        private const val DEFAULT_PAGE_SIZE = 20
        private const val RETRY_NUM = 3
        private const val DEFAULT_THREAD_SLEEP_TINE = 5000L
    }
        override fun run() {
            val semaphore = migratePipelineDataParam.semaphore
            val projectId = migratePipelineDataParam.projectId
            val pipelineId = migratePipelineDataParam.pipelineId
            val cancelFlag = migratePipelineDataParam.cancelFlag
            val doneSignal = migratePipelineDataParam.doneSignal
            val dslContext = migratePipelineDataParam.dslContext
            val migratingShardingDslContext = migratePipelineDataParam.migratingShardingDslContext
            val migrationStrategyFactory = migratePipelineDataParam.migrationStrategyFactory
            val processDao = migratePipelineDataParam.processDao
            val archiveFlag = migratePipelineDataParam.archiveFlag
            // 1、获取是否允许执行的信号量
            semaphore?.acquire()
            logger.info("migrateProjectData project[$projectId],pipeline[$pipelineId] start..............")
            if (cancelFlag) {
                // 2、取消未结束的构建
                handleUnFinishPipelines(RETRY_NUM)
                Thread.sleep(DEFAULT_THREAD_SLEEP_TINE)
            }
            // 检查构建是否结束
            isBuildCompleted(
                dslContext = dslContext,
                processDao = processDao,
                projectId = projectId,
                pipelineId = pipelineId
            )
            try {
                // 3、开始迁移流水线的数据
                executeMigration(
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    migrationStrategyFactory = migrationStrategyFactory,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    archiveFlag = archiveFlag
                )
                // 4、业务逻辑成功执行完后计数器减1
                doneSignal?.countDown()
                logger.info("migrateProjectData project[$projectId],pipeline[$pipelineId] end..............")
            } catch (ignored: Throwable) {
                logger.info("migrateProjectData project[$projectId],pipeline[$pipelineId] run task fail", ignored)
                throw ErrorCodeException(
                    errorCode = MiscMessageCode.ERROR_MIGRATING_PIPELINE_DATA_FAIL,
                    params = arrayOf(pipelineId),
                    defaultMessage = I18nUtil.getCodeLanMessage(
                        messageCode = MiscMessageCode.ERROR_MIGRATING_PIPELINE_DATA_FAIL,
                        params = arrayOf(pipelineId)
                    )
                )
            } finally {
                // 5、业务逻辑执行完成后释放信号量
                semaphore?.release()
            }
        }

    private fun handleUnFinishPipelines(retryNum: Int) {
        if (retryNum < 1) {
            return
        }
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
        val tPipelineBuildHistory = TPipelineBuildHistory.T_PIPELINE_BUILD_HISTORY
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
            historyInfoRecords?.forEach { historyInfoRecord ->
                val buildId = historyInfoRecord[tPipelineBuildHistory.BUILD_ID]
                val channel = historyInfoRecord[tPipelineBuildHistory.CHANNEL]
                val startUser = historyInfoRecord[tPipelineBuildHistory.START_USER]
                val successFlag = cancelBuild(
                    startUser = startUser,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    channel = channel
                )
                if (!successFlag) {
                    retryFlag = true
                }
            }
            offset += DEFAULT_PAGE_SIZE
        } while (historyInfoRecords?.size == DEFAULT_PAGE_SIZE)
        if (retryFlag) {
            Thread.sleep(DEFAULT_THREAD_SLEEP_TINE)
            // 重试取消动作
            handleUnFinishPipelines(retryNum - 1)
        }
    }

    private fun cancelBuild(
        startUser: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        channel: String
    ): Boolean {
        var successFlag = true
        val client = SpringContextUtil.getBean(Client::class.java)
        try {
            // 设置该次取消service接口为放行状态
            BkApiUtil.setPermissionFlag(true)
            // 强制取消流水线构建
            val shutdownResult = client.get(ServiceBuildResource::class).manualShutdown(
                userId = startUser,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                channelCode = ChannelCode.getChannel(channel) ?: ChannelCode.BS,
                terminateFlag = true
            )
            if (shutdownResult.isNotOk()) {
                logger.warn("project[$projectId]-pipelineId[$pipelineId]-buildId[$buildId] cancel fail")
                successFlag = false
            }
        } catch (ignored: Throwable) {
            logger.warn(
                "project[$projectId]-pipelineId[$pipelineId]-buildId[$buildId] cancel fail",
                ignored
            )
            successFlag = false
        }
        return successFlag
    }

    /**
     * 校验流水线构建是否已完成（用于数据迁移前的状态校验）
     *
     * @param dslContext 数据库操作上下文
     * @param processDao 流水线构建数据访问对象
     * @param projectId 项目ID
     * @param pipelineId 流水线ID
     *
     * @throws ErrorCodeException 当存在运行中的构建或已完成的成功阶段时抛出异常
     */
    private fun isBuildCompleted(
        dslContext: DSLContext,
        processDao: ProcessDao,
        projectId: String,
        pipelineId: String
    ) {
        // 在事务中执行状态校验
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)

            // 统计处于运行状态的构建数量
            val runningCount = processDao.countAllBuildWithStatus(
                dslContext = transactionContext,
                projectId = projectId,
                pipelineId = pipelineId,
                status = setOf(
                    BuildStatus.QUEUE,
                    BuildStatus.QUEUE_CACHE,
                    BuildStatus.RUNNING
                )
            )

            // 存在运行中的构建时禁止迁移
            if (runningCount > 0) {
                throw ErrorCodeException(
                    errorCode = MiscMessageCode.ERROR_MIGRATING_PIPELINE_STATUS_INVALID,
                    defaultMessage = I18nUtil.getCodeLanMessage(
                        messageCode = MiscMessageCode.ERROR_MIGRATING_PIPELINE_STATUS_INVALID
                    )
                )
            }
        }
    }

    private fun executeMigration(
        dslContext: DSLContext,
        migratingShardingDslContext: DSLContext,
        migrationStrategyFactory: MigrationStrategyFactory,
        projectId: String,
        pipelineId: String,
        archiveFlag: Boolean? = null
    ) {
        val migrationContext = MigrationContext(
            dslContext = dslContext,
            migratingShardingDslContext = migratingShardingDslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            archiveFlag = archiveFlag
        )

        migrationStrategyFactory.getPipelineMigrationStrategies().forEach { strategy ->
            strategy.migrate(migrationContext)
        }
    }
}

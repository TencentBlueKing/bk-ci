package com.tencent.devops.misc.task

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.misc.dao.process.ProcessDbMigrateDao
import com.tencent.devops.misc.pojo.process.MigratePipelineDataParam
import com.tencent.devops.model.process.tables.TPipelineBuildHistory
import com.tencent.devops.model.process.tables.records.TPipelineBuildHistoryRecord
import com.tencent.devops.process.api.service.ServiceBuildResource
import org.apache.commons.collections4.ListUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory

@Suppress("TooManyFunctions", "LongMethod", "LargeClass")
class MigratePipelineDataTask constructor(
    private val migratePipelineDataParam: MigratePipelineDataParam
) : Runnable {

    companion object {
        private val logger = LoggerFactory.getLogger(MigratePipelineDataTask::class.java)
        private const val DEFAULT_PAGE_SIZE = 20
        private const val SHORT_PAGE_SIZE = 3
        private const val MEDIUM_PAGE_SIZE = 100
        private const val LONG_PAGE_SIZE = 1000
        private const val RETRY_NUM = 3
        private const val DEFAULT_THREAD_SLEEP_TINE = 25000L
    }
        override fun run() {
            val semaphore = migratePipelineDataParam.semaphore
            val projectId = migratePipelineDataParam.projectId
            val pipelineId = migratePipelineDataParam.pipelineId
            val cancelFlag = migratePipelineDataParam.cancelFlag
            val doneSignal = migratePipelineDataParam.doneSignal
            val dslContext = migratePipelineDataParam.dslContext
            val migratingShardingDslContext = migratePipelineDataParam.migratingShardingDslContext
            val processDbMigrateDao = migratePipelineDataParam.processDbMigrateDao
            // 1、获取是否允许执行的信号量
            semaphore.acquire()
            logger.info("migrateProjectData project[$projectId],pipeline[$pipelineId] start..............")
            try {
                if (cancelFlag) {
                    // 2、取消未结束的构建
                    handleUnFinishPipelines(RETRY_NUM)
                }
                // 3、开始迁移流水线的数据
                // 3.1、迁移T_PIPELINE_BUILD_CONTAINER表数据
                migratePipelineBuildContainerData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDbMigrateDao = processDbMigrateDao
                )
                // 3.2、迁移构建相关表数据
                var offset = 0
                do {
                    val buildHistoryRecords = processDbMigrateDao.getPipelineBuildHistoryRecords(
                        dslContext = dslContext,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        limit = MEDIUM_PAGE_SIZE,
                        offset = offset
                    )
                    migrateBuildLinkedData(
                        buildHistoryRecords,
                        processDbMigrateDao,
                        dslContext,
                        projectId,
                        migratingShardingDslContext
                    )
                    processDbMigrateDao.migratePipelineBuildHistoryData(
                        migratingShardingDslContext = migratingShardingDslContext,
                        pipelineBuildHistoryRecords = buildHistoryRecords
                    )
                    offset += MEDIUM_PAGE_SIZE
                } while (buildHistoryRecords.size == MEDIUM_PAGE_SIZE)
                // 3.3、迁移T_PIPELINE_BUILD_STAGE表数据
                migratePipelineBuildStageData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDbMigrateDao = processDbMigrateDao
                )
                // 3.4、迁移T_PIPELINE_BUILD_TASK表数据
                migratePipelineBuildTaskData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDbMigrateDao = processDbMigrateDao
                )
                // 3.5、迁移T_PIPELINE_FAVOR表数据
                migratePipelineFavorData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDbMigrateDao = processDbMigrateDao
                )
                // 3.6、迁移T_PIPELINE_BUILD_SUMMARY表数据
                migratePipelineBuildSummaryData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDbMigrateDao = processDbMigrateDao
                )
                // 3.7、迁移T_PIPELINE_INFO表数据
                migratePipelineInfoData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDbMigrateDao = processDbMigrateDao
                )
                // 3.8、迁移T_PIPELINE_LABEL_PIPELINE表数据
                migratePipelineLabelPipelineData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDbMigrateDao = processDbMigrateDao
                )
                // 3.9、迁移T_PIPELINE_MODEL_TASK表数据
                migratePipelineModelTaskData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDbMigrateDao = processDbMigrateDao
                )
                // 3.10、迁移T_PIPELINE_RESOURCE表数据
                migratePipelineResourceData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDbMigrateDao = processDbMigrateDao
                )
                // 3.11、迁移T_PIPELINE_RESOURCE_VERSION表数据
                migratePipelineResourceVersionData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDbMigrateDao = processDbMigrateDao
                )
                // 3.12、迁移T_PIPELINE_SETTING表数据
                migratePipelineSettingData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDbMigrateDao = processDbMigrateDao
                )
                // 3.13、迁移T_PIPELINE_SETTING_VERSION表数据
                migratePipelineSettingVersionData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDbMigrateDao = processDbMigrateDao
                )
                // 3.14、迁移T_PIPELINE_WEBHOOK_BUILD_LOG_DETAIL表数据
                migratePipelineWebhookBuildLogDetailData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDbMigrateDao = processDbMigrateDao
                )
                // 3.15、迁移T_PIPELINE_WEBHOOK_QUEUE表数据
                migratePipelineWebhookQueueData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDbMigrateDao = processDbMigrateDao
                )
                // 3.16、迁移T_REPORT表数据
                migrateReportData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDbMigrateDao = processDbMigrateDao
                )
                // 3.17、迁移T_PIPELINE_BUILD_TEMPLATE_ACROSS_INFO表数据
                migrateBuildTemplateAcrossInfoData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDbMigrateDao = processDbMigrateDao
                )
                // 4、业务逻辑成功执行完后计数器减1
                doneSignal.countDown()
                logger.info("migrateProjectData project[$projectId],pipeline[$pipelineId] end..............")
            } finally {
                // 5、业务逻辑执行完成后释放信号量
                semaphore.release()
            }
        }

    private fun migrateBuildLinkedData(
        buildHistoryRecords: List<TPipelineBuildHistoryRecord>,
        processDbMigrateDao: ProcessDbMigrateDao,
        dslContext: DSLContext,
        projectId: String,
        migratingShardingDslContext: DSLContext
    ) {
        val buildIds = buildHistoryRecords.map { it.buildId }
        // 由于detail表的流水线模型字段可能比较大，故一次迁移3条记录
        ListUtils.partition(buildIds, SHORT_PAGE_SIZE).forEach { rids ->
            // 3.2.1、迁移T_PIPELINE_BUILD_DETAIL相关表数据
            val buildDetailRecords = processDbMigrateDao.getPipelineBuildDetailRecords(
                dslContext = dslContext,
                projectId = projectId,
                buildIds = rids
            )
            if (buildDetailRecords.isNotEmpty()) {
                processDbMigrateDao.migratePipelineBuildDetailData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    pipelineBuildDetailRecords = buildDetailRecords
                )
            }
            // 3.2.2、迁移T_PIPELINE_BUILD_VAR相关表数据
            val buildVarRecords = processDbMigrateDao.getPipelineBuildVarRecords(
                dslContext = dslContext,
                projectId = projectId,
                buildIds = rids
            )
            if (buildVarRecords.isNotEmpty()) {
                processDbMigrateDao.migratePipelineBuildVarData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    pipelineBuildVarRecords = buildVarRecords
                )
            }
            // 3.2.3、迁移T_PIPELINE_PAUSE_VALUE相关表数据
            val pipelinePauseValueRecords = processDbMigrateDao.getPipelinePauseValueRecords(
                dslContext = dslContext,
                projectId = projectId,
                buildIds = rids
            )
            if (pipelinePauseValueRecords.isNotEmpty()) {
                processDbMigrateDao.migratePipelinePauseValueData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    pipelinePauseValueRecords = pipelinePauseValueRecords
                )
            }
            // 3.2.4、迁移T_PIPELINE_WEBHOOK_BUILD_PARAMETER相关表数据
            val webhookBuildParameterRecords = processDbMigrateDao.getPipelineWebhookBuildParameterRecords(
                dslContext = dslContext,
                projectId = projectId,
                buildIds = rids
            )
            if (webhookBuildParameterRecords.isNotEmpty()) {
                processDbMigrateDao.migratePipelineWebhookBuildParameterData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    webhookBuildParameterRecords = webhookBuildParameterRecords
                )
            }
            // 3.2.5、迁移T_PIPELINE_BUILD_RECORD_CONTAINER相关表数据
            val buildRecordContainerRecords = processDbMigrateDao.getPipelineBuildRecordContainerRecords(
                dslContext = dslContext,
                projectId = projectId,
                buildIds = rids
            )
            if (buildRecordContainerRecords.isNotEmpty()) {
                processDbMigrateDao.migratePipelineBuildRecordContainerData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    buildRecordContainerRecords = buildRecordContainerRecords
                )
            }
            // 3.2.6、迁移T_PIPELINE_BUILD_RECORD_MODEL相关表数据
            val buildRecordModelRecords = processDbMigrateDao.getPipelineBuildRecordModelRecords(
                dslContext = dslContext,
                projectId = projectId,
                buildIds = rids
            )
            if (buildRecordModelRecords.isNotEmpty()) {
                processDbMigrateDao.migratePipelineBuildRecordModelData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    buildRecordModelRecords = buildRecordModelRecords
                )
            }
            // 3.2.7、迁移T_PIPELINE_BUILD_RECORD_STAGE相关表数据
            val buildRecordStageRecords = processDbMigrateDao.getPipelineBuildRecordStageRecords(
                dslContext = dslContext,
                projectId = projectId,
                buildIds = rids
            )
            if (buildRecordStageRecords.isNotEmpty()) {
                processDbMigrateDao.migratePipelineBuildRecordStageData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    buildRecordStageRecords = buildRecordStageRecords
                )
            }
            // 3.2.8、迁移T_PIPELINE_BUILD_RECORD_TASK相关表数据
            val buildRecordTaskRecords = processDbMigrateDao.getPipelineBuildRecordTaskRecords(
                dslContext = dslContext,
                projectId = projectId,
                buildIds = rids
            )
            if (buildRecordTaskRecords.isNotEmpty()) {
                processDbMigrateDao.migratePipelineBuildRecordTaskData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    buildRecordTaskRecords = buildRecordTaskRecords
                )
            }
        }
    }

    private fun migratePipelineBuildContainerData(
        projectId: String,
        pipelineId: String,
        dslContext: DSLContext,
        migratingShardingDslContext: DSLContext,
        processDbMigrateDao: ProcessDbMigrateDao
    ) {
        var offset = 0
        do {
            val buildContainerRecords = processDbMigrateDao.getPipelineBuildContainerRecords(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                limit = MEDIUM_PAGE_SIZE,
                offset = offset
            )
            if (buildContainerRecords.isNotEmpty()) {
                processDbMigrateDao.migratePipelineBuildContainerData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    pipelineBuildContainerRecords = buildContainerRecords
                )
            }
            offset += MEDIUM_PAGE_SIZE
        } while (buildContainerRecords.size == MEDIUM_PAGE_SIZE)
    }

    private fun migratePipelineBuildStageData(
        projectId: String,
        pipelineId: String,
        dslContext: DSLContext,
        migratingShardingDslContext: DSLContext,
        processDbMigrateDao: ProcessDbMigrateDao
    ) {
        var offset = 0
        do {
            val buildStageRecords = processDbMigrateDao.getPipelineBuildStageRecords(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                limit = MEDIUM_PAGE_SIZE,
                offset = offset
            )
            if (buildStageRecords.isNotEmpty()) {
                processDbMigrateDao.migratePipelineBuildStageData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    pipelineBuildStageRecords = buildStageRecords
                )
            }
            offset += MEDIUM_PAGE_SIZE
        } while (buildStageRecords.size == MEDIUM_PAGE_SIZE)
    }

    private fun migratePipelineBuildTaskData(
        projectId: String,
        pipelineId: String,
        dslContext: DSLContext,
        migratingShardingDslContext: DSLContext,
        processDbMigrateDao: ProcessDbMigrateDao
    ) {
        var offset = 0
        do {
            val buildTaskRecords = processDbMigrateDao.getPipelineBuildTaskRecords(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                limit = MEDIUM_PAGE_SIZE,
                offset = offset
            )
            if (buildTaskRecords.isNotEmpty()) {
                processDbMigrateDao.migratePipelineBuildTaskData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    pipelineBuildTaskRecords = buildTaskRecords
                )
            }
            offset += MEDIUM_PAGE_SIZE
        } while (buildTaskRecords.size == MEDIUM_PAGE_SIZE)
    }

    private fun migratePipelineFavorData(
        projectId: String,
        pipelineId: String,
        dslContext: DSLContext,
        migratingShardingDslContext: DSLContext,
        processDbMigrateDao: ProcessDbMigrateDao
    ) {
        var offset = 0
        do {
            val pipelineFavorRecords = processDbMigrateDao.getPipelineFavorRecords(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                limit = LONG_PAGE_SIZE,
                offset = offset
            )
            if(pipelineFavorRecords.isNotEmpty()) {
                processDbMigrateDao.migratePipelineFavorData(migratingShardingDslContext, pipelineFavorRecords)
            }
            offset += LONG_PAGE_SIZE
        } while (pipelineFavorRecords.size == LONG_PAGE_SIZE)
    }

    private fun migratePipelineBuildSummaryData(
        projectId: String,
        pipelineId: String,
        dslContext: DSLContext,
        migratingShardingDslContext: DSLContext,
        processDbMigrateDao: ProcessDbMigrateDao
    ) {
        val buildSummaryRecord = processDbMigrateDao.getPipelineBuildSummaryRecord(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
        if (buildSummaryRecord != null) {
            processDbMigrateDao.migratePipelineBuildSummaryData(
                migratingShardingDslContext = migratingShardingDslContext,
                buildSummaryRecord = buildSummaryRecord
            )
        }
    }

    private fun migratePipelineInfoData(
        projectId: String,
        pipelineId: String,
        dslContext: DSLContext,
        migratingShardingDslContext: DSLContext,
        processDbMigrateDao: ProcessDbMigrateDao
    ) {
        val pipelineInfoRecord = processDbMigrateDao.getPipelineInfoRecord(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
        if (pipelineInfoRecord != null) {
            processDbMigrateDao.migratePipelineInfoData(
                migratingShardingDslContext = migratingShardingDslContext,
                pipelineInfoRecord = pipelineInfoRecord
            )
        }
    }

    private fun migratePipelineLabelPipelineData(
        projectId: String,
        pipelineId: String,
        dslContext: DSLContext,
        migratingShardingDslContext: DSLContext,
        processDbMigrateDao: ProcessDbMigrateDao
    ) {
        var offset = 0
        do {
            val pipelineLabelPipelineRecords = processDbMigrateDao.getPipelineLabelPipelineRecords(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                limit = LONG_PAGE_SIZE,
                offset = offset
            )
            if (pipelineLabelPipelineRecords.isNotEmpty()) {
                processDbMigrateDao.migratePipelineLabelPipelineData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    pipelineLabelPipelineRecords = pipelineLabelPipelineRecords
                )
            }
            offset += LONG_PAGE_SIZE
        } while (pipelineLabelPipelineRecords.size == LONG_PAGE_SIZE)
    }

    private fun migratePipelineModelTaskData(
        projectId: String,
        pipelineId: String,
        dslContext: DSLContext,
        migratingShardingDslContext: DSLContext,
        processDbMigrateDao: ProcessDbMigrateDao
    ) {
        val pipelineModelTaskRecords = processDbMigrateDao.getPipelineModelTaskRecords(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
        if (pipelineModelTaskRecords.isNotEmpty()) {
            processDbMigrateDao.migratePipelineModelTaskData(
                migratingShardingDslContext = migratingShardingDslContext,
                pipelineModelTaskRecords = pipelineModelTaskRecords
            )
        }
    }

    private fun migratePipelineResourceData(
        projectId: String,
        pipelineId: String,
        dslContext: DSLContext,
        migratingShardingDslContext: DSLContext,
        processDbMigrateDao: ProcessDbMigrateDao
    ) {
        val pipelineResourceRecord = processDbMigrateDao.getPipelineResourceRecord(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
        if (pipelineResourceRecord != null) {
            processDbMigrateDao.migratePipelineResourceData(
                migratingShardingDslContext = migratingShardingDslContext,
                pipelineResourceRecord = pipelineResourceRecord
            )
        }
    }

    private fun migratePipelineResourceVersionData(
        projectId: String,
        pipelineId: String,
        dslContext: DSLContext,
        migratingShardingDslContext: DSLContext,
        processDbMigrateDao: ProcessDbMigrateDao
    ) {
        var offset = 0
        do {
            val pipelineResourceVersionRecords = processDbMigrateDao.getPipelineResourceVersionRecords(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                limit = SHORT_PAGE_SIZE,
                offset = offset
            )
            if (pipelineResourceVersionRecords.isNotEmpty()) {
                processDbMigrateDao.migratePipelineResourceVersionData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    pipelineResourceVersionRecords = pipelineResourceVersionRecords
                )
            }
            offset += SHORT_PAGE_SIZE
        } while (pipelineResourceVersionRecords.size == SHORT_PAGE_SIZE)
    }

    private fun migratePipelineSettingData(
        projectId: String,
        pipelineId: String,
        dslContext: DSLContext,
        migratingShardingDslContext: DSLContext,
        processDbMigrateDao: ProcessDbMigrateDao
    ) {
        val pipelineSettingRecord = processDbMigrateDao.getPipelineSettingRecord(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
        if (pipelineSettingRecord != null) {
            processDbMigrateDao.migratePipelineSettingData(
                migratingShardingDslContext = migratingShardingDslContext,
                pipelineSettingRecord = pipelineSettingRecord
            )
        }
    }

    private fun migratePipelineSettingVersionData(
        projectId: String,
        pipelineId: String,
        dslContext: DSLContext,
        migratingShardingDslContext: DSLContext,
        processDbMigrateDao: ProcessDbMigrateDao
    ) {
        var offset = 0
        do {
            val pipelineSettingVersionRecords = processDbMigrateDao.getPipelineSettingVersionRecords(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                limit = SHORT_PAGE_SIZE,
                offset = offset
            )
            if (pipelineSettingVersionRecords.isNotEmpty()) {
                processDbMigrateDao.migratePipelineSettingVersionData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    pipelineSettingVersionRecords = pipelineSettingVersionRecords
                )
            }
            offset += SHORT_PAGE_SIZE
        } while (pipelineSettingVersionRecords.size == SHORT_PAGE_SIZE)
    }

    private fun migratePipelineWebhookBuildLogDetailData(
        projectId: String,
        pipelineId: String,
        dslContext: DSLContext,
        migratingShardingDslContext: DSLContext,
        processDbMigrateDao: ProcessDbMigrateDao
    ) {
        var offset = 0
        do {
            val webhookBuildLogDetailRecords = processDbMigrateDao.getPipelineWebhookBuildLogDetailRecords(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                limit = SHORT_PAGE_SIZE,
                offset = offset
            )
            if (webhookBuildLogDetailRecords.isNotEmpty()) {
                processDbMigrateDao.migratePipelineWebhookBuildLogDetailData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    webhookBuildLogDetailRecords = webhookBuildLogDetailRecords
                )
            }
            offset += SHORT_PAGE_SIZE
        } while (webhookBuildLogDetailRecords.size == SHORT_PAGE_SIZE)
    }

    private fun migratePipelineWebhookQueueData(
        projectId: String,
        pipelineId: String,
        dslContext: DSLContext,
        migratingShardingDslContext: DSLContext,
        processDbMigrateDao: ProcessDbMigrateDao
    ) {
        var offset = 0
        do {
            val webhookQueueRecords = processDbMigrateDao.getPipelineWebhookQueueRecords(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                limit = LONG_PAGE_SIZE,
                offset = offset
            )
            if (webhookQueueRecords.isNotEmpty()) {
                processDbMigrateDao.migratePipelineWebhookQueueData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    webhookQueueRecords = webhookQueueRecords
                )
            }
            offset += LONG_PAGE_SIZE
        } while (webhookQueueRecords.size == LONG_PAGE_SIZE)
    }

    private fun migrateReportData(
        projectId: String,
        pipelineId: String,
        dslContext: DSLContext,
        migratingShardingDslContext: DSLContext,
        processDbMigrateDao: ProcessDbMigrateDao
    ) {
        var offset = 0
        do {
            val reportRecords = processDbMigrateDao.getReportRecords(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                limit = MEDIUM_PAGE_SIZE,
                offset = offset
            )
            if (reportRecords.isNotEmpty()) {
                processDbMigrateDao.migrateReportData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    reportRecords = reportRecords
                )
            }
            offset += MEDIUM_PAGE_SIZE
        } while (reportRecords.size == MEDIUM_PAGE_SIZE)
    }

    private fun migrateBuildTemplateAcrossInfoData(
        projectId: String,
        pipelineId: String,
        dslContext: DSLContext,
        migratingShardingDslContext: DSLContext,
        processDbMigrateDao: ProcessDbMigrateDao
    ) {
        var offset = 0
        do {
            val buildTemplateAcrossInfoRecords = processDbMigrateDao.getBuildTemplateAcrossInfoRecords(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                limit = MEDIUM_PAGE_SIZE,
                offset = offset
            )
            if (buildTemplateAcrossInfoRecords.isNotEmpty()) {
                processDbMigrateDao.migrateBuildTemplateAcrossInfoData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    buildTemplateAcrossInfoRecords = buildTemplateAcrossInfoRecords
                )
            }
            offset += MEDIUM_PAGE_SIZE
        } while (buildTemplateAcrossInfoRecords.size == MEDIUM_PAGE_SIZE)
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
                            logger.warn("project[$projectId]-pipelineId[$pipelineId]-buildId[$buildId] cancel fail")
                            retryFlag = true
                        }
                    } catch (ignored: Throwable) {
                        logger.warn(
                            "project[$projectId]-pipelineId[$pipelineId]-buildId[$buildId] cancel fail",
                            ignored
                        )
                        retryFlag = true
                    }
                }
                offset += DEFAULT_PAGE_SIZE
            } while (historyInfoRecords?.size == DEFAULT_PAGE_SIZE)
            if (retryFlag) {
                if (retryNum > 0) {
                    Thread.sleep(DEFAULT_THREAD_SLEEP_TINE)
                    // 重试取消动作
                    handleUnFinishPipelines(retryNum - 1)
                }
            }
        }
    }

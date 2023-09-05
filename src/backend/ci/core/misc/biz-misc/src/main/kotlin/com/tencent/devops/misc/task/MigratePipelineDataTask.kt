package com.tencent.devops.misc.task

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
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
            val processDbMigrateDao = migratePipelineDataParam.processDataMigrateDao
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
                    processDataMigrateDao = processDbMigrateDao
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
                    processDataMigrateDao = processDbMigrateDao
                )
                // 3.4、迁移T_PIPELINE_BUILD_TASK表数据
                migratePipelineBuildTaskData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDataMigrateDao = processDbMigrateDao
                )
                // 3.5、迁移T_PIPELINE_FAVOR表数据
                migratePipelineFavorData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDataMigrateDao = processDbMigrateDao
                )
                // 3.6、迁移T_PIPELINE_BUILD_SUMMARY表数据
                migratePipelineBuildSummaryData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDataMigrateDao = processDbMigrateDao
                )
                // 3.7、迁移T_PIPELINE_INFO表数据
                migratePipelineInfoData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDataMigrateDao = processDbMigrateDao
                )
                // 3.8、迁移T_PIPELINE_LABEL_PIPELINE表数据
                migratePipelineLabelPipelineData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDataMigrateDao = processDbMigrateDao
                )
                // 3.9、迁移T_PIPELINE_MODEL_TASK表数据
                migratePipelineModelTaskData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDataMigrateDao = processDbMigrateDao
                )
                // 3.10、迁移T_PIPELINE_RESOURCE表数据
                migratePipelineResourceData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDataMigrateDao = processDbMigrateDao
                )
                // 3.11、迁移T_PIPELINE_RESOURCE_VERSION表数据
                migratePipelineResourceVersionData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDataMigrateDao = processDbMigrateDao
                )
                // 3.12、迁移T_PIPELINE_SETTING表数据
                migratePipelineSettingData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDataMigrateDao = processDbMigrateDao
                )
                // 3.13、迁移T_PIPELINE_SETTING_VERSION表数据
                migratePipelineSettingVersionData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDataMigrateDao = processDbMigrateDao
                )
                // 3.14、迁移T_PIPELINE_WEBHOOK_BUILD_LOG_DETAIL表数据
                migratePipelineWebhookBuildLogDetailData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDataMigrateDao = processDbMigrateDao
                )
                // 3.15、迁移T_PIPELINE_WEBHOOK_QUEUE表数据
                migratePipelineWebhookQueueData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDataMigrateDao = processDbMigrateDao
                )
                // 3.16、迁移T_REPORT表数据
                migrateReportData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDataMigrateDao = processDbMigrateDao
                )
                // 3.17、迁移T_PIPELINE_BUILD_TEMPLATE_ACROSS_INFO表数据
                migrateBuildTemplateAcrossInfoData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDataMigrateDao = processDbMigrateDao
                )
                // 4、业务逻辑成功执行完后计数器减1
                doneSignal.countDown()
                logger.info("migrateProjectData project[$projectId],pipeline[$pipelineId] end..............")
            } catch (ignored: Throwable) {
                logger.info("migrateProjectData project[$projectId],pipeline[$pipelineId] run task fail", ignored)
            } finally {
                // 5、业务逻辑执行完成后释放信号量
                semaphore.release()
            }
        }

    private fun migrateBuildLinkedData(
        buildHistoryRecords: List<TPipelineBuildHistoryRecord>,
        processDataMigrateDao: ProcessDataMigrateDao,
        dslContext: DSLContext,
        projectId: String,
        migratingShardingDslContext: DSLContext
    ) {
        val buildIds = buildHistoryRecords.map { it.buildId }
        // 由于detail表的流水线模型字段可能比较大，故一次迁移3条记录
        ListUtils.partition(buildIds, SHORT_PAGE_SIZE).forEach { rids ->
            // 3.2.1、迁移T_PIPELINE_BUILD_DETAIL相关表数据
            val buildDetailRecords = processDataMigrateDao.getPipelineBuildDetailRecords(
                dslContext = dslContext,
                projectId = projectId,
                buildIds = rids
            )
            if (buildDetailRecords.isNotEmpty()) {
                processDataMigrateDao.migratePipelineBuildDetailData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    pipelineBuildDetailRecords = buildDetailRecords
                )
            }
            // 3.2.2、迁移T_PIPELINE_BUILD_VAR相关表数据
            val buildVarRecords = processDataMigrateDao.getPipelineBuildVarRecords(
                dslContext = dslContext,
                projectId = projectId,
                buildIds = rids
            )
            if (buildVarRecords.isNotEmpty()) {
                processDataMigrateDao.migratePipelineBuildVarData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    pipelineBuildVarRecords = buildVarRecords
                )
            }
            // 3.2.3、迁移T_PIPELINE_PAUSE_VALUE相关表数据
            val pipelinePauseValueRecords = processDataMigrateDao.getPipelinePauseValueRecords(
                dslContext = dslContext,
                projectId = projectId,
                buildIds = rids
            )
            if (pipelinePauseValueRecords.isNotEmpty()) {
                processDataMigrateDao.migratePipelinePauseValueData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    pipelinePauseValueRecords = pipelinePauseValueRecords
                )
            }
            // 3.2.4、迁移T_PIPELINE_WEBHOOK_BUILD_PARAMETER相关表数据
            val webhookBuildParameterRecords = processDataMigrateDao.getPipelineWebhookBuildParameterRecords(
                dslContext = dslContext,
                projectId = projectId,
                buildIds = rids
            )
            if (webhookBuildParameterRecords.isNotEmpty()) {
                processDataMigrateDao.migratePipelineWebhookBuildParameterData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    webhookBuildParameterRecords = webhookBuildParameterRecords
                )
            }
            // 3.2.5、迁移T_PIPELINE_BUILD_RECORD_CONTAINER相关表数据
            val buildRecordContainerRecords = processDataMigrateDao.getPipelineBuildRecordContainerRecords(
                dslContext = dslContext,
                projectId = projectId,
                buildIds = rids
            )
            if (buildRecordContainerRecords.isNotEmpty()) {
                processDataMigrateDao.migratePipelineBuildRecordContainerData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    buildRecordContainerRecords = buildRecordContainerRecords
                )
            }
            // 3.2.6、迁移T_PIPELINE_BUILD_RECORD_MODEL相关表数据
            val buildRecordModelRecords = processDataMigrateDao.getPipelineBuildRecordModelRecords(
                dslContext = dslContext,
                projectId = projectId,
                buildIds = rids
            )
            if (buildRecordModelRecords.isNotEmpty()) {
                processDataMigrateDao.migratePipelineBuildRecordModelData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    buildRecordModelRecords = buildRecordModelRecords
                )
            }
            // 3.2.7、迁移T_PIPELINE_BUILD_RECORD_STAGE相关表数据
            val buildRecordStageRecords = processDataMigrateDao.getPipelineBuildRecordStageRecords(
                dslContext = dslContext,
                projectId = projectId,
                buildIds = rids
            )
            if (buildRecordStageRecords.isNotEmpty()) {
                processDataMigrateDao.migratePipelineBuildRecordStageData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    buildRecordStageRecords = buildRecordStageRecords
                )
            }
            // 3.2.8、迁移T_PIPELINE_BUILD_RECORD_TASK相关表数据
            val buildRecordTaskRecords = processDataMigrateDao.getPipelineBuildRecordTaskRecords(
                dslContext = dslContext,
                projectId = projectId,
                buildIds = rids
            )
            if (buildRecordTaskRecords.isNotEmpty()) {
                processDataMigrateDao.migratePipelineBuildRecordTaskData(
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
        processDataMigrateDao: ProcessDataMigrateDao
    ) {
        var offset = 0
        do {
            val buildContainerRecords = processDataMigrateDao.getPipelineBuildContainerRecords(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                limit = MEDIUM_PAGE_SIZE,
                offset = offset
            )
            if (buildContainerRecords.isNotEmpty()) {
                processDataMigrateDao.migratePipelineBuildContainerData(
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
        processDataMigrateDao: ProcessDataMigrateDao
    ) {
        var offset = 0
        do {
            val buildStageRecords = processDataMigrateDao.getPipelineBuildStageRecords(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                limit = MEDIUM_PAGE_SIZE,
                offset = offset
            )
            if (buildStageRecords.isNotEmpty()) {
                processDataMigrateDao.migratePipelineBuildStageData(
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
        processDataMigrateDao: ProcessDataMigrateDao
    ) {
        var offset = 0
        do {
            val buildTaskRecords = processDataMigrateDao.getPipelineBuildTaskRecords(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                limit = MEDIUM_PAGE_SIZE,
                offset = offset
            )
            if (buildTaskRecords.isNotEmpty()) {
                processDataMigrateDao.migratePipelineBuildTaskData(
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
        processDataMigrateDao: ProcessDataMigrateDao
    ) {
        var offset = 0
        do {
            val pipelineFavorRecords = processDataMigrateDao.getPipelineFavorRecords(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                limit = LONG_PAGE_SIZE,
                offset = offset
            )
            if (pipelineFavorRecords.isNotEmpty()) {
                processDataMigrateDao.migratePipelineFavorData(migratingShardingDslContext, pipelineFavorRecords)
            }
            offset += LONG_PAGE_SIZE
        } while (pipelineFavorRecords.size == LONG_PAGE_SIZE)
    }

    private fun migratePipelineBuildSummaryData(
        projectId: String,
        pipelineId: String,
        dslContext: DSLContext,
        migratingShardingDslContext: DSLContext,
        processDataMigrateDao: ProcessDataMigrateDao
    ) {
        val buildSummaryRecord = processDataMigrateDao.getPipelineBuildSummaryRecord(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
        if (buildSummaryRecord != null) {
            processDataMigrateDao.migratePipelineBuildSummaryData(
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
        processDataMigrateDao: ProcessDataMigrateDao
    ) {
        val pipelineInfoRecord = processDataMigrateDao.getPipelineInfoRecord(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
        if (pipelineInfoRecord != null) {
            processDataMigrateDao.migratePipelineInfoData(
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
        processDataMigrateDao: ProcessDataMigrateDao
    ) {
        var offset = 0
        do {
            val pipelineLabelPipelineRecords = processDataMigrateDao.getPipelineLabelPipelineRecords(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                limit = LONG_PAGE_SIZE,
                offset = offset
            )
            if (pipelineLabelPipelineRecords.isNotEmpty()) {
                processDataMigrateDao.migratePipelineLabelPipelineData(
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
        processDataMigrateDao: ProcessDataMigrateDao
    ) {
        val pipelineModelTaskRecords = processDataMigrateDao.getPipelineModelTaskRecords(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
        if (pipelineModelTaskRecords.isNotEmpty()) {
            processDataMigrateDao.migratePipelineModelTaskData(
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
        processDataMigrateDao: ProcessDataMigrateDao
    ) {
        val pipelineResourceRecord = processDataMigrateDao.getPipelineResourceRecord(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
        if (pipelineResourceRecord != null) {
            processDataMigrateDao.migratePipelineResourceData(
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
        processDataMigrateDao: ProcessDataMigrateDao
    ) {
        var offset = 0
        do {
            val pipelineResourceVersionRecords = processDataMigrateDao.getPipelineResourceVersionRecords(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                limit = SHORT_PAGE_SIZE,
                offset = offset
            )
            if (pipelineResourceVersionRecords.isNotEmpty()) {
                processDataMigrateDao.migratePipelineResourceVersionData(
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
        processDataMigrateDao: ProcessDataMigrateDao
    ) {
        val pipelineSettingRecord = processDataMigrateDao.getPipelineSettingRecord(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
        if (pipelineSettingRecord != null) {
            processDataMigrateDao.migratePipelineSettingData(
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
        processDataMigrateDao: ProcessDataMigrateDao
    ) {
        var offset = 0
        do {
            val pipelineSettingVersionRecords = processDataMigrateDao.getPipelineSettingVersionRecords(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                limit = SHORT_PAGE_SIZE,
                offset = offset
            )
            if (pipelineSettingVersionRecords.isNotEmpty()) {
                processDataMigrateDao.migratePipelineSettingVersionData(
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
        processDataMigrateDao: ProcessDataMigrateDao
    ) {
        var offset = 0
        do {
            val webhookBuildLogDetailRecords = processDataMigrateDao.getPipelineWebhookBuildLogDetailRecords(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                limit = SHORT_PAGE_SIZE,
                offset = offset
            )
            if (webhookBuildLogDetailRecords.isNotEmpty()) {
                processDataMigrateDao.migratePipelineWebhookBuildLogDetailData(
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
        processDataMigrateDao: ProcessDataMigrateDao
    ) {
        var offset = 0
        do {
            val webhookQueueRecords = processDataMigrateDao.getPipelineWebhookQueueRecords(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                limit = LONG_PAGE_SIZE,
                offset = offset
            )
            if (webhookQueueRecords.isNotEmpty()) {
                processDataMigrateDao.migratePipelineWebhookQueueData(
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
        processDataMigrateDao: ProcessDataMigrateDao
    ) {
        var offset = 0
        do {
            val reportRecords = processDataMigrateDao.getReportRecords(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                limit = MEDIUM_PAGE_SIZE,
                offset = offset
            )
            if (reportRecords.isNotEmpty()) {
                processDataMigrateDao.migrateReportData(
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
        processDataMigrateDao: ProcessDataMigrateDao
    ) {
        var offset = 0
        do {
            val buildTemplateAcrossInfoRecords = processDataMigrateDao.getBuildTemplateAcrossInfoRecords(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                limit = MEDIUM_PAGE_SIZE,
                offset = offset
            )
            if (buildTemplateAcrossInfoRecords.isNotEmpty()) {
                processDataMigrateDao.migrateBuildTemplateAcrossInfoData(
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

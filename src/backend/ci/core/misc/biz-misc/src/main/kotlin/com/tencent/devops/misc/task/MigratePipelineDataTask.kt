package com.tencent.devops.misc.task

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.utils.BkApiUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.pojo.constant.MiscMessageCode
import com.tencent.devops.misc.pojo.process.MigratePipelineDataParam
import com.tencent.devops.model.process.tables.TPipelineBuildHistory
import com.tencent.devops.model.process.tables.records.TPipelineBuildHistoryRecord
import com.tencent.devops.process.api.service.ServiceBuildResource
import org.apache.commons.collections4.ListUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@Suppress("TooManyFunctions", "LongMethod", "LargeClass", "ComplexMethod", "LongParameterList")
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
            val processDbMigrateDao = migratePipelineDataParam.processDataMigrateDao
            val archiveFlag = migratePipelineDataParam.archiveFlag
            // 1、获取是否允许执行的信号量
            semaphore?.acquire()
            logger.info("migrateProjectData project[$projectId],pipeline[$pipelineId] start..............")
            try {
                if (cancelFlag) {
                    // 2、取消未结束的构建
                    handleUnFinishPipelines(RETRY_NUM)
                    Thread.sleep(DEFAULT_THREAD_SLEEP_TINE)
                }
                // 3、开始迁移流水线的数据
                // 3.1、迁移T_PIPELINE_INFO表数据
                migratePipelineInfoData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDataMigrateDao = processDbMigrateDao,
                    archiveFlag = archiveFlag
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
                    processDbMigrateDao.migratePipelineBuildHistoryData(
                        migratingShardingDslContext = migratingShardingDslContext,
                        pipelineBuildHistoryRecords = buildHistoryRecords
                    )
                    migrateBuildLinkedData(
                        buildHistoryRecords = buildHistoryRecords,
                        processDataMigrateDao = processDbMigrateDao,
                        dslContext = dslContext,
                        projectId = projectId,
                        migratingShardingDslContext = migratingShardingDslContext,
                        archiveFlag = archiveFlag
                    )
                    offset += MEDIUM_PAGE_SIZE
                } while (buildHistoryRecords.size == MEDIUM_PAGE_SIZE)
                // 3.3、迁移T_PIPELINE_BUILD_SUMMARY表数据
                migratePipelineBuildSummaryData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDataMigrateDao = processDbMigrateDao
                )
                // 3.4、迁移T_PIPELINE_LABEL_PIPELINE表数据
                migratePipelineLabelPipelineData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDataMigrateDao = processDbMigrateDao
                )
                // 3.5、迁移T_PIPELINE_RESOURCE表数据
                migratePipelineResourceData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDataMigrateDao = processDbMigrateDao
                )
                // 3.6、迁移T_PIPELINE_RESOURCE_VERSION表数据
                migratePipelineResourceVersionData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDataMigrateDao = processDbMigrateDao
                )
                // 3.7、迁移T_TEMPLATE_PIPELINE表数据
                migrateTemplatePipelineData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    dslContext = dslContext,
                    migratingShardingDslContext = migratingShardingDslContext,
                    processDataMigrateDao = processDbMigrateDao
                )
                if (archiveFlag != true) {
                    // 3.8、迁移T_PIPELINE_BUILD_CONTAINER表数据
                    migratePipelineBuildContainerData(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        dslContext = dslContext,
                        migratingShardingDslContext = migratingShardingDslContext,
                        processDataMigrateDao = processDbMigrateDao
                    )
                    // 3.9、迁移T_PIPELINE_BUILD_STAGE表数据
                    migratePipelineBuildStageData(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        dslContext = dslContext,
                        migratingShardingDslContext = migratingShardingDslContext,
                        processDataMigrateDao = processDbMigrateDao
                    )
                    // 3.10、迁移T_PIPELINE_BUILD_TASK表数据
                    migratePipelineBuildTaskData(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        dslContext = dslContext,
                        migratingShardingDslContext = migratingShardingDslContext,
                        processDataMigrateDao = processDbMigrateDao
                    )
                    // 3.11、迁移T_PIPELINE_FAVOR表数据
                    migratePipelineFavorData(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        dslContext = dslContext,
                        migratingShardingDslContext = migratingShardingDslContext,
                        processDataMigrateDao = processDbMigrateDao
                    )
                    // 3.12、迁移T_PIPELINE_MODEL_TASK表数据
                    migratePipelineModelTaskData(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        dslContext = dslContext,
                        migratingShardingDslContext = migratingShardingDslContext,
                        processDataMigrateDao = processDbMigrateDao
                    )
                    // 3.13、迁移T_PIPELINE_SETTING表数据
                    migratePipelineSettingData(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        dslContext = dslContext,
                        migratingShardingDslContext = migratingShardingDslContext,
                        processDataMigrateDao = processDbMigrateDao
                    )
                    // 3.14、迁移T_PIPELINE_SETTING_VERSION表数据
                    migratePipelineSettingVersionData(
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
                    // 3.18、迁移T_PIPELINE_RECENT_USE表数据
                    migratePipelineRecentUseData(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        dslContext = dslContext,
                        migratingShardingDslContext = migratingShardingDslContext,
                        processDataMigrateDao = processDbMigrateDao
                    )
                    // 3.19、迁移T_PIPELINE_VIEW_GROUP表数据
                    migratePipelineViewGroupData(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        dslContext = dslContext,
                        migratingShardingDslContext = migratingShardingDslContext,
                        processDataMigrateDao = processDbMigrateDao
                    )
                    // 3.20、迁移T_PIPELINE_TIMER表数据
                    migratePipelineTimerData(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        dslContext = dslContext,
                        migratingShardingDslContext = migratingShardingDslContext,
                        processDataMigrateDao = processDbMigrateDao
                    )
                    // 3.21、迁移T_PIPELINE_TRIGGER_DETAIL表数据
                    migratePipelineTriggerDetailData(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        dslContext = dslContext,
                        migratingShardingDslContext = migratingShardingDslContext,
                        processDataMigrateDao = processDbMigrateDao
                    )
                    // 3.22、迁移T_PIPELINE_REMOTE_AUTH表数据
                    migratePipelineRemoteAuthData(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        dslContext = dslContext,
                        migratingShardingDslContext = migratingShardingDslContext,
                        processDataMigrateDao = processDbMigrateDao
                    )
                    // 3.23、迁移T_PIPELINE_WEBHOOK表数据
                    migratePipelineWebhookData(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        dslContext = dslContext,
                        migratingShardingDslContext = migratingShardingDslContext,
                        processDataMigrateDao = processDbMigrateDao
                    )
                    if (doneSignal == null) {
                        // 单独迁移一条流水线的数据时需要执行以下数据迁移逻辑
                        migratePipelineAuditResourceData(
                            projectId = projectId,
                            pipelineId = pipelineId,
                            dslContext = dslContext,
                            migratingShardingDslContext = migratingShardingDslContext,
                            processDataMigrateDao = processDbMigrateDao
                        )
                    }
                }
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

    private fun migrateBuildLinkedData(
        buildHistoryRecords: List<TPipelineBuildHistoryRecord>,
        processDataMigrateDao: ProcessDataMigrateDao,
        dslContext: DSLContext,
        projectId: String,
        migratingShardingDslContext: DSLContext,
        archiveFlag: Boolean? = null
    ) {
        val buildIds = buildHistoryRecords.map { it.buildId }
        // 由于detail表的流水线模型字段可能比较大，故一次迁移3条记录
        ListUtils.partition(buildIds, SHORT_PAGE_SIZE).forEach { rids ->
            if (archiveFlag != true) {
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
                // 3.2.5、迁移T_PIPELINE_TRIGGER_REVIEW相关表数据
                val pipelineTriggerReviewRecords = processDataMigrateDao.getPipelineTriggerReviewRecords(
                    dslContext = dslContext,
                    projectId = projectId,
                    buildIds = rids
                )
                if (pipelineTriggerReviewRecords.isNotEmpty()) {
                    processDataMigrateDao.migratePipelineTriggerReviewData(
                        migratingShardingDslContext = migratingShardingDslContext,
                        pipelineTriggerReviewRecords = pipelineTriggerReviewRecords
                    )
                }
            }
            // 3.2.6、迁移T_PIPELINE_BUILD_RECORD_CONTAINER相关表数据
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
            // 3.2.7、迁移T_PIPELINE_BUILD_RECORD_MODEL相关表数据
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
            // 3.2.8、迁移T_PIPELINE_BUILD_RECORD_STAGE相关表数据
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
            // 3.2.9、迁移T_PIPELINE_BUILD_RECORD_TASK相关表数据
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
        processDataMigrateDao: ProcessDataMigrateDao,
        archiveFlag: Boolean? = null
    ) {
        val pipelineInfoRecord = processDataMigrateDao.getPipelineInfoRecord(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
        if (pipelineInfoRecord != null) {
            if (archiveFlag == true) {
                val currentTime = DateTimeUtil.toDateTime(LocalDateTime.now(), DateTimeUtil.YYYYMMDDHHMMSS)
                pipelineInfoRecord.pipelineName = "${pipelineInfoRecord.pipelineName}[$currentTime]"
            }
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

    private fun migratePipelineRecentUseData(
        projectId: String,
        pipelineId: String,
        dslContext: DSLContext,
        migratingShardingDslContext: DSLContext,
        processDataMigrateDao: ProcessDataMigrateDao
    ) {
        var offset = 0
        do {
            val pipelineRecentUseRecords = processDataMigrateDao.getPipelineRecentUseRecords(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                limit = LONG_PAGE_SIZE,
                offset = offset
            )
            if (pipelineRecentUseRecords.isNotEmpty()) {
                processDataMigrateDao.migratePipelineRecentUseData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    pipelineRecentUseRecords = pipelineRecentUseRecords
                )
            }
            offset += LONG_PAGE_SIZE
        } while (pipelineRecentUseRecords.size == LONG_PAGE_SIZE)
    }

    private fun migratePipelineViewGroupData(
        projectId: String,
        pipelineId: String,
        dslContext: DSLContext,
        migratingShardingDslContext: DSLContext,
        processDataMigrateDao: ProcessDataMigrateDao
    ) {
        var offset = 0
        do {
            val pipelineViewGroupRecords = processDataMigrateDao.getPipelineViewGroupRecords(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                limit = LONG_PAGE_SIZE,
                offset = offset
            )
            if (pipelineViewGroupRecords.isNotEmpty()) {
                processDataMigrateDao.migratePipelineViewGroupData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    pipelineViewGroupRecords = pipelineViewGroupRecords
                )
            }
            offset += LONG_PAGE_SIZE
        } while (pipelineViewGroupRecords.size == LONG_PAGE_SIZE)
    }

    private fun migratePipelineRemoteAuthData(
        projectId: String,
        pipelineId: String,
        dslContext: DSLContext,
        migratingShardingDslContext: DSLContext,
        processDataMigrateDao: ProcessDataMigrateDao
    ) {
        val pipelineRemoteAuthRecord = processDataMigrateDao.getPipelineRemoteAuthRecord(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
        if (pipelineRemoteAuthRecord != null) {
            processDataMigrateDao.migratePipelineRemoteAuthData(
                migratingShardingDslContext = migratingShardingDslContext,
                pipelineRemoteAuthRecord = pipelineRemoteAuthRecord
            )
        }
    }

    private fun migratePipelineAuditResourceData(
        projectId: String,
        pipelineId: String,
        dslContext: DSLContext,
        migratingShardingDslContext: DSLContext,
        processDataMigrateDao: ProcessDataMigrateDao
    ) {
        var offset = 0
        do {
            val auditResourceRecords = processDataMigrateDao.getAuditResourceRecords(
                dslContext = dslContext,
                projectId = projectId,
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceId = pipelineId,
                limit = MEDIUM_PAGE_SIZE,
                offset = offset
            )
            if (auditResourceRecords.isNotEmpty()) {
                processDataMigrateDao.migrateAuditResourceData(migratingShardingDslContext, auditResourceRecords)
            }
            offset += MEDIUM_PAGE_SIZE
        } while (auditResourceRecords.size == MEDIUM_PAGE_SIZE)
    }

    private fun migratePipelineWebhookData(
        projectId: String,
        pipelineId: String,
        dslContext: DSLContext,
        migratingShardingDslContext: DSLContext,
        processDataMigrateDao: ProcessDataMigrateDao
    ) {
        var offset = 0
        do {
            val pipelineWebhookRecords = processDataMigrateDao.getPipelineWebhookRecords(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                limit = MEDIUM_PAGE_SIZE,
                offset = offset
            )
            if (pipelineWebhookRecords.isNotEmpty()) {
                processDataMigrateDao.migratePipelineWebhookData(migratingShardingDslContext, pipelineWebhookRecords)
            }
            offset += MEDIUM_PAGE_SIZE
        } while (pipelineWebhookRecords.size == MEDIUM_PAGE_SIZE)
    }

    private fun migrateTemplatePipelineData(
        projectId: String,
        pipelineId: String,
        dslContext: DSLContext,
        migratingShardingDslContext: DSLContext,
        processDataMigrateDao: ProcessDataMigrateDao
    ) {
        val tTemplatePipelineRecord = processDataMigrateDao.getTemplatePipelineRecord(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
        if (tTemplatePipelineRecord != null) {
            processDataMigrateDao.migrateTemplatePipelineData(
                migratingShardingDslContext = migratingShardingDslContext,
                tTemplatePipelineRecord = tTemplatePipelineRecord
            )
        }
    }

    private fun migratePipelineTimerData(
        projectId: String,
        pipelineId: String,
        dslContext: DSLContext,
        migratingShardingDslContext: DSLContext,
        processDataMigrateDao: ProcessDataMigrateDao
    ) {
        val pipelineTimerRecord = processDataMigrateDao.getPipelineTimerRecord(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
        if (pipelineTimerRecord != null) {
            processDataMigrateDao.migratePipelineTimerData(
                migratingShardingDslContext = migratingShardingDslContext,
                pipelineTimerRecord = pipelineTimerRecord
            )
        }
    }

    private fun migratePipelineTriggerDetailData(
        projectId: String,
        pipelineId: String,
        dslContext: DSLContext,
        migratingShardingDslContext: DSLContext,
        processDataMigrateDao: ProcessDataMigrateDao
    ) {
        var offset = 0
        do {
            val pipelineTriggerDetailRecords = processDataMigrateDao.getPipelineTriggerDetailRecords(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                limit = MEDIUM_PAGE_SIZE,
                offset = offset
            )
            if (pipelineTriggerDetailRecords.isNotEmpty()) {
                processDataMigrateDao.migratePipelineTriggerDetailData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    pipelineTriggerDetailRecords = pipelineTriggerDetailRecords
                )
            }
            offset += MEDIUM_PAGE_SIZE
        } while (pipelineTriggerDetailRecords.size == MEDIUM_PAGE_SIZE)
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
}

package com.tencent.devops.misc.strategy.impl.pipeline

import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.pojo.process.MigrationContext
import com.tencent.devops.misc.strategy.MigrationStrategy
import com.tencent.devops.misc.utils.PageMigrationUtil
import com.tencent.devops.model.process.tables.records.TPipelineBuildContainerRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildDetailRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildRecordContainerRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildRecordModelRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildRecordStageRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildRecordTaskRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildStageRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildTaskRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildVarRecord
import com.tencent.devops.model.process.tables.records.TPipelinePauseValueRecord
import com.tencent.devops.model.process.tables.records.TPipelineTriggerReviewRecord
import com.tencent.devops.model.process.tables.records.TPipelineWebhookBuildParameterRecord
import org.apache.commons.collections4.ListUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory

class PipelineBuildLinkedDataMigrationStrategy(
    private val processDataMigrateDao: ProcessDataMigrateDao
) : MigrationStrategy {

    private val logger = LoggerFactory.getLogger(PipelineBuildLinkedDataMigrationStrategy::class.java)

    companion object {
        // 溢出表迁移时每批搬运的 key 数：单值最大 4M，一批 4 个的堆峰值约 4×8MB≈32MB，
        // 兼顾内存可控与查询往返次数（相比逐 key 减少约 3/4 的查询）。
        private const val OVERFLOW_MIGRATE_KEY_BATCH_SIZE = 4
    }

    // 定义泛型记录处理器接口
    private interface RecordHandler<T> {
        fun fetch(dslContext: DSLContext, projectId: String, buildIds: List<String>): List<T>
        fun migrate(migratingDslContext: DSLContext, records: List<T>)

        // 内置类型转换逻辑
        fun migrateUnsafe(migratingDslContext: DSLContext, records: List<*>) {
            @Suppress("UNCHECKED_CAST") // 内部转换安全，因fetch和migrate类型严格匹配
            migrate(migratingDslContext, records as List<T>)
        }
    }

    // 非归档状态下的处理器（运行期 / 执行明细表，归档场景不保留）
    private val nonArchiveHandlers = listOf(
        // 迁移T_PIPELINE_BUILD_DETAIL相关表数据
        object : RecordHandler<TPipelineBuildDetailRecord> {
            override fun fetch(dslContext: DSLContext, projectId: String, buildIds: List<String>) =
                processDataMigrateDao.getPipelineBuildDetailRecords(dslContext, projectId, buildIds)
            override fun migrate(migratingDslContext: DSLContext, records: List<TPipelineBuildDetailRecord>) =
                processDataMigrateDao.migratePipelineBuildDetailData(migratingDslContext, records)
        },
        // 迁移T_PIPELINE_PAUSE_VALUE相关表数据
        object : RecordHandler<TPipelinePauseValueRecord> {
            override fun fetch(dslContext: DSLContext, projectId: String, buildIds: List<String>) =
                processDataMigrateDao.getPipelinePauseValueRecords(dslContext, projectId, buildIds)
            override fun migrate(migratingDslContext: DSLContext, records: List<TPipelinePauseValueRecord>) =
                processDataMigrateDao.migratePipelinePauseValueData(migratingDslContext, records)
        },
        // 迁移T_PIPELINE_WEBHOOK_BUILD_PARAMETER相关表数据
        object : RecordHandler<TPipelineWebhookBuildParameterRecord> {
            override fun fetch(dslContext: DSLContext, projectId: String, buildIds: List<String>) =
                processDataMigrateDao.getPipelineWebhookBuildParameterRecords(dslContext, projectId, buildIds)
            override fun migrate(migratingDslContext: DSLContext, records: List<TPipelineWebhookBuildParameterRecord>) =
                processDataMigrateDao.migratePipelineWebhookBuildParameterData(migratingDslContext, records)
        },
        // 迁移T_PIPELINE_BUILD_STAGE相关表数据
        object : RecordHandler<TPipelineBuildStageRecord> {
            override fun fetch(dslContext: DSLContext, projectId: String, buildIds: List<String>) =
                processDataMigrateDao.getPipelineBuildStageRecords(dslContext, projectId, buildIds)
            override fun migrate(migratingDslContext: DSLContext, records: List<TPipelineBuildStageRecord>) =
                processDataMigrateDao.migratePipelineBuildStageData(migratingDslContext, records)
        },
        // 迁移T_PIPELINE_BUILD_CONTAINER相关表数据
        object : RecordHandler<TPipelineBuildContainerRecord> {
            override fun fetch(dslContext: DSLContext, projectId: String, buildIds: List<String>) =
                processDataMigrateDao.getPipelineBuildContainerRecords(dslContext, projectId, buildIds)
            override fun migrate(migratingDslContext: DSLContext, records: List<TPipelineBuildContainerRecord>) =
                processDataMigrateDao.migratePipelineBuildContainerData(migratingDslContext, records)
        },
        // 迁移T_PIPELINE_BUILD_TASK相关表数据
        object : RecordHandler<TPipelineBuildTaskRecord> {
            override fun fetch(dslContext: DSLContext, projectId: String, buildIds: List<String>) =
                processDataMigrateDao.getPipelineBuildTaskRecords(dslContext, projectId, buildIds)
            override fun migrate(migratingDslContext: DSLContext, records: List<TPipelineBuildTaskRecord>) =
                processDataMigrateDao.migratePipelineBuildTaskData(migratingDslContext, records)
        }
    )

    // 所有状态下的处理器（归档与非归档都迁移）
    private val commonHandlers = listOf(
        // 迁移T_PIPELINE_BUILD_VAR 变量主表：归档库要支持构建变量查询，故归档/非归档都迁。
        // 注：两张溢出表（VAR_OVERFLOW / HISTORY_PARAM_OVERFLOW）因 mediumtext 单条 ~16M，
        // 一次性按 SHORT_PAGE_SIZE（多 buildId）批量加载会造成内存峰值，改为按 buildId 流式迁移，
        // 见 [migrateOverflowPerBuild]，不放入本 handler 组。
        object : RecordHandler<TPipelineBuildVarRecord> {
            override fun fetch(dslContext: DSLContext, projectId: String, buildIds: List<String>) =
                processDataMigrateDao.getPipelineBuildVarRecords(dslContext, projectId, buildIds)
            override fun migrate(migratingDslContext: DSLContext, records: List<TPipelineBuildVarRecord>) =
                processDataMigrateDao.migratePipelineBuildVarData(migratingDslContext, records)
        },
        // 迁移T_PIPELINE_TRIGGER_REVIEW相关表数据
        object : RecordHandler<TPipelineTriggerReviewRecord> {
            override fun fetch(dslContext: DSLContext, projectId: String, buildIds: List<String>) =
                processDataMigrateDao.getPipelineTriggerReviewRecords(dslContext, projectId, buildIds)
            override fun migrate(migratingDslContext: DSLContext, records: List<TPipelineTriggerReviewRecord>) =
                processDataMigrateDao.migratePipelineTriggerReviewData(migratingDslContext, records)
        },
        // 迁移T_PIPELINE_BUILD_RECORD_CONTAINER相关表数据
        object : RecordHandler<TPipelineBuildRecordContainerRecord> {
            override fun fetch(dslContext: DSLContext, projectId: String, buildIds: List<String>) =
                processDataMigrateDao.getPipelineBuildRecordContainerRecords(dslContext, projectId, buildIds)
            override fun migrate(migratingDslContext: DSLContext, records: List<TPipelineBuildRecordContainerRecord>) =
                processDataMigrateDao.migratePipelineBuildRecordContainerData(migratingDslContext, records)
        },
        // 迁移T_PIPELINE_BUILD_RECORD_MODEL相关表数据
        object : RecordHandler<TPipelineBuildRecordModelRecord> {
            override fun fetch(dslContext: DSLContext, projectId: String, buildIds: List<String>) =
                processDataMigrateDao.getPipelineBuildRecordModelRecords(dslContext, projectId, buildIds)
            override fun migrate(migratingDslContext: DSLContext, records: List<TPipelineBuildRecordModelRecord>) =
                processDataMigrateDao.migratePipelineBuildRecordModelData(migratingDslContext, records)
        },
        // 迁移T_PIPELINE_BUILD_RECORD_STAGE相关表数据
        object : RecordHandler<TPipelineBuildRecordStageRecord> {
            override fun fetch(dslContext: DSLContext, projectId: String, buildIds: List<String>) =
                processDataMigrateDao.getPipelineBuildRecordStageRecords(dslContext, projectId, buildIds)
            override fun migrate(migratingDslContext: DSLContext, records: List<TPipelineBuildRecordStageRecord>) =
                processDataMigrateDao.migratePipelineBuildRecordStageData(migratingDslContext, records)
        },
        // 迁移T_PIPELINE_BUILD_RECORD_TASK相关表数据
        object : RecordHandler<TPipelineBuildRecordTaskRecord> {
            override fun fetch(dslContext: DSLContext, projectId: String, buildIds: List<String>) =
                processDataMigrateDao.getPipelineBuildRecordTaskRecords(dslContext, projectId, buildIds)
            override fun migrate(migratingDslContext: DSLContext, records: List<TPipelineBuildRecordTaskRecord>) =
                processDataMigrateDao.migratePipelineBuildRecordTaskData(migratingDslContext, records)
        }
    )

    override fun migrate(context: MigrationContext) {
        val pipelineId = context.pipelineId ?: run {
            logger.warn("Skipping build data migration: pipelineId is null")
            return
        }
        with(context) {
            // 迁移构建历史记录
            logger.info("Start migrating build history data for pipeline[$pipelineId]")
            migrateBuildHistoryType(
                context = this,
                fetchRecords = { offset, limit ->
                    processDataMigrateDao.getPipelineBuildHistoryRecords(
                        dslContext = dslContext,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        limit = limit,
                        offset = offset
                    )
                },
                migrateMainRecords = { records ->
                    processDataMigrateDao.migratePipelineBuildHistoryData(
                        migratingShardingDslContext = migratingShardingDslContext,
                        pipelineBuildHistoryRecords = records
                    )
                },
                extractBuildId = { it.buildId }
            )
            logger.info("Finished migrating build history data for pipeline[$pipelineId]")

            // 迁移构建历史调试记录
            logger.info("Start migrating build history debug data for pipeline[$pipelineId]")
            migrateBuildHistoryType(
                context = this,
                fetchRecords = { offset, limit ->
                    processDataMigrateDao.getPipelineBuildHistoryDebugRecords(
                        dslContext = dslContext,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        limit = limit,
                        offset = offset
                    )
                },
                migrateMainRecords = { records ->
                    processDataMigrateDao.migratePipelineBuildHistoryDebugData(
                        migratingShardingDslContext = migratingShardingDslContext,
                        pipelineBuildHistoryDebugRecords = records
                    )
                },
                extractBuildId = { it.buildId }
            )
            logger.info("Finished migrating build history debug data for pipeline[$pipelineId]")
        }
    }

    /**
     * 通用迁移方法（使用泛型确保类型安全）
     * @param T 记录类型
     * @param fetchRecords 获取记录的函数
     * @param migrateMainRecords 迁移主记录的函数
     * @param extractBuildId 从记录中提取buildId的函数
     */
    private fun <T> migrateBuildHistoryType(
        context: MigrationContext,
        fetchRecords: (offset: Int, limit: Int) -> List<T>,
        migrateMainRecords: (List<T>) -> Unit,
        extractBuildId: (T) -> String
    ) {
        PageMigrationUtil.migrateByPage(
            pageSize = PageMigrationUtil.MEDIUM_PAGE_SIZE,
            fetch = fetchRecords,
            migrate = { records ->
                migrateMainRecords(records)
                val buildIds = records.map(extractBuildId)
                migrateBuildLinkedData(context, buildIds)
            }
        )
    }

    /**
     * 迁移关联数据
     */
    private fun migrateBuildLinkedData(
        context: MigrationContext,
        buildIds: List<String>
    ) {
        if (buildIds.isEmpty()) return

        ListUtils.partition(buildIds, PageMigrationUtil.SHORT_PAGE_SIZE).forEach { batchIds ->
            with(context) {
                // 大变量 / 启动参数溢出表：归档与非归档都迁移。
                // 按 buildId 流式迁移，避免一次性把多 build 的 mediumtext 全部加载到内存。
                migrateOverflowPerBuild(
                    buildIds = batchIds,
                    dslContext = dslContext,
                    projectId = projectId,
                    migratingDslContext = migratingShardingDslContext
                )

                // 运行期 / 执行明细表仅非归档场景迁移
                if (archiveFlag != true) {
                    migrateHandlerGroup(
                        handlers = nonArchiveHandlers,
                        buildIds = batchIds,
                        dslContext = dslContext,
                        projectId = projectId,
                        migratingDslContext = migratingShardingDslContext
                    )
                }

                // 迁移通用表（含 T_PIPELINE_BUILD_VAR 变量主表，归档/非归档都迁）
                migrateHandlerGroup(
                    handlers = commonHandlers,
                    buildIds = batchIds,
                    dslContext = dslContext,
                    projectId = projectId,
                    migratingDslContext = migratingShardingDslContext
                )
            }
        }
    }

    /**
     * 流式迁移单个项目下若干 buildId 对应的溢出表记录
     * （T_PIPELINE_BUILD_VAR_OVERFLOW / T_PIPELINE_BUILD_HISTORY_PARAM_OVERFLOW）。
     *
     * 设计要点：
     *  - **按 (buildId, 一小批 key) 搬运**：先取该 build 的变量名（KEY）列表（仅 KEY 列，轻量），
     *    再把 key 按 [OVERFLOW_MIGRATE_KEY_BATCH_SIZE] 分批拉取真实值并写入目标库。运行期大变量个数
     *    无上限（插件输出 / setVariable 每条仅受 4M 上限约束，但一次构建的大变量总量不设总量闸门），
     *    若一次性 fetch 整个 build 的溢出行，单 build 内存峰值 = Σ(该 build 大变量字符数)，极端场景可达数百 MB；
     *    分小批后单次内存峰值 ≈ 批大小 × 单个大变量值（默认 4×4M，JVM UTF-16 约 32MB），与大变量个数无关，
     *    同时相比逐 key 显著减少查询往返；
     *  - 迁移失败按"快速失败"处理，避免迁移过程中"主表已迁、溢出表丢失"造成数据不一致。
     */
    private fun migrateOverflowPerBuild(
        buildIds: List<String>,
        dslContext: DSLContext,
        projectId: String,
        migratingDslContext: DSLContext
    ) {
        buildIds.forEach { buildId ->
            try {
                // 大变量溢出表：按小批 key 流式迁移，避免整 build 多条 mediumtext 同时驻留内存
                processDataMigrateDao.getPipelineBuildVarOverflowKeys(
                    dslContext = dslContext,
                    projectId = projectId,
                    buildId = buildId
                ).chunked(OVERFLOW_MIGRATE_KEY_BATCH_SIZE).forEach { keyBatch ->
                    val records = processDataMigrateDao.getPipelineBuildVarOverflowRecordsByKeys(
                        dslContext = dslContext,
                        projectId = projectId,
                        buildId = buildId,
                        keys = keyBatch
                    )
                    if (records.isNotEmpty()) {
                        processDataMigrateDao.migratePipelineBuildVarOverflowData(
                            migratingShardingDslContext = migratingDslContext,
                            records = records
                        )
                    }
                }
                // 启动参数大值溢出表（长期载体）同样按小批 key 流式迁移
                processDataMigrateDao.getPipelineBuildHistoryParamOverflowKeys(
                    dslContext = dslContext,
                    projectId = projectId,
                    buildId = buildId
                ).chunked(OVERFLOW_MIGRATE_KEY_BATCH_SIZE).forEach { keyBatch ->
                    val paramRecords = processDataMigrateDao.getPipelineBuildHistoryParamOverflowRecordsByKeys(
                        dslContext = dslContext,
                        projectId = projectId,
                        buildId = buildId,
                        keys = keyBatch
                    )
                    if (paramRecords.isNotEmpty()) {
                        processDataMigrateDao.migratePipelineBuildHistoryParamOverflowData(
                            migratingShardingDslContext = migratingDslContext,
                            records = paramRecords
                        )
                    }
                }
            } catch (e: Exception) {
                logger.error("Failed to migrate overflow data for build[$buildId]", e)
                throw e
            }
        }
    }

    /**
     * 迁移处理器组（使用接口内置方法避免类型推断）
     */
    private fun migrateHandlerGroup(
        handlers: List<RecordHandler<*>>,
        buildIds: List<String>,
        dslContext: DSLContext,
        projectId: String,
        migratingDslContext: DSLContext
    ) {
        handlers.forEach { handler ->
            try {
                val records = handler.fetch(dslContext, projectId, buildIds)
                if (records.isNotEmpty()) {
                    // 调用接口内置的类型安全迁移方法，无需外部推断T
                    handler.migrateUnsafe(migratingDslContext, records)
                }
            } catch (e: Exception) {
                logger.error("Failed to migrate linked data for handler", e)
                throw e
            }
        }
    }
}

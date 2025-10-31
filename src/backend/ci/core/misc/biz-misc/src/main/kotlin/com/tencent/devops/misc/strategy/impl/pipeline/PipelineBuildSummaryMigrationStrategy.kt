package com.tencent.devops.misc.strategy.impl.pipeline

import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.pojo.process.MigrationContext
import com.tencent.devops.misc.strategy.MigrationStrategy
import org.slf4j.LoggerFactory

class PipelineBuildSummaryMigrationStrategy(
    private val processDataMigrateDao: ProcessDataMigrateDao
) : MigrationStrategy {

    private val logger = LoggerFactory.getLogger(PipelineBuildSummaryMigrationStrategy::class.java)

    override fun migrate(context: MigrationContext) {
        val pipelineId = context.pipelineId ?: run {
            logger.warn("Skipping T_PIPELINE_BUILD_SUMMARY migration: pipelineId is null")
            return
        }
        // 迁移T_PIPELINE_BUILD_SUMMARY表数据
        logger.info("Start migrating T_PIPELINE_BUILD_SUMMARY data for pipeline $pipelineId")
        processDataMigrateDao.getPipelineBuildSummaryRecord(
            dslContext = context.dslContext,
            projectId = context.projectId,
            pipelineId = pipelineId
        )?.let { record ->
            processDataMigrateDao.migratePipelineBuildSummaryData(
                migratingShardingDslContext = context.migratingShardingDslContext,
                buildSummaryRecord = record
            )
        }
        logger.info("Finished migrating T_PIPELINE_BUILD_SUMMARY data for pipeline $pipelineId")
    }
}

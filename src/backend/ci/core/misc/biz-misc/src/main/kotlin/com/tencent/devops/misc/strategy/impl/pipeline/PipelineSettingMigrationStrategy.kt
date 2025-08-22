package com.tencent.devops.misc.strategy.impl.pipeline

import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.pojo.process.MigrationContext
import com.tencent.devops.misc.strategy.MigrationStrategy
import org.slf4j.LoggerFactory

class PipelineSettingMigrationStrategy(
    private val processDataMigrateDao: ProcessDataMigrateDao
) : MigrationStrategy {

    private val logger = LoggerFactory.getLogger(PipelineSettingMigrationStrategy::class.java)

    override fun migrate(context: MigrationContext) {
        val pipelineId = context.pipelineId ?: run {
            logger.warn("Skipping T_PIPELINE_SETTING migration: pipelineId is null")
            return
        }
        // 迁移T_PIPELINE_SETTING表数据
        logger.info("Start migrating T_PIPELINE_SETTING data for pipelineId: $pipelineId")
        processDataMigrateDao.getPipelineSettingRecord(
            dslContext = context.dslContext,
            projectId = context.projectId,
            pipelineId = pipelineId
        )?.let { record ->
            processDataMigrateDao.migratePipelineSettingData(
                migratingShardingDslContext = context.migratingShardingDslContext,
                pipelineSettingRecord = record
            )
        }
        logger.info("Finish migrating T_PIPELINE_SETTING data for pipelineId: $pipelineId")
    }
}

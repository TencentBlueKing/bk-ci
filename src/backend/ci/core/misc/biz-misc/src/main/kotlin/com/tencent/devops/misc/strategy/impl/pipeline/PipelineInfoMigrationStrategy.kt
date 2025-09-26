package com.tencent.devops.misc.strategy.impl.pipeline

import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.pojo.process.MigrationContext
import com.tencent.devops.misc.strategy.MigrationStrategy
import org.slf4j.LoggerFactory

class PipelineInfoMigrationStrategy(
    private val processDataMigrateDao: ProcessDataMigrateDao
) : MigrationStrategy {

    private val logger = LoggerFactory.getLogger(PipelineInfoMigrationStrategy::class.java)

    override fun migrate(context: MigrationContext) {
        val pipelineId = context.pipelineId ?: run {
            logger.warn("Skipping T_PIPELINE_INFO migration: pipelineId is null")
            return
        }
        // 迁移T_PIPELINE_INFO表数据
        logger.info("Start migrating T_PIPELINE_INFO data for pipelineId: $pipelineId")
        processDataMigrateDao.getPipelineInfoRecord(
            dslContext = context.dslContext,
            projectId = context.projectId,
            pipelineId = pipelineId
        )?.let { record ->
            processDataMigrateDao.migratePipelineInfoData(
                migratingShardingDslContext = context.migratingShardingDslContext,
                pipelineInfoRecord = record
            )
        }
        logger.info("Finished migrating T_PIPELINE_INFO data for pipelineId: $pipelineId")
    }
}

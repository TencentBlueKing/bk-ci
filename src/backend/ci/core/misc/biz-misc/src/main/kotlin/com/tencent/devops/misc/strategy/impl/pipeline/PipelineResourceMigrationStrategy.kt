package com.tencent.devops.misc.strategy.impl.pipeline

import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.pojo.process.MigrationContext
import com.tencent.devops.misc.strategy.MigrationStrategy
import org.slf4j.LoggerFactory

class PipelineResourceMigrationStrategy(
    private val processDataMigrateDao: ProcessDataMigrateDao
) : MigrationStrategy {

    private val logger = LoggerFactory.getLogger(PipelineResourceMigrationStrategy::class.java)

    override fun migrate(context: MigrationContext) {
        val pipelineId = context.pipelineId ?: run {
            logger.warn("Skipping T_PIPELINE_RESOURCE migration: pipelineId is null")
            return
        }
        // 迁移T_PIPELINE_RESOURCE表数据
        logger.info("Start migrating T_PIPELINE_RESOURCE data for pipeline $pipelineId")
        processDataMigrateDao.getPipelineResourceRecord(
            dslContext = context.dslContext,
            projectId = context.projectId,
            pipelineId = pipelineId
        )?.let { record ->
            processDataMigrateDao.migratePipelineResourceData(
                migratingShardingDslContext = context.migratingShardingDslContext,
                pipelineResourceRecord = record
            )
        }
        logger.info("Finished migrating T_PIPELINE_RESOURCE data for pipeline $pipelineId")
    }
}

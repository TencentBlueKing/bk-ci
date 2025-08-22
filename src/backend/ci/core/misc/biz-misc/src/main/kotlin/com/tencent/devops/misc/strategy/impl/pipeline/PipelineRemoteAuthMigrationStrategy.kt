package com.tencent.devops.misc.strategy.impl.pipeline

import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.pojo.process.MigrationContext
import com.tencent.devops.misc.strategy.MigrationStrategy
import org.slf4j.LoggerFactory

class PipelineRemoteAuthMigrationStrategy(
    private val processDataMigrateDao: ProcessDataMigrateDao
) : MigrationStrategy {

    private val logger = LoggerFactory.getLogger(PipelineRemoteAuthMigrationStrategy::class.java)

    override fun migrate(context: MigrationContext) {
        val pipelineId = context.pipelineId ?: run {
            logger.warn("Skipping T_PIPELINE_REMOTE_AUTH migration: pipelineId is null")
            return
        }
        // 迁移T_PIPELINE_REMOTE_AUTH表数据
        logger.info("Start migrating T_PIPELINE_REMOTE_AUTH data for pipeline $pipelineId")
        processDataMigrateDao.getPipelineRemoteAuthRecord(
            dslContext = context.dslContext,
            projectId = context.projectId,
            pipelineId = pipelineId
        )?.let { record ->
            processDataMigrateDao.migratePipelineRemoteAuthData(
                migratingShardingDslContext = context.migratingShardingDslContext,
                pipelineRemoteAuthRecord = record
            )
        }
        logger.info("Finish migrating T_PIPELINE_REMOTE_AUTH data for pipeline $pipelineId")
    }
}

package com.tencent.devops.misc.strategy.impl.pipeline

import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.pojo.process.MigrationContext
import com.tencent.devops.misc.strategy.MigrationStrategy
import org.slf4j.LoggerFactory

class PipelineModelTaskMigrationStrategy(
    private val processDataMigrateDao: ProcessDataMigrateDao
) : MigrationStrategy {

    private val logger = LoggerFactory.getLogger(PipelineModelTaskMigrationStrategy::class.java)

    override fun migrate(context: MigrationContext) {
        val pipelineId = context.pipelineId ?: run {
            logger.warn("Skipping T_PIPELINE_MODEL_TASK migration: pipelineId is null")
            return
        }
        // 迁移T_PIPELINE_MODEL_TASK表数据
        logger.info("Start migrating T_PIPELINE_MODEL_TASK data for pipeline $pipelineId")
        processDataMigrateDao.getPipelineModelTaskRecords(
            dslContext = context.dslContext,
            projectId = context.projectId,
            pipelineId = pipelineId
        ).takeIf { it.isNotEmpty() }?.let { records ->
            processDataMigrateDao.migratePipelineModelTaskData(
                migratingShardingDslContext = context.migratingShardingDslContext,
                pipelineModelTaskRecords = records
            )
        }
        logger.info("Finished migrating T_PIPELINE_MODEL_TASK data for pipeline $pipelineId")
    }
}

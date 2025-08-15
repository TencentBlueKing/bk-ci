package com.tencent.devops.misc.strategy.impl.pipeline

import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.pojo.process.MigrationContext
import com.tencent.devops.misc.strategy.MigrationStrategy

class PipelineModelTaskMigrationStrategy(
    private val processDataMigrateDao: ProcessDataMigrateDao
) : MigrationStrategy {

    override fun migrate(context: MigrationContext) {
        // 迁移T_PIPELINE_MODEL_TASK表数据
        context.pipelineId?.let { pipelineId ->
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
        }
    }
}

package com.tencent.devops.misc.strategy.impl.pipeline

import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.pojo.process.MigrationContext
import com.tencent.devops.misc.strategy.MigrationStrategy

class PipelineResourceMigrationStrategy(
    private val processDataMigrateDao: ProcessDataMigrateDao
) : MigrationStrategy {

    override fun migrate(context: MigrationContext) {
        // 迁移T_PIPELINE_RESOURCE表数据
        context.pipelineId?.let { pipelineId ->
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
        }
    }
}

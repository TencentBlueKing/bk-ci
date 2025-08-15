package com.tencent.devops.misc.strategy.impl.pipeline

import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.pojo.process.MigrationContext
import com.tencent.devops.misc.strategy.MigrationStrategy

class PipelineInfoMigrationStrategy(
    private val processDataMigrateDao: ProcessDataMigrateDao
) : MigrationStrategy {

    override fun migrate(context: MigrationContext) {
        // 迁移T_PIPELINE_INFO表数据
        context.pipelineId?.let { pipelineId ->
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
        }
    }
}

package com.tencent.devops.misc.strategy.impl.pipeline

import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.pojo.process.MigrationContext
import com.tencent.devops.misc.strategy.MigrationStrategy

class PipelineYamlInfoMigrationStrategy(
    private val processDataMigrateDao: ProcessDataMigrateDao
) : MigrationStrategy {

    override fun migrate(context: MigrationContext) {
        // 迁移T_PIPELINE_YAML_INFO表数据
        context.pipelineId?.let { pipelineId ->
            processDataMigrateDao.getPipelineYamlInfoRecord(
                dslContext = context.dslContext,
                projectId = context.projectId,
                pipelineId = pipelineId
            )?.let { record ->
                processDataMigrateDao.migratePipelineYamlInfoData(
                    migratingShardingDslContext = context.migratingShardingDslContext,
                    pipelineYamlInfoRecord = record
                )
            }
        }
    }
}
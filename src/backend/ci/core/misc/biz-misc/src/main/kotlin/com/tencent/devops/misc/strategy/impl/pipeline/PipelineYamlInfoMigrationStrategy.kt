package com.tencent.devops.misc.strategy.impl.pipeline

import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.pojo.process.MigrationContext
import com.tencent.devops.misc.strategy.MigrationStrategy
import org.slf4j.LoggerFactory

class PipelineYamlInfoMigrationStrategy(
    private val processDataMigrateDao: ProcessDataMigrateDao
) : MigrationStrategy {

    private val logger = LoggerFactory.getLogger(PipelineYamlInfoMigrationStrategy::class.java)

    override fun migrate(context: MigrationContext) {
        val pipelineId = context.pipelineId ?: run {
            logger.warn("Skipping T_AUDIT_RESOURCE migration: pipelineId is null")
            return
        }
        // 迁移T_PIPELINE_YAML_INFO表数据
        logger.info("Start migrating T_PIPELINE_YAML_INFO data for pipeline $pipelineId")
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
        logger.info("Finish migrating T_PIPELINE_YAML_INFO data for pipeline $pipelineId")
    }
}

package com.tencent.devops.misc.strategy.impl.pipeline

import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.pojo.process.MigrationContext
import com.tencent.devops.misc.strategy.MigrationStrategy
import org.slf4j.LoggerFactory

class TemplatePipelineMigrationStrategy(
    private val processDataMigrateDao: ProcessDataMigrateDao
) : MigrationStrategy {

    private val logger = LoggerFactory.getLogger(TemplatePipelineMigrationStrategy::class.java)

    override fun migrate(context: MigrationContext) {
        val pipelineId = context.pipelineId ?: run {
            logger.warn("Skipping T_TEMPLATE_PIPELINE migration: pipelineId is null")
            return
        }
        // 迁移T_TEMPLATE_PIPELINE表数据
        logger.info("Start migrating T_TEMPLATE_PIPELINE data for pipelineId: $pipelineId")
        processDataMigrateDao.getTemplatePipelineRecord(
            dslContext = context.dslContext,
            projectId = context.projectId,
            pipelineId = pipelineId
        )?.let { record ->
            processDataMigrateDao.migrateTemplatePipelineData(
                migratingShardingDslContext = context.migratingShardingDslContext,
                tTemplatePipelineRecord = record
            )
        }
        logger.info("Finished migrating T_TEMPLATE_PIPELINE data for pipelineId: $pipelineId")
    }
}

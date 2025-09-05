package com.tencent.devops.misc.strategy.impl.pipeline

import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.pojo.process.MigrationContext
import com.tencent.devops.misc.strategy.MigrationStrategy
import com.tencent.devops.misc.utils.PageMigrationUtil
import org.slf4j.LoggerFactory

class PipelineResourceVersionMigrationStrategy(
    private val processDataMigrateDao: ProcessDataMigrateDao
) : MigrationStrategy {

    private val logger = LoggerFactory.getLogger(PipelineResourceVersionMigrationStrategy::class.java)

    override fun migrate(context: MigrationContext) {
        val pipelineId = context.pipelineId ?: run {
            logger.warn("Skipping T_PIPELINE_RESOURCE_VERSION migration: pipelineId is null")
            return
        }
        // 迁移T_PIPELINE_RESOURCE_VERSION表数据
        logger.info("Start migrating T_PIPELINE_RESOURCE_VERSION data for pipeline $pipelineId")
        PageMigrationUtil.migrateByPage(
            pageSize = PageMigrationUtil.SHORT_PAGE_SIZE,
            fetch = { offset, limit ->
                processDataMigrateDao.getPipelineResourceVersionRecords(
                    dslContext = context.dslContext,
                    projectId = context.projectId,
                    pipelineId = pipelineId,
                    limit = limit,
                    offset = offset
                )
            },
            migrate = { records ->
                processDataMigrateDao.migratePipelineResourceVersionData(
                    migratingShardingDslContext = context.migratingShardingDslContext,
                    pipelineResourceVersionRecords = records
                )
            }
        )
        logger.info("Finished migrating T_PIPELINE_RESOURCE_VERSION data for pipeline $pipelineId")
    }
}

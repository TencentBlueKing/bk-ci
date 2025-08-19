package com.tencent.devops.misc.strategy.impl.pipeline

import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.pojo.process.MigrationContext
import com.tencent.devops.misc.strategy.MigrationStrategy
import com.tencent.devops.misc.utils.PageMigrationUtil
import org.slf4j.LoggerFactory

class PipelineRecentUseMigrationStrategy(
    private val processDataMigrateDao: ProcessDataMigrateDao
) : MigrationStrategy {

    private val logger = LoggerFactory.getLogger(PipelineRecentUseMigrationStrategy::class.java)

    override fun migrate(context: MigrationContext) {
        val pipelineId = context.pipelineId ?: run {
            logger.warn("Skipping T_PIPELINE_RECENT_USE migration: pipelineId is null")
            return
        }
        // 迁移T_PIPELINE_RECENT_USE表数据
        logger.info("Start migrating T_PIPELINE_RECENT_USE data for pipeline $pipelineId")
        PageMigrationUtil.migrateByPage(
            pageSize = PageMigrationUtil.LONG_PAGE_SIZE,
            fetch = { offset, limit ->
                processDataMigrateDao.getPipelineRecentUseRecords(
                    dslContext = context.dslContext,
                    projectId = context.projectId,
                    pipelineId = pipelineId,
                    limit = limit,
                    offset = offset
                )
            },
            migrate = { records ->
                processDataMigrateDao.migratePipelineRecentUseData(
                    migratingShardingDslContext = context.migratingShardingDslContext,
                    pipelineRecentUseRecords = records
                )
            }
        )
        logger.info("Finished migrating T_PIPELINE_RECENT_USE data for pipeline $pipelineId")
    }
}

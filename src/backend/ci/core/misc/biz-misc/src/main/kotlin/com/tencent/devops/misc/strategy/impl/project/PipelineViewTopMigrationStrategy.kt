package com.tencent.devops.misc.strategy.impl.project

import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.pojo.process.MigrationContext
import com.tencent.devops.misc.strategy.MigrationStrategy
import com.tencent.devops.misc.utils.PageMigrationUtil
import org.slf4j.LoggerFactory

class PipelineViewTopMigrationStrategy(
    private val processDataMigrateDao: ProcessDataMigrateDao
) : MigrationStrategy {

    private val logger = LoggerFactory.getLogger(PipelineViewTopMigrationStrategy::class.java)

    override fun migrate(context: MigrationContext) {
        // 迁移T_PIPELINE_VIEW_TOP表数据
        val projectId = context.projectId
        logger.info("Start migrating T_PIPELINE_VIEW_TOP data for projectId: $projectId")
        PageMigrationUtil.migrateByPage(
            pageSize = PageMigrationUtil.LONG_PAGE_SIZE,
            fetch = { offset, limit ->
                processDataMigrateDao.getPipelineViewTopRecords(
                    dslContext = context.dslContext,
                    projectId = projectId,
                    limit = limit,
                    offset = offset
                )
            },
            migrate = { records ->
                processDataMigrateDao.migratePipelineViewTopData(
                    migratingShardingDslContext = context.migratingShardingDslContext,
                    pipelineViewTopRecords = records
                )
            }
        )
        logger.info("Finish migrating T_PIPELINE_VIEW_TOP data for projectId: $projectId")
    }
}

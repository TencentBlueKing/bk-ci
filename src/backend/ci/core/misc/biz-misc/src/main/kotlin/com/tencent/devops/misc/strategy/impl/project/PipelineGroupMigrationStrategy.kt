package com.tencent.devops.misc.strategy.impl.project

import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.pojo.process.MigrationContext
import com.tencent.devops.misc.strategy.MigrationStrategy
import com.tencent.devops.misc.utils.PageMigrationUtil
import org.slf4j.LoggerFactory

class PipelineGroupMigrationStrategy(private val processDataMigrateDao: ProcessDataMigrateDao) : MigrationStrategy {

    private val logger = LoggerFactory.getLogger(PipelineGroupMigrationStrategy::class.java)

    override fun migrate(context: MigrationContext) {
        // 迁移T_PIPELINE_GROUP表数据
        val projectId = context.projectId
        logger.info("Start migrating T_PIPELINE_GROUP data for projectId: $projectId")
        PageMigrationUtil.migrateByPage(
            pageSize = PageMigrationUtil.LONG_PAGE_SIZE,
            fetch = { offset, limit ->
                processDataMigrateDao.getPipelineGroupRecords(
                    dslContext = context.dslContext,
                    projectId = projectId,
                    limit = limit,
                    offset = offset
                )
            },
            migrate = { records ->
                processDataMigrateDao.migratePipelineGroupData(
                    migratingShardingDslContext = context.migratingShardingDslContext,
                    pipelineGroupRecords = records
                )
            }
        )
        logger.info("Finish migrating T_PIPELINE_GROUP data for projectId: $projectId")
    }
}

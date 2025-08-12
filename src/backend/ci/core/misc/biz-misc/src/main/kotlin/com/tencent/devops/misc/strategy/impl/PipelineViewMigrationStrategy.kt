package com.tencent.devops.misc.strategy.impl

import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.pojo.process.MigrationContext
import com.tencent.devops.misc.strategy.MigrationStrategy
import com.tencent.devops.misc.utils.PageMigrationUtil

class PipelineViewMigrationStrategy(
    private val processDataMigrateDao: ProcessDataMigrateDao
) : MigrationStrategy {

    override fun migrate(context: MigrationContext) {
        PageMigrationUtil.migrateByPage(
            pageSize = PageMigrationUtil.LONG_PAGE_SIZE,
            fetch = { offset, limit ->
                processDataMigrateDao.getPipelineViewRecords(
                    dslContext = context.dslContext,
                    projectId = context.projectId,
                    limit = limit,
                    offset = offset
                )
            },
            migrate = { records ->
                processDataMigrateDao.migratePipelineViewData(
                    migratingShardingDslContext = context.migratingShardingDslContext,
                    pipelineViewRecords = records
                )
            }
        )
    }
}
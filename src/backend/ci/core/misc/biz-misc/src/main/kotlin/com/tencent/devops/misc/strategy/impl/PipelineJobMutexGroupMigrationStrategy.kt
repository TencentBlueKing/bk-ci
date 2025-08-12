package com.tencent.devops.misc.strategy.impl

import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.pojo.process.MigrationContext
import com.tencent.devops.misc.strategy.MigrationStrategy
import com.tencent.devops.misc.utils.PageMigrationUtil

class PipelineJobMutexGroupMigrationStrategy(
    private val processDataMigrateDao: ProcessDataMigrateDao
) : MigrationStrategy {

    override fun migrate(context: MigrationContext) {
        PageMigrationUtil.migrateByPage(
            pageSize = PageMigrationUtil.LONG_PAGE_SIZE,
            fetch = { offset, limit ->
                processDataMigrateDao.getPipelineJobMutexGroupRecords(
                    dslContext = context.dslContext,
                    projectId = context.projectId,
                    limit = limit,
                    offset = offset
                )
            },
            migrate = { records ->
                processDataMigrateDao.migratePipelineJobMutexGroupData(
                    migratingShardingDslContext = context.migratingShardingDslContext,
                    pipelineJobMutexGroupRecords = records
                )
            }
        )
    }
}
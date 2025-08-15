package com.tencent.devops.misc.strategy.impl.project

import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.pojo.process.MigrationContext
import com.tencent.devops.misc.strategy.MigrationStrategy
import com.tencent.devops.misc.utils.PageMigrationUtil

class ProjectPipelineTriggerEventMigrationStrategy(
    private val processDataMigrateDao: ProcessDataMigrateDao
) : MigrationStrategy {

    override fun migrate(context: MigrationContext) {
        // 迁移T_PIPELINE_TRIGGER_EVENT表数据
        PageMigrationUtil.migrateByPage(
            pageSize = PageMigrationUtil.MEDIUM_PAGE_SIZE,
            fetch = { offset, limit ->
                processDataMigrateDao.getProjectPipelineTriggerEventRecords(
                    dslContext = context.dslContext,
                    projectId = context.projectId,
                    limit = limit,
                    offset = offset
                )
            },
            migrate = { records ->
                processDataMigrateDao.migrateProjectPipelineTriggerEventData(
                    migratingShardingDslContext = context.migratingShardingDslContext,
                    pipelineTriggerEventRecords = records
                )
            }
        )
    }
}

package com.tencent.devops.misc.strategy.impl.pipeline

import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.pojo.process.MigrationContext
import com.tencent.devops.misc.strategy.MigrationStrategy
import com.tencent.devops.misc.utils.PageMigrationUtil

class PipelineBuildTaskMigrationStrategy(
    private val processDataMigrateDao: ProcessDataMigrateDao
) : MigrationStrategy {

    override fun migrate(context: MigrationContext) {
        val pipelineId = context.pipelineId ?: return
        // 迁移T_PIPELINE_BUILD_TASK表数据
        PageMigrationUtil.migrateByPage(
            pageSize = PageMigrationUtil.MEDIUM_PAGE_SIZE,
            fetch = { offset, limit ->
                processDataMigrateDao.getPipelineBuildTaskRecords(
                    dslContext = context.dslContext,
                    projectId = context.projectId,
                    pipelineId = pipelineId,
                    limit = limit,
                    offset = offset
                )
            },
            migrate = { records ->
                processDataMigrateDao.migratePipelineBuildTaskData(
                    migratingShardingDslContext = context.migratingShardingDslContext,
                    pipelineBuildTaskRecords = records
                )
            }
        )
    }
}
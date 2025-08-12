package com.tencent.devops.misc.strategy.impl.pipeline

import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.pojo.process.MigrationContext
import com.tencent.devops.misc.strategy.MigrationStrategy
import com.tencent.devops.misc.utils.PageMigrationUtil

class PipelineResourceVersionMigrationStrategy(
    private val processDataMigrateDao: ProcessDataMigrateDao
) : MigrationStrategy {

    override fun migrate(context: MigrationContext) {
        val pipelineId = context.pipelineId ?: return
        // 迁移T_PIPELINE_RESOURCE_VERSION表数据
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
    }
}
package com.tencent.devops.misc.strategy.impl.project

import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.pojo.process.MigrationContext
import com.tencent.devops.misc.strategy.MigrationStrategy
import com.tencent.devops.misc.utils.PageMigrationUtil
import org.slf4j.LoggerFactory

class PipelineViewUserSettingsMigrationStrategy(
    private val processDataMigrateDao: ProcessDataMigrateDao
) : MigrationStrategy {

    private val logger = LoggerFactory.getLogger(PipelineViewUserSettingsMigrationStrategy::class.java)

    override fun migrate(context: MigrationContext) {
        // 迁移T_PIPELINE_VIEW_USER_SETTINGS表数据
        val projectId = context.projectId
        logger.info("Start migrating T_PIPELINE_VIEW_USER_SETTINGS data for projectId: $projectId")
        PageMigrationUtil.migrateByPage(
            pageSize = PageMigrationUtil.LONG_PAGE_SIZE,
            fetch = { offset, limit ->
                processDataMigrateDao.getPipelineViewUserSettingsRecords(
                    dslContext = context.dslContext,
                    projectId = context.projectId,
                    limit = limit,
                    offset = offset
                )
            },
            migrate = { records ->
                processDataMigrateDao.migratePipelineViewUserSettingsData(
                    migratingShardingDslContext = context.migratingShardingDslContext,
                    pipelineViewUserSettingsRecords = records
                )
            }
        )
        logger.info("Finish migrating T_PIPELINE_VIEW_USER_SETTINGS data for projectId: $projectId")
    }
}

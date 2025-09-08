package com.tencent.devops.misc.strategy.impl.project

import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.pojo.process.MigrationContext
import com.tencent.devops.misc.strategy.MigrationStrategy
import com.tencent.devops.misc.utils.PageMigrationUtil
import org.slf4j.LoggerFactory

class ProjectPipelineCallbackMigrationStrategy(
    private val processDataMigrateDao: ProcessDataMigrateDao
) : MigrationStrategy {

    private val logger = LoggerFactory.getLogger(ProjectPipelineCallbackMigrationStrategy::class.java)

    override fun migrate(context: MigrationContext) {
        // 迁移T_PROJECT_PIPELINE_CALLBACK表数据
        val projectId = context.projectId
        logger.info("Start migrating T_PROJECT_PIPELINE_CALLBACK data for projectId: $projectId")
        PageMigrationUtil.migrateByPage(
            pageSize = PageMigrationUtil.MEDIUM_PAGE_SIZE,
            fetch = { offset, limit ->
                processDataMigrateDao.getProjectPipelineCallbackRecords(
                    dslContext = context.dslContext,
                    projectId = projectId,
                    limit = limit,
                    offset = offset
                )
            },
            migrate = { records ->
                processDataMigrateDao.migrateProjectPipelineCallbackData(
                    migratingShardingDslContext = context.migratingShardingDslContext,
                    projectPipelineCallbackRecords = records
                )
            }
        )
        logger.info("Finish migrating T_PROJECT_PIPELINE_CALLBACK data for projectId: $projectId")
    }
}

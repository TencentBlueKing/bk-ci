package com.tencent.devops.misc.strategy.impl.project

import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.pojo.process.MigrationContext
import com.tencent.devops.misc.strategy.MigrationStrategy
import com.tencent.devops.misc.utils.PageMigrationUtil
import org.slf4j.LoggerFactory

class ProjectPipelineYamlBranchFileMigrationStrategy(
    private val processDataMigrateDao: ProcessDataMigrateDao
) : MigrationStrategy {

    private val logger = LoggerFactory.getLogger(ProjectPipelineYamlBranchFileMigrationStrategy::class.java)

    override fun migrate(context: MigrationContext) {
        // 迁移T_PIPELINE_YAML_BRANCH_FILE表数据
        val projectId = context.projectId
        logger.info("Start migrating T_PIPELINE_YAML_BRANCH_FILE data for projectId: $projectId")
        PageMigrationUtil.migrateByPage(
            pageSize = PageMigrationUtil.MEDIUM_PAGE_SIZE,
            fetch = { offset, limit ->
                processDataMigrateDao.getProjectPipelineYamlBranchFileRecords(
                    dslContext = context.dslContext,
                    projectId = context.projectId,
                    limit = limit,
                    offset = offset
                )
            },
            migrate = { records ->
                processDataMigrateDao.migrateProjectPipelineYamlBranchFileData(
                    migratingShardingDslContext = context.migratingShardingDslContext,
                    pipelineYamlBranchFileRecords = records
                )
            }
        )
        logger.info("Finish migrating T_PIPELINE_YAML_BRANCH_FILE data for projectId: $projectId")
    }
}

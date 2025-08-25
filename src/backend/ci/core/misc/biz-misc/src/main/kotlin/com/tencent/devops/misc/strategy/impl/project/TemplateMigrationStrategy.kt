package com.tencent.devops.misc.strategy.impl.project

import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.pojo.process.MigrationContext
import com.tencent.devops.misc.strategy.MigrationStrategy
import com.tencent.devops.misc.utils.PageMigrationUtil
import org.slf4j.LoggerFactory

class TemplateMigrationStrategy(
    private val processDataMigrateDao: ProcessDataMigrateDao
) : MigrationStrategy {

    private val logger = LoggerFactory.getLogger(TemplateMigrationStrategy::class.java)

    override fun migrate(context: MigrationContext) {
        // 迁移T_TEMPLATE表数据
        val projectId = context.projectId
        logger.info("Start migrating T_TEMPLATE data for projectId: $projectId")
        PageMigrationUtil.migrateByPage(
            pageSize = PageMigrationUtil.SHORT_PAGE_SIZE,
            fetch = { offset, limit ->
                processDataMigrateDao.getTemplateRecords(
                    dslContext = context.dslContext,
                    projectId = context.projectId,
                    limit = limit,
                    offset = offset
                )
            },
            migrate = { records ->
                processDataMigrateDao.migrateTemplateData(
                    migratingShardingDslContext = context.migratingShardingDslContext,
                    templateRecords = records
                )
            }
        )
        logger.info("Finish migrating T_TEMPLATE data for projectId: $projectId")
    }
}

package com.tencent.devops.misc.strategy.impl.project

import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.pojo.process.MigrationContext
import com.tencent.devops.misc.strategy.MigrationStrategy
import com.tencent.devops.misc.utils.PageMigrationUtil
import org.slf4j.LoggerFactory

class AuditResourceMigrationStrategy(private val processDataMigrateDao: ProcessDataMigrateDao) : MigrationStrategy {

    private val logger = LoggerFactory.getLogger(AuditResourceMigrationStrategy::class.java)

    override fun migrate(context: MigrationContext) {
        // 迁移T_AUDIT_RESOURCE表数据
        val projectId = context.projectId
        logger.info("Start migrating T_AUDIT_RESOURCE data for projectId: $projectId")
        PageMigrationUtil.migrateByPage(
            pageSize = PageMigrationUtil.MEDIUM_PAGE_SIZE,
            fetch = { offset, limit ->
                processDataMigrateDao.getAuditResourceRecords(
                    dslContext = context.dslContext,
                    projectId = projectId,
                    limit = limit,
                    offset = offset
                )
            },
            migrate = { records ->
                processDataMigrateDao.migrateAuditResourceData(
                    migratingShardingDslContext = context.migratingShardingDslContext,
                    auditResourceRecords = records
                )
            }
        )
        logger.info("Finish migrating T_AUDIT_RESOURCE data for projectId: $projectId")
    }
}

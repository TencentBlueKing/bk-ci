package com.tencent.devops.misc.strategy.impl.pipeline

import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.pojo.process.MigrationContext
import com.tencent.devops.misc.strategy.MigrationStrategy
import com.tencent.devops.misc.utils.PageMigrationUtil

class PipelineAuditResourceMigrationStrategy(
    private val processDataMigrateDao: ProcessDataMigrateDao
) : MigrationStrategy {

    override fun migrate(context: MigrationContext) {
        val pipelineId = context.pipelineId ?: return
        // 迁移T_AUDIT_RESOURCE表数据
        PageMigrationUtil.migrateByPage(
            pageSize = PageMigrationUtil.MEDIUM_PAGE_SIZE,
            fetch = { offset, limit ->
                processDataMigrateDao.getAuditResourceRecords(
                    dslContext = context.dslContext,
                    projectId = context.projectId,
                    resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                    resourceId = pipelineId,
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
    }
}
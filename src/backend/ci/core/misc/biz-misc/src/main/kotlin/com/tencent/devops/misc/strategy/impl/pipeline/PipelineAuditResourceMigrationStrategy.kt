package com.tencent.devops.misc.strategy.impl.pipeline

import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.pojo.process.MigrationContext
import com.tencent.devops.misc.strategy.MigrationStrategy
import com.tencent.devops.misc.utils.PageMigrationUtil
import org.slf4j.LoggerFactory

class PipelineAuditResourceMigrationStrategy(
    private val processDataMigrateDao: ProcessDataMigrateDao
) : MigrationStrategy {

    private val logger = LoggerFactory.getLogger(PipelineAuditResourceMigrationStrategy::class.java)

    override fun migrate(context: MigrationContext) {
        val pipelineId = context.pipelineId ?: run {
            logger.warn("Skipping T_AUDIT_RESOURCE migration: pipelineId is null")
            return
        }
        val sourceId = context.sourceId
        if (sourceId != pipelineId) {
            // 不是单独迁移一条流水线的数据时，不处理
            logger.warn(
                "Skipping pipeline[$pipelineId] audit migration: Not single pipeline migration (sourceId=$sourceId)"
            )
            return
        }
        // 迁移T_AUDIT_RESOURCE表数据
        logger.info("Start migrating T_AUDIT_RESOURCE data for pipeline[$pipelineId]")
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
        logger.info("Finished migrating T_AUDIT_RESOURCE data for pipeline[$pipelineId]")
    }
}

package com.tencent.devops.misc.strategy.impl.pipeline

import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.pojo.process.MigrationContext
import com.tencent.devops.misc.strategy.MigrationStrategy
import com.tencent.devops.misc.utils.PageMigrationUtil
import org.slf4j.LoggerFactory

class PipelineBuildParamCombinationMigrationStrategy(
    private val processDataMigrateDao: ProcessDataMigrateDao
) : MigrationStrategy {
    private val logger = LoggerFactory.getLogger(PipelineBuildParamCombinationMigrationStrategy::class.java)

    override fun migrate(context: MigrationContext) {
        val pipelineId = context.pipelineId ?: run {
            logger.warn("Skipping T_PIPELINE_BUILD_PARAM_COMBINATION migration: pipelineId is null")
            return
        }
        // 迁移T_PIPELINE_SUB_REF表数据
        logger.info("Start migrating T_PIPELINE_BUILD_PARAM_COMBINATION data for pipeline $pipelineId")
        PageMigrationUtil.migrateByPage(
            pageSize = PageMigrationUtil.MEDIUM_PAGE_SIZE,
            fetch = { offset, limit ->
                processDataMigrateDao.getPipelineBuildParamCombinationRecords(
                    dslContext = context.dslContext,
                    projectId = context.projectId,
                    pipelineId = pipelineId,
                    limit = limit,
                    offset = offset
                )
            },
            migrate = { records ->
                processDataMigrateDao.migratePipelineBuildParamCombinationData(
                    migratingShardingDslContext = context.migratingShardingDslContext,
                    pipelineBuildParamCombinationRecords = records
                )
            }
        )
        logger.info("Finished migrating T_PIPELINE_BUILD_PARAM_COMBINATION data for pipeline $pipelineId")
    }
}

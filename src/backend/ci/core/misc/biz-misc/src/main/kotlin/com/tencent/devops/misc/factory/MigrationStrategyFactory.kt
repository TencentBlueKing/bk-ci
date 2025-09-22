package com.tencent.devops.misc.factory

import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.strategy.MigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineAuditResourceMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineBuildLinkedDataMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineBuildSummaryMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineBuildTemplateAcrossInfoMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineCallbackMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineFavorMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineInfoMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineLabelPipelineMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineModelTaskMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineOperationLogMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineRecentUseMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineResourceMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineResourceVersionMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineSettingMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineSettingVersionMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineSubRefMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineTimerBranchMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineTriggerDetailMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineViewGroupMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineWebhookQueueMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineWebhookVersionMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineYamlInfoMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineYamlVersionMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.ReportMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.TemplatePipelineMigrationStrategy
import com.tencent.devops.misc.strategy.impl.project.AuditResourceMigrationStrategy
import com.tencent.devops.misc.strategy.impl.project.PipelineGroupMigrationStrategy
import com.tencent.devops.misc.strategy.impl.project.PipelineJobMutexGroupMigrationStrategy
import com.tencent.devops.misc.strategy.impl.project.PipelineLabelMigrationStrategy
import com.tencent.devops.misc.strategy.impl.project.PipelineViewMigrationStrategy
import com.tencent.devops.misc.strategy.impl.project.PipelineViewTopMigrationStrategy
import com.tencent.devops.misc.strategy.impl.project.PipelineViewUserLastViewMigrationStrategy
import com.tencent.devops.misc.strategy.impl.project.PipelineViewUserSettingsMigrationStrategy
import com.tencent.devops.misc.strategy.impl.project.ProjectPipelineCallbackHistoryMigrationStrategy
import com.tencent.devops.misc.strategy.impl.project.ProjectPipelineCallbackMigrationStrategy
import com.tencent.devops.misc.strategy.impl.project.ProjectPipelineTriggerEventMigrationStrategy
import com.tencent.devops.misc.strategy.impl.project.ProjectPipelineYamlBranchFileMigrationStrategy
import com.tencent.devops.misc.strategy.impl.project.ProjectPipelineYamlSyncMigrationStrategy
import com.tencent.devops.misc.strategy.impl.project.ProjectPipelineYamlViewMigrationStrategy
import com.tencent.devops.misc.strategy.impl.project.TemplateMigrationStrategy
import com.tencent.devops.misc.strategy.impl.project.TemplateSettingMigrationStrategy
import com.tencent.devops.misc.strategy.impl.project.TemplateSettingVersionMigrationStrategy

class MigrationStrategyFactory(private val processDataMigrateDao: ProcessDataMigrateDao) {

    private val projectDataMigrationStrategies = listOf(
        AuditResourceMigrationStrategy(processDataMigrateDao),
        PipelineGroupMigrationStrategy(processDataMigrateDao),
        PipelineJobMutexGroupMigrationStrategy(processDataMigrateDao),
        PipelineLabelMigrationStrategy(processDataMigrateDao),
        PipelineViewMigrationStrategy(processDataMigrateDao),
        PipelineViewTopMigrationStrategy(processDataMigrateDao),
        PipelineViewUserLastViewMigrationStrategy(processDataMigrateDao),
        PipelineViewUserSettingsMigrationStrategy(processDataMigrateDao),
        ProjectPipelineCallbackHistoryMigrationStrategy(processDataMigrateDao),
        ProjectPipelineCallbackMigrationStrategy(processDataMigrateDao),
        ProjectPipelineTriggerEventMigrationStrategy(processDataMigrateDao),
        ProjectPipelineYamlBranchFileMigrationStrategy(processDataMigrateDao),
        ProjectPipelineYamlSyncMigrationStrategy(processDataMigrateDao),
        ProjectPipelineYamlViewMigrationStrategy(processDataMigrateDao),
        TemplateMigrationStrategy(processDataMigrateDao),
        TemplateSettingMigrationStrategy(processDataMigrateDao),
        TemplateSettingVersionMigrationStrategy(processDataMigrateDao)
    )

    private val commonPipelineDataStrategies = listOf(
        PipelineBuildLinkedDataMigrationStrategy(processDataMigrateDao),
        PipelineBuildSummaryMigrationStrategy(processDataMigrateDao),
        PipelineFavorMigrationStrategy(processDataMigrateDao),
        PipelineInfoMigrationStrategy(processDataMigrateDao),
        PipelineLabelPipelineMigrationStrategy(processDataMigrateDao),
        PipelineOperationLogMigrationStrategy(processDataMigrateDao),
        PipelineResourceMigrationStrategy(processDataMigrateDao),
        PipelineResourceVersionMigrationStrategy(processDataMigrateDao),
        PipelineSettingMigrationStrategy(processDataMigrateDao),
        PipelineSettingVersionMigrationStrategy(processDataMigrateDao),
        PipelineViewGroupMigrationStrategy(processDataMigrateDao),
        ReportMigrationStrategy(processDataMigrateDao),
        TemplatePipelineMigrationStrategy(processDataMigrateDao)
    )

    private val nonArchivePipelineDataStrategies = listOf(
        PipelineAuditResourceMigrationStrategy(processDataMigrateDao),
        PipelineBuildTemplateAcrossInfoMigrationStrategy(processDataMigrateDao),
        PipelineCallbackMigrationStrategy(processDataMigrateDao),
        PipelineModelTaskMigrationStrategy(processDataMigrateDao),
        PipelineRecentUseMigrationStrategy(processDataMigrateDao),
        PipelineSubRefMigrationStrategy(processDataMigrateDao),
        PipelineTimerBranchMigrationStrategy(processDataMigrateDao),
        PipelineTriggerDetailMigrationStrategy(processDataMigrateDao),
        PipelineWebhookQueueMigrationStrategy(processDataMigrateDao),
        PipelineWebhookVersionMigrationStrategy(processDataMigrateDao),
        PipelineYamlInfoMigrationStrategy(processDataMigrateDao),
        PipelineYamlVersionMigrationStrategy(processDataMigrateDao)
    )

    /**
     * 获取项目维度数据迁移策略集合
     *
     * @return 项目级迁移策略列表
     */
    fun getProjectDataMigrationStrategies(): List<MigrationStrategy> = projectDataMigrationStrategies

    /**
     * 获取流水线维度数据迁移策略集合
     *
     * @param archiveFlag 流水线归档标志：
     *   - true： 仅返回通用策略（适用于归档流水线）
     *   - false/null： 返回通用策略+非归档专属策略（适用于活跃流水线）
     *
     * @return 按条件组合的流水线迁移策略列表
     */
    fun getPipelineDataMigrationStrategies(archiveFlag: Boolean? = null): List<MigrationStrategy> {
        return buildList {
            addAll(commonPipelineDataStrategies)
            if (archiveFlag != true) {
                // 添加非归档场景专属策略
                addAll(nonArchivePipelineDataStrategies)
            }
        }
    }
}

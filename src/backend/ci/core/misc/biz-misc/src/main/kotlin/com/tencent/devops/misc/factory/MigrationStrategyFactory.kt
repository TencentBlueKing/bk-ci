package com.tencent.devops.misc.factory

import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.strategy.MigrationStrategy
import com.tencent.devops.misc.strategy.impl.AuditResourceMigrationStrategy
import com.tencent.devops.misc.strategy.impl.PipelineGroupMigrationStrategy
import com.tencent.devops.misc.strategy.impl.PipelineJobMutexGroupMigrationStrategy
import com.tencent.devops.misc.strategy.impl.PipelineLabelMigrationStrategy
import com.tencent.devops.misc.strategy.impl.PipelineViewMigrationStrategy
import com.tencent.devops.misc.strategy.impl.PipelineViewTopMigrationStrategy
import com.tencent.devops.misc.strategy.impl.PipelineViewUserLastViewMigrationStrategy
import com.tencent.devops.misc.strategy.impl.PipelineViewUserSettingsMigrationStrategy
import com.tencent.devops.misc.strategy.impl.ProjectPipelineCallbackHistoryMigrationStrategy
import com.tencent.devops.misc.strategy.impl.ProjectPipelineCallbackMigrationStrategy
import com.tencent.devops.misc.strategy.impl.ProjectPipelineTriggerEventMigrationStrategy
import com.tencent.devops.misc.strategy.impl.ProjectPipelineYamlBranchFileMigrationStrategy
import com.tencent.devops.misc.strategy.impl.ProjectPipelineYamlSyncMigrationStrategy
import com.tencent.devops.misc.strategy.impl.ProjectPipelineYamlViewMigrationStrategy
import com.tencent.devops.misc.strategy.impl.TemplateMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineAuditResourceMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineBuildContainerMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineBuildLinkedDataMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineBuildStageMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineBuildSummaryMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineBuildTaskMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineBuildTemplateAcrossInfoMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineCallbackMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineFavorMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineInfoMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineLabelPipelineMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineModelTaskMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineOperationLogMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineRecentUseMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineRemoteAuthMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineResourceMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineResourceVersionMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineSettingMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineSettingVersionMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineSubRefMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineTimerBranchMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineTriggerDetailMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineViewGroupMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineWebhookMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineWebhookQueueMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineWebhookVersionMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineYamlInfoMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.PipelineYamlVersionMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.ReportMigrationStrategy
import com.tencent.devops.misc.strategy.impl.pipeline.TemplatePipelineMigrationStrategy

class MigrationStrategyFactory(private val processDataMigrateDao: ProcessDataMigrateDao) {

    fun getProjectMigrationStrategies(): List<MigrationStrategy> {
        return listOf(
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
            TemplateMigrationStrategy(processDataMigrateDao)
        )
    }

    fun getPipelineMigrationStrategies(): List<MigrationStrategy> {
        return listOf(
            PipelineAuditResourceMigrationStrategy(processDataMigrateDao),
            PipelineBuildContainerMigrationStrategy(processDataMigrateDao),
            PipelineBuildLinkedDataMigrationStrategy(processDataMigrateDao),
            PipelineBuildStageMigrationStrategy(processDataMigrateDao),
            PipelineBuildSummaryMigrationStrategy(processDataMigrateDao),
            PipelineBuildTaskMigrationStrategy(processDataMigrateDao),
            PipelineBuildTemplateAcrossInfoMigrationStrategy(processDataMigrateDao),
            PipelineCallbackMigrationStrategy(processDataMigrateDao),
            PipelineFavorMigrationStrategy(processDataMigrateDao),
            PipelineInfoMigrationStrategy(processDataMigrateDao),
            PipelineLabelPipelineMigrationStrategy(processDataMigrateDao),
            PipelineModelTaskMigrationStrategy(processDataMigrateDao),
            PipelineOperationLogMigrationStrategy(processDataMigrateDao),
            PipelineRecentUseMigrationStrategy(processDataMigrateDao),
            PipelineRemoteAuthMigrationStrategy(processDataMigrateDao),
            PipelineResourceMigrationStrategy(processDataMigrateDao),
            PipelineResourceVersionMigrationStrategy(processDataMigrateDao),
            PipelineSettingMigrationStrategy(processDataMigrateDao),
            PipelineSettingVersionMigrationStrategy(processDataMigrateDao),
            PipelineSubRefMigrationStrategy(processDataMigrateDao),
            PipelineTimerBranchMigrationStrategy(processDataMigrateDao),
            PipelineTriggerDetailMigrationStrategy(processDataMigrateDao),
            PipelineViewGroupMigrationStrategy(processDataMigrateDao),
            PipelineWebhookMigrationStrategy(processDataMigrateDao),
            PipelineWebhookQueueMigrationStrategy(processDataMigrateDao),
            PipelineWebhookVersionMigrationStrategy(processDataMigrateDao),
            PipelineYamlInfoMigrationStrategy(processDataMigrateDao),
            PipelineYamlVersionMigrationStrategy(processDataMigrateDao),
            ReportMigrationStrategy(processDataMigrateDao),
            TemplatePipelineMigrationStrategy(processDataMigrateDao)
        )
    }
}
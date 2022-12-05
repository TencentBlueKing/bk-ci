package com.tencent.devops.process.permission.config

import com.tencent.devops.common.client.Client
import com.tencent.devops.process.engine.service.PipelineNotifyService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.notify.command.impl.NotifyPipelineCmd
import com.tencent.devops.process.notify.command.impl.NotifyReceiversCmd
import com.tencent.devops.process.notify.command.impl.NotifyUrlBuildCmd
import com.tencent.devops.process.permission.notify.BluekingNotifyPipelineCmd
import com.tencent.devops.process.permission.notify.BluekingNotifyReceiversCmdImpl
import com.tencent.devops.process.permission.notify.BluekingNotifyUrlCmdImpl
import com.tencent.devops.process.permission.service.impl.BluekingPipelineNotifyService
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class PipelineNotifyConfiguration {
    @Bean
    @ConditionalOnMissingBean(PipelineNotifyService::class)
    fun pipelineNotifyService(
        buildVariableService: BuildVariableService,
        pipelineRepositoryService: PipelineRepositoryService
    ) = BluekingPipelineNotifyService(buildVariableService, pipelineRepositoryService)

    @Bean
    @ConditionalOnMissingBean(NotifyUrlBuildCmd::class)
    fun notifyUrlBuildCmd(pipelineRepositoryService: PipelineRepositoryService) =
        BluekingNotifyUrlCmdImpl(pipelineRepositoryService)

    @Bean
    @ConditionalOnMissingBean(NotifyPipelineCmd::class)
    fun notifyPipelineCmd(
        pipelineRepositoryService: PipelineRepositoryService,
        pipelineRuntimeService: PipelineRuntimeService,
        pipelineBuildFacadeService: PipelineBuildFacadeService,
        client: Client,
        buildVariableService: BuildVariableService
    ) = BluekingNotifyPipelineCmd(
        pipelineRepositoryService = pipelineRepositoryService,
        pipelineRuntimeService = pipelineRuntimeService,
        pipelineBuildFacadeService = pipelineBuildFacadeService,
        client = client,
        buildVariableService = buildVariableService
    )

    @Bean
    @ConditionalOnMissingBean(NotifyReceiversCmd::class)
    fun notifyReceiversCmd() = BluekingNotifyReceiversCmdImpl()
}

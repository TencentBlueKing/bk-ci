package com.tencent.devops.process.notify

import com.tencent.devops.common.client.Client
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.notify.command.impl.NotifyPipelineCmd
import com.tencent.devops.process.notify.command.impl.NotifyUrlBuildCmd
import com.tencent.devops.process.notify.command.impl.BluekingNotifyPipelineCmd
import com.tencent.devops.process.notify.command.impl.BluekingNotifySendCmd
import com.tencent.devops.process.notify.command.impl.BluekingNotifyUrlCmdImpl
import com.tencent.devops.process.notify.command.impl.NotifySendCmd
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
    @ConditionalOnMissingBean(NotifySendCmd::class)
    fun notifySendCmd(client: Client) = BluekingNotifySendCmd(client)
}

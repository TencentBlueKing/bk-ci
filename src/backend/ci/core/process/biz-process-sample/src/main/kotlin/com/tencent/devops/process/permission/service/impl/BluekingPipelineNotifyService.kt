package com.tencent.devops.process.permission.service.impl

import com.tencent.devops.process.engine.service.PipelineNotifyService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.notify.command.NotifyCmd
import com.tencent.devops.process.service.BuildVariableService
import org.springframework.beans.factory.annotation.Autowired

class BluekingPipelineNotifyService @Autowired constructor(
    override val buildVariableService: BuildVariableService,
    override val pipelineRepositoryService: PipelineRepositoryService,
    override val pipelineRuntimeService: PipelineRuntimeService
) : PipelineNotifyService(
    buildVariableService,
    pipelineRepositoryService,
    pipelineRuntimeService
) {
    override fun addExtCmd(): MutableList<NotifyCmd>? {
        return null
    }
}

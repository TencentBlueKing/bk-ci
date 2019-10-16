package com.tencent.devops.dispatch.service.dispatcher

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.type.tstack.TStackDispatchType
import com.tencent.devops.dispatch.service.TstackBuildService
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TStackDispatcher @Autowired constructor(
    private val client: Client,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val rabbitTemplate: RabbitTemplate,
    private val tstackBuildService: TstackBuildService
) : Dispatcher {
    override fun canDispatch(pipelineAgentStartupEvent: PipelineAgentStartupEvent) =
        pipelineAgentStartupEvent.dispatchType is TStackDispatchType

    override fun startUp(pipelineAgentStartupEvent: PipelineAgentStartupEvent) {
        val startSuccess = tstackBuildService.startTstackBuild(pipelineAgentStartupEvent)
        if (!startSuccess) {
            logger.warn("Start tstack build failed 0 $pipelineAgentStartupEvent, retry")
            retry(client, rabbitTemplate, pipelineEventDispatcher, pipelineAgentStartupEvent)
        }
    }

    override fun shutdown(pipelineAgentShutdownEvent: PipelineAgentShutdownEvent) {
        tstackBuildService.finishTstackBuild(
            pipelineAgentShutdownEvent.buildId,
            pipelineAgentShutdownEvent.vmSeqId,
            pipelineAgentShutdownEvent.buildResult
        )
    }

//    override fun canDispatch(buildMessage: PipelineBuildMessage) =
//        buildMessage.dispatchType.buildType == BuildType.TSTACK
//
//    override fun build(buildMessage: PipelineBuildMessage) {
//        val startSuccess = tstackBuildService.startTstackBuild(buildMessage)
//        if (!startSuccess) {
//            logger.warn("Start tstack build failed 0 $buildMessage, retry")
//            retry(rabbitTemplate, buildMessage)
//        }
//    }
//
//    override fun finish(buildFinishMessage: PipelineFinishMessage) {
//        tstackBuildService.finishTstackBuild(
//            buildFinishMessage.buildId,
//            buildFinishMessage.vmSeqId,
//            buildFinishMessage.buildResult
//        )
//    }

    companion object {
        private val logger = LoggerFactory.getLogger(TStackDispatcher::class.java)
    }
}

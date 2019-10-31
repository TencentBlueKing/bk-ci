package com.tencent.devops.gitci.listener

import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.gitci.service.GitCIBuildService
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.ExchangeTypes
import org.springframework.amqp.rabbit.annotation.Exchange
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.QueueBinding
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GitCIRequestTriggerListener @Autowired
constructor(private val gitCIBuildService: GitCIBuildService) {

    @RabbitListener(
        bindings = [(QueueBinding(
            key = MQ.ROUTE_GITCI_REQUEST_TRIGGER_EVENT, value = Queue(value = MQ.QUEUE_GITCI_REQUEST_TRIGGER_EVENT, durable = "true"),
            exchange = Exchange(
                value = MQ.EXCHANGE_GITCI_REQUEST_TRIGGER_EVENT,
                durable = "true",
                delayed = "true",
                type = ExchangeTypes.DIRECT
            )
        ))]
    )
    fun listenGitCIRequestTriggerEvent(gitCIRequestTriggerEvent: GitCIRequestTriggerEvent) {
        try {
            gitCIBuildService.gitCIBuild(gitCIRequestTriggerEvent.event, gitCIRequestTriggerEvent.yaml)
        } catch (e: Throwable) {
            logger.error("Fail to start the git ci build(${gitCIRequestTriggerEvent.event})", e)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GitCIRequestTriggerListener::class.java)
    }
}

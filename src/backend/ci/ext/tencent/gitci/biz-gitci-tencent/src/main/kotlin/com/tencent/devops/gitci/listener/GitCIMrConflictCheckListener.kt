package com.tencent.devops.gitci.listener

import com.tencent.devops.gitci.constant.MQ
import com.tencent.devops.gitci.service.GitCITriggerService
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.ExchangeTypes
import org.springframework.amqp.rabbit.annotation.Exchange
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.QueueBinding
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GitCIMrConflictCheckListener @Autowired
constructor(
    private val gitCITriggerService: GitCITriggerService,
    private val rabbitTemplate: RabbitTemplate
) {

    @RabbitListener(
        bindings = [(QueueBinding(
            key = MQ.ROUTE_GITCI_MR_CONFLICT_CHECK_EVENT, value = Queue(value = MQ.QUEUE_GITCI_MR_CONFLICT_CHECK_EVENT, durable = "true"),
            exchange = Exchange(
                value = MQ.EXCHANGE_GITCI_MR_CONFLICT_CHECK_EVENT,
                durable = "true",
                delayed = "true",
                type = ExchangeTypes.DIRECT
            )
        ))]
    )
    fun listenGitCIRequestTriggerEvent(checkEvent: GitCIMrConflictCheckEvent) {

        val result = with(checkEvent) {
            gitCITriggerService.checkMrConflictByListener(
                token = token,
                gitProjectConf = gitProjectConf,
                path2PipelineExists = path2PipelineExists,
                event = event,
                gitRequestEvent = gitRequestEvent,
                isEndCheck = retryTime == 1,
                notBuildRecordId = notBuildRecordId
            )
        }
        // 未检查完成，继续进入延时队列
        if (!result && checkEvent.retryTime > 0) {
            logger.warn("Retry to check gitci mr request conflict event [${checkEvent.gitRequestEvent}|${checkEvent.retryTime}]")
            checkEvent.retryTime--
            GitCIMrConflictCheckDispatcher.dispatch(rabbitTemplate, checkEvent)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GitCIMrConflictCheckListener::class.java)
    }
}
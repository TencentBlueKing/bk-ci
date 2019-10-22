package com.tencent.devops.lambda.listener

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class LambdaListener {

//    @KafkaListener(
//        topics = [PIPELINE_BUILD_TOPIC],
//        containerFactory = "kafkaListenerContainerFactory"
//    )
//    fun onReceivePipelineBuild(
//        cr: ConsumerRecord<String, PracticalAdvice>,
//        @Payload payload: PracticalAdvice
//    ) {
//        logger.info(
//            "Logger 1 [JSON] received key {}: Type [{}] | Payload: {} | Record: {}", cr.key(),
//            typeIdHeader(cr.headers()), payload, cr.toString()
//        )
//    }

    companion object {
        private val logger = LoggerFactory.getLogger(LambdaListener::class.java)
    }
}
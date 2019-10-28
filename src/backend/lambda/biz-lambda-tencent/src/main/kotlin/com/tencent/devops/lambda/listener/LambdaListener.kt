package com.tencent.devops.lambda.listener

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class LambdaListener {
    companion object {
        private val logger = LoggerFactory.getLogger(LambdaListener::class.java)
    }
}
package com.tencent.devops.store.service

import com.tencent.devops.common.client.Client
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ExtServiceBcsInitService @Autowired constructor(
    private val client: Client
) {

    private val logger = LoggerFactory.getLogger(ExtServiceBcsInitService::class.java)

    fun initBcsNamespace(serviceId: String) {
        logger.info("the serviceId is :$serviceId")
    }

    fun initBcsImagePullSecret(serviceId: String) {
        logger.info("the serviceId is :$serviceId")
    }
}
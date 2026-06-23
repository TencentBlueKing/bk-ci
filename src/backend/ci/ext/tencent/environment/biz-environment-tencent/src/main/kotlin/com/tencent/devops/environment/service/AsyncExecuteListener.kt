package com.tencent.devops.environment.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.environment.config.async.AsyncExecuteEvent
import com.tencent.devops.environment.config.async.AsyncExecuteEventType
import com.tencent.devops.environment.pojo.AsyncInstallImateData
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AsyncExecuteListener @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val tencentNodeService: TencentNodeService
) {

    fun listenAsyncExecuteEvent(event: AsyncExecuteEvent) {
        logger.info("listenAsyncExecuteEvent|$event")
        try {
            doExecute(event)
        } catch (e: Throwable) {
            logger.error("listenAsyncExecuteEvent|${event.type}|${event.eventStr}|error", e)
        }
    }

    private fun doExecute(event: AsyncExecuteEvent) {
        when (event.type) {
            AsyncExecuteEventType.ASYNC_INSTALL_IMATE -> {
                val data = objectMapper.readValue<AsyncInstallImateData>(event.eventStr)
                tencentNodeService.importImateCallBack(data)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AsyncExecuteListener::class.java)
    }
}
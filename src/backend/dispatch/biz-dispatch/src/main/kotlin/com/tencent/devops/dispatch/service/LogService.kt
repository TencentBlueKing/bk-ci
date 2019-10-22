package com.tencent.devops.dispatch.service

import com.tencent.devops.log.utils.LogUtils
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class LogService @Autowired constructor(private val rabbitTemplate: RabbitTemplate) {

    companion object {
        private val logger = LoggerFactory.getLogger(LogService::class.java)
    }

    /**
     * 构建日志输出结束
     * @param buildId 构建ID
     */
    fun stopLog(buildId: String): Boolean {
        try {
            LogUtils.stopLog(rabbitTemplate, buildId, "", null)
        } catch (e: Exception) {
            logger.error("Fail to stop log status of build($buildId)", e)
        }
        return false
    }
}
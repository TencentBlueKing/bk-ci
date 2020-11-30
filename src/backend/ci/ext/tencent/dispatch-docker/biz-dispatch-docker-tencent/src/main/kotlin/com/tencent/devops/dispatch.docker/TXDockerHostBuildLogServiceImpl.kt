package com.tencent.devops.dispatch.docker

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.kafka.KafkaClient
import com.tencent.devops.common.kafka.KafkaTopic
import com.tencent.devops.dispatch.docker.pojo.FormatLog
import com.tencent.devops.dispatch.docker.service.DockerHostBuildLogService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TXDockerHostBuildLogServiceImpl @Autowired constructor(
    private val kafkaClient: KafkaClient
) : DockerHostBuildLogService {

    private val logger = LoggerFactory.getLogger(TXDockerHostBuildLogServiceImpl::class.java)

    override fun sendFormatLog(formatLog: FormatLog): Boolean {
        logger.info("send formatLog: $formatLog")
        kafkaClient.send(KafkaTopic.LANDUN_LOG_FORMAT_TOPIC, JsonUtil.toJson(formatLog))
        return true
    }
}

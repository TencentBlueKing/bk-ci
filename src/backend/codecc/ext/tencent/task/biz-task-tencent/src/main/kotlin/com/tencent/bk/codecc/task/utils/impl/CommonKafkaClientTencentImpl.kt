package com.tencent.bk.codecc.task.utils.impl

import com.tencent.bk.codecc.task.model.TaskInfoEntity
import com.tencent.bk.codecc.task.utils.CommonKafkaClient
import com.tencent.devops.common.kafka.KafkaClient
import com.tencent.devops.common.kafka.KafkaTopic.TASK_DETAIL_TOPIC
import com.tencent.devops.common.util.JsonUtil.toJson
import com.tencent.devops.common.util.JsonUtil.toMap
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import java.text.SimpleDateFormat
import java.util.*

@Component
@Primary
class CommonKafkaClientTencentImpl @Autowired constructor(
    private val kafkaClient: KafkaClient
): CommonKafkaClient {

    companion object {
        private val log = LoggerFactory.getLogger(CommonKafkaClientTencentImpl::class.java)
    }

    /**
     * 将任务详情推送到数据平台
     * @param taskInfoEntity
     */
    override fun pushTaskDetailToKafka(taskInfoEntity: TaskInfoEntity) {
        val taskInfoMap = toMap(taskInfoEntity).toMutableMap()
        val dateString = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        taskInfoMap["washTime"] = dateString
        try {
            kafkaClient.send(TASK_DETAIL_TOPIC, toJson(taskInfoMap))
        } catch (e: Exception) {
            log.error("send task info to kafka failed!", e)
        }
    }
}
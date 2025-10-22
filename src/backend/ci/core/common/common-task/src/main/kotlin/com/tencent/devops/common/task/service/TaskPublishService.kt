package com.tencent.devops.common.task.service

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.task.event.BatchTaskPublishEvent
import com.tencent.devops.common.task.listener.BatchTaskFinishListener
import com.tencent.devops.common.task.pojo.TaskTypeEnum
import com.tencent.devops.common.task.util.BatchTaskUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class TaskPublishService @Autowired constructor(
    private val sampleEventDispatcher: SampleEventDispatcher,
    private val redisOperation: RedisOperation
) {

    /**
     * 批量发布任务
     *
     * @param userId 用户ID
     * @param taskType 任务类型枚举
     * @param dataList 任务数据列表
     * @param expiredInHour Redis键过期时间（单位：小时，默认12小时）
     * @param targetService 目标微服务
     * @return 批次ID
     */
    fun publishTasks(
        userId: String,
        taskType: TaskTypeEnum,
        dataList: List<Map<String, Any>>,
        expiredInHour: Long = 12,
        targetService: String? = null
    ): String {
        logger.info(
            "publishTasks|userId=$userId|taskType=$taskType|dataList=$dataList|" +
                    "expiredInHour=$expiredInHour|targetService=$targetService"
        )
        val expiredInSecond = expiredInHour * 3600
        val batchId = UUIDUtil.generate()
        // 批量设置Redis相关批次信息
        redisOperation.apply {
            // 在Redis中设置批次任务执行开始时间
            set(
                key = BatchTaskUtil.generateBatchTaskStartTimeKey(taskType, batchId),
                value = DateTimeUtil.toDateTime(LocalDateTime.now()),
                expiredInSecond = expiredInSecond
            )
            // 在Redis中设置批次任务执行开始时间
            set(
                key = BatchTaskUtil.generateBatchTaskTotalKey(taskType, batchId),
                value = dataList.size.toString(),
                expiredInSecond = expiredInSecond
            )
            // 初始化已完成任务数为0
            set(
                key = BatchTaskUtil.generateBatchTaskCompletedKey(taskType, batchId),
                value = "0",
                expiredInSecond = expiredInSecond
            )
        }

        // 批量分发任务事件
        dataList.forEachIndexed { index, dataMap ->
            sampleEventDispatcher.dispatch(
                BatchTaskPublishEvent(
                    userId = userId,
                    taskType = taskType,
                    batchId = batchId,
                    taskId = "${batchId}_$index",
                    data = dataMap,
                    expiredInHour = expiredInHour,
                    targetService = targetService
                )
            )
        }
        logger.info("publishTasks|userId=$userId|taskType=$taskType|batchId=$batchId")
        return batchId
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BatchTaskFinishListener::class.java)
    }
}
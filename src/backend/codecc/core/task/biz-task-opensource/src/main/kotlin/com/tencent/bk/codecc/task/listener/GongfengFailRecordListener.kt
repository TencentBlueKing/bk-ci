package com.tencent.bk.codecc.task.listener

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.bk.codecc.task.dao.mongorepository.BaseDataRepository
import com.tencent.bk.codecc.task.dao.mongorepository.NewTaskRetryRecordRepository
import com.tencent.bk.codecc.task.dao.mongorepository.TaskFailRecordRepository
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository
import com.tencent.bk.codecc.task.model.TaskFailRecordEntity
import com.tencent.bk.codecc.task.pojo.TriggerPipelineModel
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.web.mq.EXCHANGE_GONGFENG_CODECC_SCAN
import com.tencent.devops.common.web.mq.ROUTE_GONGFENG_TRIGGER_PIPELINE
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

@Component
class GongfengFailRecordListener @Autowired constructor(
    private val taskFailRecordRepository: TaskFailRecordRepository,
    private val taskRepository: TaskRepository,
    private val baseDataRepository: BaseDataRepository,
    private val newTaskRetryRecordRepository: NewTaskRetryRecordRepository,
    private val gongfengRetryRecordListener: GongfengRetryRecordListener,
    private val rabbitTemplate: RabbitTemplate
) {

    companion object {
        private val logger = LoggerFactory.getLogger(GongfengFailRecordListener::class.java)
    }

    /**
     * 用于缓存开源扫描时间周期
     */
    private val periodInfoCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .build<String, Int>(
            object : CacheLoader<String, Int>() {
                override fun load(paramType: String): Int {
                    return getOpenSourceCheckPeriod(paramType)
                }
            }
        )

    /**
     * 取出当前未执行的报错的前几个任务，分别执行重试下发
     */
    fun openSourceFailTaskRetry(flag: String) {
        try {
            logger.info("start to retry fail open source task")
            // 如果是period点前，下发数字为10，如果是period点后，下发数字为50
            val period = periodInfoCache.get("PERIOD")
            val startTime = periodInfoCache.get("STARTTIME")
            // 超过界限之外3小时就不做操作
            val conclusionTime = LocalDateTime.of(
                LocalDate.now().year,
                LocalDate.now().month,
                LocalDate.now().dayOfMonth,
                (startTime + period + 5) % 24,
                0
            )
            if (LocalDateTime.now() > conclusionTime) {
                logger.info("no operation 3 hours after period")
                return
            }
            val judgeTime = LocalDateTime.of(
                LocalDate.now().year,
                LocalDate.now().month,
                LocalDate.now().dayOfMonth,
                (startTime + period) % 24,
                0
            )

            var executeNum = if (LocalDateTime.now() > judgeTime) {
                100
            } else {
                20
            }
            // 耗时超过3小时的任务不重试
            val openSourceFailList =
                taskFailRecordRepository.findByUploadTimeGreaterThanAndRetryFlagIsAndTimeCostLessThanAndProjectIdNotOrderByUploadTimeAsc(
                    LocalDate.now().atStartOfDay(
                        ZoneOffset.ofHours(8)
                    ).toInstant().toEpochMilli(), false,
                    3600000 * 3L,
                    "CUSTOMPROJ_PCG_RD"
                )

            val executedIdList = mutableSetOf<String>()
            /**
             * 重试失败的开源任务
             */
            if (!openSourceFailList.isNullOrEmpty()) {
                executeNum = if (executeNum > openSourceFailList.size) openSourceFailList.size else executeNum
                logger.info("execute num is $executeNum")
                var i = 0
                var j = 0
                val executedFailEntity = mutableListOf<TaskFailRecordEntity>()
                loop@ while (i < executeNum) {
                    try {
                        // 本次补录，只要执行过的，就不会再执行
                        if (j >= openSourceFailList.size) {
                            break
                        }
                        var openSourceFailEntity =
                            openSourceFailList[if (j >= openSourceFailList.size) break@loop else j++]
                        executedFailEntity.add(openSourceFailEntity)
                        var taskInfoEntity = taskRepository.findByTaskId(openSourceFailEntity.taskId)
                        while (null == taskInfoEntity || taskInfoEntity.pipelineId.isNullOrBlank() || executedIdList.contains(
                                taskInfoEntity.pipelineId
                            )
                        ) {
                            Thread.sleep(100L)
                            openSourceFailEntity =
                                openSourceFailList[if (j >= openSourceFailList.size) break@loop else j++]
                            taskInfoEntity = taskRepository.findByTaskId(openSourceFailEntity.taskId)
                            executedFailEntity.add(openSourceFailEntity)
                        }
                        val triggerPipelineModel = TriggerPipelineModel(
                            projectId = taskInfoEntity.projectId,
                            pipelineId = taskInfoEntity.pipelineId,
                            taskId = taskInfoEntity.taskId,
                            gongfengId = taskInfoEntity.gongfengProjectId,
                            owner = if (!taskInfoEntity.taskOwner.isNullOrEmpty()) taskInfoEntity.taskOwner[0] else "CodeCC"
                        )
                        rabbitTemplate.convertAndSend(
                            EXCHANGE_GONGFENG_CODECC_SCAN,
                            ROUTE_GONGFENG_TRIGGER_PIPELINE,
                            triggerPipelineModel
                        )
                        executedIdList.add(taskInfoEntity.pipelineId)
                        executedFailEntity.forEach {
                            it.retryFlag = true
                        }
                        taskFailRecordRepository.save(executedFailEntity)
                        i++
                    } catch (e: Exception) {
                        logger.info("execute retry record fail! index : $i")
                    }
                }
            } else {
                logger.info("open source task list is empty!")
            }

            /**
             * 新增开源扫描任务
             */
            if (LocalDateTime.now() > judgeTime) {
                logger.info("start to retry new open source project")
                val newTaskRetryList =
                    newTaskRetryRecordRepository.findByUploadTimeGreaterThanAndRetryFlagIsOrderByUploadTimeAsc(
                        LocalDate.now().atStartOfDay(
                            ZoneOffset.ofHours(8)
                        ).toInstant().toEpochMilli(), false
                    )
                if (!newTaskRetryList.isNullOrEmpty()) {
                    executeNum =
                        if ((200 - executeNum) > newTaskRetryList.size) newTaskRetryList.size else (200 - executeNum)
                    logger.info("new task execute num is $executeNum")
                    for (k in 0 until executeNum) {
                        try {
                            val newTaskRetryEntity = newTaskRetryList[k]
                            val taskInfoEntity = taskRepository.findByTaskId(newTaskRetryEntity.taskId)
                            if (null == taskInfoEntity || taskInfoEntity.pipelineId.isNullOrBlank()) {
                                continue
                            }
                            val triggerPipelineModel = TriggerPipelineModel(
                                projectId = taskInfoEntity.projectId,
                                pipelineId = taskInfoEntity.pipelineId,
                                taskId = taskInfoEntity.taskId,
                                gongfengId = taskInfoEntity.gongfengProjectId,
                                owner = if (!taskInfoEntity.taskOwner.isNullOrEmpty()) taskInfoEntity.taskOwner[0] else "CodeCC"
                            )
                            rabbitTemplate.convertAndSend(
                                EXCHANGE_GONGFENG_CODECC_SCAN,
                                ROUTE_GONGFENG_TRIGGER_PIPELINE,
                                triggerPipelineModel
                            )
                            newTaskRetryEntity.retryFlag = true
                            newTaskRetryRecordRepository.save(newTaskRetryEntity)
                        } catch (e: Exception) {
                            logger.info("execute new task fail! index : $k")
                        }
                    }
                }
            }

            if (Duration.between(LocalDateTime.now(), conclusionTime).toMinutes() in 0..10) {
                logger.info("start to compensate for all fail records!")
                gongfengRetryRecordListener.retryAllFailRecord()
            }
            logger.info("open source task retry finish!")
        } catch (e: Exception) {
            e.printStackTrace()
            logger.info("retry open source task fail!")
        }
    }

    /**
     * 获取开源扫描时间周期和时间起点
     */
    private fun getOpenSourceCheckPeriod(paramCode: String): Int {
        val baseDataEntity =
            baseDataRepository.findAllByParamTypeAndParamCode(ComConstants.KEY_OPENSOURCE_PERIOD, paramCode)
        // 如果是周期的默认值是24，起点的默认值是0
        return if (baseDataEntity.isNullOrEmpty()) {
            if (paramCode == "PERIOD") 24 else 0
        } else {
            try {
                baseDataEntity[0].paramValue.toInt()
            } catch (e: Exception) {
                if (paramCode == "PERIOD") 24 else 0
            }
        }
    }
}

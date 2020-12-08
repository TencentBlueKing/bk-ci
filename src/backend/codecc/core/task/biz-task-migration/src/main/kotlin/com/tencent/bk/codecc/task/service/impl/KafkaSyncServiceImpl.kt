package com.tencent.bk.codecc.task.service.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.bk.codecc.task.dao.mongorepository.GongfengActiveProjRepository
import com.tencent.bk.codecc.task.dao.mongorepository.GongfengPublicProjRepository
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository
import com.tencent.bk.codecc.task.model.TaskInfoEntity
import com.tencent.bk.codecc.task.pojo.TofOrganizationInfo
import com.tencent.bk.codecc.task.service.KafkaSyncService
import com.tencent.bk.codecc.task.tof.TofClientApi
import com.tencent.devops.common.api.CommonPageVO
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.pojo.AllProperties
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.kafka.KafkaClient
import com.tencent.devops.common.kafka.KafkaTopic
import com.tencent.devops.common.util.JsonUtil
import com.tencent.devops.common.util.OkhttpUtils
import com.tencent.devops.common.web.mq.EXCHANGE_KAFKA_DATA_PLATFORM
import com.tencent.devops.common.web.mq.QUEUE_KAFKA_DATA_TRIGGER_TASK
import com.tencent.devops.common.web.mq.ROUTE_KAFKA_DATA_TRIGGER_TASK
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.Exchange
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.QueueBinding
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
open class KafkaSyncServiceImpl @Autowired constructor(
        private val rabbitTemplate: RabbitTemplate,
        private val taskRepository: TaskRepository,
        private val objectMapper: ObjectMapper,
        private val gongfengPublicProjRepository: GongfengPublicProjRepository,
        private val gongfengActiveProjRepository: GongfengActiveProjRepository,
        private val tofClientApi: TofClientApi,
        private val kafkaClient: KafkaClient,
        private val client : Client,
        private val allProperties: AllProperties
) : KafkaSyncService {

    companion object {
        private val logger = LoggerFactory.getLogger(KafkaSyncServiceImpl::class.java)
    }

    @RabbitListener(
            bindings = [QueueBinding(
                    key = ROUTE_KAFKA_DATA_TRIGGER_TASK,
                    value = Queue(value = QUEUE_KAFKA_DATA_TRIGGER_TASK, durable = "true"),
                    exchange = Exchange(
                            value = EXCHANGE_KAFKA_DATA_PLATFORM,
                            durable = "true",
                            delayed = "true",
                            type = "topic"
                    )
            )]
    )
    override fun syncTaskInfoToKafka(startTrigger: String) {
        try {
            logger.info("kafka data synchronization start! start tigger : $startTrigger")
            if (startTrigger != "1") {
                logger.info("msg not from quartz module!")
                return
            }
            val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val nowTime = LocalDateTime.now().format(dateTimeFormatter)
            // 同步工蜂project
            sendGongfengProject(nowTime)

            // 同步工蜂task
            sendGongfengSourceTask(nowTime)

            // 同步蓝盾pipeline数据
            val landunTaskList = taskRepository.findByCreateFrom("bs_pipeline")
            sendLandunTaskDetail(nowTime, landunTaskList)

            // 同步工蜂活跃数据
            sendActiveProject(nowTime)
            logger.info("kafka data sync finish!")
        } catch (e: Exception) {
            logger.error("kafka data sync fail! msg: ${e.message}")
            e.printStackTrace()
        }

    }


    override fun getTaskInfoByCreateFrom(taskType: String, reqVO: CommonPageVO): Page<Long> {
        logger.info("start get taskInfo by createFrom")
        val taskIdList = mutableListOf<Long>()
        //封装分页类
        val pageSort = Sort(Sort.Direction.DESC, "task_id")
        val pageable: Pageable =
            PageRequest(
                if (null == reqVO.pageNum) 0 else reqVO.pageNum - 1,
                if (null == reqVO.pageSize) 10 else reqVO.pageSize,
                pageSort
            )

        val taskListPage = taskRepository.findByCreateFrom(taskType, pageable)
        val taskList = taskListPage.content
        logger.info("task list size: ${taskList.size}")
        taskList.forEach {
            taskIdList.add(it.taskId)
        }
        logger.info("finish get taskInfo by createFrom!")
        return Page(
            taskListPage.totalElements,
            if (null == reqVO.pageNum) 0 else reqVO.pageNum,
            if (null == reqVO.pageSize) 10 else reqVO.pageSize,
            taskListPage.totalPages,
            taskIdList
        )
    }

    override fun syncTaskInfoToKafkaByType(dataType: String, washTime: String): Boolean {
        logger.info("syncTaskInfoToKafkaByType dataType: {}, washTime: {}", dataType, washTime)
        when (dataType) {
            "activeProject" -> {
                sendActiveProject(washTime)
            }
            "landunTask" -> {
                val landunTaskList = taskRepository.findByCreateFrom("bs_pipeline")
                sendLandunTaskDetail(washTime, landunTaskList)
            }
            "gitSourceTask" -> {
                sendGongfengSourceTask(washTime)
            }
            "gitProject" -> {
                sendGongfengProject(washTime)
            }
            "sendGroupInfo" -> {
                sendGroupInfo(washTime)
            }
        }
        return true
    }

    private fun sendGongfengProject(washTime: String) {
        val localWashTime = if (washTime == "") {
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        } else {
            washTime
        }

        val gongfengProjList = gongfengPublicProjRepository.findAll()
        logger.info("gongfeng proj size: ${gongfengProjList.size}")
        gongfengProjList.forEach {
            val gongfengMap = JsonUtil.toMap(it).toMutableMap()
            gongfengMap["washTime"] = localWashTime
            /*rabbitTemplate.convertAndSend(
                EXCHANGE_KAFKA_DATA_PLATFORM,
                ROUTE_KAFKA_DATA_GONGFENG_PROJECT,
                objectMapper.writeValueAsString(gongfengMap)
            )*/
            kafkaClient.send(KafkaTopic.GONGFENG_PROJECT_TOPIC, objectMapper.writeValueAsString(gongfengMap))
        }
    }

    private fun sendGongfengSourceTask(washTime: String) {
        val localWashTime = if (washTime == "") {
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        } else {
            washTime
        }

        val taskList = taskRepository.findByCreateFrom("gongfeng_scan")
        logger.info("task list size: ${taskList.size}")
        taskList.forEach {
            val taskMap = JsonUtil.toMap(it).toMutableMap()
            taskMap["washTime"] = localWashTime
            taskMap["openSource"] = true
            taskMap["activity"] = true
            taskMap["devopsUrl"] =
                    "http://${allProperties.codeccGateWay}/codecc/${it.projectId}/task/${it.taskId}/detail"
/*            rabbitTemplate.convertAndSend(
                EXCHANGE_KAFKA_DATA_PLATFORM,
                ROUTE_KAFKA_DATA_TASK_DETAIL,
                objectMapper.writeValueAsString(taskMap)
            )*/
            kafkaClient.send(KafkaTopic.TASK_DETAIL_TOPIC, objectMapper.writeValueAsString(taskMap))
        }
    }

    private fun sendActiveProject(washTime: String) {
        val localWashTime = if (washTime == "") {
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        } else {
            washTime
        }
        val activeProjList = gongfengActiveProjRepository.findAll()
        logger.info("active list size: ${activeProjList.size}")
        activeProjList.forEach {
            it.gitPath = "${allProperties.gitCodePath}/${it.gitPath}.git"
            val activeMap = JsonUtil.toMap(it).toMutableMap()
            activeMap["washTime"] = localWashTime
/*            rabbitTemplate.convertAndSend(
                EXCHANGE_KAFKA_DATA_PLATFORM,
                ROUTE_KAFKA_DATA_ACTIVE_PROJECT,
                objectMapper.writeValueAsString(activeMap)
            )*/
            kafkaClient.send(KafkaTopic.ACTIVE_GONGFENG_PROJECT_TOPIC, objectMapper.writeValueAsString(activeMap))
        }
    }

    private fun sendLandunTaskDetail(washTime: String, taskList: List<TaskInfoEntity>) {
        val localWashTime = if (washTime == "") {
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        } else {
            washTime
        }
        logger.info("task list size: ${taskList.size}")
        taskList.forEach {
            if (it.taskOwner.isNotEmpty() && it.groupId == 0) {
                val staffInfo = tofClientApi.getStaffInfoByUserName(it.taskOwner[0])
                val organizationInfo = tofClientApi.getOrganizationInfoByGroupId(staffInfo.data?.GroupId ?: -1)

                it.groupId = staffInfo.data?.GroupId ?: 0
                it.centerId = organizationInfo?.centerId ?: -1
                it.bgId = organizationInfo?.bgId ?: -1
                it.deptId = organizationInfo?.deptId ?: -1
            }

            val taskMap = JsonUtil.toMap(it).toMutableMap()
            taskMap["washTime"] = localWashTime
            taskMap["openSource"] = false
            taskMap["activity"] = true
            taskMap["devopsUrl"] =
                    "http://${allProperties.codeccGateWay}/codecc/${it.projectId}/task/${it.taskId}/detail"
/*            rabbitTemplate.convertAndSend(
                EXCHANGE_KAFKA_DATA_PLATFORM,
                ROUTE_KAFKA_DATA_TASK_DETAIL,
                objectMapper.writeValueAsString(taskMap)
            )*/
            kafkaClient.send(KafkaTopic.TASK_DETAIL_TOPIC, objectMapper.writeValueAsString(taskMap))
            Thread.sleep(100)
        }
    }

    private fun sendGroupInfo(parentDeptId: String) {
        val childDeptInfos = tofClientApi.getChildDeptInfos(parentDeptId).data
        if (childDeptInfos != null) {
            logger.info("childDeptInfos size : ${childDeptInfos.size}")
            childDeptInfos.forEach{
                if (it.Enabled == "true") {
                    val deptStaffInfos = tofClientApi.getDeptStaffInfos(it.ID.toString()).data
                    if (deptStaffInfos != null) {
                        logger.info("dept: ${it.ID} staff num: ${deptStaffInfos.size}, start loop...")
                        // val mapList = mutableListOf<Map<String, Any>>()
                        val organizationCacheMap = mutableMapOf<Int, TofOrganizationInfo?>()
                        deptStaffInfos.forEach { tofDeptStaffInfo ->
                            val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                            val nowTime = LocalDateTime.now().format(dateTimeFormatter)
                            val staffMap = mutableMapOf<String, Any>()
                            staffMap["washTime"] = nowTime
                            staffMap["EnglishName"] = tofDeptStaffInfo.EnglishName
                            staffMap["FullName"] = tofDeptStaffInfo.FullName
                            staffMap["GroupId"] = tofDeptStaffInfo.GroupId
                            staffMap["GroupName"] = tofDeptStaffInfo.GroupName
                            staffMap["DepartmentId"] = tofDeptStaffInfo.DepartmentId
                            staffMap["DepartmentName"] = tofDeptStaffInfo.DepartmentName

                            val organizationInfo: TofOrganizationInfo?
                            if (organizationCacheMap.containsKey(tofDeptStaffInfo.GroupId)) {
                                organizationInfo = organizationCacheMap[tofDeptStaffInfo.GroupId]
                            } else {
                                organizationInfo = tofClientApi.getOrganizationInfoByGroupId(tofDeptStaffInfo.GroupId)
                                organizationCacheMap[tofDeptStaffInfo.GroupId] = organizationInfo
                            }
                            staffMap["CenterId"] = organizationInfo?.centerId ?: -1
                            staffMap["CenterName"] = organizationInfo?.centerName ?: ""
                            staffMap["BgName"] = organizationInfo?.bgName ?: ""
                            staffMap["BgId"] = organizationInfo?.bgId ?: -1

                            // mapList.add(staffMap)
                            kafkaClient.send("tendata-bkdevops-296-topic-staffInfos", objectMapper.writeValueAsString(staffMap))
                        }
                    } else {
                        logger.error("dept: ${it.ID} staff is null")
                    }
                }
            }
        }
    }



    override fun manualExecuteTriggerPipeline(taskIdList : List<Long>){
        val taskEntityList = taskRepository.findByTaskIdIn(taskIdList)
        taskEntityList.forEach {
            try{
                val toolArray = it.toolNames.split(",").toMutableList()
                if(ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value() != it.createFrom){
                    return
                }
                if (null != it.gongfengFlag && !it.gongfengFlag && toolArray.contains("CLOC")) {
                    toolArray.remove("CLOC")
                }
                val valueMap = mapOf(
                    "_CODECC_FILTER_TOOLS" to (toolArray.joinToString(","))
                )
                logger.info("project id : ${it.projectId}, pipeline id : ${it.pipelineId}")
                OkhttpUtils.doHttpPost("http://${allProperties.devopsDevUrl}/ms/process/api/service/builds/" +
                    "${it.projectId}/${it.pipelineId}?channelCode=GONGFENGSCAN",
                        objectMapper.writeValueAsString(valueMap), mapOf("X-DEVOPS-UID" to "admin")
                    )
//                client.getDevopsService(ServiceBuildResource::class.java).manualStartup("admin",
//                    it.projectId, it.pipelineId, valueMap, ChannelCode.GONGFENGSCAN)
                logger.info("manual trigger pipeline success! pipeline id : ${it.pipelineId}")
            } catch (e : Exception){
                logger.error("manual trigger pipeline fail! pipeline id : ${it.pipelineId}", e)
            }
        }
    }
}
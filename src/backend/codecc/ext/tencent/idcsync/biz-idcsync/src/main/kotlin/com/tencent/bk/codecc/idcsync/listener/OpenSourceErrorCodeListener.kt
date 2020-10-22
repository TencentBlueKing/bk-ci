package com.tencent.bk.codecc.idcsync.listener

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.bk.codecc.idcsync.task.dao.TaskFailRecordRepository
import com.tencent.bk.codecc.idcsync.task.dao.TaskRepository
import com.tencent.bk.codecc.idcsync.task.dao.mongotemplate.BuildIdRelationDao
import com.tencent.bk.codecc.idcsync.task.dao.mongotemplate.TaskDao
import com.tencent.bk.codecc.task.model.TaskFailRecordEntity
import com.tencent.devops.common.api.pojo.ToolRunResult
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.event.pojo.measure.AtomMonitorReportBroadCastEvent
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.util.JsonUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class OpenSourceErrorCodeListener @Autowired constructor(
    private val taskFailRecordRepository: TaskFailRecordRepository,
    private val taskRepository: TaskRepository,
    private val taskDao : TaskDao,
    private val buildIdRelationDao: BuildIdRelationDao,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private val logger = LoggerFactory.getLogger(OpenSourceErrorCodeListener::class.java)
    }

    fun syncErrorCodeInfo(atomMonitorReportBroadCastEvent: AtomMonitorReportBroadCastEvent) {
        val atomMonitorData = atomMonitorReportBroadCastEvent.monitorData
        try {
            logger.info("start to sync pipeline error code, pipeline id: ${atomMonitorData.pipelineId}, " +
                "build id: ${atomMonitorData.buildId}")
            //过滤掉非工蜂的流水线信息
            if(atomMonitorData.channel.isNullOrBlank() ||
                atomMonitorData.channel != ChannelCode.GONGFENGSCAN.name){
                logger.info("channel code is not gongfeng scan! channel code: ${atomMonitorData.channel}")
                return
            }

            val buildNum = atomMonitorData.extData?.get("BK_CI_DEVOPS_BUILD_NUM") as? String

            if (0 == atomMonitorData.errorCode || -1 == atomMonitorData.errorCode) {
                logger.info("success pipeline will not be recorded")
                if(atomMonitorData.atomCode == "CodeccCheckAtomDebug"){
                    logger.info("will update latest scan result")
                    val taskInfoEntity = taskRepository.findByPipelineId(atomMonitorData.pipelineId)
                    var taskId : Long? = null
                    if(null != taskInfoEntity && 0L != taskInfoEntity.taskId){
                        taskDao.updateTaskFailRecord(taskInfoEntity.taskId, null)
                        taskId = taskInfoEntity.taskId
                    }
                    buildIdRelationDao.updateRelationStatus(atomMonitorData.buildId,
                        ComConstants.ScanStatus.SUCCESS.code,
                        null,
                        atomMonitorData.elapseTime,
                        taskId,
                        buildNum)
                }
                return
            }
            val taskInfoEntity = taskRepository.findByPipelineId(atomMonitorData.pipelineId)
            var taskId : Long? = null
            val taskFailRecordEntity = TaskFailRecordEntity()
            with(atomMonitorData) {
                taskFailRecordEntity.taskId = taskInfoEntity?.taskId ?: 0L
                taskFailRecordEntity.projectId = projectId
                taskFailRecordEntity.pipelineId = pipelineId
                taskFailRecordEntity.buildId = buildId
                taskFailRecordEntity.vmSeqId = vmSeqId
                taskFailRecordEntity.createFrom = ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value()
                taskFailRecordEntity.machineIp =
                    if(null != extData && extData!!.containsKey("machineIp")) extData!!["machineIp"] as String else null
                //只重试开源扫描完成的
                taskFailRecordEntity.retryFlag = !projectId.startsWith("CODE_")
                taskFailRecordEntity.uploadTime = endTime
                taskFailRecordEntity.timeCost = elapseTime
                taskFailRecordEntity.atomCode = atomCode
                taskFailRecordEntity.atomVersion = version
                taskFailRecordEntity.errCode = errorCode
                taskFailRecordEntity.errMsg = errorMsg
                taskFailRecordEntity.errType = errorType
                taskFailRecordEntity.toolResult =
                    if(null != extData && extData!!.containsKey("BK_CI_CODEC_TOOL_RUN_RESULT")
                        && null != extData!!["BK_CI_CODEC_TOOL_RUN_RESULT"]) {
                        val toolResultMap = extData!!["BK_CI_CODEC_TOOL_RUN_RESULT"] as Map<*, *>
                        objectMapper.readValue<Map<String, ToolRunResult>>(
                            JsonUtil.toJson(toolResultMap), object : TypeReference<Map<String, ToolRunResult>>(){})
                } else emptyMap()
            }
            taskFailRecordRepository.save(taskFailRecordEntity)
            logger.info("task fail record save successfully!")
            //更新任务维度的最新错误码
            if (null != taskInfoEntity && 0L != taskInfoEntity.taskId) {
                taskDao.updateTaskFailRecord(taskInfoEntity.taskId, taskFailRecordEntity)
                taskId = taskInfoEntity.taskId
            }
            //更新构建维度的错误信息
            buildIdRelationDao.updateRelationStatus(
                atomMonitorData.buildId,
                ComConstants.ScanStatus.FAIL.code,
                taskFailRecordEntity,atomMonitorData.elapseTime,
                taskId,
                buildNum)
        } catch (e : Exception) {
            e.printStackTrace()
            logger.info("handle with task record fail! pipeline id: ${atomMonitorData.pipelineId}, " +
                "build id: ${atomMonitorData.buildId}, message : ${e.message}")
        }


    }

}
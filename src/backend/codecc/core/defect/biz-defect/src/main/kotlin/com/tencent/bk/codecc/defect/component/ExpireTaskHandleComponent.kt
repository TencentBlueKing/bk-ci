package com.tencent.bk.codecc.defect.component

import com.tencent.bk.codecc.defect.api.ServiceReportTaskLogRestResource
import com.tencent.bk.codecc.defect.dao.mongotemplate.TaskLogDao
import com.tencent.bk.codecc.defect.dto.ScanTaskTriggerDTO
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.ComConstants
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ExpireTaskHandleComponent @Autowired constructor(
        private val taskLogDao: TaskLogDao,
        private val client: Client
){

    companion object{
        private val logger = LoggerFactory.getLogger(ExpireTaskHandleComponent::class.java)
    }


    fun updateExpiredTaskStatus(scanTaskTriggerDTO: ScanTaskTriggerDTO)
    {
        try{
            logger.info("start to handle expired task")
            //按构建id查询任务记录清单
            val taskLogList = taskLogDao.findFirstByTaskIdAndBuildIdOrderbyStartTime(scanTaskTriggerDTO.taskId, scanTaskTriggerDTO.buildId)
            taskLogList.forEach {
                //如果超时状态下，还为处理中状态，则进行更新
                if(it.flag == ComConstants.StepFlag.PROCESSING.value())
                {
                    val uploadTaskLogStepVO = UploadTaskLogStepVO()
                    with(uploadTaskLogStepVO){
                        taskId = it.taskId
                        streamName = it.streamName
                        toolName = it.toolName
                        startTime = 0L
                        endTime = System.currentTimeMillis()
                        flag = ComConstants.StepFlag.FAIL.value()
                        msg = "任务超时失败"
                        stepNum = it.currStep
                        pipelineBuildId = it.buildId
                        triggerFrom = it.triggerFrom
                        client.get(ServiceReportTaskLogRestResource::class.java).uploadTaskLog(this)
                    }
                }
            }
        } catch (e: Exception){
            logger.error("set expire task fail! task id: ${scanTaskTriggerDTO.taskId}")
        }

    }
}
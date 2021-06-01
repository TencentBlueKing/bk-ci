package com.tencent.bk.codecc.task.service.impl

import com.tencent.bk.codecc.task.component.EnumValueByBaseDataComponent
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository
import com.tencent.bk.codecc.task.enums.CustomProjSource
import com.tencent.bk.codecc.task.pojo.TriggerPipelineOldReq
import com.tencent.bk.codecc.task.pojo.TriggerPipelineOldRsp
import com.tencent.bk.codecc.task.service.GongfengTriggerOldService
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.api.service.ServiceBuildResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class GongfengTriggerOldServiceImpl @Autowired constructor(
    private val client: Client,
    private val enumValueByBaseDataComponent: EnumValueByBaseDataComponent,
    private val taskRepository: TaskRepository
) : GongfengTriggerOldService {

    @Value("\${codecc.public.url}")
    private val codeccGateWay: String? = null

    companion object {
        private val logger = LoggerFactory.getLogger(GongfengTriggerServiceImpl::class.java)
    }

    override fun triggerCustomProjectPipeline(
        triggerPipelineReq: TriggerPipelineOldReq,
        userId: String
    ): TriggerPipelineOldRsp {
        if (triggerPipelineReq.gitUrl.isNullOrBlank()) {
            logger.error("git url or branch is emtpy!")
            throw CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf("url"))
        }

        // 取出相应枚举，及对应的bean
        val customProjSource = enumValueByBaseDataComponent.getEnumValueByName(
            triggerPipelineReq.triggerSource,
            CustomProjSource::class.java
        )
        if (null == customProjSource) {
            logger.error("no enum value found by name ${triggerPipelineReq.triggerSource}")
            throw CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID)
        }
        val customPipelineService = customProjSource.getiCustomPipelineService()

        val customProjEntity = customPipelineService.getCustomProjEntity(triggerPipelineReq)

        // 如果为空，则需要重新新建项目和流水线
        if (null == customProjEntity) {
            val newCustomProjEntity = customPipelineService.handleWithCheckProjPipeline(triggerPipelineReq, userId)
            return with(newCustomProjEntity) {
                val paramMap = customPipelineService.getParamMap(this)
                paramMap["firstTrigger"] = "true"
                val buildResult = client.getDevopsService(ServiceBuildResource::class.java).manualStartup(
                    userId, projectId, pipelineId,
                    paramMap, ChannelCode.GONGFENGSCAN
                )
                if (buildResult.isNotOk() || null == buildResult.data) {
                    logger.error("trigger pipeline fail! project id: $projectId, task id: $taskId, url: ${triggerPipelineReq.gitUrl}")
                    throw CodeCCException(CommonMessageCode.SYSTEM_ERROR)
                }
                val taskInfoEntity = taskRepository.findByTaskId(taskId)
                val toolList =
                    if (taskInfoEntity.toolConfigInfoList.isNotEmpty()) taskInfoEntity.toolConfigInfoList.filter { it.followStatus != ComConstants.FOLLOW_STATUS.WITHDRAW.value() }
                        .map { it.toolName }
                    else
                        emptyList()
                TriggerPipelineOldRsp(
                    displayAddress = "$codeccGateWay/codecc/$projectId/task/$taskId/detail",
                    buildId = buildResult.data!!.id,
                    taskId = taskId,
                    toolList = toolList
                )
            }
        } else {
            return with(customProjEntity) {
                customPipelineService.updateCustomizedCheckProjPipeline(triggerPipelineReq, taskId, userId, projectId, pipelineId)
                val paramMap = customPipelineService.getParamMap(this)
                paramMap["firstTrigger"] = "false"
                val buildResult = client.getDevopsService(ServiceBuildResource::class.java).manualStartup(
                    userId, projectId, pipelineId,
                    paramMap, ChannelCode.GONGFENGSCAN
                )
                if (buildResult.isNotOk() || null == buildResult.data) {
                    logger.error("trigger pipeline fail! project id: $projectId, task id: $taskId, url: ${triggerPipelineReq.gitUrl}")
                    throw CodeCCException(CommonMessageCode.SYSTEM_ERROR)
                }
                val taskInfoEntity = taskRepository.findByTaskId(taskId)
                val toolList =
                    if (taskInfoEntity.toolConfigInfoList.isNotEmpty()) taskInfoEntity.toolConfigInfoList.filter { it.followStatus != ComConstants.FOLLOW_STATUS.WITHDRAW.value() }
                        .map { it.toolName }
                    else
                        emptyList()
                TriggerPipelineOldRsp(
                    displayAddress = "$codeccGateWay/codecc/$projectId/task/$taskId/detail",
                    buildId = buildResult.data!!.id,
                    taskId = taskId,
                    toolList = toolList
                )
            }
        }
    }
}
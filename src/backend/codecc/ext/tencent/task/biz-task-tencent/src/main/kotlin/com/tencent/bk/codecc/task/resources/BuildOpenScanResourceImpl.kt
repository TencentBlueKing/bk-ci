package com.tencent.bk.codecc.task.resources

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.bk.codecc.task.api.BuildOpenScanResource
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository
import com.tencent.bk.codecc.task.service.GongfengPublicProjService
import com.tencent.bk.codecc.task.service.OpenSourcePipelineService
import com.tencent.bk.codecc.task.service.OpenSourceTaskService
import com.tencent.bk.codecc.task.service.impl.OpenSourceTaskRegisterServiceImpl
import com.tencent.bk.codecc.task.vo.TaskDetailVO
import com.tencent.bk.codecc.task.vo.TaskFailRecordVO
import com.tencent.bk.codecc.task.vo.TaskIdVO
import com.tencent.devops.common.api.pojo.CodeCCResult
import com.tencent.devops.common.constant.ComConstants
import com.tencent.bk.codecc.task.pojo.GongfengStatProjVO
import com.tencent.devops.common.web.RestResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildOpenScanResourceImpl @Autowired constructor(
    private val openSourceTaskRegisterService: OpenSourceTaskRegisterServiceImpl,
    private val gongfengPublicProjService: GongfengPublicProjService,
    private val taskRepository: TaskRepository,
    private val pipelineService : OpenSourcePipelineService,
    private val openSourceTaskService: OpenSourceTaskService,
    private val objectMapper: ObjectMapper
) : BuildOpenScanResource {

    companion object {
        private val logger = LoggerFactory.getLogger(BuildOpenScanResourceImpl::class.java)
    }

    override fun registerTask(taskDetailVO: TaskDetailVO, userName: String): CodeCCResult<TaskIdVO> {
        return CodeCCResult(openSourceTaskRegisterService.registerTask(taskDetailVO, userName))
    }

    override fun updateTask(taskDetailVO: TaskDetailVO, userName: String): CodeCCResult<Boolean> {
        openSourceTaskRegisterService.updateTask(taskDetailVO, userName)
        return CodeCCResult(true)
    }

    override fun queryGongfengStatProj(projectId: Int): CodeCCResult<GongfengStatProjVO> {
        val gongfengStatProjEntity = gongfengPublicProjService.findStatByProjectId(projectId)
        return CodeCCResult(
            objectMapper.readValue(
                objectMapper.writeValueAsString(gongfengStatProjEntity),
                GongfengStatProjVO::class.java
            )
        )
    }

    override fun switchCheckerSetType(
        pipelineId: String,
        userName: String,
        checkerSetType: ComConstants.OpenSourceCheckerSetType
    ): CodeCCResult<Boolean> {
        openSourceTaskRegisterService.switchCheckerSetType(pipelineId, userName, checkerSetType)
        return CodeCCResult(true)
    }

    override fun getFilterConfig() : CodeCCResult<Map<String, String>> {
        return CodeCCResult(openSourceTaskRegisterService.getFilterConfig())
    }

    override fun updatePipelineModel(
        pipelineId : String,
        codeccDispatchRoute : ComConstants.CodeCCDispatchRoute
    ) : CodeCCResult<Boolean> {
        logger.info("start to update pipeline model, pipeline id: $pipelineId")
        val taskInfoEntity = taskRepository.findByPipelineId(pipelineId)
        if(null == taskInfoEntity || null == taskInfoEntity.gongfengProjectId){
            logger.info("no task info found with pipeline id: $pipelineId")
            return CodeCCResult(false)
        }
        val gongfengPublicProjEntity = gongfengPublicProjService.findProjectById(taskInfoEntity.gongfengProjectId)
        val gongfengStatProjEntity = gongfengPublicProjService.findStatByProjectId(taskInfoEntity.gongfengProjectId)
        if(null == gongfengPublicProjEntity || null == gongfengStatProjEntity){
            logger.info("no gongfeng project info found with pipeline id $pipelineId")
            return CodeCCResult(false)
        }
        pipelineService.updateExistsCommonPipeline(
            gongfengPublicProjEntity = gongfengPublicProjEntity,
            projectId = taskInfoEntity.projectId,
            taskId = taskInfoEntity.taskId,
            pipelineId = taskInfoEntity.pipelineId,
            owner = gongfengStatProjEntity.owners,
            dispatchRoute = codeccDispatchRoute
        )
        return CodeCCResult(true)
    }


    override fun saveTaskFailRecord(taskFailRecordVO: TaskFailRecordVO) : CodeCCResult<Boolean>{
        return CodeCCResult(true)
    }

    override fun updateCommitId(buildId : String, commitId : String) : CodeCCResult<Boolean>{
        openSourceTaskService.updateBuildCommitId(buildId, commitId)
        return CodeCCResult(true)
    }
}
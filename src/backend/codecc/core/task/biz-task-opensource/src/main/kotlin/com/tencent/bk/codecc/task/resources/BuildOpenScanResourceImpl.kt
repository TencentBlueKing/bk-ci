package com.tencent.bk.codecc.task.resources

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.bk.codecc.task.api.BuildOpenScanResource
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository
import com.tencent.bk.codecc.task.service.GongfengPublicProjService
import com.tencent.bk.codecc.task.service.OpenSourceTaskService
import com.tencent.bk.codecc.task.service.PipelineService
import com.tencent.bk.codecc.task.service.impl.OpenSourceTaskRegisterServiceImpl
import com.tencent.bk.codecc.task.vo.TaskDetailVO
import com.tencent.bk.codecc.task.vo.TaskFailRecordVO
import com.tencent.bk.codecc.task.vo.TaskIdVO
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.pojo.GongfengStatProjVO
import com.tencent.devops.common.web.RestResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildOpenScanResourceImpl @Autowired constructor(
    private val openSourceTaskRegisterService: OpenSourceTaskRegisterServiceImpl,
    private val gongfengPublicProjService: GongfengPublicProjService,
    private val taskRepository: TaskRepository,
    private val pipelineService: PipelineService,
    private val openSourceTaskService: OpenSourceTaskService,
    private val objectMapper: ObjectMapper
) : BuildOpenScanResource {

    companion object {
        private val logger = LoggerFactory.getLogger(BuildOpenScanResourceImpl::class.java)
    }

    override fun registerTask(taskDetailVO: TaskDetailVO, userName: String): Result<TaskIdVO> {
        return Result(openSourceTaskRegisterService.registerTask(taskDetailVO, userName))
    }

    override fun updateTask(taskDetailVO: TaskDetailVO, userName: String): Result<Boolean> {
        openSourceTaskRegisterService.updateTask(taskDetailVO, userName)
        return Result(true)
    }

    override fun queryGongfengStatProj(projectId: Int): Result<GongfengStatProjVO> {
        val gongfengStatProjEntity = gongfengPublicProjService.findStatByProjectId(projectId)
        return Result(
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
    ): Result<Boolean> {
        openSourceTaskRegisterService.switchCheckerSetType(pipelineId, userName, checkerSetType)
        return Result(true)
    }

    override fun setCustomizedCheckerSet(
        pipelineId: String,
        userName: String
    ): Result<Boolean> {
        openSourceTaskRegisterService.setCustomizedCheckerSet(pipelineId, userName)
        return Result(true)
    }

    override fun getFilterConfig(): Result<Map<String, String>> {
        return Result(openSourceTaskRegisterService.getFilterConfig())
    }

    override fun updatePipelineModel(
        pipelineId: String,
        codeccDispatchRoute: ComConstants.CodeCCDispatchRoute
    ): Result<Boolean> {
        logger.info("start to update pipeline model, pipeline id: $pipelineId")
        val taskInfoEntity = taskRepository.findByPipelineId(pipelineId)
        if (null == taskInfoEntity || null == taskInfoEntity.gongfengProjectId) {
            logger.info("no task info found with pipeline id: $pipelineId")
            return Result(false)
        }
        val gongfengPublicProjEntity = gongfengPublicProjService.findProjectById(taskInfoEntity.gongfengProjectId)
        val gongfengStatProjEntity = gongfengPublicProjService.findStatByProjectId(taskInfoEntity.gongfengProjectId)
        if (null == gongfengPublicProjEntity || null == gongfengStatProjEntity) {
            logger.info("no gongfeng project info found with pipeline id $pipelineId")
            return Result(false)
        }
        pipelineService.updateExistsCommonPipeline(
            gongfengPublicProjEntity = gongfengPublicProjEntity,
            projectId = taskInfoEntity.projectId,
            taskId = taskInfoEntity.taskId,
            pipelineId = taskInfoEntity.pipelineId,
            owner = gongfengStatProjEntity.owners,
            dispatchRoute = codeccDispatchRoute
        )
        return Result(true)
    }

    override fun saveTaskFailRecord(taskFailRecordVO: TaskFailRecordVO): Result<Boolean> {
        return Result(true)
    }

    override fun updateCommitId(buildId: String, commitId: String): Result<Boolean> {
        openSourceTaskService.updateBuildCommitId(buildId, commitId)
        return Result(true)
    }
}

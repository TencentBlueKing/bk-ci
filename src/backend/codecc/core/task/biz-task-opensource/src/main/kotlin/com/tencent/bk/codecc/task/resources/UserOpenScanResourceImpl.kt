package com.tencent.bk.codecc.task.resources

import com.tencent.bk.codecc.task.api.UserOpenScanResource
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository
import com.tencent.bk.codecc.task.listener.GongfengCreateTaskListener
import com.tencent.bk.codecc.task.model.TaskInfoEntity
import com.tencent.bk.codecc.task.pojo.TriggerPipelineModel
import com.tencent.bk.codecc.task.service.GongfengTriggerService
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.web.RestResource
import org.apache.commons.lang.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserOpenScanResourceImpl @Autowired constructor(
    private val gongfengCreateTaskListener: GongfengCreateTaskListener,
    private val taskRepository: TaskRepository,
    private val gongfengTriggerService: GongfengTriggerService
) : UserOpenScanResource {

    companion object {
        private val logger = LoggerFactory.getLogger(UserOpenScanResourceImpl::class.java)
    }

    override fun triggerOpensourceTask(pipelineId: String, userName: String): Result<Boolean> {
        logger.info("start to trigger opensource task, pipeline id: $pipelineId")
        val taskInfoEntity: TaskInfoEntity? = taskRepository.findByPipelineId(pipelineId)
        if (null == taskInfoEntity || taskInfoEntity.taskId <= 0L) {
            logger.info("no task info found!")
            return Result(false)
        }
        val triggerPipelineModel = TriggerPipelineModel(
            projectId = taskInfoEntity.projectId,
            pipelineId = taskInfoEntity.pipelineId,
            taskId = taskInfoEntity.taskId,
            gongfengId = taskInfoEntity.gongfengProjectId,
            owner = userName
        )
        gongfengCreateTaskListener.executeTriggerPipeline(triggerPipelineModel)
        return Result(true)
    }

    override fun triggerOpensourceTaskByRepo(repoId: String, commitId: String?): Result<String?> {
        if (StringUtils.isBlank(repoId)) {
            throw CodeCCException(CommonMessageCode.PARAMETER_IS_NULL)
        }

        return try {
            Result(gongfengTriggerService.triggerGongfengTaskByRepoId(
                    repoId,
                    commitId
            ))
        } catch (e: CodeCCException) {
            Result(2300020, "2300020", "No task found")
        }
    }
}
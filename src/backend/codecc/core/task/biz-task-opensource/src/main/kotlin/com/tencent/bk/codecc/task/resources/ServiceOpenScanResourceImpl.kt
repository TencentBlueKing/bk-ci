package com.tencent.bk.codecc.task.resources

import com.tencent.bk.codecc.task.api.ServiceOpenScanResource
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository
import com.tencent.bk.codecc.task.dao.mongorepository.ToolRepository
import com.tencent.bk.codecc.task.listener.GongfengCreateTaskListener
import com.tencent.bk.codecc.task.pojo.TriggerPipelineModel
import com.tencent.bk.codecc.task.service.GongfengTriggerService
import com.tencent.bk.codecc.task.service.impl.OpenSourceTaskRegisterServiceImpl
import com.tencent.bk.codecc.task.vo.TaskDetailVO
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.web.RestResource
import org.apache.commons.lang.StringUtils
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceOpenScanResourceImpl @Autowired constructor(
    private val openSourceTaskRegisterService: OpenSourceTaskRegisterServiceImpl,
    private val gongfengCreateTaskListener: GongfengCreateTaskListener,
    private val taskRepository: TaskRepository,
    private val toolRepository: ToolRepository,
    private val gongfengTriggerService: GongfengTriggerService
) : ServiceOpenScanResource {
    override fun manualTriggerOpenScan(taskIdList: List<Long>): Result<Boolean> {
        taskIdList.forEach {
            val taskInfoEntity = taskRepository.findByTaskId(it)
            if (null != taskInfoEntity) {
                // 设置为全量扫描
                taskInfoEntity.scanType = 0
                taskRepository.save(taskInfoEntity)
                val triggerPipelineModel = TriggerPipelineModel(
                        projectId = taskInfoEntity.projectId,
                        pipelineId = taskInfoEntity.pipelineId,
                        taskId = taskInfoEntity.taskId,
                        gongfengId = taskInfoEntity.gongfengProjectId,
                        owner = taskInfoEntity.taskOwner[0]
                )
            }
            Thread.sleep(500)
        }
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
            Result(2300020, "2300020", "No task found", null)
        }
    }

    override fun updateTask(taskDetailVO: TaskDetailVO, userName: String): Result<Boolean> {
        openSourceTaskRegisterService.updateTask(taskDetailVO, userName)
        return Result(true)
    }
}

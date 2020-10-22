package com.tencent.bk.codecc.task.resources

import com.tencent.bk.codecc.task.api.ServiceOpenScanResource
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository
import com.tencent.bk.codecc.task.dao.mongorepository.ToolRepository
import com.tencent.bk.codecc.task.listener.GongfengCreateTaskListener
import com.tencent.bk.codecc.task.pojo.TriggerPipelineModel
import com.tencent.bk.codecc.task.service.impl.OpenSourceTaskRegisterServiceImpl
import com.tencent.bk.codecc.task.vo.TaskDetailVO
import com.tencent.devops.common.api.pojo.CodeCCResult
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceOpenScanResourceImpl @Autowired constructor(
    private val openSourceTaskRegisterService: OpenSourceTaskRegisterServiceImpl,
    private val gongfengCreateTaskListener: GongfengCreateTaskListener,
    private val taskRepository: TaskRepository,
    private val toolRepository: ToolRepository
) : ServiceOpenScanResource {
    override fun manualTriggerOpenScan(taskIdList: List<Long>): CodeCCResult<Boolean> {
        taskIdList.forEach {
            val taskInfoEntity = taskRepository.findByTaskId(it)
            if (null != taskInfoEntity) {
                //设置为全量扫描
                taskInfoEntity.scanType = 0
                taskRepository.save(taskInfoEntity)
                val triggerPipelineModel = TriggerPipelineModel(
                    projectId = taskInfoEntity.projectId,
                    pipelineId = taskInfoEntity.pipelineId,
                    taskId = taskInfoEntity.taskId,
                    gongfengId = taskInfoEntity.gongfengProjectId,
                    owner = taskInfoEntity.taskOwner[0]
                )
                gongfengCreateTaskListener.executeTriggerPipelineForManual(triggerPipelineModel)
            }
            Thread.sleep(500)
        }
        return CodeCCResult(true)
    }

    override fun updateTask(taskDetailVO: TaskDetailVO, userName: String): CodeCCResult<Boolean> {
        openSourceTaskRegisterService.updateTask(taskDetailVO, userName)
        return CodeCCResult(true)
    }
}
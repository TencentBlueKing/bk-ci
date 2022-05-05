package com.tencent.devops.dispatch.bcs.actions

import com.tencent.devops.dispatch.bcs.client.BcsTaskClient
import com.tencent.devops.dispatch.bcs.pojo.DispatchBuildStatusEnum
import com.tencent.devops.dispatch.bcs.pojo.DispatchBuildStatusResp
import com.tencent.devops.dispatch.bcs.pojo.bcs.BcsTaskStatusEnum
import com.tencent.devops.dispatch.bcs.pojo.bcs.getCodeMessage
import com.tencent.devops.dispatch.bcs.pojo.bcs.isFailed
import com.tencent.devops.dispatch.bcs.pojo.bcs.isRunning
import com.tencent.devops.dispatch.bcs.pojo.bcs.isSuccess
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TaskAction @Autowired constructor(
    private val bcsTaskClient: BcsTaskClient
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TaskAction::class.java)
    }

    fun getTaskStatus(userId: String, taskId: String): DispatchBuildStatusResp {
        val taskResponse = bcsTaskClient.getTasksStatus(userId, taskId)
        val status = BcsTaskStatusEnum.realNameOf(taskResponse.data?.status)
        if (taskResponse.isNotOk() || taskResponse.data == null) {
            // 创建失败
            val msg = "${taskResponse.message ?: taskResponse.getCodeMessage()}"
            logger.error("Execute task: $taskId failed, actionCode is ${taskResponse.code}, msg: $msg")
            return DispatchBuildStatusResp(DispatchBuildStatusEnum.failed.name, msg)
        }
        // 请求成功但是任务失败
        if (status != null && status.isFailed()) {
            return DispatchBuildStatusResp(DispatchBuildStatusEnum.failed.name, taskResponse.data!!.message)
        }
        return when {
            status!!.isRunning() -> DispatchBuildStatusResp(DispatchBuildStatusEnum.running.name)
            status.isSuccess() -> {
                DispatchBuildStatusResp(DispatchBuildStatusEnum.succeeded.name)
            }
            else -> DispatchBuildStatusResp(DispatchBuildStatusEnum.failed.name, status.message)
        }
    }
}

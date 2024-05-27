package com.tencent.devops.process.api.open

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineTaskService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpenPipelineTaskResourceImpl @Autowired constructor(
    private val pipelineTaskService: PipelineTaskService,
    private val pipelineRuntimeService: PipelineRuntimeService
) : OpenPipelineTaskResource {

    override fun getBuildStatus(
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String?
    ): Result<BuildStatus?> {

        // 校验参数
        val buildInfo = pipelineRuntimeService.getBuildInfo(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId
        )

        return  if (buildInfo == null) {
             Result(status = -1, message = "Build[$buildId] is not found!")
        } else if (!taskId.isNullOrBlank()) { // 查指定task的状态
            val tStatus = pipelineTaskService.getTaskStatus(projectId = projectId, buildId = buildId, taskId = taskId)
            if (tStatus == null) {
                Result(status = -1, message = "Task[$taskId] is not found!")
            } else {
                Result(tStatus)
            }
        } else {
            Result(buildInfo.status)
        }
    }
}

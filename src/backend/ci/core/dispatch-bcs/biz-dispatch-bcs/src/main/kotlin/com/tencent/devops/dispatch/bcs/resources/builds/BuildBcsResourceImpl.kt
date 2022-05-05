package com.tencent.devops.dispatch.bcs.resources.builds

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.bcs.actions.JobAction
import com.tencent.devops.dispatch.bcs.actions.TaskAction
import com.tencent.devops.dispatch.bcs.api.builds.BuildBcsResource
import com.tencent.devops.dispatch.bcs.pojo.DispatchBuildStatusResp
import com.tencent.devops.dispatch.bcs.pojo.DispatchJobLogResp
import com.tencent.devops.dispatch.bcs.pojo.DispatchJobReq
import com.tencent.devops.dispatch.bcs.pojo.DispatchTaskResp
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildBcsResourceImpl @Autowired constructor(
    private val jobAction: JobAction,
    private val taskAction: TaskAction
) : BuildBcsResource {
    override fun createJob(
        userId: String,
        projectId: String,
        buildId: String,
        jobReq: DispatchJobReq
    ): Result<DispatchTaskResp> {
        return Result(jobAction.createJob(userId, projectId, buildId, jobReq))
    }

    override fun getJobStatus(userId: String, jobName: String): Result<DispatchBuildStatusResp> {
        return Result(jobAction.getJobStatus(userId, jobName))
    }

    override fun getJobLogs(userId: String, jobName: String, sinceTime: Int?): Result<DispatchJobLogResp> {
        return Result(jobAction.getJobLogs(userId, jobName, sinceTime))
    }

    override fun getTaskStatus(userId: String, taskId: String): Result<DispatchBuildStatusResp> {
        return Result(taskAction.getTaskStatus(userId, taskId))
    }
}

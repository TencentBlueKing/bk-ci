package com.tencent.devops.dispatch.kubernetes.resource.builds

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.kubernetes.api.builds.SdkJobResource
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesJobReq
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesJobResp
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesJobStatusResp
import com.tencent.devops.dispatch.kubernetes.service.JobService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class SdkJobResourceImpl @Autowired constructor(
    private val jobService: JobService
) : SdkJobResource {
    override fun createJob(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        jobReq: KubernetesJobReq
    ): Result<KubernetesJobResp> {
        return Result(jobService.createJob(buildId, jobReq))
    }

    override fun getJobStatus(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        jobName: String
    ): Result<KubernetesJobStatusResp> {
        return Result(jobService.getJobStatus(jobName))
    }

    override fun getJobLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        jobName: String,
        sinceTime: Int?
    ): Result<String?> {
        return Result(jobService.getJobLogs(jobName, sinceTime))
    }
}

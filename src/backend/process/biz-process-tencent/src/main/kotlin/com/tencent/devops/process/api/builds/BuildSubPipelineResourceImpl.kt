package com.tencent.devops.process.api.builds

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.engine.service.PipelineBuildService
import com.tencent.devops.process.pojo.pipeline.ProjectBuildId
import com.tencent.devops.process.pojo.pipeline.SubPipelineStatus
import com.tencent.devops.process.service.SubPipelineStartUpService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildSubPipelineResourceImpl @Autowired constructor(
    private val subPipeService: SubPipelineStartUpService,
    private val buildService: PipelineBuildService
) : BuildSubPipelineResource {
    override fun callPipelineStartup(
        projectId: String,
        parentPipelineId: String,
        buildId: String,
        callPipelineId: String,
        atomCode: String,
        taskId: String,
        runMode: String,
        values: Map<String, String>
    ): Result<ProjectBuildId> {
        return subPipeService.callPipelineStartup(projectId, parentPipelineId, buildId, callPipelineId, atomCode, taskId, runMode, values)
    }

    override fun getSubPipelineStatus(projectId: String, pipelineId: String, buildId: String): Result<SubPipelineStatus> {
        var status = Result(buildService.getBuildDetail(projectId, pipelineId, buildId, ChannelCode.BS, ChannelCode.isNeedAuth(ChannelCode.BS)))
                .data?.status
        if (status == null)
            status = "ERROR"
        val result = SubPipelineStatus(status)
        return Result(result)
    }
}
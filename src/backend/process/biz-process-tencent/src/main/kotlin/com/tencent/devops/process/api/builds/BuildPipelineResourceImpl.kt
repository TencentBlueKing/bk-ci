package com.tencent.devops.process.api.builds

import com.tencent.devops.common.api.pojo.BuildHistoryPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.engine.service.PipelineBuildService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineService
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.Pipeline
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildPipelineResourceImpl @Autowired constructor(
    private val pipelineService: PipelineService,
    private val buildService: PipelineBuildService,
    private val pipelineRuntimeService: PipelineRuntimeService
) : BuildPipelineResource {
    override fun getPipelineNameByIds(projectId: String, pipelineIds: Set<String>): Result<Map<String, String>> {
        logger.info("the method of being done is: getPipelineNameByIds")
        return Result(pipelineService.getPipelineNameByIds(projectId, pipelineIds, true))
    }

    override fun getHistoryBuild(
        currentBuildId: String,
        projectId: String,
        pipelineId: String,
        page: Int?,
        pageSize: Int?
    ): Result<BuildHistoryPage<BuildHistory>> {
        logger.info("the method of being done is: getHistoryBuild")
        val userId = pipelineRuntimeService.getVariable(currentBuildId, "pipeline.start.user.id")
        val result = buildService.getHistoryBuild(userId, projectId, pipelineId, page, pageSize, ChannelCode.BS, true)
        return Result(result)
    }

    override fun list(currentBuildId: String, projectId: String, pipelineIdListString: String?): Result<List<Pipeline>> {
        logger.info("the method of being done is: list")
        val userId = pipelineRuntimeService.getVariable(currentBuildId, "pipeline.start.user.id")!!
        val pipelineIdList = pipelineIdListString?.split(",")
        val result = pipelineService.listPipelineInfo(userId, projectId, pipelineIdList)
        return Result(result)
    }

    private val logger = LoggerFactory.getLogger(BuildPipelineResourceImpl::class.java)
}
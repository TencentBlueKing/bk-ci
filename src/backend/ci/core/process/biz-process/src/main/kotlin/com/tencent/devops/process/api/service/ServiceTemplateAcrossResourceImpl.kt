package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import com.tencent.devops.process.service.PipelineBuildTemplateAcrossInfoService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceTemplateAcrossResourceImpl @Autowired constructor(
    private val templateAcrossInfoService: PipelineBuildTemplateAcrossInfoService
) : ServiceTemplateAcrossResource {
    override fun batchCreate(
        userId: String,
        projectId: String,
        pipelineId: String,
        templateAcrossInfos: List<BuildTemplateAcrossInfo>
    ) {
        templateAcrossInfoService.batchCreateAcrossInfo(projectId, pipelineId, null, userId, templateAcrossInfos)
    }

    override fun getBuildAcrossTemplateInfo(
        projectId: String,
        templateId: String
    ): Result<List<BuildTemplateAcrossInfo>> {
        return Result(
            templateAcrossInfoService.getAcrossInfo(projectId, null, templateId)
        )
    }

    override fun update(projectId: String, pipelineId: String, templateId: String, buildId: String): Result<Boolean> {
        return Result(
            templateAcrossInfoService.updateAcrossInfoBuildId(projectId, pipelineId, templateId, buildId)
        )
    }

    override fun delete(projectId: String, pipelineId: String, templateId: String?, buildId: String?): Result<Boolean> {
        return Result(
            templateAcrossInfoService.deleteAcrossInfo(projectId, pipelineId, buildId, templateId)
        )
    }
}

package com.tencent.devops.quality.resources.v2

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.quality.api.v2.BuildQualityMetadataResource
import com.tencent.devops.quality.service.v2.QualityHisMetadataService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildQualityMetadataResourceImpl @Autowired constructor(
    private val qualityHisMetadataService: QualityHisMetadataService
) : BuildQualityMetadataResource {
    override fun saveHisMetadata(projectId: String, pipelineId: String, buildId: String, elementType: String, data: Map<String, String>): Result<Boolean> {
        return Result(qualityHisMetadataService.saveHisMetadata(projectId, pipelineId, buildId, elementType, data))
    }
}
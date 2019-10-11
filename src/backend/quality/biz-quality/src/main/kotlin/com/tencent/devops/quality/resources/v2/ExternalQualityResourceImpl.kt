package com.tencent.devops.quality.resources.v2

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.quality.api.v2.ExternalQualityResource
import com.tencent.devops.quality.api.v2.pojo.request.MetadataCallback
import com.tencent.devops.quality.service.v2.QualityHisMetadataService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ExternalQualityResourceImpl @Autowired constructor(
    val hisMetadataService: QualityHisMetadataService
) : ExternalQualityResource {
    override fun metadataCallback(projectId: String, pipelineId: String, buildId: String, callback: MetadataCallback): Result<String> {
        return Result(hisMetadataService.saveHisMetadata(projectId, pipelineId, buildId, callback))
    }
}
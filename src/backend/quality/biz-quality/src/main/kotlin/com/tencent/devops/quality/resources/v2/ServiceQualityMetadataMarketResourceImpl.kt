package com.tencent.devops.quality.resources.v2

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.quality.api.v2.ServiceQualityMetadataMarketResource
import com.tencent.devops.quality.api.v2.pojo.op.QualityMetaData
import com.tencent.devops.quality.service.v2.QualityMetadataService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceQualityMetadataMarketResourceImpl @Autowired constructor(
    private val qualityMetadataService: QualityMetadataService
) : ServiceQualityMetadataMarketResource {
    override fun setTestMetadata(userId: String, atomCode: String, metadataList: List<QualityMetaData>): Result<Map<String, Long>> {
        return Result(qualityMetadataService.serviceSetTestMetadata(userId, atomCode, metadataList))
    }

    override fun refreshMetadata(elementType: String): Result<Map<String, String>> {
        return Result(qualityMetadataService.serviceRefreshMetadata(elementType))
    }

    override fun deleteTestMetadata(elementType: String): Result<Int> {
        return Result(qualityMetadataService.serviceDeleteTestMetadata(elementType))
    }
}
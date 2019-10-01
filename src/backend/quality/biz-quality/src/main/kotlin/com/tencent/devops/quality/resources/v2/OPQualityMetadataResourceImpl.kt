package com.tencent.devops.quality.resources.v2

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.quality.api.v2.OPQualityMetadataResource
import com.tencent.devops.quality.api.v2.pojo.op.ElementNameData
import com.tencent.devops.quality.api.v2.pojo.op.QualityMetaData
import com.tencent.devops.quality.service.v2.QualityMetadataService
import org.springframework.beans.factory.annotation.Autowired

/**
 * @author eltons,  Date on 2019-03-01.
 */
@RestResource
class OPQualityMetadataResourceImpl @Autowired constructor(
    private val qualityMetadataService: QualityMetadataService
) : OPQualityMetadataResource {

    override fun list(
        userId: String,
        elementName: String?,
        elementDetail: String?,
        searchString: String?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<QualityMetaData>> {
        checkParams(userId)
        val result = qualityMetadataService.opList(elementName, elementDetail, searchString, page, pageSize)
        return Result(result)
    }

    override fun getElementNames(userId: String): Result<List<ElementNameData>> {
        checkParams(userId)
        return Result(qualityMetadataService.opGetElementNames())
    }

    override fun getElementDetails(userId: String): Result<List<String>> {
        checkParams(userId)
        val result = qualityMetadataService.opGetElementDetails()
        return Result(result)
    }

    private fun checkParams(userId: String) {
        if (userId.isBlank()) throw ParamBlankException("Invalid userId")
    }
}
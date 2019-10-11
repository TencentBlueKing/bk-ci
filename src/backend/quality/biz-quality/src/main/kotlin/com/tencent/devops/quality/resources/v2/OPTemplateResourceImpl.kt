package com.tencent.devops.quality.resources.v2

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.quality.api.v2.OPTemplateResource
import com.tencent.devops.quality.api.v2.pojo.op.TemplateData
import com.tencent.devops.quality.api.v2.pojo.op.TemplateUpdateData
import com.tencent.devops.quality.service.v2.QualityTemplateService
import org.springframework.beans.factory.annotation.Autowired

/**
 * @author eltons,  Date on 2019-03-01.
 */
@RestResource
class OPTemplateResourceImpl @Autowired constructor(
    private val qualityTemplateService: QualityTemplateService
) : OPTemplateResource {
    override fun list(userId: String, page: Int?, pageSize: Int?): Result<Page<TemplateData>> {
        checkParams(userId)
        val result = qualityTemplateService.opList(userId, page, pageSize)
        return Result(result)
    }

    override fun add(userId: String, templateUpdateData: TemplateUpdateData): Result<Boolean> {
        checkParams(userId)
        val result = qualityTemplateService.opCreate(userId, templateUpdateData)
        return Result(result)
    }

    override fun delete(userId: String, id: Long): Result<Boolean> {
        checkParams(userId, id)
        val result = qualityTemplateService.opDelete(userId, id)
        return Result(result)
    }

    override fun update(userId: String, id: Long, templateUpdateData: TemplateUpdateData): Result<Boolean> {
        checkParams(userId, id)
        val result = qualityTemplateService.opUpdate(userId, id, templateUpdateData)
        return Result(result)
    }

    private fun checkParams(userId: String, id: Long = 1) {
        if (userId.isBlank()) throw ParamBlankException("Invalid userId")
        if (id <= 0L) throw ParamBlankException("Invalid id")
    }
}
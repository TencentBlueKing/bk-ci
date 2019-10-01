package com.tencent.devops.quality.resources.v2

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.quality.api.v2.UserQualityIndicatorResource
import com.tencent.devops.quality.api.v2.pojo.QualityIndicator
import com.tencent.devops.quality.api.v2.pojo.RuleIndicatorSet
import com.tencent.devops.quality.api.v2.pojo.request.IndicatorCreate
import com.tencent.devops.quality.api.v2.pojo.response.IndicatorListResponse
import com.tencent.devops.quality.api.v2.pojo.response.IndicatorStageGroup
import com.tencent.devops.quality.service.v2.QualityIndicatorService
import com.tencent.devops.quality.service.v2.QualityTemplateService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserQualityIndicatorResourceImpl @Autowired constructor(
    val indicatorService: QualityIndicatorService,
    val templateService: QualityTemplateService
) : UserQualityIndicatorResource {
    override fun update(userId: String, projectId: String, indicatorId: String, indicatorCreate: IndicatorCreate): Result<Boolean> {
        return Result(indicatorService.userUpdate(userId, projectId, indicatorId, indicatorCreate))
    }

    override fun create(userId: String, projectId: String, indicatorCreate: IndicatorCreate): Result<Boolean> {
        return Result(indicatorService.userCreate(userId, projectId, indicatorCreate))
    }

    override fun queryIndicatorList(projectId: String, keyword: String?): Result<IndicatorListResponse> {
        return Result(indicatorService.userQueryIndicatorList(projectId, keyword))
    }

    override fun get(projectId: String, indicatorId: String): Result<QualityIndicator> {
        return Result(indicatorService.userGet(projectId, indicatorId))
    }

    override fun listIndicators(projectId: String): Result<List<IndicatorStageGroup>> {
        return Result(indicatorService.listByLevel(projectId))
    }

    override fun listIndicatorSet(): Result<List<RuleIndicatorSet>> {
        return Result(templateService.userListIndicatorSet())
    }

    override fun delete(userId: String, projectId: String, indicatorId: String): Result<Boolean> {
        return Result(indicatorService.userDelete(projectId, HashUtil.decodeIdToLong(indicatorId)))
    }
}
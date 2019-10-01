package com.tencent.devops.quality.resources.v2

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.quality.api.v2.UserQualityCountResource
import com.tencent.devops.quality.api.v2.pojo.QualityRuleIntercept
import com.tencent.devops.quality.api.v2.pojo.response.CountOverviewResponse
import com.tencent.devops.quality.pojo.CountDailyIntercept
import com.tencent.devops.quality.pojo.CountPipelineIntercept
import com.tencent.devops.quality.service.v2.QualityCountService
import com.tencent.devops.quality.service.v2.QualityHistoryService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserQualityCountResourceImpl @Autowired constructor(
    private val countService: QualityCountService,
    private val historyService: QualityHistoryService
) : UserQualityCountResource {
    override fun getOverview(userId: String, projectId: String): Result<CountOverviewResponse> {
        return Result(countService.getOverview(userId, projectId))
    }

    override fun getPipelineIntercept(userId: String, projectId: String): Result<List<CountPipelineIntercept>> {
        return Result(countService.getPipelineIntercept(userId, projectId))
    }

    override fun getDailyIntercept(userId: String, projectId: String): Result<List<CountDailyIntercept>> {
        return Result(countService.getDailyIntercept(userId, projectId))
    }

    override fun getRuleIntercept(userId: String, projectId: String): Result<Page<QualityRuleIntercept>> {
        checkParams(userId, projectId)
        val page = 1
        val pageSize = 10
        val limit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
        val result = historyService.userGetRuleIntercept(userId, projectId, limit.offset, limit.limit)
        return Result(Page(page, pageSize, result.first, result.second))
    }

    private fun checkParams(userId: String, projectId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }
}
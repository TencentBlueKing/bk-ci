package com.tencent.devops.quality.resources.v2

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.quality.api.v2.UserQualityInterceptResource
import com.tencent.devops.quality.pojo.RuleInterceptHistory
import com.tencent.devops.quality.pojo.enum.RuleInterceptResult
import com.tencent.devops.quality.service.v2.QualityHistoryService
import com.tencent.devops.quality.service.v2.QualityRuleCheckService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserQualityInterceptResourceImpl @Autowired constructor(
    private val historyService: QualityHistoryService,
    private val ruleCheckService: QualityRuleCheckService
) : UserQualityInterceptResource {
    override fun list(userId: String, projectId: String, pipelineId: String?, ruleHashId: String?, interceptResult: RuleInterceptResult?, startTime: Long?, endTime: Long?, page: Int?, pageSize: Int?): Result<Page<RuleInterceptHistory>> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 20
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val result = historyService.listInterceptHistory(userId, projectId, pipelineId, ruleHashId, interceptResult, startTime, endTime, limit.offset, limit.limit)
        return Result(Page(pageNotNull, pageSizeNotNull, result.first, result.second))
    }

    override fun getAuditUserList(userId: String, projectId: String, pipelineId: String, buildId: String, taskId: String): Result<Set<String>> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (pipelineId.isBlank()) {
            throw ParamBlankException("Invalid pipelineId")
        }
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        return Result(ruleCheckService.getAuditUserList(projectId, pipelineId, buildId, taskId))
    }
}
/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.quality.resources.v2

import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.quality.api.v2.UserQualityRuleResource
import com.tencent.devops.quality.api.v2.pojo.RulePipelineRange
import com.tencent.devops.quality.api.v2.pojo.RuleTemplate
import com.tencent.devops.quality.api.v2.pojo.RuleTemplateRange
import com.tencent.devops.quality.api.v2.pojo.request.PipelineRangeDetailRequest
import com.tencent.devops.quality.api.v2.pojo.request.RuleCreateRequest
import com.tencent.devops.quality.api.v2.pojo.request.RuleUpdateRequest
import com.tencent.devops.quality.api.v2.pojo.request.TemplateRangeDetailRequest
import com.tencent.devops.quality.api.v2.pojo.response.AtomRuleResponse
import com.tencent.devops.quality.api.v2.pojo.response.QualityRuleMatchTask
import com.tencent.devops.quality.api.v2.pojo.response.QualityRuleSummaryWithPermission
import com.tencent.devops.quality.api.v2.pojo.response.UserQualityRule
import com.tencent.devops.quality.pojo.RuleInterceptHistory
import com.tencent.devops.quality.service.v2.QualityHistoryService
import com.tencent.devops.quality.service.v2.QualityPipelineService
import com.tencent.devops.quality.service.v2.QualityRuleCheckService
import com.tencent.devops.quality.service.v2.QualityRuleService
import com.tencent.devops.quality.service.v2.QualityTemplateService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserQualityRuleResourceImpl @Autowired constructor(
    private val ruleService: QualityRuleService,
    private val historyService: QualityHistoryService,
    private val ruleCheckService: QualityRuleCheckService,
    private val templateService: QualityTemplateService,
    private val pipelineService: QualityPipelineService
) : UserQualityRuleResource {

    override fun hasCreatePermission(userId: String, projectId: String): Result<Boolean> {
        checkParam(userId, projectId)
        return Result(ruleService.hasCreatePermission(userId, projectId))
    }

    @AuditEntry(actionId = ActionId.RULE_CREATE)
    override fun create(userId: String, projectId: String, rule: RuleCreateRequest): Result<String> {
        checkParam(userId, projectId)
        return Result(ruleService.userCreate(userId, projectId, rule))
    }

    @AuditEntry(actionId = ActionId.RULE_EDIT)
    override fun update(
        userId: String,
        projectId: String,
        ruleHashId: String,
        rule: RuleUpdateRequest
    ): Result<Boolean> {
        checkParam(userId, projectId, ruleHashId)
        ruleService.userUpdate(userId, projectId, ruleHashId, rule)
        return Result(true)
    }

    @AuditEntry(actionId = ActionId.RULE_ENABLE)
    override fun enable(userId: String, projectId: String, ruleHashId: String): Result<Boolean> {
        checkParam(userId, projectId, ruleHashId)
        ruleService.userUpdateEnable(userId, projectId, ruleHashId, true)
        return Result(true)
    }

    @AuditEntry(actionId = ActionId.RULE_ENABLE)
    override fun disable(userId: String, projectId: String, ruleHashId: String): Result<Boolean> {
        checkParam(userId, projectId, ruleHashId)
        ruleService.userUpdateEnable(userId, projectId, ruleHashId, false)
        return Result(true)
    }

    @AuditEntry(actionId = ActionId.RULE_DELETE)
    override fun delete(userId: String, projectId: String, ruleHashId: String): Result<Boolean> {
        checkParam(userId, projectId, ruleHashId)
        ruleService.userDelete(userId, projectId, ruleHashId)
        return Result(true)
    }

    override fun get(userId: String, projectId: String, ruleHashId: String): Result<UserQualityRule> {
        checkParam(userId, projectId, ruleHashId)
        val rule = ruleService.userGetRule(userId, projectId, ruleHashId)
        rule.interceptRecent = historyService.userGetInterceptRecent(projectId, rule.hashId)
        return Result(rule)
    }

    override fun getInterceptHistory(
        userId: String,
        projectId: String,
        ruleHashId: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<RuleInterceptHistory>> {
        checkParam(userId, projectId, ruleHashId)
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 20
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val result = historyService.userGetInterceptHistory(userId, projectId, ruleHashId, limit.offset, limit.limit)
        return Result(Page(pageNotNull, pageSizeNotNull, result.first, result.second))
    }

    override fun list(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<QualityRuleSummaryWithPermission>> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 20
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val result = ruleService.listRuleDataSummary(userId, projectId, limit.offset, limit.limit)
        return Result(Page(pageNotNull, pageSizeNotNull, result.first, result.second))
    }

    override fun matchRuleList(
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<List<QualityRuleMatchTask>> {
        checkParam(userId, projectId)
        val result = ruleCheckService.userGetMatchRuleList(projectId, pipelineId)
        return Result(result)
    }

    override fun matchTemplateRuleList(
        userId: String,
        projectId: String,
        templateId: String?
    ): Result<List<QualityRuleMatchTask>> {
        return Result(ruleCheckService.userGetMatchTemplateList(projectId, templateId))
    }

    override fun listTemplates(projectId: String): Result<List<RuleTemplate>> {
        return Result(templateService.userList(projectId))
    }

    override fun listProjectTemplates(projectId: String): Result<List<RuleTemplate>> {
        return Result(templateService.userList(projectId))
    }

    override fun listPipelineRangeDetail(request: PipelineRangeDetailRequest): Result<List<RulePipelineRange>> {
        return Result(
            pipelineService.userListPipelineRangeDetail(request)
        )
    }

    override fun listTemplateRangeDetail(request: TemplateRangeDetailRequest): Result<List<RuleTemplateRange>> {
        return Result(
            pipelineService.userListTemplateRangeDetail(request)
        )
    }

    override fun listAtomRule(
        userId: String,
        projectId: String,
        pipelineId: String,
        atomCode: String,
        atomVersion: String
    ): Result<AtomRuleResponse> {
        return Result(ruleCheckService.userListAtomRule(projectId, pipelineId, atomCode, atomVersion))
    }

    override fun listTemplateAtomRule(
        userId: String,
        projectId: String,
        templateId: String,
        atomCode: String,
        atomVersion: String
    ): Result<AtomRuleResponse> {
        return Result(ruleCheckService.userListTemplateAtomRule(projectId, templateId, atomCode, atomVersion))
    }

    fun checkParam(userId: String, projectId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
    }

    fun checkParam(userId: String, projectId: String, ruleHashId: String) {
        checkParam(userId, projectId)
        if (ruleHashId.isBlank()) {
            throw ParamBlankException("Invalid ruleHashId")
        }
    }
}

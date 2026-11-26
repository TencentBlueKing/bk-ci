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

package com.tencent.devops.process.trigger.scm.condition

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BRANCH_IGNORED
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BRANCH_NOT_MATCH
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.SOURCE_BRANCH_IGNORED
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.SOURCE_BRANCH_NOT_MATCH
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.TAG_NAME_IGNORED
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.TAG_NAME_NOT_MATCH
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.TAG_SOURCE_BRANCH_NOT_MATCH
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.TARGET_BRANCH_IGNORED
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.TARGET_BRANCH_NOT_MATCH
import com.tencent.devops.common.webhook.service.code.filter.BranchFilter
import com.tencent.devops.common.webhook.util.WebhookUtils.convert

/**
 * 分支过滤条件
 *
 */
class BranchCondition(private val filterType: BranchFilterType) : WebhookCondition {
    override fun match(context: WebhookConditionContext): Boolean {
        with(context.webhookParams) {
            val (includedMessageCode, excludedMessageCode) = when (filterType) {
                BranchFilterType.BRANCH ->
                    Pair(BRANCH_NOT_MATCH, BRANCH_IGNORED)

                BranchFilterType.SOURCE_BRANCH ->
                    Pair(SOURCE_BRANCH_NOT_MATCH, SOURCE_BRANCH_IGNORED)

                BranchFilterType.TARGET_BRANCH ->
                    Pair(TARGET_BRANCH_NOT_MATCH, TARGET_BRANCH_IGNORED)

                BranchFilterType.TAG_NAME ->
                    Pair(TAG_NAME_NOT_MATCH, TAG_NAME_IGNORED)

                BranchFilterType.TAG_CREATE_FROM ->
                    Pair(TAG_SOURCE_BRANCH_NOT_MATCH, "")
            }
            val (includedBranches, excludedBranches, triggerOnBranchName) = when (filterType) {
                BranchFilterType.BRANCH ->
                    Triple(convert(branchName), convert(excludeBranchName), context.factParam.branch)

                BranchFilterType.SOURCE_BRANCH ->
                    Triple(
                        convert(includeSourceBranchName),
                        convert(excludeSourceBranchName),
                        context.factParam.sourceBranch
                    )

                BranchFilterType.TARGET_BRANCH ->
                    Triple(convert(branchName), convert(excludeBranchName), context.factParam.branch)

                BranchFilterType.TAG_NAME ->
                    Triple(convert(tagName), listOf(), context.factParam.branch)

                BranchFilterType.TAG_CREATE_FROM ->
                    Triple(convert(fromBranches), listOf(), context.factParam.sourceBranch)
            }
            val branchFilter = BranchFilter(
                pipelineId = context.pipelineId,
                triggerOnBranchName = triggerOnBranchName,
                includedBranches = includedBranches,
                excludedBranches = excludedBranches,
                includedFailedReason = I18Variable(
                    code = includedMessageCode,
                    params = listOf(triggerOnBranchName)
                ).toJsonStr(),
                excludedFailedReason = I18Variable(
                    code = excludedMessageCode,
                    params = listOf(triggerOnBranchName)
                ).toJsonStr()
            )
            return branchFilter.doFilter(context.response)
        }
    }
}

enum class BranchFilterType {
    BRANCH,
    SOURCE_BRANCH,
    TARGET_BRANCH,
    TAG_NAME, // Tag 名称匹配
    TAG_CREATE_FROM // Tag 来源分支匹配
}

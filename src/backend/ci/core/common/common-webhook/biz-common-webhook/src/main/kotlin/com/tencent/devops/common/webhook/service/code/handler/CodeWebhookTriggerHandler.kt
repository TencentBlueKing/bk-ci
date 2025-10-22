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

package com.tencent.devops.common.webhook.service.code.handler

import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.webhook.pojo.code.CodeWebhookEvent
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilter
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilterChain
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilterResponse
import com.tencent.devops.common.webhook.service.code.pojo.WebhookMatchResult
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.scm.pojo.WebhookCommit

@Suppress("TooManyFunctions")
interface CodeWebhookTriggerHandler<T : CodeWebhookEvent> {

    /**
     * 处理类是否能够处理
     */
    fun eventClass(): Class<T>

    fun getUrl(event: T): String

    fun getUsername(event: T): String

    fun getRevision(event: T): String

    fun getRepoName(event: T): String

    fun getBranchName(event: T): String

    fun getEventType(): CodeEventType

    fun getEventType(event: T): CodeEventType? = null

    fun getHookSourceUrl(event: T): String? = null

    fun getHookTargetUrl(event: T): String? = null

    fun getEnv(event: T): Map<String, Any> = emptyMap()

    fun getMergeRequestId(event: T): Long? = null

    /**
     * 事件产生时的消息
     */
    fun getMessage(event: T): String?

    /**
     * 获取事件说明,根据不同的事件组织事件说明
     */
    fun getEventDesc(event: T): String = ""

    /**
     * 获取webhook事件生产者ID,工蜂-工蜂ID,github-github id,svn-svn path,p4-p4port
     */
    fun getExternalId(event: T): String = ""

    fun preMatch(event: T): WebhookMatchResult = WebhookMatchResult(isMatch = true)

    fun getWebhookCommitList(
        event: T,
        projectId: String?,
        repository: Repository?,
        page: Int,
        size: Int
    ): List<WebhookCommit> {
        return emptyList()
    }

    /**
     * 匹配事件
     */
    fun isMatch(
        event: T,
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: WebHookParams
    ): WebhookMatchResult {
        val filters = getWebhookFilters(
            event = event,
            projectId = projectId,
            pipelineId = pipelineId,
            repository = repository,
            webHookParams = webHookParams
        )
        val response = WebhookFilterResponse()
        return if (filters.isNotEmpty()) {
            WebhookMatchResult(
                isMatch = WebhookFilterChain(filters = filters).doFilter(response),
                extra = response.params,
                reason = response.failedReason
            )
        } else {
            WebhookMatchResult(isMatch = true)
        }
    }

    fun getWebhookFilters(
        event: T,
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: WebHookParams
    ): List<WebhookFilter>

    fun retrieveParams(
        event: T,
        projectId: String? = null,
        repository: Repository? = null
    ): Map<String, Any>

    /**
     * 兼容仓库名，考虑仓库迁移场景
     * 当webhook中[Repo_Group/Repo_Name]和[Repo_Group_Suffix/Repo_Name] 两个RepoName都指向同一个仓库时
     * 在此处做兼容，进而触发相关流水线
     */
    fun getCompatibilityRepoName(event: T): Set<String> = setOf()
}

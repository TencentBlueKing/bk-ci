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

package com.tencent.devops.common.webhook.service.code.matcher

import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.service.code.pojo.WebhookMatchResult
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.scm.pojo.WebhookCommit
import org.slf4j.LoggerFactory

@Suppress("TooManyFunctions")
interface ScmWebhookMatcher {

    /**
     * 预匹配,在还没有解析流水线列表时判断
     */
    fun preMatch(): WebhookMatchResult

    fun isMatch(
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: WebHookParams
    ): WebhookMatchResult

    fun getUsername(): String

    fun getRevision(): String

    fun getRepoName(): String

    fun getBranchName(): String?

    fun getEventType(): CodeEventType

    fun getCodeType(): CodeType

    fun getHookSourceUrl(): String? = null

    fun getHookTargetUrl(): String? = null

    fun getEnv() = emptyMap<String, Any>()

    fun getMergeRequestId(): Long? = null

    fun getMessage(): String?

    /**
     * 获取事件描述,根据不同的事件组织事件说明
     */
    fun getEventDesc(): String = ""

    /**
     * 获取webhook事件生产者ID,工蜂-工蜂ID,github-github id,svn-svn path,p4-p4port
     */
    fun getExternalId(): String = ""

    fun getWebHookParamsMap(): Map<String/*pipelineId*/, WebHookParams/*pipeline webhookParams*/> = emptyMap()

    fun retrieveParams(
        projectId: String? = null,
        repository: Repository? = null
    ): Map<String, Any>

    data class MatchResult(
        val isMatch: Boolean,
        val extra: Map<String, String> = mapOf()
    )

    fun getWebhookCommitList(
        projectId: String,
        pipelineId: String,
        repository: Repository,
        page: Int,
        size: Int
    ): List<WebhookCommit> {
        return emptyList()
    }

    fun getCompatibilityRepoName(): Set<String> = setOf()

    companion object {
        private val logger = LoggerFactory.getLogger(ScmWebhookMatcher::class.java)
    }
}

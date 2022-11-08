/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.devops.stream.trigger.parsers

import com.tencent.devops.common.webhook.enums.code.tgit.TGitPushOperationKind
import com.tencent.devops.common.webhook.pojo.code.git.GitPushEvent
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.config.StreamPreTriggerConfig
import com.tencent.devops.stream.service.StreamBasicSettingService
import com.tencent.devops.stream.service.StreamOauthService
import com.tencent.devops.stream.service.StreamScmService
import com.tencent.devops.stream.util.GitCommonUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TXPreTrigger @Autowired constructor(
    private val config: StreamPreTriggerConfig,
    private val scmService: StreamScmService,
    private val streamOauthService: StreamOauthService,
    private val gitBasicSettingService: StreamBasicSettingService,
    private val streamGitConfig: StreamGitConfig
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TXPreTrigger::class.java)
        private const val DEVELOPER = 30
    }

    // 开启研发商店插件的ci
    fun enableAtomCi(
        event: GitPushEvent
    ) {
        if (!event.isCreate()) {
            return
        }

        gitBasicSettingService.getStreamConf(event.project_id).let {
            if (it != null && it.enableCi) {
                return
            }
        }
        // TODO: 目前直接写死使用橘子的oauth去拿用户的名称，后续支持公共账号了再改成公共账号
        val token = streamOauthService.getOauthToken("fayewang")?.accessToken
        if (token.isNullOrBlank()) {
            logger.warn("TXPreTrigger|enableAtomCi|create from store atom get project members error: get token null")
            return
        }
        // 因为用户是 devops 所以需要修改
        val realUser = getRealUser(event, token)
        if (realUser.isNullOrBlank()) {
            logger.warn("TXPreTrigger|enableAtomCi|create from store atom get project members error: no develop user")
            return
        }

        try {
            gitBasicSettingService.initStreamConf(
                userId = realUser,
                projectId = GitCommonUtils.getCiProjectId(event.project_id, streamGitConfig.getScmType()),
                gitProjectId = event.project_id,
                enabled = true
            )
        } catch (e: Throwable) {
            logger.warn("TXPreTrigger|enableAtomCi|error=${e.message}")
        }
    }

    private fun getRealUser(event: GitPushEvent, token: String): String? {
        val projectMember = scmService.getProjectMembersRetry(
            token = token,
            gitProjectId = event.project_id.toString(),
            page = 1,
            pageSize = 20,
            search = null
        )
        if (projectMember.isNullOrEmpty()) {
            logger.warn("TXPreTrigger|getRealUser|create from store atom get project members error")
            return null
        }
        var realUser: String? = null
        run breaking@{
            projectMember.forEach { member ->
                if (member.accessLevel >= DEVELOPER) {
                    realUser = member.username
                    return@breaking
                }
            }
        }
        return realUser
    }

    private fun GitPushEvent.isCreate(): Boolean {
        if (operation_kind != TGitPushOperationKind.CREAT.value || user_name != config.username) {
            return false
        }

        if (repository.git_http_url.isNotBlank() && repository.git_http_url.startsWith(config.gitPrefix!!)) {
            return true
        }

        return false
    }
}

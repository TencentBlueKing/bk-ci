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

package com.tencent.devops.gitci.v2.template

import com.tencent.devops.gitci.v2.service.GitCIBasicSettingService
import com.tencent.devops.gitci.v2.service.OauthService
import com.tencent.devops.gitci.v2.service.ScmService
import com.tencent.devops.gitci.v2.service.TicketService
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.lang.RuntimeException

@Service
class YamlTemplateService @Autowired constructor(
    private val oauthService: OauthService,
    private val scmService: ScmService,
    private val gitCIBasicSettingService: GitCIBasicSettingService,
    private val ticketService: TicketService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(YamlTemplateService::class.java)
        private const val templateDirectory = ".ci/templates/"
    }

    // 获取当前库文件，使用超级token直接拉取，token区分fork和非fork
    fun getTemplate(
        token: String,
        fileName: String,
        gitProjectId: Long,
        userId: String,
        ref: String
    ): String {
        return scmService.getYamlFromGit(
            token = token,
            gitProjectId = gitProjectId,
            ref = ref,
            fileName = templateDirectory + fileName,
            useAccessToken = true
        )
    }

    /**
     * 获取远程库文件，没有凭证使用项目开启人，有凭证，使用用户写的
     * 如果是fork库，凭证使用目标库的凭证
     * gitProjectId: fork库为主库ID
     */
    fun getResTemplate(
        gitProjectId: Long,
        repo: String,
        ref: String? = "master",
        personalAccessToken: String?,
        userId: String,
        fileName: String
    ): String {
        if (personalAccessToken.isNullOrBlank()) {
            val enableUserId =
                gitCIBasicSettingService.getGitCIConf(gitProjectId)?.enableUserId
                    ?: throw RuntimeException(
                        "工蜂项目${gitProjectId}未开启工蜂CI"
                    )
            val token = oauthService.getOauthToken(enableUserId)?.accessToken
                ?: throw RuntimeException("用户${enableUserId}未进行OAuth授权")
            val targetProjectId = scmService.getProjectInfo(
                token = token,
                gitProjectId = repo,
                useAccessToken = true
            )?.gitProjectId ?: throw RuntimeException("未找到项目$repo")
            return scmService.getYamlFromGit(
                token = token,
                gitProjectId = targetProjectId.toLong(),
                ref = ref!!,
                fileName = templateDirectory + fileName,
                useAccessToken = false
            )
        } else {
            val (isTicket, key) = getKey(personalAccessToken)
            val token = if (isTicket) {
                val ticket = ticketService.getCredential(
                    projectId = "git_$gitProjectId",
                    credentialId = key
                )
                if (ticket["type"] != CredentialType.ACCESSTOKEN.name) {
                    throw RuntimeException("不支持凭证类型: ${ticket["type"]}")
                }
                ticket["v1"]!!
            } else {
                key
            }
            val targetProjectId = scmService.getProjectInfo(
                token = token,
                gitProjectId = repo,
                useAccessToken = false
            )?.gitProjectId ?: throw RuntimeException("未找到项目$repo")
            return scmService.getYamlFromGit(
                token = token,
                gitProjectId = targetProjectId.toLong(),
                ref = ref!!,
                fileName = templateDirectory + fileName,
                useAccessToken = false
            )
        }
    }

    private fun getKey(personalAccessToken: String): Pair<Boolean, String> {
        return if (personalAccessToken.contains("\${{") && personalAccessToken.contains("}}")) {
            val str = personalAccessToken.split("\${{")[1].split("}}")[0]
            if (str.startsWith("settings.")) {
                Pair(true, str.removePrefix("settings."))
            } else {
                throw RuntimeException("\$凭证仅支持setting.引用")
            }
        } else {
            Pair(false, personalAccessToken)
        }
    }
}

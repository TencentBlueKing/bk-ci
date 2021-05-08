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

import com.tencent.devops.common.client.Client
import com.tencent.devops.gitci.v2.service.ScmService
import com.tencent.devops.gitci.v2.service.TicketService
import com.tencent.devops.gitci.v2.template.pojo.enums.ResourceCredentialType
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.lang.RuntimeException

@Service
class YamlTemplateService @Autowired constructor(
    private val client: Client,
    private val scmService: ScmService,
    private val ticketService: TicketService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(YamlTemplateService::class.java)
        private const val ciFileName = ".ci.yml"
        private const val templateDirectoryName = ".ci/templates"
        private const val ciFileExtension = ".yml"
    }

    fun getResTemplates(
        gitProjectId: Long,
        repo: String,
        name: String,
        ref: String,
        credentialType: ResourceCredentialType,
        key: String,
        userTicket: Boolean,
        userId: String
    ): Map<String, String?> {
        when (credentialType) {
            ResourceCredentialType.PRIVATE_KEY -> {
                val token = if (!userTicket) {
                    val ticket = ticketService.getCredential("git_$gitProjectId", credentialId = key)
                    if (ticket["type"] != CredentialType.ACCESSTOKEN.name) {
                        throw RuntimeException("Not Support this credentialType: ${ticket["type"]}")
                    }
                    ticket["v1"]!!
                } else {
                    key
                }
                val targetProjectId = scmService.getProjectInfo(
                    token = token,
                    gitProjectId = repo,
                    useAccessToken = false
                )?.gitProjectId ?: throw RuntimeException("Cant find project $repo")
                return scmService.getAllTemplates(
                    token = token,
                    gitProjectId = targetProjectId.toLong(),
                    filePath = templateDirectoryName,
                    ref = ref,
                    useAccessToken = false,
                    removePrefix = ".ci/",
                    addLastFix = "@$name"
                )
            }
            ResourceCredentialType.OAUTH -> {
                val accessToken = client.get(ServiceOauthResource::class).gitGet(userId).data ?: throw
                RuntimeException("$userId hasn't oauth")
                val targetProjectId = scmService.getProjectInfo(
                    token = accessToken.accessToken,
                    gitProjectId = repo,
                    useAccessToken = true
                )?.gitProjectId ?: throw RuntimeException("Cant find project $repo")
                return scmService.getAllTemplates(
                    token = accessToken.accessToken,
                    gitProjectId = targetProjectId.toLong(),
                    filePath = templateDirectoryName,
                    ref = ref,
                    useAccessToken = true,
                    removePrefix = ".ci/",
                    addLastFix = "@$name"
                )
            }
            else -> {
                throw RuntimeException("Not Support this credentialType")
            }
        }
    }
}

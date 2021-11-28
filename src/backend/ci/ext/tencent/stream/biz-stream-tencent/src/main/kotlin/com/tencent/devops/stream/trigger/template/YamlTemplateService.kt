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

package com.tencent.devops.stream.trigger.template

import com.fasterxml.jackson.core.JsonProcessingException
import com.tencent.devops.common.ci.v2.enums.gitEventKind.TGitMergeActionKind
import com.tencent.devops.common.ci.v2.exception.YamlFormatException
import com.tencent.devops.common.ci.v2.utils.ScriptYmlUtils
import com.tencent.devops.stream.common.exception.YamlBehindException
import com.tencent.devops.stream.common.exception.YamlBlankException
import com.tencent.devops.common.webhook.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.stream.trigger.parsers.YamlVersion
import com.tencent.devops.stream.trigger.parsers.yamlCheck.YamlSchemaCheck
import com.tencent.devops.stream.trigger.template.pojo.GetTemplateParam
import com.tencent.devops.stream.trigger.template.pojo.enums.TemplateType
import com.tencent.devops.stream.v2.service.StreamOauthService
import com.tencent.devops.stream.v2.service.StreamScmService
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class YamlTemplateService @Autowired constructor(
    private val oauthService: StreamOauthService,
    private val streamScmService: StreamScmService,
    private val ticketService: TicketService,
    private val yamlVersion: YamlVersion,
    private val yamlSchemaCheck: YamlSchemaCheck
) {

    companion object {
        private const val templateDirectory = ".ci/templates/"
        const val UN_SUPPORT_TICKET_ERROR = "Unsupported ticket type: [%s]"
        const val ONLY_SUPPORT_ERROR = "Only supports using the settings context to access credentials"
    }

    /**
     * 获取代码库文件，
     * 1、有token直接使用token
     * 2、没有personalAccessToken使用项目开启人的Oauth，
     * 3、有personalAccessToken，使用用户写的("settings.xxx"是从凭证系统去拿)
     * 3、如果是fork库，凭证系统使用目标库的蓝盾项目的凭证系统
     * 注：gitProjectId: fork库为主库ID
     */
    @Suppress("ComplexMethod")
    @Throws(YamlBlankException::class, YamlFormatException::class, JsonProcessingException::class)
    fun getTemplate(
        param: GetTemplateParam
    ): String {
        val token = param.token
        val event = param.event
        val forkToken = param.forkToken
        val gitProjectId = param.gitProjectId
        val targetRepo = param.targetRepo
        val ref = param.ref
        val personalAccessToken = param.personalAccessToken
        val fileName = param.fileName
        val changeSet = param.changeSet
        val templateType = param.templateType

        if (token != null) {
            // 获取触发库的模板需要对比版本问题
            val content = if (event is GitMergeRequestEvent && event.object_attributes.action !=
                TGitMergeActionKind.MERGE.value
            ) {
                val (result, orgYaml) = yamlVersion.checkYmlVersion(
                    mrEvent = event,
                    targetGitToken = token,
                    sourceGitToken = forkToken,
                    filePath = templateDirectory + fileName,
                    changeSet = changeSet ?: emptySet()
                )
                if (!result) {
                    throw YamlBehindException(templateDirectory + fileName)
                }
                orgYaml
            } else {
                streamScmService.getYamlFromGit(
                    token = forkToken ?: token,
                    gitProjectId = gitProjectId.toString(),
                    ref = ref!!,
                    fileName = templateDirectory + fileName,
                    useAccessToken = true
                )
            }
            if (content.isBlank()) {
                throw YamlBlankException(templateDirectory + fileName)
            }

            schemaCheck(templateDirectory + fileName, content, templateType)

            return ScriptYmlUtils.formatYaml(content)
        }
        if (personalAccessToken.isNullOrBlank()) {
            val oAuthToken = oauthService.getGitCIEnableToken(gitProjectId).accessToken

            val content = streamScmService.getYamlFromGit(
                token = oAuthToken,
                gitProjectId = targetRepo!!,
                ref = ref ?: getDefaultBranch(oAuthToken, targetRepo, true),
                fileName = templateDirectory + fileName,
                useAccessToken = true
            ).ifBlank { throw YamlBlankException(templateDirectory + fileName, targetRepo) }

            schemaCheck(templateDirectory + fileName, content, templateType)

            return ScriptYmlUtils.formatYaml(content)
        } else {
            val (isTicket, key) = getKey(personalAccessToken)
            val personToken = if (isTicket) {
                val ticket = ticketService.getCredential(
                    projectId = "git_$gitProjectId",
                    credentialId = key
                )
                if (ticket["type"] != CredentialType.ACCESSTOKEN.name) {
                    throw YamlFormatException(UN_SUPPORT_TICKET_ERROR.format(ticket["type"]))
                }
                ticket["v1"]!!
            } else {
                key
            }

            val content = streamScmService.getYamlFromGit(
                token = personToken,
                gitProjectId = targetRepo!!,
                ref = ref ?: getDefaultBranch(personToken, targetRepo, false),
                fileName = templateDirectory + fileName,
                useAccessToken = false
            ).ifBlank { throw YamlBlankException(templateDirectory + fileName, targetRepo) }

            // 针对模板替换时，如果类型为空就不校验
            schemaCheck(templateDirectory + fileName, content, templateType)

            return ScriptYmlUtils.formatYaml(content)
        }
    }

    private fun schemaCheck(file: String, originYaml: String, templateType: TemplateType?) {
        try {
            yamlSchemaCheck.check(originYaml, templateType, false)
        } catch (e: YamlFormatException) {
            throw YamlFormatException("${templateType?.text} template $file schema error: ${e.message}")
        }
    }

    private fun getDefaultBranch(token: String, gitProjectId: String, useAccessToken: Boolean): String {
        return streamScmService.getProjectInfoRetry(token, gitProjectId, useAccessToken).defaultBranch!!
    }

    private fun getKey(personalAccessToken: String): Pair<Boolean, String> {
        return if (personalAccessToken.contains("\${{") && personalAccessToken.contains("}}")) {
            val str = personalAccessToken.split("\${{")[1].split("}}")[0].trim()
            if (str.startsWith("settings.")) {
                Pair(true, str.removePrefix("settings."))
            } else {
                throw YamlFormatException(ONLY_SUPPORT_ERROR)
            }
        } else {
            Pair(false, personalAccessToken)
        }
    }
}

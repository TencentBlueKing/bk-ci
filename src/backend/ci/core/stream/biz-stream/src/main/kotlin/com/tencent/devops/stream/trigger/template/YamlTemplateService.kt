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
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.yaml.v2.enums.TemplateType
import com.tencent.devops.process.yaml.v2.exception.YamlFormatException
import com.tencent.devops.process.yaml.v2.parsers.template.models.GetTemplateParam
import com.tencent.devops.process.yaml.v2.utils.ScriptYmlUtils
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.exception.YamlBlankException
import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.stream.trigger.parsers.StreamTriggerCache
import com.tencent.devops.stream.trigger.parsers.yamlCheck.YamlSchemaCheck
import com.tencent.devops.stream.util.CommonCredentialUtils
import com.tencent.devops.stream.util.GitCommonUtils
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ComplexCondition", "NestedBlockDepth")
@Service
class YamlTemplateService @Autowired constructor(
    private val client: Client,
    private val yamlSchemaCheck: YamlSchemaCheck,
    private val streamTriggerCache: StreamTriggerCache,
    private val streamGitConfig: StreamGitConfig
) {

    companion object {
        private val logger = LoggerFactory.getLogger(YamlTemplateService::class.java)
        private const val templateDirectory = ".ci/templates/"
        const val GET_TICKET_ERROR = "get ticket type: [%s]"
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
        param: GetTemplateParam<BaseAction>
    ): String {
        logger.info("YamlTemplateService|getTemplate|param|${param.format()}")
        with(param) {
            // 获取当前库的模板
            if (targetRepo == null) {
                val content = extraParameters.getYamlContent(templateDirectory + path)

                if (content.content.isBlank()) {
                    throw YamlBlankException(templateDirectory + path, content.ref)
                }

                schemaCheck(templateDirectory + path, content.content, templateType)

                return ScriptYmlUtils.formatYaml(content.content)
            }
            // 获取目标库模板，但没有填写凭证token信息，使用开启人的
            if (targetRepo?.credentials?.personalAccessToken.isNullOrBlank()) {
                val ref = targetRepo?.ref ?: streamTriggerCache.getAndSaveRequestGitProjectInfo(
                    gitProjectKey = targetRepo!!.repository,
                    action = extraParameters,
                    getProjectInfo = extraParameters.api::getGitProjectInfo,
                    cred = extraParameters.getGitCred()
                ).let {
                    if (it == null) {
                        throw YamlFormatException(
                            "${extraParameters.data.setting.enableUser} access ${targetRepo!!.repository} error, " +
                                "Check projectName or ci enable user permission"
                        )
                    }
                    it
                }.defaultBranch!!
                val content = extraParameters.api.getFileContent(
                    cred = extraParameters.getGitCred(),
                    gitProjectId = extraParameters.getGitProjectIdOrName(targetRepo!!.repository),
                    fileName = templateDirectory + path,
                    ref = ref,
                    retry = ApiRequestRetryInfo(true)
                ).ifBlank { throw YamlBlankException(templateDirectory + path, ref, targetRepo?.repository) }

                schemaCheck(templateDirectory + path, content, templateType)

                return ScriptYmlUtils.formatYaml(content)
            } else {
                // 获取目标库模板，通过填写的凭证token信息
                val content = getYamlByPersonToken(param)
                return ScriptYmlUtils.formatYaml(content)
            }
        }
    }

    private fun getYamlByPersonToken(
        param: GetTemplateParam<BaseAction>
    ): String {
        with(param) {
            val key = targetRepo?.credentials?.personalAccessToken!!
            val personToken = kotlin.runCatching { getTicket(param, key) }.getOrDefault(key)
            val ref = targetRepo?.ref ?: streamTriggerCache.getAndSaveRequestGitProjectInfo(
                gitProjectKey = targetRepo?.repository!!,
                action = extraParameters,
                getProjectInfo = extraParameters.api::getGitProjectInfo,
                cred = extraParameters.getGitCred(personToken)
            )!!.defaultBranch!!
            val content = extraParameters.api.getFileContent(
                cred = extraParameters.getGitCred(personToken = personToken),
                gitProjectId = extraParameters.getGitProjectIdOrName(targetRepo!!.repository),
                fileName = templateDirectory + path,
                ref = ref,
                retry = ApiRequestRetryInfo(true)
            ).ifBlank { throw YamlBlankException(templateDirectory + path, ref, targetRepo?.repository) }

            // 针对模板替换时，如果类型为空就不校验
            schemaCheck(templateDirectory + path, content, templateType)
            return content
        }
    }

    private fun getTicket(param: GetTemplateParam<BaseAction>, key: String): String {
        with(param) {
            try {
                return CommonCredentialUtils.getCredential(
                    client = client,
                    projectId = extraParameters.getProjectCode(),
                    credentialId = key,
                    typeCheck = listOf(CredentialType.ACCESSTOKEN)
                ).v1
            } catch (ignore: Exception) {
                if (nowRepoId == null) {
                    // 没有库信息说明是触发库，并不需要获取跨项目信息
                    throw YamlFormatException("no across" + GET_TICKET_ERROR.format(ignore.message))
                }
            }
            // 运行至此说明当前触发项目中没用保存凭证，需要校验远程库是否开启凭证共享
            // 因为需要获取到引用的远程库的gitProjectId(num)才可以拿到凭证，所以需要先用开启人的oauth去拿
            val acrossGitProjectId = streamTriggerCache.getAndSaveRequestGitProjectInfo(
                gitProjectKey = nowRepoId!!,
                action = extraParameters,
                getProjectInfo = extraParameters.api::getGitProjectInfo,
                cred = extraParameters.getGitCred()
            )!!.gitProjectId
            logger.info("YamlTemplateService|getTemplate|getTicket|acrossGitProjectId|$acrossGitProjectId")
            try {
                return CommonCredentialUtils.getCredential(
                    client = client,
                    projectId = GitCommonUtils.getCiProjectId(
                        acrossGitProjectId.toLong(),
                        streamGitConfig.getScmType()
                    ),
                    credentialId = key,
                    typeCheck = listOf(CredentialType.ACCESSTOKEN),
                    acrossProject = true
                ).v1
            } catch (ignore: Exception) {
                throw YamlFormatException("across" + GET_TICKET_ERROR.format(ignore.message))
            }
        }
    }

    private fun schemaCheck(file: String, originYaml: String, templateType: TemplateType?) {
        try {
            yamlSchemaCheck.check(originYaml, templateType, false)
        } catch (e: YamlFormatException) {
            throw YamlFormatException("${templateType?.text} template $file schema error: ${e.message}")
        }
    }

    private fun getKey(personalAccessToken: String): Pair<Boolean, String> {
        return if (personalAccessToken.contains("\${{") && personalAccessToken.contains("}}")) {
            val str = personalAccessToken.split("\${{")[1].split("}}")[0].trim()
            val key = getCredentialKey(str)
            if (str != key) {
                Pair(true, key)
            } else {
                throw YamlFormatException(ONLY_SUPPORT_ERROR)
            }
        } else {
            Pair(false, personalAccessToken)
        }
    }

    private fun getCredentialKey(key: String): String {
        // 参考CredentialType
        return if (key.startsWith("settings.") && (
                key.endsWith(".password") ||
                    key.endsWith(".access_token") ||
                    key.endsWith(".username") ||
                    key.endsWith(".secretKey") ||
                    key.endsWith(".appId") ||
                    key.endsWith(".privateKey") ||
                    key.endsWith(".passphrase") ||
                    key.endsWith(".token") ||
                    key.endsWith(".cosappId") ||
                    key.endsWith(".secretId") ||
                    key.endsWith(".region")
                )
        ) {
            key.substringAfter("settings.").substringBeforeLast(".")
        } else {
            key
        }
    }
}

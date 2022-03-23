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

package com.tencent.devops.stream.trigger.parsers.modelCreate

import com.devops.process.yaml.modelCreate.ModelCommon
import com.tencent.devops.common.api.util.EmojiUtil
import com.tencent.devops.common.ci.v2.ScriptBuildYaml
import com.tencent.devops.common.ci.v2.Variable
import com.tencent.devops.common.ci.v2.YamlTransferData
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_COMMIT_AUTHOR
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_COMMIT_MESSAGE
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_GROUP
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_NAME
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_SHA
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_SHA_SHORT
import com.tencent.devops.common.webhook.enums.code.tgit.TGitObjectKind
import com.tencent.devops.common.webhook.pojo.code.BK_CI_RUN
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitPushEvent
import com.tencent.devops.process.utils.PIPELINE_BUILD_MSG
import com.tencent.devops.scm.utils.code.git.GitUtils
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.trigger.pojo.StreamGitProjectCache
import com.tencent.devops.stream.trigger.v2.StreamYamlBuild
import com.tencent.devops.stream.v2.common.CommonVariables

@Suppress("ComplexMethod")
object ModelParameters {

    private const val PUSH_OPTIONS_PREFIX = "ci.variable::"

    fun createPipelineParams(
        yaml: ScriptBuildYaml,
        streamGitProjectInfo: StreamGitProjectCache,
        event: GitRequestEvent,
        v2GitUrl: String?,
        originEvent: GitEvent?,
        webhookParams: Map<String, String> = mapOf(),
        yamlTransferData: YamlTransferData? = null
    ): MutableList<BuildFormProperty> {
        val result = mutableListOf<BuildFormProperty>()

        val startParams = mutableMapOf<String, String>()
        val parsedCommitMsg = EmojiUtil.removeAllEmoji(event.commitMsg ?: "")

        // 通用参数
        startParams[CommonVariables.CI_PIPELINE_NAME] = yaml.name ?: ""
        startParams[CommonVariables.CI_BUILD_URL] = v2GitUrl ?: ""
        startParams[BK_CI_RUN] = "true"
        startParams[CommonVariables.CI_ACTOR] = if (event.objectKind == TGitObjectKind.SCHEDULE.value) {
            "system"
        } else {
            event.userId
        }
        startParams[CommonVariables.CI_BRANCH] = event.branch
        startParams[PIPELINE_GIT_COMMIT_MESSAGE] = parsedCommitMsg
        startParams[PIPELINE_GIT_SHA] = event.commitId
        if (event.commitId.isNotBlank() && event.commitId.length >= 8) {
            startParams[PIPELINE_GIT_SHA_SHORT] = event.commitId.substring(0, 8)
        }

        // 模板替换关键字
        if (yamlTransferData != null) {
            startParams[CommonVariables.TEMPLATE_ACROSS_INFO_ID] = yamlTransferData.templateData.templateId
        }

        // 替换BuildMessage为了展示commit信息
        startParams[PIPELINE_BUILD_MSG] = parsedCommitMsg

        when (originEvent) {
            is GitEvent -> {
                startParams.putAll(webhookParams)
            }
            else -> {
                startParams[PIPELINE_GIT_EVENT] = if (event.objectKind == TGitObjectKind.SCHEDULE.value) {
                    startParams[PIPELINE_GIT_COMMIT_AUTHOR] = event.commitAuthorName ?: ""
                    TGitObjectKind.SCHEDULE.value
                } else {
                    startParams[PIPELINE_GIT_COMMIT_AUTHOR] = event.userId
                    TGitObjectKind.MANUAL.value
                }
                startParams[PIPELINE_GIT_REPO_URL] = streamGitProjectInfo.gitHttpUrl
                val gitProjectName = GitUtils.getProjectName(streamGitProjectInfo.gitHttpUrl)
                startParams[PIPELINE_GIT_REPO] = gitProjectName
                val (group, name) = GitUtils.getRepoGroupAndName(gitProjectName)
                startParams[PIPELINE_GIT_REPO_NAME] = name
                startParams[PIPELINE_GIT_REPO_GROUP] = group
            }
        }

        // 用户自定义变量
        val buildFormProperties = if (originEvent is GitPushEvent) {
            getBuildFormPropertyFromYmlVariable(
                // 根据 push options 参数改变variables的值
                variables = replaceVariablesByPushOptions(yaml.variables, originEvent.push_options),
                startParams = startParams
            )
        } else {
            getBuildFormPropertyFromYmlVariable(yaml.variables, startParams)
        }

        startParams.forEach {
            result.add(
                BuildFormProperty(
                    id = it.key,
                    required = false,
                    type = BuildFormPropertyType.STRING,
                    defaultValue = it.value,
                    options = null,
                    desc = null,
                    repoHashId = null,
                    relativePath = null,
                    scmType = null,
                    containerType = null,
                    glob = null,
                    properties = null
                )
            )
        }
        result.addAll(buildFormProperties)

        return result
    }

    private fun getBuildFormPropertyFromYmlVariable(
        variables: Map<String, Variable>?,
        startParams: MutableMap<String, String>
    ): List<BuildFormProperty> {
        if (variables.isNullOrEmpty()) {
            return emptyList()
        }
        val buildFormProperties = mutableListOf<BuildFormProperty>()
        variables.forEach { (key, variable) ->
            buildFormProperties.add(
                BuildFormProperty(
                    id = StreamYamlBuild.VARIABLE_PREFIX + key,
                    required = false,
                    type = BuildFormPropertyType.STRING,
                    defaultValue = ModelCommon.formatVariablesValue(variable.value, startParams) ?: "",
                    options = null,
                    desc = null,
                    repoHashId = null,
                    relativePath = null,
                    scmType = null,
                    containerType = null,
                    glob = null,
                    properties = null,
                    readOnly = variable.readonly
                )
            )
        }
        return buildFormProperties
    }

    // git push -o ci.variable::<name>="<value>" -o ci.variable::<name>="<value>"
    private fun replaceVariablesByPushOptions(
        variables: Map<String, Variable>?,
        pushOptions: Map<String, String>?
    ): Map<String, Variable>? {
        if (variables.isNullOrEmpty() || pushOptions.isNullOrEmpty()) {
            return variables
        }
        val variablesOptionsKeys = pushOptions.keys.filter { it.startsWith(PUSH_OPTIONS_PREFIX) }
            .map { it.removePrefix(PUSH_OPTIONS_PREFIX) }

        val result = variables.toMutableMap()
        variables.forEach { (key, value) ->
            // 不替换只读变量
            if (value.readonly != null && value.readonly == true) {
                return@forEach
            }
            if (key in variablesOptionsKeys) {
                result[key] = Variable(
                    value = pushOptions["$PUSH_OPTIONS_PREFIX$key"],
                    readonly = value.readonly
                )
            }
        }
        return result
    }
}

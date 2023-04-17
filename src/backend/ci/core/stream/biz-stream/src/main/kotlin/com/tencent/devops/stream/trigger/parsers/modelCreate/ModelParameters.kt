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

import com.tencent.devops.common.api.util.EmojiUtil
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_COMMIT_AUTHOR
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_COMMIT_MESSAGE
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_CREATE_TIME
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_CREATOR
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_GROUP
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_NAME
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_SHA
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_SHA_SHORT
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_YAML_PATH
import com.tencent.devops.common.webhook.pojo.code.BK_CI_RUN
import com.tencent.devops.process.utils.PIPELINE_BUILD_MSG
import com.tencent.devops.process.utils.PIPELINE_START_MANUAL_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_PIPELINE_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_SERVICE_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_TIME_TRIGGER_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_WEBHOOK_USER_ID
import com.tencent.devops.process.yaml.modelCreate.ModelCommon
import com.tencent.devops.process.yaml.v2.enums.StreamObjectKind
import com.tencent.devops.process.yaml.v2.models.ScriptBuildYaml
import com.tencent.devops.process.yaml.v2.models.Variable
import com.tencent.devops.process.yaml.v2.models.YamlTransferData
import com.tencent.devops.scm.utils.code.git.GitUtils
import com.tencent.devops.stream.common.CommonVariables
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.pojo.ModelParametersData
import com.tencent.devops.stream.trigger.pojo.StreamGitProjectCache

@Suppress("ComplexMethod")
object ModelParameters {
    const val VARIABLE_PREFIX = "variables."
    const val CIDir = ".ci/"

    fun createPipelineParams(
        action: BaseAction,
        yaml: ScriptBuildYaml,
        streamGitProjectInfo: StreamGitProjectCache,
        webhookParams: Map<String, String> = mapOf(),
        yamlTransferData: YamlTransferData? = null
    ): ModelParametersData {
        val event = action.data.eventCommon
        val startParams = mutableMapOf<String, String>()
        val parsedCommitMsg = EmojiUtil.removeAllEmoji(event.commit.commitMsg ?: "")

        // 通用参数
        startParams[CommonVariables.CI_PIPELINE_NAME] = yaml.name ?: ""
        startParams[PIPELINE_GIT_YAML_PATH] = action.data.context.pipeline?.filePath?.removePrefix(CIDir) ?: ""
        startParams[PIPELINE_GIT_REPO_CREATE_TIME] = action.data.context.repoCreatedTime ?: ""
        startParams[PIPELINE_GIT_REPO_CREATOR] = action.data.context.repoCreatorId ?: ""
        startParams[BK_CI_RUN] = "true"
        // 增加触发人上下文
        when (action.getStartType()) {
            StartType.PIPELINE -> startParams[PIPELINE_START_PIPELINE_USER_ID] = action.data.eventCommon.userId
            StartType.WEB_HOOK -> startParams[PIPELINE_START_WEBHOOK_USER_ID] = action.data.eventCommon.userId
            StartType.SERVICE -> startParams[PIPELINE_START_SERVICE_USER_ID] = action.data.eventCommon.userId
            StartType.MANUAL -> startParams[PIPELINE_START_MANUAL_USER_ID] = action.data.eventCommon.userId
            StartType.TIME_TRIGGER -> startParams[PIPELINE_START_TIME_TRIGGER_USER_ID] =
                action.data.context.pipeline?.lastModifier ?: ""
        }

        startParams[CommonVariables.CI_BRANCH] = event.branch
        startParams[PIPELINE_GIT_COMMIT_MESSAGE] = parsedCommitMsg
        startParams[PIPELINE_GIT_SHA] = event.commit.commitId
        if (event.commit.commitId.isNotBlank() && event.commit.commitId.length >= 8) {
            startParams[PIPELINE_GIT_SHA_SHORT] = event.commit.commitId.substring(0, 8)
        }

        // 模板替换关键字
        if (yamlTransferData != null) {
            startParams[CommonVariables.TEMPLATE_ACROSS_INFO_ID] = yamlTransferData.templateData.templateId
        }

        // 替换BuildMessage为了展示commit信息
        startParams[PIPELINE_BUILD_MSG] = parsedCommitMsg

        // git事件触发的action直接使用webhook参数即可
        if (action.needAddWebhookParams()) {
            startParams.putAll(webhookParams)
        } else {
            // stream独有事件的单独判断
            startParams[PIPELINE_GIT_EVENT] = if (action.metaData.streamObjectKind == StreamObjectKind.SCHEDULE) {
                startParams[PIPELINE_GIT_COMMIT_AUTHOR] = event.commit.commitAuthorName ?: ""
                StreamObjectKind.SCHEDULE.value
            } else {
                startParams[PIPELINE_GIT_COMMIT_AUTHOR] = event.userId
                StreamObjectKind.MANUAL.value
            }
            startParams[PIPELINE_GIT_REPO_URL] = streamGitProjectInfo.gitHttpUrl
            val gitProjectName = GitUtils.getProjectName(streamGitProjectInfo.gitHttpUrl)
            startParams[PIPELINE_GIT_REPO] = gitProjectName
            val (group, name) = GitUtils.getRepoGroupAndName(gitProjectName)
            startParams[PIPELINE_GIT_REPO_NAME] = name
            startParams[PIPELINE_GIT_REPO_GROUP] = group
        }

        // 用户自定义变量
        val buildFormProperties = getBuildFormPropertyFromYmlVariable(
            variables = action.getUserVariables(yaml.variables) ?: yaml.variables,
            startParams = startParams
        )

        return ModelParametersData(buildFormProperties, startParams)
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
            val keyWithPrefix = if (key.startsWith(VARIABLE_PREFIX)) {
                key
            } else {
                VARIABLE_PREFIX + key
            }
            buildFormProperties.add(
                BuildFormProperty(
                    id = keyWithPrefix,
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
}

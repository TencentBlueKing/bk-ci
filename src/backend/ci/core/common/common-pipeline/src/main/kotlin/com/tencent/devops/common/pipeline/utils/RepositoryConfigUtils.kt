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

package com.tencent.devops.common.pipeline.utils

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitlabElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeSvnElement
import com.tencent.devops.common.pipeline.pojo.element.agent.GithubElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitlabWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeP4WebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeScmGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeScmSvnWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType

object RepositoryConfigUtils {

    @Suppress("ALL")
    fun buildConfig(element: Element): RepositoryConfig {
        return when (element) {
            is CodeGitElement -> RepositoryConfig(
                repositoryHashId = element.repositoryHashId,
                repositoryName = element.repositoryName,
                repositoryType = element.repositoryType ?: RepositoryType.ID
            )
            is CodeGitWebHookTriggerElement -> RepositoryConfig(
                repositoryHashId = element.repositoryHashId,
                repositoryName = element.repositoryName,
                triggerRepositoryType = element.repositoryType,
                selfRepoHashId = null
            )
            is CodeSvnElement -> RepositoryConfig(
                repositoryHashId = element.repositoryHashId,
                repositoryName = element.repositoryName,
                repositoryType = element.repositoryType ?: RepositoryType.ID
            )
            is CodeSVNWebHookTriggerElement -> RepositoryConfig(
                repositoryHashId = element.repositoryHashId,
                repositoryName = element.repositoryName,
                triggerRepositoryType = element.repositoryType,
                selfRepoHashId = null
            )
            is CodeGitlabElement -> RepositoryConfig(
                repositoryHashId = element.repositoryHashId,
                repositoryName = element.repositoryName,
                repositoryType = element.repositoryType ?: RepositoryType.ID
            )
            is CodeGitlabWebHookTriggerElement -> RepositoryConfig(
                repositoryHashId = element.repositoryHashId,
                repositoryName = element.repositoryName,
                triggerRepositoryType = element.repositoryType,
                selfRepoHashId = null
            )
            is GithubElement -> RepositoryConfig(
                repositoryHashId = element.repositoryHashId,
                repositoryName = element.repositoryName,
                repositoryType = element.repositoryType ?: RepositoryType.ID
            )
            is CodeGithubWebHookTriggerElement -> RepositoryConfig(
                repositoryHashId = element.repositoryHashId,
                repositoryName = element.repositoryName,
                triggerRepositoryType = element.repositoryType,
                selfRepoHashId = null
            )
            is CodeTGitWebHookTriggerElement -> RepositoryConfig(
                repositoryHashId = element.data.input.repositoryHashId,
                repositoryName = element.data.input.repositoryName,
                triggerRepositoryType = element.data.input.repositoryType,
                selfRepoHashId = null
            )
            is CodeP4WebHookTriggerElement -> RepositoryConfig(
                repositoryHashId = element.data.input.repositoryHashId,
                repositoryName = element.data.input.repositoryName,
                triggerRepositoryType = element.data.input.repositoryType,
                selfRepoHashId = null
            )
            else -> throw InvalidParamException("Unknown code element -> $element")
        }
    }

    /**
     * 如果代码库是别名，就需要做一次环境变量替换
     */
    fun replaceCodeProp(repositoryConfig: RepositoryConfig, variables: Map<String, String>): RepositoryConfig {
        if (repositoryConfig.repositoryType == RepositoryType.NAME) {
            if (!repositoryConfig.repositoryName.isNullOrBlank()) {
                return RepositoryConfig(
                    repositoryHashId = repositoryConfig.repositoryHashId,
                    repositoryName = EnvUtils.parseEnv(repositoryConfig.repositoryName!!, variables),
                    repositoryType = repositoryConfig.repositoryType
                )
            }
        }
        return repositoryConfig
    }

    fun buildConfig(repositoryId: String, repositoryType: RepositoryType?) =
        if (repositoryType == null || repositoryType == RepositoryType.ID) {
            RepositoryConfig(
                repositoryHashId = repositoryId,
                repositoryName = null,
                repositoryType = RepositoryType.ID
            )
        } else {
            RepositoryConfig(
                repositoryHashId = null,
                repositoryName = repositoryId,
                repositoryType = RepositoryType.NAME
            )
        }

    @Suppress("CyclomaticComplexMethod", "ComplexMethod")
    fun buildWebhookConfig(
        element: Element,
        variables: Map<String, String>
    ): Triple<ScmType, CodeEventType?, RepositoryConfig> {
        return when (element) {
            is CodeGitWebHookTriggerElement -> {
                val repositoryConfig = RepositoryConfig(
                    repositoryHashId = element.repositoryHashId,
                    repositoryName = EnvUtils.parseEnv(element.repositoryName, variables),
                    triggerRepositoryType = element.repositoryType,
                    selfRepoHashId = variables[PIPELINE_PAC_REPO_HASH_ID]
                )
                val eventType = if (element.eventType == CodeEventType.MERGE_REQUEST_ACCEPT) {
                    CodeEventType.MERGE_REQUEST
                } else {
                    element.eventType
                }
                Triple(ScmType.CODE_GIT, eventType, repositoryConfig)
            }

            is CodeSVNWebHookTriggerElement -> {
                val repositoryConfig = RepositoryConfig(
                    repositoryHashId = element.repositoryHashId,
                    repositoryName = EnvUtils.parseEnv(element.repositoryName, variables),
                    triggerRepositoryType = element.repositoryType,
                    selfRepoHashId = variables[PIPELINE_PAC_REPO_HASH_ID]
                )
                Triple(ScmType.CODE_SVN, CodeEventType.POST_COMMIT, repositoryConfig)
            }

            is CodeGitlabWebHookTriggerElement -> {
                val repositoryConfig = RepositoryConfig(
                    repositoryHashId = element.repositoryHashId,
                    repositoryName = EnvUtils.parseEnv(element.repositoryName, variables),
                    triggerRepositoryType = element.repositoryType,
                    selfRepoHashId = variables[PIPELINE_PAC_REPO_HASH_ID]
                )
                val eventType = if (element.eventType == CodeEventType.MERGE_REQUEST_ACCEPT) {
                    CodeEventType.MERGE_REQUEST
                } else {
                    element.eventType
                }
                Triple(ScmType.CODE_GITLAB, eventType, repositoryConfig)
            }

            is CodeGithubWebHookTriggerElement -> {
                val repositoryConfig = RepositoryConfig(
                    repositoryHashId = element.repositoryHashId,
                    repositoryName = EnvUtils.parseEnv(element.repositoryName, variables),
                    triggerRepositoryType = element.repositoryType,
                    selfRepoHashId = variables[PIPELINE_PAC_REPO_HASH_ID]
                )
                Triple(ScmType.GITHUB, element.eventType, repositoryConfig)
            }

            is CodeTGitWebHookTriggerElement -> {
                val repositoryConfig = RepositoryConfig(
                    repositoryHashId = element.data.input.repositoryHashId,
                    repositoryName = EnvUtils.parseEnv(element.data.input.repositoryName, variables),
                    triggerRepositoryType = element.data.input.repositoryType,
                    selfRepoHashId = variables[PIPELINE_PAC_REPO_HASH_ID]
                )
                val eventType = if (element.data.input.eventType == CodeEventType.MERGE_REQUEST_ACCEPT) {
                    CodeEventType.MERGE_REQUEST
                } else {
                    element.data.input.eventType
                }
                Triple(ScmType.CODE_TGIT, eventType, repositoryConfig)
            }

            is CodeP4WebHookTriggerElement -> {
                val repositoryConfig = RepositoryConfig(
                    repositoryHashId = element.data.input.repositoryHashId,
                    repositoryName = EnvUtils.parseEnv(element.data.input.repositoryName, variables),
                    triggerRepositoryType = element.data.input.repositoryType,
                    selfRepoHashId = variables[PIPELINE_PAC_REPO_HASH_ID]
                )
                Triple(ScmType.CODE_P4, element.data.input.eventType, repositoryConfig)
            }

            is CodeScmGitWebHookTriggerElement -> {
                val repositoryConfig = RepositoryConfig(
                    repositoryHashId = element.data.input.repositoryHashId,
                    repositoryName = EnvUtils.parseEnv(element.data.input.repositoryName, variables),
                    triggerRepositoryType = element.data.input.repositoryType,
                    selfRepoHashId = null
                )
                Triple(ScmType.SCM_GIT, element.data.input.eventType, repositoryConfig)
            }

            is CodeScmSvnWebHookTriggerElement -> {
                val repositoryConfig = RepositoryConfig(
                    repositoryHashId = element.data.input.repositoryHashId,
                    repositoryName = EnvUtils.parseEnv(element.data.input.repositoryName, variables),
                    triggerRepositoryType = element.data.input.repositoryType,
                    selfRepoHashId = null
                )
                Triple(ScmType.SCM_SVN, element.data.input.eventType, repositoryConfig)
            }

            else ->
                throw InvalidParamException("Unknown code element -> $element")
        }
    }

    fun getRepositoryConfig(
        repoHashId: String?,
        repoName: String?,
        repoType: RepositoryType?,
        variables: Map<String, String>? = null
    ): RepositoryConfig {
        return when (repoType) {
            RepositoryType.ID -> RepositoryConfig(repoHashId, null, RepositoryType.ID)
            RepositoryType.NAME -> {
                val repositoryName = if (variables.isNullOrEmpty()) {
                    repoName!!
                } else {
                    EnvUtils.parseEnv(repoName!!, variables)
                }
                RepositoryConfig(null, repositoryName, RepositoryType.NAME)
            }
            else -> {
                if (!repoHashId.isNullOrBlank()) {
                    RepositoryConfig(repoHashId, null, RepositoryType.ID)
                } else if (!repoName.isNullOrBlank()) {
                    val repositoryName = if (variables.isNullOrEmpty()) {
                        repoName
                    } else {
                        EnvUtils.parseEnv(repoName, variables)
                    }
                    RepositoryConfig(null, repositoryName, RepositoryType.NAME)
                } else {
                    // 两者不能同时为空
                    throw IllegalArgumentException("repoName and Id cannot both be empty")
                }
            }
        }
    }
}

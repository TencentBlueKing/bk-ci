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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
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
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitlabElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeSvnElement
import com.tencent.devops.common.pipeline.pojo.element.agent.GithubElement

/**
 * deng
 * 2019-03-01
 */
object RepositoryConfigUtils {

    fun buildConfig(element: Element): RepositoryConfig {
        return when (element) {
            is CodeGitElement -> RepositoryConfig(
                element.repositoryHashId,
                element.repositoryName,
                element.repositoryType ?: RepositoryType.ID
            )
            is CodeSvnElement -> RepositoryConfig(
                element.repositoryHashId,
                element.repositoryName,
                element.repositoryType ?: RepositoryType.ID
            )
            is CodeGitlabElement -> RepositoryConfig(
                element.repositoryHashId,
                element.repositoryName,
                element.repositoryType ?: RepositoryType.ID
            )
            is GithubElement -> RepositoryConfig(
                element.repositoryHashId,
                element.repositoryName,
                element.repositoryType ?: RepositoryType.ID
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
}
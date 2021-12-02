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

package com.tencent.devops.repository.pojo

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.annotations.ApiModel

@ApiModel("代码库模型-多态基类")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@JsonSubTypes(
    JsonSubTypes.Type(value = CodeSvnRepository::class, name = CodeSvnRepository.classType),
    JsonSubTypes.Type(value = CodeGitRepository::class, name = CodeGitRepository.classType),
    JsonSubTypes.Type(value = CodeGitlabRepository::class, name = CodeGitlabRepository.classType),
    JsonSubTypes.Type(value = GithubRepository::class, name = GithubRepository.classType),
    JsonSubTypes.Type(value = CodeTGitRepository::class, name = CodeTGitRepository.classType),
    JsonSubTypes.Type(value = CodeP4Repository::class, name = CodeP4Repository.classType)
)
interface Repository {
    val aliasName: String
    val url: String
    val credentialId: String
    val projectName: String
    var userName: String
    val projectId: String?
    val repoHashId: String?

    fun isLegal() = url.startsWith(getStartPrefix())

    fun getStartPrefix(): String

    fun getFormatURL() = url
}

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

package com.tencent.devops.stream.trigger.git.pojo

import com.tencent.devops.stream.pojo.StreamGitProjectInfoWithProject

/**
 * Stream需要的各git平台的项目信息
 */
interface StreamGitProjectInfo {
    // 项目唯一标识
    val gitProjectId: String

    // 默认分支
    val defaultBranch: String?

    // 项目的http/https的git链接，例 https://github.com/Tencent/bk-ci.git
    val gitHttpUrl: String

    // 项目名称，例 bk-ci
    val name: String

    // git ssh链接
    val gitSshUrl: String?

    // git主页链接
    val homepage: String?

    // git https链接
    val gitHttpsUrl: String?

    // git 仓库描述
    val description: String?

    // git 仓库图片地址
    val avatarUrl: String?

    // 地址全称  xxx/xx
    val pathWithNamespace: String?

    // 名称全称
    val nameWithNamespace: String

    // 触发仓库创建时间字符串 如:2017-08-13T07:37:14+0000
    val repoCreatedTime: String

    // 触发仓库创建人id， 工蜂侧是数字 id 需要使用时转换为 name
    val repoCreatorId: String
}

fun StreamGitProjectInfo.toStreamGitProjectInfoWithProject() = StreamGitProjectInfoWithProject(
    gitProjectId = this.gitProjectId.toLong(),
    defaultBranch = this.defaultBranch,
    gitHttpUrl = this.gitHttpUrl,
    name = this.name,
    gitSshUrl = this.gitSshUrl,
    homepage = this.homepage,
    gitHttpsUrl = this.gitHttpsUrl,
    description = this.description,
    avatarUrl = this.avatarUrl,
    pathWithNamespace = this.pathWithNamespace,
    nameWithNamespace = this.nameWithNamespace,
    routerTag = null
)

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

package com.tencent.devops.stream.pojo

import com.tencent.devops.repository.pojo.git.GitCodeProjectInfo
import com.tencent.devops.repository.sdk.github.pojo.GithubRepo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "Git拿到的项目信息")
data class StreamProjectGitInfo(
    @get:Schema(title = "Git项目ID")
    val id: Long,
    @get:Schema(title = "是否为stream 公共项目")
    val public: Boolean?,
    @get:Schema(title = "stream 项目名称")
    val name: String?,
    @get:Schema(title = "stream 项目名称带有路径")
    val pathWithNamespace: String?,
    @get:Schema(title = "https-git链接")
    val httpsUrlToRepo: String?,
    @get:Schema(title = "项目网页链接")
    val webUrl: String?,
    @get:Schema(title = "项目头像")
    val avatarUrl: String?,
    @get:Schema(title = "项目描述")
    val description: String?
) {
    constructor(p: GitCodeProjectInfo) : this(
        id = p.id!!,
        public = p.public,
        name = p.name,
        pathWithNamespace = p.pathWithNamespace,
        httpsUrlToRepo = p.httpsUrlToRepo,
        webUrl = p.webUrl,
        avatarUrl = p.avatarUrl,
        description = p.description
    )

    constructor(p: GithubRepo) : this(
        id = p.id,
        public = !p.private,
        name = p.name,
        pathWithNamespace = p.fullName,
        httpsUrlToRepo = p.cloneUrl,
        webUrl = p.htmlUrl,
        avatarUrl = p.owner.avatarUrl,
        description = p.description
    )

    constructor(streamProjectSimpleInfo: StreamProjectSimpleInfo) : this(
        id = streamProjectSimpleInfo.id,
        public = streamProjectSimpleInfo.public,
        name = streamProjectSimpleInfo.name,
        pathWithNamespace = streamProjectSimpleInfo.pathWithNamespace,
        httpsUrlToRepo = streamProjectSimpleInfo.httpsUrlToRepo,
        webUrl = streamProjectSimpleInfo.webUrl,
        avatarUrl = streamProjectSimpleInfo.avatarUrl,
        description = streamProjectSimpleInfo.description
    )
}

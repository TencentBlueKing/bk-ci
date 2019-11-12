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

package com.tencent.devops.common.pipeline.pojo.coverity

import com.tencent.devops.common.api.enums.RepositoryConfig

/**
 * deng
 * 26/01/2018
 */
data class CoverityConfig(
    val name: String,
    val cnName: String,
    val projectType: CoverityProjectType,
    val tools: List<String>,
    var asynchronous: Boolean, // 是否同步，默认是同步
    val filterTools: List<String>,
    val repos: List<RepoItem>,
    val scanCodePath: String,
    val scmType: String,
    val certType: String,
    val timeOut: Long = 4 * 3600 // 4小时
) {
    data class RepoItem(
        val repositoryConfig: RepositoryConfig,
        val type: String,
        val relPath: String = "", // 代码路径
        val relativePath: String = "", // 代码相对路径
        var url: String = "",
        var authType: String = "",
        var repoHashId: String = ""
    )
}

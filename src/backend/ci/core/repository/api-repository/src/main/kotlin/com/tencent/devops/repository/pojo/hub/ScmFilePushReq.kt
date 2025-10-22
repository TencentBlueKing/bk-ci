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

package com.tencent.devops.repository.pojo.hub

import com.tencent.devops.repository.pojo.credential.AuthRepository
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "推送文件请求")
data class ScmFilePushReq(
    @get:Schema(title = "文件路径", required = true)
    val path: String,
    @get:Schema(title = "分支", required = true)
    val ref: String,
    @get:Schema(title = "默认分支,如果ref分支不存在,则从默认分支创建", required = true)
    val defaultBranch: String,
    @get:Schema(title = "内容", required = true)
    val content: String,
    @get:Schema(title = "提交信息", required = true)
    val message: String,
    @get:Schema(title = "授权代码库, authRepository和repositoryConfig必须传一个", required = true)
    val authRepository: AuthRepository
)

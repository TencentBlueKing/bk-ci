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

package com.tencent.devops.process.pojo

import com.tencent.devops.common.api.enums.ScmType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 构建历史中的源材料
 */
@Schema(title = "")
data class PipelineBuildMaterial(
    @get:Schema(title = "代码库类型", required = false)
    val scmType: String? = ScmType.CODE_TGIT.name,
    @get:Schema(title = "别名", required = false)
    val aliasName: String?,
    @get:Schema(title = "url 地址", required = false)
    val url: String,
    @get:Schema(title = "分支名称", required = false)
    val branchName: String?,
    @get:Schema(title = "当前最新提交id", required = false)
    val newCommitId: String?,
    @get:Schema(title = "当前提交备注信息", required = false)
    val newCommitComment: String?,
    @get:Schema(title = "提交次数", required = false)
    val commitTimes: Int?,
    @get:Schema(title = "是否为源材料主仓库", required = false)
    val mainRepo: Boolean? = false,
    @get:Schema(title = "提交时间", required = false)
    val createTime: Long? = System.currentTimeMillis() / 1000,
    @get:Schema(title = "插件ID", required = false)
    val taskId: String? = null
)

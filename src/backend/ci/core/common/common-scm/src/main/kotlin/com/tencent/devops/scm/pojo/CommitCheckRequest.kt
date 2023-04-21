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

package com.tencent.devops.scm.pojo

import com.tencent.devops.common.api.annotation.SkipLogField
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.scm.enums.CodeSvnRegion
import io.swagger.annotations.ApiParam

data class CommitCheckRequest(
    @ApiParam("项目名称", required = true)
    val projectName: String,
    @ApiParam("仓库地址", required = true)
    val url: String,
    @ApiParam("仓库类型", required = true)
    val type: ScmType,
    @ApiParam("privateKey", required = true)
    @SkipLogField
    val privateKey: String?,
    @ApiParam("passPhrase", required = false)
    @SkipLogField
    val passPhrase: String?,
    @ApiParam("token", required = true)
    @SkipLogField
    val token: String?,
    @ApiParam("仓库区域前缀（只有svn用到）", required = false)
    val region: CodeSvnRegion?,
    @ApiParam("CommitId", required = false)
    val commitId: String,
    @ApiParam("状态", required = true)
    val state: String,
    @ApiParam("详情链接", required = true)
    val targetUrl: String,
    @ApiParam("区分标志", required = true)
    val context: String,
    @ApiParam("详情链接", required = true)
    val description: String,
    @ApiParam("是否锁mr", required = true)
    val block: Boolean,
    @ApiParam("mr对应的requestId", required = true)
    val mrRequestId: Long?,
    @ApiParam("报表数据", required = true)
    val reportData: Pair<List<String>, MutableMap<String, MutableList<List<String>>>>,
    @ApiParam("检查结果关联的MR", required = true)
    val targetBranch: List<String>? = null
)

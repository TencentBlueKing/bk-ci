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

package com.tencent.devops.stream.v1.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "蓝盾工蜂项目配置")
data class V1GitRepositoryConf(
    @get:Schema(title = "工蜂项目ID")
    val gitProjectId: Long,
    @get:Schema(title = "工蜂项目名")
    val name: String,
    @get:Schema(title = "工蜂项目url")
    val url: String,
    @get:Schema(title = "homepage")
    val homepage: String,
    @get:Schema(title = "gitHttpUrl")
    val gitHttpUrl: String,
    @get:Schema(title = "gitSshUrl")
    val gitSshUrl: String,
    @get:Schema(title = "是否启用CI")
    val enableCi: Boolean,
    @get:Schema(title = "Build pushed branches")
    val buildPushedBranches: Boolean = true,
    @get:Schema(title = "Limit concurrent jobs")
    val limitConcurrentJobs: Int?,
    @get:Schema(title = "Build pushed pull request")
    val buildPushedPullRequest: Boolean = true,
    @get:Schema(title = "Auto cancel branch builds")
    val autoCancelBranchBuilds: Boolean = false,
    @get:Schema(title = "Auto cancel pull request builds")
    val autoCancelPullRequestBuilds: Boolean = false,
    @get:Schema(title = "Environment variable")
    val env: List<V1EnvironmentVariables>?,
    @get:Schema(title = "创建时间")
    val createTime: Long?,
    @get:Schema(title = "修改时间")
    val updateTime: Long?,
    @get:Schema(title = "蓝盾项目Code")
    val projectCode: String?,
    @get:Schema(title = "企业微信客服通知")
    val rtxCustomProperty: V1RtxCustomProperty?,
    @get:Schema(title = "企业微信群通知")
    val rtxGroupProperty: V1RtxGroupProperty?,
    @get:Schema(title = "邮件通知")
    val emailProperty: V1EmailProperty?,
    @get:Schema(title = "是否只在失败时通知")
    val onlyFailedNotify: Boolean? = true,
    @get:Schema(title = "是否开启Mr锁定")
    val enableMrBlock: Boolean? = true
)

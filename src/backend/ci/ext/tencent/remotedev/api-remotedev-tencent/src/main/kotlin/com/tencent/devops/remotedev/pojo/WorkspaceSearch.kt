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

package com.tencent.devops.remotedev.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "工作空间搜索模型,为什么是list，因为支持同时匹配多个目标")
data class WorkspaceSearch(
    @get:Schema(title = "工作空间名称")
    val workspaceName: List<String>? = null,
    @get:Schema(title = "工作空间备注名称")
    val displayName: List<String>? = null,
    @get:Schema(title = "操作系统类型")
    val workspaceSystemType: List<WorkspaceSystemType>? = null,
    @get:Schema(title = "工作空间状态")
    val status: List<WorkspaceStatus>? = null,
    @get:Schema(title = "区域简称，SZ,NJ")
    var zoneShortName: List<String>? = null,
    @get:Schema(title = "资源类型：M，L，XL，S")
    val size: List<String>? = null,
    @get:Schema(title = "工作空间对应的IP")
    val ips: List<String>? = null,
    @get:Schema(title = "云桌面对应的mac地址")
    val macAddress: List<String>? = null,
    @get:Schema(title = "拥有者_CN")
    val ownerCN: List<String>? = null,
    @get:Schema(title = "拥有者")
    val owner: List<String>? = null,
    @get:Schema(title = "查看者_CN")
    val viewersCN: List<String>? = null,
    @get:Schema(title = "查看者")
    var viewers: List<String>? = null,
    @get:Schema(title = "项目id")
    var projectId: List<String>? = null,
    @get:Schema(title = "协助工单，仅op有效")
    var expertSupId: List<Long>? = null,
    @get:Schema(title = "是否模糊匹配，可以关闭，查询会更快。")
    val onFuzzyMatch: Boolean = true
) {
    fun onlyNeedCheckWorkspace() = !needCheckDetail() &&
            owner.isNullOrEmpty() &&
            ownerCN.isNullOrEmpty() &&
            viewers.isNullOrEmpty() &&
            viewersCN.isNullOrEmpty() &&
            size.isNullOrEmpty() &&
            expertSupId.isNullOrEmpty() &&
            macAddress.isNullOrEmpty()

    fun needCheckDetail() = !ips.isNullOrEmpty() || !zoneShortName.isNullOrEmpty()
}

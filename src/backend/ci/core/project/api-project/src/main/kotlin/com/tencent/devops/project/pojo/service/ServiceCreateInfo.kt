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

package com.tencent.devops.project.pojo.service

import io.swagger.v3.oas.annotations.media.Schema

/**
 *   Date on 2018-12-05.
 */
@Schema(description = "服务-创建模型")
data class ServiceCreateInfo(
    @Schema(description = "服务名称，英文名，不传则从中文名中读取 流水线(Pipeline)")
    var englishName: String?,
    @Schema(description = "服务名称，中文名, 例如 流水线(Pipeline)", required = true)
    val name: String,
    @Schema(description = "服务类型ID，1:项目管理 2:开发 3:测试 4:部署 5:运营 6:安全 8:管理工具", required = true)
    val serviceTypeId: Long,
    @Schema(description = "是否展示项目列表")
    val showProjectList: Boolean = true,
    @Schema(description = "是否在服务导航条上显示")
    val showNav: Boolean = true,
    @Schema(description = "服务状态 ok=正常(可用) planning=规划中(灰色不可用) new=新上线(可用)")
    val status: String = "ok",

    @Schema(description = "链接1，例如 /pipeline/")
    val link: String?,
    @Schema(description = "链接2与链接1保持一样，例如 /pipeline/")
    val linkNew: String?,
    @Schema(description = "注入类型：amd/iframe")
    val injectType: String?,
    @Schema(description = "iframeUrl")
    val iframeUrl: String?,
    @Schema(description = "grayIframeUrl")
    val grayIframeUrl: String?,
    @Schema(description = "cssUrl")
    val cssUrl: String?,
    @Schema(description = "jsUrl")
    val jsUrl: String?,
    @Schema(description = "grayCssUrl")
    val grayCssUrl: String?,
    @Schema(description = "grayJsUrl")
    val grayJsUrl: String?,
    @Schema(description = "projectIdType")
    val projectIdType: String?,
    @Schema(description = "权重")
    val weight: Int,
    @Schema(description = "logo地址")
    val logoUrl: String?,
    @Schema(description = "支持webSocket的页面")
    val webSocket: String?,
    @Schema(description = "集群类型")
    val clusterType: String? = ""
)

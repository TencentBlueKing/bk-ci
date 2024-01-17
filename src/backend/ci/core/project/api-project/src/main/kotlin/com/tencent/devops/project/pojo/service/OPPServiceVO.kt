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

@Schema(title = "OP服务-显示模型")
data class OPPServiceVO(
    @Schema(title = "主键ID")
    val id: Long,
    @Schema(title = "服务名称", required = true)
    val name: String,
    @Schema(title = "英文名称", required = true)
    val englishName: String,
    @Schema(title = "服务类型ID", required = true)
    val serviceTypeId: Long,
    @Schema(title = "是否在页面显示")
    val showProjectList: Boolean = true,
    @Schema(title = "showNav")
    val showNav: Boolean = true,
    @Schema(title = "状态（是否默认显示灰色）")
    val status: String = "ok",
    @Schema(title = "链接1")
    val link: String?,
    @Schema(title = "链接2")
    val linkNew: String?,
    @Schema(title = "注入类型")
    val injectType: String?,
    @Schema(title = "iframeUrl")
    val iframeUrl: String?,
    @Schema(title = "grayIframeUrl")
    val grayIframeUrl: String?,
    @Schema(title = "cssUrl")
    val cssUrl: String?,
    @Schema(title = "jsUrl")
    val jsUrl: String?,
    @Schema(title = "grayCssUrl")
    val grayCssUrl: String?,
    @Schema(title = "grayJsUrl")
    val grayJsUrl: String?,
    @Schema(title = "projectIdType")
    val projectIdType: String?,
    @Schema(title = "logo地址")
    val logoUrl: String?,
    @Schema(title = "支持webSocket的页面")
    val webSocket: String?,
    @Schema(title = "权重")
    val weight: Int? = null,
    @Schema(title = "创建人")
    val createdUser: String,
    @Schema(title = "创建时间")
    val createdTime: String,
    @Schema(title = "修改人修改时间")
    val updatedUser: String,
    @Schema(title = "修改时间")
    val updatedTime: String,
    @Schema(title = "集群类型")
    val clusterType: String = ""
)

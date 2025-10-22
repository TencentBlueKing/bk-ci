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

package com.tencent.devops.project.pojo.service

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "OP服务-显示模型")
data class OPPServiceVO(
    @get:Schema(title = "主键ID")
    val id: Long,
    @get:Schema(title = "服务名称", required = true)
    val name: String,
    @get:Schema(title = "英文名称", required = true)
    val englishName: String,
    @get:Schema(title = "服务类型ID", required = true)
    val serviceTypeId: Long,
    @get:Schema(title = "是否在页面显示")
    val showProjectList: Boolean = true,
    @get:Schema(title = "showNav")
    val showNav: Boolean = true,
    @get:Schema(title = "状态（是否默认显示灰色）")
    val status: String = "ok",
    @get:Schema(title = "链接1")
    val link: String?,
    @get:Schema(title = "链接2")
    val linkNew: String?,
    @get:Schema(title = "注入类型")
    val injectType: String?,
    @get:Schema(title = "iframeUrl")
    val iframeUrl: String?,
    @get:Schema(title = "grayIframeUrl")
    val grayIframeUrl: String?,
    @get:Schema(title = "cssUrl")
    val cssUrl: String?,
    @get:Schema(title = "jsUrl")
    val jsUrl: String?,
    @get:Schema(title = "grayCssUrl")
    val grayCssUrl: String?,
    @get:Schema(title = "grayJsUrl")
    val grayJsUrl: String?,
    @get:Schema(title = "projectIdType")
    val projectIdType: String?,
    @get:Schema(title = "logo地址")
    val logoUrl: String?,
    @get:Schema(title = "支持webSocket的页面")
    val webSocket: String?,
    @get:Schema(title = "权重")
    val weight: Int? = null,
    @get:Schema(title = "创建人")
    val createdUser: String,
    @get:Schema(title = "创建时间")
    val createdTime: String,
    @get:Schema(title = "修改人修改时间")
    val updatedUser: String,
    @get:Schema(title = "修改时间")
    val updatedTime: String,
    @get:Schema(title = "集群类型")
    val clusterType: String = ""
)

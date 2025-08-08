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

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "服务-显示模型")
data class ServiceVO(
    @get:Schema(title = "主键ID")
    val id: Long,
    @get:Schema(title = "标识")
    val code: String,
    @get:Schema(title = "名称")
    val name: String,
    @get:Schema(title = "链接")
    val link: String,
    @get:Schema(title = "新链接", description = "link_new")
    @JsonProperty("link_new")
    val linkNew: String,
    @get:Schema(title = "状态")
    val status: String,
    @get:Schema(title = "注入类型", description = "inject_type")
    @JsonProperty("inject_type")
    val injectType: String,
    @get:Schema(title = "框架URL", description = "iframe_url")
    @JsonProperty("iframe_url")
    var iframeUrl: String,
    @get:Schema(title = "grayIframeUrl")
    var grayIframeUrl: String?,
    @get:Schema(title = "cssURL", description = "css_url")
    @JsonProperty("css_url")
    val cssUrl: String,
    @get:Schema(title = "jsURL", description = "js_url")
    @JsonProperty("js_url")
    val jsUrl: String,
    @get:Schema(title = "grayCssURL", description = "gray_css_url")
    @JsonProperty("gray_css_url")
    val grayCssUrl: String,
    @get:Schema(title = "grayJsURL", description = "gray_js_url")
    @JsonProperty("gray_js_url")
    val grayJsUrl: String,
    @get:Schema(title = "显示项目列表", description = "show_project_list")
    @JsonProperty("show_project_list")
    val showProjectList: Boolean,
    @get:Schema(title = "显示导航", description = "show_nav")
    @JsonProperty("show_nav")
    val showNav: Boolean,
    @get:Schema(title = "项目ID类型", description = "project_id_type")
    @JsonProperty("project_id_type")
    val projectIdType: String,
    @get:Schema(title = "是否收藏")
    val collected: Boolean,
    @get:Schema(title = "权重")
    val weigHt: Int,
    @get:Schema(title = "logo地址")
    val logoUrl: String?,
    @get:Schema(title = "支持webSocket的页面")
    val webSocket: String?,
    @get:Schema(title = "是否可见")
    val hidden: Boolean? = false,
    @get:Schema(title = "new_window")
    var newWindow: Boolean? = false,
    @get:Schema(title = "new_window_url")
    var newWindowUrl: String? = null,
    @get:Schema(title = "集群类型")
    val clusterType: String = "",
    @get:Schema(title = "文档链接")
    val docUrl: String = ""
)

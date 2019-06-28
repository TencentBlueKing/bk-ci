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

package com.tencent.devops.project.pojo.service

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("服务-显示模型")
data class ServiceVO(
    @ApiModelProperty("主键ID")
    val id: Long,
    @ApiModelProperty("名称")
    val name: String,
    @ApiModelProperty("链接")
    val link: String,
    @ApiModelProperty("新链接")
    @JsonProperty("link_new")
    val linkNew: String,
    @ApiModelProperty("状态")
    val status: String,
    @ApiModelProperty("注入类型")
    @JsonProperty("inject_type")
    val injectType: String,
    @ApiModelProperty("框架URL")
    @JsonProperty("iframe_url")
    val iframeUrl: String,
    @ApiModelProperty("cssURL")
    @JsonProperty("css_url")
    val cssUrl: String,
    @ApiModelProperty("jsURL")
    @JsonProperty("js_url")
    val jsUrl: String,
    @ApiModelProperty("grayCssURL")
    @JsonProperty("gray_css_url")
    val grayCssUrl: String,
    @ApiModelProperty("grayJsURL")
    @JsonProperty("gray_js_url")
    val grayJsUrl: String,
    @ApiModelProperty("显示项目列表")
    @JsonProperty("show_project_list")
    val showProjectList: Boolean,
    @ApiModelProperty("显示导航")
    @JsonProperty("show_nav")
    val showNav: Boolean,
    @ApiModelProperty("项目ID类型")
    @JsonProperty("project_id_type")
    val projectIdType: String,
    @ApiModelProperty("是否收藏")
    val collected: Boolean,
    @ApiModelProperty("权重")
    val weigHt: Int
)
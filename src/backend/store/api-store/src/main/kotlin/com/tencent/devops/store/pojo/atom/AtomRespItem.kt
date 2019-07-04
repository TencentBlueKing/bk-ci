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

package com.tencent.devops.store.pojo.atom

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线-插件信息")
data class AtomRespItem(
    @ApiModelProperty("插件名称", required = true)
    val name: String,
    @ApiModelProperty("插件代码", required = true)
    val atomCode: String,
    @ApiModelProperty("插件默认版本号", required = true)
    val defaultVersion: String,
    @ApiModelProperty("插件大类（插件市场发布的插件分为有marketBuild：构建环境和marketBuildLess：无构建环境）", required = true)
    val classType: String,
    @ApiModelProperty("服务范围", required = true)
    val serviceScope: List<String>,
    @ApiModelProperty("支持的操作系统", required = true)
    val os: List<String>,
    @ApiModelProperty("插件logo", required = false)
    val logoUrl: String?,
    @ApiModelProperty("插件图标", required = false)
    val icon: String?,
    @ApiModelProperty("所属分类编码", required = true)
    val classifyCode: String,
    @ApiModelProperty("所属分类名称", required = true)
    val classifyName: String,
    @ApiModelProperty("插件所属范畴，TRIGGER：触发器类插件 TASK：任务类插件", required = true)
    val category: String,
    @ApiModelProperty("插件简介", required = false)
    val summary: String?,
    @ApiModelProperty("插件说明文档链接", required = false)
    val docsLink: String?,
    @ApiModelProperty("插件类型，SELF_DEVELOPED：自研 THIRD_PARTY：第三方开发", required = true)
    val atomType: String,
    @ApiModelProperty(
        "插件状态，INIT：初始化|COMMITTING：提交中|BUILDING：构建中|BUILD_FAIL：构建失败|TESTING：测试中|AUDITING：审核中|AUDIT_REJECT：审核驳回|RELEASED：已发布|GROUNDING_SUSPENSION：上架中止|UNDERCARRIAGING：下架中|UNDERCARRIAGED：已下架",
        required = true
    )
    val atomStatus: String,
    @ApiModelProperty("插件描述", required = false)
    val description: String?,
    @ApiModelProperty("发布者")
    val publisher: String?,
    @ApiModelProperty("创建人", required = true)
    val creator: String,
    @ApiModelProperty("是否为默认插件（默认插件默认所有项目可见）true：默认插件 false：普通插件", required = true)
    val defaultFlag: Boolean,
    @ApiModelProperty("是否为最新版本插件 true：最新 false：非最新", required = true)
    val latestFlag: Boolean,
    @ApiModelProperty("前端渲染模板版本（1.0代表历史存量插件渲染模板版本）", required = true)
    val htmlTemplateVersion: String,
    @ApiModelProperty("无构建环境插件是否可以在有构建环境运行标识， TRUE：可以 FALSE：不可以", required = false)
    val buildLessRunFlag: Boolean?,
    @ApiModelProperty("权重（数值越大代表权重越高）")
    val weight: Int?
)
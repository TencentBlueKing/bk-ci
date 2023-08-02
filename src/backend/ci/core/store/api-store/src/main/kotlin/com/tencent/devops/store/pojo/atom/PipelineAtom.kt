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

package com.tencent.devops.store.pojo.atom

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.api.enums.I18nSourceEnum
import com.tencent.devops.store.pojo.common.Label
import com.tencent.devops.store.pojo.common.VersionInfo
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线-流水线插件信息")
data class PipelineAtom(
    @ApiModelProperty("插件ID", required = true)
    val id: String,
    @ApiModelProperty("插件名称", required = true)
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val name: String,
    @ApiModelProperty("插件代码", required = true)
    val atomCode: String,
    @ApiModelProperty("版本号", required = true)
    val version: String,
    @ApiModelProperty("插件大类（插件市场发布的插件分为有marketBuild：构建环境和marketBuildLess：无构建环境）", required = true)
    val classType: String,
    @ApiModelProperty("插件logo", required = false)
    val logoUrl: String?,
    @ApiModelProperty("插件图标", required = false)
    val icon: String?,
    @ApiModelProperty("插件简介", required = false)
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val summary: String?,
    @ApiModelProperty("服务范围", required = false)
    val serviceScope: List<String>?,
    @ApiModelProperty("适用Job类型，AGENT： 编译环境，AGENT_LESS：无编译环境", required = false)
    val jobType: String?,
    @ApiModelProperty("支持的操作系统", required = false)
    val os: List<String>?,
    @ApiModelProperty("所属插件分类Id", required = false)
    val classifyId: String?,
    @ApiModelProperty("所属插件分类代码", required = false)
    val classifyCode: String?,
    @ApiModelProperty("所属插件分类名称", required = false)
    val classifyName: String?,
    @ApiModelProperty("插件说明文档链接", required = false)
    val docsLink: String?,
    @ApiModelProperty("插件所属范畴，TRIGGER：触发器类插件 TASK：任务类插件", required = false)
    val category: String?,
    @ApiModelProperty("插件类型，SELF_DEVELOPED：自研 THIRD_PARTY：第三方开发", required = false)
    val atomType: String?,
    @ApiModelProperty("插件状态", required = true)
    val atomStatus: String,
    @ApiModelProperty("插件描述", required = false)
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val description: String?,
    @ApiModelProperty("版本信息列表", required = true)
    val versionList: List<VersionInfo>,
    @ApiModelProperty("插件标签列表", required = false)
    val atomLabelList: List<Label>?,
    @ApiModelProperty("创建人", required = true)
    val creator: String,
    @ApiModelProperty("是否为默认插件（默认插件默认所有项目可见）true：默认插件 false：普通插件", required = false)
    val defaultFlag: Boolean?,
    @ApiModelProperty("是否为最新版本插件 true：最新 false：非最新", required = false)
    val latestFlag: Boolean?,
    @ApiModelProperty("前端渲染模板版本（1.0代表历史存量插件渲染模板版本）", required = false)
    val htmlTemplateVersion: String?,
    @ApiModelProperty("无构建环境插件是否可以在有构建环境运行标识， TRUE：可以 FALSE：不可以", required = false)
    val buildLessRunFlag: Boolean?,
    @ApiModelProperty("权重（数值越大代表权重越高）")
    val weight: Int?,
    @ApiModelProperty("自定义扩展容器前端表单属性字段的Json串", required = false)
    val props: Map<String, Any>?,
    @ApiModelProperty("预留字段（设置规则等信息的json串）", required = false)
    val data: Map<String, Any>?,
    @ApiModelProperty("是否推荐标识 true：推荐，false：不推荐", required = false)
    val recommendFlag: Boolean? = null,
    @ApiModelProperty(value = "前端UI渲染方式", required = false)
    val frontendType: FrontendTypeEnum?,
    @ApiModelProperty("项目可视范围,PRIVATE:私有 LOGIN_PUBLIC:登录用户开源", required = false)
    val visibilityLevel: String? = null,
    @ApiModelProperty("插件创建时间", required = true)
    val createTime: Long,
    @ApiModelProperty("插件最后修改时间", required = true)
    val updateTime: Long
)

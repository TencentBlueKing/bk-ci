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
import com.tencent.devops.store.pojo.common.HonorInfo
import com.tencent.devops.store.pojo.common.Label
import com.tencent.devops.store.pojo.common.StoreDailyStatistic
import com.tencent.devops.store.pojo.common.index.StoreIndexInfo
import com.tencent.devops.store.pojo.common.StoreUserCommentInfo
import io.swagger.annotations.ApiModelProperty

data class AtomVersion(
    @ApiModelProperty("插件ID")
    val atomId: String,
    @ApiModelProperty("插件标识")
    val atomCode: String,
    @ApiModelProperty("插件名称")
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val name: String,
    @ApiModelProperty("logo地址")
    val logoUrl: String?,
    @ApiModelProperty("插件分类code")
    val classifyCode: String?,
    @ApiModelProperty("插件分类名称")
    val classifyName: String?,
    @ApiModelProperty("插件范畴")
    val category: String?,
    @ApiModelProperty("插件说明文档链接")
    val docsLink: String?,
    @ApiModelProperty("前端渲染模板版本（1.0代表历史存量插件渲染模板版本）")
    val htmlTemplateVersion: String,
    @ApiModelProperty("插件类型")
    val atomType: String?,
    @ApiModelProperty("适用Job类型")
    val jobType: String?,
    @ApiModelProperty("操作系统")
    val os: List<String>?,
    @ApiModelProperty("插件简介")
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val summary: String?,
    @ApiModelProperty("插件描述")
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val description: String?,
    @ApiModelProperty("版本号")
    val version: String?,
    @ApiModelProperty("插件状态", required = true)
    val atomStatus: String,
    @ApiModelProperty("发布类型")
    val releaseType: String?,
    @ApiModelProperty("版本日志")
    @BkFieldI18n(source = I18nSourceEnum.DB, keyPrefixName = "versionInfo")
    val versionContent: String?,
    @ApiModelProperty("开发语言")
    val language: String?,
    @ApiModelProperty("代码库链接")
    val codeSrc: String?,
    @ApiModelProperty("发布者")
    @BkFieldI18n(source = I18nSourceEnum.DB, keyPrefixName = "versionInfo")
    val publisher: String?,
    @ApiModelProperty("创建人")
    val creator: String,
    @ApiModelProperty("修改人")
    val modifier: String,
    @ApiModelProperty("创建时间")
    val createTime: String,
    @ApiModelProperty("修改时间")
    val updateTime: String,
    @ApiModelProperty("是否为默认插件（默认插件默认所有项目可见）true：默认插件 false：普通插件")
    val defaultFlag: Boolean?,
    @ApiModelProperty("是否可安装标识")
    val flag: Boolean?,
    @ApiModelProperty("插件代码库授权者")
    val repositoryAuthorizer: String?,
    @ApiModelProperty("插件的调试项目")
    val projectCode: String?,
    @ApiModelProperty("插件的初始化项目")
    val initProjectCode: String?,
    @ApiModelProperty("标签列表", required = false)
    val labelList: List<Label>?,
    @ApiModelProperty("用户评论信息")
    val userCommentInfo: StoreUserCommentInfo,
    @ApiModelProperty("项目可视范围,PRIVATE:私有 LOGIN_PUBLIC:登录用户开源")
    val visibilityLevel: String?,
    @ApiModelProperty("插件代码库不开源原因")
    val privateReason: String?,
    @ApiModelProperty("是否推荐标识 true：推荐，false：不推荐", required = false)
    val recommendFlag: Boolean? = null,
    @ApiModelProperty(value = "前端UI渲染方式", required = false)
    val frontendType: FrontendTypeEnum?,
    @ApiModelProperty("yaml可用标识 true：是，false：否")
    val yamlFlag: Boolean? = null,
    @ApiModelProperty("是否可编辑")
    val editFlag: Boolean? = null,
    @ApiModelProperty("每日统计信息列表")
    val dailyStatisticList: List<StoreDailyStatistic>? = null,
    @ApiModelProperty("荣誉信息", required = false)
    val honorInfos: List<HonorInfo>? = null,
    @ApiModelProperty("指标信息列表")
    val indexInfos: List<StoreIndexInfo>? = null
)

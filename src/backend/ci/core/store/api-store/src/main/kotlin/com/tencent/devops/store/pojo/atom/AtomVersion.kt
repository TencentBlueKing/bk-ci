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
import io.swagger.v3.oas.annotations.media.Schema

data class AtomVersion(
    @Schema(description = "插件ID")
    val atomId: String,
    @Schema(description = "插件标识")
    val atomCode: String,
    @Schema(description = "插件名称")
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val name: String,
    @Schema(description = "logo地址")
    val logoUrl: String?,
    @Schema(description = "插件分类code")
    val classifyCode: String?,
    @Schema(description = "插件分类名称")
    val classifyName: String?,
    @Schema(description = "插件范畴")
    val category: String?,
    @Schema(description = "插件说明文档链接")
    val docsLink: String?,
    @Schema(description = "前端渲染模板版本（1.0代表历史存量插件渲染模板版本）")
    val htmlTemplateVersion: String,
    @Schema(description = "插件类型")
    val atomType: String?,
    @Schema(description = "适用Job类型")
    val jobType: String?,
    @Schema(description = "操作系统")
    val os: List<String>?,
    @Schema(description = "插件简介")
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val summary: String?,
    @Schema(description = "插件描述")
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val description: String?,
    @Schema(description = "版本号")
    val version: String?,
    @Schema(description = "插件状态", required = true)
    val atomStatus: String,
    @Schema(description = "发布类型")
    val releaseType: String?,
    @Schema(description = "版本日志")
    @BkFieldI18n(source = I18nSourceEnum.DB, keyPrefixName = "versionInfo")
    val versionContent: String?,
    @Schema(description = "开发语言")
    val language: String?,
    @Schema(description = "代码库链接")
    val codeSrc: String?,
    @Schema(description = "发布者")
    @BkFieldI18n(source = I18nSourceEnum.DB, keyPrefixName = "versionInfo")
    val publisher: String?,
    @Schema(description = "创建人")
    val creator: String,
    @Schema(description = "修改人")
    val modifier: String,
    @Schema(description = "创建时间")
    val createTime: String,
    @Schema(description = "修改时间")
    val updateTime: String,
    @Schema(description = "是否为默认插件（默认插件默认所有项目可见）true：默认插件 false：普通插件")
    val defaultFlag: Boolean?,
    @Schema(description = "是否可安装标识")
    val flag: Boolean?,
    @Schema(description = "插件代码库授权者")
    val repositoryAuthorizer: String?,
    @Schema(description = "插件的调试项目")
    val projectCode: String?,
    @Schema(description = "插件的初始化项目")
    val initProjectCode: String?,
    @Schema(description = "标签列表", required = false)
    val labelList: List<Label>?,
    @Schema(description = "用户评论信息")
    val userCommentInfo: StoreUserCommentInfo,
    @Schema(description = "项目可视范围,PRIVATE:私有 LOGIN_PUBLIC:登录用户开源")
    val visibilityLevel: String?,
    @Schema(description = "插件代码库不开源原因")
    val privateReason: String?,
    @Schema(description = "是否推荐标识 true：推荐，false：不推荐", required = false)
    val recommendFlag: Boolean? = null,
    @Schema(description = value = "前端UI渲染方式", required = false)
    val frontendType: FrontendTypeEnum?,
    @Schema(description = "yaml可用标识 true：是，false：否")
    val yamlFlag: Boolean? = null,
    @Schema(description = "是否可编辑")
    val editFlag: Boolean? = null,
    @Schema(description = "每日统计信息列表")
    val dailyStatisticList: List<StoreDailyStatistic>? = null,
    @Schema(description = "荣誉信息", required = false)
    val honorInfos: List<HonorInfo>? = null,
    @Schema(description = "指标信息列表")
    val indexInfos: List<StoreIndexInfo>? = null
)

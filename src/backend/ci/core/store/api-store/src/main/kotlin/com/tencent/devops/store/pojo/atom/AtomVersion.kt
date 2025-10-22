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

package com.tencent.devops.store.pojo.atom

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.api.enums.I18nSourceEnum
import com.tencent.devops.store.pojo.common.honor.HonorInfo
import com.tencent.devops.store.pojo.common.label.Label
import com.tencent.devops.store.pojo.common.statistic.StoreDailyStatistic
import com.tencent.devops.store.pojo.common.index.StoreIndexInfo
import com.tencent.devops.store.pojo.common.comment.StoreUserCommentInfo
import io.swagger.v3.oas.annotations.media.Schema

data class AtomVersion(
    @get:Schema(title = "插件ID")
    val atomId: String,
    @get:Schema(title = "插件标识")
    val atomCode: String,
    @get:Schema(title = "插件名称")
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val name: String,
    @get:Schema(title = "logo地址")
    val logoUrl: String?,
    @get:Schema(title = "插件分类code")
    val classifyCode: String?,
    @get:Schema(title = "插件分类名称")
    val classifyName: String?,
    @get:Schema(title = "插件范畴")
    val category: String?,
    @get:Schema(title = "插件说明文档链接")
    val docsLink: String?,
    @get:Schema(title = "前端渲染模板版本（1.0代表历史存量插件渲染模板版本）")
    val htmlTemplateVersion: String,
    @get:Schema(title = "插件类型")
    val atomType: String?,
    @get:Schema(title = "适用Job类型")
    val jobType: String?,
    @get:Schema(title = "操作系统")
    val os: List<String>?,
    @get:Schema(title = "插件简介")
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val summary: String?,
    @get:Schema(title = "插件描述")
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val description: String?,
    @get:Schema(title = "版本号")
    val version: String?,
    @get:Schema(title = "插件状态", required = true)
    val atomStatus: String,
    @get:Schema(title = "发布类型")
    val releaseType: String?,
    @get:Schema(title = "版本日志")
    @BkFieldI18n(source = I18nSourceEnum.DB, keyPrefixName = "versionInfo")
    val versionContent: String?,
    @get:Schema(title = "开发语言")
    val language: String?,
    @get:Schema(title = "代码库链接")
    val codeSrc: String?,
    @get:Schema(title = "发布者")
    @BkFieldI18n(source = I18nSourceEnum.DB, keyPrefixName = "versionInfo")
    val publisher: String?,
    @get:Schema(title = "创建人")
    val creator: String,
    @get:Schema(title = "修改人")
    val modifier: String,
    @get:Schema(title = "创建时间")
    val createTime: String,
    @get:Schema(title = "修改时间")
    val updateTime: String,
    @get:Schema(title = "是否为默认插件（默认插件默认所有项目可见）true：默认插件 false：普通插件")
    val defaultFlag: Boolean?,
    @get:Schema(title = "是否可安装标识")
    val flag: Boolean?,
    @get:Schema(title = "插件代码库授权者")
    val repositoryAuthorizer: String?,
    @get:Schema(title = "插件的调试项目")
    val projectCode: String?,
    @get:Schema(title = "插件的初始化项目")
    val initProjectCode: String?,
    @get:Schema(title = "标签列表", required = false)
    val labelList: List<Label>?,
    @get:Schema(title = "用户评论信息")
    val userCommentInfo: StoreUserCommentInfo,
    @get:Schema(title = "项目可视范围,PRIVATE:私有 LOGIN_PUBLIC:登录用户开源")
    val visibilityLevel: String?,
    @get:Schema(title = "插件代码库不开源原因")
    val privateReason: String?,
    @get:Schema(title = "是否推荐标识 true：推荐，false：不推荐", required = false)
    val recommendFlag: Boolean? = null,
    @get:Schema(title = "前端UI渲染方式", required = false)
    val frontendType: FrontendTypeEnum?,
    @get:Schema(title = "yaml可用标识 true：是，false：否")
    val yamlFlag: Boolean? = null,
    @get:Schema(title = "是否可编辑")
    val editFlag: Boolean? = null,
    @get:Schema(title = "每日统计信息列表")
    val dailyStatisticList: List<StoreDailyStatistic>? = null,
    @get:Schema(title = "荣誉信息", required = false)
    val honorInfos: List<HonorInfo>? = null,
    @get:Schema(title = "指标信息列表")
    val indexInfos: List<StoreIndexInfo>? = null
)

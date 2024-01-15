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

package com.tencent.devops.store.pojo.ideatom

import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.pojo.common.Category
import com.tencent.devops.store.pojo.common.Label
import com.tencent.devops.store.pojo.common.StoreUserCommentInfo
import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomStatusEnum
import io.swagger.v3.oas.annotations.media.Schema

data class IdeAtomDetail(
    @Schema(description = "插件ID")
    val atomId: String,
    @Schema(description = "插件标识")
    val atomCode: String,
    @Schema(description = "插件名称")
    val atomName: String,
    @Schema(description = "logo地址")
    val logoUrl: String?,
    @Schema(description = "插件分类code")
    val classifyCode: String?,
    @Schema(description = "插件分类名称")
    val classifyName: String?,
    @Schema(description = "下载量", required = true)
    val downloads: Int,
    @Schema(description = "星级评分", required = false)
    val score: Double?,
    @Schema(description = "范畴列表")
    val categoryList: List<Category>?,
    @Schema(description = "插件类型")
    val atomType: String?,
    @Schema(description = "插件简介")
    val summary: String?,
    @Schema(description = "插件描述")
    val description: String?,
    @Schema(description = "版本号")
    val version: String?,
    @Schema(description = "插件状态")
    val atomStatus: IdeAtomStatusEnum,
    @Schema(description = "发布类型")
    val releaseType: String?,
    @Schema(description = "版本日志")
    val versionContent: String?,
    @Schema(description = "代码库链接")
    val codeSrc: String?,
    @Schema(description = "发布者")
    val publisher: String?,
    @Schema(description = "发布时间")
    val pubTime: String?,
    @Schema(description = "是否为最新版本插件 true：最新 false：非最新")
    val latestFlag: Boolean,
    @Schema(description = "是否为公共插件 true：公共插件 false：普通插件")
    val publicFlag: Boolean,
    @Schema(description = "是否推荐， TRUE：是 FALSE：不是")
    val recommendFlag: Boolean,
    @Schema(description = "是否可安装标识 true：可以 false：不可以")
    val flag: Boolean?,
    @Schema(description = "标签列表")
    val labelList: List<Label>?,
    @Schema(description = "用户评论信息")
    val userCommentInfo: StoreUserCommentInfo,
    @Schema(description = "项目可视范围,PRIVATE:私有 LOGIN_PUBLIC:登录用户开源")
    val visibilityLevel: VisibilityLevelEnum?,
    @Schema(description = "插件代码库不开源原因")
    val privateReason: String?,
    @Schema(description = "创建人")
    val creator: String,
    @Schema(description = "修改人")
    val modifier: String,
    @Schema(description = "创建时间")
    val createTime: String,
    @Schema(description = "修改时间")
    val updateTime: String
)

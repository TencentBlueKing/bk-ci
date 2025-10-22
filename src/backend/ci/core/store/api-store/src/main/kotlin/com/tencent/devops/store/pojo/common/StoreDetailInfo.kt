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

package com.tencent.devops.store.pojo.common

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.I18nSourceEnum
import com.tencent.devops.store.pojo.common.category.Category
import com.tencent.devops.store.pojo.common.classify.Classify
import com.tencent.devops.store.pojo.common.comment.StoreUserCommentInfo
import com.tencent.devops.store.pojo.common.honor.HonorInfo
import com.tencent.devops.store.pojo.common.index.StoreIndexInfo
import com.tencent.devops.store.pojo.common.label.Label
import com.tencent.devops.store.pojo.common.version.VersionModel
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "研发商店组件详细信息")
data class StoreDetailInfo(
    @get:Schema(title = "组件ID", required = true)
    val storeId: String,
    @get:Schema(title = "组件标识", required = true)
    val storeCode: String,
    @get:Schema(title = "组件类型", required = true)
    val storeType: String,
    @get:Schema(title = "组件名称", required = true)
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val name: String,
    @get:Schema(title = "组件版本", required = true)
    var version: String = "",
    @get:Schema(title = "组件状态", required = true)
    val status: String,
    @get:Schema(title = "所属分类", required = false)
    val classify: Classify? = null,
    @get:Schema(title = "logo链接", required = false)
    val logoUrl: String?,
    @get:Schema(title = "版本信息", required = false)
    val versionInfo: VersionModel? = null,
    @get:Schema(title = "下载量", required = false)
    val downloads: Int? = 0,
    @get:Schema(title = "评分", required = false)
    val score: Double?,
    @get:Schema(title = "简介", required = false)
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val summary: String?,
    @get:Schema(title = "描述", required = false)
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val description: String?,
    @get:Schema(title = "组件的调试项目", required = false)
    val testProjectCode: String?,
    @get:Schema(title = "组件的初始化项目", required = false)
    val initProjectCode: String?,
    @get:Schema(title = "范畴列表", required = false)
    val categoryList: List<Category>?,
    @get:Schema(title = "标签列表", required = false)
    val labelList: List<Label>?,
    @get:Schema(title = "是否为最新版本 true：最新 false：非最新", required = true)
    val latestFlag: Boolean,
    @get:Schema(title = "是否有权限安装标识", required = true)
    val installFlag: Boolean,
    @get:Schema(title = "是否是公共组件", required = true)
    val publicFlag: Boolean,
    @get:Schema(title = "是否推荐标识 true：推荐，false：不推荐", required = true)
    val recommendFlag: Boolean,
    @get:Schema(title = "是否官方认证 true：是 false：否", required = true)
    val certificationFlag: Boolean,
    @get:Schema(title = "应用类型", required = false)
    val type: String?,
    @get:Schema(title = "研发类型", required = false)
    val rdType: String?,
    @get:Schema(title = "用户评论信息", required = true)
    val userCommentInfo: StoreUserCommentInfo,
    @get:Schema(title = "是否可编辑", required = false)
    val editFlag: Boolean? = null,
    @get:Schema(title = "荣誉信息", required = false)
    val honorInfos: List<HonorInfo>? = null,
    @get:Schema(title = "指标信息列表", required = false)
    val indexInfos: List<StoreIndexInfo>? = null,
    @get:Schema(title = "扩展字段集合", required = false)
    val extData: Map<String, Any>? = null
)

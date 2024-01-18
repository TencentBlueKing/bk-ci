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

package com.tencent.devops.store.pojo.common

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.I18nSourceEnum
import com.tencent.devops.store.pojo.common.index.StoreIndexInfo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "研发商店组件信息")
data class MarketItem(
    @get:Schema(title = "ID")
    val id: String,
    @get:Schema(title = "名称")
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val name: String,
    @get:Schema(title = "标识")
    val code: String,
    @get:Schema(title = "版本号")
    val version: String,
    @get:Schema(title = "类型")
    val type: String,
    @get:Schema(title = "研发来源")
    val rdType: String,
    @get:Schema(title = "分类")
    val classifyCode: String?,
    @get:Schema(title = "所属范畴")
    val category: String? = null,
    @get:Schema(title = "logo链接")
    val logoUrl: String?,
    @get:Schema(title = "发布者")
    val publisher: String,
    @get:Schema(title = "操作系统")
    val os: List<String>?,
    @get:Schema(title = "下载量")
    val downloads: Int?,
    @get:Schema(title = "评分")
    val score: Double?,
    @get:Schema(title = "简介")
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val summary: String?,
    @get:Schema(title = "是否有权限安装标识")
    val flag: Boolean,
    @get:Schema(title = "是否公共标识")
    val publicFlag: Boolean,
    @get:Schema(title = "无编译环境插件是否可以在编译环境下执行标识")
    val buildLessRunFlag: Boolean?,
    @get:Schema(title = "帮助文档")
    val docsLink: String?,
    @get:Schema(title = "修改人")
    val modifier: String,
    @get:Schema(title = "修改时间")
    val updateTime: String,
    @get:Schema(title = "是否推荐标识 true：推荐，false：不推荐", required = false)
    val recommendFlag: Boolean? = null,
    @get:Schema(title = "yaml可用标识 true：是，false：否")
    val yamlFlag: Boolean? = null,
    @get:Schema(title = "是否已在该项目安装 true：是，false：否")
    val installed: Boolean? = null,
    @get:Schema(title = "每日统计信息列表")
    val dailyStatisticList: List<StoreDailyStatistic>? = null,
    @get:Schema(title = "荣誉信息列表")
    val honorInfos: List<HonorInfo>? = null,
    @get:Schema(title = "指标信息列表")
    val indexInfos: List<StoreIndexInfo>? = null,
    @get:Schema(title = "最近执行次数")
    val recentExecuteNum: Int? = null,
    @get:Schema(title = "是否为受欢迎组件")
    val hotFlag: Boolean? = null
)

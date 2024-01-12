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

@Schema(description = "研发商店组件信息")
data class MarketItem(
    @Schema(description = "ID")
    val id: String,
    @Schema(description = "名称")
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val name: String,
    @Schema(description = "标识")
    val code: String,
    @Schema(description = "版本号")
    val version: String,
    @Schema(description = "类型")
    val type: String,
    @Schema(description = "研发来源")
    val rdType: String,
    @Schema(description = "分类")
    val classifyCode: String?,
    @Schema(description = "所属范畴")
    val category: String? = null,
    @Schema(description = "logo链接")
    val logoUrl: String?,
    @Schema(description = "发布者")
    val publisher: String,
    @Schema(description = "操作系统")
    val os: List<String>?,
    @Schema(description = "下载量")
    val downloads: Int?,
    @Schema(description = "评分")
    val score: Double?,
    @Schema(description = "简介")
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val summary: String?,
    @Schema(description = "是否有权限安装标识")
    val flag: Boolean,
    @Schema(description = "是否公共标识")
    val publicFlag: Boolean,
    @Schema(description = "无编译环境插件是否可以在编译环境下执行标识")
    val buildLessRunFlag: Boolean?,
    @Schema(description = "帮助文档")
    val docsLink: String?,
    @Schema(description = "修改人")
    val modifier: String,
    @Schema(description = "修改时间")
    val updateTime: String,
    @Schema(description = "是否推荐标识 true：推荐，false：不推荐", required = false)
    val recommendFlag: Boolean? = null,
    @Schema(description = "yaml可用标识 true：是，false：否")
    val yamlFlag: Boolean? = null,
    @Schema(description = "是否已在该项目安装 true：是，false：否")
    val installed: Boolean? = null,
    @Schema(description = "每日统计信息列表")
    val dailyStatisticList: List<StoreDailyStatistic>? = null,
    @Schema(description = "荣誉信息列表")
    val honorInfos: List<HonorInfo>? = null,
    @Schema(description = "指标信息列表")
    val indexInfos: List<StoreIndexInfo>? = null,
    @Schema(description = "最近执行次数")
    val recentExecuteNum: Int? = null,
    @Schema(description = "是否为受欢迎组件")
    val hotFlag: Boolean? = null
)

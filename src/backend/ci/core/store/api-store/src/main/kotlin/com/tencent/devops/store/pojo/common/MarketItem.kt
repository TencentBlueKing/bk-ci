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

@Schema(name = "研发商店组件信息")
data class MarketItem(
    @Schema(name = "ID")
    val id: String,
    @Schema(name = "名称")
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val name: String,
    @Schema(name = "标识")
    val code: String,
    @Schema(name = "版本号")
    val version: String,
    @Schema(name = "类型")
    val type: String,
    @Schema(name = "研发来源")
    val rdType: String,
    @Schema(name = "分类")
    val classifyCode: String?,
    @Schema(name = "所属范畴")
    val category: String? = null,
    @Schema(name = "logo链接")
    val logoUrl: String?,
    @Schema(name = "发布者")
    val publisher: String,
    @Schema(name = "操作系统")
    val os: List<String>?,
    @Schema(name = "下载量")
    val downloads: Int?,
    @Schema(name = "评分")
    val score: Double?,
    @Schema(name = "简介")
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val summary: String?,
    @Schema(name = "是否有权限安装标识")
    val flag: Boolean,
    @Schema(name = "是否公共标识")
    val publicFlag: Boolean,
    @Schema(name = "无编译环境插件是否可以在编译环境下执行标识")
    val buildLessRunFlag: Boolean?,
    @Schema(name = "帮助文档")
    val docsLink: String?,
    @Schema(name = "修改人")
    val modifier: String,
    @Schema(name = "修改时间")
    val updateTime: String,
    @Schema(name = "是否推荐标识 true：推荐，false：不推荐", required = false)
    val recommendFlag: Boolean? = null,
    @Schema(name = "yaml可用标识 true：是，false：否")
    val yamlFlag: Boolean? = null,
    @Schema(name = "是否已在该项目安装 true：是，false：否")
    val installed: Boolean? = null,
    @Schema(name = "每日统计信息列表")
    val dailyStatisticList: List<StoreDailyStatistic>? = null,
    @Schema(name = "荣誉信息列表")
    val honorInfos: List<HonorInfo>? = null,
    @Schema(name = "指标信息列表")
    val indexInfos: List<StoreIndexInfo>? = null,
    @Schema(name = "最近执行次数")
    val recentExecuteNum: Int? = null,
    @Schema(name = "是否为受欢迎组件")
    val hotFlag: Boolean? = null
)

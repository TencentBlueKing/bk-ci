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
import com.tencent.devops.store.pojo.common.honor.HonorInfo
import com.tencent.devops.store.pojo.common.index.StoreIndexInfo
import com.tencent.devops.store.pojo.common.statistic.StoreDailyStatistic
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "研发商店组件信息")
data class MarketItem(
    @get:Schema(title = "ID", required = true)
    val id: String,
    @get:Schema(title = "名称", required = true)
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val name: String,
    @get:Schema(title = "组件标识", required = true)
    val code: String,
    @get:Schema(title = "版本号", required = true)
    val version: String,
    @get:Schema(title = "状态", required = true)
    val status: String,
    @get:Schema(title = "组件类型", required = true)
    val type: String,
    @get:Schema(title = "研发来源类型", required = false)
    val rdType: String? = null,
    @get:Schema(title = "分类", required = true)
    val classifyCode: String?,
    @get:Schema(title = "所属范畴，多个范畴标识用逗号分隔", required = false)
    val category: String? = null,
    @get:Schema(title = "logo链接", required = false)
    val logoUrl: String? = null,
    @get:Schema(title = "发布者", required = true)
    val publisher: String,
    @get:Schema(title = "支持的操作系统", required = false)
    val os: List<String>? = null,
    @get:Schema(title = "下载量", required = false)
    val downloads: Int?,
    @get:Schema(title = "评分", required = false)
    val score: Double?,
    @get:Schema(title = "简介", required = false)
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val summary: String?,
    @get:Schema(title = "是否有权限安装标识", required = true)
    val flag: Boolean,
    @get:Schema(title = "是否公共标识", required = true)
    val publicFlag: Boolean,
    @get:Schema(title = "无编译环境组件是否可以在编译环境下执行标识", required = false)
    val buildLessRunFlag: Boolean? = null,
    @get:Schema(title = "帮助文档", required = false)
    val docsLink: String?,
    @get:Schema(title = "修改人", required = true)
    val modifier: String,
    @get:Schema(title = "修改时间", required = true)
    val updateTime: String,
    @get:Schema(title = "是否推荐标识 true：推荐，false：不推荐", required = false)
    val recommendFlag: Boolean? = null,
    @get:Schema(title = "yaml可用标识 true：是，false：否", required = false)
    val yamlFlag: Boolean? = null,
    @get:Schema(title = "是否已在该项目安装 true：是，false：否", required = false)
    val installed: Boolean? = null,
    @get:Schema(title = "每日统计信息列表", required = false)
    val dailyStatisticList: List<StoreDailyStatistic>? = null,
    @get:Schema(title = "荣誉信息列表", required = false)
    val honorInfos: List<HonorInfo>? = null,
    @get:Schema(title = "指标信息列表", required = false)
    val indexInfos: List<StoreIndexInfo>? = null,
    @get:Schema(title = "最近执行次数", required = false)
    val recentExecuteNum: Int? = null,
    @get:Schema(title = "是否为受欢迎组件", required = false)
    val hotFlag: Boolean? = null,
    @get:Schema(title = "是否需要更新", required = false)
    val updateFlag: Boolean? = null,
    @get:Schema(title = "扩展字段集合", required = false)
    val extData: Map<String, Any>? = null
)

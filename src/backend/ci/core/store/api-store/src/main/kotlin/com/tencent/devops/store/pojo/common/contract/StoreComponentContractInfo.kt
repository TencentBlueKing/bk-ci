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

package com.tencent.devops.store.pojo.common.contract

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "组件运行时契约")
data class StoreComponentContractInfo(
    @get:Schema(title = "组件标识")
    val storeCode: String,
    @get:Schema(title = "组件类型")
    val storeType: String,
    @get:Schema(title = "视图类型")
    val view: StoreComponentContractViewEnum,
    @get:Schema(title = "契约格式版本")
    val schemaVersion: String,
    @get:Schema(title = "契约基于的组件版本")
    val basedOnVersion: String,
    @get:Schema(title = "抽取时扫描的代码提交ID")
    val sourceCommit: String?,
    @get:Schema(title = "基于版本 PROPS 的哈希")
    val propsHash: String,
    @get:Schema(title = "覆盖程度")
    val coverage: String,
    @get:Schema(title = "是否过期")
    val staleFlag: Boolean,
    @get:Schema(title = "过期原因")
    val staleReason: String?,
    @get:Schema(title = "发布链路最近看到的最新版本")
    val lastSeenVersion: String?,
    @get:Schema(title = "选型用一句话说明")
    val selectionSummary: String?,
    @get:Schema(title = "选型用意图词与同义词")
    val selectionKeywords: List<String>,
    @get:Schema(title = "契约正文，卡片视图为裁剪后的内容")
    val contract: Map<String, Any?>,
    @get:Schema(title = "基于契约生成的详细描述，仅完整视图返回")
    val generatedDescription: String? = null,
    @get:Schema(title = "原始扫描报告在制品库的路径，仅完整视图返回")
    val reportPath: String? = null,
    @get:Schema(title = "产出本契约的抽取流水线构建ID，仅完整视图返回")
    val extractBuildId: String? = null,
    @get:Schema(title = "最近核验人")
    val reviewer: String?,
    @get:Schema(title = "最近核验时间")
    val reviewTime: LocalDateTime?,
    @get:Schema(title = "修改时间")
    val updateTime: LocalDateTime
)

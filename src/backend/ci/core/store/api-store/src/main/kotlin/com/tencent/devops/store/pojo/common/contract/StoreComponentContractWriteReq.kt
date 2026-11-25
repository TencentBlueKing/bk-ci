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

@Schema(title = "组件运行时契约写入请求")
data class StoreComponentContractWriteReq(
    @get:Schema(title = "契约格式版本", required = true)
    val schemaVersion: String,
    @get:Schema(title = "契约基于的组件版本", required = true)
    val basedOnVersion: String,
    @get:Schema(title = "抽取时扫描的代码提交ID", required = false)
    val sourceCommit: String? = null,
    @get:Schema(title = "基于版本 PROPS 的哈希", required = true)
    val propsHash: String,
    @get:Schema(title = "覆盖程度，不允许为 VERIFIED", required = true)
    val coverage: StoreComponentContractCoverageEnum,
    @get:Schema(title = "选型用一句话说明", required = false)
    val selectionSummary: String? = null,
    @get:Schema(title = "选型用意图词与同义词", required = false)
    val selectionKeywords: List<String>? = null,
    @get:Schema(title = "契约正文JSON", required = true)
    val contract: String,
    @get:Schema(title = "基于契约生成的详细描述（Markdown）", required = false)
    val generatedDescription: String? = null,
    @get:Schema(title = "原始扫描报告在制品库的路径", required = false)
    val reportPath: String? = null,
    @get:Schema(title = "产出本契约的抽取流水线构建ID", required = false)
    val extractBuildId: String? = null
)

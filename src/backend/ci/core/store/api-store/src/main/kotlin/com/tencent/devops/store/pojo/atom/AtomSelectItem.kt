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

import com.tencent.devops.store.pojo.atom.enums.AtomAvailabilityEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "插件选型候选")
data class AtomSelectItem(
    @get:Schema(title = "插件标识")
    val atomCode: String,
    @get:Schema(title = "插件名称")
    val name: String,
    @get:Schema(title = "YAML 中引用的默认版本，如 5.*")
    val defaultVersion: String,
    @get:Schema(title = "在目标项目中的可用状态")
    val availability: AtomAvailabilityEnum,
    @get:Schema(title = "近期执行次数")
    val recentExecuteNum: Int?,
    @get:Schema(title = "命中的检索词")
    val matched: List<String>,
    @get:Schema(title = "是否有运行时契约")
    val hasContract: Boolean,
    @get:Schema(title = "选型用一句话说明，没有契约时为插件简介")
    val selectionSummary: String?,
    @get:Schema(title = "不适用场景")
    val notFor: List<String>,
    @get:Schema(title = "使用前提")
    val prerequisites: List<String>,
    @get:Schema(title = "契约覆盖程度")
    val coverage: String?,
    @get:Schema(title = "契约是否因新版本过期")
    val stale: Boolean?
)

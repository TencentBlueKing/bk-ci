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

import com.tencent.devops.store.pojo.atom.enums.AtomTypeEnum
import com.tencent.devops.store.pojo.atom.enums.MarketAtomSortTypeEnum
import com.tencent.devops.store.pojo.common.enums.ServiceScopeEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "插件市场列表查询参数")
data class MarketAtomListQuery(
    @get:Schema(title = "用户ID", required = true)
    val userId: String,
    @get:Schema(title = "搜索关键字")
    val keyword: String? = null,
    @get:Schema(title = "插件分类")
    val classifyCode: String? = null,
    @get:Schema(title = "功能标签")
    val labelCode: String? = null,
    @get:Schema(title = "评分")
    val score: Int? = null,
    @get:Schema(title = "研发来源")
    val rdType: AtomTypeEnum? = null,
    @get:Schema(title = "yaml是否可用")
    val yamlFlag: Boolean? = null,
    @get:Schema(title = "是否推荐")
    val recommendFlag: Boolean? = null,
    @get:Schema(title = "是否有红线指标")
    val qualityFlag: Boolean? = null,
    @get:Schema(title = "排序方式")
    val sortType: MarketAtomSortTypeEnum? = null,
    @get:Schema(title = "页码")
    val page: Int? = 1,
    @get:Schema(title = "每页数量")
    val pageSize: Int? = 100,
    @get:Schema(title = "是否裁剪URL协议")
    val urlProtocolTrim: Boolean = false,
    @get:Schema(title = "支持的服务范围")
    val serviceScope: ServiceScopeEnum? = null
)

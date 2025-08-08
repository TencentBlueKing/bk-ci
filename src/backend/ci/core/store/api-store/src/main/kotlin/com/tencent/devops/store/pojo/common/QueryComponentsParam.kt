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

import com.tencent.devops.store.pojo.common.enums.StoreSortTypeEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "研发商店-查询组件条件")
data class QueryComponentsParam(
    @get:Schema(title = "组件类型", required = true)
    val storeType: String,
    @get:Schema(title = "类型", required = false)
    val type: String? = null,
    @get:Schema(title = "store组件名称", required = false)
    val name: String? = null,
    @get:Schema(title = "是否处于流程中", required = false)
    val processFlag: Boolean? = null,
    @get:Schema(title = "分类", required = false)
    val classifyCode: String? = null,
    @get:Schema(title = "应用范畴，多个用逗号分隔", required = false)
    val categoryCodes: String? = null,
    @get:Schema(title = "功能标签，多个用逗号分隔", required = false)
    val labelCodes: String? = null,
    @get:Schema(title = "排序", required = false)
    val sortType: StoreSortTypeEnum? = null,
    @get:Schema(title = "页码", required = true)
    val page: Int = 1,
    @get:Schema(title = "每页数量", required = true)
    val pageSize: Int = 10
)

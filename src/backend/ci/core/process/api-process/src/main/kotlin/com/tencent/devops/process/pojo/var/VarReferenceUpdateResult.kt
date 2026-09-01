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

package com.tencent.devops.process.pojo.`var`

import com.tencent.devops.process.pojo.`var`.po.ResourcePublicVarReferPO
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 变量引用更新结果
 *
 * 封装变量引用处理流程中需要新增的引用记录。
 * 注意：自方案 4 起，`T_RESOURCE_PUBLIC_VAR_VERSION_SUMMARY.REFER_COUNT` 不再由代码维护，
 * referCount 统一通过实时 JOIN 聚合查询得出，因此不再需要"需要重算计数的变量信息"字段。
 */
@Schema(title = "变量引用更新结果")
data class VarReferenceUpdateResult(
    @get:Schema(title = "需要新增的引用记录列表", description = "需要新增的变量引用记录", required = true)
    val referRecordsToAdd: List<ResourcePublicVarReferPO>
)

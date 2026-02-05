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

package com.tencent.devops.process.pojo.`var`.po

import com.tencent.devops.common.pipeline.enums.PublicVerGroupReferenceTypeEnum
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 变量组引用更新上下文
 * 按 groupName 分组的操作集合
 */
@Schema(title = "变量组引用更新上下文")
data class VarGroupReferUpdateContext(
    @get:Schema(title = "项目ID")
    val projectId: String,
    @get:Schema(title = "变量组名称")
    val groupName: String,
    @get:Schema(title = "引用资源ID")
    val referId: String,
    @get:Schema(title = "引用资源类型")
    val referType: PublicVerGroupReferenceTypeEnum,
    @get:Schema(title = "引用资源版本号")
    val referVersion: Int,
    @get:Schema(title = "需要删除的引用记录")
    val referInfosToDelete: List<ResourcePublicVarGroupReferPO>,
    @get:Schema(title = "需要新增的引用记录")
    val referInfosToAdd: List<ResourcePublicVarGroupReferPO>,
    @get:Schema(title = "版本号到计数变化量的映射（正数增加，负数减少）")
    val countChanges: Map<Int, Int>
)

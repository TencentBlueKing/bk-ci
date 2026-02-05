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

package com.tencent.devops.process.pojo.`var`.`do`

import com.tencent.devops.common.pipeline.enums.PublicVerGroupReferenceTypeEnum
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "公共变量组引用资源信息")
data class PublicGroupVarRefDO(
    @get:Schema(title = "引用ID")
    val referId: String,
    @get:Schema(title = "引用名称")
    val referName: String,
    @get:Schema(title = "引用链接")
    val referUrl: String,
    @get:Schema(title = "引用类型")
    val referType: PublicVerGroupReferenceTypeEnum,
    @get:Schema(title = "创建人")
    val creator: String,
    @get:Schema(title = "最近更新人")
    val modifier: String,
    @get:Schema(title = "最近更新时间")
    val updateTime: LocalDateTime,
    @get:Schema(title = "实际引用变量数")
    val actualRefCount: Int,
    @get:Schema(title = "实列个数")
    val instanceCount: Int? = null,
    @get:Schema(title = "最近一个月的执行次数")
    val executeCount: Int? = null
)

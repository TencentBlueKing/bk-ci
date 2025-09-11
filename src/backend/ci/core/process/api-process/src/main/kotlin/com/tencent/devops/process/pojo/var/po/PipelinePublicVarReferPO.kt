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

import com.tencent.devops.process.pojo.`var`.enums.PublicVerGroupReferenceTypeEnum
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "流水线公共变量关联信息数据")
data class PipelinePublicVarReferPO(
    @get:Schema(title = "主键ID")
    val id: Long,
    @get:Schema(title = "变量组名称")
    val groupName: String,
    @get:Schema(title = "项目ID")
    val projectId: String,
    @get:Schema(title = "变量名称")
    val varName: String,
    @get:Schema(title = "版本号")
    val version: Int? = null,
    @get:Schema(title = "关联ID")
    val referId: String,
    @get:Schema(title = "关联类型")
    val referType: PublicVerGroupReferenceTypeEnum,
    @get:Schema(title = "引用的版本名称")
    val referVersionName: String,
    @get:Schema(title = "创建者")
    val creator: String,
    @get:Schema(title = "修改者")
    val modifier: String,
    @get:Schema(title = "创建时间")
    val createTime: LocalDateTime,
    @get:Schema(title = "更新时间")
    val updateTime: LocalDateTime
)
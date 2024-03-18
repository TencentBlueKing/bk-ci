/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.devops.quality.pojo.po

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "质量红线基础数据PO")
data class QualityMetadataPO(
    @get:Schema(title = "ID")
    val id: Long,
    @get:Schema(title = "数据ID")
    val dataId: String?,
    @get:Schema(title = "基础数据名称")
    var dataName: String?,
    @get:Schema(title = "原子的classType")
    val elementType: String?,
    @get:Schema(title = "产出原子")
    var elementName: String?,
    @get:Schema(title = "工具/原子子类")
    val elementDetail: String?,
    @get:Schema(title = "数值类型")
    val valueType: String?,
    @get:Schema(title = "说明")
    var desc: String?,
    @get:Schema(title = "额外的一些字段")
    val extra: String?,
    @get:Schema(title = "创建用户")
    val createUser: String?,
    @get:Schema(title = "更新用户")
    val updateUser: String?,
    @get:Schema(title = "创建时间")
    val createTime: LocalDateTime?,
    @get:Schema(title = "更新时间")
    val updateTime: LocalDateTime?
)

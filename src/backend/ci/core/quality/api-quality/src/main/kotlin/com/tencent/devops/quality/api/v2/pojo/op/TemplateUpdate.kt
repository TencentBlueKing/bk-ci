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

package com.tencent.devops.quality.api.v2.pojo.op

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(title = "质量红线-(模板/指标集)配置展示信息")
data class TemplateUpdate(
    @get:Schema(title = "模板名称")
    val name: String?,
    @get:Schema(title = "模板类型(指标集, 模板)")
    val type: String?,
    @get:Schema(title = "描述")
    val desc: String?,
    @get:Schema(title = "可见范围类型(ANY, PART_BY_NAME)")
    val range: String?,
    @get:Schema(title = "ANY-项目ID集合, PART_BY_NAME-空集合")
    val rangeIdentification: String?,
    @get:Schema(title = "研发环节")
    val stage: String?,
    @get:Schema(title = "原子的ClassType")
    val elementType: String?,
    @get:Schema(title = "原子名称")
    val elementName: String?,
    @get:Schema(title = "红线位置(BEFORE, AFTER)")
    val controlPointPostion: String?,
    @get:Schema(title = "是否可用")
    val enable: Boolean?
)

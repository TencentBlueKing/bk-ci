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

package com.tencent.devops.store.pojo.container

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线-构建容器信息")
data class Container(
    @Schema(description = "数据库主键", required = true)
    val id: String,
    @Schema(description = "构建容器名称", required = true)
    val name: String,
    @Schema(description = "流水线容器类型", required = true)
    val type: String,
    @Schema(description = "操作系统", required = true)
    val os: String,
    @Schema(description = "容器是否为必需", required = true)
    val required: Byte,
    @Schema(description = "最长排队时间", required = false)
    val maxQueueMinutes: Int?,
    @Schema(description = "最长运行时间", required = false)
    val maxRunningMinutes: Int?,
    @Schema(description = "支持的构建资源ID列表", required = false)
    val resourceIdList: List<String>?,
    @Schema(description = "自定义扩展容器前端表单属性字段的Json串", required = true)
    val props: Map<String, Any>
)

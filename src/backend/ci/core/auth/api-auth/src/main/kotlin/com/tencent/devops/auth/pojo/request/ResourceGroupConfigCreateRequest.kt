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

package com.tencent.devops.auth.pojo.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "资源用户组配置创建请求")
data class ResourceGroupConfigCreateRequest(
    @get:Schema(title = "资源类型", required = true, example = "creative_stream")
    val resourceType: String,
    @get:Schema(title = "组代码", required = true, example = "manager")
    val groupCode: String,
    @get:Schema(title = "组名称", required = true, example = "拥有者")
    val groupName: String,
    @get:Schema(title = "组描述", required = false, example = "创作流拥有者，可以管理当前创作流的权限")
    val description: String? = null,
    @get:Schema(title = "创建模式", required = false, example = "false")
    val createMode: Boolean = false,
    @get:Schema(title = "组类型", required = false, example = "0")
    val groupType: Int = 0,
    @get:Schema(title = "操作列表", required = true)
    val actions: List<String>,
    @get:Schema(title = "授权范围JSON", required = false)
    val authorizationScopes: String? = null
)

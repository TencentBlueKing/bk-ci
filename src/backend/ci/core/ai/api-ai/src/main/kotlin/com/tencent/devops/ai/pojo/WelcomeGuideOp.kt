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

package com.tencent.devops.ai.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "运营-欢迎引导列表项")
data class WelcomeGuideOpItemVO(
    @get:Schema(title = "引导项ID")
    val id: String,
    @get:Schema(title = "父级ID，CARD 为 null")
    val parentId: String?,
    @get:Schema(title = "类型：CARD / ACTION")
    val type: String,
    @get:Schema(title = "显示文案")
    val label: String,
    @get:Schema(title = "卡片描述，仅 CARD")
    val description: String,
    @get:Schema(title = "是否启用")
    val enabled: Boolean,
    @get:Schema(title = "排序值")
    val sortOrder: Int,
    @get:Schema(title = "创建时间（毫秒时间戳，东八区）")
    val createdTime: Long
)

@Schema(title = "运营-欢迎引导创建请求")
data class WelcomeGuideCreateRequest(
    @get:Schema(title = "引导项ID，空则自动生成")
    val id: String? = null,
    @get:Schema(title = "父级卡片ID；ACTION 必填，CARD 必须为空")
    val parentId: String? = null,
    @get:Schema(title = "类型：CARD / ACTION", requiredMode = Schema.RequiredMode.REQUIRED)
    val type: String,
    @get:Schema(title = "显示文案", requiredMode = Schema.RequiredMode.REQUIRED)
    val label: String,
    @get:Schema(title = "卡片描述，仅 CARD")
    val description: String? = null,
    @get:Schema(title = "预置提示内容，仅 ACTION（FORM_COLLECT 可为空）")
    val promptContent: String? = null,
    @get:Schema(title = "交互方式，默认 PROMPT_COMPLETION")
    val interactionType: String? = null,
    @get:Schema(title = "表单定义，仅 FORM_COLLECT")
    val formSchema: FormSchemaVO? = null,
    @get:Schema(title = "角色过滤：ADMIN / MEMBER，仅 ACTION")
    val roleFilter: String? = null,
    @get:Schema(title = "图标标识，仅 CARD")
    val icon: String? = null,
    @get:Schema(title = "排序，默认 0")
    val sortOrder: Int? = null,
    @get:Schema(title = "是否启用，默认 true")
    val enabled: Boolean? = null
)

@Schema(title = "运营-欢迎引导部分更新请求")
data class WelcomeGuidePatchRequest(
    @get:Schema(title = "是否启用")
    val enabled: Boolean? = null,
    @get:Schema(title = "排序值")
    val sortOrder: Int? = null
)

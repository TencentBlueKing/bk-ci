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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "AI欢迎页引导-完整数据")
data class WelcomeGuideVO(
    @get:Schema(title = "能力卡片列表")
    val cards: List<WelcomeCardVO>,
    @get:Schema(title = "热点问题列表")
    val hotQuestions: List<HotQuestionVO>
)

@Schema(title = "AI欢迎页-能力卡片")
data class WelcomeCardVO(
    @get:Schema(title = "卡片ID")
    val id: String,
    @get:Schema(title = "卡片标题")
    val label: String,
    @get:Schema(title = "卡片描述")
    val description: String?,
    @get:Schema(title = "图标标识")
    val icon: String?,
    @get:Schema(title = "快捷操作列表")
    val actions: List<WelcomeActionVO>
)

@Schema(title = "AI欢迎页-快捷操作")
data class WelcomeActionVO(
    @get:Schema(title = "操作ID")
    val id: String,
    @get:Schema(title = "按钮文字")
    val label: String,
    @get:Schema(title = "点击后发送给AI的消息（FORM_COLLECT 时可为空，以表单 promptTemplate 为准）")
    val prompt: String,
    @get:Schema(
        title = "交互方式",
        description = "PROMPT_COMPLETION / DIRECT_TRIGGER（FORM_COLLECT 预留）"
    )
    val interactionType: String,
    @get:Schema(title = "表单定义，仅 FORM_COLLECT 时有值")
    val formSchema: FormSchemaVO?,
    @get:Schema(
        title = "角色过滤",
        description = "ADMIN / MEMBER，空表示不过滤，由前端按项目角色筛选"
    )
    val roleFilter: String?
)

@Schema(title = "欢迎引导-表单 Schema")
@JsonIgnoreProperties(ignoreUnknown = true)
data class FormSchemaVO(
    @get:Schema(title = "字段列表")
    val fields: List<FormFieldVO> = emptyList(),
    @get:Schema(title = "提交后拼装用户消息的模板，占位符 {{key}}")
    val promptTemplate: String? = null
)

@Schema(title = "欢迎引导-表单字段")
@JsonIgnoreProperties(ignoreUnknown = true)
data class FormFieldVO(
    @get:Schema(title = "字段键")
    val key: String,
    @get:Schema(title = "展示标签")
    val label: String,
    @get:Schema(
        title = "控件类型",
        description = "searchable_select / input / user_input"
    )
    val type: String,
    @get:Schema(title = "是否必填")
    val required: Boolean = false,
    @get:Schema(title = "是否多选")
    val multiple: Boolean = false,
    @get:Schema(title = "搜索数据源标识，如 pipeline / build")
    val searchApi: String? = null,
    @get:Schema(title = "依赖的字段 key，如构建号依赖流水线")
    val dependsOn: String? = null,
    @get:Schema(title = "占位提示")
    val placeholder: String? = null,
    @get:Schema(title = "默认值")
    val defaultValue: String? = null
)

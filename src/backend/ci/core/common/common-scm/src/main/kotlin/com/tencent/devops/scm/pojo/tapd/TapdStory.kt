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

package com.tencent.devops.scm.pojo.tapd

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import io.swagger.v3.oas.annotations.media.Schema

/**
 * TAPD 需求(Story)实体
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "TAPD 需求实体")
data class TapdStory(
    @get:Schema(description = "需求 ID")
    val id: String,
    @get:Schema(description = "标题")
    val name: String? = null,
    @get:Schema(description = "优先级")
    val priority: String? = null,
    @get:Schema(description = "优先级（自定义计划应用名称）")
    val priorityLabel: String? = null,
    @get:Schema(description = "业务价值")
    val businessValue: String? = null,
    @get:Schema(description = "状态")
    val status: String? = null,
    @get:Schema(description = "版本")
    val version: String? = null,
    @get:Schema(description = "模块")
    val module: String? = null,
    @get:Schema(description = "测试重点")
    val testFocus: String? = null,
    @get:Schema(description = "规模")
    val size: String? = null,
    @get:Schema(description = "处理人")
    val owner: String? = null,
    @get:Schema(description = "抄送人")
    val cc: String? = null,
    @get:Schema(description = "创建人")
    val creator: String? = null,
    @get:Schema(description = "开发人员")
    val developer: String? = null,
    @get:Schema(description = "预计开始时间")
    val begin: String? = null,
    @get:Schema(description = "预计结束时间")
    val due: String? = null,
    @get:Schema(description = "创建时间，格式 yyyy-MM-dd HH:mm:ss")
    val created: String? = null,
    @get:Schema(description = "最后修改时间")
    val modified: String? = null,
    @get:Schema(description = "完成时间")
    val completed: String? = null,
    @get:Schema(description = "迭代 ID")
    val iterationId: String? = null,
    @get:Schema(description = "预估工时")
    val effort: String? = null,
    @get:Schema(description = "完成工时")
    val effortCompleted: String? = null,
    @get:Schema(description = "剩余工时")
    val remain: String? = null,
    @get:Schema(description = "超出工时")
    val exceed: String? = null,
    @get:Schema(description = "需求分类 ID")
    val categoryId: String? = null,
    @get:Schema(description = "需求类别 ID")
    val workitemTypeId: String? = null,
    @get:Schema(description = "发布计划 ID")
    val releaseId: String? = null,
    @get:Schema(description = "来源")
    val source: String? = null,
    @get:Schema(description = "类型")
    val type: String? = null,
    @get:Schema(description = "关联的 Bug ID")
    val bugId: String? = null,
    @get:Schema(description = "父需求 ID")
    val parentId: String? = null,
    @get:Schema(description = "子需求 ID（多个用 `|` 分隔）")
    val childrenId: String? = null,
    @get:Schema(description = "祖先需求 ID")
    val ancestorId: String? = null,
    @get:Schema(description = "详细描述（HTML 富文本）")
    val description: String? = null,
    @get:Schema(description = "项目 ID")
    val workspaceId: String,
    @get:Schema(description = "创建来源（如 api、web 等）")
    val createdFrom: String? = null,
    @get:Schema(description = "流程节点")
    val step: String? = null,
    @get:Schema(description = "需求位置（到根需求的直系父需求 ID 组成）")
    val path: String? = null,
    @get:Schema(description = "需求层级（到根需求的直系父需求数量）")
    val level: String? = null,
    @get:Schema(description = "模板 ID")
    val templatedId: String? = null,
    @get:Schema(description = "特性")
    val feature: String? = null,
    @get:Schema(description = "标签")
    val label: String? = null,
    @get:Schema(description = "进度（百分比）")
    val progress: String? = null,
    @get:Schema(description = "是否归档：0 否，1 是")
    val isArchived: String? = null,
    @get:Schema(description = "技术风险")
    val techRisk: String? = null,
    @get:Schema(description = "状态流转步骤快照")
    val flows: String? = null,
    @get:Schema(description = "需求保密根节点")
    val secretRootId: String? = null,
    @get:Schema(description = "进度（已废弃，可忽略）")
    val progressManual: String? = null
) {
    /**
     * 兜底容器：所有未显式声明的字段（含 `custom_field_*` / `custom_field_*_html` /
     * `custom_plan_field_*` 等自定义/扩展字段）都会被收集到这里，避免实体爆炸。
     */
    @get:JsonAnyGetter
    @get:JsonIgnore
    val extraFields: MutableMap<String, Any?> = mutableMapOf()

    @JsonAnySetter
    fun putExtra(key: String, value: Any?) {
        extraFields[key] = value
    }

    /** 取扩展字段（自定义字段、自定义计划应用字段等）的字符串值 */
    fun extraField(key: String): String? = extraFields[key]?.toString()
}

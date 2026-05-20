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
 * TAPD 缺陷(Bug)实体
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "TAPD 缺陷实体")
data class TapdBug(
    @get:Schema(description = "缺陷 ID")
    val id: String,
    @get:Schema(description = "标题")
    val title: String? = null,
    @get:Schema(description = "优先级")
    val priority: String? = null,
    @get:Schema(description = "标签")
    val label: String? = null,
    @get:Schema(description = "优先级（自定义计划应用名称）")
    val priorityLabel: String? = null,
    @get:Schema(description = "严重程度")
    val severity: String? = null,
    @get:Schema(description = "状态")
    val status: String? = null,
    @get:Schema(description = "迭代 ID")
    val iterationId: String? = null,
    @get:Schema(description = "模块")
    val module: String? = null,
    @get:Schema(description = "特性")
    val feature: String? = null,
    @get:Schema(description = "发布计划 ID")
    val releaseId: String? = null,
    @get:Schema(description = "发现版本")
    val versionReport: String? = null,
    @get:Schema(description = "验证版本")
    val versionTest: String? = null,
    @get:Schema(description = "合入版本")
    val versionFix: String? = null,
    @get:Schema(description = "关闭版本")
    val versionClose: String? = null,
    @get:Schema(description = "发现基线")
    val baselineFind: String? = null,
    @get:Schema(description = "合入基线")
    val baselineJoin: String? = null,
    @get:Schema(description = "验证基线")
    val baselineTest: String? = null,
    @get:Schema(description = "关闭基线")
    val baselineClose: String? = null,
    @get:Schema(description = "处理人")
    val currentOwner: String? = null,
    @get:Schema(description = "抄送人")
    val cc: String? = null,
    @get:Schema(description = "创建人")
    val reporter: String? = null,
    @get:Schema(description = "参与人")
    val participator: String? = null,
    @get:Schema(description = "测试人员")
    val te: String? = null,
    @get:Schema(description = "开发人员")
    val de: String? = null,
    @get:Schema(description = "审核人")
    val auditer: String? = null,
    @get:Schema(description = "验证人")
    val confirmer: String? = null,
    @get:Schema(description = "修复人")
    val fixer: String? = null,
    @get:Schema(description = "关闭人")
    val closer: String? = null,
    @get:Schema(description = "最后修改人")
    val lastmodify: String? = null,
    @get:Schema(description = "规模")
    val size: String? = null,
    @get:Schema(description = "创建时间，格式 yyyy-MM-dd HH:mm:ss")
    val created: String? = null,
    @get:Schema(description = "接受处理时间")
    val inProgressTime: String? = null,
    @get:Schema(description = "解决时间")
    val resolved: String? = null,
    @get:Schema(description = "验证时间")
    val verifyTime: String? = null,
    @get:Schema(description = "关闭时间")
    val closed: String? = null,
    @get:Schema(description = "拒绝时间")
    val rejectTime: String? = null,
    @get:Schema(description = "最后修改时间")
    val modified: String? = null,
    @get:Schema(description = "预计开始")
    val begin: String? = null,
    @get:Schema(description = "预计结束")
    val due: String? = null,
    @get:Schema(description = "解决期限")
    val deadline: String? = null,
    @get:Schema(description = "操作系统")
    val os: String? = null,
    @get:Schema(description = "软件平台")
    val platform: String? = null,
    @get:Schema(description = "测试方式")
    val testmode: String? = null,
    @get:Schema(description = "测试阶段")
    val testphase: String? = null,
    @get:Schema(description = "测试类型")
    val testtype: String? = null,
    @get:Schema(description = "缺陷根源")
    val source: String? = null,
    @get:Schema(description = "缺陷类型")
    val bugtype: String? = null,
    @get:Schema(description = "问题 ID")
    val issueId: String? = null,
    @get:Schema(description = "重现规律")
    val frequency: String? = null,
    @get:Schema(description = "发现阶段")
    val originphase: String? = null,
    @get:Schema(description = "引入阶段")
    val sourcephase: String? = null,
    @get:Schema(description = "解决方法")
    val resolution: String? = null,
    @get:Schema(description = "预计解决时间")
    val estimate: String? = null,
    @get:Schema(description = "详细描述（HTML 富文本）")
    val description: String? = null,
    @get:Schema(description = "项目 ID")
    val workspaceId: String,
    @get:Schema(description = "预估工时")
    val effort: String? = null,
    @get:Schema(description = "完成工时")
    val effortCompleted: String? = null,
    @get:Schema(description = "剩余工时")
    val remain: String? = null,
    @get:Schema(description = "超出工时")
    val exceed: String? = null
) {
    /**
     * 兜底容器：所有未显式声明的字段（含 `custom_field_*` / `custom_plan_field_*` 等
     * 自定义/扩展字段）都会被收集到这里，避免实体爆炸。
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

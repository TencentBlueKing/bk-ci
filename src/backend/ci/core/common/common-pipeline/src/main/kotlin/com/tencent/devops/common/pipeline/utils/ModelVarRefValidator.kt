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
 * The above copyright notice and permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.pipeline.utils

import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.VarRefDetail
import java.util.regex.Pattern

/**
 * 模型变量引用表达式校验器
 *
 * 规范：只允许双大括号 ${{xxx}}；表达式前缀必须是 [variables.|stages.|jobs.|steps.|ci.]
 * 渠道：仅当 ChannelCode 为 CREATIVE_STREAM 时执行规范性校验；报错信息逻辑收敛在本类内。
 */
object ModelVarRefValidator {

    /** 默认允许的前缀（仅允许双大括号且前缀在此列表中），易扩展：新增前缀即可 */
    private val DEFAULT_ALLOWED_PREFIXES: List<String> = listOf(
        "variables.",
        "stages.",
        "jobs.",
        "steps.",
        "ci."
    )

    /** 解析 positionPath：model(.stages[i])?(.containers[j])?(.elements[k])?.?suffix，stages/containers/elements 均可为空 */
    private val POSITION_PATH_PATTERN = Pattern.compile(
        "model(?:\\.stages\\[(\\d+)])?(?:\\.containers\\[(\\d+)])?(?:\\.elements\\[(\\d+)])?\\.?(.*)"
    )

    /**
     * 返回 model 中不合规的变量引用列表（仅当 ChannelCode 为 CREATIVE_STREAM 时执行）。
     * 不抛异常，供需要自行处理列表的场景使用。
     */
    @JvmStatic
    fun getInvalidRefs(
        model: Model,
        projectId: String,
        allowedPrefixes: List<String> = DEFAULT_ALLOWED_PREFIXES
    ): List<VarRefDetail> {
        if (ChannelCode.getRequestChannelCode() != ChannelCode.CREATIVE_STREAM) {
            return emptyList()
        }
        val refs = ModelVarRefUtils.parseModelVarReferences(model, projectId, filterByTriggerParams = false)
        return refs.filter { ref -> !isValidRef(ref, allowedPrefixes) }
    }

    /**
     * 判断单条引用是否合规：双大括号 且 前缀在允许列表中。
     */
    @JvmStatic
    fun isValidRef(ref: VarRefDetail, allowedPrefixes: List<String> = DEFAULT_ALLOWED_PREFIXES): Boolean {
        if (!ref.isDoubleBrace) return false
        val name = ref.varName.trim()
        return allowedPrefixes.any { name.startsWith(it) }
    }

    /**
     * 将不合规引用格式化为报错提示文案。
     * stages/containers/elements 解析规则一致，均可为空、不一定有值。
     * 当至少有一段存在时：[阶段-容器-元素]任务名:字段名；
     * 当三段都不存在时：表达式规范为 任务名:字段名（无方括号）。
     */
    @JvmStatic
    fun formatInvalidRefsMessage(invalidRefs: List<VarRefDetail>): String {
        if (invalidRefs.isEmpty()) return ""
        return invalidRefs.joinToString("; ") { ref ->
            val (indexPart, fieldName) = parsePositionPath(ref.positionPath)
            val taskName = ref.taskName?.takeIf { it.isNotBlank() } ?: "-"
            val field = fieldName.ifBlank { "-" }
            if (indexPart == "-") "$taskName:$field" else "[$indexPart]$taskName:$field"
        }
    }

    /**
     * 解析 positionPath（如 model.stages[0].containers[0].elements[0].customCondition 或 model.triggerContainer.params），
     * 返回 (下标展示串, 字段名)。stages、containers、elements 解析规则一致，均可选；有则下标加 1 用 "-" 拼接；均无则 "-"。
     */
    private fun parsePositionPath(positionPath: String): Pair<String, String> {
        val m = POSITION_PATH_PATTERN.matcher(positionPath)
        if (!m.matches()) {
            return "-" to positionPath.trim()
        }
        val stageIdx = m.group(1)?.toInt()?.plus(1)
        val containerIdx = m.group(2)?.toInt()?.plus(1)
        val elementIdx = m.group(3)?.toInt()?.plus(1)
        val indexPart = buildList {
            stageIdx?.let { add(it.toString()) }
            containerIdx?.let { add(it.toString()) }
            elementIdx?.let { add(it.toString()) }
        }.joinToString("-").ifBlank { "-" }
        val fieldName = m.group(4)?.trim() ?: ""
        return indexPart to fieldName
    }
}

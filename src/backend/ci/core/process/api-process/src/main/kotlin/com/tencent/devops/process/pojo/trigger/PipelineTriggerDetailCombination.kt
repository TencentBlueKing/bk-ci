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

package com.tencent.devops.process.pojo.trigger

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线触发事件原因详情-明细组合")
class PipelineTriggerDetailCombination(
    @get:Schema(title = "组合的错误信息", required = true)
    val details: List<PipelineTriggerReasonDetail>,
    @get:Schema(title = "错误信息拼接符号", required = true)
    val separator: String = ":"
) : PipelineTriggerReasonDetail {
    companion object {
        const val classType = "combination"
    }

    /**
     * 渲染规则：
     * - 每个子明细的"首行"共同拼接成本组合的首行（使用 [separator] 分隔），
     *   保留"业务标题: 具体原因"这种老展示效果；
     * - 子明细如果本身返回多行（例如流水线转换失败带多条 details），
     *   除首行进入前缀外，其余行独立成行展开，避免被压成一行难以阅读。
     */
    override fun getReasonDetailList(): List<String> {
        val singleLine = mutableListOf<String>()
        val multiLines = mutableListOf<String>()
        details.forEach { detail ->
            val subList = detail.getReasonDetailList().orEmpty()
            if (subList.isEmpty()) return@forEach
            singleLine.add(subList.first())
            if (subList.size > 1) {
                multiLines.addAll(subList.drop(1))
            }
        }
        val result = mutableListOf<String>()
        if (singleLine.isNotEmpty()) {
            result.add(singleLine.joinToString(separator))
        }
        result.addAll(multiLines)
        return result
    }
}

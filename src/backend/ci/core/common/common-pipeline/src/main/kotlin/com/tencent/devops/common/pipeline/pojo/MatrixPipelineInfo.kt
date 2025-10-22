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

package com.tencent.devops.common.pipeline.pojo

import com.tencent.devops.common.api.util.YamlUtil
import io.swagger.v3.oas.annotations.media.Schema
import java.util.regex.Pattern

@Schema(title = "matrix流水线编辑校验yaml模型")
data class MatrixPipelineInfo(
    @get:Schema(title = "作为输入值时:额外的参数组合(String)/作为输出值时:校验结果", required = false)
    val include: String?,
    @get:Schema(title = "作为输入值时:排除的参数组合(String)/作为输出值时:校验结果", required = false)
    val exclude: String?,
    @get:Schema(title = "作为输入值时:分裂策略(String)/作为输出值时:校验结果", required = false)
    var strategy: String?
) {
    fun toMatrixConvert(): Any {
        return mapOf(
            "include" to if (!this.include.isNullOrBlank()) {
                val pattern = Pattern.compile("^(\\\$\\{\\{[ ]*fromJSON\\()([^(^)]+)(\\)[ ]*\\}\\})\$")
                val matcher = pattern.matcher(this.include)
                if (!matcher.find()) {
                    YamlUtil.to<Any>(this.include)
                } else null
            } else null,
            "exclude" to if (!this.exclude.isNullOrBlank()) {
                val pattern = Pattern.compile("^(\\\$\\{\\{[ ]*fromJSON\\()([^(^)]+)(\\)[ ]*\\}\\})\$")
                val matcher = pattern.matcher(this.exclude)
                if (!matcher.find()) {
                    YamlUtil.to<Any>(this.exclude)
                } else null
            } else null,
            "strategy" to if (!this.strategy.isNullOrBlank()) {
                val pattern = Pattern.compile("^(\\\$\\{\\{[ ]*fromJSON\\()([^(^)]+)(\\)[ ]*\\}\\})\$")
                val matcher = pattern.matcher(this.strategy!!)
                if (!matcher.find()) {
                    YamlUtil.to<Any>(this.strategy!!)
                } else null
            } else null
        )
    }
}

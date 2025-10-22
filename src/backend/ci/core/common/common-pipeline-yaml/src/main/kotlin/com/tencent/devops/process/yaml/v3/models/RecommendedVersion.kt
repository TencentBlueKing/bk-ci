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

package com.tencent.devops.process.yaml.v3.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.pipeline.pojo.BuildNoType
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(title = "推荐版本号 Code 定义")
data class RecommendedVersion(
    @get:Schema(title = "是否启用")
    val enabled: Boolean,
    @JsonProperty("allow-modify-at-startup")
    @get:Schema(title = "是否为入参")
    val allowModifyAtStartup: Boolean? = true,
    @get:Schema(title = "主版本")
    var major: Int = 0,
    @get:Schema(title = "特性版本")
    var minor: Int = 0,
    @get:Schema(title = "修正版本")
    var fix: Int = 0,
    @get:Schema(title = "构建号")
    @JsonProperty("build-no")
    val buildNo: BuildNo
) {
    data class BuildNo(
        @get:Schema(title = "初始值")
        @JsonProperty("initial-value")
        val initialValue: Int = 0,
        @get:Schema(title = "自增策略")
        val strategy: String = Strategy.PLUS1_EVERYTIME.alis
    )

    enum class Strategy(val alis: String) {
        PLUS1_WHEN_SUCCESS("plus1-when-success"),
        PLUS1_EVERYTIME("plus1-everytime"),
        INVARIABLE("invariable");

        fun toBuildNoType(): BuildNoType = when (this) {
            INVARIABLE -> BuildNoType.CONSISTENT
            PLUS1_EVERYTIME -> BuildNoType.EVERY_BUILD_INCREMENT
            PLUS1_WHEN_SUCCESS -> BuildNoType.SUCCESS_BUILD_INCREMENT
        }

        companion object {
            fun parse(input: BuildNoType): Strategy {
                return when (input) {
                    BuildNoType.CONSISTENT -> INVARIABLE
                    BuildNoType.EVERY_BUILD_INCREMENT -> PLUS1_EVERYTIME
                    BuildNoType.SUCCESS_BUILD_INCREMENT -> PLUS1_WHEN_SUCCESS
                }
            }

            fun parse(input: String): Strategy {
                return values().find { it.alis == input } ?: PLUS1_EVERYTIME
            }
        }
    }
}

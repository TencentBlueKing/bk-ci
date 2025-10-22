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

package com.tencent.devops.common.api.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "设置-YAML流水线功能设置")
data class PipelineAsCodeSettings(
    @get:Schema(title = "是否支持YAML流水线功能", required = true)
    val enable: Boolean = false,
    @get:Schema(title = "项目级流水线语法风格", required = false)
    var projectDialect: String? = null,
    @get:Schema(title = "是否继承项目流水线语言风格", required = false)
    val inheritedDialect: Boolean? = true,
    @get:Schema(title = "流水线语言风格", required = false)
    var pipelineDialect: String? = null
) {
    companion object {
        fun initDialect(inheritedDialect: Boolean?, pipelineDialect: String?): PipelineAsCodeSettings {
            return PipelineAsCodeSettings(
                inheritedDialect = inheritedDialect ?: true,
                // 如果继承项目方言配置,置空pipelineDialect字段,防止数据库存储多余数据
                pipelineDialect = if (inheritedDialect == false) {
                    pipelineDialect
                } else {
                    null
                }
            )
        }
    }

    /**
     * 入库时,重置方言字段值
     */
    fun resetDialect() {
        projectDialect = null
        if (inheritedDialect != false) {
            pipelineDialect = null
        }
    }
}

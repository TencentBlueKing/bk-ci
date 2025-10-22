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

package com.tencent.devops.common.pipeline.dialect

import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.pipeline.dialect.PipelineDialectType.CLASSIC
import com.tencent.devops.common.pipeline.dialect.PipelineDialectType.CONSTRAINED
import com.tencent.devops.common.pipeline.enums.ChannelCode

object PipelineDialectUtil {
    fun getPipelineDialect(pipelineDialectType: String?): IPipelineDialect {
        return pipelineDialectType?.let {
            PipelineDialectType.valueOf(it).dialect
        } ?: CLASSIC.dialect
    }

    fun getPipelineDialect(asCodeSettings: PipelineAsCodeSettings?): IPipelineDialect {
        if (asCodeSettings == null) return CLASSIC.dialect
        return with(asCodeSettings) {
            getPipelineDialect(
                inheritedDialect = inheritedDialect,
                projectDialect = projectDialect,
                pipelineDialect = pipelineDialect
            )
        }
    }

    fun getPipelineDialect(
        inheritedDialect: Boolean?,
        projectDialect: String?,
        pipelineDialect: String?
    ): IPipelineDialect {
        return getPipelineDialectType(
            inheritedDialect = inheritedDialect,
            projectDialect = projectDialect,
            pipelineDialect = pipelineDialect
        ).dialect
    }

    fun getPipelineDialectType(
        inheritedDialect: Boolean?,
        projectDialect: String?,
        pipelineDialect: String?
    ): PipelineDialectType {
        return when {
            // inheritedDialect为空和true都继承项目配置
            inheritedDialect != false && projectDialect != null ->
                PipelineDialectType.valueOf(projectDialect)

            inheritedDialect == false && pipelineDialect != null ->
                PipelineDialectType.valueOf(pipelineDialect)

            else ->
                CLASSIC
        }
    }

    fun getPipelineDialectType(asCodeSettings: PipelineAsCodeSettings?): PipelineDialectType {
        if (asCodeSettings == null) return CLASSIC
        return with(asCodeSettings) {
            getPipelineDialectType(
                inheritedDialect = inheritedDialect,
                projectDialect = projectDialect,
                pipelineDialect = pipelineDialect
            )
        }
    }

    fun getPipelineDialectType(
        channelCode: ChannelCode,
        asCodeSettings: PipelineAsCodeSettings?
    ): PipelineDialectType {
        return when {
            asCodeSettings == null -> CLASSIC
            // stream并且开启pac需要使用制约模式
            channelCode == ChannelCode.GIT && asCodeSettings.enable ->
                CONSTRAINED

            else -> {
                with(asCodeSettings) {
                    getPipelineDialectType(
                        inheritedDialect = inheritedDialect,
                        projectDialect = projectDialect,
                        pipelineDialect = pipelineDialect
                    )
                }
            }
        }
    }
}

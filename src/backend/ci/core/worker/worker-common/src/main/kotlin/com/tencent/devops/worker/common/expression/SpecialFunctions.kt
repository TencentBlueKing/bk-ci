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

package com.tencent.devops.worker.common.expression

import com.tencent.devops.common.expression.expression.ExpressionOutput
import com.tencent.devops.common.expression.expression.FunctionInfo
import com.tencent.devops.common.expression.expression.specialFuctions.hashFiles.HashFilesFunction
import com.tencent.devops.worker.common.logger.LoggerService
import org.slf4j.LoggerFactory

/**
 * 针对只有worker使用的特殊函数的集成类
 */
object SpecialFunctions {

    private val logger = LoggerFactory.getLogger(SpecialFunctions::class.java)

    val functions: List<FunctionInfo> = listOf(
        initHashFunction()
    )

    val output = object : ExpressionOutput {
        override fun writeDebugLog(content: String) {
            try {
                LoggerService.addDebugLine(content)
            } catch (e: Throwable) {
                logger.warn("hashFunction out put log error", e)
            }
        }
    }

    private fun initHashFunction(): FunctionInfo {
        return FunctionInfo(
            HashFilesFunction.name,
            1,
            Byte.MAX_VALUE.toInt(),
            HashFilesFunction()
        )
    }
}

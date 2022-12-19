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

package com.tencent.devops.common.expression.expression

import com.tencent.devops.common.expression.SubNameValueEvaluateInfo
import com.tencent.devops.common.expression.SubNameValueEvaluateResult

interface IExpressionNode {
    fun evaluate(
        trace: ITraceWriter?,
        state: Any?,
        options: EvaluationOptions?,
        expressionOutput: ExpressionOutput?
    ): EvaluationResult

    /**
     * 只负责替换指定的nameValued和index
     *
     * 如果包含其他nameValued
     * * 指定的nameValued计算后替换为 toJson 之后的字符串
     * * 其他的nameValued涉及到与指定的nameValue进行计算时，为null，不涉及计算时打印原样保持不变
     * * 运算符除index之外保持原样不变
     * * 函数保持原样不变（只能保持为函数原命名）
     *
     * 如果不包含
     * * 直接计算
     */
    fun subNameValueEvaluate(
        trace: ITraceWriter?,
        state: Any?,
        options: EvaluationOptions?,
        subInfo: SubNameValueEvaluateInfo,
        expressionOutput: ExpressionOutput?
    ): SubNameValueEvaluateResult
}

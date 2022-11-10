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

package com.tencent.devops.common.expression.expression.specialFuctions.hashFiles

import com.tencent.devops.common.expression.ContextNotFoundException
import com.tencent.devops.common.expression.ExecutionContext
import com.tencent.devops.common.expression.context.DictionaryContextData
import com.tencent.devops.common.expression.context.StringContextData
import com.tencent.devops.common.expression.expression.sdk.EvaluationContext
import com.tencent.devops.common.expression.expression.sdk.Function
import com.tencent.devops.common.expression.expression.sdk.ResultMemory

/**
 * 计算给定文件的 SHA-256 哈希 #7855
 * hashFiles(path,path,..)
 * 注：因为涉及到文件计算，所以只能在worker或template中使用
 */
class HashFilesFunction : Function() {

    override fun createNode() = HashFilesFunction()

    override fun evaluateCore(context: EvaluationContext): Pair<ResultMemory?, Any?> {
        val contextValues = context.state as ExecutionContext
        var workspaceData = contextValues.expressionValues[ciWorkSpaceKey.split(".")[0]]
        workspaceData = if (workspaceData == null) {
            if (contextValues.expressionValues[workSpaceKey] == null) {
                throw ContextNotFoundException("$workSpaceKey/$ciWorkSpaceKey")
            } else {
                contextValues.expressionValues[workSpaceKey]
            }
        } else {
            if (workspaceData !is DictionaryContextData || workspaceData[ciWorkSpaceKey.split(".")[1]] == null) {
                if (contextValues.expressionValues[workSpaceKey] == null) {
                    throw ContextNotFoundException("$workSpaceKey/$ciWorkSpaceKey")
                } else {
                    contextValues.expressionValues[workSpaceKey]
                }
            } else {
                workspaceData[ciWorkSpaceKey.split(".")[1]]
            }
        }
        val workspace = (workspaceData as StringContextData).value

        val patterns = parameters.map { it.evaluate(context).convertToString() }.toList()

        val hash = HashFiles(workspace, context.expressionOutput).calculate(patterns)

        return Pair(null, hash)
    }

    override fun subNameValueEvaluateCore(context: EvaluationContext): Pair<Any?, Boolean> {
        val sb = StringBuilder(name).append("(")
        parameters.forEachIndexed { index, param ->
            sb.append(param.subNameValueEvaluate(context).parseSubNameValueEvaluateResult())
            if (index != parameters.count() - 1) {
                sb.append(", ")
            }
        }
        sb.append(")")
        return Pair(sb.toString(), false)
    }

    companion object {
        const val name = "hashFiles"
        private const val workSpaceKey = "WORKSPACE"
        private const val ciWorkSpaceKey = "ci.workspace"
    }
}

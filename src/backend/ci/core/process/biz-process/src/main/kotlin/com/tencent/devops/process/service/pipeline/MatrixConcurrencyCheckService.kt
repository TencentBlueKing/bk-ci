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

package com.tencent.devops.process.service.pipeline

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.EnvReplacementParser
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.service.PipelineAsCodeService
import com.tencent.devops.process.utils.MatrixConcurrencyUtils
import com.tencent.devops.process.utils.PIPELINE_MATRIX_CON_RUNNING_SIZE_MAX
import com.tencent.devops.process.utils.PipelineVarUtil
import org.springframework.stereotype.Service

/**
 * 模板实例化时校验Matrix Job的最大并发数
 *
 * 最大并发数支持变量占位（MatrixControlOption.maxConcurrencyVar），实例化时用实例参数解析变量，
 * 解析结果必须是1到[PIPELINE_MATRIX_CON_RUNNING_SIZE_MAX]之间的整数，否则实例化失败，
 * 不允许用变量绕过最大并发数的有效性要求，也不做静默截断。
 *
 * 实例化阶段只具备流水线参数上下文，内置变量、运行时上下文等无法在此解析，
 * 这类「解析不出结果」的情况不在本阶段判定（[isUnresolved]），留到运行时启动matrix job时严格校验，
 * 避免把「本阶段无法求值」误报为「并发数非法」。
 *
 * 模板实例化存在Model链路（TemplateFacadeService）与版本化链路（PipelineTemplateInstanceReqConverter），
 * 两条链路都要经过此处，避免遗漏。
 */
@Service
class MatrixConcurrencyCheckService(
    private val pipelineAsCodeService: PipelineAsCodeService
) {

    /**
     * 解析后仍是空白或仍残留变量占位符，说明该变量在实例化阶段取不到值
     */
    private fun isUnresolved(value: String): Boolean =
        value.isBlank() || MatrixConcurrencyUtils.containsVariable(value)

    fun checkInstanceMatrixMaxConcurrency(projectId: String, pipelineName: String, model: Model) {
        val dialect = pipelineAsCodeService.getProjectDialect(projectId = projectId)
        // 参数未填值时取空串，避免出现「最大并发数为 null」这种无意义文案
        val contextMap = PipelineVarUtil.fillVariableMap(
            model.getTriggerContainer().params.associate { it.id to (it.defaultValue?.toString() ?: "") }
        )
        for (stage in model.stages) {
            for (container in stage.containers) {
                // 非矩阵Job跳过（等价于原 forEach + return@container 的语义，不能用 break，
                // 否则会终止本 Stage 剩余容器的遍历，漏掉后面的矩阵Job）
                if (container.matrixGroupFlag != true) continue
                val option = when (container) {
                    is VMBuildContainer -> container.matrixControlOption
                    is NormalContainer -> container.matrixControlOption
                    else -> null
                } ?: continue
                // 未配置并发数时取默认值，属于合法配置
                val maxConcurrencyVar = option.maxConcurrencyVar?.trim()
                val value = if (!maxConcurrencyVar.isNullOrBlank()) {
                    val parsed = EnvReplacementParser.parse(
                        value = maxConcurrencyVar,
                        contextMap = contextMap,
                        onlyExpression = dialect.supportUseExpression()
                    ).trim()
                    // 实例化阶段取不到值：交由运行时启动matrix job时校验，不在此误判为非法
                    if (isUnresolved(parsed)) continue
                    parsed
                } else {
                    option.maxConcurrency?.toString() ?: continue
                }
                if (!MatrixConcurrencyUtils.isValid(value.toIntOrNull())) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_TEMPLATE_MATRIX_MAX_CONCURRENCY_INVALID,
                        params = arrayOf(
                            pipelineName,
                            container.name,
                            // 解析结果为空时展示原始变量表达式，避免出现「最大并发数为 ，须为...」这种无值文案
                            value.takeIf { it.isNotBlank() } ?: maxConcurrencyVar.orEmpty(),
                            PIPELINE_MATRIX_CON_RUNNING_SIZE_MAX.toString()
                        )
                    )
                }
            }
        }
    }
}

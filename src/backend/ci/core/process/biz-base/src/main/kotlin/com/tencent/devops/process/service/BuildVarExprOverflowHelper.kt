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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.util.ObjectReplaceEnvVarUtil
import com.tencent.devops.process.utils.BuildVarOverflowUtils
import com.tencent.devops.process.utils.PIPELINE_ID
import org.slf4j.LoggerFactory

/**
 * 引擎侧表达式求值：从变量 Map 组装 overflowKeys + 按需 loader。
 * 无大变量时 loader 为 null，调用方可直接传给 [com.tencent.devops.common.pipeline.EnvReplacementParser]。
 *
 * loader 直接使用变量 Map 中的引用串定位制品库文件，不再回查数据库；
 * 同一次 [options] / [session] 内同值只下载一次，并受会话级预算约束。
 */
object BuildVarExprOverflowHelper {

    private val logger = LoggerFactory.getLogger(BuildVarExprOverflowHelper::class.java)

    fun options(
        buildVariableService: BuildVariableService,
        projectId: String,
        buildId: String,
        variables: Map<String, String>,
        pipelineId: String? = null
    ): Pair<Set<String>, ((String) -> String?)?> {
        val overflowKeys = BuildVarOverflowUtils.collectOverflowKeys(variables)
        if (overflowKeys.isEmpty()) {
            return emptySet<String>() to null
        }
        return overflowKeys to newLoader(buildVariableService, projectId, pipelineId, buildId, variables)
    }

    /**
     * 组装一次求值会话：把变量表与 overflowKeys / loader 绑成单个入参。
     *
     * 供 [com.tencent.devops.process.engine.control.ControlUtils] 的 Stage/Job/Task 自定义条件判断使用，
     * 避免这两个参数被逐个透传——漏传时不会编译报错，只会让大变量停留在引用串上导致条件静默算错。
     *
     * 无大变量时等价于 [BuildVarExprSession.empty]，不产生任何制品库 IO。
     */
    fun session(
        buildVariableService: BuildVariableService,
        projectId: String,
        buildId: String,
        variables: Map<String, String>,
        pipelineId: String? = null
    ): BuildVarExprSession {
        val (overflowKeys, overflowLoader) = options(
            buildVariableService = buildVariableService,
            projectId = projectId,
            buildId = buildId,
            variables = variables,
            pipelineId = pipelineId
        )
        return BuildVarExprSession(
            variables = variables,
            overflowKeys = overflowKeys,
            overflowLoader = overflowLoader
        )
    }

    /**
     * 传统方言(不走表达式引擎)在引擎侧 claim 时对任务参数做变量替换的入口，
     * 仅用于兼容未声明 `replaceVarOnWorker` 的老 Worker。
     *
     * 与直接调用 [ObjectReplaceEnvVarUtil.replaceEnvVar] 的差异**仅**在于：被 `${{ key }}` 引用到、
     * 且命中溢出键的大变量会被替换成真实值(按需从制品库下载)；`${x}` / `$x` 旧语法、未被引用的大变量、
     * 其它普通变量一律保持原有行为不变。
     *
     * 实现：把命中的 `${{ overflowKey }}` 改写成 `${{ 合成唯一键 }}`，并把该键的真实值放进一份变量副本，
     * 让 [ObjectReplaceEnvVarUtil.replaceEnvVar] 像处理**普通变量**一样完成替换，复用其 JSON 转义逻辑。
     */
    @Suppress("LongParameterList")
    fun replaceEnvVarWithOverflow(
        value: Any?,
        variables: Map<String, String>,
        buildVariableService: BuildVariableService,
        projectId: String,
        buildId: String,
        pipelineId: String? = null
    ): Any {
        val overflowKeys = BuildVarOverflowUtils.collectOverflowKeys(variables)
        if (overflowKeys.isEmpty()) {
            return ObjectReplaceEnvVarUtil.replaceEnvVar(value, variables)
        }
        val loader = newLoader(buildVariableService, projectId, pipelineId, buildId, variables)
        val (rewritten, synthVars) =
            BuildVarOverflowUtils.rewriteOverflowRefs(value, overflowKeys, loader)
        if (synthVars.isEmpty()) {
            return ObjectReplaceEnvVarUtil.replaceEnvVar(value, variables)
        }
        // 仅追加合成键，原变量表原样保留；${x}/$x、其它变量的替换行为完全不变
        val effectiveVars = HashMap(variables).apply { putAll(synthVars) }
        logger.info("CLASSIC_OVERFLOW_RESOLVED|buildId=$buildId|count=${synthVars.size}")
        return ObjectReplaceEnvVarUtil.replaceEnvVar(rewritten, effectiveVars)
    }

    /**
     * 按表达式 key 解析变量 Map 中的引用串并下载真实值；下载失败时返回引用串本身。
     */
    private fun newLoader(
        buildVariableService: BuildVariableService,
        projectId: String,
        pipelineId: String?,
        buildId: String,
        variables: Map<String, String>
    ): (String) -> String? {
        val realPipelineId = pipelineId ?: variables[PIPELINE_ID]
        if (realPipelineId.isNullOrBlank()) {
            logger.warn("$buildId|LARGE_VAR_LOADER_NO_PIPELINE_ID|keep reference")
            return { key -> BuildVarOverflowUtils.resolveStoredKey(key, variables)?.let { variables[it] } }
        }
        val loader = buildVariableService.newOverflowLoader(
            projectId = projectId,
            pipelineId = realPipelineId,
            buildId = buildId
        )
        return { key ->
            BuildVarOverflowUtils.resolveStoredKey(key, variables)?.let { variables[it] }?.let { stored ->
                loader.load(stored) ?: stored
            }
        }
    }
}

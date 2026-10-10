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

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.dao.PipelineBuildHistoryParamOverflowDao
import com.tencent.devops.process.utils.BuildVarOverflowUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 构建启动参数大值"引用化 / 按需解析"服务（长期载体版）。
 *
 * ## 背景
 *  - 启动参数（manualStartup 等）单值最多 4M，长期载体是
 *    T_PIPELINE_BUILD_HISTORY[_DEBUG].BUILD_PARAMETERS（mediumtext）JSON；
 *  - 大值原样进 JSON 会：超 mediumtext 上限 / 列表反序列化 N 行大值导致 OOM。
 *
 * ## 方案（与 [BuildVarOverflowUtils] 引用协议一致）
 *  - **写入**（[persistAndStrip]）：value 长度 > 4K 的启动参数，full value 落
 *    [PipelineBuildHistoryParamOverflowDao]，JSON 里只留引用串 `__BK_OVF__:<len>`；
 *  - **读取**：
 *    - 启动参数 Tab（getBuildParameters）**不解析**，只返回引用串，避免高频接口批量加载大值；
 *    - 单 key 按需接口（getBuildParameterValue）走 [resolveSingleValue]；
 *    - retry / replay 走 [resolveForRestart]（必须完整真实值，带预算护栏）；
 *  - 列表 / 历史接口**不调用** resolve，只看到引用串 → 天然 OOM 安全。
 *
 * ## 对历史逻辑零影响
 *  - 历史构建启动参数最大 4K，[persistAndStrip] 不会触发溢出、[resolve] 走"无引用"快路径直接原样返回；
 *  - 仅"启用大启动参数后的新构建"才进入溢出路径。
 *
 * 注意：本服务**只处理 String 类型的大 value**；非 String（Boolean/Int/级联 Map 等）天然很小，原样保留。
 */
@Service
class BuildStartupParamOverflowService @Autowired constructor(
    private val paramOverflowDao: PipelineBuildHistoryParamOverflowDao,
    private val pipelineVarOverflowConfig: PipelineVarOverflowConfig
) {

    /**
     * 把超过 4K 的启动参数大值落溢出表，并返回一个"引用化"的副本：
     * 大值在副本里被替换为 `__BK_OVF__:<len>` 引用串，可安全写进 BUILD_PARAMETERS JSON。
     *
     * - 不修改入参对象（[BuildParameters] 元素与 pipelineParamMap 共享，**禁止**原地修改 value）；
     * - 无任何大值时走快路径，返回原列表、不触库。
     */
    @Suppress("LongParameterList")
    fun persistAndStrip(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        debug: Boolean,
        params: List<BuildParameters>
    ): List<BuildParameters> {
        if (params.none { isOverflowString(it.value) }) return params
        return params.map { param ->
            val value = param.value
            if (isOverflowString(value)) {
                val raw = value as String
                paramOverflowDao.save(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    key = param.key,
                    value = raw,
                    debug = debug
                )
                param.copy(value = BuildVarOverflowUtils.toMainTableValue(raw))
            } else {
                param
            }
        }
    }

    /**
     * 批量展示解析（带预算降级）。启动参数 Tab 已改为默认不解析；
     * 仍保留给"需要尽量还原、但可降级"的单构建消费方（如参数组合回填）。
     * **严格不超过 [maxResolveBytes]**，超过即停止加载、剩余保留引用串。
     */
    fun resolveForDisplay(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        params: List<BuildParameters>
    ): List<BuildParameters> = resolveInternal(
        dslContext = dslContext,
        projectId = projectId,
        buildId = buildId,
        params = params,
        // 与 startupTotalMax 解耦：单次最多加载 lazyLoadBudgetMax（默认 32M），超出保留引用串。
        maxResolveBytes = pipelineVarOverflowConfig.lazyLoadBudgetMax,
        degradeOnExceed = true
    )

    /**
     * 单 key 按需解析真实值。非引用串原样返回；引用串则查溢出表，查不到时回退为引用串本身。
     */
    fun resolveSingleValue(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        key: String,
        storedValue: Any?
    ): Any? {
        if (storedValue !is String || !BuildVarOverflowUtils.isOverflowReference(storedValue)) {
            return storedValue
        }
        return paramOverflowDao.getValue(dslContext, projectId, buildId, key) ?: storedValue
    }

    /**
     * 重试 / 重放场景使用：必须拿到真实值，否则会把引用串当真实值喂给新构建。
     * 单构建启动参数总量在创建时已 ≤ [PipelineVarOverflowConfig.startupParamsTotalMax]，
     * 故按该值解析可保证"合法创建的构建"总能被完整还原；
     * 仅当运营把上限调小到低于历史构建实际大小这种极端情况才会越界——此时**抛错快速失败**（绝不静默降级成错误值）。
     *
     * 内存说明：单次峰值受单构建总量约束（≤ startupParamsTotalMax），与原始 manualStartup 持有请求体的开销同量级；
     * retry/replay 属低频用户操作，整体内存随启动并发由既有限流 / 排队约束。
     */
    fun resolveForRestart(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        params: List<BuildParameters>
    ): List<BuildParameters> = resolveInternal(
        dslContext = dslContext,
        projectId = projectId,
        buildId = buildId,
        params = params,
        maxResolveBytes = pipelineVarOverflowConfig.startupParamsTotalMax,
        degradeOnExceed = false
    )

    /**
     * - 无引用串时走快路径，返回原列表、不触库；
     * - **加载前**先用引用串里编码的长度做预算判断：会越界的值根本不查库、不入堆；
     * - [degradeOnExceed]=true：越界时保留引用串（展示降级）；false：越界抛错（重启场景，避免静默错误）。
     */
    @Suppress("LongParameterList")
    private fun resolveInternal(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        params: List<BuildParameters>,
        maxResolveBytes: Long,
        degradeOnExceed: Boolean
    ): List<BuildParameters> {
        if (params.none { isOverflowReference(it.value) }) return params
        var loaded = 0L
        return params.map { param ->
            val value = param.value
            if (!isOverflowReference(value)) return@map param
            // 预读引用串编码的长度，加载前判断是否越界，避免把会丢弃的大值读进堆
            val declaredLen = referenceLength(value as String)
            if (loaded + declaredLen > maxResolveBytes) {
                if (degradeOnExceed) {
                    LOG.warn("$buildId|STARTUP_PARAM_RESOLVE_DEGRADE|key=${param.key}|loaded=$loaded|" +
                            "max=$maxResolveBytes")
                    return@map param
                }
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_START_VARIABLES_TOTAL_OVERSIZE,
                    params = arrayOf((loaded + declaredLen).toString(), maxResolveBytes.toString()),
                    defaultMessage = "Startup variables total size exceeds the limit[$maxResolveBytes]"
                )
            }
            val real = paramOverflowDao.getValue(dslContext, projectId, buildId, param.key)
                ?: return@map param
            loaded += real.length.toLong()
            param.copy(value = real)
        }
    }

    private fun isOverflowString(value: Any?): Boolean =
        value is String && BuildVarOverflowUtils.shouldOverflow(value)

    private fun isOverflowReference(value: Any?): Boolean =
        value is String && BuildVarOverflowUtils.isOverflowReference(value)

    /** 从引用串 `__BK_OVF__:<len>` 解析声明长度；无法解析时返回 0（退化为加载后按真实长度计） */
    private fun referenceLength(reference: String): Long =
        reference.removePrefix(BuildVarOverflowUtils.OVERFLOW_PREFIX).toLongOrNull() ?: 0L

    companion object {
        private val LOG = LoggerFactory.getLogger(BuildStartupParamOverflowService::class.java)
    }
}

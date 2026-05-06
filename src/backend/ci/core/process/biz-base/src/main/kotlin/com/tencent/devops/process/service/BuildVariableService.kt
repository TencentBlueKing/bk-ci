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

import com.tencent.devops.common.api.util.TemplateFastReplaceUtils
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.pipeline.dialect.PipelineDialectUtil
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.process.engine.control.lock.PipelineBuildVarLock
import com.tencent.devops.process.engine.dao.PipelineBuildVarDao
import com.tencent.devops.process.engine.dao.PipelineBuildVarOverflowDao
import com.tencent.devops.process.pojo.BuildVariableSnapshot
import com.tencent.devops.process.utils.BuildVarOverflowUtils
import com.tencent.devops.process.utils.PIPELINE_DIALECT
import com.tencent.devops.process.utils.PIPELINE_RETRY_COUNT
import com.tencent.devops.process.utils.PipelineVarUtil
import org.apache.commons.lang3.math.NumberUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("TooManyFunctions")
class BuildVariableService @Autowired constructor(
    private val commonDslContext: DSLContext,
    private val pipelineBuildVarDao: PipelineBuildVarDao,
    private val pipelineBuildVarOverflowDao: PipelineBuildVarOverflowDao,
    private val pipelineAsCodeService: PipelineAsCodeService,
    private val redisOperation: RedisOperation
) {

    /**
     * 获取构建执行次数（重试次数+1），如没有重试过，则为1
     */
    fun getBuildExecuteCount(projectId: String, pipelineId: String, buildId: String): Int {
        val retryCount = getVariable(
            projectId = projectId, pipelineId = pipelineId,
            buildId = buildId, varName = PIPELINE_RETRY_COUNT
        )
        return try {
            if (NumberUtils.isParsable(retryCount)) 1 + retryCount!!.toInt() else 1
        } catch (ignored: Exception) {
            1
        }
    }

    /**
     * 将模板语法中的[template]模板字符串替换成当前构建[buildId]下对应的真正的字符串
     */
    fun replaceTemplate(projectId: String, buildId: String, template: String?): String {
        return TemplateFastReplaceUtils.replaceTemplate(templateString = template) { templateWord ->
            val word = PipelineVarUtil.oldVarToNewVar(templateWord)
                ?: PipelineVarUtil.fetchVarName(templateWord)
                ?: templateWord
            val templateValByType = pipelineBuildVarDao.getVarsWithType(
                dslContext = commonDslContext,
                projectId = projectId,
                buildId = buildId,
                key = word
            )
            if (templateValByType.isNotEmpty()) templateValByType[0].value.toString() else null
        }
    }

    fun getVariable(projectId: String, pipelineId: String, buildId: String, varName: String): String? {
        val vars = getAllVariable(projectId = projectId, pipelineId = pipelineId, buildId = buildId)
        return if (vars.isNotEmpty()) vars[varName] else null
    }

    fun getAllVariable(
        projectId: String,
        pipelineId: String,
        buildId: String,
        keys: Set<String>? = null
    ): Map<String, String> {
        val dataMap = pipelineBuildVarDao.getVars(
            dslContext = commonDslContext,
            projectId = projectId,
            buildId = buildId,
            keys = keys
        )
        val dialect = PipelineDialectUtil.getPipelineDialect(dataMap[PIPELINE_DIALECT])
        return if (dialect.supportUseExpression()) {
            dataMap
        } else {
            PipelineVarUtil.mixOldVarAndNewVar(dataMap)
        }
    }

    /**
     * 获取构建变量"快照"。
     *
     * 与 [getAllVariable] 不同的是，本方法**始终**返回原始（含摘要前缀）的小值 +
     * 溢出键集合 + 懒加载器。仅在"需要按 ${{ xxx }} 表达式求值"的场景使用，
     * 普通查询继续使用 [getAllVariable]，不会改变现有行为与内存占用。
     *
     * 仅当 [keys] 命中"溢出键"时才会触发表查询，避免无效流量。
     */
    fun getVariableSnapshot(
        projectId: String,
        pipelineId: String,
        buildId: String,
        keys: Set<String>? = null
    ): BuildVariableSnapshot {
        val dataMap = pipelineBuildVarDao.getVars(
            dslContext = commonDslContext,
            projectId = projectId,
            buildId = buildId,
            keys = keys
        )
        val largeKeys = dataMap.entries.asSequence()
            .filter { BuildVarOverflowUtils.isOverflowSummary(it.value) }
            .map { it.key }
            .toSet()
        return BuildVariableSnapshot(
            smallVars = dataMap,
            largeKeys = largeKeys,
            largeValueLoader = { key ->
                if (key !in largeKeys) {
                    dataMap[key]
                } else {
                    pipelineBuildVarOverflowDao.getValue(commonDslContext, projectId, buildId, key)
                }
            }
        )
    }

    /**
     * 单变量按需获取真实值（含溢出表）。
     * 主要供 ${{ xxx }} 表达式按需求值时调用。
     */
    fun getVariableValue(
        projectId: String,
        buildId: String,
        varName: String
    ): String? {
        val main = pipelineBuildVarDao.getVars(
            dslContext = commonDslContext,
            projectId = projectId,
            buildId = buildId,
            keys = setOf(varName)
        )[varName]
        return if (BuildVarOverflowUtils.isOverflowSummary(main)) {
            pipelineBuildVarOverflowDao.getValue(commonDslContext, projectId, buildId, varName)
        } else {
            main
        }
    }

    fun getAllVariableWithType(projectId: String, buildId: String): List<BuildParameters> {
        return pipelineBuildVarDao.getVarsWithType(commonDslContext, projectId, buildId)
    }

    fun setVariable(
        projectId: String,
        pipelineId: String,
        buildId: String,
        varName: String,
        varValue: Any,
        readOnly: Boolean? = null,
        rewriteReadOnly: Boolean? = null
    ) {
        val realVarName = PipelineVarUtil.oldVarToNewVar(varName) ?: varName
        saveVariable(
            dslContext = commonDslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            name = realVarName,
            value = varValue,
            readOnly = readOnly,
            rewriteReadOnly = rewriteReadOnly
        )
    }

    fun batchUpdateVariable(
        projectId: String,
        pipelineId: String,
        buildId: String,
        variables: Map<String, Any>,
        sensitiveKeys: Set<String>? = null
    ) {
        commonDslContext.transaction { t ->
            val context = DSL.using(t)
            batchSetVariable(
                dslContext = context,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                variables = variables.map { va ->
                    va.key to BuildParameters(
                        key = va.key,
                        value = va.value,
                        valueType = BuildFormPropertyType.STRING,
                        sensitive = sensitiveKeys?.contains(va.key)
                    )
                }.toMap()
            )
        }
    }

    /**
     * will delete the [buildId] 's all writable vars
     */
    fun deleteWritableVars(dslContext: DSLContext, projectId: String, buildId: String) {
        // 先删大变量溢出，再删主表，避免出现"主表删除而溢出表残留"的孤儿。
        // 注意：当前 deleteWritableVars 仅清理 readOnly=false 的变量，溢出表不区分 readOnly，
        // 此处采用"全量删除溢出 + 重新写回 readOnly=true 的溢出"模式过于复杂，
        // 业务侧的调用方（PipelineBuildRetryService 等）实际是为重试做"清理可写变量"，
        // 此时 readOnly=true 的内置变量不会有溢出值，全量删除溢出表数据是安全的。
        pipelineBuildVarOverflowDao.deleteByBuildId(commonDslContext, projectId, buildId)
        pipelineBuildVarDao.deleteBuildVar(
            dslContext = commonDslContext,
            projectId = projectId,
            buildId = buildId,
            varName = null,
            readOnly = false
        )
    }

    fun deleteBuildVars(projectId: String, pipelineId: String, buildId: String) {
        pipelineBuildVarOverflowDao.deleteByBuildId(commonDslContext, projectId, buildId)
        pipelineBuildVarDao.deleteBuildVars(
            dslContext = commonDslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId
        )
    }

    fun deletePipelineBuildVar(projectId: String, pipelineId: String) {
        pipelineBuildVarOverflowDao.deleteByPipelineId(commonDslContext, projectId, pipelineId)
        pipelineBuildVarDao.deletePipelineBuildVar(
            dslContext = commonDslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
    }

    // 保存方法需要提供事务保护的实现，传入特定dslContext
    @Suppress("LongParameterList")
    fun saveVariable(
        dslContext: DSLContext,
        buildId: String,
        projectId: String,
        pipelineId: String,
        name: String,
        value: Any,
        readOnly: Boolean? = null,
        rewriteReadOnly: Boolean? = null
    ) {
        val rawValue = value.toString()
        // 单值就先做硬上限校验，避免大对象进入锁内
        rejectIfHardOversize(buildId, name, rawValue)
        val redisLock = PipelineBuildVarLock(redisOperation, buildId, name)
        try {
            redisLock.lock()
            // 1) 先把溢出值写入溢出表，再让主表写"摘要"，保证一致性
            persistOverflowIfNeeded(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                key = name,
                rawValue = rawValue,
                readOnly = readOnly
            )
            val varMap = pipelineBuildVarDao.getVars(dslContext, projectId, buildId, setOf(name))
            if (varMap.isEmpty()) {
                pipelineBuildVarDao.save(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    name = name,
                    value = rawValue,
                    readOnly = readOnly
                )
            } else {
                pipelineBuildVarDao.update(
                    dslContext = dslContext,
                    projectId = projectId,
                    buildId = buildId,
                    name = name,
                    value = rawValue,
                    rewriteReadOnly = rewriteReadOnly
                )
            }
            // 2) 当一个原本是大值的变量被改回小值时，需要把溢出表的旧记录清理掉
            if (!BuildVarOverflowUtils.shouldOverflow(rawValue)) {
                pipelineBuildVarOverflowDao.deleteByKey(dslContext, projectId, buildId, name)
            }
        } finally {
            redisLock.unlock()
        }
    }

    /**
     * 注意：该方法没做并发保护，仅用于在刚启动构建时使用，其他并发场景请使用[batchSetVariable]
     */
    fun startBuildBatchSaveWithoutThreadSafety(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        variables: Map<String, BuildParameters>
    ) {
        val params = variables.map { it.value }
        // 启动阶段也做溢出处理，便于一开始就让大变量进入溢出表
        params.forEach { rejectIfHardOversize(buildId, it.key, it.value.toString()) }
        val overflowParams = params.filter { BuildVarOverflowUtils.shouldOverflow(it.value.toString()) }
        if (overflowParams.isNotEmpty()) {
            pipelineBuildVarOverflowDao.batchSave(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                params = overflowParams
            )
        }
        pipelineBuildVarDao.batchSave(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            variables = params
        )
    }

    fun batchSetVariable(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        variables: Map<String, BuildParameters>
    ) {
        val watch = Watcher(id = "batchSetVariable| $pipelineId| $buildId")
        watch.start("replaceOldByNewVar")

        val varMaps = variables.map {
            it.key to Pair(it.value.value.toString(), it.value.valueType ?: BuildFormPropertyType.STRING)
        }.toMap().toMutableMap()
        // tip： 移除掉旧变量，旧变量不入库
        PipelineVarUtil.replaceOldByNewVar(varMaps) // varMaps <= variables

        val pipelineBuildParameters = ArrayList<BuildParameters>(varMaps.size)
        varMaps.forEach { (key, valueAndType) ->
            // 不持久化的类型不保存
            if (valueAndType.second != BuildFormPropertyType.TEMPORARY) {
                pipelineBuildParameters.add(
                    BuildParameters(
                        key = key,
                        value = valueAndType.first,
                        valueType = valueAndType.second,
                        readOnly = variables[key]?.readOnly ?: false,
                        sensitive = variables[key]?.sensitive
                    )
                )
            }
        }
        // 入库前统一做硬上限校验：超 4M 直接抛错而不是默默丢数据
        pipelineBuildParameters.forEach { rejectIfHardOversize(buildId, it.key, it.value.toString()) }

        val redisLock = PipelineBuildVarLock(redisOperation, buildId)
        try {
            watch.start("getLock")
            // 加锁防止数据被重复插入
            redisLock.lock()
            watch.start("getVars")
            val buildVarMap = pipelineBuildVarDao.getVars(dslContext, projectId, buildId)
            val insertBuildParameters = mutableListOf<BuildParameters>()
            val updateBuildParameters = mutableListOf<BuildParameters>()
            pipelineBuildParameters.forEach {
                if (!buildVarMap.containsKey(it.key)) {
                    insertBuildParameters.add(it)
                } else {
                    updateBuildParameters.add(it)
                }
            }
            // 1) 先把溢出值写入溢出表（无论 insert 还是 update 路径）
            watch.start("overflowSave")
            val overflowParams = pipelineBuildParameters
                .filter { BuildVarOverflowUtils.shouldOverflow(it.value.toString()) }
            if (overflowParams.isNotEmpty()) {
                pipelineBuildVarOverflowDao.batchSave(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    params = overflowParams
                )
            }
            // 2) 由小变大、由大变小的回退也要清理：把不再溢出的 key 从溢出表清掉
            val shrunkKeys = pipelineBuildParameters
                .filter { !BuildVarOverflowUtils.shouldOverflow(it.value.toString()) }
                .map { it.key }
            if (shrunkKeys.isNotEmpty()) {
                shrunkKeys.forEach { key ->
                    pipelineBuildVarOverflowDao.deleteByKey(dslContext, projectId, buildId, key)
                }
            }
            watch.start("batchSave")
            pipelineBuildVarDao.batchSave(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                variables = insertBuildParameters
            )
            watch.start("batchUpdate")
            pipelineBuildVarDao.batchUpdate(
                dslContext = dslContext,
                projectId = projectId,
                buildId = buildId,
                variables = updateBuildParameters
            )
        } finally {
            redisLock.unlock()
            LogUtils.printCostTimeWE(watch)
        }
    }

    // #10082 查询Agent复用互斥使用的AgentId
    fun fetchAgentReuseMutexVar(
        projectId: String,
        buildId: String,
        likeStr: String
    ): Set<String> {
        return pipelineBuildVarDao.fetchVarByLikeKey(
            dslContext = commonDslContext,
            projectId = projectId,
            buildId = buildId,
            readOnly = true,
            likeStr = likeStr
        )
    }

    private fun persistOverflowIfNeeded(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        key: String,
        rawValue: String,
        readOnly: Boolean?
    ) {
        if (!BuildVarOverflowUtils.shouldOverflow(rawValue)) return
        pipelineBuildVarOverflowDao.save(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            param = BuildParameters(
                key = key,
                value = rawValue,
                readOnly = readOnly
            )
        )
    }

    /**
     * 便捷方法：基于 [getVariableSnapshot] 构造一个可直接喂给
     * [com.tencent.devops.common.pipeline.EnvReplacementParser.parse] 的"快照入参组"。
     *
     * 调用方在替换大量片段时可以一次构造、多次复用，避免每次都查询溢出表。
     */
    fun buildExpressionParseInputs(
        projectId: String,
        pipelineId: String,
        buildId: String
    ): ExpressionParseInputs {
        val snapshot = getVariableSnapshot(projectId, pipelineId, buildId)
        return ExpressionParseInputs(
            contextMap = snapshot.smallVars,
            overflowKeys = snapshot.largeKeys,
            overflowLoader = snapshot.largeValueLoader
        )
    }

    /** 表达式替换的标准入参组。 */
    data class ExpressionParseInputs(
        val contextMap: Map<String, String>,
        val overflowKeys: Set<String>,
        val overflowLoader: (String) -> String?
    )

    private fun rejectIfHardOversize(buildId: String, key: String, rawValue: String) {
        if (rawValue.length <= BuildVarOverflowUtils.HARD_MAX_LENGTH) return
        // 直接 WARN，让上游决定是抛异常还是降级。这里采用降级策略：
        // 截断到硬上限，避免单条变量值撑爆 mediumtext。
        LOG.warn(
            "$buildId|VAR_HARD_OVERSIZE|key=$key|len=${rawValue.length}|" +
                "hardMax=${BuildVarOverflowUtils.HARD_MAX_LENGTH}, will be truncated"
        )
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(BuildVariableService::class.java)
    }
}

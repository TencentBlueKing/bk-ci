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
    private val redisOperation: RedisOperation,
    private val pipelineVarOverflowConfig: PipelineVarOverflowConfig
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
     * 将模板语法中的[template]模板字符串替换成当前构建[buildId]下对应的真正的字符串。
     *
     * 注意：旧风格 `${xxx}` 模板**不会**触发大变量懒加载——若变量是溢出值，
     * 这里看到的是引用串 `__BK_OVF__:<len>`。这与历史 `$xxx`/`${xxx}` 行为一致：
     * 旧语法不应感知到大值，避免脚本里直接展开 4M 内容造成日志爆炸 / OOM。
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
     * 与 [getAllVariable] 不同的是，本方法**始终**返回原始值（小变量真实值 + 大变量引用串）+
     * 溢出键集合 + 懒加载器。
     *
     * 仅在"需要按 ${{ xxx }} 表达式求值"的场景使用，普通查询继续使用 [getAllVariable]，
     * 不会改变现有行为与内存占用。
     *
     * 内存安全：
     *  - 返回的 [BuildVariableSnapshot.largeValueLoader] 由 [BuildVarOverflowLoader] 提供，
     *    具备 Caffeine 字符加权缓存与会话级总字节预算控制；
     *  - 调用方应将快照视为"会话级"对象，**不要**长期持有，避免持有缓存中的大变量。
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
            .filter { BuildVarOverflowUtils.isOverflowReference(it.value) }
            .map { it.key }
            .toSet()
        val loader = BuildVarOverflowLoader(
            overflowDao = pipelineBuildVarOverflowDao,
            dslContext = commonDslContext,
            projectId = projectId,
            buildId = buildId,
            maxCacheBytes = pipelineVarOverflowConfig.lazyLoadCacheMax,
            maxBudgetBytes = pipelineVarOverflowConfig.lazyLoadBudgetMax
        )
        return BuildVariableSnapshot(
            smallVars = dataMap,
            largeKeys = largeKeys,
            largeValueLoader = { key ->
                if (key !in largeKeys) {
                    dataMap[key]
                } else {
                    loader.load(key)
                }
            }
        )
    }

    /**
     * 单变量按需获取真实值（含溢出表）。
     * 主要供 ${{ xxx }} 表达式按需求值或 REST 单变量查询使用。
     *
     * 注：本方法每次调用都会新建一个 [BuildVarOverflowLoader]——只走单条查询，
     * 没有跨调用的缓存收益，但相应也不持有任何热数据；适合"零碎、单点"使用。
     * 若需要在一次会话中多次访问大变量，请改用 [getVariableSnapshot]。
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
        return if (BuildVarOverflowUtils.isOverflowReference(main)) {
            BuildVarOverflowLoader(
                overflowDao = pipelineBuildVarOverflowDao,
                dslContext = commonDslContext,
                projectId = projectId,
                buildId = buildId,
                maxCacheBytes = pipelineVarOverflowConfig.lazyLoadCacheMax,
                maxBudgetBytes = pipelineVarOverflowConfig.lazyLoadBudgetMax
            ).load(varName) ?: run {
                LOG.warn(
                    "$buildId|VAR_OVERFLOW_MISS|key=$varName|main=$main|" +
                        "overflow table has no value, keep reference"
                )
                main
            }
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
        // 超 4M：直接丢弃 + WARN，避免单条变量值撑爆 mediumtext / 内存
        if (!acceptWithinHardLimit(buildId, name, rawValue)) return
        val redisLock = PipelineBuildVarLock(redisOperation, buildId, name)
        try {
            redisLock.lock()
            // 1) 先把溢出值写入溢出表，再让主表写"引用"，保证一致性
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
        // 启动阶段先做硬上限过滤，避免把 >4M 的脏数据继续传下去
        val params = variables.values.filter { acceptWithinHardLimit(buildId, it.key, it.value.toString()) }
        if (params.isEmpty()) return
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
        // 入库前统一做硬上限校验：超 4M 直接丢弃并 WARN，避免堆积大对象在内存中
        val acceptedParameters = pipelineBuildParameters
            .filter { acceptWithinHardLimit(buildId, it.key, it.value.toString()) }
        if (acceptedParameters.isEmpty()) {
            LOG.warn("$buildId|batchSetVariable|all params dropped after hard-limit filter")
            return
        }

        val redisLock = PipelineBuildVarLock(redisOperation, buildId)
        try {
            watch.start("getLock")
            // 加锁防止数据被重复插入
            redisLock.lock()
            watch.start("getVars")
            val buildVarMap = pipelineBuildVarDao.getVars(dslContext, projectId, buildId)
            val insertBuildParameters = mutableListOf<BuildParameters>()
            val updateBuildParameters = mutableListOf<BuildParameters>()
            acceptedParameters.forEach {
                if (!buildVarMap.containsKey(it.key)) {
                    insertBuildParameters.add(it)
                } else {
                    updateBuildParameters.add(it)
                }
            }
            // 1) 先把溢出值写入溢出表（无论 insert 还是 update 路径）
            watch.start("overflowSave")
            val overflowParams = acceptedParameters
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
            val shrunkKeys = acceptedParameters
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
     * 单变量硬上限校验：超过 [PipelineVarOverflowConfig.hardMaxLength]（默认 4M，
     * 可由 `pipeline.variables.hardMaxLength` 动态调整）则**直接丢弃**。
     *
     * 设计取舍：
     *  - 抛异常会让一次 batch save 整体失败，影响构建；
     *  - 静默截断会让用户拿到错乱的数据；
     *  - 选择"丢弃 + WARN"更接近过去 `failIfVariableInvalid=false` 的语义，
     *    且与 worker 端 [com.tencent.devops.worker.common.task.TaskDaemon] 的过滤行为一致，
     *    也是防止 16G/Pod 内存被一条异常变量击穿的关键防线。
     *
     * @return true 表示通过校验可以继续保存；false 表示已被丢弃。
     */
    private fun acceptWithinHardLimit(buildId: String, key: String, rawValue: String): Boolean {
        val hardMax = pipelineVarOverflowConfig.hardMaxLength
        if (rawValue.length <= hardMax) return true
        LOG.warn(
            "$buildId|VAR_HARD_OVERSIZE|key=$key|len=${rawValue.length}|" +
                "hardMax=$hardMax|dropped"
        )
        return false
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(BuildVariableService::class.java)
    }
}

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

import com.tencent.devops.common.api.constant.HIDDEN_SYMBOL
import com.tencent.devops.common.api.util.TemplateFastReplaceUtils
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.pipeline.dialect.PipelineDialectUtil
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.process.engine.control.lock.PipelineBuildVarLock
import com.tencent.devops.process.engine.dao.PipelineBuildVarDao
import com.tencent.devops.process.utils.BuildVarOverflowUtils
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_DIALECT
import com.tencent.devops.process.utils.PIPELINE_NAME
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
    private val pipelineAsCodeService: PipelineAsCodeService,
    private val redisOperation: RedisOperation,
    private val pipelineVarOverflowConfig: PipelineVarOverflowConfig,
    private val buildVarOverflowStorage: BuildVarOverflowStorage
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
     * 注意：旧风格 `${xxx}` 模板**不会**触发大变量下载——若变量是大变量，
     * 这里看到的是引用串 `__BK_OVF__:<len>:<executeCount>:<sha256>`。
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
        // 单变量查询只按 key 精准查，避免把一次构建的全部变量都拉出来（历史实现会全量加载再取一个 key）。
        // 落库统一使用新变量名，这里把调用方可能传入的旧变量名 / 预置上下文名映射成落库 key 后再查，
        // 与 mixOldVarAndNewVar 的取值口径保持一致，不改变对旧命名（含前缀映射）的兼容能力。
        val storedKey = PipelineVarUtil.resolveStoredVarKey(varName)
        val dataMap = pipelineBuildVarDao.getVars(
            dslContext = commonDslContext,
            projectId = projectId,
            buildId = buildId,
            keys = setOf(varName, storedKey)
        )
        return dataMap[varName] ?: dataMap[storedKey]
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
     * 新建一个会话级大变量加载器，见 [BuildVarOverflowLoader]。
     */
    fun newOverflowLoader(
        projectId: String,
        pipelineId: String,
        buildId: String,
        userId: String? = null
    ) = BuildVarOverflowLoader(
        storage = buildVarOverflowStorage,
        userId = userId,
        projectId = projectId,
        pipelineId = pipelineId,
        buildId = buildId,
        maxCacheBytes = pipelineVarOverflowConfig.lazyLoadCacheMax,
        maxBudgetBytes = pipelineVarOverflowConfig.lazyLoadBudgetMax
    )

    /**
     * 单变量按需获取真实值：大变量从制品库下载，下载失败时退化为引用串。
     * 若需要在一次会话中多次访问大变量，请改用 [newOverflowLoader]。
     *
     * @param reference 指定要读的大变量引用串（来自构建日志的 `output(overflow)` 行），
     *   用于按**执行次数**回看内容，见 [resolveOverflowTarget]；为空或格式非法时按 [varName] 取当前值
     * @param maskSensitive 面向用户展示时传 true，敏感变量返回 [HIDDEN_SYMBOL]
     */
    @Suppress("LongParameterList")
    fun getVariableValue(
        projectId: String,
        pipelineId: String,
        buildId: String,
        varName: String,
        reference: String? = null,
        queryDslContext: DSLContext? = null,
        maskSensitive: Boolean = false
    ): String? {
        // 落库统一新变量名，兼容旧名 / 预置上下文名（含前缀映射）后单点查询，避免漏查。
        val storedKey = PipelineVarUtil.resolveStoredVarKey(varName)
        val queryContext = queryDslContext ?: commonDslContext
        val stored = if (maskSensitive) {
            val param = sequenceOf(varName, storedKey).distinct().firstNotNullOfOrNull { key ->
                pipelineBuildVarDao.getVarsWithType(queryContext, projectId, buildId, key).firstOrNull()
            }
            if (param?.sensitive == true) return HIDDEN_SYMBOL
            param?.value?.toString()
        } else {
            val varMap = pipelineBuildVarDao.getVars(
                dslContext = queryContext,
                projectId = projectId,
                buildId = buildId,
                keys = setOf(varName, storedKey)
            )
            varMap[varName] ?: varMap[storedKey]
        }
        val value = resolveOverflowTarget(reference = reference, stored = stored) ?: return null
        if (!BuildVarOverflowUtils.isOverflowReference(value)) return value
        return newOverflowLoader(projectId = projectId, pipelineId = pipelineId, buildId = buildId)
            .load(value) ?: run {
            LOG.warn("$buildId|LARGE_VAR_MISS|key=$varName|ref=$value|keep reference")
            value
        }
    }

    /**
     * 决定这次要读的值：给了合法引用串就按它读，否则读变量表里的当前值。
     *
     * 引用串自带执行次数与内容摘要，文件路径完全由它拼出。变量表每个 key 只保留最后一次写入的值，
     * 重试会把它覆盖掉，分区表的保留期也可能短于构建记录；而构建日志按执行次数分开保存，
     * 引用串就印在那一次的 `output(overflow)` 行里。所以认引用串既能按执行次数回看，
     * 也能在变量表那一行已经不在时仍然取到内容。
     *
     * 越权读不成立：文件只在**本次构建目录**下解析，而本接口已校验调用者对该流水线有查看权限
     * （见 `BuildVarOverflowStorage.download`、`BuildVarOverflowUtils.blobPath`）。
     * 敏感变量也拿不到：敏感值从不上传（见 [toStoredValue]），且上面已先按变量名做过脱敏判定。
     */
    private fun resolveOverflowTarget(reference: String?, stored: String?): String? {
        if (reference != null && BuildVarOverflowUtils.parseReference(reference) != null) return reference
        return stored
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
        // 大变量上传是网络 IO，放在事务外完成，避免长时间占用数据库连接
        val parameters = externalizeLargeValues(
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
        if (parameters.isEmpty()) return
        commonDslContext.transaction { t ->
            val context = DSL.using(t)
            batchSetVariable(
                dslContext = context,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                variables = parameters
            )
        }
    }

    /**
     * will delete the [buildId] 's all writable vars
     */
    fun deleteWritableVars(dslContext: DSLContext, projectId: String, buildId: String) {
        pipelineBuildVarDao.deleteBuildVar(
            dslContext = commonDslContext,
            projectId = projectId,
            buildId = buildId,
            varName = null,
            readOnly = false
        )
    }

    fun deleteBuildVars(projectId: String, pipelineId: String, buildId: String) {
        pipelineBuildVarDao.deleteBuildVars(
            dslContext = commonDslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId
        )
    }

    fun deletePipelineBuildVar(projectId: String, pipelineId: String) {
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
        val storedValue = toStoredValue(
            LargeValue(
                buildId = buildId,
                key = name,
                rawValue = value.toString(),
                scope = { uploadScope(projectId, pipelineId, buildId) }
            )
        ) ?: return
        val redisLock = PipelineBuildVarLock(redisOperation, buildId, name)
        try {
            redisLock.lock()
            val varMap = pipelineBuildVarDao.getVars(dslContext, projectId, buildId, setOf(name))
            if (varMap.isEmpty()) {
                pipelineBuildVarDao.save(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    name = name,
                    value = storedValue,
                    readOnly = readOnly
                )
            } else {
                pipelineBuildVarDao.update(
                    dslContext = dslContext,
                    projectId = projectId,
                    buildId = buildId,
                    name = name,
                    value = storedValue,
                    rewriteReadOnly = rewriteReadOnly
                )
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
        pipelineBuildVarDao.batchSave(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            variables = variables.map { it.value }
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

    /**
     * 把超过 4K 的值上传制品库并替换为引用串；无法外置的值被丢弃。
     * 未超过 4K 的值（含已是引用串的值）原样返回，因此可安全地重复调用。
     */
    private fun externalizeLargeValues(
        projectId: String,
        pipelineId: String,
        buildId: String,
        variables: Map<String, BuildParameters>
    ): Map<String, BuildParameters> {
        if (variables.values.none { BuildVarOverflowUtils.shouldOverflow(it.value.toString()) }) return variables
        val target by lazy {
            uploadScope(projectId = projectId, pipelineId = pipelineId, buildId = buildId)
        }
        val result = LinkedHashMap<String, BuildParameters>(variables.size)
        variables.forEach { (key, param) ->
            val raw = param.value.toString()
            if (!BuildVarOverflowUtils.shouldOverflow(raw)) {
                result[key] = param
                return@forEach
            }
            toStoredValue(
                LargeValue(
                    buildId = buildId,
                    key = param.key,
                    rawValue = raw,
                    scope = { target }
                )
            )?.let {
                result[key] = param.copy(value = it)
            }
        }
        return result
    }

    /**
     * 待入库的变量。 [scope] 只在真正上传时求值，避免短值也去查执行次数。
     */
    private class LargeValue(
        val buildId: String,
        val key: String,
        val rawValue: String,
        val scope: () -> BuildVarOverflowStorage.UploadScope
    )

    /**
     * 计算变量入库值：≤ 4K 原样返回；> 4K 上传制品库后返回引用串；
     * 制品库非 bkrepo、超过 256KB 或上传失败时返回 null（丢弃 + WARN，与历史 4K 截断行为一致）。
     *
     * 敏感值同样走制品库，否则超长的敏感变量传不到后续插件。脱敏不依赖「不上传」：
     * 变量表记下 SENSITIVE 后，[getVariableValue] 在下载之前就返回 [HIDDEN_SYMBOL]，
     * 构建日志侧由 Worker 的 SensitiveValueService 覆盖。
     */
    private fun toStoredValue(value: LargeValue): String? {
        if (!BuildVarOverflowUtils.shouldOverflow(value.rawValue)) return value.rawValue
        val reason = when {
            !buildVarOverflowStorage.isEnabled() -> "storageDisabled"
            BuildVarOverflowUtils.exceedsLargeValueLimit(
                value.rawValue,
                pipelineVarOverflowConfig.effectiveLargeValueMaxBytes()
            ) -> "exceedsLargeValueLimit"
            else -> null
        }
        if (reason == null) {
            try {
                return buildVarOverflowStorage.upload(scope = value.scope(), value = value.rawValue)
            } catch (ignored: Exception) {
                LOG.warn(
                    "${value.buildId}|LARGE_VAR_UPLOAD_FAIL|key=${value.key}|len=${value.rawValue.length}",
                    ignored
                )
            }
        }
        LOG.warn(
            "${value.buildId}|ABANDON_DATA|key=${value.key}|len=${value.rawValue.length}|" +
                "reason=${reason ?: "uploadFail"}"
        )
        return null
    }

    /** 一次查询取齐上传所需的执行次数（与 [getBuildExecuteCount] 口径一致）、流水线名与构建号。 */
    private fun uploadScope(
        projectId: String,
        pipelineId: String,
        buildId: String
    ): BuildVarOverflowStorage.UploadScope {
        val vars = pipelineBuildVarDao.getVars(
            dslContext = commonDslContext,
            projectId = projectId,
            buildId = buildId,
            keys = setOf(PIPELINE_RETRY_COUNT, PIPELINE_NAME, PIPELINE_BUILD_NUM)
        )
        val retryCount = vars[PIPELINE_RETRY_COUNT]
        val executeCount = if (NumberUtils.isParsable(retryCount)) {
            (retryCount!!.toIntOrNull() ?: 0) + 1
        } else {
            1
        }
        return BuildVarOverflowStorage.UploadScope(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            executeCount = executeCount,
            pipelineName = vars[PIPELINE_NAME],
            buildNum = vars[PIPELINE_BUILD_NUM]
        )
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(BuildVariableService::class.java)
    }
}

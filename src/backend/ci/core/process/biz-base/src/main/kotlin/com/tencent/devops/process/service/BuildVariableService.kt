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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.util.TemplateFastReplaceUtils
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.process.engine.control.lock.PipelineBuildVarLock
import com.tencent.devops.process.engine.dao.PipelineBuildVarDao
import com.tencent.devops.process.utils.PIPELINE_RETRY_COUNT
import com.tencent.devops.process.utils.PipelineVarUtil
import org.apache.commons.lang3.math.NumberUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("TooManyFunctions")
class BuildVariableService @Autowired constructor(
    private val commonDslContext: DSLContext,
    private val pipelineBuildVarDao: PipelineBuildVarDao,
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
            val word = PipelineVarUtil.oldVarToNewVar(templateWord) ?: templateWord
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
        buildId: String
    ): Map<String, String> {
        return if (pipelineAsCodeService.asCodeEnabled(projectId, pipelineId) == true) {
            pipelineBuildVarDao.getVars(commonDslContext, projectId, buildId)
        } else {
            PipelineVarUtil.mixOldVarAndNewVar(pipelineBuildVarDao.getVars(commonDslContext, projectId, buildId))
        }
    }

    fun getAllVariableWithType(projectId: String, buildId: String): List<BuildParameters> {
        return pipelineBuildVarDao.getVarsWithType(commonDslContext, projectId, buildId)
    }

    fun setVariable(projectId: String, pipelineId: String, buildId: String, varName: String, varValue: Any) {
        val realVarName = PipelineVarUtil.oldVarToNewVar(varName) ?: varName
        saveVariable(
            dslContext = commonDslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            name = realVarName,
            value = varValue
        )
    }

    fun batchUpdateVariable(projectId: String, pipelineId: String, buildId: String, variables: Map<String, Any>) {
        commonDslContext.transaction { t ->
            val context = DSL.using(t)
            batchSetVariable(
                dslContext = context,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                variables = variables.map { va ->
                    va.key to BuildParameters(key = va.key, value = va.value, valueType = BuildFormPropertyType.STRING)
                }.toMap()
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
        value: Any
    ) {
        val redisLock = PipelineBuildVarLock(redisOperation, buildId, name)
        try {
            redisLock.lock()
            val varMap = pipelineBuildVarDao.getVars(dslContext, projectId, buildId, name)
            if (varMap.isEmpty()) {
                pipelineBuildVarDao.save(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    name = name,
                    value = value
                )
            } else {
                pipelineBuildVarDao.update(
                    dslContext = dslContext,
                    projectId = projectId,
                    buildId = buildId,
                    name = name,
                    value = value
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
                        readOnly = variables[key]?.readOnly ?: false
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
}

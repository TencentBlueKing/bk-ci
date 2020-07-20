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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.service

import com.tencent.devops.common.api.util.EmojiUtil
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.dao.PipelineBuildVarDao
import com.tencent.devops.process.utils.PipelineVarUtil
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BuildVariableService @Autowired constructor(
    private val commonDslContext: DSLContext,
    private val pipelineBuildVarDao: PipelineBuildVarDao,
    private val redisOperation: RedisOperation
) {

    companion object {
        private const val PIPELINE_BUILD_VAR_KEY = "pipelineBuildVar"
    }

    fun getVariable(buildId: String, varName: String): String? {
        val vars = getAllVariable(buildId)
        return if (vars.isNotEmpty()) vars[varName] else null
    }

    fun getAllVariable(buildId: String): Map<String, String> {
        return PipelineVarUtil.mixOldVarAndNewVar(pipelineBuildVarDao.getVars(commonDslContext, buildId))
    }

    fun getAllVariableWithType(buildId: String): List<BuildParameters> {
        return pipelineBuildVarDao.getVarsWithType(commonDslContext, buildId)
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

    fun batchSetVariable(projectId: String, pipelineId: String, buildId: String, variables: Map<String, Any>) =
        batchSetVariable(commonDslContext, projectId, pipelineId, buildId, variables)

    fun deletePipelineBuildVar(projectId: String, pipelineId: String) {
        pipelineBuildVarDao.deletePipelineBuildVar(
            dslContext = commonDslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
    }

    // 保存方法需要提供事务保护的实现，传入特定dslContext
    fun saveVariable(
        dslContext: DSLContext,
        buildId: String,
        projectId: String,
        pipelineId: String,
        name: String,
        value: Any
    ) {
        val redisLock = RedisLock(redisOperation, "$PIPELINE_BUILD_VAR_KEY:$buildId:$name", 10)
        try {
            redisLock.lock()
            val varMap = pipelineBuildVarDao.getVars(dslContext, buildId, name)
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
                    buildId = buildId,
                    name = name,
                    value = value
                )
            }
        } finally {
            redisLock.unlock()
        }
    }

    fun batchSetVariable(dslContext: DSLContext, projectId: String, pipelineId: String, buildId: String, variables: Map<String, Any>) {
        val vars = variables.map { it.key to it.value.toString() }.toMap().toMutableMap()
        PipelineVarUtil.replaceOldByNewVar(vars)

        val pipelineBuildParameters = mutableListOf<BuildParameters>()
        vars.forEach { (t, u) -> pipelineBuildParameters.add(BuildParameters(key = t, value = EmojiUtil.removeAllEmoji(u))) }
        val redisLock = RedisLock(redisOperation, "$PIPELINE_BUILD_VAR_KEY:$buildId", 60)
        try {
            // 加锁防止数据被重复插入
            redisLock.lock()
            val buildVarMap = pipelineBuildVarDao.getVars(dslContext, buildId)
            val insertBuildParameters = mutableListOf<BuildParameters>()
            val updateBuildParameters = mutableListOf<BuildParameters>()
            pipelineBuildParameters.forEach {
                if (!buildVarMap.containsKey(it.key)) {
                    insertBuildParameters.add(it)
                } else {
                    updateBuildParameters.add(it)
                }
            }
            dslContext.transaction { t ->
                val context = DSL.using(t)
                pipelineBuildVarDao.batchSave(
                    dslContext = context,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    variables = insertBuildParameters
                )
                pipelineBuildVarDao.batchUpdate(
                    dslContext = context,
                    buildId = buildId,
                    variables = updateBuildParameters
                )
            }
        } finally {
            redisLock.unlock()
        }
    }
}
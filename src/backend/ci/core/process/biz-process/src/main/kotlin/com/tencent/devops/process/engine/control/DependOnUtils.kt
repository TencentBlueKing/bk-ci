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

package com.tencent.devops.process.engine.control

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.DependOnType
import com.tencent.devops.common.pipeline.pojo.DependOnConfig
import com.tencent.devops.process.constant.ProcessMessageCode
import java.util.regex.Pattern
import javax.ws.rs.core.Response

object DependOnUtils {

    private val regex = Pattern.compile("[,;]")

    fun checkRepeatedJobId(
        stage: Stage
    ) {
        val jobIdSet = mutableSetOf<String>()
        stage.containers.forEach container@{ c ->
            val jobId = c.jobId
            if (jobId.isNullOrBlank()) {
                return@container
            }
            if (jobIdSet.contains(jobId)) {
                throw ErrorCodeException(
                    statusCode = Response.Status.CONFLICT.statusCode,
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_JOBID_EXIST,
                    params = arrayOf(c.jobId!!),
                    defaultMessage = "流水线jobId (${c.jobId})已存在"
                )
            }
            jobIdSet.add(jobId!!)
        }
    }

    /**
     * depnedOn jobId与containerId映射
     */
    fun initDependOn(
        stage: Stage,
        params: Map<String, Any>
    ) {
        val allJobId2JobMap = mutableMapOf<String, Container>()
        stage.containers.forEach container@{ c ->
            if (c.jobId.isNullOrBlank()) {
                return@container
            }
            allJobId2JobMap[c.jobId!!] = c
        }
        if (allJobId2JobMap.isEmpty()) {
            return
        }

        val cycleCheckJobMap = mutableMapOf<String, List<String>>()
        stage.containers.forEach container@{ c ->
            val jobControlOption = when (c) {
                is VMBuildContainer -> c.jobControlOption
                is NormalContainer -> c.jobControlOption
                else -> null
            } ?: return@container
            val dependOnJobIds = getDependOnJobIds(
                dependOnConfig = DependOnConfig(
                    dependOnType = jobControlOption.dependOnType,
                    dependOnId = jobControlOption.dependOnId,
                    dependOnName = jobControlOption.dependOnName
                ),
                params = params.map { it.key to it.value.toString() }.toMap()
            )
            if (dependOnJobIds.isEmpty()) {
                return@container
            }
            if (!c.jobId.isNullOrBlank()) {
                cycleCheckJobMap[c.jobId!!] = dependOnJobIds
            }
            val containerId2JobIds = mutableMapOf<String, String>()
            dependOnJobIds.forEach {
                val dependOnJob = allJobId2JobMap[it] ?: return@forEach
                containerId2JobIds[dependOnJob.id!!] = it
            }
            if (containerId2JobIds.isNotEmpty()) {
                jobControlOption.dependOnContainerId2JobIds = containerId2JobIds
            }
        }

        // 校验是否循环依赖
        val visited = mutableMapOf<String, Int>()
        cycleCheckJobMap.keys.forEach {
            DSF(it, cycleCheckJobMap, visited)
        }
    }

    private fun getDependOnJobIds(
        dependOnConfig: DependOnConfig,
        params: Map<String, String>
    ): List<String> {
        with(dependOnConfig) {
            return when (dependOnType) {
                DependOnType.ID -> {
                    if (dependOnId == null || dependOnId!!.isEmpty())
                        return listOf()
                    dependOnConfig.dependOnId!!
                }
                DependOnType.NAME -> {
                    if (dependOnName.isNullOrBlank()) return listOf()
                    val dependONames = dependOnName!!.split(regex)
                    dependONames.map {
                        EnvUtils.parseEnv(it, params)
                    }
                } else ->
                    listOf()
            }
        }
    }

    /**
     * visited: key为jobId,value: 0-未访问,1-正在访问,2-已经访问
     */
    private fun DSF(jobId: String, dependOnMap: Map<String, List<String>>, visited: MutableMap<String, Int>): Boolean {
        if (visited[jobId] == 1) {
            return true
        }
        if (visited[jobId] == 2) {
            return false
        }

        visited[jobId] = 1
        dependOnMap[jobId]?.forEach {
            if (DSF(it, dependOnMap, visited)) {
                throw ErrorCodeException(
                    defaultMessage = "($jobId)与($it)循环依赖",
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_DEPENDON_CYCLE
                )
            }
        }
        visited[jobId] = 2
        return false
    }
}
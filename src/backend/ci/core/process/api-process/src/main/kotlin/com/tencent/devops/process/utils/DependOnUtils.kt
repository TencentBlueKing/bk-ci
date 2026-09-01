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

package com.tencent.devops.process.utils

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.DependOnType
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.pojo.DependOnConfig
import com.tencent.devops.process.constant.ProcessMessageCode
import java.util.regex.Pattern
import jakarta.ws.rs.core.Response

@Suppress("ALL")
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
                val jobName = getContainerName(stage = stage, container = c, jobId = jobId)
                throw ErrorCodeException(
                    statusCode = Response.Status.CONFLICT.statusCode,
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_JOBID_EXIST,
                    params = arrayOf(jobName, c.jobId!!),
                    defaultMessage = "$jobName 的jobId(${c.jobId})已存在"
                )
            }
            jobIdSet.add(jobId)
        }
        removeNonexistentJob(
            stage = stage,
            jobIdSet = jobIdSet
        )
    }

    /**
     * 移除不存在的jobId
     * 如果已选择的jobId名字被修改，前端并不会把job删除,而是不展示，需要后端将不存在的jobId删除
     */
    private fun removeNonexistentJob(
        stage: Stage,
        jobIdSet: Set<String>
    ) {
        stage.containers.forEach container@{ c ->
            val jobControlOption = when (c) {
                is VMBuildContainer -> c.jobControlOption
                is NormalContainer -> c.jobControlOption
                else -> null
            } ?: return@container
            val isEmpty = jobControlOption.dependOnId?.isEmpty() ?: true
            if (jobControlOption.dependOnType != DependOnType.ID || isEmpty) {
                return@container
            }
            val existJobIds = jobControlOption.dependOnId!!.filter { jobIdSet.contains(it) }
            jobControlOption.dependOnId = existJobIds
        }
    }

    /**
     * dependOn jobId与containerId映射
     * 前端通过jobId声明依赖关系,流水线真正运行是通过containerId
     */
    fun initDependOn(stage: Stage, params: Map<String, String>) {
        val allJobId2JobMap = mutableMapOf<String, Container>()
        val jobs = mutableListOf<DependOnJob>()
        stage.containers.forEach container@{ c ->
            if (!c.jobId.isNullOrBlank()) {
                allJobId2JobMap[c.jobId!!] = c
            }
            val jobControlOption = when (c) {
                is VMBuildContainer -> c.jobControlOption
                is NormalContainer -> c.jobControlOption
                else -> null
            } ?: return@container
            val containerId = c.id ?: return@container
            jobs.add(
                DependOnJob(
                    jobId = c.jobId,
                    containerId = containerId,
                    jobControlOption = jobControlOption
                )
            )
        }
        initDependOn(
            jobs = jobs,
            params = params,
            displayName = { jobId ->
                getContainerName(stage = stage, container = allJobId2JobMap[jobId], jobId = jobId)
            }
        )
    }

    /**
     * 按给定变量解析 dependOn，并做循环依赖校验。
     * 解析不到任何已存在 jobId 时会清空旧映射，避免沿用启动期过期结果。
     */
    fun initDependOn(
        jobs: List<DependOnJob>,
        params: Map<String, String>,
        displayName: (String) -> String = { it }
    ) {
        val allJobId2JobMap = jobs.filter { !it.jobId.isNullOrBlank() }.associateBy { it.jobId!! }
        if (allJobId2JobMap.isEmpty()) {
            return
        }

        val cycleCheckJobMap = mutableMapOf<String, List<String>>()
        jobs.forEach job@{ job ->
            val dependOnJobIds = getDependOnJobIds(
                dependOnConfig = DependOnConfig(
                    dependOnType = job.jobControlOption.dependOnType,
                    dependOnId = job.jobControlOption.dependOnId,
                    dependOnName = job.jobControlOption.dependOnName
                ),
                params = params
            )
            if (dependOnJobIds.isEmpty()) {
                return@job
            }
            if (!job.jobId.isNullOrBlank()) {
                cycleCheckJobMap[job.jobId!!] = dependOnJobIds
            }
            val containerId2JobIds = mutableMapOf<String, String>()
            dependOnJobIds.forEach { dependOnJobId ->
                val dependOnJob = allJobId2JobMap[dependOnJobId] ?: return@forEach
                containerId2JobIds[dependOnJob.containerId] = dependOnJobId
            }
            job.jobControlOption.dependOnContainerId2JobIds = containerId2JobIds.takeIf { it.isNotEmpty() }
        }

        val visited = mutableMapOf<String, Int>()
        cycleCheckJobMap.keys.forEach { jobId ->
            dsf(
                jobId = jobId,
                dependOnMap = cycleCheckJobMap,
                visited = visited,
                displayName = displayName
            )
        }
    }

    fun enableDependOn(container: Container): Boolean {
        val jobControlOption = when (container) {
            is VMBuildContainer -> container.jobControlOption
            is NormalContainer -> container.jobControlOption
            else -> null
        } ?: return false
        return enableDependOn(jobControlOption)
    }

    fun enableDependOn(jobControlOption: JobControlOption): Boolean {
        return when (jobControlOption.dependOnType) {
            DependOnType.ID ->
                jobControlOption.dependOnId != null && jobControlOption.dependOnId!!.isNotEmpty()
            DependOnType.NAME ->
                jobControlOption.dependOnName?.isNotEmpty() ?: false
            else ->
                false
        }
    }

    /**
     * 判断[container]是否直接或间接依赖[targetContainerId]所指的Job。
     * dependOn关系只在同一个Stage内声明，因此只在[stage]范围内做传递闭包搜索。
     * 依赖映射由[initDependOn]刷新，调用前需确保其已执行。
     */
    fun dependOnContainer(stage: Stage, container: Container, targetContainerId: String): Boolean {
        if (container.id == targetContainerId) {
            return false
        }
        val containerMap = stage.containers.filter { !it.id.isNullOrBlank() }.associateBy { it.id!! }
        val visited = mutableSetOf<String>()
        val pending = mutableListOf(container)
        while (pending.isNotEmpty()) {
            val current = pending.removeAt(pending.size - 1)
            val currentId = current.id
            // initDependOn已做循环依赖校验，这里的visited仅作兜底防止意外死循环
            if (currentId.isNullOrBlank() || !visited.add(currentId)) {
                continue
            }
            val dependOnContainerIds = getDependOnContainerIds(current)
            if (dependOnContainerIds.contains(targetContainerId)) {
                return true
            }
            dependOnContainerIds.forEach { id -> containerMap[id]?.let { pending.add(it) } }
        }
        return false
    }

    private fun getDependOnContainerIds(container: Container): Set<String> {
        val jobControlOption = when (container) {
            is VMBuildContainer -> container.jobControlOption
            is NormalContainer -> container.jobControlOption
            else -> null
        } ?: return emptySet()
        return jobControlOption.dependOnContainerId2JobIds?.keys ?: emptySet()
    }

    private fun getDependOnJobIds(dependOnConfig: DependOnConfig, params: Map<String, String>): List<String> {
        return when (dependOnConfig.dependOnType) {
            DependOnType.ID -> {
                if (dependOnConfig.dependOnId == null || dependOnConfig.dependOnId!!.isEmpty()) {
                    listOf()
                } else {
                    dependOnConfig.dependOnId!!
                }
            }
            DependOnType.NAME -> {
                if (dependOnConfig.dependOnName.isNullOrBlank()) {
                    listOf()
                } else {
                    val dependONames = dependOnConfig.dependOnName!!.split(regex)
                    dependONames.map { EnvUtils.parseEnv(it, params) }
                }
            }
            else -> listOf()
        }
    }

    /**
     * visited: key为jobId,value: 0-未访问,1-正在访问,2-已经访问
     */
    private fun dsf(
        jobId: String,
        dependOnMap: Map<String, List<String>>,
        visited: MutableMap<String, Int>,
        displayName: (String) -> String
    ): Boolean {
        if (visited[jobId] == 1) {
            return true
        }
        if (visited[jobId] == 2) {
            return false
        }

        visited[jobId] = 1
        dependOnMap[jobId]?.forEach { dependOnJobId ->
            if (dsf(
                    jobId = dependOnJobId,
                    dependOnMap = dependOnMap,
                    visited = visited,
                    displayName = displayName
                )
            ) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_DEPENDON_CYCLE,
                    params = arrayOf(displayName(jobId), displayName(dependOnJobId))
                )
            }
        }
        visited[jobId] = 2
        return false
    }

    private fun getContainerName(stage: Stage, container: Container?, jobId: String): String {
        if (container == null) {
            return jobId
        }
        val namePrefix = stage.name?.removePrefix("stage-")
        return "$namePrefix-${container.id}"
    }
}

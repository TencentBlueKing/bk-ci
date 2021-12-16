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

import com.tencent.devops.common.client.pojo.enums.GatewayType
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_TIME_TRIGGER_KIND
import com.tencent.devops.process.engine.control.ControlUtils
import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.process.utils.PIPELINE_START_TYPE
import com.tencent.devops.process.utils.PipelineVarUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ComplexMethod", "TooManyFunctions", "NestedBlockDepth")
@Service
class PipelineContextService @Autowired constructor(
    private val pipelineBuildDetailService: PipelineBuildDetailService
) {
    private val logger = LoggerFactory.getLogger(PipelineContextService::class.java)

    fun buildContext(
        buildId: String,
        containerId: String?,
        buildVar: Map<String, String>,
    ): Map<String, String> {
        val modelDetail = pipelineBuildDetailService.get(buildId) ?: return emptyMap()
        val varMap = mutableMapOf<String, String>()
        try {
            modelDetail.model.stages.forEach { stage ->
                stage.containers.forEach { container ->
                    // containers
                    buildJobContext(container, containerId, varMap, stage)
                    // steps
                    buildStepContext(container, varMap, buildVar)
                    // groupContainer
                    container.fetchGroupContainers()?.forEach { c ->
                        // containers
                        buildJobContext(c, containerId, varMap, stage)
                        // steps
                        buildStepContext(c, varMap, buildVar)
                    }
                }
            }
            buildCiContext(varMap, buildVar)
        } catch (ignore: Throwable) {
            logger.warn("BKSystemErrorMonitor|buildContextFailed|", ignore)
        }

        return varMap
    }

    fun getAllBuildContext(buildVar: Map<String, String>): Map<String, String> {
        val allContext = buildVar.toMutableMap()
        // 将流水线变量按预置映射关系做替换
        PipelineVarUtil.fillContextVarMap(allContext, buildVar)
        return allContext
    }

    fun getBuildContext(buildVar: Map<String, String>, contextName: String): String? {
        return PipelineVarUtil.fetchContextInBuildVars(contextName, buildVar)
    }

    fun getBuildVarName(contextName: String): String? {
        return PipelineVarUtil.fetchVarName(contextName)
    }

    private fun buildCiContext(
        varMap: MutableMap<String, String>,
        buildVar: Map<String, String>
    ) {
        // 将流水线变量按预置映射关系做替换
        PipelineVarUtil.fillContextVarMap(varMap, buildVar)

        // 特殊处理触发类型以免定时触发无法记录
        if (buildVar[PIPELINE_START_TYPE] == StartType.TIME_TRIGGER.name) {
            varMap["ci.event"] = PIPELINE_GIT_TIME_TRIGGER_KIND
        } else if (!buildVar[PIPELINE_GIT_EVENT].isNullOrBlank()) {
            varMap["ci.event"] = buildVar[PIPELINE_GIT_EVENT]!!
        }
    }

    private fun buildStepContext(
        c: Container,
        varMap: MutableMap<String, String>,
        buildVar: Map<String, String>
    ) {
        c.elements.forEach { e ->
            val stepId = e.stepId ?: return@forEach
            varMap["steps.$stepId.name"] = e.name
            varMap["steps.$stepId.id"] = e.id ?: ""
            varMap["steps.$stepId.status"] = getStepStatus(e)
            varMap["steps.$stepId.outcome"] = e.status ?: ""
            val jobId = c.jobId ?: return@forEach
            varMap.putAll(getStepOutput(jobId, stepId, buildVar))
            varMap["jobs.$jobId.steps.$stepId.name"] = e.name
            varMap["jobs.$jobId.steps.$stepId.id"] = e.id ?: ""
            varMap["jobs.$jobId.steps.$stepId.status"] = getStepStatus(e)
            varMap["jobs.$jobId.steps.$stepId.outcome"] = e.status ?: ""
        }
    }

    private fun buildJobContext(
        c: Container,
        containerId: String?,
        varMap: MutableMap<String, String>,
        stage: Stage
    ) {
        // current job
        if (c.id?.let { it == containerId } == true) {
            varMap["job.id"] = c.jobId ?: ""
            varMap["job.name"] = c.name
            varMap["job.status"] = getJobStatus(c)
            varMap["job.outcome"] = c.status ?: ""
            varMap["job.container.network"] = getNetWork(c) ?: ""
            varMap["job.stage_id"] = stage.id ?: ""
            varMap["job.stage_name"] = stage.name ?: ""
        }

        // other job
        val jobId = c.jobId ?: return
        varMap["jobs.$jobId.id"] = jobId
        varMap["jobs.$jobId.name"] = c.name
        varMap["jobs.$jobId.status"] = getJobStatus(c)
        varMap["jobs.$jobId.outcome"] = c.status ?: ""
        varMap["jobs.$jobId.container.network"] = getNetWork(c) ?: ""
        varMap["jobs.$jobId.stage_id"] = stage.id ?: ""
        varMap["jobs.$jobId.stage_name"] = stage.name ?: ""
    }

    private fun getStepOutput(
        jobId: String,
        stepId: String,
        buildVar: Map<String, String>,
    ): Map<out String, String> {
        val outputMap = mutableMapOf<String, String>()
        buildVar.filterKeys { it.contains("steps.$stepId.outputs.") }.forEach { (key, value) ->
            outputMap["jobs.$jobId.$key"] = value
        }
        buildVar.filterKeys { it.startsWith("jobs.$jobId.os") }.forEach { (_, value) ->
            outputMap["jobs.$jobId.os"] = value
        }
        return outputMap
    }

    private fun getNetWork(c: Container) = when (c) {
        is VMBuildContainer -> {
            if (c.dispatchType?.buildType() != BuildType.THIRD_PARTY_AGENT_ID &&
                c.dispatchType?.buildType() != BuildType.THIRD_PARTY_AGENT_ENV
            ) {
                GatewayType.DEVNET.name
            } else {
                GatewayType.IDC.name
            }
        }
        is NormalContainer -> {
            GatewayType.IDC.name
        }
        else -> null
    }

    private fun getJobStatus(c: Container): String {
        return if (c is VMBuildContainer && c.status == BuildStatus.FAILED.name) {
            if (c.jobControlOption?.continueWhenFailed == true) {
                BuildStatus.SUCCEED.name
            } else {
                BuildStatus.FAILED.name
            }
        } else if (c is NormalContainer && c.status == BuildStatus.FAILED.name) {
            if (c.jobControlOption?.continueWhenFailed == true) {
                BuildStatus.SUCCEED.name
            } else {
                BuildStatus.FAILED.name
            }
        } else {
            c.status ?: ""
        }
    }

    private fun getStepStatus(e: Element): String {
        return if (e.status == BuildStatus.FAILED.name) {
            if (ControlUtils.continueWhenFailure(e.additionalOptions)) {
                BuildStatus.SUCCEED.name
            } else {
                BuildStatus.FAILED.name
            }
        } else {
            e.status ?: ""
        }
    }
}

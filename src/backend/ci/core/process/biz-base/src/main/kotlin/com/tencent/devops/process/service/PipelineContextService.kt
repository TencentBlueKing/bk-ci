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
        variables: Map<String, String>
    ): Map<String, String> {
        val modelDetail = pipelineBuildDetailService.get(buildId) ?: return emptyMap()
        val contextMap = mutableMapOf<String, String>()
        try {
            modelDetail.model.stages.forEach { stage ->
                stage.containers.forEach { container ->
                    buildJobContext(stage, container, containerId, contextMap, variables)
                    container.fetchGroupContainers()?.forEach { c ->
                        buildJobContext(stage, c, containerId, contextMap, variables)
                    }
                }
            }
            buildCiContext(contextMap, variables)
        } catch (ignore: Throwable) {
            logger.warn("BKSystemErrorMonitor|buildContextFailed|", ignore)
        }

        return contextMap
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

    private fun buildJobContext(
        stage: Stage,
        c: Container,
        containerId: String?,
        contextMap: MutableMap<String, String>,
        variables: Map<String, String>
    ) {
        // TODO 兼容逻辑，暂时把该job所有插件的output都去掉前缀，在后的覆盖前者
        // 后续需要改为只有本job才去掉前缀
        variables.forEach { (key, value) ->
            val prefix = "jobs.${c.jobId ?: containerId}."
            if (key.startsWith(prefix)) {
                contextMap[key.removePrefix(prefix)] = value
            }
        }

        // current job
        if (c.id?.let { it == containerId } == true) {
            contextMap["job.id"] = c.jobId ?: ""
            contextMap["job.name"] = c.name
            contextMap["job.status"] = getJobStatus(c)
            contextMap["job.outcome"] = c.status ?: ""
            contextMap["job.container.network"] = getNetWork(c) ?: ""
            contextMap["job.stage_id"] = stage.id ?: ""
            contextMap["job.stage_name"] = stage.name ?: ""
        }

        // other job
        val jobId = c.jobId ?: return
        contextMap["jobs.$jobId.id"] = jobId
        contextMap["jobs.$jobId.name"] = c.name
        contextMap["jobs.$jobId.status"] = getJobStatus(c)
        contextMap["jobs.$jobId.outcome"] = c.status ?: ""
        contextMap["jobs.$jobId.container.network"] = getNetWork(c) ?: ""
        contextMap["jobs.$jobId.stage_id"] = stage.id ?: ""
        contextMap["jobs.$jobId.stage_name"] = stage.name ?: ""

        // all element
        buildStepContext(c, contextMap)
    }

    private fun buildStepContext(
        c: Container,
        contextMap: MutableMap<String, String>
    ) {
        c.elements.forEach { e ->
            val stepId = e.stepId ?: return@forEach
            contextMap["steps.$stepId.name"] = e.name
            contextMap["steps.$stepId.id"] = e.id ?: ""
            contextMap["steps.$stepId.status"] = getStepStatus(e)
            contextMap["steps.$stepId.outcome"] = e.status ?: ""
            val jobId = c.jobId ?: return@forEach
            contextMap["jobs.$jobId.steps.$stepId.name"] = e.name
            contextMap["jobs.$jobId.steps.$stepId.id"] = e.id ?: ""
            contextMap["jobs.$jobId.steps.$stepId.status"] = getStepStatus(e)
            contextMap["jobs.$jobId.steps.$stepId.outcome"] = e.status ?: ""
        }
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

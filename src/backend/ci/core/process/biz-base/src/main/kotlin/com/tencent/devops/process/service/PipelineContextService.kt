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

import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_BASE_REF
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_BASE_REPO_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_COMMIT_MESSAGE
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT_CONTENT
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_HEAD_REF
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_HEAD_REPO_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_MR_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REF
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_GROUP
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_NAME
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_SHA
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_SHA_SHORT
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_TIME_TRIGGER_KIND
import com.tencent.devops.process.engine.control.ControlUtils
import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import com.tencent.devops.process.utils.PIPELINE_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_START_TYPE
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class PipelineContextService@Autowired constructor(
    private val pipelineBuildDetailService: PipelineBuildDetailService
) {
    private val logger = LoggerFactory.getLogger(PipelineContextService::class.java)

    fun buildContext(buildId: String, containerId: String?, buildVar: Map<String, String>): Map<String, String> {
        val modelDetail = pipelineBuildDetailService.get(buildId) ?: return emptyMap()
        val varMap = mutableMapOf<String, String>()
        try {
            modelDetail.model.stages.forEach { stage ->
                stage.containers.forEach { c ->
                    buildJobContext(c, containerId, varMap, stage)
                    // steps
                    buildStepContext(c, varMap, buildVar)
                }
            }
            buildCiContext(varMap, modelDetail, buildVar)
        } catch (ignore: Throwable) {
            logger.warn("BKSystemErrorMonitor|buildContextFailed|", ignore)
        }

        return varMap
    }

    private fun buildCiContext(
        varMap: MutableMap<String, String>,
        modelDetail: ModelDetail,
        buildVar: Map<String, String>
    ) {
        varMap["ci.pipeline_id"] = modelDetail.pipelineId
        varMap["ci.pipeline_name"] = modelDetail.pipelineName
        varMap["ci.actor"] = modelDetail.userId
        if (!buildVar[PIPELINE_BUILD_ID].isNullOrBlank())
            varMap["ci.build_id"] = buildVar[PIPELINE_BUILD_ID]!!
        if (!buildVar[PIPELINE_BUILD_NUM].isNullOrBlank())
            varMap["ci.build_num"] = buildVar[PIPELINE_BUILD_NUM]!!
        if (!buildVar[PIPELINE_GIT_REF].isNullOrBlank())
            varMap["ci.ref"] = buildVar[PIPELINE_GIT_REF]!!
        if (!buildVar[PIPELINE_GIT_HEAD_REF].isNullOrBlank())
            varMap["ci.head_ref"] = buildVar[PIPELINE_GIT_HEAD_REF]!!
        if (!buildVar[PIPELINE_GIT_BASE_REF].isNullOrBlank())
            varMap["ci.base_ref"] = buildVar[PIPELINE_GIT_BASE_REF]!!
        if (!buildVar[PIPELINE_GIT_REPO].isNullOrBlank())
            varMap["ci.repo"] = buildVar[PIPELINE_GIT_REPO]!!
        if (!buildVar[PIPELINE_GIT_REPO_NAME].isNullOrBlank())
            varMap["ci.repo_name"] = buildVar[PIPELINE_GIT_REPO_NAME]!!
        if (!buildVar[PIPELINE_GIT_REPO_GROUP].isNullOrBlank())
            varMap["ci.repo_group"] = buildVar[PIPELINE_GIT_REPO_GROUP]!!
        if (!buildVar[PIPELINE_GIT_EVENT_CONTENT].isNullOrBlank())
            varMap["ci.event_content"] = buildVar[PIPELINE_GIT_EVENT_CONTENT]!!
        if (!buildVar[PIPELINE_GIT_SHA].isNullOrBlank())
            varMap["ci.sha"] = buildVar[PIPELINE_GIT_SHA]!!
        if (!buildVar[PIPELINE_GIT_SHA_SHORT].isNullOrBlank())
            varMap["ci.sha_short"] = buildVar[PIPELINE_GIT_SHA_SHORT]!!
        if (!buildVar[PIPELINE_GIT_COMMIT_MESSAGE].isNullOrBlank())
            varMap["ci.commit_message"] = buildVar[PIPELINE_GIT_COMMIT_MESSAGE]!!
        // 特殊处理触发类型以免定时触发无法记录
        if (buildVar[PIPELINE_START_TYPE] == StartType.TIME_TRIGGER.name) {
            varMap["ci.event"] = PIPELINE_GIT_TIME_TRIGGER_KIND
        } else if (!buildVar[PIPELINE_GIT_EVENT].isNullOrBlank()) {
            varMap["ci.event"] = buildVar[PIPELINE_GIT_EVENT]!!
        }
        if (!buildVar[PIPELINE_GIT_REPO_URL].isNullOrBlank()) {
            varMap["ci.repo_url"] = buildVar[PIPELINE_GIT_REPO_URL]!!
        }
        if (!buildVar[PIPELINE_GIT_BASE_REPO_URL].isNullOrBlank()) {
            varMap["ci.base_repo_url"] = buildVar[PIPELINE_GIT_BASE_REPO_URL]!!
        }
        if (!buildVar[PIPELINE_GIT_HEAD_REPO_URL].isNullOrBlank()) {
            varMap["ci.head_repo_url"] = buildVar[PIPELINE_GIT_HEAD_REPO_URL]!!
        }
        if (!buildVar[PIPELINE_GIT_MR_URL].isNullOrBlank()) {
            varMap["ci.mr_url"] = buildVar[PIPELINE_GIT_MR_URL]!!
        }
    }

    private fun buildStepContext(
        c: Container,
        varMap: MutableMap<String, String>,
        buildVar: Map<String, String>
    ) {
        c.elements.forEach { e ->
            varMap["jobs.${c.jobId ?: ""}.steps.${e.id}.name"] = e.name
            varMap["jobs.${c.jobId ?: ""}.steps.${e.id}.id"] = e.id ?: ""
            varMap["jobs.${c.jobId ?: ""}.steps.${e.id}.status"] = getStepStatus(e)
            varMap["jobs.${c.jobId ?: ""}.steps.${e.id}.outcome"] = e.status ?: ""
            varMap["steps.${e.id}.name"] = e.name
            varMap["steps.${e.id}.id"] = e.id ?: ""
            varMap["steps.${e.id}.status"] = getStepStatus(e)
            varMap["steps.${e.id}.outcome"] = e.status ?: ""
            varMap.putAll(getStepOutput(c, e, buildVar))
        }
    }

    private fun buildJobContext(
        c: Container,
        containerId: String?,
        varMap: MutableMap<String, String>,
        stage: Stage
    ) {
        // current job
        if (c.id != null && c.id!! == containerId) {
            varMap["job.id"] = c.jobId ?: ""
            varMap["job.name"] = c.name
            varMap["job.status"] = getJobStatus(c)
            varMap["job.outcome"] = c.status ?: ""
            varMap["job.container.network"] = getNetWork(c)
            varMap["job.stage_id"] = stage.id ?: ""
            varMap["job.stage_name"] = stage.name ?: ""
        }

        // other job
        varMap["jobs.${c.jobId ?: c.id ?: ""}.id"] = c.jobId ?: ""
        varMap["jobs.${c.jobId ?: c.id ?: ""}.name"] = c.name
        varMap["jobs.${c.jobId ?: c.id ?: ""}.status"] = getJobStatus(c)
        varMap["jobs.${c.jobId ?: c.id ?: ""}.outcome"] = c.status ?: ""
        varMap["jobs.${c.jobId ?: c.id ?: ""}.container.network"] = getNetWork(c)
        varMap["jobs.${c.jobId ?: c.id ?: ""}.stage_id"] = stage.id ?: ""
        varMap["jobs.${c.jobId ?: c.id ?: ""}.stage_name"] = stage.name ?: ""
    }

    private fun getStepOutput(c: Container, e: Element, buildVar: Map<String, String>): Map<out String, String> {
        val outputMap = mutableMapOf<String, String>()
        buildVar.filterKeys { it.startsWith("steps.${e.id ?: ""}.outputs.") }.forEach { (t, u) ->
            outputMap["jobs.${c.jobId ?: c.id ?: ""}.$t"] = u
        }
        buildVar.filterKeys { it.startsWith("jobs.${c.id ?: ""}.os") }.forEach { (t, u) ->
            outputMap["jobs.${c.jobId ?: c.id ?: ""}.os"] = u
        }
        return outputMap
    }

    private fun getNetWork(c: Container) = when (c) {
        is VMBuildContainer -> {
            if (c.dispatchType?.buildType() != BuildType.THIRD_PARTY_AGENT_ID &&
                c.dispatchType?.buildType() != BuildType.THIRD_PARTY_AGENT_ENV
            ) {
                "DEVNET"
            } else {
                "IDC"
            }
        }
        is NormalContainer -> {
            "IDC"
        }
        else -> {
            ""
        }
    }

    private fun getOs(c: Container) = when (c) {
        is VMBuildContainer -> {
            c.baseOS.name
        }
        is NormalContainer -> {
            VMBaseOS.LINUX.name
        }
        else -> {
            ""
        }
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

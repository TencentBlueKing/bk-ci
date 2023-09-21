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

package com.tencent.devops.process.yaml.modelTransfer

import com.tencent.devops.process.pojo.transfer.PositionResponse
import com.tencent.devops.process.yaml.v2.models.job.PreJob
import com.tencent.devops.process.yaml.v2.models.stage.PreStage
import com.tencent.devops.process.yaml.v3.models.ITemplateFilter
import com.tencent.devops.process.yaml.v3.models.job.Job
import com.tencent.devops.process.yaml.v3.utils.ScriptYmlUtils
import java.util.concurrent.atomic.AtomicInteger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class YamlIndexService @Autowired constructor(
    val dispatchTransfer: DispatchTransfer
) {

    companion object {
        private val logger = LoggerFactory.getLogger(YamlIndexService::class.java)
    }

    fun checkYamlIndex(preYaml: ITemplateFilter, nodeIndex: TransferMapper.NodeIndex): PositionResponse {
        when (nodeIndex.key) {
            ITemplateFilter::stages.name -> {
                val next = nodeIndex.next ?: return PositionResponse(type = PositionResponse.PositionType.STAGE)
                val index = next.index ?: throw PacYamlNotValidException(nodeIndex.toString())
                return checkStage(preYaml.stages!![index], next.next).apply {
                    stageIndex = index
                }
            }

            ITemplateFilter::jobs.name -> {
                val jobs = preYaml.jobs!!
                val next = nodeIndex.next ?: return PositionResponse(type = PositionResponse.PositionType.STAGE)
                val key = next.key ?: throw PacYamlNotValidException(nodeIndex.toString())
                val indexAtomic = AtomicInteger(0)
                jobs.forEach { (jobId, job) ->
                    val index = indexAtomic.getAndIncrement()
                    if (jobId == key) {
                        return checkJob(job as Map<String, Any>, next.next).apply {
                            this.jobId = key
                            this.containerIndex = index
                        }
                    }
                }
            }

            ITemplateFilter::steps.name -> {
                val next = nodeIndex.next ?: return PositionResponse(type = PositionResponse.PositionType.JOB)
                val index = next.index ?: throw PacYamlNotValidException(nodeIndex.toString())
                return checkStep(preYaml.steps!![index], next.next).apply {
                    stepIndex = index
                }
            }
        }
        return PositionResponse(type = PositionResponse.PositionType.SETTING)
    }

    fun checkStage(stage: Map<String, Any>, nodeIndex: TransferMapper.NodeIndex?): PositionResponse {
        if (nodeIndex?.key == PreStage::jobs.name) {
            val jobs = stage[PreStage::jobs.name] as LinkedHashMap<String, Any>
            val next = nodeIndex.next ?: return PositionResponse(type = PositionResponse.PositionType.STAGE)
            val key = next.key ?: throw PacYamlNotValidException(nodeIndex.toString())
            val indexAtomic = AtomicInteger(0)
            jobs.forEach { (jobId, job) ->
                val index = indexAtomic.getAndIncrement()
                if (jobId == key) {
                    return checkJob(job as Map<String, Any>, next.next).apply {
                        this.jobId = key
                        this.containerIndex = index
                    }
                }
            }
        }
        return PositionResponse(type = PositionResponse.PositionType.STAGE)
    }

    fun checkJob(job: Map<String, Any>, nodeIndex: TransferMapper.NodeIndex?): PositionResponse {
        val runsOn = ScriptYmlUtils.formatRunsOn(job["runs-on"])
        val (_, os) = dispatchTransfer.makeDispatchType(Job(runsOn = runsOn), null)
        if (nodeIndex?.key == PreJob::steps.name) {
            val steps = job[PreJob::steps.name] as List<Any>
            val next = nodeIndex.next ?: return PositionResponse(type = PositionResponse.PositionType.JOB)
            val index = next.index ?: throw PacYamlNotValidException(nodeIndex.toString())
            return checkStep(steps[index] as Map<String, Any>, next.next).apply {
                stepIndex = index
                jobBaseOs = os
            }
        }
        return PositionResponse(type = PositionResponse.PositionType.JOB, jobBaseOs = os)
    }

    fun checkStep(job: Map<String, Any>, nodeIndex: TransferMapper.NodeIndex?): PositionResponse {
        return PositionResponse(type = PositionResponse.PositionType.STEP)
    }
}

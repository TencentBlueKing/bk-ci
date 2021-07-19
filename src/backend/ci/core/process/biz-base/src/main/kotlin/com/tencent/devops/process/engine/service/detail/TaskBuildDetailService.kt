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

package com.tencent.devops.process.engine.service.detail

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.agent.ManualReviewUserTaskElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateInElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateOutElement
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.dao.BuildDetailDao
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.store.api.atom.ServiceMarketAtomEnvResource
import org.jooq.DSLContext
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Suppress("LongParameterList", "MagicNumber")
@Service
class TaskBuildDetailService(
    private val client: Client,
    private val buildVariableService: BuildVariableService,
    dslContext: DSLContext,
    pipelineBuildDao: PipelineBuildDao,
    buildDetailDao: BuildDetailDao,
    pipelineEventDispatcher: PipelineEventDispatcher,
    redisOperation: RedisOperation
) : BaseBuildDetailService(
    dslContext,
    pipelineBuildDao,
    buildDetailDao,
    pipelineEventDispatcher,
    redisOperation
) {

    fun taskPause(buildId: String, stageId: String, containerId: String, taskId: String, buildStatus: BuildStatus) {
        update(buildId = buildId, modelInterface = object : ModelInterface {
            var update = false

            override fun onFindElement(e: Element, c: Container): Traverse {
                if (c.id.equals(containerId)) {
                    if (e.id.equals(taskId)) {
                        logger.info("ENGINE|$buildId|pauseTask|$stageId|j($containerId)|t($taskId)|${buildStatus.name}")
                        update = true
                        e.status = buildStatus.name
                        return Traverse.BREAK
                    }
                }
                return Traverse.CONTINUE
            }

            override fun needUpdate(): Boolean {
                return update
            }
        },
            buildStatus = BuildStatus.RUNNING,
            operation = "taskPause"
        )
    }

    fun taskSkip(buildId: String, taskId: String) {
        update(
            buildId = buildId,
            modelInterface = object : ModelInterface {
                var update = false
                override fun onFindElement(e: Element, c: Container): Traverse {
                    if (e.id == taskId) {
                        update = true
                        e.status = BuildStatus.SKIP.name
                        return Traverse.BREAK
                    }
                    return Traverse.CONTINUE
                }

                override fun needUpdate(): Boolean {
                    return update
                }
            },
            buildStatus = BuildStatus.RUNNING,
            operation = "taskSkip"
        )
    }

    fun taskStart(buildId: String, taskId: String) {
        update(
            buildId = buildId,
            modelInterface = object : ModelInterface {
                var update = false
                val delimiters = ","
                override fun onFindElement(e: Element, c: Container): Traverse {
                    if (e.id == taskId) {
                        if (e is ManualReviewUserTaskElement) {
                            e.status = BuildStatus.REVIEWING.name
                            //                        c.status = BuildStatus.REVIEWING.name
                            // Replace the review user with environment
                            val list = mutableListOf<String>()
                            e.reviewUsers.forEach { reviewUser ->
                                list.addAll(buildVariableService.replaceTemplate(buildId, reviewUser).split(delimiters))
                            }
                            e.reviewUsers.clear()
                            e.reviewUsers.addAll(list)
                        } else if (e is QualityGateInElement || e is QualityGateOutElement) {
                            e.status = BuildStatus.REVIEWING.name
                            c.status = BuildStatus.REVIEWING.name
                        } else {
                            c.status = BuildStatus.RUNNING.name
                            e.status = BuildStatus.RUNNING.name
                        }
                        e.startEpoch = System.currentTimeMillis()
                        if (c.startEpoch == null) {
                            c.startEpoch = e.startEpoch
                        }
                        e.errorType = null
                        e.errorCode = null
                        e.errorMsg = null
                        e.version = findTaskVersion(buildId, e.getAtomCode(), e.version, e.getClassType())
                        update = true
                        return Traverse.BREAK
                    }
                    return Traverse.CONTINUE
                }

                override fun needUpdate(): Boolean {
                    return update
                }
            },
            buildStatus = BuildStatus.RUNNING,
            operation = "taskStart"
        )
    }

    fun taskCancel(buildId: String, containerId: String, taskId: String, cancelUser: String?) {
        update(
            buildId = buildId,
            modelInterface = object : ModelInterface {
                var update = false

                override fun onFindElement(e: Element, c: Container): Traverse {
                    if (c.id.equals(containerId)) {
                        if (e.id.equals(taskId)) {
                            c.status = BuildStatus.CANCELED.name
                            e.status = BuildStatus.CANCELED.name
                            update = true
                            return Traverse.BREAK
                        }
                    }
                    return Traverse.CONTINUE
                }

                override fun needUpdate(): Boolean {
                    return update
                }
            },
            buildStatus = BuildStatus.RUNNING,
            cancelUser = cancelUser,
            operation = "taskCancel"
        )
    }

    fun taskEnd(
        buildId: String,
        taskId: String,
        buildStatus: BuildStatus,
        errorType: ErrorType? = null,
        errorCode: Int? = null,
        errorMsg: String? = null
    ) {
        update(
            buildId = buildId,
            modelInterface = object : ModelInterface {

                var update = false
                override fun onFindElement(e: Element, c: Container): Traverse {
                    if (e.id == taskId) {
                        e.status = buildStatus.name
                        if (e.startEpoch == null) {
                            e.elapsed = 0
                        } else {
                            e.elapsed = System.currentTimeMillis() - e.startEpoch!!
                        }
                        if (errorType != null) {
                            e.errorType = errorType.name
                            e.errorCode = errorCode
                            e.errorMsg = errorMsg
                        }

                        var elementElapsed = 0L
                        @Suppress("LoopWithTooManyJumpStatements")
                        for (it in c.elements) {
                            if (it.elapsed == null) {
                                continue
                            }
                            elementElapsed += it.elapsed!!
                            if (it == e) {
                                break
                            }
                        }

                        c.elementElapsed = elementElapsed
                        update = true
                        return Traverse.BREAK
                    }
                    return Traverse.CONTINUE
                }

                override fun needUpdate(): Boolean {
                    return update
                }
            },
            buildStatus = BuildStatus.RUNNING,
            operation = "taskEnd"
        )
    }

    @Suppress("NestedBlockDepth")
    fun taskContinue(buildId: String, stageId: String, containerId: String, taskId: String, element: Element?) {
        update(
            buildId = buildId,
            modelInterface = object : ModelInterface {

                var update = false

                override fun onFindStage(stage: Stage, model: Model): Traverse {
                    // 不是当前Stage要跳过，不再进入onFindContainer循环
                    return if (stage.id.equals(stageId)) Traverse.CONTINUE else Traverse.SKIP
                }

                override fun onFindContainer(id: Int, container: Container, stage: Stage): Traverse {
                    if (container.id.equals(containerId)) {
                        val newElement: ArrayList<Element> by lazy { ArrayList<Element>(container.elements.size) }
                        container.elements.forEach { e ->
                            if (e.id.equals(taskId)) {
                                // 设置插件状态为排队状态
                                container.status = BuildStatus.QUEUE.name
                                update = true
                                if (element != null) { // 若element不为null，说明element内的input有改动，需要替换
                                    element.status = null
                                    newElement.add(element)
                                } else {
                                    // 若element为null，需把status至空，用户展示
                                    e.status = null
                                }
                            } else {
                                if (element != null) {
                                    newElement.add(e)
                                }
                            }
                        }
                        if (element != null) {
                            container.elements = newElement
                        }
                        return Traverse.BREAK
                    }
                    return Traverse.SKIP
                }

                override fun needUpdate(): Boolean {
                    return update
                }
            },
            buildStatus = BuildStatus.RUNNING,
            operation = "updateElementWhenPauseContinue"
        )
    }

    private val projectCache = Caffeine.newBuilder()
        .maximumSize(50000)
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build<String/*BuildId*/, String/*projectId*/> { buildId ->
            pipelineBuildDao.getBuildInfo(dslContext, buildId)?.projectId
        }

    private val atomCache = Caffeine.newBuilder()
        .maximumSize(2000)
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build<String/*projectCode VS atomCode VS atomVersion*/, String/*true version*/> { mix ->
            val keys = mix.split(" VS ")
            client.get(ServiceMarketAtomEnvResource::class)
                .getAtomEnv(projectCode = keys[0], atomCode = keys[1], version = keys[2]).data?.version
        }

    fun findTaskVersion(buildId: String, atomCode: String, atomVersion: String, atomClass: String): String {
        // 只有是研发商店插件,获取插件的版本信息
        if (atomClass != "marketBuild" && atomClass != "marketBuildLess") {
            return atomVersion
        }
        return if (atomVersion.contains("*")) {
            val projectCode = projectCache.get(buildId)
            if (projectCode != null) {
                atomCache.get("$projectCode VS $atomCode VS $atomVersion") ?: atomVersion
            } else {
                atomVersion
            }
        } else {
            atomVersion
        }
    }
}

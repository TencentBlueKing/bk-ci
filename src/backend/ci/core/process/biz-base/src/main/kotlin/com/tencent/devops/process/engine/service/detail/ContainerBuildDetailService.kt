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

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.dao.BuildDetailDao
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Suppress("LongParameterList", "MagicNumber")
@Service
class ContainerBuildDetailService(
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

    fun containerPreparing(buildId: String, containerId: Int) {
        update(
            buildId = buildId,
            modelInterface = object : ModelInterface {
                var update = false
                override fun onFindContainer(id: Int, container: Container, stage: Stage): Traverse {
                    if (id == containerId) {
                        container.startEpoch = System.currentTimeMillis()
                        container.status = BuildStatus.PREPARE_ENV.name
                        container.startVMStatus = BuildStatus.RUNNING.name
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
            operation = "containerPreparing"
        )
    }

    fun containerStarted(buildId: String, containerId: Int, containerBuildStatus: BuildStatus) {
        update(
            buildId = buildId,
            modelInterface = object : ModelInterface {
                var update = false

                override fun onFindContainer(id: Int, container: Container, stage: Stage): Traverse {
                    if (id == containerId) {
                        if (container.startEpoch != null) {
                            container.systemElapsed = System.currentTimeMillis() - container.startEpoch!!
                        }
                        logger.info("[$buildId]|containerStarted|containerId=$containerId|startVMStatus " +
                            "changed from ${container.startVMStatus} to ${containerBuildStatus.name}")
                        container.startVMStatus = containerBuildStatus.name
                        // #2074 containerBuildStatus如果是失败的，则将Job整体状态设置为失败
                        if (containerBuildStatus.isFailure()) {
                            container.status = containerBuildStatus.name
                        } else {
                            container.status = BuildStatus.RUNNING.name
                        }
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
            operation = "containerStarted"
        )
    }

    fun updateContainerStatus(buildId: String, containerId: String, buildStatus: BuildStatus) {
        logger.info("[$buildId]|container_end|containerId=$containerId|status=$buildStatus")
        update(buildId, object : ModelInterface {

            var update = false

            override fun onFindContainer(id: Int, container: Container, stage: Stage): Traverse {
                if (container.id == containerId) {
                    update = true
                    container.status = buildStatus.name
                    if (buildStatus.isFinish() &&
                        (container.startVMStatus == null || !BuildStatus.valueOf(container.startVMStatus!!).isFinish())
                    ) {
                        logger.info("[$buildId]|updateContainer|containerId=$containerId|startVMStatus " +
                            "changed from ${container.startVMStatus} to ${container.status}")
                        container.startVMStatus = container.status
                    }
                    return Traverse.BREAK
                }
                return Traverse.CONTINUE
            }

            override fun needUpdate(): Boolean {
                return update
            }
        }, BuildStatus.RUNNING)
    }

    fun containerSkip(buildId: String, containerId: String) {
        logger.info("[$buildId|$containerId] Normal container skip")
        update(
            buildId = buildId,
            modelInterface = object : ModelInterface {

                var update = false

                override fun onFindContainer(id: Int, container: Container, stage: Stage): Traverse {
                    if (container !is TriggerContainer) {
                        // 兼容id字段
                        if (container.id == containerId || container.containerId == containerId) {
                            update = true
                            container.status = BuildStatus.SKIP.name
                            container.startVMStatus = BuildStatus.SKIP.name
                            container.elements.forEach {
                                it.status = BuildStatus.SKIP.name
                            }
                            return Traverse.BREAK
                        }
                    }
                    return Traverse.CONTINUE
                }

                override fun needUpdate(): Boolean {
                    return update
                }
            },
            buildStatus = BuildStatus.RUNNING, operation = "containerSkip"
        )
    }
}

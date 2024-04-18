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

package com.tencent.devops.dispatch.devcloud.service

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.dispatch.devcloud.client.DispatchDevCloudClient
import com.tencent.devops.dispatch.devcloud.dao.BuildContainerPoolNoDao
import com.tencent.devops.dispatch.devcloud.dao.DcPersistenceBuildDao
import com.tencent.devops.dispatch.devcloud.dao.DevCloudBuildDao
import com.tencent.devops.dispatch.devcloud.pojo.Action
import com.tencent.devops.dispatch.devcloud.pojo.ContainerBuildStatus
import com.tencent.devops.dispatch.devcloud.pojo.TaskStatus
import com.tencent.devops.dispatch.devcloud.pojo.persistence.PersistenceBuildStatus
import com.tencent.devops.dispatch.devcloud.service.context.DcShutdownHandlerContext
import com.tencent.devops.dispatch.devcloud.utils.DevCloudJobRedisUtils
import com.tencent.devops.model.dispatch.devcloud.tables.records.TBuildContainerPoolNoRecord
import com.tencent.devops.model.dispatch.devcloud.tables.records.TDevcloudBuildRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DcContainerShutdownHandler @Autowired constructor(
    private val dslContext: DSLContext,
    private val devCloudBuildDao: DevCloudBuildDao,
    private val buildContainerPoolNoDao: BuildContainerPoolNoDao,
    private val dcPersistenceBuildDao: DcPersistenceBuildDao,
    private val dispatchDevCloudClient: DispatchDevCloudClient,
    private val devCloudJobRedisUtils: DevCloudJobRedisUtils,
    private val pipelineEventDispatcher: PipelineEventDispatcher
) : Handler<DcShutdownHandlerContext>() {

    companion object {
        private val logger = LoggerFactory.getLogger(DcContainerShutdownHandler::class.java)
    }

    override fun handlerRequest(handlerContext: DcShutdownHandlerContext) {
        with(handlerContext) {
            handlerContext.buildLogKey = "$pipelineId|$buildId|$vmSeqId|$executeCount"

            val persistence = isPersistenceBuild(this)

            // 有可能出现devcloud返回容器状态running了，但是其实流水线任务早已经执行完了，
            // 导致shutdown消息先收到而redis和db还没有设置的情况，因此扔回队列，sleep等待30秒重新触发
            // 持久化构建容器不做此校验
            val buildContainerPools = buildContainerPoolNoDao.getBuildContainerPoolNo(
                dslContext = dslContext,
                buildId = buildId,
                vmSeqId = vmSeqId,
                executeCount = executeCount ?: 1
            )

            if (buildContainerPools.none { it.containerName != null } &&
                shutdownEvent.retryTime <= 3 &&
                !persistence
            ) {
                logger.info(
                    "$buildLogKey shutdown no containerName, sleep 10s and retry ${shutdownEvent.retryTime}. "
                )
                shutdownEvent.retryTime += 1
                shutdownEvent.delayMills = 10000
                pipelineEventDispatcher.dispatch(shutdownEvent)

                return
            }

            // 非持久化构建结束事件, 需要关机
            if (!persistence) {
                stopContainer(this, buildContainerPools)
            }
        }
    }

    fun forceStopContainer(devCloudBuildRecord: TDevcloudBuildRecord) {
        with(devCloudBuildRecord) {
            logger.info("Container is running, stop it, containerName:$containerName")
            val taskId =
                dispatchDevCloudClient.operateContainer(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = "",
                    vmSeqId = vmSeqId,
                    userId = userId,
                    name = containerName,
                    action = Action.STOP
                )
            val opResult = dispatchDevCloudClient.waitTaskFinish(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                taskId = taskId
            )
            if (opResult.first == TaskStatus.SUCCEEDED) {
                logger.info("stop dev cloud vm success. then update debug status to false")
                devCloudBuildDao.updateDebugStatus(
                    dslContext = dslContext,
                    pipelineId = pipelineId,
                    vmSeqId = vmSeqId,
                    containerName = containerName,
                    debugStatus = false
                )
            } else {
                // 停不掉？尝试删除
                logger.info("stop dev cloud vm failed, msg: ${opResult.second}")
                logger.info(
                    "stop dev cloud vm failed, try to delete it, " +
                            "containerName:$containerName"
                )
                devCloudBuildDao.delete(dslContext, pipelineId, vmSeqId, poolNo)
                dispatchDevCloudClient.operateContainer(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = "",
                    vmSeqId = vmSeqId,
                    userId = userId,
                    name = containerName,
                    action = Action.DELETE
                )
            }
        }
    }

    private fun isPersistenceBuild(handlerContext: DcShutdownHandlerContext): Boolean {
        with(handlerContext) {
            val buildRecord = dcPersistenceBuildDao.getPersistenceBuildInfo(
                dslContext = dslContext,
                buildId = buildId,
                vmSeqId = vmSeqId,
                executeCount = executeCount
            )

            if (buildRecord != null) {
                logger.info("$buildLogKey finish persistenceBuild.")
                dcPersistenceBuildDao.updateStatus(dslContext, buildRecord.id, PersistenceBuildStatus.DONE.status)
            }

            return buildRecord != null
        }
    }

    private fun stopContainer(
        handlerContext: DcShutdownHandlerContext,
        buildContainerPools: Result<TBuildContainerPoolNoRecord>
    ) {
        with(handlerContext) {
            buildContainerPools.filter { it.containerName != null }.forEach {
                try {
                    logger.info(
                        "$buildLogKey stop dev cloud container,vmSeqId: ${it.vmSeqId}, " +
                            "containerName:${it.containerName}"
                    )
                    val taskId = dispatchDevCloudClient.operateContainer(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        vmSeqId = vmSeqId ?: "",
                        userId = userId,
                        name = it.containerName!!,
                        action = Action.STOP
                    )
                    val opResult = dispatchDevCloudClient.waitTaskFinish(
                        userId = userId,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        taskId = taskId
                    )
                    if (opResult.first == TaskStatus.SUCCEEDED) {
                        logger.info("$buildLogKey stop dev cloud vm success.")
                    } else {
                        logger.info("$buildLogKey stop dev cloud vm failed, msg: ${opResult.second}")
                    }
                } catch (e: Exception) {
                    logger.error(
                        "$buildLogKey stop dev cloud vm failed. containerName: ${it.containerName}",
                        e
                    )
                } finally {
                    // 清除job创建记录
                    devCloudJobRedisUtils.deleteJobCount(buildId, it.containerName!!)
                }
            }

            buildContainerPools.filter { it.poolNo != null }.forEach {
                logger.info("$buildLogKey update status in db,vmSeqId: ${it.vmSeqId}, poolNo:${it.poolNo}")
                devCloudBuildDao.updateStatus(
                    dslContext = dslContext,
                    pipelineId = pipelineId,
                    vmSeqId = it.vmSeqId,
                    poolNo = it.poolNo!!.toInt(),
                    status = ContainerBuildStatus.IDLE.status
                )
            }

            logger.info("[$buildLogKey delete buildContainerPoolNo.")
            buildContainerPoolNoDao.deleteDevCloudBuildLastContainerPoolNo(
                dslContext = dslContext,
                buildId = buildId,
                vmSeqId = vmSeqId,
                executeCount = executeCount ?: 1
            )
        }
    }
}

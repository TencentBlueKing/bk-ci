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

package com.tencent.devops.process.engine.control

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildStatusBroadCastEvent
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.event.CallBackEvent
import com.tencent.devops.common.pipeline.event.ProjectPipelineCallBack
import com.tencent.devops.process.TestBase
import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.ProjectPipelineCallBackService
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CallBackControlTest : TestBase() {

    private val pipelineBuildDetailService: PipelineBuildDetailService = mockk()
    private val pipelineRepositoryService: PipelineRepositoryService = mockk(relaxed = true)
    private val projectPipelineCallBackService: ProjectPipelineCallBackService = mockk()
    private val client: Client = mockk()
    private val callbackCircuitBreakerRegistry: CircuitBreakerRegistry = mockk()

    private val callBackControl = CallBackControl(
        pipelineBuildDetailService = pipelineBuildDetailService,
        pipelineRepositoryService = pipelineRepositoryService,
        projectPipelineCallBackService = projectPipelineCallBackService,
        client = client,
        callbackCircuitBreakerRegistry = callbackCircuitBreakerRegistry
    )

    private val testUrl = "https://mock/callback"
    private var modelDetail: ModelDetail? = null

    private var callbacks: MutableList<ProjectPipelineCallBack>? = null

    @BeforeEach
    fun setUp2() {

        val existsModel = genModel(stageSize = 4, jobSize = 3, elementSize = 2)

        modelDetail = ModelDetail(
            id = buildId,
            pipelineId = pipelineId,
            pipelineName = "testCase",
            userId = userId,
            trigger = userId,
            model = existsModel,
            startTime = System.currentTimeMillis(),
            endTime = null,
            status = BuildStatus.RUNNING.name,
            buildNum = 2,
            currentTimestamp = System.currentTimeMillis(),
            cancelUserId = null,
            curVersion = 2,
            latestBuildNum = 1,
            latestVersion = 1,
            lastModifyUser = "yongyiduan",
            executeTime = 100
        )

        every {
            pipelineBuildDetailService.get(
                projectId = projectId,
                buildId = buildId,
                refreshStatus = false
            )
        } returns (modelDetail)
    }

    @Test
    fun callbackErrorBuildEvent() {
        initBuildStartEnd(CallBackEvent.BUILD_START)
        val buildStartEvent = PipelineBuildStatusBroadCastEvent(
            source = "vm-build-claim($firstContainerId)", projectId = projectId, pipelineId = pipelineId,
            userId = userId, buildId = buildId, actionType = ActionType.START
        )

//        val startTime = System.currentTimeMillis()

        callBackControl.callBackBuildEvent(buildStartEvent)
//        Thread.sleep(100)
//        val errorTime = callBackControl.statForTest().get(testUrl)!!
//        Assertions.assertNotEquals(0L, errorTime.get())
//        println("fail time:" + DateTimeUtil.formatDate(Date(errorTime.get())))
//        val buildEvent = callBackControl.buildEvent(buildStartEvent, modelDetail!!)
//        val requestBody: String = ObjectMapper().writeValueAsString(buildEvent)
//        val request = callBackControl.packRequest(callBack = callbacks!![0], requestBody = requestBody)
//        val response: Response = Response.Builder()
//            .code(200)
//            .request(Request.Builder().url(testUrl).build())
//            .protocol(Protocol.HTTP_1_1)
//            .message("mock")
//            .build()
//        whenever(callBackControl.sendRequest(request = request)).thenReturn(response)
//        println("1")
//        Thread.sleep(1100)
//        println("2")
//        callBackControl.callBackBuildEvent(buildStartEvent)
//        Thread.sleep(1000)
//        if (System.currentTimeMillis() - startTime > (testSeconds * 1000)) {
//            Assertions.assertEquals(0L, callBackControl.statForTest().get(testUrl)!!.get())
//            return
//        }
//        callBackControl.callBackBuildEvent(buildStartEvent)
//        Assertions.assertEquals(0L, errorTime)
//        Assertions.assertNotEquals(0L, callBackControl.statForTest().get(testUrl)!!.get())
//        println("end")
    }

    private fun initBuildStartEnd(vararg callbackEvents: CallBackEvent) {
        callbacks = mutableListOf()
        val events = StringBuilder()
        callbackEvents.forEach {
            if (events.isNotEmpty()) {
                events.append(",")
            }
            events.append(it.name)
            callbacks!!.addAll(genCallBackList(it))
        }
        every {
            projectPipelineCallBackService.listProjectCallBack(
                projectId = projectId,
                events = events.toString()
            )
        } returns (callbacks!!)
    }

    @Test
    fun `stage running cover finish`() {
        val expectStatus = BuildStatus.RUNNING.name
        val existsModel = modelDetail!!.model
        val currentTimeMillis = System.currentTimeMillis()
        existsModel.stages.forEachIndexed { si, s ->
            if (si == 0) {
                s.containers[0].status = BuildStatus.SUCCEED.name
                s.containers[0].elementElapsed = 10
                return@forEachIndexed
            }
            if (si == 1) {
                s.startEpoch = currentTimeMillis // 有值不会被覆盖
            }
            s.containers.forEachIndexed { ci, container ->
                if (si == existsModel.stages.size - 1 && ci == 0) {
                    container.status = expectStatus
                    container.systemElapsed = 10
                    container.elementElapsed = 1000
                    container.startEpoch = currentTimeMillis - 100000
                } else {
                    container.status = BuildStatus.SUCCEED.name
                    container.systemElapsed = 10
                    container.elementElapsed = 1000
                    container.startEpoch = currentTimeMillis - 100000 - (ci * 10000)
                }
                container.elements.forEach {
                    it.status = container.status
                    it.startEpoch = container.startEpoch
                    it.elapsed = 1
                }
            }
        }
        val parseModel = callBackControl.parseModel(existsModel)
        parseModel.forEachIndexed { index, stage ->
            if (index == 0) {
                return@forEachIndexed
            }

            println("${stage.stageName},status=${stage.status}, start=${stage.startTime}, end=${stage.endTime}")
            if (index == 1) {
                Assertions.assertEquals(currentTimeMillis, stage.startTime)
            }

            if (index == parseModel.size - 1) {
                Assertions.assertEquals(expectStatus, stage.status)
            } else {
                Assertions.assertEquals(BuildStatus.SUCCEED.name, stage.status)
            }
        }
    }

    @Test
    fun `stage failure cover other`() {
        val expectStatus = BuildStatus.FAILED.name
        val existsModel = modelDetail!!.model
        val currentTimeMillis = System.currentTimeMillis()
        existsModel.stages.forEachIndexed { si, s ->
            if (si == 0) {
                s.containers[0].status = BuildStatus.SUCCEED.name
                s.containers[0].elementElapsed = 10
                return@forEachIndexed
            }
            if (si == 1) {
                s.startEpoch = currentTimeMillis // 有值不会被覆盖
            }
            s.containers.forEachIndexed { ci, container ->
                if (si == existsModel.stages.size - 1 && ci == 0) {
                    container.status = expectStatus
                    container.systemElapsed = 10
                    container.elementElapsed = 1000
                    container.startEpoch = currentTimeMillis - 100000
                } else {
                    container.status = BuildStatus.SUCCEED.name
                    container.systemElapsed = 10
                    container.elementElapsed = 1000
                    container.startEpoch = currentTimeMillis - 100000 - (ci * 10000)
                }
                container.elements.forEach {
                    it.status = container.status
                    it.startEpoch = container.startEpoch
                    it.elapsed = 1
                }
            }
        }
        val parseModel = callBackControl.parseModel(existsModel)
        parseModel.forEachIndexed { index, stage ->
            if (index == 0) {
                return@forEachIndexed
            }

            println("${stage.stageName},status=${stage.status}, start=${stage.startTime}, end=${stage.endTime}")
            if (index == 1) {
                Assertions.assertEquals(currentTimeMillis, stage.startTime)
            }

            if (index == parseModel.size - 1) {
                Assertions.assertEquals(expectStatus, stage.status)
            } else {
                Assertions.assertEquals(BuildStatus.SUCCEED.name, stage.status)
            }
        }
    }

    private fun genCallBackList(event: CallBackEvent): List<ProjectPipelineCallBack> {
        return listOf(
            ProjectPipelineCallBack(
                projectId = projectId,
                callBackUrl = testUrl,
                events = event.name,
                secretToken = "secretToken"
            )
        )
    }
}

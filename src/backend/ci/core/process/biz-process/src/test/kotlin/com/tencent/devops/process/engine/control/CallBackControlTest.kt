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

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildStatusBroadCastEvent
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.event.CallBackEvent
import com.tencent.devops.process.TestBase
import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.ProjectPipelineCallBackService
import com.tencent.devops.process.pojo.ProjectPipelineCallBack
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import org.junit.Before
import org.junit.Test

class CallBackControlTest : TestBase() {

    private val pipelineBuildDetailService: PipelineBuildDetailService = mock()
    private val pipelineRepositoryService: PipelineRepositoryService = mock()
    private val projectPipelineCallBackService: ProjectPipelineCallBackService = mock()

    private val callBackControl = CallBackControl(
        pipelineBuildDetailService = pipelineBuildDetailService,
        pipelineRepositoryService = pipelineRepositoryService,
        projectPipelineCallBackService = projectPipelineCallBackService
    )

    private val testUrl = "https://mock/callback"
    private var modelDetail: ModelDetail? = null

    private var callbacks: MutableList<ProjectPipelineCallBack>? = null

    @Before
    fun setUp2() {

        val existsModel = genModel(stageSize = 4, jobSize = 2, elementSize = 2)

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
            latestVersion = 1
        )

        whenever(pipelineBuildDetailService.get(buildId = buildId, refreshStatus = false))
            .thenReturn(modelDetail)
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
//        Assert.assertNotEquals(0L, errorTime.get())
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
//            Assert.assertEquals(0L, callBackControl.statForTest().get(testUrl)!!.get())
//            return
//        }
//        callBackControl.callBackBuildEvent(buildStartEvent)
//        Assert.assertEquals(0L, errorTime)
//        Assert.assertNotEquals(0L, callBackControl.statForTest().get(testUrl)!!.get())
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
        whenever(projectPipelineCallBackService.listProjectCallBack(projectId = projectId, events = events.toString()))
            .thenReturn(callbacks)
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

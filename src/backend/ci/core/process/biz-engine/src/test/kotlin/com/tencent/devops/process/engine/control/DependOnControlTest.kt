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
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.process.engine.service.PipelineContainerService
import com.tencent.devops.process.engine.utils.TestTool
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DependOnControlTest {
    private val pipelineContainerService: PipelineContainerService = mockk()
    private val client: Client = mockk()
    private val buildLogPrinter: BuildLogPrinter = BuildLogPrinter(client)
    private val dependOnControl = DependOnControl(
        pipelineContainerService = pipelineContainerService,
        buildLogPrinter = buildLogPrinter
    )

    @BeforeEach
    fun setUp() {
        justRun {
            buildLogPrinter.addLine(buildId = "", message = "", tag = "", jobId = "", executeCount = 1, subTag = "")
        }
    }

    @Test
    fun `when dependOnContainerId2JobIds is null return success`() {
        val dependContainerId = 2
        val status2 = BuildStatus.RUNNING
        val mockJob = TestTool.genVmBuildContainer(
            vmSeqId = 1,
            jobControlOption = JobControlOption(dependOnContainerId2JobIds = null)
        )

        val mockContainers = listOf(
            TestTool.genVmBuildContainer(vmSeqId = dependContainerId).copy(status = status2)
        )
        every {
            pipelineContainerService.listContainers(
                TestTool.projectId,
                TestTool.buildId,
                TestTool.stageId
            )
        } returns (mockContainers)
        Assertions.assertEquals(BuildStatus.SUCCEED, dependOnControl.dependOnJobStatus(container = mockJob))
    }

    @Test
    fun `when depend on job is running then return running`() {
        val dependContainerId = 2
        val status2 = BuildStatus.RUNNING
        val mockJob = TestTool.genVmBuildContainer(
            vmSeqId = 1,
            jobControlOption = JobControlOption(
                dependOnContainerId2JobIds = mapOf(dependContainerId.toString() to "jobId2")
            )
        )

        val mockContainers = listOf(
            TestTool.genVmBuildContainer(vmSeqId = dependContainerId).copy(status = status2)
        )
        every {
            pipelineContainerService.listContainers(
                TestTool.projectId,
                TestTool.buildId,
                TestTool.stageId
            )
        } returns (mockContainers)
        Assertions.assertEquals(status2, dependOnControl.dependOnJobStatus(container = mockJob))
    }

    @Test
    fun `when depend on job is failed then return failed`() {
        val dependContainerId = 2
        val status2 = BuildStatus.FAILED
        val mockJob = TestTool.genVmBuildContainer(
            vmSeqId = 1,
            jobControlOption = JobControlOption(
                dependOnContainerId2JobIds = mapOf(dependContainerId.toString() to "jobId2")
            )
        )

        val mockContainers = listOf(
            TestTool.genVmBuildContainer(vmSeqId = dependContainerId).copy(status = status2)
        )
        every {
            pipelineContainerService.listContainers(
                TestTool.projectId,
                TestTool.buildId,
                TestTool.stageId
            )
        } returns (mockContainers)
        val dependOnJobStatus = dependOnControl.dependOnJobStatus(container = mockJob)
        Assertions.assertEquals(status2, dependOnJobStatus)
    }

    @Test
    fun `when depend on job is running or success then return running`() {
        val dependContainerId = 2
        val dependContainerId3 = 3
        val mockJob = TestTool.genVmBuildContainer(
            vmSeqId = 1,
            jobControlOption = JobControlOption(
                dependOnContainerId2JobIds = mapOf("2" to "jobId2").plus("3" to "jobId3")
            ), status = BuildStatus.RUNNING
        )

        every {
            pipelineContainerService.listContainers(
                TestTool.projectId,
                TestTool.buildId,
                TestTool.stageId
            )
        } returns (
                listOf(
                    TestTool.genVmBuildContainer(vmSeqId = dependContainerId).copy(status = BuildStatus.RUNNING),
                    TestTool.genVmBuildContainer(vmSeqId = dependContainerId3).copy(status = BuildStatus.SUCCEED)
                )
                )
        Assertions.assertEquals(BuildStatus.RUNNING, dependOnControl.dependOnJobStatus(container = mockJob))

        every {
            pipelineContainerService.listContainers(
                TestTool.projectId,
                TestTool.buildId,
                TestTool.stageId
            )
        } returns (
                listOf(
                    TestTool.genVmBuildContainer(vmSeqId = dependContainerId).copy(status = BuildStatus.SUCCEED),
                    TestTool.genVmBuildContainer(vmSeqId = dependContainerId3).copy(status = BuildStatus.SUCCEED)
                )
                )
        Assertions.assertEquals(BuildStatus.SUCCEED, dependOnControl.dependOnJobStatus(container = mockJob))

        // when fail
        every {
            pipelineContainerService.listContainers(
                TestTool.projectId,
                TestTool.buildId,
                TestTool.stageId
            )
        } returns (
                listOf(
                    TestTool.genVmBuildContainer(vmSeqId = dependContainerId).copy(status = BuildStatus.RUNNING),
                    TestTool.genVmBuildContainer(vmSeqId = dependContainerId3).copy(status = BuildStatus.FAILED)
                )
                )
        Assertions.assertEquals(BuildStatus.FAILED, dependOnControl.dependOnJobStatus(container = mockJob))
    }
}

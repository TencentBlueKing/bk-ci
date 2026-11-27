/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildQueueBroadCastEvent
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.api.service.ServiceSubPipelineResource
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.PipelineBuildStage
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineContainerService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineStageService
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.event.SubPipelineRetrySyncEvent
import com.tencent.devops.process.service.builds.PipelineBuildRetryService
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_TASK_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_PIPELINE_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_PROJECT_ID
import com.tencent.devops.process.utils.PIPELINE_START_SUB_RETRY_SYNC
import com.tencent.devops.process.utils.PIPELINE_START_SUB_RUN_MODE
import com.tencent.devops.process.utils.SUB_PIPELINE_ENABLE_RETRY_SYNC_PARAM
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.script.RedisScript

@DisplayName("#8609 子流水线同步模式重试联动父流水线单步重试服务单测")
class SubPipelineRetrySyncServiceTest {

    private val redisOperation: RedisOperation = mockk(relaxed = true)
    private val sampleEventDispatcher: SampleEventDispatcher = mockk(relaxed = true)
    private val pipelineRuntimeService: PipelineRuntimeService = mockk(relaxed = true)
    private val pipelineRepositoryService: PipelineRepositoryService = mockk(relaxed = true)
    private val pipelineStageService: PipelineStageService = mockk(relaxed = true)
    private val pipelineContainerService: PipelineContainerService = mockk(relaxed = true)
    private val pipelineTaskService: PipelineTaskService = mockk(relaxed = true)
    private val pipelinePermissionService: PipelinePermissionService = mockk(relaxed = true)
    private val pipelineBuildRetryService: PipelineBuildRetryService = mockk(relaxed = true)
    private val subPipelineRetryAttachService: SubPipelineRetryAttachService = mockk(relaxed = true)
    private val client: Client = mockk(relaxed = true)

    private lateinit var service: SubPipelineRetrySyncService

    @BeforeEach
    fun setUp() {
        service = SubPipelineRetrySyncService(
            redisOperation = redisOperation,
            sampleEventDispatcher = sampleEventDispatcher,
            pipelineRuntimeService = pipelineRuntimeService,
            pipelineRepositoryService = pipelineRepositoryService,
            pipelineStageService = pipelineStageService,
            pipelineContainerService = pipelineContainerService,
            pipelineTaskService = pipelineTaskService,
            pipelinePermissionService = pipelinePermissionService,
            pipelineBuildRetryService = pipelineBuildRetryService,
            subPipelineRetryAttachService = subPipelineRetryAttachService,
            client = client
        )
    }

    @Test
    @DisplayName("onBuildQueue：存量未开启开关、异步模式或非重试启动一律静默跳过")
    fun testOnBuildQueueSkipsNonEligible() {
        // 首次启动 START -> 跳过
        service.onBuildQueue(
            PipelineBuildQueueBroadCastEvent(
                source = "startQueue",
                projectId = "p-sub",
                pipelineId = "pipe-sub",
                userId = "u1",
                buildId = "b-sub",
                actionType = ActionType.START,
                triggerType = StartType.PIPELINE.name
            )
        )
        // 异步模式 / 未开启插件开关 -> 跳过
        every { pipelineRuntimeService.getBuildVariableService("p-sub", "pipe-sub", "b-sub", any()) } returns mapOf(
            PIPELINE_START_SUB_RUN_MODE to "syn",
            PIPELINE_START_SUB_RETRY_SYNC to "false"
        )
        service.onBuildQueue(
            PipelineBuildQueueBroadCastEvent(
                source = "startQueue",
                projectId = "p-sub",
                pipelineId = "pipe-sub",
                userId = "u1",
                buildId = "b-sub",
                actionType = ActionType.RETRY,
                triggerType = StartType.PIPELINE.name
            )
        )
        verify(exactly = 0) { sampleEventDispatcher.dispatch(any()) }
    }

    @Test
    @DisplayName("onBuildQueue：同步模式且开启插件开关时派发联动事件并记录回溯链路")
    fun testOnBuildQueueDispatchesSyncEvent() {
        every { pipelineRuntimeService.getBuildVariableService("p-sub", "pipe-sub", "b-sub", any()) } returns mapOf(
            PIPELINE_START_SUB_RUN_MODE to "syn",
            PIPELINE_START_SUB_RETRY_SYNC to "true",
            PIPELINE_START_PARENT_PROJECT_ID to "p-parent",
            PIPELINE_START_PARENT_PIPELINE_ID to "pipe-parent",
            PIPELINE_START_PARENT_BUILD_ID to "b-parent",
            PIPELINE_START_PARENT_BUILD_TASK_ID to "e-task1"
        )
        val subBuild = mockk<BuildInfo>(relaxed = true)
        every { subBuild.parentBuildId } returns "b-parent"
        every { subBuild.parentTaskId } returns "e-task1"
        every { subBuild.buildNum } returns 12
        every { subBuild.executeCount } returns 2
        every { pipelineRuntimeService.getBuildInfo("p-sub", "b-sub") } returns subBuild
        val subPipeline = mockk<PipelineInfo>(relaxed = true)
        every { subPipeline.pipelineName } returns "sub-pipe-name"
        every { pipelineRepositoryService.getPipelineInfo("p-sub", "pipe-sub") } returns subPipeline

        val eventSlot = slot<SubPipelineRetrySyncEvent>()
        every { sampleEventDispatcher.dispatch(capture(eventSlot)) } returns Unit

        service.onBuildQueue(
            PipelineBuildQueueBroadCastEvent(
                source = "startQueue",
                projectId = "p-sub",
                pipelineId = "pipe-sub",
                userId = "u-retry",
                buildId = "b-sub",
                actionType = ActionType.RETRY,
                triggerType = StartType.PIPELINE.name
            )
        )

        assertEquals("p-parent", eventSlot.captured.parentProjectId)
        assertEquals("b-parent", eventSlot.captured.parentBuildId)
        assertEquals("e-task1", eventSlot.captured.parentTaskId)
        assertEquals("b-sub", eventSlot.captured.subBuildId)
        assertEquals(listOf("b-sub"), eventSlot.captured.visitedBuildIds)
    }

    @Test
    @DisplayName("onRetrySyncEvent：跨项目通过网关路由至父集群，不直接本地抢锁")
    fun testCrossProjectForward() {
        val remoteResource = mockk<ServiceSubPipelineResource>()
        every { client.getGateway(ServiceSubPipelineResource::class) } returns remoteResource
        every { remoteResource.syncParentOnSubRetry("p-parent", any()) } returns Result(true)

        service.onRetrySyncEvent(sampleEvent(parentProjectId = "p-parent", subProjectId = "p-sub"))

        verify(exactly = 1) { remoteResource.syncParentOnSubRetry("p-parent", any()) }
        verify(exactly = 0) { pipelineBuildRetryService.retryWithinLock(any(), any(), any(), any()) }
    }

    @Test
    @DisplayName("非阻塞防线：BuildIdLock.tryLock 失败立即释放并按指数退避重投 MQ")
    fun testBuildLockBusyRequeuesWithoutBlocking() {
        every { redisOperation.setNxEx(any(), any(), any(), any()) } returns false
        val requeued = slot<SubPipelineRetrySyncEvent>()
        every { sampleEventDispatcher.dispatch(capture(requeued)) } returns Unit

        service.syncParentOnSubRetry(sampleEvent(lockRetryCount = 0))

        assertEquals(1, requeued.captured.lockRetryCount)
        assertEquals(1000, requeued.captured.delayMills)
        verify(exactly = 0) { pipelineRuntimeService.getBuildInfo(any(), any()) }
    }

    @Test
    @DisplayName("防错写：父步骤已指向新子构建 b-sub-2 时，拒绝旧子构建 b-sub-1 回写")
    fun testRejectWhenSubBuildIdChanged() {
        mockAcquireAllLocks()
        val parentBuild = mockParentBuild(BuildStatus.FAILED, executeCount = 1)
        val parentTask = mockParentTask(BuildStatus.FAILED, subProjectId = "p1", subBuildId = "b-sub-2", enabled = true)
        every { pipelineRuntimeService.getBuildInfo("p1", "b-parent") } returns parentBuild
        every { pipelineTaskService.getByTaskId(null, "p1", "b-parent", "e-task1") } returns parentTask

        service.syncParentOnSubRetry(sampleEvent(parentProjectId = "p1", subProjectId = "p1", subBuildId = "b-sub-1"))

        verify(exactly = 0) { subPipelineRetryAttachService.saveAttachInfo(any()) }
        verify(exactly = 0) { pipelineBuildRetryService.retryWithinLock(any(), any(), any(), any()) }
    }

    @Test
    @DisplayName("并发并行子流水线：父构建因兄弟步骤短暂 QUEUE 且本步骤仍 FAILED 时退避重投")
    fun testRequeueWhenParentBuildQueueingAndTargetTaskStillFailed() {
        mockAcquireAllLocks()
        val parentBuild = mockParentBuild(BuildStatus.QUEUE, executeCount = 2)
        val parentTask = mockParentTask(BuildStatus.FAILED, subProjectId = "p1", subBuildId = "b-sub-1", enabled = true)
        every { pipelineRuntimeService.getBuildInfo("p1", "b-parent") } returns parentBuild
        every { pipelineTaskService.getByTaskId(null, "p1", "b-parent", "e-task1") } returns parentTask

        val requeued = slot<SubPipelineRetrySyncEvent>()
        every { sampleEventDispatcher.dispatch(capture(requeued)) } returns Unit

        service.syncParentOnSubRetry(sampleEvent(parentProjectId = "p1", subProjectId = "p1", subBuildId = "b-sub-1"))

        assertEquals(1, requeued.captured.lockRetryCount)
        assertEquals(1000, requeued.captured.delayMills)
        verify(exactly = 0) { pipelineBuildRetryService.retryWithinLock(any(), any(), any(), any()) }
    }

    @Test
    @DisplayName("父构建已失败：校验通过后写入 expectedExecuteCount=N+1 绑定凭证并复用单步重试")
    fun testSyncFinishedParentSuccess() {
        mockAcquireAllLocks()
        val parentBuild = mockParentBuild(BuildStatus.FAILED, executeCount = 1)
        val parentTask = mockParentTask(BuildStatus.FAILED, subProjectId = "p1", subBuildId = "b-sub-1", enabled = true)
        mockParentPipelineAndAuth("p1", "pipe-parent", "oauth-u1")
        every { pipelineRuntimeService.getBuildInfo("p1", "b-parent") } returns parentBuild
        every { pipelineTaskService.getByTaskId(null, "p1", "b-parent", "e-task1") } returns parentTask
        every {
            pipelineBuildRetryService.retryWithinLock(
                userId = "oauth-u1",
                projectId = "p1",
                pipelineId = "pipe-parent",
                buildId = "b-parent",
                taskId = "e-task1",
                failedContainer = false,
                skipFailedTask = false,
                isMobile = false,
                channelCode = ChannelCode.BS,
                preAcquiredStageLock = false,
                clearAttachOnRetry = false,
                frequencyLimit = false,
                extraParamMap = any()
            )
        } returns BuildId("b-parent")

        val attachSlot = slot<SubPipelineRetryAttachService.SubPipelineRetryAttachInfo>()
        every { subPipelineRetryAttachService.saveAttachInfo(capture(attachSlot)) } returns Unit

        service.syncParentOnSubRetry(sampleEvent(parentProjectId = "p1", subProjectId = "p1", subBuildId = "b-sub-1"))

        assertEquals(2, attachSlot.captured.expectedParentExecuteCount)
        assertEquals("b-sub-1", attachSlot.captured.subBuildId)
        verify(exactly = 0) { subPipelineRetryAttachService.clearTaskAttachInfo(any(), any(), any()) }
    }

    @Test
    @DisplayName("父构建运行中：满足单步重试条件时沿用当前 executeCount 并走 preAcquiredStageLock 局部重试")
    fun testSyncRunningParentSuccess() {
        mockAcquireAllLocks()
        val parentBuild = mockParentBuild(BuildStatus.RUNNING, executeCount = 1)
        val parentTask = mockParentTask(BuildStatus.FAILED, subProjectId = "p1", subBuildId = "b-sub-1", enabled = true)
        mockParentPipelineAndAuth("p1", "pipe-parent", "oauth-u1")
        every { pipelineRuntimeService.getBuildInfo("p1", "b-parent") } returns parentBuild
        every { pipelineTaskService.getByTaskId(null, "p1", "b-parent", "e-task1") } returns parentTask
        val stage = mockk<PipelineBuildStage>(relaxed = true)
        every { stage.status } returns BuildStatus.RUNNING
        every { pipelineStageService.getStage("p1", "b-parent", "stage-2") } returns stage
        val container = mockk<PipelineBuildContainer>(relaxed = true)
        every { container.status } returns BuildStatus.FAILED
        every { pipelineContainerService.getContainer("p1", "b-parent", "stage-2", "1") } returns container
        every {
            pipelineBuildRetryService.retryWithinLock(
                userId = "oauth-u1",
                projectId = "p1",
                pipelineId = "pipe-parent",
                buildId = "b-parent",
                taskId = "e-task1",
                failedContainer = false,
                skipFailedTask = false,
                isMobile = false,
                channelCode = ChannelCode.BS,
                preAcquiredStageLock = true,
                clearAttachOnRetry = false,
                frequencyLimit = false,
                extraParamMap = any()
            )
        } returns BuildId("b-parent")

        val attachSlot = slot<SubPipelineRetryAttachService.SubPipelineRetryAttachInfo>()
        every { subPipelineRetryAttachService.saveAttachInfo(capture(attachSlot)) } returns Unit

        service.syncParentOnSubRetry(sampleEvent(parentProjectId = "p1", subProjectId = "p1", subBuildId = "b-sub-1"))

        assertEquals(1, attachSlot.captured.expectedParentExecuteCount)
        assertTrue(attachSlot.isCaptured)
    }

    @Test
    @DisplayName("异常回滚：retryWithinLock 失败时立即清理已写入的一次性凭证")
    fun testRollbackAttachOnRetryFailure() {
        mockAcquireAllLocks()
        val parentBuild = mockParentBuild(BuildStatus.FAILED, executeCount = 1)
        val parentTask = mockParentTask(BuildStatus.FAILED, subProjectId = "p1", subBuildId = "b-sub-1", enabled = true)
        mockParentPipelineAndAuth("p1", "pipe-parent", "oauth-u1")
        every { pipelineRuntimeService.getBuildInfo("p1", "b-parent") } returns parentBuild
        every { pipelineTaskService.getByTaskId(null, "p1", "b-parent", "e-task1") } returns parentTask
        every {
            pipelineBuildRetryService.retryWithinLock(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        } throws RuntimeException("mock retry failure")

        service.syncParentOnSubRetry(sampleEvent(parentProjectId = "p1", subProjectId = "p1", subBuildId = "b-sub-1"))

        verify(exactly = 1) { subPipelineRetryAttachService.saveAttachInfo(any()) }
        verify(exactly = 1) { subPipelineRetryAttachService.clearTaskAttachInfo("p1", "b-parent", "e-task1") }
    }

    @Test
    @DisplayName("并发竞态保护：父构建已进入 RUNNING 但目标 Stage 尚处 QUEUE 过渡态时退避重投而非丢弃")
    fun testRunningParentStageQueueingRequeues() {
        mockAcquireAllLocks()
        val parentBuild = mockParentBuild(status = BuildStatus.RUNNING, executeCount = 2)
        every { pipelineRuntimeService.getBuildInfo("p1", "b-parent") } returns parentBuild
        every {
            pipelineTaskService.getByTaskId(null, "p1", "b-parent", "e-task1")
        } returns mockParentTask(
            status = BuildStatus.FAILED,
            subProjectId = "p1",
            subBuildId = "b-sub-1",
            enabled = true
        )

        val stage = mockk<PipelineBuildStage>(relaxed = true)
        every { stage.status } returns BuildStatus.QUEUE
        every { stage.executeCount } returns 2
        val container = mockk<PipelineBuildContainer>(relaxed = true)
        every { container.status } returns BuildStatus.FAILED
        every { pipelineStageService.getStage("p1", "b-parent", "stage-2") } returns stage
        every { pipelineContainerService.getContainer("p1", "b-parent", "stage-2", "1") } returns container

        val requeued = slot<SubPipelineRetrySyncEvent>()
        every { sampleEventDispatcher.dispatch(capture(requeued)) } returns Unit

        service.syncParentOnSubRetry(sampleEvent(lockRetryCount = 0))

        assertEquals(1, requeued.captured.lockRetryCount)
        assertEquals(1000, requeued.captured.delayMills)
        verify(exactly = 0) {
            pipelineBuildRetryService.retryWithinLock(
                any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any()
            )
        }
    }

    @Test
    @DisplayName("防环保护：visitedBuildIds 包含父构建 ID 时立即熔断")
    fun testCycleBlocked() {
        service.syncParentOnSubRetry(
            sampleEvent(
                parentProjectId = "p1",
                subProjectId = "p1",
                visitedBuildIds = listOf("b-parent", "b-sub-1")
            )
        )
        verify(exactly = 0) { redisOperation.setNxEx(any(), any(), any(), any()) }
        verify(exactly = 0) { pipelineRuntimeService.getBuildInfo(any(), any()) }
    }

    private fun mockAcquireAllLocks() {
        every {
            redisOperation.setNxEx(
                key = any(),
                value = any(),
                expiredInSecond = any(),
                isRedisLock = any()
            )
        } returns true
        every {
            redisOperation.execute(
                script = any<RedisScript<Long>>(),
                keys = any(),
                args = anyVararg(),
                isRedisLock = any()
            )
        } returns 1L
    }

    private fun mockParentPipelineAndAuth(projectId: String, pipelineId: String, oauthUser: String) {
        val pipelineInfo = mockk<PipelineInfo>(relaxed = true)
        every { pipelineInfo.locked } returns false
        every { pipelineInfo.channelCode } returns ChannelCode.BS
        every { pipelineRepositoryService.getPipelineInfo(projectId, pipelineId) } returns pipelineInfo
        every { pipelineRepositoryService.getPipelineOauthUser(projectId, pipelineId) } returns oauthUser
        every {
            pipelinePermissionService.checkPipelinePermission(
                userId = oauthUser,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.EXECUTE
            )
        } returns true
    }

    private fun mockParentBuild(status: BuildStatus, executeCount: Int): BuildInfo {
        val build = mockk<BuildInfo>(relaxed = true)
        every { build.pipelineId } returns "pipe-parent"
        every { build.buildId } returns "b-parent"
        every { build.status } returns status
        every { build.executeCount } returns executeCount
        every { build.startUser } returns "start-user"
        every { build.channelCode } returns ChannelCode.BS
        every { build.isFinish() } returns status.isFinish()
        every { build.isStageSuccess() } returns (status == BuildStatus.STAGE_SUCCESS)
        return build
    }

    private fun mockParentTask(
        status: BuildStatus,
        subProjectId: String,
        subBuildId: String,
        enabled: Boolean
    ): PipelineBuildTask {
        val task = mockk<PipelineBuildTask>(relaxed = true)
        every { task.stageId } returns "stage-2"
        every { task.containerId } returns "1"
        every { task.taskId } returns "e-task1"
        every { task.status } returns status
        every { task.subProjectId } returns subProjectId
        every { task.subBuildId } returns subBuildId
        every { task.taskParams } returns mutableMapOf(
            "data" to mapOf("input" to mapOf(SUB_PIPELINE_ENABLE_RETRY_SYNC_PARAM to enabled))
        )
        return task
    }

    private fun sampleEvent(
        parentProjectId: String = "p1",
        subProjectId: String = "p1",
        subBuildId: String = "b-sub-1",
        lockRetryCount: Int = 0,
        visitedBuildIds: List<String> = listOf(subBuildId)
    ) = SubPipelineRetrySyncEvent(
        parentProjectId = parentProjectId,
        parentPipelineId = "pipe-parent",
        parentBuildId = "b-parent",
        parentTaskId = "e-task1",
        subProjectId = subProjectId,
        subPipelineId = "pipe-sub",
        subPipelineName = "sub-pipeline",
        subBuildId = subBuildId,
        subBuildNum = 8,
        subExecuteCount = 2,
        operator = "tester",
        visitedBuildIds = visitedBuildIds,
        lockRetryCount = lockRetryCount
    )
}

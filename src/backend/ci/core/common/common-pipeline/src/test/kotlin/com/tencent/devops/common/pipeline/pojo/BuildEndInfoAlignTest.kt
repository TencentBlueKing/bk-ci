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
 * documentation files (the Software), to deal in the Software without restriction, including without limitation the
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

package com.tencent.devops.common.pipeline.pojo

import com.tencent.devops.common.pipeline.enums.BuildEndCategory
import com.tencent.devops.common.pipeline.enums.BuildEndType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * 覆盖终态卡片大类与构建最终状态错配：取消链路提前落库后构建以失败/成功/超时收尾。
 */
class BuildEndInfoAlignTest {

    @Test
    fun `category of build status covers cancel fail timeout success`() {
        Assertions.assertEquals(BuildEndCategory.CANCEL, BuildEndCategory.of(BuildStatus.CANCELED))
        Assertions.assertEquals(BuildEndCategory.FAIL, BuildEndCategory.of(BuildStatus.FAILED))
        Assertions.assertEquals(BuildEndCategory.FAIL, BuildEndCategory.of(BuildStatus.TERMINATE))
        Assertions.assertEquals(BuildEndCategory.FAIL, BuildEndCategory.of(BuildStatus.REVIEW_ABORT))
        Assertions.assertEquals(BuildEndCategory.FAIL, BuildEndCategory.of(BuildStatus.HEARTBEAT_TIMEOUT))
        Assertions.assertEquals(BuildEndCategory.FAIL, BuildEndCategory.of(BuildStatus.EXEC_TIMEOUT))
        Assertions.assertEquals(BuildEndCategory.TIMEOUT, BuildEndCategory.of(BuildStatus.QUEUE_TIMEOUT))
        Assertions.assertEquals(BuildEndCategory.SUCCESS, BuildEndCategory.of(BuildStatus.SUCCEED))
        Assertions.assertEquals(BuildEndCategory.SUCCESS, BuildEndCategory.of(BuildStatus.STAGE_SUCCESS))
        Assertions.assertNull(BuildEndCategory.of(BuildStatus.RUNNING))
        Assertions.assertNull(BuildEndCategory.of(BuildStatus.PAUSE))
    }

    @Test
    fun `given cancel info and failed status then aligned to fail exec`() {
        val stored = BuildEndInfo.ofCancelUser(operator = "ccc", reasonCode = "bkBuildCancelUserInFlightStopped")
            .withPositions(
                listOf(
                    EndPosition(
                        position = "1-1-2",
                        componentPath = "stage-1/构建环境-Linux/0806插件",
                        statusAtEnd = BuildStatus.PAUSE.name,
                        stageId = "stage-2",
                        containerId = "1",
                        taskId = "e-47733d4fa30f41e9ab1c91f575b3d8f2"
                    )
                )
            )

        val aligned = stored.alignedTo(BuildStatus.FAILED) { BuildStatus.FAILED.name }

        Assertions.assertNotNull(aligned)
        Assertions.assertEquals(BuildEndType.FAIL_EXEC, aligned!!.endType)
        Assertions.assertNull(aligned.operator)
        Assertions.assertNull(aligned.reasonCode)
        val position = aligned.positions!!.single()
        Assertions.assertEquals(BuildStatus.FAILED.name, position.statusAtEnd)
        Assertions.assertEquals("1-1-2", position.position)
    }

    @Test
    fun `given cancel info and model fail positions then card uses plugin final status`() {
        val stored = BuildEndInfo.ofCancelUser(operator = "ccc", reasonCode = "bkBuildCancelUserInFlightStopped")
            .withPositions(
                listOf(
                    EndPosition(
                        position = "1-1-2",
                        componentPath = "stage-1/构建环境-Linux/0806插件",
                        statusAtEnd = BuildStatus.PAUSE.name,
                        stageId = "stage-2",
                        containerId = "1",
                        taskId = "e-0806"
                    )
                )
            )
        val modelPositions = listOf(
            EndPosition(
                position = "1-1-2",
                componentPath = "stage-1/构建环境-Linux/0806插件",
                statusAtEnd = BuildStatus.FAILED.name,
                endType = BuildEndType.FAIL_EXEC,
                stageId = "stage-2",
                containerId = "1",
                taskId = "e-0806"
            ),
            EndPosition(
                position = "1-1-4",
                componentPath = "stage-1/构建环境-Linux/人工审核",
                statusAtEnd = BuildStatus.REVIEW_ABORT.name,
                endType = BuildEndType.FAIL_REVIEW,
                stageId = "stage-2",
                containerId = "1",
                taskId = "e-review"
            )
        )

        val aligned = stored.alignedTo(BuildStatus.FAILED, modelFailPositions = modelPositions)

        Assertions.assertEquals(BuildEndType.FAIL_MULTIPLE, aligned!!.endType)
        Assertions.assertNull(aligned.operator)
        Assertions.assertEquals(2, aligned.positionCount)
        Assertions.assertEquals(BuildStatus.FAILED.name, aligned.positions!![0].statusAtEnd)
        Assertions.assertEquals(BuildStatus.REVIEW_ABORT.name, aligned.positions!![1].statusAtEnd)
    }

    @Test
    fun `given cancel info and succeed status then aligned drops leftover`() {
        val stored = BuildEndInfo.ofCancelSystem(reasonCode = "bkBuildCancelSystemHeartbeat")

        Assertions.assertNull(stored.alignedTo(BuildStatus.SUCCEED))
        Assertions.assertFalse(stored.matchesBuildStatus(BuildStatus.SUCCEED))
        Assertions.assertTrue(stored.matchesBuildStatus(BuildStatus.CANCELED))
    }

    @Test
    fun `given system cancel info and failed status then keep system cause`() {
        val stored = BuildEndInfo.ofCancelSystem(
            reasonCode = "bkBuildCancelSystemHeartbeat",
            reasonParams = listOf("agent-1")
        )

        val aligned = stored.alignedTo(BuildStatus.FAILED)

        Assertions.assertEquals(BuildEndType.FAIL_EXEC, aligned!!.endType)
        Assertions.assertEquals("bkBuildCancelSystemHeartbeat", aligned.reasonCode)
        Assertions.assertEquals(listOf("agent-1"), aligned.reasonParams)
    }

    @Test
    fun `given cancel info and queue timeout then aligned to timeout queue`() {
        val stored = BuildEndInfo.ofCancelUser(operator = "ccc", reasonCode = "bkBuildCancelUserManual")

        val aligned = stored.alignedTo(BuildStatus.QUEUE_TIMEOUT)

        Assertions.assertEquals(BuildEndType.TIMEOUT_QUEUE, aligned!!.endType)
    }

    @Test
    fun `given fail info and failed status then keep original`() {
        val stored = BuildEndInfo.of(endType = BuildEndType.FAIL_REVIEW, reason = "驳回")

        Assertions.assertSame(stored, stored.alignedTo(BuildStatus.FAILED))
    }

    @Test
    fun `given matching cancel info then refresh stale pause status`() {
        val stored = BuildEndInfo.ofCancelSystem(reasonCode = "bkBuildCancelSystemJobExecTimeout")
            .withPositions(
                listOf(
                    EndPosition(
                        position = "1-1-2",
                        componentPath = "stage-1/构建环境-Linux/0806插件",
                        statusAtEnd = BuildStatus.PAUSE.name,
                        stageId = "stage-2",
                        containerId = "1",
                        taskId = "e-0806"
                    )
                )
            )

        val aligned = stored.alignedTo(BuildStatus.CANCELED) { BuildStatus.CANCELED.name }

        Assertions.assertEquals(BuildEndType.CANCEL_SYSTEM, aligned!!.endType)
        Assertions.assertEquals("bkBuildCancelSystemJobExecTimeout", aligned.reasonCode)
        Assertions.assertEquals(BuildStatus.CANCELED.name, aligned.positions!!.single().statusAtEnd)
    }

    @Test
    fun `given matching fail info then fill missing reason and pause position`() {
        val stored = BuildEndInfo.of(endType = BuildEndType.FAIL_EXEC)
            .withPositions(
                listOf(
                    EndPosition(
                        position = "1-1-2",
                        componentPath = "stage-1/构建环境-Linux/0806插件",
                        statusAtEnd = BuildStatus.FAILED.name,
                        endType = BuildEndType.FAIL_EXEC,
                        stageId = "stage-2",
                        containerId = "1",
                        taskId = "e-timeout"
                    )
                )
            )
        val modelPositions = listOf(
            EndPosition(
                position = "1-1-2",
                componentPath = "stage-1/构建环境-Linux/0806插件",
                statusAtEnd = BuildStatus.FAILED.name,
                endType = BuildEndType.TIMEOUT_JOB,
                reasonCode = "bkBuildCancelSystemJobExecTimeout",
                reasonParams = listOf("1"),
                stageId = "stage-2",
                containerId = "1",
                taskId = "e-timeout"
            ),
            EndPosition(
                position = "1-1-1",
                componentPath = "stage-1/构建环境-Linux/0806插件",
                statusAtEnd = BuildStatus.CANCELED.name,
                endType = BuildEndType.FAIL_EXEC,
                reasonCode = "bkBuildEndFailPauseTerminated",
                stageId = "stage-2",
                containerId = "1",
                taskId = "e-pause"
            )
        )

        val aligned = stored.alignedTo(BuildStatus.FAILED, modelFailPositions = modelPositions)

        Assertions.assertEquals(2, aligned!!.positionCount)
        Assertions.assertEquals("bkBuildCancelSystemJobExecTimeout", aligned.positions!![0].reasonCode)
        Assertions.assertEquals("e-pause", aligned.positions!![1].taskId)
        Assertions.assertEquals("bkBuildEndFailPauseTerminated", aligned.positions!![1].reasonCode)
    }

    @Test
    fun `given matching cancel info then merge missing cancel plugins`() {
        val stored = BuildEndInfo.ofCancelSystem(reasonCode = "bkBuildCancelSystemJobExecTimeout")
            .withPositions(
                listOf(
                    EndPosition(
                        position = "1-3-2",
                        componentPath = "stage-1/构建环境-Linux/0806插件",
                        statusAtEnd = BuildStatus.PAUSE.name,
                        stageId = "stage-2",
                        containerId = "3",
                        taskId = "e-0806-a"
                    )
                )
            )
        val modelCancelPositions = listOf(
            EndPosition(
                position = "1-3-2",
                componentPath = "stage-1/构建环境-Linux/0806插件",
                statusAtEnd = BuildStatus.CANCELED.name,
                reasonCode = "bkBuildEndFailPauseTerminated",
                stageId = "stage-2",
                containerId = "3",
                taskId = "e-0806-a"
            ),
            EndPosition(
                position = "1-4-2",
                componentPath = "stage-1/构建环境-Linux/0806插件",
                statusAtEnd = BuildStatus.CANCELED.name,
                reasonCode = "bkBuildEndFailPauseTerminated",
                stageId = "stage-2",
                containerId = "4",
                taskId = "e-0806-b"
            )
        )

        val aligned = stored.alignedTo(
            status = BuildStatus.CANCELED,
            modelCancelPositions = modelCancelPositions
        ) { BuildStatus.CANCELED.name }

        Assertions.assertEquals(BuildEndType.CANCEL_SYSTEM, aligned!!.endType)
        Assertions.assertEquals("bkBuildCancelSystemJobExecTimeout", aligned.reasonCode)
        Assertions.assertEquals(2, aligned.positionCount)
        Assertions.assertEquals("e-0806-a", aligned.positions!![0].taskId)
        Assertions.assertEquals(BuildStatus.CANCELED.name, aligned.positions!![0].statusAtEnd)
        Assertions.assertEquals("bkBuildEndFailPauseTerminated", aligned.positions!![0].reasonCode)
        Assertions.assertEquals("e-0806-b", aligned.positions!![1].taskId)
    }

    @Test
    fun `given matching user cancel then merge later canceled plugins`() {
        val stored = BuildEndInfo.ofCancelUser(operator = "ccc", reasonCode = "bkBuildCancelUserInFlightStopped")
            .withPositions(
                listOf(
                    EndPosition(
                        position = "1-1-2",
                        componentPath = "stage-1/构建环境-Linux/0806插件",
                        statusAtEnd = BuildStatus.PAUSE.name,
                        stageId = "stage-2",
                        containerId = "1",
                        taskId = "e-0806-a"
                    )
                )
            )
        val modelCancelPositions = listOf(
            EndPosition(
                position = "1-1-2",
                componentPath = "stage-1/构建环境-Linux/0806插件",
                statusAtEnd = BuildStatus.CANCELED.name,
                stageId = "stage-2",
                containerId = "1",
                taskId = "e-0806-a"
            ),
            EndPosition(
                position = "1-2-2",
                componentPath = "stage-1/构建环境-Linux/0806插件",
                statusAtEnd = BuildStatus.CANCELED.name,
                stageId = "stage-2",
                containerId = "2",
                taskId = "e-0806-b"
            )
        )

        val aligned = stored.alignedTo(
            status = BuildStatus.CANCELED,
            modelCancelPositions = modelCancelPositions
        ) { BuildStatus.CANCELED.name }

        Assertions.assertEquals(BuildEndType.CANCEL_USER, aligned!!.endType)
        Assertions.assertEquals(2, aligned.positionCount)
        Assertions.assertEquals("ccc", aligned.operator)
    }

    @Test
    fun `given matching fail info then do not merge cancel extras`() {
        val stored = BuildEndInfo.of(endType = BuildEndType.FAIL_EXEC)
            .withPositions(
                listOf(
                    EndPosition(
                        position = "1-1-1",
                        componentPath = "stage-1/构建环境-Linux/Bash",
                        statusAtEnd = BuildStatus.FAILED.name,
                        endType = BuildEndType.FAIL_EXEC,
                        stageId = "stage-2",
                        containerId = "1",
                        taskId = "e-fail"
                    )
                )
            )
        val modelCancelPositions = listOf(
            EndPosition(
                position = "1-2-2",
                componentPath = "stage-1/构建环境-Linux/0806插件",
                statusAtEnd = BuildStatus.CANCELED.name,
                stageId = "stage-2",
                containerId = "2",
                taskId = "e-cancel"
            )
        )

        val aligned = stored.alignedTo(
            status = BuildStatus.FAILED,
            modelCancelPositions = modelCancelPositions
        )

        Assertions.assertSame(stored, aligned)
        Assertions.assertEquals(1, aligned!!.positionCount)
    }

    @Test
    fun `job timeout snapshot of one plugin is replaced by all paused plugins`() {
        val stored = BuildEndInfo.ofCancelSystem(reasonCode = "bkBuildCancelSystemJobExecTimeout")
            .withPositions(
                listOf(
                    EndPosition(
                        position = "1-3-2",
                        componentPath = "stage-1/构建环境-Linux/0806插件",
                        statusAtEnd = BuildStatus.CANCELED.name,
                        stageId = "stage-2",
                        containerId = "3",
                        taskId = "e-0806-a"
                    )
                )
            )
        val modelCancelPositions = listOf(
            EndPosition(
                position = "1-3-2",
                componentPath = "stage-1/构建环境-Linux/0806插件",
                statusAtEnd = BuildStatus.PAUSE.name,
                reasonCode = "bkBuildEndFailPauseTerminated",
                stageId = "stage-2",
                containerId = "3",
                taskId = "e-0806-a"
            ),
            EndPosition(
                position = "1-4-2",
                componentPath = "stage-1/构建环境-Linux/0806插件",
                statusAtEnd = BuildStatus.PAUSE.name,
                reasonCode = "bkBuildEndFailPauseTerminated",
                stageId = "stage-2",
                containerId = "4",
                taskId = "e-0806-b"
            )
        )

        val aligned = stored.alignedTo(
            status = BuildStatus.CANCELED,
            modelCancelPositions = modelCancelPositions
        )

        Assertions.assertEquals(BuildEndType.CANCEL_SYSTEM, aligned!!.endType)
        Assertions.assertEquals(2, aligned.positionCount)
        Assertions.assertEquals(BuildStatus.PAUSE.name, aligned.positions!![0].statusAtEnd)
        Assertions.assertEquals(BuildStatus.PAUSE.name, aligned.positions!![1].statusAtEnd)
        Assertions.assertEquals(listOf("e-0806-a", "e-0806-b"), aligned.positions!!.map { it.taskId })
    }

    @Test
    fun `given job level cancel then drop job row after merging tasks of same container`() {
        val stored = BuildEndInfo.ofCancelSystem(reasonCode = "bkBuildCancelSystemJobExecTimeout")
            .withPositions(
                listOf(
                    EndPosition(
                        position = "1-3",
                        componentPath = "stage-1/构建环境-Linux",
                        statusAtEnd = BuildStatus.PREPARE_ENV.name,
                        stageId = "stage-2",
                        containerId = "3"
                    )
                )
            )
        val modelCancelPositions = listOf(
            EndPosition(
                position = "1-3-2",
                componentPath = "stage-1/构建环境-Linux/0806插件",
                statusAtEnd = BuildStatus.CANCELED.name,
                reasonCode = "bkBuildEndFailPauseTerminated",
                stageId = "stage-2",
                containerId = "3",
                taskId = "e-0806-a"
            ),
            EndPosition(
                position = "1-4-2",
                componentPath = "stage-1/构建环境-Linux/0806插件",
                statusAtEnd = BuildStatus.CANCELED.name,
                reasonCode = "bkBuildEndFailPauseTerminated",
                stageId = "stage-2",
                containerId = "4",
                taskId = "e-0806-b"
            )
        )

        val aligned = stored.alignedTo(
            status = BuildStatus.CANCELED,
            modelCancelPositions = modelCancelPositions
        )

        Assertions.assertEquals(2, aligned!!.positionCount)
        Assertions.assertTrue(aligned.positions!!.none { it.taskId.isNullOrBlank() })
        Assertions.assertEquals(listOf("e-0806-a", "e-0806-b"), aligned.positions!!.map { it.taskId })
    }

    @Test
    fun `preserve system cause when fail card has no reason`() {
        val existing = BuildEndInfo.ofCancelSystem(
            reasonCode = "bkBuildCancelSystemHeartbeat",
            reasonParams = listOf("agent-1")
        )
        val incoming = BuildEndInfo.of(endType = BuildEndType.FAIL_EXEC)

        val merged = incoming.preserveSystemCause(existing)

        Assertions.assertEquals(BuildEndType.FAIL_EXEC, merged.endType)
        Assertions.assertEquals("bkBuildCancelSystemHeartbeat", merged.reasonCode)
        Assertions.assertEquals(listOf("agent-1"), merged.reasonParams)
    }

    @Test
    fun `preserve system cause does not override incoming fail reason`() {
        val existing = BuildEndInfo.ofCancelSystem(reasonCode = "bkBuildCancelSystemHeartbeat")
        val incoming = BuildEndInfo.of(endType = BuildEndType.FAIL_QUALITY, reason = "指标超标")

        val merged = incoming.preserveSystemCause(existing)

        Assertions.assertEquals("指标超标", merged.reason)
        Assertions.assertNull(merged.reasonCode)
    }
}

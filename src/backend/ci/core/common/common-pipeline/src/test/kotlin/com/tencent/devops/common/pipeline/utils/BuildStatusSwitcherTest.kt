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

package com.tencent.devops.common.pipeline.utils

import com.tencent.devops.common.pipeline.enums.BuildStatus
import org.junit.Assert
import org.junit.Test

@Suppress("ALL")
class BuildStatusSwitcherTest {

    @Test
    fun pipelineFinish() {
        BuildStatus.values().forEach { status ->
            when {
                status == BuildStatus.QUEUE_TIMEOUT -> {
                    Assert.assertEquals(
                        BuildStatus.QUEUE_TIMEOUT,
                        BuildStatusSwitcher.pipelineStatusMaker.finish(status)
                    )
                }
                status == BuildStatus.STAGE_SUCCESS -> {
                    Assert.assertEquals(
                        BuildStatus.STAGE_SUCCESS,
                        BuildStatusSwitcher.pipelineStatusMaker.finish(status)
                    )
                }
                status == BuildStatus.TERMINATE -> {
                    Assert.assertEquals(
                        BuildStatus.TERMINATE,
                        BuildStatusSwitcher.pipelineStatusMaker.finish(status)
                    )
                }
                status.isReadyToRun() -> {
                    Assert.assertEquals(BuildStatus.CANCELED, BuildStatusSwitcher.pipelineStatusMaker.finish(status))
                }
                status.isRunning() -> {
                    Assert.assertEquals(BuildStatus.SUCCEED, BuildStatusSwitcher.pipelineStatusMaker.finish(status))
                }
                status == BuildStatus.SKIP -> {
                    Assert.assertEquals(BuildStatus.SUCCEED, BuildStatusSwitcher.pipelineStatusMaker.finish(status))
                }
                status.isSuccess() -> {
                    Assert.assertEquals(BuildStatus.SUCCEED, BuildStatusSwitcher.pipelineStatusMaker.finish(status))
                }
                status.isCancel() -> {
                    Assert.assertEquals(status, BuildStatusSwitcher.pipelineStatusMaker.finish(status))
                }
                status.isFailure() -> {
                    Assert.assertEquals(BuildStatus.FAILED, BuildStatusSwitcher.pipelineStatusMaker.finish(status))
                }
                status.isFinish() -> {
                    Assert.assertTrue(
                        BuildStatusSwitcher.pipelineStatusMaker.statusSet().contains(
                            BuildStatusSwitcher.pipelineStatusMaker.finish(status)
                        )
                    )
                }
                else -> {
                    Assert.assertEquals(BuildStatus.SUCCEED, BuildStatusSwitcher.pipelineStatusMaker.finish(status))
                }
            }
        }
    }

    @Test
    fun pipelineCancel() {
        BuildStatus.values().forEach { status ->
            when {
                status.isReadyToRun() -> {
                    Assert.assertEquals(BuildStatus.CANCELED, BuildStatusSwitcher.pipelineStatusMaker.cancel(status))
                }
                status.isRunning() -> {
                    Assert.assertEquals(BuildStatus.CANCELED, BuildStatusSwitcher.pipelineStatusMaker.cancel(status))
                }
                status.isFinish() -> {
                    Assert.assertTrue(
                        BuildStatusSwitcher.pipelineStatusMaker.statusSet().contains(
                            BuildStatusSwitcher.pipelineStatusMaker.cancel(status)
                        )
                    )
                }
                else -> {
                    Assert.assertEquals(BuildStatus.CANCELED, BuildStatusSwitcher.pipelineStatusMaker.cancel(status))
                }
            }
        }
    }

    @Test
    fun pipelineForceFinish() {

        BuildStatus.values().forEach { status ->
            when {
                status.isSuccess() -> {
                    Assert.assertEquals(
                        BuildStatus.SUCCEED,
                        BuildStatusSwitcher.pipelineStatusMaker.forceFinish(status)
                    )
                }
                status == BuildStatus.QUEUE_TIMEOUT -> {
                    Assert.assertEquals(
                        BuildStatus.QUEUE_TIMEOUT,
                        BuildStatusSwitcher.pipelineStatusMaker.forceFinish(status)
                    )
                }
                status == BuildStatus.TERMINATE -> {
                    Assert.assertEquals(
                        BuildStatus.TERMINATE,
                        BuildStatusSwitcher.pipelineStatusMaker.forceFinish(status)
                    )
                }
                status.isFailure() -> {
                    Assert.assertEquals(
                        BuildStatus.FAILED,
                        BuildStatusSwitcher.pipelineStatusMaker.forceFinish(status)
                    )
                }
                status.isFinish() -> {
                    Assert.assertTrue(
                        BuildStatusSwitcher.pipelineStatusMaker.statusSet().contains(
                            BuildStatusSwitcher.pipelineStatusMaker.forceFinish(status)
                        )
                    )
                }
                status == BuildStatus.STAGE_SUCCESS -> {
                    Assert.assertEquals(
                        BuildStatus.STAGE_SUCCESS,
                        BuildStatusSwitcher.pipelineStatusMaker.forceFinish(status)
                    )
                }
                else -> {
                    Assert.assertEquals(
                        BuildStatus.FAILED,
                        BuildStatusSwitcher.pipelineStatusMaker.forceFinish(status)
                    )
                }
            }
        }
    }

    @Test
    fun stageCancel() {
        BuildStatus.values().forEach { stageStatus ->
            when {
                stageStatus == BuildStatus.UNKNOWN -> {
                    Assert.assertEquals(BuildStatus.CANCELED, BuildStatusSwitcher.stageStatusMaker.cancel(stageStatus))
                }
                stageStatus.isReadyToRun() -> {
                    Assert.assertEquals(BuildStatus.CANCELED, BuildStatusSwitcher.stageStatusMaker.cancel(stageStatus))
                }
                stageStatus.isRunning() -> {
                    Assert.assertEquals(BuildStatus.CANCELED, BuildStatusSwitcher.stageStatusMaker.cancel(stageStatus))
                }
                stageStatus.isFinish() -> {
                    Assert.assertTrue(
                        BuildStatusSwitcher.jobStatusMaker.statusSet().contains(
                            BuildStatusSwitcher.stageStatusMaker.cancel(stageStatus)
                        )
                    )
                }
                else -> {
                    Assert.assertEquals(BuildStatus.CANCELED, BuildStatusSwitcher.stageStatusMaker.cancel(stageStatus))
                }
            }
        }
    }

    @Test
    fun stageFinish() {
        BuildStatus.values().forEach { stageStatus ->
            when {
                stageStatus == BuildStatus.QUEUE_TIMEOUT -> {
                    Assert.assertEquals(
                        BuildStatus.QUEUE_TIMEOUT,
                        BuildStatusSwitcher.stageStatusMaker.forceFinish(stageStatus)
                    )
                }
                stageStatus.isFinish() -> {
                    Assert.assertTrue(
                        BuildStatusSwitcher.stageStatusMaker.statusSet().contains(
                            BuildStatusSwitcher.stageStatusMaker.finish(stageStatus)
                        )
                    )
                }
                stageStatus == BuildStatus.STAGE_SUCCESS -> {
                    Assert.assertEquals(BuildStatus.STAGE_SUCCESS,
                        BuildStatusSwitcher.stageStatusMaker.finish(stageStatus)
                    )
                }
                stageStatus.isReadyToRun() -> {
                    Assert.assertEquals(BuildStatus.CANCELED, BuildStatusSwitcher.stageStatusMaker.finish(stageStatus))
                }
                else -> {
                    Assert.assertEquals(BuildStatus.SUCCEED, BuildStatusSwitcher.stageStatusMaker.finish(stageStatus))
                }
            }
        }
    }

    @Test
    fun stageForceFinish() {

        BuildStatus.values().forEach { stageStatus ->
            when {
                stageStatus == BuildStatus.QUEUE_TIMEOUT -> {
                    Assert.assertEquals(
                        BuildStatus.QUEUE_TIMEOUT,
                        BuildStatusSwitcher.stageStatusMaker.forceFinish(stageStatus)
                    )
                }
                stageStatus.isFinish() -> {
                    Assert.assertTrue(
                        BuildStatusSwitcher.stageStatusMaker.statusSet().contains(
                            BuildStatusSwitcher.stageStatusMaker.forceFinish(stageStatus)
                        )
                    )
                }
                stageStatus == BuildStatus.STAGE_SUCCESS -> {
                    Assert.assertEquals(
                        BuildStatus.STAGE_SUCCESS,
                        BuildStatusSwitcher.stageStatusMaker.finish(stageStatus)
                    )
                }
                else -> {
                    Assert.assertEquals(
                        BuildStatus.FAILED,
                        BuildStatusSwitcher.stageStatusMaker.forceFinish(stageStatus)
                    )
                }
            }
        }
    }

    @Test
    fun jobCancel() {
        BuildStatus.values().forEach { jobStatus ->
            when {
                jobStatus == BuildStatus.UNKNOWN -> {
                    Assert.assertEquals(BuildStatus.CANCELED, BuildStatusSwitcher.jobStatusMaker.cancel(jobStatus))
                }
                jobStatus.isReadyToRun() -> {
                    Assert.assertEquals(BuildStatus.CANCELED, BuildStatusSwitcher.jobStatusMaker.cancel(jobStatus))
                }
                jobStatus.isRunning() -> {
                    Assert.assertEquals(BuildStatus.CANCELED, BuildStatusSwitcher.jobStatusMaker.cancel(jobStatus))
                }
                jobStatus.isFinish() -> {
                    Assert.assertTrue(
                        BuildStatusSwitcher.jobStatusMaker.statusSet().contains(
                            BuildStatusSwitcher.jobStatusMaker.cancel(jobStatus)
                        )
                    )
                }
                else -> {
                    Assert.assertEquals(BuildStatus.CANCELED, BuildStatusSwitcher.jobStatusMaker.cancel(jobStatus))
                }
            }
        }
    }

    @Test
    fun jobFinish() {

        BuildStatus.values().forEach { jobStatus ->
            when {
                jobStatus.isFinish() -> {
                    Assert.assertTrue(
                        BuildStatusSwitcher.jobStatusMaker.statusSet().contains(
                            BuildStatusSwitcher.jobStatusMaker.finish(jobStatus)
                        )
                    )
                }
                jobStatus.isReadyToRun() -> {
                    Assert.assertEquals(BuildStatus.CANCELED, BuildStatusSwitcher.jobStatusMaker.finish(jobStatus))
                }
                else -> {
                    Assert.assertEquals(BuildStatus.SUCCEED, BuildStatusSwitcher.jobStatusMaker.finish(jobStatus))
                }
            }
        }
    }

    @Test
    fun jobForceFinish() {

        BuildStatus.values().forEach { jobStatus ->
            when {
                jobStatus.isFinish() -> {
                    Assert.assertTrue(
                        BuildStatusSwitcher.jobStatusMaker.statusSet().contains(
                            BuildStatusSwitcher.jobStatusMaker.forceFinish(jobStatus)
                        )
                    )
                }
                else -> {
                    Assert.assertEquals(BuildStatus.FAILED, BuildStatusSwitcher.jobStatusMaker.forceFinish(jobStatus))
                }
            }
        }
    }

    @Test
    fun taskFinish() {
        BuildStatus.values().forEach { status ->
            when {
                status == BuildStatus.REVIEW_ABORT -> {
                    Assert.assertEquals(BuildStatus.REVIEW_ABORT, BuildStatusSwitcher.taskStatusMaker.finish(status))
                }
                status == BuildStatus.REVIEW_PROCESSED -> {
                    Assert.assertEquals(
                        BuildStatus.REVIEW_PROCESSED,
                        BuildStatusSwitcher.taskStatusMaker.finish(status)
                    )
                }
                status == BuildStatus.QUALITY_CHECK_FAIL -> {
                    Assert.assertEquals(
                        BuildStatus.QUALITY_CHECK_FAIL,
                        BuildStatusSwitcher.taskStatusMaker.finish(status)
                    )
                }
                status == BuildStatus.TERMINATE -> {
                    Assert.assertEquals(BuildStatus.TERMINATE, BuildStatusSwitcher.taskStatusMaker.finish(status))
                }
                status.isReadyToRun() -> {
                    Assert.assertEquals(BuildStatus.CANCELED, BuildStatusSwitcher.taskStatusMaker.finish(status))
                }
                status.isRunning() -> {
                    Assert.assertEquals(BuildStatus.SUCCEED, BuildStatusSwitcher.taskStatusMaker.finish(status))
                }
                status == BuildStatus.SKIP -> {
                    Assert.assertEquals(BuildStatus.SKIP, BuildStatusSwitcher.taskStatusMaker.finish(status))
                }
                status.isSuccess() -> {
                    Assert.assertEquals(BuildStatus.SUCCEED, BuildStatusSwitcher.taskStatusMaker.finish(status))
                }
                status.isCancel() -> {
                    Assert.assertEquals(status, BuildStatusSwitcher.taskStatusMaker.finish(status))
                }
                status == BuildStatus.QUEUE_TIMEOUT -> {
                    Assert.assertEquals(BuildStatus.QUEUE_TIMEOUT, BuildStatusSwitcher.taskStatusMaker.finish(status))
                }
                status.isFailure() -> {
                    Assert.assertEquals(BuildStatus.FAILED, BuildStatusSwitcher.taskStatusMaker.finish(status))
                }
                status.isFinish() -> {
                    Assert.assertTrue(
                        BuildStatusSwitcher.taskStatusMaker.statusSet().contains(
                            BuildStatusSwitcher.taskStatusMaker.finish(status)
                        )
                    )
                }
                else -> {
                    Assert.assertEquals(BuildStatus.SUCCEED, BuildStatusSwitcher.taskStatusMaker.finish(status))
                }
            }
        }
    }

    @Test
    fun taskCancel() {
        BuildStatus.values().forEach { status ->
            when {
                status.isReadyToRun() -> {
                    Assert.assertEquals(BuildStatus.CANCELED, BuildStatusSwitcher.taskStatusMaker.cancel(status))
                }
                status.isRunning() -> {
                    Assert.assertEquals(BuildStatus.CANCELED, BuildStatusSwitcher.taskStatusMaker.cancel(status))
                }
                status.isFinish() -> {
                    Assert.assertTrue(
                        BuildStatusSwitcher.taskStatusMaker.statusSet().contains(
                            BuildStatusSwitcher.taskStatusMaker.cancel(status)
                        )
                    )
                }
                else -> {
                    Assert.assertEquals(BuildStatus.CANCELED, BuildStatusSwitcher.taskStatusMaker.cancel(status))
                }
            }
        }
    }

    @Test
    fun taskForceFinish() {

        BuildStatus.values().forEach { status ->
            when {
                status == BuildStatus.REVIEW_ABORT -> {
                    Assert.assertEquals(
                        BuildStatus.REVIEW_ABORT,
                        BuildStatusSwitcher.taskStatusMaker.forceFinish(status)
                    )
                }
                status == BuildStatus.REVIEW_PROCESSED -> {
                    Assert.assertEquals(
                        BuildStatus.REVIEW_PROCESSED,
                        BuildStatusSwitcher.taskStatusMaker.forceFinish(status)
                    )
                }
                status == BuildStatus.QUALITY_CHECK_FAIL -> {
                    Assert.assertEquals(
                        BuildStatus.QUALITY_CHECK_FAIL,
                        BuildStatusSwitcher.taskStatusMaker.forceFinish(status)
                    )
                }
                status == BuildStatus.TERMINATE -> {
                    Assert.assertEquals(BuildStatus.TERMINATE, BuildStatusSwitcher.taskStatusMaker.forceFinish(status))
                }
                status.isReadyToRun() -> {
                    Assert.assertEquals(BuildStatus.FAILED, BuildStatusSwitcher.taskStatusMaker.forceFinish(status))
                }
                status.isRunning() -> {
                    Assert.assertEquals(BuildStatus.TERMINATE, BuildStatusSwitcher.taskStatusMaker.forceFinish(status))
                }
                status == BuildStatus.SKIP -> {
                    Assert.assertEquals(BuildStatus.SKIP, BuildStatusSwitcher.taskStatusMaker.forceFinish(status))
                }
                status.isSuccess() -> {
                    Assert.assertEquals(BuildStatus.SUCCEED, BuildStatusSwitcher.taskStatusMaker.forceFinish(status))
                }
                status.isCancel() -> {
                    Assert.assertEquals(status, BuildStatusSwitcher.taskStatusMaker.forceFinish(status))
                }
                status == BuildStatus.QUEUE_TIMEOUT -> {
                    Assert.assertEquals(BuildStatus.QUEUE_TIMEOUT, BuildStatusSwitcher.taskStatusMaker.finish(status))
                }
                status.isFailure() -> {
                    Assert.assertEquals(BuildStatus.FAILED, BuildStatusSwitcher.taskStatusMaker.forceFinish(status))
                }
                status.isFinish() -> {
                    Assert.assertTrue(
                        BuildStatusSwitcher.taskStatusMaker.statusSet().contains(
                            BuildStatusSwitcher.taskStatusMaker.forceFinish(status)
                        )
                    )
                }
                else -> {
                    Assert.assertEquals(BuildStatus.FAILED, BuildStatusSwitcher.taskStatusMaker.forceFinish(status))
                }
            }
        }
    }

    @Test
    fun readyToSkipWhen() {

        BuildStatus.values().forEach { currentBuildStatus ->
            if (currentBuildStatus.isFailure() || currentBuildStatus.isCancel()) {
                Assert.assertEquals(BuildStatus.UNEXEC, BuildStatusSwitcher.readyToSkipWhen(currentBuildStatus))
            } else {
                Assert.assertEquals(BuildStatus.SKIP, BuildStatusSwitcher.readyToSkipWhen(currentBuildStatus))
            }
        }
    }
}

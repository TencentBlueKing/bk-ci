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

class BuildStatusSwitcherTest {

    @Test
    fun fixPipelineFinish() {
        BuildStatus.values().forEach { status ->
            when {
                status == BuildStatus.UNKNOWN -> {
                    Assert.assertEquals(BuildStatus.SUCCEED, BuildStatusSwitcher.fixPipelineFinish(status))
                }
                status.isReadyToRun() -> {
                    Assert.assertEquals(BuildStatus.SUCCEED, BuildStatusSwitcher.fixPipelineFinish(status))
                }
                status.isRunning() -> {
                    Assert.assertEquals(BuildStatus.SUCCEED, BuildStatusSwitcher.fixPipelineFinish(status))
                }
                status == BuildStatus.SKIP -> {
                    Assert.assertEquals(BuildStatus.SUCCEED, BuildStatusSwitcher.fixPipelineFinish(status))
                }
                status.isSuccess() -> {
                    Assert.assertEquals(BuildStatus.SUCCEED, BuildStatusSwitcher.fixPipelineFinish(status))
                }
                status.isCancel() -> {
                    Assert.assertEquals(status, BuildStatusSwitcher.fixPipelineFinish(status))
                }
                status.isFailure() -> {
                    Assert.assertEquals(BuildStatus.FAILED, BuildStatusSwitcher.fixPipelineFinish(status))
                }
                status == BuildStatus.STAGE_SUCCESS -> {
                    Assert.assertEquals(BuildStatus.STAGE_SUCCESS, BuildStatusSwitcher.fixPipelineFinish(status))
                }
                else -> {
                    Assert.assertEquals(BuildStatus.SUCCEED, BuildStatusSwitcher.fixPipelineFinish(status))
                }
            }
        }
    }

    @Test
    fun cancel() {
        BuildStatus.values().forEach { currentBuildStatus ->
            when {
                currentBuildStatus == BuildStatus.UNKNOWN -> {
                    Assert.assertEquals(BuildStatus.CANCELED, BuildStatusSwitcher.cancel(currentBuildStatus))
                }
                currentBuildStatus.isReadyToRun() -> {
                    Assert.assertEquals(BuildStatus.CANCELED, BuildStatusSwitcher.cancel(currentBuildStatus))
                }
                currentBuildStatus.isRunning() -> {
                    Assert.assertEquals(BuildStatus.CANCELED, BuildStatusSwitcher.cancel(currentBuildStatus))
                }
                currentBuildStatus.isFinish() -> {
                    Assert.assertEquals(currentBuildStatus, BuildStatusSwitcher.cancel(currentBuildStatus))
                }
                else -> {
                    Assert.assertEquals(BuildStatus.CANCELED, BuildStatusSwitcher.cancel(currentBuildStatus))
                }
            }
        }
    }

    @Test
    fun finish() {

        BuildStatus.values().forEach { currentBuildStatus ->
            when {
                currentBuildStatus.isFinish() -> {
                    Assert.assertEquals(currentBuildStatus, BuildStatusSwitcher.finish(currentBuildStatus))
                }
                currentBuildStatus == BuildStatus.STAGE_SUCCESS -> {
                    Assert.assertEquals(currentBuildStatus, BuildStatusSwitcher.finish(currentBuildStatus))
                }
                else -> {
                    Assert.assertEquals(BuildStatus.SUCCEED, BuildStatusSwitcher.finish(currentBuildStatus))
                }
            }
        }
    }

    @Test
    fun forceFinish() {

        BuildStatus.values().forEach { currentBuildStatus ->
            when {
                currentBuildStatus.isFinish() -> {
                    Assert.assertEquals(currentBuildStatus, BuildStatusSwitcher.forceFinish(currentBuildStatus))
                }
                currentBuildStatus == BuildStatus.STAGE_SUCCESS -> {
                    Assert.assertEquals(currentBuildStatus, BuildStatusSwitcher.forceFinish(currentBuildStatus))
                }
                else -> {
                    Assert.assertEquals(BuildStatus.FAILED, BuildStatusSwitcher.forceFinish(currentBuildStatus))
                }
            }
        }
    }

    @Test
    fun readyToSkipWhen() {

        BuildStatus.values().forEach { currentBuildStatus ->
            if (currentBuildStatus.isFailure()) {
                Assert.assertEquals(BuildStatus.UNEXEC, BuildStatusSwitcher.readyToSkipWhen(true))
            } else {
                Assert.assertEquals(BuildStatus.SKIP, BuildStatusSwitcher.readyToSkipWhen(false))
            }
        }
    }
}

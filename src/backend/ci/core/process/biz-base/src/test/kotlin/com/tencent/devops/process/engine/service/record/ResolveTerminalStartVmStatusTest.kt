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

package com.tencent.devops.process.engine.service.record

import com.tencent.devops.common.pipeline.enums.BuildStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ResolveTerminalStartVmStatusTest {

    @Test
    fun runningSetupBecomesSucceedWhenContainerSucceeds() {
        assertEquals(
            BuildStatus.SUCCEED.name,
            resolveTerminalStartVmStatus(BuildStatus.SUCCEED, matrixGroup = false, BuildStatus.RUNNING.name)
        )
    }

    @Test
    fun missingSetupBecomesContainerFailure() {
        assertEquals(
            BuildStatus.CANCELED.name,
            resolveTerminalStartVmStatus(BuildStatus.CANCELED, matrixGroup = null, currentStartVmStatus = null)
        )
        assertEquals(
            BuildStatus.HEARTBEAT_TIMEOUT.name,
            resolveTerminalStartVmStatus(
                BuildStatus.HEARTBEAT_TIMEOUT,
                matrixGroup = false,
                BuildStatus.RUNNING.name
            )
        )
    }

    @Test
    fun finishedSetupIsKeptWhenContainerFailsLater() {
        assertNull(
            resolveTerminalStartVmStatus(BuildStatus.FAILED, matrixGroup = false, BuildStatus.SUCCEED.name)
        )
    }

    @Test
    fun matrixGroupAndUnfinishedContainerAreLeftAlone() {
        assertNull(
            resolveTerminalStartVmStatus(BuildStatus.SUCCEED, matrixGroup = true, BuildStatus.RUNNING.name)
        )
        assertNull(
            resolveTerminalStartVmStatus(BuildStatus.RUNNING, matrixGroup = false, BuildStatus.RUNNING.name)
        )
    }

    @Test
    fun canceledOrFailedJobMarksUnstartedTasks() {
        assertTrue(shouldMarkUnstartedTasksUnexec(BuildStatus.CANCELED, matrixGroup = false))
        assertTrue(shouldMarkUnstartedTasksUnexec(BuildStatus.FAILED, matrixGroup = null))
        assertTrue(shouldMarkUnstartedTasksUnexec(BuildStatus.TERMINATE, matrixGroup = false))
    }

    @Test
    fun successSkipRunningAndMatrixDoNotMarkUnstartedTasks() {
        assertFalse(shouldMarkUnstartedTasksUnexec(BuildStatus.SUCCEED, matrixGroup = false))
        assertFalse(shouldMarkUnstartedTasksUnexec(BuildStatus.SKIP, matrixGroup = false))
        assertFalse(shouldMarkUnstartedTasksUnexec(BuildStatus.RUNNING, matrixGroup = false))
        assertFalse(shouldMarkUnstartedTasksUnexec(BuildStatus.CANCELED, matrixGroup = true))
    }
}

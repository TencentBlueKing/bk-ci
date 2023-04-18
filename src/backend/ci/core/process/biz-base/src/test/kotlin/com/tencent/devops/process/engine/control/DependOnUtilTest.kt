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

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.enums.DependOnType
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.process.utils.DependOnUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class DependOnUtilTest {

    @Test
    fun `check normal depend on`() {
        val container1 = NormalContainer(
            id = "1",
            enableSkip = false,
            conditions = null,
            jobId = "job_bash1",
            jobControlOption = JobControlOption(
                runCondition = JobRunCondition.STAGE_RUNNING,
                enable = true
            )
        )
        val container2 = NormalContainer(
            id = "2",
            enableSkip = false,
            conditions = null,
            jobId = "job_bash2",
            jobControlOption = JobControlOption(
                runCondition = JobRunCondition.STAGE_RUNNING,
                enable = true
            )
        )
        val stage = Stage(
            id = "1",
            containers = listOf(container1, container2)
        )
        DependOnUtils.checkRepeatedJobId(stage)
    }

    @Test
    fun `check repeated job`() {
        val container1 = NormalContainer(
            id = "1",
            enableSkip = false,
            conditions = null,
            jobId = "job_bash1",
            jobControlOption = JobControlOption(
                runCondition = JobRunCondition.STAGE_RUNNING,
                enable = true
            )
        )
        val container2 = NormalContainer(
            id = "2",
            enableSkip = false,
            conditions = null,
            jobId = "job_bash1",
            jobControlOption = JobControlOption(
                runCondition = JobRunCondition.STAGE_RUNNING,
                enable = true
            )
        )
        val stage = Stage(
            id = "1",
            containers = listOf(container1, container2)
        )
        Assertions.assertThrows(ErrorCodeException::class.java) { DependOnUtils.checkRepeatedJobId(stage) }
    }

    @Test
    fun `check empty job`() {
        val container1 = NormalContainer(
            id = "1",
            enableSkip = false,
            conditions = null,
            jobControlOption = JobControlOption(
                runCondition = JobRunCondition.STAGE_RUNNING,
                enable = true
            )
        )
        val container2 = NormalContainer(
            id = "2",
            enableSkip = false,
            conditions = null,
            jobControlOption = JobControlOption(
                runCondition = JobRunCondition.STAGE_RUNNING,
                enable = true
            )
        )
        val stage = Stage(
            id = "1",
            containers = listOf(container1, container2)
        )
        DependOnUtils.checkRepeatedJobId(stage)
    }

    @Test
    fun `remove not exist job`() {
        val container1 = NormalContainer(
            id = "1",
            enableSkip = false,
            conditions = null,
            jobId = "job_bash1",
            jobControlOption = JobControlOption(
                runCondition = JobRunCondition.STAGE_RUNNING,
                enable = true
            )
        )
        val container2 = NormalContainer(
            id = "2",
            enableSkip = false,
            conditions = null,
            jobControlOption = JobControlOption(
                runCondition = JobRunCondition.STAGE_RUNNING,
                enable = true,
                dependOnType = DependOnType.ID,
                dependOnId = listOf("job_bash1", "job_bash2")
            )
        )
        val stage = Stage(
            id = "1",
            containers = listOf(container1, container2)
        )
        val expectedContainer2 = NormalContainer(
            id = "2",
            enableSkip = false,
            conditions = null,
            jobControlOption = JobControlOption(
                runCondition = JobRunCondition.STAGE_RUNNING,
                enable = true,
                dependOnType = DependOnType.ID,
                dependOnId = listOf("job_bash1")
            )
        )
        val expectedStage = Stage(
            id = "1",
            containers = listOf(container1, expectedContainer2)
        )
        DependOnUtils.checkRepeatedJobId(stage)
        Assertions.assertEquals(expectedStage, stage)
    }

    @Test
    fun `check init dependon for empty job`() {
        val container1 = NormalContainer(
            id = "1",
            enableSkip = false,
            conditions = null,
            jobControlOption = JobControlOption(
                runCondition = JobRunCondition.STAGE_RUNNING,
                enable = true
            )
        )
        val container2 = NormalContainer(
            id = "2",
            enableSkip = false,
            conditions = null,
            jobControlOption = JobControlOption(
                runCondition = JobRunCondition.STAGE_RUNNING,
                enable = true
            )
        )
        val stage = Stage(
            id = "1",
            containers = listOf(container1, container2)
        )
        val expectedStage = Stage(
            id = "1",
            containers = listOf(container1, container2)
        )
        val params = mapOf<String, String>()
        DependOnUtils.initDependOn(stage, params)
        Assertions.assertEquals(stage, expectedStage)
    }

    @Test
    fun `check init dependon for normal job`() {
        val container1 = NormalContainer(
            id = "1",
            enableSkip = false,
            conditions = null,
            jobId = "job1",
            jobControlOption = JobControlOption(
                runCondition = JobRunCondition.STAGE_RUNNING,
                enable = true
            )
        )
        val container2 = NormalContainer(
            id = "2",
            enableSkip = false,
            conditions = null,
            jobControlOption = JobControlOption(
                runCondition = JobRunCondition.STAGE_RUNNING,
                enable = true,
                dependOnType = DependOnType.ID,
                dependOnId = listOf("job1")
            )
        )
        val stage = Stage(
            id = "1",
            containers = listOf(container1, container2)
        )
        val expectedContainer2 = NormalContainer(
            id = "2",
            enableSkip = false,
            conditions = null,
            jobControlOption = JobControlOption(
                runCondition = JobRunCondition.STAGE_RUNNING,
                enable = true,
                dependOnType = DependOnType.ID,
                dependOnId = listOf("job1"),
                dependOnContainerId2JobIds = mapOf("1" to "job1")
            )
        )
        val expectedStage = Stage(
            id = "1",
            containers = listOf(container1, expectedContainer2)
        )
        val params = mapOf<String, String>()
        DependOnUtils.initDependOn(stage, params)
        Assertions.assertEquals(stage, expectedStage)
    }

    @Test
    fun `check init dependon for cycle dependon`() {
        val container1 = NormalContainer(
            id = "1",
            enableSkip = false,
            conditions = null,
            jobId = "job1",
            jobControlOption = JobControlOption(
                runCondition = JobRunCondition.STAGE_RUNNING,
                enable = true,
                dependOnType = DependOnType.ID,
                dependOnId = listOf("job2")
            )
        )
        val container2 = NormalContainer(
            id = "2",
            enableSkip = false,
            conditions = null,
            jobId = "job2",
            jobControlOption = JobControlOption(
                runCondition = JobRunCondition.STAGE_RUNNING,
                enable = true,
                dependOnType = DependOnType.ID,
                dependOnId = listOf("job1")
            )
        )
        val stage = Stage(
            id = "1",
            containers = listOf(container1, container2)
        )
        val params = mapOf<String, String>()
        Assertions.assertThrows(ErrorCodeException::class.java) { DependOnUtils.initDependOn(stage, params) }
    }

    @Test
    fun `check init dependon for not exit job dependon`() {
        val container1 = NormalContainer(
            id = "1",
            enableSkip = false,
            conditions = null,
            jobId = "job1",
            jobControlOption = JobControlOption(
                runCondition = JobRunCondition.STAGE_RUNNING,
                enable = true
            )
        )
        val container2 = NormalContainer(
            id = "2",
            enableSkip = false,
            conditions = null,
            jobId = "job2",
            jobControlOption = JobControlOption(
                runCondition = JobRunCondition.STAGE_RUNNING,
                enable = true,
                dependOnType = DependOnType.ID,
                dependOnId = listOf("job1", "job3")
            )
        )
        val stage = Stage(
            id = "1",
            containers = listOf(container1, container2)
        )
        val params = mapOf<String, String>()
        DependOnUtils.initDependOn(stage, params)

        val expectedContainer2 = NormalContainer(
            id = "2",
            enableSkip = false,
            conditions = null,
            jobId = "job2",
            jobControlOption = JobControlOption(
                runCondition = JobRunCondition.STAGE_RUNNING,
                enable = true,
                dependOnType = DependOnType.ID,
                dependOnId = listOf("job1", "job3"),
                dependOnContainerId2JobIds = mapOf("1" to "job1")
            )
        )
        val expectedStage = Stage(
            id = "1",
            containers = listOf(container1, expectedContainer2)
        )
        Assertions.assertEquals(expectedStage, stage)
    }

    @Test
    fun `check init dependOnName`() {
        val container1 = NormalContainer(
            id = "1",
            enableSkip = false,
            conditions = null,
            jobId = "job1",
            jobControlOption = JobControlOption(
                runCondition = JobRunCondition.STAGE_RUNNING,
                enable = true
            )
        )
        val container2 = NormalContainer(
            id = "2",
            enableSkip = false,
            conditions = null,
            jobControlOption = JobControlOption(
                runCondition = JobRunCondition.STAGE_RUNNING,
                enable = true,
                dependOnType = DependOnType.NAME,
                dependOnName = "job1"
            )
        )
        val stage = Stage(
            id = "1",
            containers = listOf(container1, container2)
        )
        val params = mapOf<String, String>()
        DependOnUtils.initDependOn(stage, params)

        val expectedContainer2 = NormalContainer(
            id = "2",
            enableSkip = false,
            conditions = null,
            jobControlOption = JobControlOption(
                runCondition = JobRunCondition.STAGE_RUNNING,
                enable = true,
                dependOnType = DependOnType.NAME,
                dependOnName = "job1",
                dependOnContainerId2JobIds = mapOf("1" to "job1")
            )
        )
        val expectedStage = Stage(
            id = "1",
            containers = listOf(container1, expectedContainer2)
        )
        Assertions.assertEquals(expectedStage, stage)
    }

    @Test
    fun `check init dependon for invalid dependOnName parse`() {
        val container1 = NormalContainer(
            id = "1",
            enableSkip = false,
            conditions = null,
            jobId = "job1",
            jobControlOption = JobControlOption(
                runCondition = JobRunCondition.STAGE_RUNNING,
                enable = true
            )
        )
        val container2 = NormalContainer(
            id = "2",
            enableSkip = false,
            conditions = null,
            jobControlOption = JobControlOption(
                runCondition = JobRunCondition.STAGE_RUNNING,
                enable = true,
                dependOnType = DependOnType.NAME,
                dependOnName = "\${job1}"
            )
        )
        val stage = Stage(
            id = "1",
            containers = listOf(container1, container2)
        )
        val params = mapOf<String, String>()
        DependOnUtils.initDependOn(stage, params)

        val expectedContainer2 = NormalContainer(
            id = "2",
            enableSkip = false,
            conditions = null,
            jobControlOption = JobControlOption(
                runCondition = JobRunCondition.STAGE_RUNNING,
                enable = true,
                dependOnType = DependOnType.NAME,
                dependOnName = "\${job1}"
            )
        )
        val expectedStage = Stage(
            id = "1",
            containers = listOf(container1, expectedContainer2)
        )
        Assertions.assertEquals(expectedStage, stage)
    }

    @Test
    fun `check init dependon for normal dependOnName parse`() {
        val container1 = NormalContainer(
            id = "1",
            enableSkip = false,
            conditions = null,
            jobId = "job1",
            jobControlOption = JobControlOption(
                runCondition = JobRunCondition.STAGE_RUNNING,
                enable = true
            )
        )
        val container2 = NormalContainer(
            id = "2",
            enableSkip = false,
            conditions = null,
            jobControlOption = JobControlOption(
                runCondition = JobRunCondition.STAGE_RUNNING,
                enable = true,
                dependOnType = DependOnType.NAME,
                dependOnName = "\${job1}"
            )
        )
        val stage = Stage(
            id = "1",
            containers = listOf(container1, container2)
        )
        val params = mapOf("job1" to "job1")
        DependOnUtils.initDependOn(stage, params)

        val expectedContainer2 = NormalContainer(
            id = "2",
            enableSkip = false,
            conditions = null,
            jobControlOption = JobControlOption(
                runCondition = JobRunCondition.STAGE_RUNNING,
                enable = true,
                dependOnType = DependOnType.NAME,
                dependOnName = "\${job1}",
                dependOnContainerId2JobIds = mapOf("1" to "job1")
            )
        )
        val expectedStage = Stage(
            id = "1",
            containers = listOf(container1, expectedContainer2)
        )
        Assertions.assertEquals(expectedStage, stage)
    }
}

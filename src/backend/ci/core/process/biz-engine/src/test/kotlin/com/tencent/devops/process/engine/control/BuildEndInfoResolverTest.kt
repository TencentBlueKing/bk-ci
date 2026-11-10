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

package com.tencent.devops.process.engine.control

import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorInfo
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.MutexGroup
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.enums.BuildEndType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.option.StageControlOption
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.pojo.PipelineBuildStage
import com.tencent.devops.process.engine.pojo.PipelineBuildStageControlOption
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * 覆盖没有任何插件错误信息时的终态归因：互斥组抢锁失败、FastKill 连带终止这两类 Job 级终止
 * 都不写 errorType，只能从 Model 中的容器状态反推。
 */
class BuildEndInfoResolverTest {

    private val resolver = BuildEndInfoResolver(mockk<Client>())

    @Test
    fun `given mutex job failed without queue then job level position is filled`() {
        val context = genContext(
            model = genModel(
                genContainer(containerId = "1", name = "job-A", status = BuildStatus.SUCCEED),
                genContainer(
                    containerId = "2",
                    name = "job-B",
                    status = BuildStatus.FAILED,
                    mutexGroup = MutexGroup(enable = true, mutexGroupName = "lock-123", queueEnable = false)
                )
            ),
            buildStatus = BuildStatus.FAILED
        )

        val endInfo = resolver.resolve(context)

        Assertions.assertNotNull(endInfo)
        Assertions.assertEquals(BuildEndType.FAIL_EXEC, endInfo!!.endType)
        val position = endInfo.positions!!.single()
        Assertions.assertEquals("1-2", position.position)
        Assertions.assertEquals("stage-1/job-B", position.componentPath)
        Assertions.assertEquals(BuildStatus.FAILED.name, position.statusAtEnd)
        Assertions.assertEquals(BuildEndType.FAIL_EXEC, position.endType)
        Assertions.assertEquals(
            ProcessMessageCode.BK_BUILD_END_FAIL_MUTEX_QUEUE_DISABLED,
            position.reasonCode
        )
        Assertions.assertEquals(listOf("lock-123"), position.reasonParams)
    }

    @Test
    fun `given mutex job failed with queue enabled then queue reason is used`() {
        val context = genContext(
            model = genModel(
                genContainer(
                    containerId = "1",
                    name = "job-A",
                    status = BuildStatus.FAILED,
                    mutexGroup = MutexGroup(enable = true, mutexGroupName = "lock-123", queueEnable = true)
                )
            ),
            buildStatus = BuildStatus.FAILED
        )

        val position = resolver.resolve(context)!!.positions!!.single()

        Assertions.assertEquals(ProcessMessageCode.BK_BUILD_END_FAIL_MUTEX_QUEUE, position.reasonCode)
        Assertions.assertEquals(listOf("lock-123"), position.reasonParams)
    }

    @Test
    fun `given no position can be resolved then end info is still returned`() {
        val context = genContext(
            model = genModel(genContainer(containerId = "1", name = "job-A", status = BuildStatus.SUCCEED)),
            buildStatus = BuildStatus.FAILED
        )

        val endInfo = resolver.resolve(context)

        Assertions.assertNotNull(endInfo)
        Assertions.assertEquals(BuildEndType.FAIL_EXEC, endInfo!!.endType)
        Assertions.assertEquals(0, endInfo.positionCount)
    }

    @Test
    fun `given fast kill terminated job then position carries cause job name`() {
        val context = genContext(
            model = genModel(
                genContainer(containerId = "1", name = "job-A", status = BuildStatus.FAILED),
                genContainer(containerId = "2", name = "job-B", status = BuildStatus.CANCELED)
            ),
            buildStatus = BuildStatus.FAILED,
            errorInfoList = listOf(genErrorInfo(containerId = "1", errorCode = ErrorCode.USER_SCRIPT_TASK_FAIL)),
            buildStages = listOf(genBuildStage(status = BuildStatus.FAILED, fastKill = true))
        )

        val endInfo = resolver.resolve(context)!!

        Assertions.assertEquals(BuildEndType.FAIL_MULTIPLE, endInfo.endType)
        val positions = endInfo.positions!!
        Assertions.assertEquals(2, positions.size)
        val fastKillPosition = positions.single { it.endType == BuildEndType.FAIL_FAST_KILL }
        Assertions.assertEquals("1-2", fastKillPosition.position)
        Assertions.assertEquals(BuildStatus.CANCELED.name, fastKillPosition.statusAtEnd)
        Assertions.assertEquals(ProcessMessageCode.BK_BUILD_END_FAIL_FAST_KILL, fastKillPosition.reasonCode)
        Assertions.assertEquals(listOf("job-A"), fastKillPosition.reasonParams)
    }

    @Test
    fun `given fast kill caused by stage level failure then position falls back to stage reason`() {
        val context = genContext(
            model = genModel(genContainer(containerId = "1", name = "job-A", status = BuildStatus.CANCELED)),
            buildStatus = BuildStatus.FAILED,
            // Stage 级质量红线失败的错误信息里没有容器，定位不到引发 FastKill 的 Job
            errorInfoList = listOf(
                genErrorInfo(containerId = "", errorCode = ErrorCode.USER_QUALITY_CHECK_FAIL)
            ),
            buildStages = listOf(genBuildStage(status = BuildStatus.FAILED, fastKill = true))
        )

        val endInfo = resolver.resolve(context)!!
        Assertions.assertEquals(BuildEndType.FAIL_MULTIPLE, endInfo.endType)
        val positions = endInfo.positions!!
        val fastKillPosition = positions.single { it.endType == BuildEndType.FAIL_FAST_KILL }

        Assertions.assertEquals(ProcessMessageCode.BK_BUILD_END_FAIL_FAST_KILL_STAGE, fastKillPosition.reasonCode)
        Assertions.assertNull(fastKillPosition.reasonParams)
    }

    @Test
    fun `given plugin fail and fast kill error then build type is multiple`() {
        val context = genContext(
            model = genModel(
                genContainer(containerId = "1", name = "job-A", status = BuildStatus.FAILED),
                genContainer(containerId = "2", name = "job-B", status = BuildStatus.FAILED)
            ),
            buildStatus = BuildStatus.FAILED,
            errorInfoList = listOf(
                genErrorInfo(containerId = "1", errorCode = ErrorCode.USER_SCRIPT_TASK_FAIL),
                genErrorInfo(containerId = "2", errorCode = ErrorCode.USER_STAGE_FASTKILL_TERMINATE)
            ),
            buildStages = listOf(genBuildStage(status = BuildStatus.FAILED, fastKill = true))
        )

        val endInfo = resolver.resolve(context)!!

        Assertions.assertEquals(BuildEndType.FAIL_MULTIPLE, endInfo.endType)
        val fastKillPosition = endInfo.positions!!.single { it.endType == BuildEndType.FAIL_FAST_KILL }
        Assertions.assertEquals("1-2", fastKillPosition.position)
        Assertions.assertEquals(ProcessMessageCode.BK_BUILD_END_FAIL_FAST_KILL, fastKillPosition.reasonCode)
        Assertions.assertEquals(listOf("job-A"), fastKillPosition.reasonParams)
        val execPosition = endInfo.positions!!.single { it.endType == BuildEndType.FAIL_EXEC }
        Assertions.assertEquals("1-1", execPosition.position)
        Assertions.assertNull(execPosition.reasonCode)
    }

    @Test
    fun `given failed job in fast kill stage without task error then it is fast kill`() {
        val context = genContext(
            model = genModel(
                genContainer(containerId = "1", name = "job-A", status = BuildStatus.FAILED),
                genContainer(containerId = "2", name = "job-B", status = BuildStatus.FAILED)
            ),
            buildStatus = BuildStatus.FAILED,
            errorInfoList = listOf(genErrorInfo(containerId = "1", errorCode = ErrorCode.USER_SCRIPT_TASK_FAIL)),
            buildStages = listOf(genBuildStage(status = BuildStatus.FAILED, fastKill = true))
        )

        val endInfo = resolver.resolve(context)!!

        Assertions.assertEquals(BuildEndType.FAIL_MULTIPLE, endInfo.endType)
        val fastKillPosition = endInfo.positions!!.single { it.endType == BuildEndType.FAIL_FAST_KILL }
        Assertions.assertEquals("1-2", fastKillPosition.position)
        Assertions.assertEquals(BuildStatus.FAILED.name, fastKillPosition.statusAtEnd)
        Assertions.assertEquals(ProcessMessageCode.BK_BUILD_END_FAIL_FAST_KILL, fastKillPosition.reasonCode)
        Assertions.assertEquals(listOf("job-A"), fastKillPosition.reasonParams)
    }

    @Test
    fun `given mutex job canceled without queue then job level position is filled`() {
        val context = genContext(
            model = genModel(
                genContainer(
                    containerId = "1",
                    name = "job-B",
                    status = BuildStatus.CANCELED,
                    mutexGroup = MutexGroup(enable = true, mutexGroupName = "lock-123", queueEnable = false)
                )
            ),
            buildStatus = BuildStatus.FAILED
        )

        val position = resolver.resolve(context)!!.positions!!.single()

        Assertions.assertEquals(BuildEndType.FAIL_EXEC, position.endType)
        Assertions.assertEquals(
            ProcessMessageCode.BK_BUILD_END_FAIL_MUTEX_QUEUE_DISABLED,
            position.reasonCode
        )
        Assertions.assertEquals(listOf("lock-123"), position.reasonParams)
    }

    @Test
    fun `given pause plugin failed without error then pause reason is filled`() {
        val context = genContext(
            model = genModel(
                genContainer(
                    containerId = "1",
                    name = "job-A",
                    status = BuildStatus.FAILED,
                    elements = listOf(
                        LinuxScriptElement(
                            id = "e-0806",
                            name = "0806插件",
                            status = BuildStatus.FAILED.name,
                            scriptType = BuildScriptType.SHELL,
                            script = "echo",
                            continueNoneZero = false,
                            additionalOptions = ElementAdditionalOptions(pauseBeforeExec = true)
                        )
                    )
                )
            ),
            buildStatus = BuildStatus.FAILED
        )

        val position = resolver.resolve(context)!!.positions!!.single()

        Assertions.assertEquals(BuildEndType.FAIL_EXEC, position.endType)
        Assertions.assertEquals(ProcessMessageCode.BK_BUILD_END_FAIL_PAUSE_TERMINATED, position.reasonCode)
    }

    @Test
    fun `given canceled job in stage without fast kill then no position is filled`() {
        val context = genContext(
            model = genModel(genContainer(containerId = "1", name = "job-A", status = BuildStatus.CANCELED)),
            buildStatus = BuildStatus.FAILED,
            buildStages = listOf(genBuildStage(status = BuildStatus.FAILED, fastKill = false))
        )

        Assertions.assertEquals(0, resolver.resolve(context)!!.positionCount)
    }

    @Test
    fun `given canceled build then end info is left to the cancel flow`() {
        val context = genContext(
            model = genModel(genContainer(containerId = "1", name = "job-A", status = BuildStatus.FAILED)),
            buildStatus = BuildStatus.CANCELED
        )

        Assertions.assertNull(resolver.resolve(context))
    }

    private fun genContext(
        model: Model,
        buildStatus: BuildStatus,
        errorInfoList: List<ErrorInfo>? = null,
        buildStages: List<PipelineBuildStage> = emptyList()
    ) = BuildEndContext(
        projectId = PROJECT_ID,
        pipelineId = PIPELINE_ID,
        buildId = BUILD_ID,
        buildStatus = buildStatus,
        model = model,
        errorInfoList = errorInfoList,
        buildTasks = emptyList(),
        buildStages = buildStages
    )

    /** 详情页不展示触发器阶段，因此 Model 的第一个阶段是占位的触发器阶段 */
    private fun genModel(vararg containers: NormalContainer) = Model(
        name = "test-pipeline",
        desc = null,
        stages = listOf(
            Stage(containers = emptyList(), id = "stage-0", name = "trigger"),
            Stage(containers = containers.toList(), id = STAGE_ID, name = STAGE_ID)
        )
    )

    private fun genContainer(
        containerId: String,
        name: String,
        status: BuildStatus,
        mutexGroup: MutexGroup? = null,
        elements: List<Element> = emptyList()
    ) = NormalContainer(
        id = containerId,
        containerId = containerId,
        containerHashId = "c-$containerId",
        name = name,
        status = status.name,
        jobId = "job_$containerId",
        mutexGroup = mutexGroup,
        elements = elements
    )

    private fun genErrorInfo(containerId: String, errorCode: Int) = ErrorInfo(
        stageId = STAGE_ID,
        containerId = containerId,
        taskId = "t-$containerId",
        taskName = "Bash",
        atomCode = "linuxScript",
        errorType = ErrorType.USER.num,
        errorCode = errorCode,
        errorMsg = "script exit with code 1"
    )

    private fun genBuildStage(status: BuildStatus, fastKill: Boolean) = PipelineBuildStage(
        projectId = PROJECT_ID,
        pipelineId = PIPELINE_ID,
        buildId = BUILD_ID,
        stageId = STAGE_ID,
        seq = 1,
        status = status,
        controlOption = PipelineBuildStageControlOption(
            stageControlOption = StageControlOption(),
            fastKill = fastKill
        )
    )

    companion object {
        private const val PROJECT_ID = "test-project"
        private const val PIPELINE_ID = "p-test"
        private const val BUILD_ID = "b-test"
        private const val STAGE_ID = "stage-1"
    }
}

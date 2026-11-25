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

package com.tencent.devops.process.engine.control.command.container.impl

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.dialect.PipelineDialectType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.option.MatrixControlOption
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.engine.atom.parser.DispatchTypeParser
import com.tencent.devops.process.engine.cfg.ModelContainerIdGenerator
import com.tencent.devops.process.engine.cfg.ModelTaskIdGenerator
import com.tencent.devops.process.engine.control.command.container.ContainerContext
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.PipelineBuildContainerControlOption
import com.tencent.devops.process.engine.pojo.event.PipelineBuildContainerEvent
import com.tencent.devops.process.engine.service.PipelineContainerService
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.engine.service.record.ContainerBuildRecordService
import com.tencent.devops.process.service.PipelineBuildTemplateAcrossInfoService
import com.tencent.devops.process.service.PipelineContextService
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.jooq.DSLContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * 矩阵最大并发数（[MatrixControlOption.maxConcurrencyVar]）运行时解析与校验的用例
 */
class InitializeMatrixGroupStageCmdTest {

    private lateinit var pipelineContextService: PipelineContextService
    private lateinit var cmd: InitializeMatrixGroupStageCmd

    @BeforeEach
    fun setUp() {
        // 失败文案依赖 Spring 上下文（取默认语言），单测环境下隔离掉，避免掩盖并发数校验本身的失败
        mockkObject(I18nUtil)
        every { I18nUtil.getLanguage(any()) } returns "zh_CN"
        every { I18nUtil.getCodeLanMessage(any(), any(), any(), any(), any(), any()) } returns "invalid message"
        pipelineContextService = mockk()
        // 默认不提供聚合上下文，由各用例按需覆盖
        every {
            pipelineContextService.buildContext(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns mutableMapOf()
        cmd = InitializeMatrixGroupStageCmd(
            dslContext = mockk<DSLContext>(),
            containerBuildRecordService = mockk<ContainerBuildRecordService>(),
            templateAcrossInfoService = mockk<PipelineBuildTemplateAcrossInfoService>(),
            pipelineContainerService = mockk<PipelineContainerService>(),
            pipelineTaskService = mockk<PipelineTaskService>(),
            pipelineContextService = pipelineContextService,
            modelContainerIdGenerator = mockk<ModelContainerIdGenerator>(),
            modelTaskIdGenerator = mockk<ModelTaskIdGenerator>(),
            dispatchTypeParser = mockk<DispatchTypeParser>(),
            buildLogPrinter = mockk<BuildLogPrinter>(),
            redisOperation = mockk<RedisOperation>()
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    @DisplayName("未配置并发数时取默认值")
    fun resolveDefault() {
        assertEquals(DEFAULT_CONCURRENCY, resolve(option = MatrixControlOption(strategyStr = STRATEGY)))
    }

    @Test
    @DisplayName("数字型并发数合法时直接生效")
    fun resolveValidNumber() {
        assertEquals(1, resolve(option = MatrixControlOption(strategyStr = STRATEGY, maxConcurrency = 1)))
        assertEquals(50, resolve(option = MatrixControlOption(strategyStr = STRATEGY, maxConcurrency = 50)))
    }

    @Test
    @DisplayName("数字型并发数非法时失败退出，不做静默截断")
    fun resolveInvalidNumber() {
        assertThrows(InvalidParamException::class.java) {
            resolve(option = MatrixControlOption(strategyStr = STRATEGY, maxConcurrency = 0))
        }
        assertThrows(InvalidParamException::class.java) {
            resolve(option = MatrixControlOption(strategyStr = STRATEGY, maxConcurrency = 51))
        }
    }

    @Test
    @DisplayName("经典方言：变量从构建变量中解析")
    fun resolveVarFromBuildVariables() {
        assertEquals(
            3,
            resolve(
                option = MatrixControlOption(strategyStr = STRATEGY, maxConcurrencyVar = "\${MATRIX_MAX}"),
                variables = mapOf("MATRIX_MAX" to "3"),
                dialectType = PipelineDialectType.CLASSIC
            )
        )
    }

    @Test
    @DisplayName("制约方言：job 输出变量从聚合上下文中解析")
    fun resolveVarFromJobOutputContext() {
        every {
            pipelineContextService.buildContext(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns mutableMapOf(JOB_OUTPUT_KEY to "2")
        assertEquals(
            2,
            resolve(
                option = MatrixControlOption(strategyStr = STRATEGY, maxConcurrencyVar = "\${{$JOB_OUTPUT_KEY}}"),
                variables = emptyMap(),
                dialectType = PipelineDialectType.CONSTRAINED
            )
        )
        // 确认聚合上下文确实参与解析（构建变量中并没有该输出变量的键）
        verify(exactly = 1) {
            pipelineContextService.buildContext(any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    @DisplayName("变量解析不出结果时失败退出")
    fun resolveUnresolvedVar() {
        assertThrows(InvalidParamException::class.java) {
            resolve(
                option = MatrixControlOption(strategyStr = STRATEGY, maxConcurrencyVar = "\${NOT_EXIST}"),
                dialectType = PipelineDialectType.CLASSIC
            )
        }
    }

    @Test
    @DisplayName("变量解析结果非法时失败退出")
    fun resolveInvalidVarValue() {
        assertThrows(InvalidParamException::class.java) {
            resolve(
                option = MatrixControlOption(strategyStr = STRATEGY, maxConcurrencyVar = "\${MATRIX_MAX}"),
                variables = mapOf("MATRIX_MAX" to "51"),
                dialectType = PipelineDialectType.CLASSIC
            )
        }
    }

    @Test
    @DisplayName("未配置变量时不读取聚合上下文，避免额外的构建记录开销")
    fun skipBuildContextWhenNoVar() {
        resolve(option = MatrixControlOption(strategyStr = STRATEGY, maxConcurrency = 5))
        verify(exactly = 0) {
            pipelineContextService.buildContext(any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    private fun resolve(
        option: MatrixControlOption,
        variables: Map<String, String> = emptyMap(),
        dialectType: PipelineDialectType = PipelineDialectType.CLASSIC
    ): Int {
        val container = PipelineBuildContainer(
            projectId = PROJECT_ID,
            pipelineId = PIPELINE_ID,
            buildId = BUILD_ID,
            stageId = STAGE_ID,
            containerId = CONTAINER_ID,
            containerHashId = null,
            containPostTaskFlag = false,
            jobId = null,
            matrixGroupFlag = true,
            matrixGroupId = null,
            containerType = "vmBuild",
            seq = 1,
            status = BuildStatus.RUNNING,
            controlOption = PipelineBuildContainerControlOption(
                jobControlOption = JobControlOption(),
                matrixControlOption = option
            )
        )
        val commandContext = ContainerContext(
            container = container,
            containerTasks = emptyList(),
            buildStatus = BuildStatus.RUNNING,
            event = PipelineBuildContainerEvent(
                source = "test",
                projectId = PROJECT_ID,
                pipelineId = PIPELINE_ID,
                userId = USER_ID,
                buildId = BUILD_ID,
                stageId = STAGE_ID,
                containerId = CONTAINER_ID,
                executeCount = 1,
                containerHashId = null,
                containerType = "vmBuild",
                actionType = ActionType.START
            ),
            latestSummary = "",
            variables = variables,
            watcher = Watcher(id = "test"),
            executeCount = 1
        )
        return cmd.resolveMaxConcurrency(
            option = option,
            jobName = JOB_NAME,
            commandContext = commandContext,
            dialect = dialectType.dialect
        )
    }

    companion object {
        private const val PROJECT_ID = "demo"
        private const val PIPELINE_ID = "p-12345678901234567890123456789012"
        private const val BUILD_ID = "b-12345678901234567890123456789012"
        private const val STAGE_ID = "stage-1"
        private const val CONTAINER_ID = "1"
        private const val USER_ID = "user0"
        private const val JOB_NAME = "matrix-job"
        private const val STRATEGY = "{}"
        private const val DEFAULT_CONCURRENCY = 5
        private const val JOB_OUTPUT_KEY = "jobs.job_gv.steps.s1.outputs.my_name"
    }
}

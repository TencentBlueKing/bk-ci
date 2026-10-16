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

package com.tencent.devops.process.service.pipeline

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.dialect.PipelineDialectType
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.option.MatrixControlOption
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_TEMPLATE_MATRIX_MAX_CONCURRENCY_INVALID
import com.tencent.devops.process.service.PipelineAsCodeService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class MatrixConcurrencyCheckServiceTest {

    private lateinit var pipelineAsCodeService: PipelineAsCodeService
    private lateinit var service: MatrixConcurrencyCheckService

    @BeforeEach
    fun setUp() {
        pipelineAsCodeService = mockk()
        every { pipelineAsCodeService.getProjectDialect(any()) } returns PipelineDialectType.CLASSIC.dialect
        service = MatrixConcurrencyCheckService(pipelineAsCodeService = pipelineAsCodeService)
    }

    @Test
    @DisplayName("变量解析为1-MAX之间的整数时实例化通过")
    fun checkValidVariable() {
        service.checkInstanceMatrixMaxConcurrency(
            projectId = PROJECT_ID,
            pipelineName = PIPELINE_NAME,
            model = buildModel(maxConcurrencyVar = "\${MATRIX_MAX}", paramValue = "10")
        )
        service.checkInstanceMatrixMaxConcurrency(
            projectId = PROJECT_ID,
            pipelineName = PIPELINE_NAME,
            model = buildModel(maxConcurrencyVar = "\${MATRIX_MAX}", paramValue = "1")
        )
    }

    @Test
    @DisplayName("变量解析结果超过MAX时实例化失败")
    fun checkVariableExceedMax() {
        assertInvalid(maxConcurrencyVar = "\${MATRIX_MAX}", paramValue = "51")
    }

    @Test
    @DisplayName("变量解析结果小于1时实例化失败")
    fun checkVariableBelowMin() {
        assertInvalid(maxConcurrencyVar = "\${MATRIX_MAX}", paramValue = "0")
    }

    @Test
    @DisplayName("变量解析结果不是整数时实例化失败")
    fun checkVariableNotNumber() {
        assertInvalid(maxConcurrencyVar = "\${MATRIX_MAX}", paramValue = "abc")
    }

    @Test
    @DisplayName("实例化阶段取不到值的变量交由运行时校验，不在此误判为非法")
    fun checkUnresolvedVariable() {
        service.checkInstanceMatrixMaxConcurrency(
            projectId = PROJECT_ID,
            pipelineName = PIPELINE_NAME,
            model = buildModel(maxConcurrencyVar = "\${MATRIX_MAX}", paramValue = "")
        )
        // 引用不在流水线参数中的变量（如内置变量）同样跳过
        service.checkInstanceMatrixMaxConcurrency(
            projectId = PROJECT_ID,
            pipelineName = PIPELINE_NAME,
            model = buildModel(maxConcurrencyVar = "\${BK_CI_BUILD_NUM}", paramValue = "10")
        )
    }

    @Test
    @DisplayName("数字型并发数非法时实例化失败")
    fun checkInvalidNumber() {
        assertInvalid(maxConcurrency = 0)
        assertInvalid(maxConcurrency = 51)
    }

    @Test
    @DisplayName("未配置并发数时取默认值，属于合法配置")
    fun checkNotConfigured() {
        service.checkInstanceMatrixMaxConcurrency(
            projectId = PROJECT_ID,
            pipelineName = PIPELINE_NAME,
            model = buildModel()
        )
    }

    private fun assertInvalid(maxConcurrency: Int? = null, maxConcurrencyVar: String? = null, paramValue: String = "") {
        val exception = assertThrows(ErrorCodeException::class.java) {
            service.checkInstanceMatrixMaxConcurrency(
                projectId = PROJECT_ID,
                pipelineName = PIPELINE_NAME,
                model = buildModel(
                    maxConcurrency = maxConcurrency,
                    maxConcurrencyVar = maxConcurrencyVar,
                    paramValue = paramValue
                )
            )
        }
        assertEquals(ERROR_TEMPLATE_MATRIX_MAX_CONCURRENCY_INVALID, exception.errorCode)
    }

    private fun buildModel(
        maxConcurrency: Int? = null,
        maxConcurrencyVar: String? = null,
        paramValue: String = ""
    ): Model {
        val triggerContainer = TriggerContainer(
            id = "0",
            name = "trigger",
            params = mutableListOf(
                BuildFormProperty(
                    id = "MATRIX_MAX",
                    name = "MATRIX_MAX",
                    required = false,
                    type = BuildFormPropertyType.STRING,
                    defaultValue = paramValue,
                    options = null,
                    desc = null,
                    repoHashId = null,
                    relativePath = null,
                    scmType = null,
                    containerType = null,
                    glob = null,
                    properties = null
                )
            )
        )
        val matrixJob = NormalContainer(
            id = "1",
            name = "matrix-job",
            matrixGroupFlag = true,
            matrixControlOption = MatrixControlOption(
                strategyStr = "{}",
                maxConcurrency = maxConcurrency,
                maxConcurrencyVar = maxConcurrencyVar
            )
        )
        return Model(
            name = PIPELINE_NAME,
            desc = null,
            stages = listOf(
                Stage(containers = listOf(triggerContainer), id = "stage-0"),
                Stage(containers = listOf(matrixJob), id = "stage-1")
            )
        )
    }

    companion object {
        private const val PROJECT_ID = "demo"
        private const val PIPELINE_NAME = "demo-pipeline"
    }
}

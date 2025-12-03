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

package com.tencent.devops.process.service.template.v2

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.PipelineStorageType
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.TemplateModelAndSetting
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.pojo.transfer.TransferActionType
import com.tencent.devops.common.pipeline.pojo.transfer.TransferBody
import com.tencent.devops.common.pipeline.pojo.transfer.TransferResponse
import com.tencent.devops.common.pipeline.pojo.transfer.YamlWithVersion
import com.tencent.devops.common.pipeline.template.PipelineTemplateType
import com.tencent.devops.process.engine.service.PipelineInfoExtService
import com.tencent.devops.process.service.pipeline.PipelineTransferYamlService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("PipelineTemplateGenerator.transfer 方法单元测试")
class PipelineTemplateGeneratorTest {

    private val client: Client = mockk()
    private val transferService: PipelineTransferYamlService = mockk()
    private val resourceService: PipelineTemplateResourceService = mockk()
    private val settingService: PipelineTemplateSettingService = mockk()
    private val pipelineInfoExtService: PipelineInfoExtService = mockk()

    private lateinit var generator: PipelineTemplateGenerator

    private val userId = "testUser"
    private val projectId = "testProject"

    @BeforeEach
    fun setUp() {
        generator = PipelineTemplateGenerator(
            client = client,
            transferService = transferService,
            pipelineTemplateResourceService = resourceService,
            pipelineTemplateSettingService = settingService,
            pipelineInfoExtService = pipelineInfoExtService
        )
    }

    // ========== YAML 转 Model 测试 ==========

    @Test
    @DisplayName("YAML 转 Model - Pipeline 模板转换成功")
    fun `should transfer YAML to Pipeline Model successfully`() {
        // Given
        val yaml = "version: v3.0\npipeline:\n  name: test"
        val expectedModel = createMockPipelineModel()
        val expectedSetting = createMockSetting()
        val expectedYaml = YamlWithVersion(yamlStr = yaml, versionTag = "v3.0")

        every {
            transferService.transfer(
                userId = userId,
                projectId = projectId,
                pipelineId = null,
                actionType = TransferActionType.TEMPLATE_YAML2MODEL_PIPELINE,
                data = TransferBody(oldYaml = yaml)
            )
        } returns TransferResponse(
            templateModelAndSetting = TemplateModelAndSetting(
                templateModel = expectedModel,
                setting = expectedSetting
            ),
            yamlWithVersion = expectedYaml
        )

        // When
        val result = generator.transfer(
            userId = userId,
            projectId = projectId,
            storageType = PipelineStorageType.YAML,
            templateType = PipelineTemplateType.PIPELINE,
            templateModel = null,
            templateSetting = null,
            params = null,
            yaml = yaml,
            fallbackOnError = false
        )

        // Then
        assertNotNull(result)
        assertEquals(PipelineTemplateType.PIPELINE, result.templateType)
        assertEquals(expectedModel, result.templateModel)
        assertEquals(expectedSetting, result.templateSetting)
        assertEquals(expectedYaml, result.yamlWithVersion)
        assertEquals(expectedModel.getTriggerContainer().params, result.params)

        verify(exactly = 1) {
            transferService.transfer(
                userId = userId,
                projectId = projectId,
                pipelineId = null,
                actionType = TransferActionType.TEMPLATE_YAML2MODEL_PIPELINE,
                data = TransferBody(oldYaml = yaml)
            )
        }
    }

    @Test
    @DisplayName("YAML 转 Model - yaml 为 null 时抛出异常")
    fun `should throw exception when YAML is null`() {
        // When & Then
        assertThrows<ParamBlankException> {
            generator.transfer(
                userId = userId,
                projectId = projectId,
                storageType = PipelineStorageType.YAML,
                templateType = PipelineTemplateType.PIPELINE,
                templateModel = null,
                templateSetting = null,
                params = null,
                yaml = null,
                fallbackOnError = false
            )
        }
    }

    // ========== Model 转 YAML 测试 ==========
    @Test
    @DisplayName("Model 转 YAML - Pipeline 模板转换成功")
    fun `should transfer Pipeline Model to YAML successfully`() {
        // Given
        val templateModel = createMockPipelineModel()
        val templateSetting = createMockSetting()
        val expectedYaml = YamlWithVersion(yamlStr = "version: v3.0", versionTag = "v3.0")

        every {
            transferService.transfer(
                userId = userId,
                projectId = projectId,
                pipelineId = null,
                actionType = TransferActionType.TEMPLATE_MODEL2YAML_PIPELINE,
                data = match { body ->
                    body.templateModelAndSetting?.templateModel == templateModel &&
                        body.templateModelAndSetting?.setting == templateSetting
                }
            )
        } returns TransferResponse(
            yamlWithVersion = expectedYaml
        )

        // When
        val result = generator.transfer(
            userId = userId,
            projectId = projectId,
            storageType = PipelineStorageType.MODEL,
            templateType = PipelineTemplateType.PIPELINE,
            templateModel = templateModel,
            templateSetting = templateSetting,
            params = null,
            yaml = null,
            fallbackOnError = false
        )

        // Then
        assertNotNull(result)
        assertEquals(PipelineTemplateType.PIPELINE, result.templateType)
        assertEquals(templateModel, result.templateModel)
        assertEquals(templateSetting, result.templateSetting)
        assertEquals(expectedYaml, result.yamlWithVersion)
        assertEquals(templateModel.getTriggerContainer().params, result.params)

        verify(exactly = 1) {
            transferService.transfer(
                userId = userId,
                projectId = projectId,
                pipelineId = null,
                actionType = TransferActionType.TEMPLATE_MODEL2YAML_PIPELINE,
                data = any()
            )
        }
    }

    @Test
    @DisplayName("Model 转 YAML - Pipeline 模板合并 templateParams 到 params")
    fun `should merge templateParams into params for Pipeline Model`() {
        // Given
        val templateModel = createMockPipelineModelWithTemplateParams()
        val templateSetting = createMockSetting()
        val expectedYaml = YamlWithVersion(yamlStr = "version: v3.0", versionTag = "v3.0")

        // 记录合并前的参数状态
        val originalParamsSize = templateModel.getTriggerContainer().params.size
        val originalTemplateParamsSize = templateModel.getTriggerContainer().templateParams?.size ?: 0
        val templateParamId = templateModel.getTriggerContainer().templateParams?.firstOrNull()?.id

        every {
            transferService.transfer(
                userId = userId,
                projectId = projectId,
                pipelineId = null,
                actionType = TransferActionType.TEMPLATE_MODEL2YAML_PIPELINE,
                data = any()
            )
        } returns TransferResponse(
            yamlWithVersion = expectedYaml
        )

        // When
        val result = generator.transfer(
            userId = userId,
            projectId = projectId,
            storageType = PipelineStorageType.MODEL,
            templateType = PipelineTemplateType.PIPELINE,
            templateModel = templateModel,
            templateSetting = templateSetting,
            params = null,
            yaml = null,
            fallbackOnError = false
        )

        // Then
        assertNotNull(result)
        
        // 1. 验证 templateParams 已被置空
        assertNull(templateModel.getTriggerContainer().templateParams)
        
        // 2. 验证参数数量正确（原来1个param + 1个templateParam = 2个）
        val expectedSize = originalParamsSize + originalTemplateParamsSize
        assertEquals(expectedSize, result.params.size)
        assertEquals(expectedSize, templateModel.getTriggerContainer().params.size)
        
        // 3. 验证 templateParam 已合并到 params 中
        val mergedParam = result.params.find { it.id == templateParamId }
        assertNotNull(mergedParam, "templateParam 应该被合并到 params 中")
        
        // 4. 验证合并后的 templateParam 被标记为 constant = true
        assertEquals(true, mergedParam?.constant, "合并后的 templateParam 应该被标记为 constant = true")
        
        // 5. 验证原有的 param 仍然存在
        val originalParam = result.params.find { it.id == "param1" }
        assertNotNull(originalParam, "原有的 param 应该仍然存在")
    }

    @Test
    @DisplayName("Model 转 YAML - Pipeline 模板参数去重（params 覆盖同名 templateParams）")
    fun `should merge templateParams and deduplicate params with same id`() {
        // Given
        val templateModel = createMockPipelineModelWithDuplicateParams()
        val templateSetting = createMockSetting()
        val expectedYaml = YamlWithVersion(yamlStr = "version: v3.0", versionTag = "v3.0")

        every {
            transferService.transfer(
                userId = userId,
                projectId = projectId,
                pipelineId = null,
                actionType = TransferActionType.TEMPLATE_MODEL2YAML_PIPELINE,
                data = any()
            )
        } returns TransferResponse(
            yamlWithVersion = expectedYaml
        )

        // When
        val result = generator.transfer(
            userId = userId,
            projectId = projectId,
            storageType = PipelineStorageType.MODEL,
            templateType = PipelineTemplateType.PIPELINE,
            templateModel = templateModel,
            templateSetting = templateSetting,
            params = null,
            yaml = null,
            fallbackOnError = false
        )

        // Then
        assertNotNull(result)
        
        // 1. 验证 templateParams 已被置空
        assertNull(templateModel.getTriggerContainer().templateParams)
        
        // 2. 验证参数去重：原来有 2 个 params (param1, duplicateParam) 
        //    + 2 个 templateParams (duplicateParam, templateParam1)
        //    去重后应该有 3 个 (param1, duplicateParam 保留 params 的值, templateParam1)
        assertEquals(3, result.params.size)
        
        // 3. 验证同名参数保留 params 的值（mergeProperties 中 to 参数优先）
        val duplicateParam = result.params.find { it.id == "duplicateParam" }
        assertNotNull(duplicateParam)
        assertEquals("paramsValue", duplicateParam?.defaultValue, 
            "同名参数应该保留 params 中的值（params 优先级高于 templateParams）")
        
        // 4. 验证来自 templateParams 的独有参数被正确添加并标记为 constant
        val templateParam = result.params.find { it.id == "templateParam1" }
        assertNotNull(templateParam, "templateParams 中的独有参数应该被添加")
        assertEquals(true, templateParam?.constant, 
            "来自 templateParams 的参数应该被标记为 constant = true")
    }

    @Test
    @DisplayName("Model 转 YAML - templateType 为 null 时抛出异常")
    fun `should throw exception when templateType is null for Model transfer`() {
        // Given
        val templateModel = createMockPipelineModel()
        val templateSetting = createMockSetting()

        // When & Then
        assertThrows<ParamBlankException> {
            generator.transfer(
                userId = userId,
                projectId = projectId,
                storageType = PipelineStorageType.MODEL,
                templateType = null,
                templateModel = templateModel,
                templateSetting = templateSetting,
                params = null,
                yaml = null,
                fallbackOnError = false
            )
        }
    }

    @Test
    @DisplayName("Model 转 YAML - templateModel 为 null 时抛出异常")
    fun `should throw exception when templateModel is null for Model transfer`() {
        // Given
        val templateSetting = createMockSetting()

        // When & Then
        assertThrows<ParamBlankException> {
            generator.transfer(
                userId = userId,
                projectId = projectId,
                storageType = PipelineStorageType.MODEL,
                templateType = PipelineTemplateType.PIPELINE,
                templateModel = null,
                templateSetting = templateSetting,
                params = null,
                yaml = null,
                fallbackOnError = false
            )
        }
    }

    @Test
    @DisplayName("Model 转 YAML - templateSetting 为 null 时抛出异常")
    fun `should throw exception when templateSetting is null for Model transfer`() {
        // Given
        val templateModel = createMockPipelineModel()

        // When & Then
        assertThrows<ParamBlankException> {
            generator.transfer(
                userId = userId,
                projectId = projectId,
                storageType = PipelineStorageType.MODEL,
                templateType = PipelineTemplateType.PIPELINE,
                templateModel = templateModel,
                templateSetting = null,
                params = null,
                yaml = null,
                fallbackOnError = false
            )
        }
    }

    // ========== 异常兜底测试 ==========

    @Test
    @DisplayName("Model 转 YAML - 转换失败且未启用兜底，抛出异常")
    fun `should throw exception when Model to YAML fails and fallback disabled`() {
        // Given
        val templateModel = createMockPipelineModel()
        val templateSetting = createMockSetting()
        val exception = RuntimeException("Transfer failed")

        every {
            transferService.transfer(
                userId = userId,
                projectId = projectId,
                pipelineId = null,
                actionType = TransferActionType.TEMPLATE_MODEL2YAML_PIPELINE,
                data = any()
            )
        } throws exception

        // When & Then
        assertThrows<RuntimeException> {
            generator.transfer(
                userId = userId,
                projectId = projectId,
                storageType = PipelineStorageType.MODEL,
                templateType = PipelineTemplateType.PIPELINE,
                templateModel = templateModel,
                templateSetting = templateSetting,
                params = null,
                yaml = null,
                fallbackOnError = false
            )
        }
    }

    @Test
    @DisplayName("Model 转 YAML - 转换失败启用兜底，返回兜底数据")
    fun `should return fallback result when Model to YAML fails and fallback enabled`() {
        // Given
        val templateModel = createMockPipelineModelWithTemplateParams()
        val templateSetting = createMockSetting()
        val exception = RuntimeException("Transfer failed")
        
        // 记录 templateParam 的 ID 以便后续验证
        val templateParamId = templateModel.getTriggerContainer().templateParams?.firstOrNull()?.id

        every {
            transferService.transfer(
                userId = userId,
                projectId = projectId,
                pipelineId = null,
                actionType = TransferActionType.TEMPLATE_MODEL2YAML_PIPELINE,
                data = any()
            )
        } throws exception

        // When
        val result = generator.transfer(
            userId = userId,
            projectId = projectId,
            storageType = PipelineStorageType.MODEL,
            templateType = PipelineTemplateType.PIPELINE,
            templateModel = templateModel,
            templateSetting = templateSetting,
            params = null,
            yaml = null,
            fallbackOnError = true
        )

        // Then
        assertNotNull(result)
        assertEquals(PipelineTemplateType.PIPELINE, result.templateType)
        assertEquals(templateModel, result.templateModel)
        assertEquals(templateSetting, result.templateSetting)
        assertNull(result.yamlWithVersion) // 转换失败，返回 null
        
        // 验证即使转换失败，参数仍被合并
        assertNull(templateModel.getTriggerContainer().templateParams, "templateParams 应该被置空")
        assertEquals(2, result.params.size, "应该有2个参数（1个原始 + 1个合并的模板参数）")
        
        // 验证 templateParam 确实被合并到 params 中
        val mergedTemplateParam = result.params.find { it.id == templateParamId }
        assertNotNull(mergedTemplateParam, "templateParam 应该被合并到 params 中")
        assertEquals(true, mergedTemplateParam?.constant, "合并的 templateParam 应该被标记为 constant = true")
    }

    // ========== 辅助方法 ==========

    private fun createMockPipelineModel(): Model {
        val triggerContainer = TriggerContainer(
            id = "1",
            name = "trigger",
            elements = emptyList(),
            params = mutableListOf(
                BuildFormProperty(
                    id = "param1",
                    required = true,
                    type = BuildFormPropertyType.STRING,
                    defaultValue = "value1",
                    options = null,
                    desc = "Param 1",
                    repoHashId = null,
                    relativePath = null,
                    scmType = null,
                    containerType = null,
                    glob = null,
                    properties = null
                )
            )
        )

        return Model(
            name = "Test Pipeline",
            desc = "Test Description",
            stages = listOf(Stage(id = "stage-1", containers = listOf(triggerContainer))),
            labels = emptyList(),
            instanceFromTemplate = false,
            pipelineCreator = userId,
            srcTemplateId = null
        )
    }

    private fun createMockPipelineModelWithTemplateParams(): Model {
        val triggerContainer = TriggerContainer(
            id = "1",
            name = "trigger",
            elements = emptyList(),
            params = mutableListOf(
                BuildFormProperty(
                    id = "param1",
                    required = true,
                    type = BuildFormPropertyType.STRING,
                    defaultValue = "value1",
                    options = null,
                    desc = "Param 1",
                    repoHashId = null,
                    relativePath = null,
                    scmType = null,
                    containerType = null,
                    glob = null,
                    properties = null
                )
            ),
            templateParams = mutableListOf(
                BuildFormProperty(
                    id = "templateParam1",
                    required = true,
                    type = BuildFormPropertyType.STRING,
                    defaultValue = "tplValue1",
                    options = null,
                    desc = "Template Param 1",
                    repoHashId = null,
                    relativePath = null,
                    scmType = null,
                    containerType = null,
                    glob = null,
                    properties = null
                )
            )
        )

        return Model(
            name = "Test Pipeline",
            desc = "Test Description",
            stages = listOf(Stage(id = "stage-1", containers = listOf(triggerContainer))),
            labels = emptyList(),
            instanceFromTemplate = false,
            pipelineCreator = userId,
            srcTemplateId = null
        )
    }

    /**
     * 创建带重复参数的 Pipeline Model
     * params 和 templateParams 中都包含 id="duplicateParam" 的参数，用于测试去重逻辑
     */
    private fun createMockPipelineModelWithDuplicateParams(): Model {
        val triggerContainer = TriggerContainer(
            id = "1",
            name = "trigger",
            elements = emptyList(),
            params = mutableListOf(
                BuildFormProperty(
                    id = "param1",
                    required = true,
                    type = BuildFormPropertyType.STRING,
                    defaultValue = "value1",
                    options = null,
                    desc = "Param 1",
                    repoHashId = null,
                    relativePath = null,
                    scmType = null,
                    containerType = null,
                    glob = null,
                    properties = null
                ),
                BuildFormProperty(
                    id = "duplicateParam",
                    required = false,
                    type = BuildFormPropertyType.STRING,
                    defaultValue = "paramsValue",
                    options = null,
                    desc = "Duplicate in params",
                    repoHashId = null,
                    relativePath = null,
                    scmType = null,
                    containerType = null,
                    glob = null,
                    properties = null
                )
            ),
            templateParams = mutableListOf(
                BuildFormProperty(
                    id = "duplicateParam",
                    required = true,
                    type = BuildFormPropertyType.STRING,
                    defaultValue = "templateValue",
                    options = null,
                    desc = "Duplicate in templateParams",
                    repoHashId = null,
                    relativePath = null,
                    scmType = null,
                    containerType = null,
                    glob = null,
                    properties = null
                ),
                BuildFormProperty(
                    id = "templateParam1",
                    required = true,
                    type = BuildFormPropertyType.STRING,
                    defaultValue = "tplValue1",
                    options = null,
                    desc = "Template Param 1",
                    repoHashId = null,
                    relativePath = null,
                    scmType = null,
                    containerType = null,
                    glob = null,
                    properties = null
                )
            )
        )

        return Model(
            name = "Test Pipeline",
            desc = "Test Description",
            stages = listOf(Stage(id = "stage-1", containers = listOf(triggerContainer))),
            labels = emptyList(),
            instanceFromTemplate = false,
            pipelineCreator = userId,
            srcTemplateId = null
        )
    }

    private fun createMockSetting(): PipelineSetting {
        return PipelineSetting(
            projectId = projectId,
            pipelineId = "templateId",
            pipelineName = "Test Template",
            desc = "Test Description",
            pipelineAsCodeSettings = null,
            creator = userId,
            updater = userId
        )
    }
}

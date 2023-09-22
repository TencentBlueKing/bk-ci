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

package com.tencent.devops.process.yaml.modelTransfer

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.CommonPipelineAutoConfiguration
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.PipelineModelAndSetting
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.test.BkCiAbstractTest
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.pojo.classify.PipelineGroup
import com.tencent.devops.process.pojo.classify.PipelineLabel
import com.tencent.devops.process.pojo.transfer.ElementInsertBody
import com.tencent.devops.process.yaml.modelTransfer.inner.TransferCreator
import com.tencent.devops.process.yaml.modelTransfer.inner.TransferCreatorImpl
import com.tencent.devops.process.yaml.modelTransfer.pojo.ModelTransferInput
import com.tencent.devops.process.yaml.modelTransfer.pojo.YamlTransferInput
import com.tencent.devops.process.yaml.pojo.TemplatePath
import com.tencent.devops.process.yaml.pojo.YamlVersion
import com.tencent.devops.process.yaml.v3.models.IPreTemplateScriptBuildYaml
import com.tencent.devops.process.yaml.v3.models.ITemplateFilter
import com.tencent.devops.process.yaml.v3.models.PreScriptBuildYaml
import com.tencent.devops.process.yaml.v3.parsers.template.YamlTemplate
import com.tencent.devops.process.yaml.v3.parsers.template.YamlTemplateConf
import com.tencent.devops.process.yaml.v3.parsers.template.models.GetTemplateParam
import com.tencent.devops.repository.pojo.CodeGitRepository
import io.mockk.every
import io.mockk.mockk
import java.io.BufferedReader
import java.io.InputStreamReader
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.util.ReflectionTestUtils

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [SpringContextUtil::class, CommonConfig::class, CommonPipelineAutoConfiguration::class])
internal class ModelTransferTest : BkCiAbstractTest() {
    private val client: Client = mockk()
    private val creator: TransferCreator = TransferCreatorImpl()
    private val transferCache: TransferCacheService = mockk()
    private val triggerTransfer: TriggerTransfer = TriggerTransfer(
        client = client, creator = creator, transferCache = transferCache
    )
    private val dispatchTransfer: DispatchTransfer = TXDispatchTransfer(
        client = client, objectMapper = objectMapper, inner = creator, transferCache = transferCache
    )
    private val modelContainer: ContainerTransfer = ContainerTransfer(
        client = client,
        objectMapper = objectMapper,
        transferCache = transferCache,
        dispatchTransfer = dispatchTransfer
    )
    private val yamlIndexService: YamlIndexService = YamlIndexService(dispatchTransfer)
    private val variableTransfer: VariableTransfer = VariableTransfer()

    private val elementTransfer: ElementTransfer = ElementTransfer(
        client = client, creator = creator, transferCache = transferCache, triggerTransfer = triggerTransfer,
        yamlIndexService = yamlIndexService
    )
    private val stageTransfer: StageTransfer = StageTransfer(
        client = client,
        objectMapper = objectMapper,
        containerTransfer = modelContainer,
        elementTransfer = elementTransfer,
        variableTransfer = variableTransfer
    )
    private val modelTransfer: ModelTransfer = ModelTransfer(
        client = client,
        modelStage = stageTransfer,
        elementTransfer = elementTransfer,
        variableTransfer = variableTransfer,
        transferCache = transferCache
    )


    private val pipelineInfo = PipelineInfo(
        projectId = "",
        pipelineId = "",
        templateId = "",
        pipelineName = "",
        pipelineDesc = "",
        version = 1,
        createTime = 1,
        updateTime = 1,
        creator = "",
        lastModifyUser = "",
        channelCode = ChannelCode.BS,
        canManualStartup = true,
        canElementSkip = true,
        taskCount = 1,
        versionName = "",
        id = 1,
        viewNames = emptyList()
    )

    @BeforeEach
    fun setUp() {
        every {
            transferCache.getAtomDefaultValue(any())
        }.returns(emptyMap())

        every {
            transferCache.getStoreImageInfo(any(), any())
        }.returns(null)

        every {
            transferCache.getProjectGroupAndUsers(any())
        }.returns(
            listOf(
                BkAuthGroupAndUserList(
                    displayName = "管理员",
                    roleId = 1,
                    roleName = "manager",
                    userIdList = emptyList(),
                    type = ""
                ),
                BkAuthGroupAndUserList(
                    displayName = "开发人员",
                    roleId = 2,
                    roleName = "developer",
                    userIdList = emptyList(),
                    type = ""
                )
            )
        )

        every {
            transferCache.getPipelineLabel(any(), any())
        }.returns(
            listOf(
                PipelineGroup(
                    id = "qweqwe",
                    projectId = "a",
                    name = "a",
                    createTime = 1,
                    updateTime = 1,
                    createUser = "superD",
                    updateUser = "superD",
                    labels = listOf(
                        PipelineLabel(
                            id = "a1",
                            groupId = "aa",
                            name = "标签A",
                            createTime = 1,
                            uptimeTime = 1,
                            createUser = "b",
                            updateUser = "b"
                        ),
                        PipelineLabel(
                            id = "a2",
                            groupId = "aa",
                            name = "标签B",
                            createTime = 1,
                            uptimeTime = 1,
                            createUser = "b",
                            updateUser = "b"
                        )
                    )
                )
            )
        )

        every {
            transferCache.getGitRepository(any(), any(), any())
        }.returns(
            CodeGitRepository(
                aliasName = "aliasName/xxx",
                url = "https://git.code.oa.com/XXX/XXX.git",
                credentialId = "credentialId",
                projectName = "projectName",
                userName = "userName",
                projectId = "projectId",
                repoHashId = "repoHashId",
                gitProjectId = 0
            )
        )

        every {
            transferCache.getPipelineRemoteToken(any(), any(), any())
        }.returns(
            "PipelineRemoteToken"
        )
        ReflectionTestUtils.setField(creator, "marketRunTaskData", true)
        ReflectionTestUtils.setField(creator, "runPlugInAtomCodeData", "run")
        ReflectionTestUtils.setField(creator, "runPlugInVersionData", "1.*")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "model-yaml-001"
        ]
    )
    fun model2Yaml(value: String) {
        val file = testReadResourceFile("transfer/$value/model.json")
        val yamlV2 = testReadResourceFile("transfer/$value/yamlV2.yaml")
        val yamlV3 = testReadResourceFile("transfer/$value/yamlV3.yaml")
        val modelAndSetting = JsonUtil.to(file, object : TypeReference<PipelineModelAndSetting>() {})

        val watcher = Watcher(id = "yaml and model transfer watcher")
        watcher.start("step_1|FULL_MODEL2YAML V3 start")
        val yml = modelTransfer.model2yaml(
            ModelTransferInput(
                "test",
                modelAndSetting.model,
                modelAndSetting.setting,
                YamlVersion.Version.V3_0
            )
        )
        val newYaml = TransferMapper.toYaml(yml)
        Assertions.assertEquals(newYaml, TransferMapper.toYaml(TransferMapper.to(yamlV3)))
        watcher.start("step_2|FULL_MODEL2YAML V2 start")
        val ymlV2 = modelTransfer.model2yaml(
            ModelTransferInput(
                "test",
                modelAndSetting.model,
                modelAndSetting.setting,
                YamlVersion.Version.V2_0
            )
        )
        val newYamlV2 = TransferMapper.toYaml(ymlV2)
        Assertions.assertEquals(newYamlV2, TransferMapper.toYaml(TransferMapper.to(yamlV2)))
        watcher.stop()
        println(watcher.toString())
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "yaml-model-001-v3",
            "yaml-model-001-v2"
        ]
    )
    fun yaml2model(value: String) {
        val modelFile = testReadResourceFile("transfer/$value/model.json")
        val yaml = testReadResourceFile("transfer/$value/yaml.yaml")

        val watcher = Watcher(id = "yaml and model transfer watcher")
        watcher.start("parse PreScriptBuildYaml")
        val pYml = YamlUtil.getObjectMapper().readValue(yaml, object : TypeReference<IPreTemplateScriptBuildYaml>() {})
        watcher.start("normalize Yaml")
        pYml.replaceTemplate { templateFilter ->
            YamlTemplate(
                yamlObject = templateFilter,
                filePath = TemplatePath("TEMPLATE_ROOT_FILE"),
                extraParameters = this,
                getTemplateMethod = ::getTemplate,
                nowRepo = null,
                repo = null,
                resourcePoolMapExt = null,
                conf = YamlTemplateConf(
                    useOldParametersExpression = false // todo
                )
            ).replace()
        }
        watcher.start("yaml2Model")
        val input = YamlTransferInput(
            "testUser",
            "testProject",
            pipelineInfo,
            pYml
        )
        val model = modelTransfer.yaml2Model(input)
        val setting = modelTransfer.yaml2Setting(input)
        watcher.start("last")
        val newModel = JsonUtil.toJson(PipelineModelAndSetting(model, setting))
        Assertions.assertEquals(newModel, JsonUtil.toJson(JsonUtil.to(modelFile)))
        watcher.stop()
        println(watcher.toString())
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "yaml-model-001-v3"
        ]
    )
    fun markerYaml(value: String) {
        val yaml = testReadResourceFile("transfer/$value/yaml.yaml")
        val index = TransferMapper.indexYaml(yaml, 198, 30)!!
        val pYml = YamlUtil.getObjectMapper().readValue(yaml, object : TypeReference<ITemplateFilter>() {})
        val res = yamlIndexService.checkYamlIndex(pYml, index)
        println(res)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "yaml-model-001-v3"
        ]
    )
    fun yamlElementInsert(value: String) {
        val yaml = testReadResourceFile("transfer/$value/yaml.yaml")
        val res = elementTransfer.modelTaskInsert(
            "test", "p-test", 365, 25, ElementInsertBody(
                yaml, MarketBuildAtomElement("yamlElementInsert test")
            )
        )
        println(res.mark)
        Assertions.assertEquals(yaml, res.yaml)
    }

    @Test
    fun yaml2Model() {
        val watcher = Watcher(id = "yaml2Model")
        watcher.start("read file")
        val yml = testReadResourceFile("temp.yml")
        watcher.start("parse PreScriptBuildYaml")
        val pYml = YamlUtil.getObjectMapper().readValue(yml, object : TypeReference<IPreTemplateScriptBuildYaml>() {})
        watcher.start("normalize Yaml")
        pYml.replaceTemplate {
            YamlUtil.getObjectMapper().readValue(yml, object : TypeReference<PreScriptBuildYaml>() {})
        }
        watcher.start("yaml2Model")
        val input = YamlTransferInput(
            "a",
            "a",
            pipelineInfo,
            pYml
        )
        val model = modelTransfer.yaml2Model(input)
        val setting = modelTransfer.yaml2Setting(input)
        watcher.start("model2yaml")
        val ymls = modelTransfer.model2yaml(ModelTransferInput("test", model, setting, YamlVersion.Version.V2_0))
        watcher.start("step_2|mergeYaml")
        val newYaml = TransferMapper.toYaml(ymls)
        watcher.stop()
        println(watcher)
        Assertions.assertEquals(yml, newYaml)
//        println(JsonUtil.toJson(model))
    }

    @Test
    fun yaml2ModelV3() {
        val watcher = Watcher(id = "yaml2Model")
        watcher.start("read file")
        val yml = testReadResourceFile("tempV3.yml")
        watcher.start("parse PreScriptBuildYaml")
        val pYml = YamlUtil.getObjectMapper().readValue(yml, object : TypeReference<IPreTemplateScriptBuildYaml>() {})
        watcher.start("normalize Yaml")
        pYml.replaceTemplate { templateFilter ->
            YamlTemplate(
                yamlObject = templateFilter,
                filePath = TemplatePath("TEMPLATE_ROOT_FILE"),
                extraParameters = this,
                getTemplateMethod = ::getTemplate,
                nowRepo = null,
                repo = null,
                resourcePoolMapExt = null,
                conf = YamlTemplateConf(
                    useOldParametersExpression = false // todo
                )
            ).replace()
        }
        watcher.start("yaml2Model")
        val input = YamlTransferInput(
            "a",
            "a",
            pipelineInfo,
            pYml
        )
        val model = modelTransfer.yaml2Model(input)
        val setting = modelTransfer.yaml2Setting(input)
        watcher.start("model2yaml")
        val ymls = modelTransfer.model2yaml(ModelTransferInput("test", model, setting, YamlVersion.Version.V3_0))
        watcher.start("step_2|mergeYaml")
        val newYaml = TransferMapper.toYaml(ymls)
        watcher.stop()
        println(watcher)
        Assertions.assertEquals(yml, newYaml)
//        println(JsonUtil.toJson(model))
    }

    fun getTemplate(param: GetTemplateParam<Any>): String {
        return ""
    }

    private fun testReadResourceFile(resourceName: String): String {
        val inputStream = javaClass.classLoader.getResourceAsStream(resourceName) ?: return ""
        val reader = BufferedReader(InputStreamReader(inputStream))
        return reader.readText().replace("\r\n", "\n")
    }
}

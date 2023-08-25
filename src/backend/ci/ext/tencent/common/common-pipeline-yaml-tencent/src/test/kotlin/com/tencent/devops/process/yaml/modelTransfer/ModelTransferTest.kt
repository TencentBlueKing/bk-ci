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
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.test.BkCiAbstractTest
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.pojo.classify.PipelineGroup
import com.tencent.devops.process.pojo.classify.PipelineLabel
import com.tencent.devops.process.yaml.modelTransfer.inner.TransferCreator
import com.tencent.devops.process.yaml.modelTransfer.inner.TransferCreatorImpl
import com.tencent.devops.process.yaml.modelTransfer.pojo.ModelTransferInput
import com.tencent.devops.process.yaml.modelTransfer.pojo.YamlTransferInput
import com.tencent.devops.process.yaml.pojo.TemplatePath
import com.tencent.devops.process.yaml.pojo.YamlVersion
import com.tencent.devops.process.yaml.v2.models.IPreTemplateScriptBuildYaml
import com.tencent.devops.process.yaml.v2.models.PreScriptBuildYaml
import com.tencent.devops.process.yaml.v2.parsers.template.YamlTemplate
import com.tencent.devops.process.yaml.v2.parsers.template.YamlTemplateConf
import com.tencent.devops.process.yaml.v2.parsers.template.models.GetTemplateParam
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.util.ReflectionTestUtils
import java.io.BufferedReader
import java.io.InputStreamReader

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [SpringContextUtil::class, CommonConfig::class])
internal class ModelTransferTest : BkCiAbstractTest() {
    private val client: Client = mockk()
    private val creator: TransferCreator = TransferCreatorImpl()
    private val transferCache: TransferCacheService = mockk()
    private val triggerTransfer: TriggerTransfer = TriggerTransfer(client, creator, transferCache)
    private val dispatchTransfer: DispatchTransfer = TXDispatchTransfer(client, objectMapper, creator, transferCache)
    private val modelContainer: ContainerTransfer = ContainerTransfer(
        client, objectMapper, transferCache, dispatchTransfer
    )

    private val elementTransfer: ElementTransfer = ElementTransfer(client, creator, transferCache, triggerTransfer)
    private val stageTransfer: StageTransfer = StageTransfer(
        client, objectMapper, modelContainer, elementTransfer
    )
    private val modelTransfer: ModelTransfer = ModelTransfer(client, stageTransfer, elementTransfer, transferCache)
    private val pipelineInfo = PipelineInfo(
        projectId = "", pipelineId = "", templateId = "", pipelineName = "",
        pipelineDesc = "", version = 1, createTime = 1, updateTime = 1, creator = "", lastModifyUser = "",
        channelCode = ChannelCode.BS, canManualStartup = true, canElementSkip = true,
        taskCount = 1, versionName = "", id = 1, viewNames = emptyList()
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
            transferCache.getPipelineLabel(any())
        }.returns(
            listOf(
                PipelineGroup(
                    id = "qweqwe", projectId = "a", name = "a", createTime = 1, updateTime = 1,
                    createUser = "superD", updateUser = "superD", labels = listOf(
                        PipelineLabel(
                            id = "a1", groupId = "aa", name = "标签A", createTime = 1, uptimeTime = 1,
                            createUser = "b", updateUser = "b"
                        ),
                        PipelineLabel(
                            id = "a2", groupId = "aa", name = "标签B", createTime = 1, uptimeTime = 1,
                            createUser = "b", updateUser = "b"
                        )
                    )
                )
            )
        )

        ReflectionTestUtils.setField(creator, "marketRunTaskData", true)
        ReflectionTestUtils.setField(creator, "runPlugInAtomCodeData", "run")
        ReflectionTestUtils.setField(creator, "runPlugInVersionData", "1.*")
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
            "a", "a", pipelineInfo, pYml
        )
        val model = modelTransfer.yaml2Model(input)
        val setting = modelTransfer.yaml2Setting(input)
        watcher.start("model2yaml")
        val ymls = modelTransfer.model2yaml(ModelTransferInput(model, setting, YamlVersion.Version.V2_0))
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
            "a", "a", pipelineInfo, pYml
        )
        val model = modelTransfer.yaml2Model(input)
        val setting = modelTransfer.yaml2Setting(input)
        watcher.start("model2yaml")
        val ymls = modelTransfer.model2yaml(ModelTransferInput(model, setting, YamlVersion.Version.V3_0))
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
        return reader.readText()
    }
}

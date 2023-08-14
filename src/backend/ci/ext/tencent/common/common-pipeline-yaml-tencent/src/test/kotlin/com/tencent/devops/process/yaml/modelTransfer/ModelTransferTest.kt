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
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.test.BkCiAbstractTest
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.yaml.modelCreate.ModelContainer
import com.tencent.devops.process.yaml.modelCreate.TXModelContainer
import com.tencent.devops.process.yaml.modelCreate.inner.TXInnerModelCreatorImpl
import com.tencent.devops.process.yaml.modelTransfer.inner.TransferModelCreator
import com.tencent.devops.process.yaml.modelTransfer.inner.TransferModelCreatorImpl
import com.tencent.devops.process.yaml.modelTransfer.pojo.ModelTransferInput
import com.tencent.devops.process.yaml.modelTransfer.pojo.YamlTransferInput
import com.tencent.devops.process.yaml.pojo.YamlVersion
import com.tencent.devops.process.yaml.v2.models.IPreTemplateScriptBuildYaml
import com.tencent.devops.process.yaml.v2.models.PreScriptBuildYaml
import io.mockk.every
import io.mockk.mockk
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
    private val inner: TXInnerModelCreatorImpl = TXInnerModelCreatorImpl()
    private val creator: TransferModelCreator = TransferModelCreatorImpl()
    private val transferCache: TransferCacheService = mockk()
    private val triggerTransfer: TriggerTransfer = TriggerTransfer(client, creator, transferCache)
    private val modelContainer: ModelContainer = TXModelContainer(client, objectMapper, inner, transferCache)

    private val elementTransfer: ElementTransfer = ElementTransfer(client, creator, transferCache, triggerTransfer)
    private val stageTransfer: StageTransfer = StageTransfer(
        client, objectMapper, modelContainer, elementTransfer
    )
    private val modelTransfer: ModelTransfer = ModelTransfer(client, stageTransfer, elementTransfer)
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
        val model = modelTransfer.yaml2Model(
            YamlTransferInput(
                "a", "a", pipelineInfo, pYml
            )
        )
        val setting = PipelineSetting()
        watcher.start("model2yaml")
        val ymls = modelTransfer.model2yaml(ModelTransferInput(model, setting, YamlVersion.Version.V2_0))
        watcher.stop()
        println(watcher)
        println(TransferMapper.toYaml(ymls))
//        println(JsonUtil.toJson(model))
    }

    private fun testReadResourceFile(resourceName: String): String? {
        val inputStream = javaClass.classLoader.getResourceAsStream(resourceName) ?: return null
        val reader = BufferedReader(InputStreamReader(inputStream))
        return reader.readText()
    }
}

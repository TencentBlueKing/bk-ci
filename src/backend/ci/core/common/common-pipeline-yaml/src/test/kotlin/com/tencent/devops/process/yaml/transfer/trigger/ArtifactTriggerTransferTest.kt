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

package com.tencent.devops.process.yaml.transfer.trigger

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.trigger.ArtifactTriggerElement
import com.tencent.devops.process.yaml.transfer.aspect.PipelineTransferAspectWrapper
import com.tencent.devops.process.yaml.v3.models.PreTemplateScriptBuildYamlV3Parser
import com.tencent.devops.process.yaml.v3.models.TriggerType
import com.tencent.devops.process.yaml.v3.models.on.ArrivedRule
import com.tencent.devops.process.yaml.v3.models.on.TriggerOn
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

/**
 * 制品触发器「YAML 资源文件驱动」的转换验证测试。
 *
 * 覆盖统一「触发器 -> 事件类型」结构的两种 YAML 形态：
 * - 单触发器嵌套形态：[trigger/artifact.yml]（`on.artifact.arrived`）
 * - 多触发器列表形态：[trigger/artifact-list.yml]（`on[].type = artifact`）
 *
 * 每个用例都验证完整往返：YAML -> TriggerOn -> Element（yaml2Model）-> TriggerOn（model2Yaml）。
 */
class ArtifactTriggerTransferTest {

    private val converter = ArtifactTriggerConverter()

    private val aspectWrapper = mockk<PipelineTransferAspectWrapper>(relaxed = true)

    private fun loadYaml(resource: String): String =
        ClassPathResource(resource).inputStream.bufferedReader().use { it.readText() }

    private fun parseTriggers(resource: String): List<Pair<TriggerType, TriggerOn>> {
        val preYaml = YamlUtil.getObjectMapper().readValue(
            loadYaml(resource),
            PreTemplateScriptBuildYamlV3Parser::class.java
        )
        preYaml.replaceTemplate { (it as PreTemplateScriptBuildYamlV3Parser).initPreScriptBuildYamlI() }
        return preYaml.formatTriggerOn(ScmType.CODE_GIT)
    }

    private fun arrivedOf(triggerOn: TriggerOn): ArrivedRule =
        JsonUtil.anyTo(triggerOn.events?.get("arrived"), object : TypeReference<ArrivedRule>() {})

    /** YAML -> Element -> YAML 往返后读取 arrived 规则，验证 model2Yaml 与解析一致 */
    private fun roundTripArrived(triggerOn: TriggerOn): ArrivedRule {
        val elements = mutableListOf<Element>()
        converter.yaml2Elements(triggerOn, elements)
        val back = converter.elements2Yaml(elements, aspectWrapper).single()
        return back.events?.get("arrived") as ArrivedRule
    }

    @Test
    fun `nested artifact yaml resource converts correctly (format 1)`() {
        val triggers = parseTriggers("trigger/artifact.yml")

        val artifact = triggers.single { it.first == TriggerType.ARTIFACT }.second
        val arrived = arrivedOf(artifact)
        assertEquals("制品到达触发", arrived.name)
        assertEquals("pipeline", arrived.repository)
        assertEquals("file", arrived.kind)
        assertEquals("p-xxxxx", arrived.watchPipeline)
        assertEquals(listOf("*.msi", "setup-*.exe"), arrived.artifactsName)
        assertEquals(listOf("*_unsigned.exe"), arrived.artifactsNameIgnore)
        assertEquals("quality-gate", arrived.metadata?.single()?.key)
        assertEquals("eq", arrived.metadata?.single()?.operator)
        assertEquals("passed", arrived.metadata?.single()?.value)

        // yaml -> Element 校验
        val elements = mutableListOf<Element>()
        converter.yaml2Elements(artifact, elements)
        val element = elements.single() as ArtifactTriggerElement
        assertEquals("制品到达触发", element.name)
        assertEquals("pipeline", element.data.input.repository.value)
        assertEquals("*.msi,setup-*.exe", element.data.input.artifactsName)

        // Element -> yaml 往返校验
        val back = roundTripArrived(artifact)
        assertEquals("pipeline", back.repository)
        assertEquals(listOf("*.msi", "setup-*.exe"), back.artifactsName)
        assertEquals("quality-gate", back.metadata?.single()?.key)
    }

    @Test
    fun `list artifact yaml resource converts correctly (format 2)`() {
        val triggers = parseTriggers("trigger/artifact-list.yml")

        // 基础触发器（manual）单独成条目
        assertEquals(1, triggers.count { it.first == TriggerType.BASE })
        val artifacts = triggers.filter { it.first == TriggerType.ARTIFACT }.map { arrivedOf(it.second) }
        assertEquals(2, artifacts.size)

        val msi = artifacts.single { it.repository == "pipeline" }
        assertEquals("MSI 归档触发", msi.name)
        assertEquals(listOf("*.msi"), msi.artifactsName)

        val image = artifacts.single { it.repository == "image" }
        assertEquals("镜像到达触发", image.name)
        assertEquals("bk-ci/backend", image.image)
        assertEquals(listOf("v*"), image.tags)

        // 逐个往返校验
        triggers.filter { it.first == TriggerType.ARTIFACT }.forEach { (_, triggerOn) ->
            val origin = arrivedOf(triggerOn)
            val back = roundTripArrived(triggerOn)
            assertEquals(origin.repository, back.repository)
            assertEquals(origin.name, back.name)
        }
    }
}

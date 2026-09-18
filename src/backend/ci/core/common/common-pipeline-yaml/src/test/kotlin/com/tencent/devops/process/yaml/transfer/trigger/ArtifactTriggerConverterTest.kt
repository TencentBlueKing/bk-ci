package com.tencent.devops.process.yaml.transfer.trigger

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.trigger.ArtifactTriggerElement
import com.tencent.devops.process.yaml.transfer.aspect.PipelineTransferAspectWrapper
import com.tencent.devops.process.yaml.v3.models.PreTemplateScriptBuildYamlV3Parser
import com.tencent.devops.process.yaml.v3.models.TriggerType
import com.tencent.devops.process.yaml.pojo.YamlVersion
import com.tencent.devops.process.yaml.v3.models.on.ArrivedMetadata
import com.tencent.devops.process.yaml.v3.models.on.ArrivedRule
import com.tencent.devops.process.yaml.v3.models.on.PreTriggerOnV3
import com.tencent.devops.process.yaml.v3.models.on.TriggerOn
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArtifactTriggerConverterTest {

    private val converter = ArtifactTriggerConverter()

    private val aspectWrapper = mockk<PipelineTransferAspectWrapper>(relaxed = true)

    private fun arrivedEvents(rule: ArrivedRule) = mapOf<String, Any?>("arrived" to rule)

    private fun parseArrived(triggerOn: TriggerOn): ArrivedRule =
        JsonUtil.anyTo(triggerOn.events?.get("arrived"), object : TypeReference<ArrivedRule>() {})

    @Test
    fun `yaml arrived rule converts to artifact trigger element`() {
        val elements = mutableListOf<Element>()
        converter.yaml2Elements(
            triggerOn = TriggerOn(
                events = arrivedEvents(
                    ArrivedRule(
                        name = "制品到达触发",
                        repository = "pipeline",
                        watchPipeline = "p-xxxxx",
                        artifactsName = listOf("*.msi", "setup-*.exe"),
                        artifactsNameIgnore = listOf("*_unsigned.exe"),
                        metadata = listOf(
                            ArrivedMetadata(key = "quality-gate", operator = "eq", value = "passed")
                        )
                    )
                )
            ),
            elements = elements
        )

        val element = elements.single() as ArtifactTriggerElement
        val input = element.data.input
        assertEquals("制品到达触发", element.name)
        assertEquals("pipeline", input.repository.value)
        assertEquals("p-xxxxx", input.watchPipeline)
        assertEquals("*.msi,setup-*.exe", input.artifactsName)
        assertEquals("*_unsigned.exe", input.artifactsNameIgnore)
        assertEquals("quality-gate", input.metadata?.single()?.key)
    }

    @Test
    fun `artifact trigger element converts back to arrived rule (round trip)`() {
        val elements = mutableListOf<Element>()
        val origin = ArrivedRule(
            name = "MSI 归档触发",
            repository = "pipeline",
            watchPipeline = "p-xxxxx",
            artifactsName = listOf("*.msi", "setup-*.exe"),
            artifactsNameIgnore = listOf("*_unsigned.exe"),
            metadata = listOf(
                ArrivedMetadata(key = "quality-gate", operator = "eq", value = "passed")
            )
        )
        converter.yaml2Elements(TriggerOn(events = arrivedEvents(origin)), elements)

        val result = converter.elements2Yaml(elements, aspectWrapper).single().events?.get("arrived") as? ArrivedRule
        assertNotNull(result)
        assertEquals(origin.name, result!!.name)
        assertEquals(origin.repository, result.repository)
        assertEquals(origin.watchPipeline, result.watchPipeline)
        assertEquals(origin.artifactsName, result.artifactsName)
        assertEquals(origin.artifactsNameIgnore, result.artifactsNameIgnore)
        assertEquals(origin.metadata?.single()?.key, result.metadata?.single()?.key)
        assertEquals(origin.metadata?.single()?.operator, result.metadata?.single()?.operator)
        assertEquals(origin.metadata?.single()?.value, result.metadata?.single()?.value)
    }

    @Test
    fun `nested on artifact object parses to ARTIFACT trigger (format 1)`() {
        val parser = PreTemplateScriptBuildYamlV3Parser(
            version = "v3.0",
            triggerOn = mapOf(
                "artifact" to mapOf(
                    "arrived" to mapOf(
                        "repository" to "pipeline",
                        "artifacts-name" to listOf("*.msi")
                    )
                )
            )
        )
        parser.replaceTemplate { (it as PreTemplateScriptBuildYamlV3Parser).initPreScriptBuildYamlI() }

        val triggers = parser.formatTriggerOn(ScmType.CODE_GIT)
        val arrived = parseArrived(triggers.single { it.first == TriggerType.ARTIFACT }.second)
        assertEquals("pipeline", arrived.repository)
        assertEquals(listOf("*.msi"), arrived.artifactsName)
    }

    @Test
    fun `list on with type artifact parses to ARTIFACT trigger (format 2)`() {
        val parser = PreTemplateScriptBuildYamlV3Parser(
            version = "v3.0",
            triggerOn = listOf(
                mapOf("manual" to true),
                mapOf(
                    "type" to "artifact",
                    "arrived" to mapOf(
                        "repository" to "image",
                        "image" to "bk-ci/backend",
                        "tags" to listOf("v*")
                    )
                )
            )
        )
        parser.replaceTemplate { (it as PreTemplateScriptBuildYamlV3Parser).initPreScriptBuildYamlI() }

        val triggers = parser.formatTriggerOn(ScmType.CODE_GIT)
        val arrived = parseArrived(triggers.single { it.first == TriggerType.ARTIFACT }.second)
        assertEquals("image", arrived.repository)
        assertEquals("bk-ci/backend", arrived.image)
        assertEquals(listOf("v*"), arrived.tags)
    }

    @Test
    fun `object on with base and nested artifact parses to BASE plus ARTIFACT (fused object form)`() {
        // 融合对象形态：基础触发器(manual)字段平铺到 on 顶层，artifact 作为同级 nested key
        val parser = PreTemplateScriptBuildYamlV3Parser(
            version = "v3.0",
            triggerOn = mapOf(
                "manual" to true,
                "artifact" to mapOf(
                    "arrived" to mapOf(
                        "repository" to "pipeline",
                        "artifacts-name" to listOf("*.msi")
                    )
                )
            )
        )
        parser.replaceTemplate { (it as PreTemplateScriptBuildYamlV3Parser).initPreScriptBuildYamlI() }

        val triggers = parser.formatTriggerOn(ScmType.CODE_GIT)
        // 基础触发器与 artifact 触发器各自成条目
        assertNotNull(triggers.singleOrNull { it.first == TriggerType.BASE })
        val arrived = parseArrived(triggers.single { it.first == TriggerType.ARTIFACT }.second)
        assertEquals("pipeline", arrived.repository)
        assertEquals(listOf("*.msi"), arrived.artifactsName)
    }

    @Test
    fun `events serializes flattened to top level (not nested under an events key)`() {
        // events 进构造函数后，@JsonAnyGetter 仍应把其内容平铺到顶层，且不额外冒出 events 字段
        val pre = PreTriggerOnV3(type = "artifact").also {
            it.events["arrived"] = mapOf("repository" to "pipeline")
        }
        val map = JsonUtil.toMutableMap(pre)
        assertTrue(map.containsKey("arrived"), "事件应平铺到顶层")
        assertFalse(map.containsKey("events"), "不应出现字面量 events 字段")
        @Suppress("UNCHECKED_CAST")
        assertEquals("pipeline", (map["arrived"] as Map<String, Any?>)["repository"])
    }

    @Test
    fun `events survives toPre round trip (constructor field)`() {
        // TriggerOn.events -> PreTriggerOnV3.events 映射后仍在
        val pre = TriggerOn(events = mapOf("arrived" to mapOf("repository" to "pipeline")))
            .toPre(YamlVersion.V3_0) as PreTriggerOnV3
        assertTrue(pre.events.containsKey("arrived"))
    }
}

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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

/**
 * 触发器「YAML 资源文件驱动」的转换验证测试（统一入口）。
 *
 * 覆盖各类触发器及其组合，同时覆盖「列表形态」与「对象（简写）形态」两种 YAML 写法，
 * 资源文件位于 `test/resources/trigger/` 目录：
 * - 基础触发器（manual / schedules / remote）：[trigger/base-list.yml]（列表）、[trigger/base-object.yml]（对象简写）
 * - 代码库触发器（push / tag / mr）：[trigger/git-list.yml]（列表）、[trigger/git-object.yml]（对象简写）
 * - TAPD 触发器（story / bug）：[trigger/tapd.yml]
 * - 制品触发器（统一「触发器 -> 事件类型」结构）
 *   - 单触发器嵌套形态：[trigger/artifact.yml]（`on.artifact.arrived`）
 *   - 多触发器列表形态：[trigger/artifact-list.yml]（`on[].type = artifact`）
 * - 组合：base + git（[trigger/combo-base-git.yml]）、base + artifact（[trigger/combo-base-artifact.yml]）、
 *   base + git + tapd + artifact（[trigger/combo-multi.yml]）
 */
class TriggerTransferTest {

    private val converter = ArtifactTriggerConverter()

    private val aspectWrapper = mockk<PipelineTransferAspectWrapper>(relaxed = true)

    private fun parseTriggers(resource: String): List<Pair<TriggerType, TriggerOn>> {
        val yaml = ClassPathResource(resource).inputStream.bufferedReader().use { it.readText() }
        val preYaml = YamlUtil.getObjectMapper().readValue(
            yaml,
            PreTemplateScriptBuildYamlV3Parser::class.java
        )
        preYaml.replaceTemplate { (it as PreTemplateScriptBuildYamlV3Parser).initPreScriptBuildYamlI() }
        return preYaml.formatTriggerOn(ScmType.CODE_GIT)
    }

    private fun arrivedOf(triggerOn: TriggerOn): ArrivedRule =
        JsonUtil.anyTo(triggerOn.events?.get("arrived"), object : TypeReference<ArrivedRule>() {})

    /** 制品触发器 YAML -> Element -> YAML 往返后读取 arrived 规则，验证 model2Yaml 与解析一致 */
    private fun roundTripArrived(triggerOn: TriggerOn): ArrivedRule {
        val elements = mutableListOf<Element>()
        converter.yaml2Elements(triggerOn, elements)
        val back = converter.elements2Yaml(elements, aspectWrapper).single()
        return back.events?.get("arrived") as ArrivedRule
    }

    // ---------------- 单触发器 ----------------

    @Test
    fun `base trigger yaml converts correctly (list form)`() {
        val triggers = parseTriggers("trigger/base-list.yml")
        val base = triggers.single { it.first == TriggerType.BASE }.second
        assertEquals("enabled", base.remote?.enable)
        assertNotNull(base.schedules)
        assertEquals(1, base.schedules?.size)
        assertEquals("0 0 1 * *", base.schedules?.single()?.cron)
        assertNotNull(base.manual)
    }

    @Test
    fun `base trigger yaml converts correctly (object shorthand form)`() {
        // 对象简写形态：makeRunsOn 会拆出 BASE(基础) 与一个默认代码库触发器
        val triggers = parseTriggers("trigger/base-object.yml")
        val base = triggers.single { it.first == TriggerType.BASE }.second
        assertEquals("enabled", base.remote?.enable)
        assertEquals("0 0 1 * *", base.schedules?.single()?.cron)
        assertNotNull(base.manual)
        // 简写形态会同时产出一个默认（git）代码库触发条目
        assertTrue(triggers.any { it.first == TriggerType.CODE_GIT })
    }

    @Test
    fun `git trigger yaml converts correctly (list form)`() {
        val triggers = parseTriggers("trigger/git-list.yml")
        val git = triggers.single { it.first == TriggerType.CODE_GIT }.second
        assertEquals("aliasName/xxx", git.repoName)
        assertEquals(listOf("master", "dev"), git.push?.branches)
        assertEquals(listOf("src/**"), git.push?.paths)
        assertEquals(listOf("v*"), git.tag?.tags)
        assertEquals(listOf("master"), git.mr?.targetBranches)
        assertEquals(listOf("open", "reopen"), git.mr?.action)
    }

    @Test
    fun `git trigger yaml converts correctly (object shorthand form)`() {
        // 对象简写形态：BASE 承载 manual，代码库触发条目承载 repo-name/push/tag/mr
        val triggers = parseTriggers("trigger/git-object.yml")

        val base = triggers.single { it.first == TriggerType.BASE }.second
        assertEquals(false, base.manual?.enable)

        val git = triggers.single { it.first == TriggerType.CODE_GIT }.second
        assertEquals("aliasName/xxx", git.repoName)
        assertEquals(listOf("master", "dev"), git.push?.branches)
        assertEquals(listOf("src/**"), git.push?.paths)
        assertEquals(listOf("v*"), git.tag?.tags)
        assertEquals(listOf("master"), git.mr?.targetBranches)
        assertEquals(listOf("open", "reopen"), git.mr?.action)
    }

    @Test
    fun `tapd trigger yaml converts correctly`() {
        val triggers = parseTriggers("trigger/tapd.yml")
        val tapd = triggers.single { it.first == TriggerType.TAPD }.second
        assertEquals("12345", tapd.workspaceId)
        assertEquals("trigger_story", tapd.story?.id)
        assertEquals(listOf("create", "update"), tapd.story?.action)
        assertEquals(listOf("u1"), tapd.story?.users)
        assertEquals(listOf("o1"), tapd.story?.owners)
        assertEquals(listOf("label1", "label2"), tapd.story?.labels)
        assertEquals(listOf("high"), tapd.story?.priorities)
        assertEquals("trigger_bug", tapd.bug?.id)
        assertEquals(listOf("create"), tapd.bug?.action)
    }

    @Test
    fun `nested artifact yaml converts correctly (format 1)`() {
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
    fun `list artifact yaml converts correctly (format 2)`() {
        val triggers = parseTriggers("trigger/artifact-list.yml")
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

        triggers.filter { it.first == TriggerType.ARTIFACT }.forEach { (_, triggerOn) ->
            val origin = arrivedOf(triggerOn)
            val back = roundTripArrived(triggerOn)
            assertEquals(origin.repository, back.repository)
            assertEquals(origin.name, back.name)
        }
    }

    // ---------------- 组合触发器 ----------------

    @Test
    fun `combo base and git yaml converts correctly`() {
        val triggers = parseTriggers("trigger/combo-base-git.yml")

        val base = triggers.single { it.first == TriggerType.BASE }.second
        assertEquals("enabled", base.remote?.enable)
        assertEquals("0 0 1 * *", base.schedules?.single()?.cron)

        val git = triggers.single { it.first == TriggerType.CODE_GIT }.second
        assertEquals("aliasName/xxx", git.repoName)
        assertEquals(listOf("master"), git.push?.branches)
        assertEquals(listOf("master"), git.mr?.targetBranches)
    }

    @Test
    fun `combo base and nested artifact yaml converts correctly (fused object form)`() {
        val triggers = parseTriggers("trigger/combo-base-artifact.yml")

        val base = triggers.single { it.first == TriggerType.BASE }.second
        assertEquals("enabled", base.remote?.enable)

        val arrived = arrivedOf(triggers.single { it.first == TriggerType.ARTIFACT }.second)
        assertEquals("pipeline", arrived.repository)
        assertEquals(listOf("*.msi"), arrived.artifactsName)
    }

    @Test
    fun `combo multi trigger yaml converts correctly`() {
        val triggers = parseTriggers("trigger/combo-multi.yml")

        assertTrue(triggers.any { it.first == TriggerType.BASE })

        val git = triggers.single { it.first == TriggerType.CODE_GIT }.second
        assertEquals("aliasName/xxx", git.repoName)
        assertEquals(listOf("master"), git.push?.branches)

        val tapd = triggers.single { it.first == TriggerType.TAPD }.second
        assertEquals("12345", tapd.workspaceId)
        assertEquals(listOf("create"), tapd.story?.action)

        val artifacts = triggers.filter { it.first == TriggerType.ARTIFACT }.map { arrivedOf(it.second) }
        assertEquals(2, artifacts.size)
        assertNotNull(artifacts.single { it.repository == "pipeline" })
        val image = artifacts.single { it.repository == "image" }
        assertEquals("bk-ci/backend", image.image)
        assertEquals(listOf("v*"), image.tags)
    }
}

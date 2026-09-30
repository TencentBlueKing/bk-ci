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

package com.tencent.devops.process.yaml.transfer.aspect

import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.transfer.PreStep
import com.tencent.devops.process.yaml.transfer.TransferCacheService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RelyParamFilterAspectTest {

    companion object {
        private const val ATOM_KEY = "demoAtom@1.0.0"
        // 插件参数定义（task.json 的 input 部分），p2 仅在 p1 选择 A 时展示
        private val INPUT_PROPS = mapOf<String, Any>(
            "p1" to mapOf(
                "type" to "selector",
                "default" to "A"
            ),
            "p2" to mapOf(
                "type" to "string",
                "default" to "",
                "rely" to mapOf(
                    "expression" to listOf(mapOf("key" to "p1", "value" to "A")),
                    "operation" to "AND"
                )
            )
        )
    }

    @Test
    fun `hidden param should be removed from with`() {
        val with = mutableMapOf<String, Any?>("p1" to "B", "p2" to "kept by mistake")
        val aspect = RelyParamFilterAspect(mockCache())
        aspect.elementAspect().after(joinPoint(buildElement("B"), with))
        assertTrue(with.containsKey("p1"))
        assertFalse(with.containsKey("p2"))
    }

    @Test
    fun `shown param should be kept`() {
        val with = mutableMapOf<String, Any?>("p1" to "A", "p2" to "normal value")
        val aspect = RelyParamFilterAspect(mockCache())
        aspect.elementAspect().after(joinPoint(buildElement("A"), with))
        assertEquals(2, with.size)
    }

    @Test
    fun `param which equals default value should be judged by full value`() {
        val cache = mockCache()
        // p1 与默认值相同，不会出现在 with 中，但求值时仍需参与判断
        every { cache.getAtomDefaultValue(ATOM_KEY) } returns JSONObject(mapOf("p1" to "A"))
        val with = mutableMapOf<String, Any?>("p2" to "normal value")
        val aspect = RelyParamFilterAspect(cache)
        val element = MarketBuildAtomElement(
            atomCode = "demoAtom",
            version = "1.0.0",
            data = mapOf("input" to mutableMapOf<String, Any>("p1" to "A", "p2" to "normal value"))
        )
        aspect.elementAspect().after(joinPoint(element, with))
        assertTrue(with.containsKey("p2"))
    }

    @Test
    fun `prefetch model should request props only once`() {
        val cache = mockCache()
        val aspect = RelyParamFilterAspect(cache)
        aspect.modelAspect().before(mockk<PipelineTransferJoinPoint>(relaxed = true).apply {
            every { model() } returns Model(
                name = "demo",
                desc = "",
                stages = listOf(
                    Stage(
                        containers = listOf(
                            NormalContainer(
                                elements = listOf(buildElement("B"))
                            )
                        ),
                        id = "1",
                        name = "stage"
                    )
                )
            )
        })
        val with = mutableMapOf<String, Any?>("p1" to "B", "p2" to "kept by mistake")
        aspect.elementAspect().after(joinPoint(buildElement("B"), with))
        assertFalse(with.containsKey("p2"))
        // 预取阶段一次批量请求，element 阶段不再重复请求
        verify(exactly = 1) { cache.getAtomInputProps(any()) }
    }

    @Test
    fun `built in element should be ignored`() {
        val with = mutableMapOf<String, Any?>("p2" to "kept")
        val aspect = RelyParamFilterAspect(mockCache())
        val jp = mockk<PipelineTransferJoinPoint>(relaxed = true)
        every { jp.modelElement() } returns LinuxScriptElement(
            name = "script",
            scriptType = BuildScriptType.SHELL,
            script = "echo",
            continueNoneZero = false
        )
        every { jp.yamlPreStep() } returns preStep(with)
        aspect.elementAspect().after(jp)
        assertTrue(with.containsKey("p2"))
    }

    private fun mockCache(): TransferCacheService {
        val cache = mockk<TransferCacheService>()
        every { cache.getAtomDefaultValue(any()) } returns JSONObject()
        every { cache.getAtomInputProps(any()) } returns mapOf(ATOM_KEY to INPUT_PROPS)
        return cache
    }

    private fun buildElement(value: String) = MarketBuildAtomElement(
        atomCode = "demoAtom",
        version = "1.0.0",
        data = mapOf("input" to mutableMapOf<String, Any>("p1" to value, "p2" to "kept by mistake"))
    )

    private fun joinPoint(
        element: Element,
        with: MutableMap<String, Any?>
    ): PipelineTransferJoinPoint {
        val jp = mockk<PipelineTransferJoinPoint>(relaxed = true)
        every { jp.modelElement() } returns element
        every { jp.yamlPreStep() } returns preStep(with)
        return jp
    }

    private fun preStep(with: MutableMap<String, Any?>) = PreStep(
        name = "demo",
        id = null,
        uses = ATOM_KEY,
        with = with,
        timeoutMinutes = null,
        continueOnError = null,
        retryTimes = null
    )
}

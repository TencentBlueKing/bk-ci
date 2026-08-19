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

package com.tencent.devops.process.engine.atom

import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.option.StageControlOption
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.store.pojo.atom.enums.JobTypeEnum
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 覆盖「插件是否适用于其运行所在节点操作系统」校验里的纯函数部分。
 *
 * 这项校验会阻断保存，正确性集中在几个边界上：Job 的操作系统何时算「无从确定」、
 * 差集豁免基准与前向校验的口径是否一致、以及三个按渠道分叉的规则。
 */
class AtomUtilsTest {

    // region resolveJobRunEnvOs：Job 自身声明的构建环境操作系统

    @Test
    fun `resolveJobRunEnvOs 取 baseOS 的明确声明`() {
        assertEquals(OS.LINUX, AtomUtils.resolveJobRunEnvOs(vmContainer(baseOS = VMBaseOS.LINUX)))
        assertEquals(OS.WINDOWS, AtomUtils.resolveJobRunEnvOs(vmContainer(baseOS = VMBaseOS.WINDOWS)))
        assertEquals(OS.MACOS, AtomUtils.resolveJobRunEnvOs(vmContainer(baseOS = VMBaseOS.MACOS)))
    }

    @Test
    fun `resolveJobRunEnvOs 对不限制操作系统的 Job 返回空`() {
        // 第三方构建机环境未指定 agentSelector 时即为 ALL，取不到唯一确定的操作系统
        assertNull(AtomUtils.resolveJobRunEnvOs(vmContainer(baseOS = VMBaseOS.ALL)))
        assertNull(AtomUtils.resolveJobRunEnvOs(vmContainer(baseOS = null)))
    }

    @Test
    fun `resolveJobRunEnvOs 对矩阵 Job 返回空`() {
        // 矩阵 Job 的构建机可由矩阵变量决定，baseOS 只是转换期的单一取值，不足以代表每种组合
        val matrixJob = vmContainer(baseOS = VMBaseOS.LINUX).apply { matrixGroupFlag = true }
        assertNull(AtomUtils.resolveJobRunEnvOs(matrixJob))
    }

    @Test
    fun `resolveJobRunEnvOs 对无编译环境 Job 返回空`() {
        assertNull(AtomUtils.resolveJobRunEnvOs(NormalContainer(name = "buildless")))
    }

    // endregion

    // region collectRunEnvOsAtomKeys：差集豁免的基准组合

    @Test
    fun `collectRunEnvOsAtomKeys 对空编排返回空集`() {
        assertTrue(AtomUtils.collectRunEnvOsAtomKeys(model = null, settingRunEnvOs = OS.LINUX).isEmpty())
    }

    @Test
    fun `collectRunEnvOsAtomKeys 逐 Job 取各自声明的操作系统`() {
        val model = modelOf(
            vmContainer(baseOS = VMBaseOS.LINUX, elements = listOf(marketAtom("atomA", "1.0.0"))),
            vmContainer(baseOS = VMBaseOS.WINDOWS, elements = listOf(marketAtom("atomB", "2.1.0")))
        )
        assertEquals(
            setOf("LINUX:atomA:1.0.0", "WINDOWS:atomB:2.1.0"),
            AtomUtils.collectRunEnvOsAtomKeys(model = model, settingRunEnvOs = null)
        )
    }

    @Test
    fun `collectRunEnvOsAtomKeys 设置指定的操作系统覆盖 Job 自身的声明`() {
        // 创作流的 Job 跑在设置所选的创作环境上，其 baseOS 是 YAML 互转落下的默认值，不能作为校验目标
        val model = modelOf(
            vmContainer(baseOS = VMBaseOS.LINUX, elements = listOf(marketAtom("atomA", "1.0.0")))
        )
        assertEquals(
            setOf("WINDOWS:atomA:1.0.0"),
            AtomUtils.collectRunEnvOsAtomKeys(model = model, settingRunEnvOs = OS.WINDOWS)
        )
    }

    @Test
    fun `collectRunEnvOsAtomKeys 跳过操作系统无从确定的 Job`() {
        val model = modelOf(
            vmContainer(baseOS = VMBaseOS.ALL, elements = listOf(marketAtom("atomA", "1.0.0"))),
            vmContainer(baseOS = null, elements = listOf(marketAtom("atomB", "1.0.0"))),
            vmContainer(baseOS = VMBaseOS.LINUX, elements = listOf(marketAtom("atomC", "1.0.0")))
                .apply { matrixGroupFlag = true },
            vmContainer(baseOS = VMBaseOS.LINUX, elements = listOf(marketAtom("atomD", "1.0.0")))
        )
        assertEquals(
            setOf("LINUX:atomD:1.0.0"),
            AtomUtils.collectRunEnvOsAtomKeys(model = model, settingRunEnvOs = null)
        )
    }

    @Test
    fun `collectRunEnvOsAtomKeys 跳过无编译环境 Job 与非市场插件`() {
        val model = modelOf(
            NormalContainer(name = "buildless", elements = listOf(marketAtom("atomA", "1.0.0"))),
            vmContainer(
                baseOS = VMBaseOS.LINUX,
                elements = listOf(ManualTriggerElement(name = "manual"), marketAtom("atomB", "1.0.0"))
            )
        )
        assertEquals(
            setOf("LINUX:atomB:1.0.0"),
            AtomUtils.collectRunEnvOsAtomKeys(model = model, settingRunEnvOs = null)
        )
    }

    @Test
    fun `collectRunEnvOsAtomKeys 把未声明版本归一为浮动版本`() {
        // 与前向校验必须用同一种归一方式，否则同一个组合会被误判为本次新引入
        val model = modelOf(
            vmContainer(baseOS = VMBaseOS.LINUX, elements = listOf(marketAtom("atomA", "")))
        )
        assertEquals(
            setOf("LINUX:atomA:1.*"),
            AtomUtils.collectRunEnvOsAtomKeys(model = model, settingRunEnvOs = null)
        )
    }

    @Test
    fun `collectRunEnvOsAtomKeys 计入被禁用的 Stage 与 Job`() {
        // 基准只回答「该组合此前是否已存在于编排中」，与是否会被调度无关：
        // 把禁用的 Job 重新启用不该被当作本次新引入而拦下
        val disabledJob = vmContainer(
            baseOS = VMBaseOS.LINUX,
            elements = listOf(marketAtom("atomA", "1.0.0"))
        ).apply { jobControlOption = JobControlOption(enable = false) }
        val enabledJob = vmContainer(baseOS = VMBaseOS.LINUX, elements = listOf(marketAtom("atomB", "1.0.0")))
        val model = Model(
            name = "test",
            desc = "",
            stages = listOf(
                Stage(
                    id = "stage-1",
                    name = "stage-1",
                    containers = listOf(disabledJob, enabledJob),
                    stageControlOption = StageControlOption(enable = false)
                )
            )
        )
        assertEquals(
            setOf("LINUX:atomA:1.0.0", "LINUX:atomB:1.0.0"),
            AtomUtils.collectRunEnvOsAtomKeys(model = model, settingRunEnvOs = null)
        )
    }

    @Test
    fun `collectRunEnvOsAtomKeys 对同一插件在不同操作系统下算出不同的组合`() {
        // 换过运行环境时基准与本次算出的 key 不同，同一个插件因而会被照常拦下
        val model = modelOf(
            vmContainer(baseOS = VMBaseOS.LINUX, elements = listOf(marketAtom("atomA", "1.0.0")))
        )
        val onLinux = AtomUtils.collectRunEnvOsAtomKeys(model = model, settingRunEnvOs = OS.LINUX)
        val onWindows = AtomUtils.collectRunEnvOsAtomKeys(model = model, settingRunEnvOs = OS.WINDOWS)
        assertTrue(onLinux.intersect(onWindows).isEmpty())
    }

    // endregion

    // region 三个按渠道分叉的规则，新增渠道时需一并确认

    @Test
    fun `resolveOsJobType 仅创作流取创作流的插件声明`() {
        ChannelCode.values().forEach { channelCode ->
            val expected = if (channelCode == ChannelCode.CREATIVE_STREAM) {
                JobTypeEnum.CREATIVE_STREAM
            } else {
                JobTypeEnum.AGENT
            }
            assertEquals(expected, AtomUtils.resolveOsJobType(channelCode), "channel=$channelCode")
        }
    }

    @Test
    fun `isRunEnvSpecifiedBySetting 仅创作流的运行环境由设置指定`() {
        ChannelCode.values().forEach { channelCode ->
            assertEquals(
                channelCode == ChannelCode.CREATIVE_STREAM,
                AtomUtils.isRunEnvSpecifiedBySetting(channelCode),
                "channel=$channelCode"
            )
        }
    }

    @Test
    fun `isPlatformMaintainedChannel 仅研发商店的内置流水线由平台维护`() {
        ChannelCode.values().forEach { channelCode ->
            assertEquals(
                channelCode == ChannelCode.AM,
                AtomUtils.isPlatformMaintainedChannel(channelCode),
                "channel=$channelCode"
            )
        }
    }

    @Test
    fun `运行环境由设置指定的渠道不会同时被当作平台维护的渠道`() {
        // 两者都会改变校验目标的取法，同时命中会让口径含义不明
        ChannelCode.values().forEach { channelCode ->
            assertFalse(
                AtomUtils.isRunEnvSpecifiedBySetting(channelCode) &&
                    AtomUtils.isPlatformMaintainedChannel(channelCode),
                "channel=$channelCode"
            )
        }
    }

    // endregion

    private fun vmContainer(
        baseOS: VMBaseOS?,
        elements: List<Element> = emptyList()
    ) = VMBuildContainer(
        name = "job-${baseOS ?: "none"}-${elements.size}",
        baseOS = baseOS,
        elements = elements
    )

    private fun marketAtom(atomCode: String, version: String) = MarketBuildAtomElement(
        name = atomCode,
        atomCode = atomCode,
        version = version
    )

    private fun modelOf(vararg containers: Container) = Model(
        name = "test",
        desc = "",
        stages = containers.mapIndexed { index, container ->
            Stage(id = "stage-$index", name = "stage-$index", containers = listOf(container))
        }
    )
}

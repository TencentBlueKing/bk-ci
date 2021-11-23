package com.tencent.devops.common.pipeline.pojo

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.matrix.NormalMatrixGroupContainer
import com.tencent.devops.common.pipeline.container.matrix.VMBuildMatrixGroupContainer
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParam
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import javax.swing.text.html.parser.Element
import org.junit.Assert
import org.junit.Test

internal class StagePauseCheckTest {

    @Test
    fun parseReviewParams() {
        val check = StagePauseCheck(
            manualTrigger = true,
            reviewParams = mutableListOf(
                ManualReviewParam(key = "p1", value = "111"),
                ManualReviewParam(key = "p2", value = "222")
            )
        )
        val originKeys = check.reviewParams?.map { it.key }?.toList()
        val params = mutableListOf(
            ManualReviewParam(key = "p1", value = "123"),
            ManualReviewParam(key = "p2", value = "222")
        )
        Assert.assertEquals(
            mutableListOf(ManualReviewParam(key = "p1", value = "123")),
            check.parseReviewParams(params)
        )
        Assert.assertEquals(
            check.reviewParams?.map { it.key }?.toList(),
            originKeys
        )
    }

    @Test
    fun genContainer() {
        val vmContainer = VMBuildMatrixGroupContainer(
            id = "1",
            name = "构建矩阵demo",
            elements = listOf(
                MarketBuildAtomElement(
                    name = "市场插件",
                    atomCode = "checkout",
                    version = "1.*",
                    data = mapOf()
                ),
                LinuxScriptElement(
                    name = "内置脚本插件",
                    scriptType = BuildScriptType.SHELL,
                    script = "echo \${{ matrix.var1 }} \n echo \${{ matrix.var2 }}",
                    continueNoneZero = false
                )
            ),
            status = BuildStatus.FAILED.name,
            startEpoch = 1551807007986,
            executeCount = 1,
            jobId = "myMatrix1",
            canRetry = true,
            containerId = "c-9de5cd612def4e7caa448ee116fa8560",
            matrixGroupFlag = true,
            strategyStr = """
                os: [docker,macos]
                var1: [a,b,c]
                var2: [1,2,3]
            """,
            runsOn = "#stream保留字段#",
            includeCase = listOf(mapOf("var1" to "a"), mapOf("var2" to "2")),
            excludeCase = listOf(mapOf("var2" to "1")),
            groupContainers = mutableListOf(),
            totalCount = 10, // 3*3 + 2 - 1
            runningCount = 1,
            buildEnv = mapOf("variables.var1" to "aaa", "variables.var2" to "bbb"),
            customBuildEnv = mapOf("e1" to "123", "e2" to "321"),
            mutexGroup = null,
            fastKill = true,
            maxConcurrency = 50
        )

        val normalContainer = NormalMatrixGroupContainer(
            id = "2",
            name = "构建矩阵demo",
            elements = listOf(),
            status = BuildStatus.FAILED.name,
            startEpoch = 1551807007986,
            executeCount = 1,
            jobId = "myMatrix2",
            canRetry = true,
            containerId = "c-9de5cd612def4e7caa448ee116fa8560",
            matrixGroupFlag = true,
            strategyStr = """
                m1: [a,b,c]
                m2: [1,2,3]
            """,
            includeCase = listOf(mapOf("m3" to "1"), mapOf("m3" to "2")),
            excludeCase = listOf(mapOf("m2" to "1")),
            groupContainers = mutableListOf(),
            totalCount = 10, // 3*3 + 2 - 1
            runningCount = 1,
            mutexGroup = null,
            fastKill = true,
            maxConcurrency = 50
        )
        val model = Model(
            name = "构建矩阵样例",
            desc = null,
            stages = listOf(
                Stage(
                    containers = listOf(TriggerContainer(
                        id = "0",
                        name = "手动触发",
                        elements = listOf(ManualTriggerElement)
                    ))
                )
            )
        )

        println(JsonUtil.toJson(model))
    }
}

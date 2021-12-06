package com.tencent.devops.common.pipeline.pojo

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.option.MatrixControlOption
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParam
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.pipeline.pojo.element.matrix.MatrixStatusElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
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
    @Suppress("ALL")
    fun genMatrixContainer() {
        val vmContainer = VMBuildContainer(
            id = "1",
            name = "构建矩阵demo",
            elements = listOf(
                MarketBuildAtomElement(
                    name = "有编译环境市场插件",
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
            baseOS = VMBaseOS.ALL,
            status = BuildStatus.SUCCEED.name,
            startEpoch = 1551807007986,
            executeCount = 1,
            jobId = "myMatrix1",
            canRetry = true,
            containerId = "1",
            containerHashId = "c-9de5cd612def4e7caa448ee116fa8560",
            matrixGroupFlag = true,
            matrixControlOption = MatrixControlOption(
                strategyStr = """
                    os: [docker,macos]
                    var1: [a,b,c]
                    var2: [1,2,3]
                """,
                includeCaseStr = YamlUtil.toYaml(listOf(mapOf("var1" to "a"), mapOf("var2" to "2"))),
                excludeCaseStr = YamlUtil.toYaml(listOf(mapOf("var2" to "1"))),
                totalCount = 10, // 3*3 + 2 - 1
                finishCount = 1,
                fastKill = true,
                maxConcurrency = 50
            ),
            groupContainers = mutableListOf(
                VMBuildContainer(
                    id = "10001",
                    name = "构建矩阵demo",
                    elements = listOf(
                        MatrixStatusElement(
                            name = "有编译环境市场插件",
                            id = "e-8104cd612def4e7caa448ee116fa8560",
                            status = BuildStatus.SUCCEED.name,
                            executeCount = 1,
                            elapsed = 123
                        ),
                        MatrixStatusElement(
                            name = "内置脚本插件",
                            id = "e-1536cd612def4e7caa448ee116fa8560",
                            status = BuildStatus.SUCCEED.name,
                            executeCount = 1,
                            elapsed = 321
                        )
                    ),
                    status = BuildStatus.SUCCEED.name,
                    startEpoch = 1551807007986,
                    executeCount = 1,
                    canRetry = true,
                    containerId = "10001",
                    containerHashId = "c-9de5cd612def4e7caa448ee116fa8560",
                    matrixGroupFlag = true,
                    buildEnv = mapOf("variables.var1" to "aaa", "variables.var2" to "bbb"),
                    customBuildEnv = mapOf("e1" to "123", "e2" to "321"),
                    mutexGroup = null,
                    baseOS = VMBaseOS.ALL,
                    matrixGroupId = "c-9de5cd612def4e7caa448ee116fa8560"
                )
            ),
            buildEnv = mapOf("variables.var1" to "aaa", "variables.var2" to "bbb"),
            customBuildEnv = mapOf("e1" to "123", "e2" to "321"),
            mutexGroup = null
        )
        val normalContainer = NormalContainer(
            id = "2",
            name = "构建矩阵demo",
            elements = listOf(
                MarketBuildLessAtomElement(
                    name = "无有编译环境市场插件",
                    atomCode = "JobScriptExecutionA",
                    version = "1.*",
                    data = mapOf()
                )
            ),
            status = BuildStatus.SUCCEED.name,
            startEpoch = 1551807007986,
            executeCount = 1,
            jobId = "myMatrix2",
            canRetry = true,
            containerId = "2",
            containerHashId = "c-4512cd612def4e7caa448ee116fa8560",
            mutexGroup = null,
            matrixGroupFlag = true,
            matrixControlOption = MatrixControlOption(
                strategyStr = """
                    m1: [a,b,c]
                    m2: [1,2,3]
                """,
                includeCaseStr = YamlUtil.toYaml(listOf(mapOf("m3" to "1"), mapOf("m3" to "2"))),
                excludeCaseStr = YamlUtil.toYaml(listOf(mapOf("m2" to "1"))),
                totalCount = 10, // 3*3 + 2 - 1
                finishCount = 1,
                fastKill = true,
                maxConcurrency = 50
            ),
            groupContainers = mutableListOf(
                NormalContainer(
                    id = "20001",
                    name = "构建矩阵demo",
                    elements = listOf(
                        MatrixStatusElement(
                            name = "无有编译环境市场插件",
                            id = "e-2314cd612def4e7caa448ee116fa8560",
                            status = BuildStatus.SUCCEED.name,
                            executeCount = 1,
                            elapsed = 234
                        )
                    ),
                    status = BuildStatus.SUCCEED.name,
                    startEpoch = 1551807007986,
                    executeCount = 1,
                    jobId = "myMatrix2",
                    canRetry = true,
                    containerId = "20001",
                    containerHashId = "c-3124cd612def4e7caa448ee116fa8560",
                    matrixGroupFlag = true,
                    mutexGroup = null,
                    matrixGroupId = "c-4512cd612def4e7caa448ee116fa8560"
                )
            )
        )
        val triggerContainer = TriggerContainer(
            id = "0",
            name = "触发构建",
            elements = listOf(ManualTriggerElement(name = "手动触发"))
        )

        val model = Model(
            name = "构建矩阵样例",
            desc = null,
            stages = listOf(
                Stage(id = "stage-1", name = "stage-1", containers = listOf(triggerContainer)),
                Stage(id = "stage-2", name = "stage-2", containers = listOf(vmContainer, normalContainer))
            )
        )
        println(model.getStage("stage-2")?.getContainer("1")?.getContainerById("10001"))
        println(model.getContainer("10001"))
        println(model.getContainer("20001"))
        println(JsonUtil.toJson(model))
    }
}

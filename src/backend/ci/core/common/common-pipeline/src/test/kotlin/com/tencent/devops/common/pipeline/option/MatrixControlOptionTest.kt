package com.tencent.devops.common.pipeline.option

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.YamlUtil
import org.junit.Assert
import org.junit.Test

internal class MatrixControlOptionTest {

    @Test
    fun convertStrategyYaml() {
        val matrixControlOption = MatrixControlOption(
            strategyStr = """
                    os: [docker,macos]
                    var1: [a,b,c]
                    var2: [1,2,3]
                """,
            includeCaseStr = YamlUtil.toYaml(listOf(mapOf("var1" to "a"), mapOf("var2" to "2"))),
            excludeCaseStr = YamlUtil.toYaml(listOf(mapOf("var2" to "1"))),
            totalCount = 10, // 3*3 + 2 - 1
            runningCount = 1,
            fastKill = true,
            maxConcurrency = 50
        )
        val result =
            mapOf("os" to listOf("docker", "macos"), "var1" to listOf("a", "b", "c"), "var2" to listOf(1, 2, 3))

        print(matrixControlOption.convertStrategy())
        Assert.assertEquals(JsonUtil.toJson(matrixControlOption.convertStrategy()), JsonUtil.toJson(result))
    }

    @Test
    fun convertStrategyJson() {
        val matrixControlOption = MatrixControlOption(
            strategyStr = """{
                    "os": [docker,macos],
                    "var1": [a,b,c],
                    "var2": [1,2,3],
                }""",
            includeCaseStr = YamlUtil.toYaml(listOf(mapOf("var1" to "a"), mapOf("var2" to "2"))),
            excludeCaseStr = YamlUtil.toYaml(listOf(mapOf("var2" to "1"))),
            totalCount = 10, // 3*3 + 2 - 1
            runningCount = 1,
            fastKill = true,
            maxConcurrency = 50
        )
        val result =
            mapOf("os" to listOf("docker", "macos"), "var1" to listOf("a", "b", "c"), "var2" to listOf(1, 2, 3))

        print(matrixControlOption.convertStrategy())
        Assert.assertEquals(JsonUtil.toJson(matrixControlOption.convertStrategy()), JsonUtil.toJson(result))
    }

    @Test
    fun convertIncludeCase() {
        val matrixControlOption = MatrixControlOption(
            strategyStr = """{
                    "os": [docker,macos],
                    "var1": [a,b,c],
                    "var2": [1,2,3],
                }""",
            includeCaseStr = YamlUtil.toYaml(listOf(mapOf("var1" to "a"), mapOf("var2" to "2"))),
            excludeCaseStr = YamlUtil.toYaml(listOf(mapOf("var2" to "1"))),
            totalCount = 10, // 3*3 + 2 - 1
            runningCount = 1,
            fastKill = true,
            maxConcurrency = 50
        )
        val result = listOf(mapOf("var1" to "a"), mapOf("var2" to "2"))

        print(matrixControlOption.convertIncludeCase())
        Assert.assertEquals(JsonUtil.toJson(matrixControlOption.convertIncludeCase()), JsonUtil.toJson(result))
    }

    @Test
    fun convertExcludeCase() {
        val matrixControlOption = MatrixControlOption(
            strategyStr = """{
                    "os": [docker,macos],
                    "var1": [a,b,c],
                    "var2": [1,2,3],
                }""",
            includeCaseStr = YamlUtil.toYaml(listOf(mapOf("var1" to "a"), mapOf("var2" to "2"))),
            excludeCaseStr = YamlUtil.toYaml(listOf(mapOf("var2" to "1"))),
            totalCount = 10, // 3*3 + 2 - 1
            runningCount = 1,
            fastKill = true,
            maxConcurrency = 50
        )
        val result = listOf(mapOf("var2" to "1"))

        print(matrixControlOption.convertExcludeCase())
        Assert.assertEquals(JsonUtil.toJson(matrixControlOption.convertExcludeCase()), JsonUtil.toJson(result))
    }
}
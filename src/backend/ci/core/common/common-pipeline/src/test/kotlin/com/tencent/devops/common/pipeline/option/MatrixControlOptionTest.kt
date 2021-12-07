package com.tencent.devops.common.pipeline.option

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.pipeline.utils.MatrixContextUtils
import org.junit.Assert
import org.junit.Test
import java.util.Random

internal class MatrixControlOptionTest {

    @Test
    fun convertCase(){
        val matrixControlOption = MatrixControlOption(
            // 2*3*3 = 18
            strategyStr = """
                    os: [docker,macos]
                    var1: [a,b,c]
                    var2: [1,2,3]
                """,
            includeCaseStr = YamlUtil.toYaml(
                listOf(
                    // +1
                    mapOf(
                        "os" to "docker",
                        "var1" to "d",
                        "var2" to "0"
                    ),
                    // +1
                    mapOf(
                        "os" to "macos",
                        "var1" to "d",
                        "var2" to "4"
                    ),
                    // +0 一样的值只会加入一个
                    mapOf(
                        "os" to "macos",
                        "var1" to "d",
                        "var2" to "4"
                    ),
                    // +1 多一个变量也是一个新组合
                    mapOf(
                        "os" to "macos",
                        "var1" to "d",
                        "var2" to "4",
                        "var3" to "6"
                    ),
                    // +0 重复值加入无效
                    mapOf(
                        "os" to "docker",
                        "var1" to "a",
                        "var2" to "1"
                    )
                )
            ),
            // -1
            excludeCaseStr = YamlUtil.toYaml(
                listOf(
                    mapOf(
                        "os" to "docker",
                        "var1" to "a",
                        "var2" to "1"
                    )
                )
            ),
            totalCount = 10, // 2*3*3 + 3 -1
            finishCount = 1,
            fastKill = true,
            maxConcurrency = 50
        )
        val contextCase = matrixControlOption.getAllContextCase(emptyMap())
        println(contextCase.size)
        contextCase.forEachIndexed { index, map ->
            println("$index: $map")
        }
        Assert.assertEquals(contextCase.size, 20)
    }

    @Test
    fun cartesianProductTest() {
        val array2d = List(7) { List(7) { Random().nextInt(100) } }
        val timeAStart = System.currentTimeMillis()
        val a = MatrixContextUtils.loopCartesianProduct(array2d)
        val timeAEnd = System.currentTimeMillis()
        val timeBStart = System.currentTimeMillis()
        val b = MatrixContextUtils.recursiveCartesianProduct(array2d)
        val timeBEnd = System.currentTimeMillis()
        println("loopCartesianProduct cost:${timeAEnd - timeAStart}ms")
        println("recursiveCartesianProduct cost:${timeBEnd - timeBStart}ms")
        Assert.assertEquals(JsonUtil.toJson(a), JsonUtil.toJson(b))
    }

    @Test
    fun calculateValueMatrix() {
        val matrixControlOption = MatrixControlOption(
            // 2*3*3 = 18
            strategyStr = """
                    os: [docker,macos]
                    var1: [a,b,c]
                    var2: [1,2,3]
                """,
            // +2
            includeCaseStr = YamlUtil.toYaml(
                listOf(
                    mapOf(
                        "os" to "docker",
                        "var1" to "d",
                        "var2" to "0"
                    ),
                    mapOf(
                        "os" to "macos",
                        "var1" to "d",
                        "var2" to "4"
                    ),
                    // +0 重复值不加入
                    mapOf(
                        "os" to "docker",
                        "var1" to "a",
                        "var2" to "1"
                    )
                )
            ),
            // -1
            excludeCaseStr = YamlUtil.toYaml(
                listOf(
                    mapOf(
                        "os" to "docker",
                        "var1" to "a",
                        "var2" to "1"
                    )
                )
            ),
            totalCount = 10, // 3*3 + 2 - 1
            finishCount = 1,
            fastKill = true,
            maxConcurrency = 50
        )
        val contextCase = matrixControlOption.getAllContextCase(emptyMap())
        println(contextCase.size)
        contextCase.forEachIndexed { index, map ->
            println("$index: $map")
        }
        Assert.assertEquals(contextCase.size, 20)
    }

    @Test
    fun calculateValueMatrix2() {
        val matrixControlOption = MatrixControlOption(
            // 2*3*3 = 18
            strategyStr = """
                    {
                        "include": [
                            {
                                "os": "docker",
                                "var1": "form_json",
                                "var2": 2
                            },
                            {
                                "os": "macos",
                                "var1": "form_json",
                                "var2": 1
                            }
                        ],
                        "exclude": [
                            {
                                "os": "docker",
                                "var1": "b",
                                "var2": 3
                            },
                            {
                                "os": "macos",
                                "var1": "c",
                                "var2": 2
                            }
                        ],
                        "strategy": {
                            "os": [
                                "docker",
                                "macos"
                            ],
                            "var1": [
                                "a",
                                "b",
                                "c"
                            ],
                            "var2": [
                                1,
                                2,
                                3
                            ]
                        }
                    }
                """,
            // +2
            includeCaseStr = YamlUtil.toYaml(
                listOf(
                    mapOf(
                        "os" to "docker",
                        "var1" to "includeCaseStr",
                        "var2" to "0"
                    ),
                    mapOf(
                        "os" to "macos",
                        "var1" to "includeCaseStr",
                        "var2" to "4"
                    ),
                    // +0 重复值不加入
                    mapOf(
                        "os" to "docker",
                        "var1" to "includeCaseStr",
                        "var2" to "1"
                    )
                )
            ),
            // -1
            excludeCaseStr = YamlUtil.toYaml(
                listOf(
                    mapOf(
                        "os" to "docker",
                        "var1" to "a",
                        "var2" to "1"
                    )
                )
            ),
            totalCount = 10, // 3*3 + 2 - 1
            finishCount = 1,
            fastKill = true,
            maxConcurrency = 50
        )
        val contextCase = matrixControlOption.getAllContextCase(emptyMap())
        println(contextCase.size)
        contextCase.forEachIndexed { index, map ->
            println("$index: $map")
        }
        Assert.assertEquals(contextCase.size, 20)
    }

    @Test
    fun calculateValueMatrix3() {
        val matrixControlOption = MatrixControlOption(
            // 2*3*3 = 18
            strategyStr = "\${{fromJSON(depends.job1.outputs.matrix)}}",
            // +2
            includeCaseStr = YamlUtil.toYaml(
                listOf(
                    mapOf(
                        "os" to "docker",
                        "var1" to "includeCaseStr",
                        "var2" to "0"
                    ),
                    mapOf(
                        "os" to "macos",
                        "var1" to "includeCaseStr",
                        "var2" to "4"
                    ),
                    // +0 重复值不加入
                    mapOf(
                        "os" to "docker",
                        "var1" to "includeCaseStr",
                        "var2" to "1"
                    )
                )
            ),
            // -1
            excludeCaseStr = YamlUtil.toYaml(
                listOf(
                    mapOf(
                        "os" to "docker",
                        "var1" to "a",
                        "var2" to "1"
                    )
                )
            ),
            totalCount = 10, // 3*3 + 2 - 1
            finishCount = 1,
            fastKill = true,
            maxConcurrency = 50
        )
        val buildContext = mapOf(
            "depends.job1.outputs.matrix" to """
            {
                        "include": [
                            {
                                "os": "docker",
                                "var1": "form_json",
                                "var2": 2
                            },
                            {
                                "os": "macos",
                                "var1": "form_json",
                                "var2": 1
                            }
                        ],
                        "exclude": [
                            {
                                "os": "docker",
                                "var1": "b",
                                "var2": 3
                            },
                            {
                                "os": "macos",
                                "var1": "c",
                                "var2": 2
                            }
                        ],
                        "strategy": {
                            "os": [
                                "docker",
                                "macos"
                            ],
                            "var1": [
                                "a",
                                "b",
                                "c"
                            ],
                            "var2": [
                                1,
                                2,
                                3
                            ]
                        }
                    }
        """
        )
        val contextCase = matrixControlOption.getAllContextCase(buildContext)
        println(contextCase.size)
        contextCase.forEachIndexed { index, map ->
            println("$index: $map")
        }
        Assert.assertEquals(contextCase.size, 20)
    }

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
            finishCount = 1,
            fastKill = true,
            maxConcurrency = 50
        )
        val result = mapOf(
            "os" to listOf("docker", "macos"),
            "var1" to listOf("a", "b", "c"),
            "var2" to listOf("1", "2", "3")
        )
//        Assert.assertEquals(JsonUtil.toJson(matrixControlOption.getAllContextCase()), JsonUtil.toJson(result))
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
            finishCount = 1,
            fastKill = true,
            maxConcurrency = 50
        )
        val result =
            mapOf("os" to listOf("docker", "macos"), "var1" to listOf("a", "b", "c"), "var2" to listOf(1, 2, 3))

//        print(matrixControlOption.getAllContextCase())
//        Assert.assertEquals(JsonUtil.toJson(matrixControlOption.getAllContextCase()), JsonUtil.toJson(result))
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
            finishCount = 1,
            fastKill = true,
            maxConcurrency = 50
        )
        val result = listOf(mapOf("var1" to "a"), mapOf("var2" to "2"))

//        print(matrixControlOption.convertIncludeCase())
//        Assert.assertEquals(JsonUtil.toJson(matrixControlOption.convertIncludeCase()), JsonUtil.toJson(result))
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
            finishCount = 1,
            fastKill = true,
            maxConcurrency = 50
        )
        val result = listOf(mapOf("var2" to "1"))

//        print(matrixControlOption.convertExcludeCase())
//        Assert.assertEquals(JsonUtil.toJson(matrixControlOption.convertExcludeCase()), JsonUtil.toJson(result))
    }
}

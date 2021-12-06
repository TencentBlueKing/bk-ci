package com.tencent.devops.common.pipeline.option

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.pipeline.pojo.MatrixConvert
import com.tencent.devops.common.pipeline.utils.MatrixContextUtils
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.junit.Assert
import org.junit.Test
import org.yaml.snakeyaml.Yaml
import java.util.Random

@ApiModel("matrix流水线编辑校验yaml模型")
data class MatrixPipelineInfo(
    @ApiModelProperty("作为输入值时:额外的参数组合(String)/作为输出值时:校验结果", required = false)
    val include: String?,
    @ApiModelProperty("作为输入值时:排除的参数组合(String)/作为输出值时:校验结果", required = false)
    val exclude: String?,
    @ApiModelProperty("作为输入值时:分裂策略(String)/作为输出值时:校验结果", required = false)
    var strategy: String?
) {
    fun MatrixPipelineInfo.toMatrixConvert(): MatrixConvert {
        return MatrixConvert(
            include = run { YamlUtil.to<List<Map<String, String>>>(include ?: return null) },
            exclude = run { YamlUtil.to<List<Map<String, String>>>(exclude ?: return  ) },
            strategy = run {
                try {
                    YamlUtil.to<Map<String, List<String>>>(strategy ?: return null)
                } catch (ignore: Throwable) {
                    throw Exception("yaml parse error :${ignore.message}")
                }
            }
        )
    }
}

internal class MatrixControlOptionTest {

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

    private val yaml = Yaml()

    @Test
    fun checkYaml() {
        val yamlstr = MatrixPipelineInfo(
            include = """
                - a: 11
                  b: 33
                - a: 22
                  b: 44
        """,
            exclude = """
                - a: 1
                  b: 3
                - a: 2
                  b: 4
            """,
            strategy = """
                    os: [docker,macos]
                    var1: [a,b,c]
                    var2: [1,2,3]
        """
        )

        println(
            YamlUtil.toYaml(
                YamlUtil.to < Any::class.java > (JsonUtil.toJson(
                    MatrixPipelineInfo(
                        include = yamlstr.include,
                        exclude = null,
                        strategy = null
                    )
                ),
            )
        ))
        println(
            JsonUtil.toJson(
                MatrixPipelineInfo(
                    include = null,
                    exclude = null,
                    strategy = null
                )
            )
        )

        val matrixPipelineInfo = MatrixPipelineInfo(
            include = try {
                MatrixContextUtils.schemaCheck(
                    JsonUtil.toJson(
                        MatrixPipelineInfo(
                            include = yamlstr.include,
                            exclude = null,
                            strategy = null
                        )
                    )
                )
                null
            } catch (e: Exception) {
                e.message
            },
            exclude = try {
                MatrixContextUtils.schemaCheck(
                    JsonUtil.toJson(
                        MatrixPipelineInfo(
                            include = null,
                            exclude = yamlstr.exclude,
                            strategy = null
                        )
                    )
                )
                null
            } catch (e: Exception) {
                e.message
            },
            strategy = try {
                MatrixContextUtils.schemaCheck(
                    JsonUtil.toJson(
                        MatrixPipelineInfo(
                            include = null,
                            exclude = null,
                            strategy = yamlstr.strategy
                        )
                    )
                )
                null
            } catch (e: Exception) {
                e.message
            }
        )
        println(JsonUtil.toJson(matrixPipelineInfo))
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

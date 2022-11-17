package com.tencent.devops.common.pipeline.option

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.pipeline.utils.MatrixContextUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.Random

@Suppress("ALL")
internal class MatrixControlOptionTest {

    @Test
    fun calculateValueMatrix() {
        val matrixControlOption = MatrixControlOption(
            // 2*3*3 = 18
            strategyStr = """
                    os: [docker,macos]
                    var1: [a,b,c]
                    var2: [1,2,3]
                """,
            includeCaseStr = YamlUtil.toYaml(
                listOf(
                    // +0 os = docker, var1 = d 在矩阵中不存在，抛弃
                    mapOf(
                        "os" to "docker",
                        "var1" to "d",
                        "var2" to "0"
                    ),
                    // +0 符合 os = macos, var1 = a, var2 = 1 增加 var3 = xxx
                    mapOf(
                        "os" to "macos",
                        "var1" to "a",
                        "var2" to "1",
                        "var3" to "xxx"
                    ),
                    // +0 符合 os = macos, var1 = b 的增加 var3 = yyy
                    mapOf(
                        "os" to "macos",
                        "var1" to "b",
                        "var3" to "yyy"
                    ),
                    // +0 符合 var1 = c 的增加 var3 = zzz
                    mapOf(
                        "var1" to "c",
                        "var3" to "zzz"
                    ),
                    // +1 符合 os = macos, var1 = b 的增加 var3 = kkk
                    mapOf(
                        "os" to "macos",
                        "var1" to "d",
                        "var3" to "kkk"
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
            totalCount = 10, // 2*3*3 -1
            finishCount = 1,
            fastKill = true,
            maxConcurrency = 50
        )
        val contextCase = matrixControlOption.convertMatrixConfig(emptyMap()).getAllCombinations()
        println(contextCase.size)
        contextCase.forEachIndexed { index, map ->
            println("$index: $map")
        }
        Assertions.assertEquals(19, contextCase.size)
    }

    @Test
    fun calculateValueMatrix1() {
        val matrixControlOption = MatrixControlOption(
            // 2*3*3 = 18
            strategyStr = """
                    platform: [Android, IOS]
                    env: [Test, Development, Shipping]
                """,
            includeCaseStr = YamlUtil.toYaml(
                listOf(
                    // +0 os = docker, var1 = d 在矩阵中不存在，抛弃
                    mapOf(
                        "platform" to "Android",
                        "file_ext" to "apk"
                    ),
                    // +0 符合 os = macos, var1 = a, var2 = 1 增加 var3 = xxx
                    mapOf(
                        "platform" to "IOS",
                        "file_ext" to "ipa"
                    )
                )
            ),
            // -1
            excludeCaseStr = null,
            totalCount = 10, // 2*3*3 -1
            finishCount = 1,
            fastKill = true,
            maxConcurrency = 50
        )
        val contextCase = matrixControlOption.convertMatrixConfig(emptyMap()).getAllCombinations()
        println(contextCase.size)
        contextCase.forEachIndexed { index, map ->
            println("$index: $map")
        }
        Assertions.assertEquals(6, contextCase.size)
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
        Assertions.assertEquals(JsonUtil.toJson(a), JsonUtil.toJson(b))
    }

    @Test
    fun calculateValueMatrixYaml() {
        val matrixControlOption = MatrixControlOption(
            strategyStr = """
---
os:
- "docker"
- "macos"
var1:
- "a"
- "b"
- "c"
var2:
- 1
- 2
- 3
                """,
            includeCaseStr = YamlUtil.toYaml(
                listOf(
                    // +0 os = docker, var1 = d 在矩阵中不存在，抛弃
                    mapOf(
                        "os" to "docker",
                        "var1" to "d",
                        "var2" to "0"
                    ),
                    // +0 符合 os = macos, var1 = a, var2 = 1 增加 var3 = xxx
                    mapOf(
                        "os" to "macos",
                        "var1" to "a",
                        "var2" to "1",
                        "var3" to "xxx"
                    ),
                    // +0 符合 os = macos, var1 = b 的增加 var3 = yyy
                    mapOf(
                        "os" to "macos",
                        "var1" to "b",
                        "var3" to "yyy"
                    ),
                    // +0 符合 var1 = c 的增加 var3 = zzz
                    mapOf(
                        "var1" to "c",
                        "var3" to "zzz"
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
            totalCount = 10,
            finishCount = 1,
            fastKill = true,
            maxConcurrency = 50
        )
        val contextCase = matrixControlOption.convertMatrixConfig(emptyMap()).getAllCombinations()
        println(contextCase.size)
        contextCase.forEachIndexed { index, map ->
            println("$index: $map")
        }
        Assertions.assertEquals(
            JsonUtil.to(
                "[{\"matrix.os\":\"docker\",\"matrix.var1\":\"a\",\"matrix.var2\":\"2\"},{\"matrix.os\":\"docker\"," +
                    "\"matrix.var1\":\"a\",\"matrix.var2\":\"3\"},{\"matrix.os\":\"docker\",\"matrix.var1\":\"b\"," +
                    "\"matrix.var2\":\"1\"},{\"matrix.os\":\"docker\",\"matrix.var1\":\"b\",\"matrix.var2\":\"2\"}," +
                    "{\"matrix.os\":\"docker\",\"matrix.var1\":\"b\",\"matrix.var2\":\"3\"},{\"matrix.os\":\"docker\"" +
                    ",\"matrix.var1\":\"c\",\"matrix.var2\":\"1\",\"matrix.var3\":\"zzz\"},{\"matrix.os\":\"docker\"," +
                    "\"matrix.var1\":\"c\",\"matrix.var2\":\"2\",\"matrix.var3\":\"zzz\"},{\"matrix.os\":\"docker\"," +
                    "\"matrix.var1\":\"c\",\"matrix.var2\":\"3\",\"matrix.var3\":\"zzz\"},{\"matrix.os\":\"macos\"," +
                    "\"matrix.var1\":\"a\",\"matrix.var2\":\"1\",\"matrix.var3\":\"xxx\"},{\"matrix.os\":\"macos\"," +
                    "\"matrix.var1\":\"a\",\"matrix.var2\":\"2\"},{\"matrix.os\":\"macos\",\"matrix.var1\":\"a\"," +
                    "\"matrix.var2\":\"3\"},{\"matrix.os\":\"macos\",\"matrix.var1\":\"b\",\"matrix.var2\":\"1\"," +
                    "\"matrix.var3\":\"yyy\"},{\"matrix.os\":\"macos\",\"matrix.var1\":\"b\",\"matrix.var2\":\"2\"," +
                    "\"matrix.var3\":\"yyy\"},{\"matrix.os\":\"macos\",\"matrix.var1\":\"b\",\"matrix.var2\":\"3\"," +
                    "\"matrix.var3\":\"yyy\"},{\"matrix.os\":\"macos\",\"matrix.var1\":\"c\",\"matrix.var2\":\"1\"," +
                    "\"matrix.var3\":\"zzz\"},{\"matrix.os\":\"macos\",\"matrix.var1\":\"c\",\"matrix.var2\":\"2\"," +
                    "\"matrix.var3\":\"zzz\"},{\"matrix.os\":\"macos\",\"matrix.var1\":\"c\",\"matrix.var2\":\"3\"," +
                    "\"matrix.var3\":\"zzz\"},{\"matrix.os\":\"docker\",\"matrix.var1\":\"d\",\"matrix.var2\":\"0\"}]\n"
            ),
            contextCase
        )
    }

    @Test
    fun calculateValueMatrixYaml2() {
        val matrixControlOption = MatrixControlOption(
            strategyStr = """
---
os:
- "docker"
- "macos"
var1:
- "a"
- "b"
- "c"
var2:
- 1
- 2
- 3
                """,
            includeCaseStr = YamlUtil.toYaml(
                listOf(
                    // +1 os = docker, var1 = d, var2 = 0 不存在，直接计入
                    mapOf(
                        "os" to "docker",
                        "var1" to "d",
                        "var2" to "0"
                    ),
                    // +0 符合 os = macos, var1 = a, var2 = 1 增加 var3 = xxx
                    mapOf(
                        "os" to "macos",
                        "var1" to "a",
                        "var2" to "1",
                        "var3" to "xxx"
                    ),
                    // +0 符合 os = macos, var1 = b 的增加 var3 = yyy
                    mapOf(
                        "os" to "macos",
                        "var1" to "b",
                        "var3" to "yyy"
                    ),
                    // +0 符合 var1 = c 的增加 var3 = zzz
                    mapOf(
                        "var1" to "c",
                        "var3" to "zzz"
                    ),
                    // +1 符合 var1 = c 的增加 var3 = zzz
                    mapOf(
                        "var1" to "c",
                        "os" to "docker",
                        "var2" to "222"
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
            totalCount = 10,
            finishCount = 1,
            fastKill = true,
            maxConcurrency = 50
        )
        val contextCase = matrixControlOption.convertMatrixConfig(emptyMap()).getAllCombinations()
        println(contextCase.size)
        contextCase.forEachIndexed { index, map ->
            println("$index: $map")
        }
        Assertions.assertEquals(19, contextCase.size)
    }

    @Test
    fun calculateValueMatrix3() {
        val matrixControlOption = MatrixControlOption(
            // 2*3*3 = 18
            strategyStr = "\${{ fromJSON(depends.job1.outputs.matrix) }}",
            includeCaseStr = YamlUtil.toYaml(
                listOf(
                    // +0 符合 os = macos, var1 = b 的增加 var3 = yyy
                    mapOf(
                        "service" to "api",
                        "var1" to "b",
                        "var3" to "yyy"
                    ),
                    // +1 不存在 service = c, cpu = zzz ，直接计入新组合
                    mapOf(
                        "service" to "c",
                        "cpu" to "zzz"
                    )
                )
            ),
            // -1
            excludeCaseStr = YamlUtil.toYaml(
                listOf(
                    mapOf(
                        "service" to "project",
                        "cpu" to "arm64"
                    )
                )
            ),
            totalCount = 10,
            finishCount = 1,
            fastKill = true,
            maxConcurrency = 50
        )
        val buildContext = mapOf(
            "depends.job1.outputs.matrix" to """
            {"service":["api","project","gateway"],"cpu":["amd64", "arm64"]}
        """
        )
        val contextCase = matrixControlOption.convertMatrixConfig(buildContext).getAllCombinations()
        println(contextCase.size)
        contextCase.forEachIndexed { index, map ->
            println("$index: $map")
        }
        Assertions.assertEquals(6, contextCase.size)
    }

    @Test
    fun calculateValueMatrix8() {
        val matrixControlOption = MatrixControlOption(
            // 2*3*3 = 18
            strategyStr = null,
            includeCaseStr = "\${{ fromJSON(depends.job1.outputs.matrix_include) }}",
            // -1
            excludeCaseStr = null,
            totalCount = 10,
            finishCount = 1,
            fastKill = true,
            maxConcurrency = 50
        )
        val buildContext = mapOf(
            "depends.job1.outputs.matrix" to """
            {"service": "${'$'}{{ fromJSON(depends.job1.outputs.service) }}","cpu":"${'$'}{{ fromJSON(depends.job1.outputs.cpu) }}"}
        """,
            "depends.job1.outputs.matrix1" to JsonUtil.toJson(
                mapOf(
                    "service" to "\${{ fromJSON(depends.job1.outputs.service) }}",
                    "cpu" to "\${{ fromJSON(depends.job1.outputs.cpu) }}"
                )
            ),
            "depends.job1.outputs.matrix_include" to """
                [{"service":"api","var1":"b","var3":"yyy"},{"service":"c","cpu":"zzz"}]
            """,
            "depends.job1.outputs.matrix_exclude" to """
                [{"service":"project","cpu":"arm64"}]
            """,
            "depends.job1.outputs.service" to """
                ["api","project","gateway"]
            """.trim(),
            "depends.job1.outputs.cpu" to """
                ["amd64", "arm64"]
            """.trim()
        )
        val contextCase = matrixControlOption.convertMatrixConfig(buildContext).getAllCombinations()
        println(contextCase.size)
        contextCase.forEachIndexed { index, map ->
            println("$index: $map")
        }
        Assertions.assertEquals(2, contextCase.size)
    }

    @Test
    fun calculateValueMatrix7() {
        val matrixControlOption = MatrixControlOption(
            // 2*3*3 = 18
            strategyStr = YamlUtil.toYaml(
                mapOf(
                    "service" to "\${{ fromJSON(depends.job1.outputs.service) }}",
                    "cpu" to listOf("amd64", "arm64")
                )
            ),
            includeCaseStr = "\${{ fromJSON(depends.job1.outputs.matrix_include) }}",
            // -1
            excludeCaseStr = "\${{ fromJSON(depends.job1.outputs.matrix_exclude) }}",
            totalCount = 10,
            finishCount = 1,
            fastKill = true,
            maxConcurrency = 50
        )
        val buildContext = mapOf(
            "depends.job1.outputs.matrix" to """
            {"service": "${'$'}{{ fromJSON(depends.job1.outputs.service) }}","cpu":"${'$'}{{ fromJSON(depends.job1.outputs.cpu) }}"}
        """,
            "depends.job1.outputs.matrix1" to JsonUtil.toJson(
                mapOf(
                    "service" to "\${{ fromJSON(depends.job1.outputs.service) }}",
                    "cpu" to "\${{ fromJSON(depends.job1.outputs.cpu) }}"
                )
            ),
            // +0 service = api 时追加 var1 = b, var3 = yyy
            // +1 service = c, cpu = zzz 同key全都不匹配，增加组合
            "depends.job1.outputs.matrix_include" to """
                [{"service":"api","var1":"b","var3":"yyy"},{"service":"c","cpu":"zzz"}]
            """,
            // -1 排除一种情况
            "depends.job1.outputs.matrix_exclude" to """
                [{"service":"project","cpu":"arm64"}]
            """,
            "depends.job1.outputs.service" to """
                ["api","project","gateway"]
            """.trim(),
            "depends.job1.outputs.cpu" to """
                ["amd64", "arm64"]
            """.trim()
        )
        val contextCase = matrixControlOption.convertMatrixConfig(buildContext).getAllCombinations()
        println(contextCase.size)
        contextCase.forEachIndexed { index, map ->
            println("$index: $map")
        }
        Assertions.assertEquals(6, contextCase.size)
    }

    @Test
    fun calculateValueMatrix6() {
        val matrixControlOption = MatrixControlOption(
            // 2*3*3 = 18
            strategyStr = """---
service: ${'$'}{{ fromJSON(depends.job1.outputs.service) }}
cpu: ${'$'}{{ fromJSON(depends.job1.outputs.cpu) }}""",
            includeCaseStr = "\${{ fromJSON(depends.job1.outputs.matrix_include) }}",
            // -1
            excludeCaseStr = "\${{ fromJSON(depends.job1.outputs.matrix_exclude) }}",
            totalCount = 10,
            finishCount = 1,
            fastKill = true,
            maxConcurrency = 50
        )
        val buildContext = mapOf(
            "depends.job1.outputs.matrix" to """
            {"service": "${'$'}{{ fromJSON(depends.job1.outputs.service) }}","cpu":"${'$'}{{ fromJSON(depends.job1.outputs.cpu) }}"}
        """,
            "depends.job1.outputs.matrix1" to JsonUtil.toJson(
                mapOf(
                    "service" to "\${{ fromJSON(depends.job1.outputs.service) }}",
                    "cpu" to "\${{ fromJSON(depends.job1.outputs.cpu) }}"
                )
            ),
            // +0 service = api 时追加 var1 = b, var3 = yyy
            // +1 service = c, cpu = zzz 同key全都不匹配，增加组合
            "depends.job1.outputs.matrix_include" to """
                [{"service":"api","var1":"b","var3":"yyy"},{"service":"c","cpu":"zzz"}]
            """,
            // -1 排除一种情况
            "depends.job1.outputs.matrix_exclude" to """
                [{"service":"project","cpu":"arm64"}]
            """,
            "depends.job1.outputs.service" to """
                ["api","project","gateway"]
            """.trim(),
            "depends.job1.outputs.cpu" to """
                ["amd64", "arm64"]
            """.trim()
        )
        val contextCase = matrixControlOption.convertMatrixConfig(buildContext).getAllCombinations()
        println(contextCase.size)
        contextCase.forEachIndexed { index, map ->
            println("$index: $map")
        }
        Assertions.assertEquals(6, contextCase.size)
    }

    @Test
    fun calculateValueMatrix5() {
        val matrixControlOption = MatrixControlOption(
            // 2*3*3 = 18
            strategyStr = YamlUtil.toYaml(
                mapOf(
                    "service" to "\${{ fromJSON(depends.job1.outputs.service) }}",
                    "cpu" to "\${{ fromJSON(depends.job1.outputs.cpu) }}"
                )
            ),
            includeCaseStr = "\${{ fromJSON(depends.job1.outputs.matrix_include) }}",
            // -1
            excludeCaseStr = "\${{ fromJSON(depends.job1.outputs.matrix_exclude) }}",
            totalCount = 10,
            finishCount = 1,
            fastKill = true,
            maxConcurrency = 50
        )
        val buildContext = mapOf(
            "depends.job1.outputs.matrix" to """
            {"service": "${'$'}{{ fromJSON(depends.job1.outputs.service) }}","cpu":"${'$'}{{ fromJSON(depends.job1.outputs.cpu) }}"}
        """,
            "depends.job1.outputs.matrix1" to JsonUtil.toJson(
                mapOf(
                    "service" to "\${{ fromJSON(depends.job1.outputs.service) }}",
                    "cpu" to "\${{ fromJSON(depends.job1.outputs.cpu) }}"
                )
            ),
            // +0 service = api 时追加 var1 = b, var3 = yyy
            // +1 service = c, cpu = zzz 同key全都不匹配，增加组合
            "depends.job1.outputs.matrix_include" to """
                [{"service":"api","var1":"b","var3":"yyy"},{"service":"c","cpu":"zzz"}]
            """,
            // -1 排除一种情况
            "depends.job1.outputs.matrix_exclude" to """
                [{"service":"project","cpu":"arm64"}]
            """,
            "depends.job1.outputs.service" to """
                ["api","project","gateway"]
            """.trim(),
            "depends.job1.outputs.cpu" to """
                ["amd64", "arm64"]
            """.trim()
        )
        val contextCase = matrixControlOption.convertMatrixConfig(buildContext).getAllCombinations()
        println(contextCase.size)
        contextCase.forEachIndexed { index, map ->
            println("$index: $map")
        }
        Assertions.assertEquals(6, contextCase.size)
    }

    @Test
    fun calculateValueMatrix4() {
        val matrixControlOption = MatrixControlOption(
            // 2*3*3 = 18
            strategyStr = "\${{ fromJSON(depends.job1.outputs.matrix) }}",
            includeCaseStr = YamlUtil.toYaml(
                listOf(
                    // +0 符合 os = macos, var1 = b 的增加 var3 = yyy
                    mapOf(
                        "service" to "api",
                        "var1" to "b",
                        "var3" to "yyy"
                    ),
                    // +0 符合 var1 = c 的增加 var3 = zzz
                    mapOf(
                        "service" to "c",
                        "cpu" to "zzz"
                    )
                )
            ),
            // -1
            excludeCaseStr = YamlUtil.toYaml(
                listOf(
                    mapOf(
                        "service" to "project",
                        "cpu" to "arm64"
                    )
                )
            ),
            totalCount = 10,
            finishCount = 1,
            fastKill = true,
            maxConcurrency = 50
        )
        // +1 service = gateway, cpu = m1max 无法完全匹配
        // -1 排除掉一种情况
        val buildContext = mapOf(
            "depends.job1.outputs.matrix" to """
            {
    "service": [
        "api",
        "project",
        "gateway"
    ],
    "cpu": [
        "amd64",
        "arm64"
    ],
    "include": [
        {
            "service": "gateway",
            "cpu": "m1max"
        }
    ],
    "exclude": [
        {
            "service": "gateway",
            "cpu": "arm64"
        }
    ]
}
        """
        )
        val contextCase = matrixControlOption.convertMatrixConfig(buildContext).getAllCombinations()
        println(contextCase.size)
        contextCase.forEachIndexed { index, map ->
            println("$index: $map")
        }
        Assertions.assertEquals(6, contextCase.size)
    }

    @Test
    fun calculateValueMatrixMaxSizeTest() {
        val matrixControlOption = MatrixControlOption(
            // 4 * 4 * 4 * 4 = 256
            strategyStr = """
                    os: [docker,macos,os1,os2]
                    var1: [a,b,c,d]
                    var2: [1,2,3,4]
                    var3: [Q,W,E,R]
                """,
            includeCaseStr = YamlUtil.toYaml(
                listOf(
                    // +0 var2 = 1 的全部增加 var4 = a
                    mapOf(
                        "var4" to "a",
                        "var2" to "1"
                    )
                )
            ),
            excludeCaseStr = YamlUtil.toYaml(
                listOf(
                    // -0 并没有完全匹配项
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
        val contextCase = matrixControlOption.convertMatrixConfig(emptyMap()).getAllCombinations()
        println(contextCase.size)
        contextCase.forEachIndexed { index, map ->
            println("$index: $map")
        }
        Assertions.assertEquals(256, contextCase.size)
    }

    @Test
    fun calculateValueMatrixMinSizeTest1() {
        val matrixControlOption = MatrixControlOption(
            strategyStr = """
                    os: [docker]
                """,
            includeCaseStr = YamlUtil.toYaml(
                listOf(
                    // +0 追加 var1 var2
                    mapOf(
                        "os" to "docker",
                        "var1" to "a",
                        "var2" to "1"
                    ),
                    // 追加 var1
                    mapOf(
                        "os" to "docker",
                        "var1" to "b"
                    )
                )
            ),
            excludeCaseStr = YamlUtil.toYaml(
                listOf(
                    // -1 先include再exclude，能匹配
                    mapOf(
                        "os" to "docker",
                        "var1" to "b",
                        "var2" to "1"
                    )
                )
            ),
            totalCount = 10, // 3*3 + 2 - 1
            finishCount = 1,
            fastKill = true,
            maxConcurrency = 50
        )
        val contextCase = matrixControlOption.convertMatrixConfig(emptyMap()).getAllCombinations()
        println(contextCase.size)
        contextCase.forEachIndexed { index, map ->
            println("$index: $map")
        }
        Assertions.assertEquals(0, contextCase.size)
    }

    @Test
    fun calculateValueMatrixMinSizeTest2() {
        val matrixControlOption = MatrixControlOption(
            strategyStr = """
                    os: [docker]
                """,
            includeCaseStr = YamlUtil.toYaml(
                listOf(
                    mapOf(
                        "var1" to "b",
                        "var2" to "1"
                    ),
                    mapOf(
                        "os" to "docker",
                        "var1" to "b"
                    )
                )
            ),
            excludeCaseStr = YamlUtil.toYaml(
                listOf(
                    // -0 不匹配
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
        val contextCase = matrixControlOption.convertMatrixConfig(emptyMap()).getAllCombinations()
        println(contextCase.size)
        contextCase.forEachIndexed { index, map ->
            println("$index: $map")
        }
        Assertions.assertEquals(JsonUtil.to("[{\"matrix.os\":\"docker\",\"matrix.var1\":\"b\"}]"), contextCase)
    }

    @Test
    fun calculateValueMatrixMinSizeTest3() {
        val matrixControlOption = MatrixControlOption(
            strategyStr = """
---
os:
- a
                """,
            includeCaseStr = YamlUtil.toYaml(
                listOf(
                    mapOf(
                        "os" to "docker",
                        "var1" to "a",
                        "var2" to "1"
                    )
                )
            ),
            excludeCaseStr = YamlUtil.toYaml(
                listOf(
                    // -0 先include再exclude
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
        val contextCase = matrixControlOption.convertMatrixConfig(emptyMap()).getAllCombinations()
        println(contextCase.size)
        contextCase.forEachIndexed { index, map ->
            println("$index: $map")
        }
        Assertions.assertEquals(1, contextCase.size)
    }

    @Test
    fun calculateValueMatrixJson() {
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
                        ]
                    }
                """,
            includeCaseStr = "",
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
        val contextCase = matrixControlOption.convertMatrixConfig(emptyMap()).getAllCombinations()
        println(contextCase.size)
        contextCase.forEachIndexed { index, map ->
            println("$index: $map")
        }
        Assertions.assertEquals(2, contextCase.size)
    }

    @Test
    fun calculateValueMatrixJson2() {
        val matrixControlOption = MatrixControlOption(
            // 2*3*3 = 18
            strategyStr = """
                    {
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
                        ]
                    }
                """,
            includeCaseStr = YamlUtil.toYaml(
                listOf(
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
        val contextCase = matrixControlOption.convertMatrixConfig(emptyMap()).getAllCombinations()
        println(contextCase.size)
        contextCase.forEachIndexed { index, map ->
            println("$index: $map")
        }
        Assertions.assertEquals(0, contextCase.size)
    }

    @Test
    fun calculateValueMatrixJson3() {
        val matrixControlOption = MatrixControlOption(
            // 2*3*3 = 18
            strategyStr = """
var1: ['OFF', 'ON']
var2: [ { a: '1', b: '2' }, { a: '3', b: '4' } ]
                """,
            totalCount = 10, // 3*3 + 2 - 1
            finishCount = 1,
            fastKill = true,
            maxConcurrency = 50
        )
        val contextCase = matrixControlOption.convertMatrixConfig(emptyMap()).getAllCombinations()
        println(contextCase.size)
        contextCase.forEachIndexed { index, map ->
            println("$index: $map")
        }
        Assertions.assertEquals(4, contextCase.size)
    }
}

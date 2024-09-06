package com.tencent.devops.process.util

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.MatrixPipelineInfo
import com.tencent.devops.common.pipeline.utils.MatrixContextUtils
import com.tencent.devops.common.pipeline.utils.MatrixYamlCheckUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class MatrixYamlCheckUtilsTest {

    @Test
    fun checkYaml1() {
        val yamlstr = MatrixPipelineInfo(
            include = """
                - a: s
                  b: 2
                - a: 2
                  b: 4
        """,
            exclude = """
                - a: 1
                  b: 2
                - a: 2
                  b: 4
            """,
            strategy = "\${{fromJSON(asd)}}"
        )
        val result = MatrixYamlCheckUtils.checkYaml(yamlstr)
        Assertions.assertTrue(result.include == null)
        Assertions.assertTrue(result.exclude == null)
        print(result.strategy)
        Assertions.assertTrue(result.strategy == null)
    }

    @Test
    fun checkYaml2() {
        val yamlstr = MatrixPipelineInfo(
            include = """
                - a: s
                  b: 2
                - a: 2
                  b: 4
        """,
            exclude = """
                - a: 1
                  b: 2
                - a: 2
                  b: 4
            """,
            strategy = "\${{fromJSONasd(asd)}}"
        )
        Assertions.assertThrowsExactly(
            InvalidParamException::class.java
        ) {
            MatrixYamlCheckUtils.checkYaml(yamlstr)
        }
    }

    @Test
    fun checkYaml3() {
        val yamlstr = MatrixPipelineInfo(
            include = """
                - a: s
                  b: 2
                - a: 2
                  b: 4
        """,
            exclude = """
                - a: 1
                  b: 2
                - a: 2
                  b: 4
            """,
            strategy = """
                    os: {"ads" : "asd"}
                    var1: [a,b,c]
                    var2: [1,2,3]
                """
        )
        Assertions.assertThrowsExactly(
            InvalidParamException::class.java
        ) {
            MatrixYamlCheckUtils.checkYaml(yamlstr)
        }
    }

    @Test
    fun checkYaml4() {
        val yamlstr = MatrixPipelineInfo(
            include = """
                - a: s
                  b: 2
                - a: 2
                  b: 4
        """,
            exclude = """
                - a: 1
                  b: 2
                - a: 2
                  b: 4
            """,
            strategy = """
                    os: asd
                    var1: [a,b,c]
                    var2: [1,2,3]
                """
        )
        Assertions.assertThrowsExactly(
            InvalidParamException::class.java
        ) {
            MatrixYamlCheckUtils.checkYaml(yamlstr)
        }
    }

    @Test
    fun checkYaml5() {
        val yamlstr = JsonUtil.toJson(
            mapOf(
                "strategy" to "\${{fromJSONasd(asd)}}"
            )
        )
        val result = try {
            MatrixContextUtils.schemaCheck(yamlstr)
            false
        } catch (e: Exception) {
            true
        }

        Assertions.assertTrue(result)
    }

    @Test
    fun checkYaml6() {
        val yamlstr = JsonUtil.toJson(
            mapOf(
                "strategy" to "\${{fromJSON(asd)}}"
            )
        )
        MatrixContextUtils.schemaCheck(yamlstr)
    }

    @Test
    fun checkYaml7() {
        val yamlstr = MatrixPipelineInfo(
            include = """
                - a: s
                  b: 2
                - a: 2
                  b: 4
        """,
            exclude = null,
            strategy = null
        )
        val result = MatrixYamlCheckUtils.checkYaml(yamlstr)
        Assertions.assertTrue(result.include == null)
        Assertions.assertTrue(result.exclude == null)
        Assertions.assertTrue(result.strategy == null)
    }

    @Test
    fun checkYaml8() {
        val yamlstr = MatrixPipelineInfo(
            include = """
                ${'$'}{{fromJSON(xxx)}}
        """,
            exclude = null,
            strategy = null
        )
        val result = MatrixYamlCheckUtils.checkYaml(yamlstr)
        Assertions.assertTrue(result.include == null)
        Assertions.assertTrue(result.exclude == null)
        Assertions.assertTrue(result.strategy == null)
    }

    @Test
    fun checkYaml9() {
        val yamlstr = MatrixPipelineInfo(
            include = "\${{ fromJSON(FTP_DEPLOY_MODULE_LIST) }}",
            exclude = "",
            strategy = "\${{ fromJSON(FTP_DEPLOY_MODULE_NAMES) }}"
        )
        /*测试并发*/
        List(1000) { it }.parallelStream().forEach {
            MatrixYamlCheckUtils.checkYaml(yamlstr)
        }
    }
}

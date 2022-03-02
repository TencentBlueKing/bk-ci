package com.tencent.devops.process.util

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.MatrixPipelineInfo
import com.tencent.devops.common.pipeline.utils.MatrixContextUtils
import com.tencent.devops.common.pipeline.utils.MatrixYamlCheckUtils
import org.junit.Assert
import org.junit.Test

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
        Assert.assertTrue(result.include == null)
        Assert.assertTrue(result.exclude == null)
        print(result.strategy)
        Assert.assertTrue(result.strategy == null)
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
        val result = MatrixYamlCheckUtils.checkYaml(yamlstr)
        Assert.assertTrue(result.include == null)
        Assert.assertTrue(result.exclude == null)
        Assert.assertTrue(result.strategy != null)
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
        val result = MatrixYamlCheckUtils.checkYaml(yamlstr)
        Assert.assertTrue(result.include == null)
        Assert.assertTrue(result.exclude == null)
        Assert.assertTrue(result.strategy != null)
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
        val result = MatrixYamlCheckUtils.checkYaml(yamlstr)
        Assert.assertTrue(result.include == null)
        Assert.assertTrue(result.exclude == null)
        Assert.assertTrue(result.strategy != null)
    }

    @Test
    fun checkYaml5() {
        val yamlstr = JsonUtil.toJson(mapOf(
            "strategy" to "\${{fromJSONasd(asd)}}"
        ))
        val result = try {
            MatrixContextUtils.schemaCheck(yamlstr)
            false
        } catch (e: Exception) {
            true
        }

        Assert.assertTrue(result)
    }

    @Test
    fun checkYaml6() {
        val yamlstr = JsonUtil.toJson(mapOf(
            "strategy" to "\${{fromJSON(asd)}}"
        ))
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
        Assert.assertTrue(result.include == null)
        Assert.assertTrue(result.exclude == null)
        Assert.assertTrue(result.strategy == null)
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
        Assert.assertTrue(result.include == null)
        Assert.assertTrue(result.exclude == null)
        Assert.assertTrue(result.strategy == null)
    }
}

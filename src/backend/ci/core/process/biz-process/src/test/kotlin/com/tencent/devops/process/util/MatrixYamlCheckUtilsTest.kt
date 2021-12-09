package com.tencent.devops.process.util

import com.tencent.devops.common.pipeline.pojo.MatrixPipelineInfo
import com.tencent.devops.common.pipeline.utils.MatrixYamlCheckUtils
import org.junit.Test
import org.junit.Assert

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
                    os: [docker,macos]
                    var1: [a,b,c]
                    var2: [1,2,3]
                """
        )
        val result = MatrixYamlCheckUtils.checkYaml(yamlstr)
        Assert.assertTrue(result.include == null)
        Assert.assertTrue(result.exclude == null)
        Assert.assertTrue(result.strategy == null)
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
}
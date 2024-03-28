package com.tencent.devops.process.yaml.transfer

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.core.io.ClassPathResource
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class MergeYamlTest {
    private fun yamlPath(p: String) = "TransferYaml/$p"

    private fun getStrFromResource(testYaml: String): String {
        val classPathResource = ClassPathResource(testYaml)
        val inputStream: InputStream = classPathResource.inputStream
        val isReader = InputStreamReader(inputStream)

        val reader = BufferedReader(isReader)
        val sb = StringBuffer()
        var str: String?
        while (reader.readLine().also { str = it } != null) {
            sb.append(str).append("\n")
        }
        inputStream.close()
        return sb.toString()
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "base-1",
            "base-2",
            "base-3"
        ]
    )
    fun base(value: String) {
        val new = getStrFromResource(yamlPath("$value/new.yml"))
        val old = getStrFromResource(yamlPath("$value/old.yml"))
        val out = getStrFromResource(yamlPath("$value/out.yml"))

        val m = TransferMapper.mergeYaml(old, new)
        Assertions.assertEquals(m, out)
    }
}

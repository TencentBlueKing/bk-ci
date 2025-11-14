package com.tencent.devops.process.yaml.transfer

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.yaml.transfer.schema.CodeSchemaCheck
import io.mockk.mockk
import java.io.BufferedReader
import java.io.InputStreamReader
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.core.io.ClassPathResource

class CodeSchemaCheckTest {

    private lateinit var redisOperation: RedisOperation
    private lateinit var codeSchemaCheck: CodeSchemaCheck

    @BeforeEach
    fun setUp() {
        redisOperation = mockk(relaxed = true)
        codeSchemaCheck = CodeSchemaCheck(redisOperation)
    }

    @Test
    fun `test check with valid v3_0 yaml should pass`() {
        // 准备测试数据 - 从资源文件读取有效的 v3.0 YAML
        val validYaml = readResourceFile("test-yamls/valid-v3.yml")

        // 执行测试 - 不应该抛出异常
        Assertions.assertDoesNotThrow {
            codeSchemaCheck.check(validYaml)
        }
    }

    @Test
    fun `test check with invalid yaml syntax should throw exception`() {
        // 准备测试数据 - 从资源文件读取无效的 YAML 语法
        val invalidYaml = readResourceFile("test-yamls/invalid-syntax.yml")

        // 执行测试 - 应该抛出 PipelineTransferException
        val exception = assertThrows<PipelineTransferException> {
            codeSchemaCheck.check(invalidYaml)
        }

        println(exception)
        // 验证异常信息
        Assertions.assertTrue(
            exception.params?.firstOrNull()?.contains(
                "There may be a problem with your yaml syntax"
            ) == true
        )
    }

    @Test
    fun `test check with unsupported version should throw exception`() {
        // 准备测试数据 - 从资源文件读取不支持的版本
        val unsupportedVersionYaml = readResourceFile("test-yamls/unsupported-version.yml")

        // 执行测试 - 应该抛出 PipelineTransferException
        val exception = assertThrows<PipelineTransferException> {
            codeSchemaCheck.check(unsupportedVersionYaml)
        }

        println(exception)
        // 验证异常信息
        Assertions.assertTrue(
            exception.params?.firstOrNull()?.contains(
                "yaml version(v1.0) not valid, only support v3.0"
            ) == true
        )
    }

    @Test
    fun `test check with missing version should throw exception`() {
        // 准备测试数据 - 从资源文件读取缺少版本信息的YAML
        val noVersionYaml = readResourceFile("test-yamls/no-version.yml")

        // 执行测试 - 应该抛出 PipelineTransferException
        val exception = assertThrows<PipelineTransferException> {
            codeSchemaCheck.check(noVersionYaml)
        }

        // 验证异常信息
        Assertions.assertTrue(
            exception.params?.firstOrNull()?.contains(
                "yaml version(null) not valid, only support v3.0"
            ) == true
        )
    }

    @Test
    fun `test check with empty yaml should throw exception`() {
        // 准备测试数据 - 空的 YAML
        val emptyYaml = ""

        // 执行测试 - 应该抛出 PipelineTransferException
        assertThrows<PipelineTransferException> {
            codeSchemaCheck.check(emptyYaml)
        }
    }

    @Test
    fun `test check with null yaml should throw exception`() {
        // 执行测试 - 应该抛出异常（可能是 NullPointerException 或 PipelineTransferException）
        assertThrows<Exception> {
            codeSchemaCheck.check(null as String)
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "TransferYaml/base-3/new.yml",
            "TransferYaml/base-3/old.yml"
        ]
    )
    fun `test check with valid yaml files from resources`(yamlFile: String) {
        // 从资源文件读取 YAML 内容
        val yamlContent = readResourceFile(yamlFile)

        // 执行测试 - 不应该抛出异常
        Assertions.assertDoesNotThrow {
            codeSchemaCheck.check(yamlContent)
        }
    }

    @Test
    fun `test check with complex valid yaml should pass`() {
        // 准备测试数据 - 从资源文件读取复杂的有效 YAML
        val complexYaml = readResourceFile("test-yamls/complex-valid.yml")

        // 执行测试 - 不应该抛出异常
        Assertions.assertDoesNotThrow {
            codeSchemaCheck.check(complexYaml)
        }
    }

    /**
     * 从资源文件读取内容
     */
    private fun readResourceFile(resourcePath: String): String {
        val resource = ClassPathResource(resourcePath)
        return resource.inputStream.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.readText()
            }
        }
    }
}

package com.tencent.devops.prebuild.component

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.prebuild.ServiceBaseTest
import com.tencent.devops.prebuild.v2.component.PreCIYAMLValidator
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class PreCIYAMLValidatorTest : ServiceBaseTest() {

    @InjectMocks
    lateinit var preCIYAMLValidator: PreCIYAMLValidator

    @Test
    @DisplayName("")
    fun testValidate() {
        // 正确的格式
        assertTrue(preCIYAMLValidator.validate(getYamlForLocal()).first)
        assertTrue(preCIYAMLValidator.validate(getYamlForDevCloud()).first)
        assertTrue(preCIYAMLValidator.validate(getYamlForDockerVM()).first)
        assertTrue(preCIYAMLValidator.validate(getYamlForAgentLess()).first)

        // 非法格式测试
        assertFalse(preCIYAMLValidator.validate(getYamlForInvalidDispatchType()).first)
        assertThrows<CustomException> { preCIYAMLValidator.validate(getYamlForCheckEntendsBiz()) }
    }
}
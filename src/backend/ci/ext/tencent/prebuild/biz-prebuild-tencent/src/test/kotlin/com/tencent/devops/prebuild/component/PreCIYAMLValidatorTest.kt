package com.tencent.devops.prebuild.component

import com.tencent.devops.prebuild.ServiceBaseTest
import com.tencent.devops.prebuild.v2.component.PreCIYAMLValidator
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
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
        preCIYAMLValidator.validate("")
    }
}
package com.tencent.devops.process.service.template

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateCommonCondition
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class PipelineTemplateTest {
    @Test
    fun testPipelineTemplateCommonCondition() {
        try {
            val pipelineTemplateCommonCondition = PipelineTemplateCommonCondition()
            pipelineTemplateCommonCondition.checkAllFieldsAreNull()
        } catch (e: Throwable) {
            Assertions.assertThrows(ErrorCodeException::class.java) { throw e }
        }
    }
}

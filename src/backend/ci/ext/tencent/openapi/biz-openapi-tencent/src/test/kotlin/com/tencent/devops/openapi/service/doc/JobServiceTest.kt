package com.tencent.devops.openapi.service.doc

import com.tencent.devops.environment.service.job.ScriptExecuteService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [ScriptExecuteService::class])
class JobServiceTest @Autowired constructor(
    private val scriptExecuteService: ScriptExecuteService
) {
    @Test
    fun executeScriptTest() = { ->
        // TODO：mock test
    }
}
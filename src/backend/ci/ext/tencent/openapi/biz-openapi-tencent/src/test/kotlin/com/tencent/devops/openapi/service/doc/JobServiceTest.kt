package com.tencent.devops.openapi.service.doc

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.job.ExecuteTarget
import com.tencent.devops.environment.pojo.job.Host
import com.tencent.devops.environment.pojo.job.ScriptExecuteReq
import com.tencent.devops.environment.pojo.job.ScriptExecuteResult
import com.tencent.devops.environment.service.job.ScriptExecuteService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [ScriptExecuteService::class])
class JobServiceTest @Autowired constructor(
    private val scriptExecuteService: ScriptExecuteService
) {
    @Test
    fun executeScriptTest() = {
        ->
        val host: Host = Host(bkCloudId = 0, ip = "9.146.98.105", bkHostId = null)
        val executeTarget: ExecuteTarget = ExecuteTarget(listOf(""), listOf(""), listOf(host))
        val scriptExecuteReq: ScriptExecuteReq = ScriptExecuteReq(
            scriptContent = "ZWNobyAkMQ==",
            timeout = 1000,
            scriptParam = "aGVsbG8=",
            isSensiveParam = 0,
            scriptType = 1,
            executeTarget = executeTarget,
            account = "root",
        )
        val executeScriptResult = scriptExecuteService.executeScript("jianingzhao", "jianingzhao-test", scriptExecuteReq)
        println("[executeScriptTest] executeScriptResult：${executeScriptResult}")
    }
}
package com.tencent.devops.worker.common.utils

import com.tencent.devops.worker.common.task.script.ScriptEnvUtils
import java.io.File
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class BatScriptUtilTest {

    private val tmpDir = File(System.getProperty("java.io.tmpdir"))

    @Test
    fun formatMultipleLinesInjectedTest() {
        val buildId = "bat_multi_line_test"
        val script = "call:format_multiple_lines \"::set-output name=TEST::hello%%25world\""
        val workspace = File(tmpDir, "bat_multi_line_test_workspace")
        workspace.mkdirs()

        val file = BatScriptUtil.getCommandFile(
            buildId = buildId,
            script = script,
            runtimeVariables = emptyMap(),
            dir = workspace,
            workspace = workspace
        )

        val content = file.readText()
        /*标签函数被注入*/
        Assertions.assertTrue(content.contains(":format_multiple_lines"))
        /*PowerShell 编码逻辑被注入*/
        Assertions.assertTrue(content.contains("powershell"))
        Assertions.assertTrue(content.contains("Add-Content"))
        /*编码目标 %25、%0A、%0D 被注入*/
        Assertions.assertTrue(content.contains("%25"))
        Assertions.assertTrue(content.contains("%0A"))
        Assertions.assertTrue(content.contains("%0D"))
        /*占位符被替换为实际路径*/
        Assertions.assertFalse(content.contains("##multiLineFile##"))
        Assertions.assertTrue(content.contains(ScriptEnvUtils.getMultipleLineFile(buildId)))
        /*用户脚本被保留*/
        Assertions.assertTrue(content.contains(script))

        file.delete()
        workspace.deleteRecursively()
    }

    @Test
    fun formatMultipleLinesPlaceholderReplacedTest() {
        val buildId = "bat_placeholder_test"
        val workspace = File(tmpDir, "bat_placeholder_test_workspace")
        workspace.mkdirs()

        val file = BatScriptUtil.getCommandFile(
            buildId = buildId,
            script = "echo done",
            runtimeVariables = emptyMap(),
            dir = workspace,
            workspace = workspace
        )

        val content = file.readText()
        val expectedFileName = ScriptEnvUtils.getMultipleLineFile(buildId)
        /*占位符被替换*/
        Assertions.assertFalse(content.contains("##multiLineFile##"))
        /*文件名正确*/
        Assertions.assertTrue(content.contains(expectedFileName))

        file.delete()
        workspace.deleteRecursively()
    }

    @Test
    fun formatMultipleLinesWithoutCallTest() {
        val buildId = "bat_no_call_test"
        val workspace = File(tmpDir, "bat_no_call_test_workspace")
        workspace.mkdirs()

        val file = BatScriptUtil.getCommandFile(
            buildId = buildId,
            script = "echo hello",
            runtimeVariables = emptyMap(),
            dir = workspace,
            workspace = workspace
        )

        val content = file.readText()
        /*即使脚本不调用,标签函数也存在*/
        Assertions.assertTrue(content.contains(":format_multiple_lines"))
        /*::set-output 不在用户脚本中但标签函数定义中包含*/
        Assertions.assertTrue(content.contains("Add-Content"))

        file.delete()
        workspace.deleteRecursively()
    }
}

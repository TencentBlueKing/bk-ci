package com.tencent.devops.worker.common.utils

import com.tencent.devops.worker.common.task.script.ScriptEnvUtils
import com.tencent.devops.worker.common.task.script.ScriptTask
import java.io.File
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS

class ShellUtilTest {

    private val tmpDir = File(System.getProperty("java.io.tmpdir"))
    private val jobId = "job_xx"
    private val stepId = "step_xx"

    @Test
    fun formatMultipleLinesInjectedTest() {
        val buildId = "sh_multi_line_test"
        val script = "format_multiple_lines \"::set-output name=TEST::test_value\""
        val workspace = File(tmpDir, "sh_multi_line_test_workspace")
        workspace.deleteRecursively()
        workspace.mkdirs()

        val file = ShellUtil.getCommandFile(
            buildId = buildId,
            script = script,
            dir = workspace,
            buildEnvs = emptyList(),
            runtimeVariables = emptyMap(),
            workspace = workspace
        )

        val content = file.readText()
        /* bash 函数被注入 */
        Assertions.assertTrue(content.contains("format_multiple_lines()"))
        /* 编码目标 %25、%0D、%0A 被注入（与解码端格式一致） */
        Assertions.assertTrue(content.contains("%25"))
        Assertions.assertTrue(content.contains("%0D"))
        Assertions.assertTrue(content.contains("%0A"))
        /* 真实控制字符编码方式被注入（bash ANSI-C 引用） */
        Assertions.assertTrue(content.contains("\$'\\r'"))
        Assertions.assertTrue(content.contains("\$'\\n'"))
        /* 编码顺序：% 先于换行符（避免编码产物被二次命中） */
        Assertions.assertTrue(content.indexOf("%25") < content.indexOf("\$'\\r'"))
        /* 追加单行到 multiLine 文件 */
        Assertions.assertTrue(content.contains("printf '%s\\n'"))
        /* 占位符被替换为实际文件名 */
        Assertions.assertFalse(content.contains("##multiLineFile##"))
        Assertions.assertTrue(content.contains(ScriptEnvUtils.getMultipleLineFile(buildId)))
        /* 用户脚本写入独立文件并被主脚本 source */
        val match = Regex("""(?m)^\. (.+)$""").find(content)
        Assertions.assertTrue(match != null, "should source user script file")
        val userScriptFile = File(match!!.groupValues[1])
        Assertions.assertTrue(userScriptFile.exists(), "user script file should exist")
        Assertions.assertEquals(script, userScriptFile.readText())

        file.delete()
        workspace.deleteRecursively()
    }

    @Test
    fun formatMultipleLinesWithoutCallTest() {
        val buildId = "sh_no_call_test"
        val workspace = File(tmpDir, "sh_no_call_test_workspace")
        workspace.deleteRecursively()
        workspace.mkdirs()

        val file = ShellUtil.getCommandFile(
            buildId = buildId,
            script = "echo hello",
            dir = workspace,
            buildEnvs = emptyList(),
            runtimeVariables = emptyMap(),
            workspace = workspace
        )

        val content = file.readText()
        /* 即使脚本不调用，函数定义也存在 */
        Assertions.assertTrue(content.contains("format_multiple_lines()"))
        Assertions.assertFalse(content.contains("##multiLineFile##"))

        file.delete()
        workspace.deleteRecursively()
    }

    /**
     * 用 bash 真实执行生成的 .sh。
     * 输出重定向到文件后再 waitFor(timeout)：避免管道读阻塞导致超时保护失效。
     */
    private fun runSh(scriptFile: File, workspace: File): Pair<Int, String> {
        val consoleFile = File.createTempFile("sh_e2e_console_", ".log")
        consoleFile.deleteOnExit()
        val process = ProcessBuilder("bash", scriptFile.absolutePath)
            .directory(workspace)
            .redirectErrorStream(true)
            .redirectOutput(consoleFile)
            .start()
        val finished = process.waitFor(60, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
        }
        Assertions.assertTrue(finished, "bash 执行超时")
        return process.exitValue() to consoleFile.readText()
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    fun formatMultipleLinesEndToEndTest() {
        val buildId = "sh_e2e_multi"
        val workspace = File(tmpDir, "sh_e2e_multi_workspace")
        workspace.deleteRecursively()
        workspace.mkdirs()
        /* 覆盖：多行 / 百分号 / 字面 %0A / 中文 / CR / 多变量追加 */
        val result = "line1\n100% done\nliteral %0A here\n中文\r"
        val script = "format_multiple_lines \"::set-output name=RESULT::$result\"\n" +
            "format_multiple_lines \"::set-output name=COUNT::42\""

        val file = ShellUtil.getCommandFile(
            buildId = buildId,
            script = script,
            dir = workspace,
            buildEnvs = emptyList(),
            runtimeVariables = emptyMap(),
            workspace = workspace
        )

        val (exitCode, console) = runSh(file, workspace)
        Assertions.assertEquals(0, exitCode, console)

        val decoded = ScriptTask.decodeMultipleLines(
            lines = ScriptEnvUtils.getMultipleLines(buildId, workspace),
            jobId = jobId,
            stepId = stepId
        )
        /* 解码结果与原始字符串逐字符一致 */
        Assertions.assertEquals(result, decoded["jobs.$jobId.steps.$stepId.outputs.RESULT"])
        /* 同一脚本多次调用逐行追加，互不覆盖 */
        Assertions.assertEquals("42", decoded["jobs.$jobId.steps.$stepId.outputs.COUNT"])

        file.delete()
        workspace.deleteRecursively()
    }
}

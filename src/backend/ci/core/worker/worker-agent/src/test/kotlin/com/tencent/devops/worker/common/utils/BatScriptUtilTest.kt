package com.tencent.devops.worker.common.utils

import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.worker.common.task.script.ScriptEnvUtils
import com.tencent.devops.worker.common.task.script.ScriptTask
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.text.Charsets
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS

class BatScriptUtilTest {

    private val tmpDir = File(System.getProperty("java.io.tmpdir"))
    private val jobId = "job_xx"
    private val stepId = "step_xx"

    @Test
    fun formatMultipleLinesInjectedTest() {
        val buildId = "bat_multi_line_test"
        val script = "call:format_multiple_lines TEST \"test_value.txt\""
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
        /*PowerShell ReadAllText 文件读取被注入*/
        Assertions.assertTrue(content.contains("[System.IO.File]::ReadAllText"))
        Assertions.assertTrue(content.contains("AppendAllText"))
        /*编码目标 %25、%0A、%0D 被注入（与解码端格式一致）*/
        Assertions.assertTrue(content.contains("%25"))
        Assertions.assertTrue(content.contains("%0A"))
        Assertions.assertTrue(content.contains("%0D"))
        /*文件版不包含旧 arg 版特征（环境变量通道）*/
        Assertions.assertFalse(content.contains("set RAW="))
        Assertions.assertFalse(content.contains("\$env:RAW"))
        /*真实控制字符编码方式被注入*/
        Assertions.assertTrue(content.contains("[char]13"))
        Assertions.assertTrue(content.contains("[char]10"))
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
        Assertions.assertTrue(content.contains("AppendAllText"))

        file.delete()
        workspace.deleteRecursively()
    }

    @Test
    fun formatMultipleLinesLiteralBackslashSafetyTest() {
        /* 验证文件版中不含任何对字面 \n/\r 的正则替换，确保路径安全 */
        val buildId = "bat_literal_safety_test"
        val workspace = File(tmpDir, "bat_literal_safety_test_workspace")
        workspace.mkdirs()

        val file = BatScriptUtil.getCommandFile(
            buildId = buildId,
            script = "echo done",
            runtimeVariables = emptyMap(),
            dir = workspace,
            workspace = workspace
        )

        val content = file.readText()
        /* 不应包含任何对字面 \n 或 \r 的正则替换（arg 版曾用 \\\\n / \\\\r） */
        Assertions.assertFalse(content.contains("\\\\n"))
        Assertions.assertFalse(content.contains("\\\\r"))
        /* 应包含 [char] 编码（真实控制字符编码方式） */
        Assertions.assertTrue(content.contains("[char]13"))
        Assertions.assertTrue(content.contains("[char]10"))

        file.delete()
        workspace.deleteRecursively()
    }

    @Test
    fun preprocessMultilineBlockBasicTest() {
        /* 内联多行块：三行内容应被写入临时文件并替换为文件版调用 */
        val buildId = "bat_inline_test"
        val workspace = File(tmpDir, "bat_inline_test_workspace")
        workspace.mkdirs()

        val script = "call:format_multiple_lines CONFIG \"\n" +
            "[server]\n" +
            "host=0.0.0.0\n" +
            "port=8080\n" +
            "\""

        val file = BatScriptUtil.getCommandFile(
            buildId = buildId,
            script = script,
            runtimeVariables = emptyMap(),
            dir = workspace,
            workspace = workspace
        )

        val content = file.readText()
        /* 内联内容不应出现在生成的 bat 中（已写入临时文件） */
        Assertions.assertFalse(content.contains("[server]"))
        /* 应替换为文件版调用，且引用已存在的临时文件 */
        val regex = Regex("""call:format_multiple_lines CONFIG "([^"]+)"""")
        val match = regex.find(content)
        Assertions.assertTrue(match != null, "should generate file-based call")
        val blockFile = File(match!!.groupValues[1])
        Assertions.assertTrue(blockFile.exists(), "block temp file should exist")
        Assertions.assertEquals(
            "[server]\r\nhost=0.0.0.0\r\nport=8080",
            blockFile.readText(Charsets.UTF_8)
        )

        file.delete()
        deleteBlockFiles(buildId)
        workspace.deleteRecursively()
    }

    @Test
    fun preprocessMultilineBlockFileVersionUntouchedTest() {
        /* 文件版单行调用不应被预处理拦截，原样保留 */
        val buildId = "bat_inline_file_test"
        val workspace = File(tmpDir, "bat_inline_file_test_workspace")
        workspace.mkdirs()

        val script = "call:format_multiple_lines RESULT \"result.txt\""

        val file = BatScriptUtil.getCommandFile(
            buildId = buildId,
            script = script,
            runtimeVariables = emptyMap(),
            dir = workspace,
            workspace = workspace
        )

        val content = file.readText()
        Assertions.assertTrue(content.contains(script))

        file.delete()
        workspace.deleteRecursively()
    }

    @Test
    fun preprocessMultilineBlockMixedTest() {
        /* 文件版 + 内联块 + 普通命令混用，各自正确 */
        val buildId = "bat_inline_mixed_test"
        val workspace = File(tmpDir, "bat_inline_mixed_test_workspace")
        workspace.mkdirs()

        val script = "echo hello\n" +
            "call:format_multiple_lines RESULT \"result.txt\"\n" +
            "call:format_multiple_lines CONFIG \"\n" +
            "line1\n" +
            "line2\n" +
            "\"\n" +
            "echo done"

        val file = BatScriptUtil.getCommandFile(
            buildId = buildId,
            script = script,
            runtimeVariables = emptyMap(),
            dir = workspace,
            workspace = workspace
        )

        val content = file.readText()
        Assertions.assertTrue(content.contains("echo hello"))
        Assertions.assertTrue(content.contains("call:format_multiple_lines RESULT \"result.txt\""))
        Assertions.assertFalse(content.contains("line1"))
        Assertions.assertTrue(content.contains("echo done"))

        file.delete()
        deleteBlockFiles(buildId)
        workspace.deleteRecursively()
    }

    @Test
    fun preprocessMultilineBlockUnterminatedTest() {
        /* 缺少结束引号应抛出明确异常 */
        val buildId = "bat_inline_unterminated_test"
        val workspace = File(tmpDir, "bat_inline_unterminated_test_workspace")
        workspace.mkdirs()

        val script = "call:format_multiple_lines CONFIG \"\n" +
            "line1\n" +
            "line2"

        val exception = Assertions.assertThrows(TaskExecuteException::class.java) {
            BatScriptUtil.getCommandFile(
                buildId = buildId,
                script = script,
                runtimeVariables = emptyMap(),
                dir = workspace,
                workspace = workspace
            )
        }
        /* 错误信息经 i18n 渲染，跨语言统一断言：含变量名与起始行号（起始行号为 1） */
        Assertions.assertTrue(exception.message!!.contains("CONFIG"))
        Assertions.assertTrue(exception.message!!.contains("1"))

        workspace.deleteRecursively()
    }

    /**
     * 用 cmd.exe 真实执行生成的 .bat。
     * 输出重定向到文件后再 waitFor(timeout)：避免管道读阻塞导致超时保护失效。
     */
    private fun runBat(bat: File, workspace: File): Pair<Int, String> {
        val consoleFile = File.createTempFile("bat_e2e_console_", ".log")
        consoleFile.deleteOnExit()
        val process = ProcessBuilder("cmd.exe", "/C", bat.absolutePath)
            .directory(workspace)
            .redirectErrorStream(true)
            .redirectOutput(consoleFile)
            .start()
        val finished = process.waitFor(60, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
        }
        Assertions.assertTrue(finished, "bat 执行超时")
        return process.exitValue() to consoleFile.readText()
    }

    /** block 临时文件已写入 java.io.tmpdir，workspace 递归删除不再覆盖，需按前缀单独清理 */
    private fun deleteBlockFiles(buildId: String) {
        tmpDir.listFiles { f -> f.isFile && f.name.startsWith("ml_block_${buildId}_") }
            ?.forEach { it.delete() }
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    fun formatMultipleLinesFileEndToEndTest() {
        val buildId = "bat_e2e_file"
        /* 上一次断言失败会跳过清理留下旧 multiLine.log，先重建 workspace 避免读到陈旧值 */
        val workspace = File(tmpDir, "bat_e2e_file_workspace")
        workspace.deleteRecursively()
        workspace.mkdirs()
        /* 覆盖：多行 / 百分号 / 字面 %0A / 中文 / CRLF */
        val content = "line1\r\n100% done\r\nliteral %0A here\r\n中文"
        val blockFile = File(workspace, "result.txt").apply { writeText(content, Charsets.UTF_8) }

        val bat = BatScriptUtil.getCommandFile(
            buildId = buildId,
            script = "call:format_multiple_lines RESULT \"${blockFile.absolutePath}\"",
            runtimeVariables = emptyMap(),
            dir = workspace,
            workspace = workspace
        )

        val (exitCode, console) = runBat(bat, workspace)
        Assertions.assertEquals(0, exitCode, console)

        val decoded = ScriptTask.decodeMultipleLines(
            lines = ScriptEnvUtils.getMultipleLines(buildId, workspace),
            jobId = jobId,
            stepId = stepId
        )
        /* 解码结果与原始文件逐字节一致 */
        Assertions.assertEquals(content, decoded["jobs.$jobId.steps.$stepId.outputs.RESULT"])

        bat.delete()
        workspace.deleteRecursively()
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    fun formatMultipleLinesInlineBlockEndToEndTest() {
        val buildId = "bat_e2e_inline"
        val workspace = File(tmpDir, "bat_e2e_inline_workspace")
        workspace.deleteRecursively()
        workspace.mkdirs()
        /* script 用 \n（ScriptTask 传参前会 replace("\r","")），块内容由后端拼接为 CRLF */
        val script = "call:format_multiple_lines CONFIG \"\n[server]\nhost=0.0.0.0\nport=8080\n\""

        val bat = BatScriptUtil.getCommandFile(
            buildId = buildId,
            script = script,
            runtimeVariables = emptyMap(),
            dir = workspace,
            workspace = workspace
        )

        val (exitCode, console) = runBat(bat, workspace)
        Assertions.assertEquals(0, exitCode, console)

        val decoded = ScriptTask.decodeMultipleLines(
            lines = ScriptEnvUtils.getMultipleLines(buildId, workspace),
            jobId = jobId,
            stepId = stepId
        )
        Assertions.assertEquals(
            "[server]\r\nhost=0.0.0.0\r\nport=8080",
            decoded["jobs.$jobId.steps.$stepId.outputs.CONFIG"]
        )

        bat.delete()
        deleteBlockFiles(buildId)
        workspace.deleteRecursively()
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    fun formatMultipleLinesMissingFileFailsLoudlyTest() {
        val buildId = "bat_e2e_missing"
        val workspace = File(tmpDir, "bat_e2e_missing_workspace")
        workspace.deleteRecursively()
        workspace.mkdirs()
        val missing = File(workspace, "does_not_exist.txt").canonicalPath

        val bat = BatScriptUtil.getCommandFile(
            buildId = buildId,
            script = "call:format_multiple_lines RESULT \"$missing\"",
            runtimeVariables = emptyMap(),
            dir = workspace,
            workspace = workspace
        )

        val (exitCode, console) = runBat(bat, workspace)
        /* 契约：目标文件不存在必须让进程非 0 退出（响亮失败），不能静默丢弃 */
        Assertions.assertTrue(exitCode != 0, "expected non-zero exit code, got $exitCode. console: $console")

        bat.delete()
        workspace.deleteRecursively()
    }
}

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

    @Test
    fun formatMultipleLinesPosixInjectedTest() {
        val buildId = "sh_posix_inject_test"
        val workspace = File(tmpDir, "sh_posix_inject_test_workspace")
        workspace.deleteRecursively()
        workspace.mkdirs()

        val file = ShellUtil.getCommandFile(
            buildId = buildId,
            script = "#!/bin/sh\necho hi",
            dir = workspace,
            buildEnvs = emptyList(),
            runtimeVariables = emptyMap(),
            workspace = workspace
        )

        val content = file.readText()
        /* 保留用户 shebang */
        Assertions.assertTrue(content.contains("#!/bin/sh"))
        /* POSIX 分支：编码逻辑由 bash -c 承载 */
        Assertions.assertTrue(content.contains("bash -c"))
        /* 不出现 bash 版特有的 local 声明 */
        Assertions.assertFalse(content.contains("local content="))

        file.delete()
        workspace.deleteRecursively()
    }

    @Test
    fun formatMultipleLinesEnvShShebangInjectedTest() {
        val buildId = "sh_env_posix_inject_test"
        val workspace = File(tmpDir, "sh_env_posix_inject_test_workspace")
        workspace.deleteRecursively()
        workspace.mkdirs()

        val file = ShellUtil.getCommandFile(
            buildId = buildId,
            script = "#!/usr/bin/env sh\necho hi",
            dir = workspace,
            buildEnvs = emptyList(),
            runtimeVariables = emptyMap(),
            workspace = workspace
        )

        val content = file.readText()
        Assertions.assertTrue(content.contains("bash -c"))
        Assertions.assertFalse(content.contains("local content="))

        file.delete()
        workspace.deleteRecursively()
    }

    @Test
    fun formatMultipleLinesBashShebangInjectedTest() {
        val buildId = "sh_bash_inject_test"
        val workspace = File(tmpDir, "sh_bash_inject_test_workspace")
        workspace.deleteRecursively()
        workspace.mkdirs()

        val file = ShellUtil.getCommandFile(
            buildId = buildId,
            script = "#!/bin/bash\necho hi",
            dir = workspace,
            buildEnvs = emptyList(),
            runtimeVariables = emptyMap(),
            workspace = workspace
        )

        val content = file.readText()
        /* bash 系维持既有实现 */
        Assertions.assertTrue(content.contains("local content="))
        Assertions.assertFalse(content.contains("bash -c"))

        file.delete()
        workspace.deleteRecursively()
    }

    @Test
    fun formatMultipleLinesNoShebangKeepsBashTest() {
        val buildId = "sh_no_shebang_test"
        val workspace = File(tmpDir, "sh_no_shebang_test_workspace")
        workspace.deleteRecursively()
        workspace.mkdirs()

        val file = ShellUtil.getCommandFile(
            buildId = buildId,
            script = "echo hi",
            dir = workspace,
            buildEnvs = emptyList(),
            runtimeVariables = emptyMap(),
            workspace = workspace
        )

        val content = file.readText()
        Assertions.assertTrue(content.contains("local content="))
        Assertions.assertFalse(content.contains("bash -c"))

        file.delete()
        workspace.deleteRecursively()
    }

    @Test
    fun formatMultipleLinesPosixShebangVariantsTest() {
        /* 以下写法均使用 POSIX shell，必须命中 POSIX 分支（解释器可位于参数或其它程序之后） */
        listOf(
            "#!/bin/sh",
            "#!/usr/bin/env sh",
            "#!/bin/busybox sh",
            "#!/bin/dash",
            "#!/bin/ash",
            "#!/bin/sh -e",
            "#!/usr/bin/env -S sh -e",
            "#!/usr/bin/env -u VAR sh",
            "#!/usr/bin/nice sh",
            "#!/usr/bin/time -p sh",
            "#!/bin/sh\t-e"
        ).forEachIndexed { index, shebang ->
            val buildId = "sh_shebang_posix_test_$index"
            val workspace = File(tmpDir, "${buildId}_workspace")
            workspace.deleteRecursively()
            workspace.mkdirs()

            val content = ShellUtil.getCommandFile(
                buildId = buildId,
                script = "$shebang\necho hi",
                dir = workspace,
                buildEnvs = emptyList(),
                runtimeVariables = emptyMap(),
                workspace = workspace
            ).readText()

            Assertions.assertTrue(content.contains("bash -c"), "should use POSIX branch: $shebang")
            Assertions.assertFalse(content.contains("local content="), "should not use bash branch: $shebang")

            workspace.deleteRecursively()
        }
    }

    @Test
    fun formatMultipleLinesPosixInnerDollarEscapedTest() {
        /* 与 OS 无关的契约测试：内层脚本体中的 $ 必须全部被反斜杠转义，
           否则会被外层 sh 提前展开——表现为变量值丢失，且内容可逃逸为命令 */
        val buildId = "sh_escape_contract_test"
        val workspace = File(tmpDir, "sh_escape_contract_test_workspace")
        workspace.deleteRecursively()
        workspace.mkdirs()

        val file = ShellUtil.getCommandFile(
            buildId = buildId,
            script = "#!/bin/sh\necho hi",
            dir = workspace,
            buildEnvs = emptyList(),
            runtimeVariables = emptyMap(),
            workspace = workspace
        )

        val content = file.readText()
        /*取 `bash -c "…" _ ` 之间的内层脚本体*/
        val body = Regex("""bash -c "(.*?)" _ """).find(content)!!.groupValues[1]
        Assertions.assertFalse(
            Regex("""(?<!\\)\$""").containsMatchIn(body),
            "inner script body contains unescaped \$: $body"
        )

        file.delete()
        workspace.deleteRecursively()
    }

    /**
     * 用 bash 真实执行生成的 .sh。
     * 输出重定向到文件后再 waitFor(timeout)：避免管道读阻塞导致超时保护失效。
     */
    private fun runSh(scriptFile: File, workspace: File): Pair<Int, String> =
        runShWith("bash", scriptFile, workspace)

    /** 用指定解释器真实执行生成的 .sh */
    private fun runShWith(shell: String, scriptFile: File, workspace: File): Pair<Int, String> {
        val consoleFile = File.createTempFile("sh_e2e_console_", ".log")
        consoleFile.deleteOnExit()
        val process = ProcessBuilder(shell, scriptFile.absolutePath)
            .directory(workspace)
            .redirectErrorStream(true)
            .redirectOutput(consoleFile)
            .start()
        val finished = process.waitFor(60, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
        }
        Assertions.assertTrue(finished, "$shell 执行超时")
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

    @Test
    @EnabledOnOs(OS.LINUX)
    fun formatMultipleLinesPosixMatchesBashTest() {
        /* 同一输入分别经 bash 分支与 POSIX(sh) 分支编码，产物应逐字节相同 */
        val bashBuildId = "sh_impl_compare_bash"
        val posixBuildId = "sh_impl_compare_posix"
        val bashWorkspace = File(tmpDir, "sh_impl_compare_bash_workspace")
        val posixWorkspace = File(tmpDir, "sh_impl_compare_posix_workspace")
        listOf(bashWorkspace, posixWorkspace).forEach {
            it.deleteRecursively()
            it.mkdirs()
        }
        /* 覆盖：多行 / 百分号 / 字面 %0A / 中文 / CR / 尾换行 / 叹号 */
        val value = "line1\n100% done\nliteral %0A here\n中文\r\nhello!world\n"
        val call = "format_multiple_lines \"::set-output name=RESULT::$value\""

        val bashFile = ShellUtil.getCommandFile(
            buildId = bashBuildId,
            script = "#!/bin/bash\n$call",
            dir = bashWorkspace,
            buildEnvs = emptyList(),
            runtimeVariables = emptyMap(),
            workspace = bashWorkspace
        )
        val posixFile = ShellUtil.getCommandFile(
            buildId = posixBuildId,
            script = "#!/bin/sh\n$call",
            dir = posixWorkspace,
            buildEnvs = emptyList(),
            runtimeVariables = emptyMap(),
            workspace = posixWorkspace
        )

        val (bashExit, bashConsole) = runShWith("bash", bashFile, bashWorkspace)
        Assertions.assertEquals(0, bashExit, bashConsole)
        val (posixExit, posixConsole) = runShWith("sh", posixFile, posixWorkspace)
        Assertions.assertEquals(0, posixExit, posixConsole)

        val bashLines = ScriptEnvUtils.getMultipleLines(bashBuildId, bashWorkspace)
        val posixLines = ScriptEnvUtils.getMultipleLines(posixBuildId, posixWorkspace)
        /* 两个实现的编码产物逐字节相同 */
        Assertions.assertEquals(bashLines, posixLines)
        /* 且往返后与原文一致（含尾换行） */
        val decoded = ScriptTask.decodeMultipleLines(lines = posixLines, jobId = jobId, stepId = stepId)
        Assertions.assertEquals(value, decoded["jobs.$jobId.steps.$stepId.outputs.RESULT"])

        bashFile.delete()
        posixFile.delete()
        bashWorkspace.deleteRecursively()
        posixWorkspace.deleteRecursively()
    }

    /**
     * 内容含引号与命令替换时的注入抵抗：外层若提前展开内容，二者都会被当作命令执行。
     * 内容经变量传入（而非内联进调用字面量），以便构造含引号的用例。
     */
    @Test
    @EnabledOnOs(OS.LINUX)
    fun formatMultipleLinesPosixInjectionResistanceTest() {
        val buildId = "sh_inject_test"
        val workspace = File(tmpDir, "sh_inject_test_workspace")
        workspace.deleteRecursively()
        workspace.mkdirs()

        val sideEffect = File(workspace, "PWNED")
        val marker = sideEffect.absolutePath
        val value = "x\"; touch $marker; echo \"\$(touch $marker)"
        val script = "#!/bin/sh\n__v=" + shellSingleQuote(value) +
            "\nformat_multiple_lines \"::set-output name=K::\$__v\""

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
        /* 内容必须原样落盘，不得被执行 */
        Assertions.assertFalse(sideEffect.exists(), "payload was executed as a command: $console")

        val decoded = ScriptTask.decodeMultipleLines(
            lines = ScriptEnvUtils.getMultipleLines(buildId, workspace),
            jobId = jobId,
            stepId = stepId
        )
        Assertions.assertEquals(value, decoded["jobs.$jobId.steps.$stepId.outputs.K"])

        file.delete()
        workspace.deleteRecursively()
    }

    @Test
    fun getMultipleLinesTooLargeReturnsEmptyTest() {
        /* 文件超限时跳过读取并返回空列表（不进内存），不抛异常 */
        val buildId = "ml_too_large_test"
        val workspace = File(tmpDir, "ml_too_large_test_workspace")
        workspace.deleteRecursively()
        workspace.mkdirs()

        val file = File(workspace, ScriptEnvUtils.getMultipleLineFile(buildId))
        file.outputStream().use { it.write(ByteArray(10 * 1024 * 1024 + 1)) }

        Assertions.assertEquals(emptyList<String>(), ScriptEnvUtils.getMultipleLines(buildId, workspace))

        workspace.deleteRecursively()
    }

    @Test
    fun getMultipleLinesMissingFileReturnsEmptyTest() {
        val buildId = "ml_missing_test"
        val workspace = File(tmpDir, "ml_missing_test_workspace")
        workspace.deleteRecursively()
        workspace.mkdirs()

        Assertions.assertEquals(emptyList<String>(), ScriptEnvUtils.getMultipleLines(buildId, workspace))

        workspace.deleteRecursively()
    }

    /** 转成单引号 shell 字面量，用于把待测内容安全嵌入被测脚本 */
    private fun shellSingleQuote(s: String): String = "'" + s.replace("'", "'\\''") + "'"
}

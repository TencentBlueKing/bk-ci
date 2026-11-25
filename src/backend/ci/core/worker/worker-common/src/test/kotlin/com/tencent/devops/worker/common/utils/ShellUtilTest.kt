package com.tencent.devops.worker.common.utils

import com.tencent.devops.worker.common.task.script.ScriptEnvUtils
import com.tencent.devops.worker.common.task.script.ScriptTask
import java.io.File
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS

class ShellUtilTest {

    private val tmpDir = File(System.getProperty("java.io.tmpdir"))
    private val jobId = "job_xx"
    private val stepId = "step_xx"

    /** 每个用例独占的干净 workspace：先清理，避免读到上次运行的残留 */
    private fun newWorkspace(name: String) = File(tmpDir, name).apply {
        deleteRecursively()
        mkdirs()
    }

    /** 生成注入脚本：用例只需关心 script 与 buildId，其余参数固定 */
    private fun generateScript(buildId: String, script: String, workspace: File) =
        ShellUtil.getCommandFile(
            buildId = buildId,
            script = script,
            dir = workspace,
            buildEnvs = emptyList(),
            runtimeVariables = emptyMap(),
            workspace = workspace
        )

    @Test
    @DisplayName("bash 版函数注入与编码目标正确")
    fun formatMultipleLinesInjectedTest() {
        val buildId = "sh_multi_line_test"
        val script = "format_multiple_lines \"::set-output name=TEST::test_value\""
        val workspace = newWorkspace("sh_multi_line_test_workspace")

        val file = generateScript(buildId, script, workspace)

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
    @DisplayName("不调用时仍注入函数且占位符已替换")
    fun formatMultipleLinesWithoutCallTest() {
        val buildId = "sh_no_call_test"
        val workspace = newWorkspace("sh_no_call_test_workspace")

        val file = generateScript(buildId, "echo hello", workspace)

        val content = file.readText()
        /* 即使脚本不调用，函数定义也存在 */
        Assertions.assertTrue(content.contains("format_multiple_lines()"))
        Assertions.assertFalse(content.contains("##multiLineFile##"))

        file.delete()
        workspace.deleteRecursively()
    }

    @Test
    @DisplayName("#!/bin/sh 命中 POSIX 分支")
    fun formatMultipleLinesPosixInjectedTest() {
        val buildId = "sh_posix_inject_test"
        val workspace = newWorkspace("sh_posix_inject_test_workspace")

        val file = generateScript(buildId, "#!/bin/sh\necho hi", workspace)

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
    @DisplayName("#!/usr/bin/env sh 命中 POSIX 分支")
    fun formatMultipleLinesEnvShShebangInjectedTest() {
        val buildId = "sh_env_posix_inject_test"
        val workspace = newWorkspace("sh_env_posix_inject_test_workspace")

        val file = generateScript(buildId, "#!/usr/bin/env sh\necho hi", workspace)

        val content = file.readText()
        Assertions.assertTrue(content.contains("bash -c"))
        Assertions.assertFalse(content.contains("local content="))

        file.delete()
        workspace.deleteRecursively()
    }

    @Test
    @DisplayName("#!/bin/bash 保持 bash 分支")
    fun formatMultipleLinesBashShebangInjectedTest() {
        val buildId = "sh_bash_inject_test"
        val workspace = newWorkspace("sh_bash_inject_test_workspace")

        val file = generateScript(buildId, "#!/bin/bash\necho hi", workspace)

        val content = file.readText()
        /* bash 系维持既有实现 */
        Assertions.assertTrue(content.contains("local content="))
        Assertions.assertFalse(content.contains("bash -c"))

        file.delete()
        workspace.deleteRecursively()
    }

    @Test
    @DisplayName("无 shebang 保持 bash 分支（既有行为）")
    fun formatMultipleLinesNoShebangKeepsBashTest() {
        val buildId = "sh_no_shebang_test"
        val workspace = newWorkspace("sh_no_shebang_test_workspace")

        val file = generateScript(buildId, "echo hi", workspace)

        val content = file.readText()
        Assertions.assertTrue(content.contains("local content="))
        Assertions.assertFalse(content.contains("bash -c"))

        file.delete()
        workspace.deleteRecursively()
    }

    @Test
    @DisplayName("带参数等 11 种 POSIX shebang 写法均命中")
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
            val workspace = newWorkspace("${buildId}_workspace")

            val content = generateScript(buildId, "$shebang\necho hi", workspace).readText()

            Assertions.assertTrue(content.contains("bash -c"), "should use POSIX branch: $shebang")
            Assertions.assertFalse(content.contains("local content="), "should not use bash branch: $shebang")

            workspace.deleteRecursively()
        }
    }

    @Test
    @DisplayName("内层脚本体中的 $ 必须全部转义")
    fun formatMultipleLinesPosixInnerDollarEscapedTest() {
        /* 与 OS 无关的契约测试：内层脚本体中的 $ 必须全部被反斜杠转义，
           否则会被外层 sh 提前展开——表现为变量值丢失，且内容可逃逸为命令 */
        val buildId = "sh_escape_contract_test"
        val workspace = newWorkspace("sh_escape_contract_test_workspace")

        val file = generateScript(buildId, "#!/bin/sh\necho hi", workspace)

        val content = file.readText()
        /*取 `bash -c "…" >> ` 之间的内层脚本体*/
        val body = Regex("""bash -c "(.*?)" >> """).find(content)!!.groupValues[1]
        Assertions.assertFalse(
            Regex("""(?<!\\)\$""").containsMatchIn(body),
            "inner script body contains unescaped \$: $body"
        )

        file.delete()
        workspace.deleteRecursively()
    }

    @Test
    @DisplayName("函数定义与后续 source 命令之间必须分行")
    fun formatMultipleLinesFunctionEndsWithNewlineTest() {
        /* 注入的函数定义与后续 source 用户脚本的命令之间必须有换行 */
        listOf("#!/bin/sh", "#!/bin/bash", "").forEachIndexed { index, shebang ->
            val buildId = "sh_fn_newline_test_$index"
            val workspace = newWorkspace("${buildId}_workspace")

            val content = generateScript(buildId, shebang + "\necho hi", workspace).readText()

            Assertions.assertTrue(
                content.contains("}\n. "),
                "function definition is not terminated by newline (shebang=$shebang)"
            )

            workspace.deleteRecursively()
        }
    }

    @Test
    @DisplayName("bash 分支注入内容超限预检且位于编码之前")
    fun formatMultipleLinesOversizeGuardInjectedTest() {
        val buildId = "sh_oversize_guard"
        val workspace = newWorkspace("sh_oversize_guard_workspace")

        val file = generateScript(buildId, "format_multiple_lines \"::set-output name=RESULT::value\"", workspace)

        val content = file.readText()
        /* 转义后形态：预检语句须与 shell 语法逐字符一致，避免多转义/少转义 */
        Assertions.assertTrue(
            content.contains(
                "    if [ \"\${#content}\" -gt " +
                    "${ScriptEnvUtils.MULTILINE_FILE_MAX_LENGTH} ]; then\n"
            ),
            "预检语句的转义后形态不符: $content"
        )
        /* 残留未展开的转义（如 \$ 未还原）不得出现 */
        Assertions.assertFalse(content.contains("\\\${#content}"), "存在未展开的转义: $content")
        val guardIndex = content.indexOf("content too large")
        val encodeIndex = content.indexOf("content//%/%25")
        Assertions.assertTrue(guardIndex > 0, "应注入内容超限预检: $content")
        Assertions.assertTrue(guardIndex < encodeIndex, "预检须位于编码之前: $content")

        file.delete()
        workspace.deleteRecursively()
    }

    @Test
    @DisplayName("bash 分支按字节计数且 locale 设置先于预检")
    fun formatMultipleLinesByteCountGuardInjectedTest() {
        val buildId = "sh_byte_guard"
        val workspace = newWorkspace("sh_byte_guard_workspace")

        val content =
            generateScript(buildId, "format_multiple_lines \"::set-output name=RESULT::v\"", workspace).readText()

        Assertions.assertTrue(content.contains("local LC_ALL=C"), "预检须按字节计数: $content")
        Assertions.assertTrue(
            content.indexOf("local LC_ALL=C") < content.indexOf("content too large"),
            "locale 设置须先于预检: $content"
        )

        workspace.deleteRecursively()
    }

    @Test
    @DisplayName("POSIX 分支以尾字节校验传输并校验内容完整")
    fun formatMultipleLinesPosixTrailerCheckTest() {
        val buildId = "sh_posix_delimiter"
        val workspace = newWorkspace("sh_posix_delimiter_workspace")

        val content = generateScript(buildId, "#!/bin/sh\necho hi", workspace).readText()

        Assertions.assertTrue(
            content.contains(
                "if ! (LC_ALL=C; [ \"\${#1}\" -le ${ScriptEnvUtils.MULTILINE_FILE_MAX_LENGTH} ])"
            ),
            "外层须在管道之前按字节预检: $content"
        )
        Assertions.assertTrue(
            content.contains("printf '%s\\001' \"\$1\""),
            "内容须以 `\\001` 结尾后送入管道: $content"
        )
        Assertions.assertTrue(
            content.contains("read -r -d '' content || true"),
            "内层须读到流末尾: $content"
        )
        Assertions.assertTrue(
            content.contains("content%?"),
            "内层须校验并剥离尾字节: $content"
        )
        Assertions.assertTrue(
            content.contains("content read failed"),
            "尾字节缺失须显式失败: $content"
        )

        workspace.deleteRecursively()
    }

    @Test
    @DisplayName("bash 分支内容超限时显式失败且不产出多行变量")
    @EnabledOnOs(OS.LINUX)
    fun formatMultipleLinesOversizeFailsLoudlyTest() {
        /* 构造刚好超过 10 MB 的内容，须在编码之前被拒绝 */
        val buildId = "sh_e2e_oversize"
        val workspace = newWorkspace("sh_e2e_oversize_workspace")
        val oversize = "a".repeat(ScriptEnvUtils.MULTILINE_FILE_MAX_LENGTH.toInt() + 1)
        val script = "format_multiple_lines \"::set-output name=RESULT::$oversize\""

        val file = generateScript(buildId, script, workspace)

        val (exitCode, console) = runSh(file, workspace)
        Assertions.assertNotEquals(0, exitCode, "超限内容须显式失败: ${console.take(500)}")
        Assertions.assertTrue(console.contains("content too large"), "须给出明确原因: ${console.take(500)}")
        Assertions.assertEquals(
            emptyList<String>(),
            ScriptEnvUtils.getMultipleLines(buildId, workspace),
            "超限时不应产出多行变量"
        )

        file.delete()
        workspace.deleteRecursively()
    }

    @Test
    @DisplayName("POSIX 分支内容超限时在管道之前显式失败")
    @EnabledOnOs(OS.LINUX)
    fun formatMultipleLinesPosixOversizeFailsLoudlyTest() {
        /* 构造刚好超过 10 MB 的内容，须在进入管道之前被拒绝 */
        val buildId = "sh_posix_oversize"
        val workspace = newWorkspace("sh_posix_oversize_workspace")
        val oversize = "a".repeat(ScriptEnvUtils.MULTILINE_FILE_MAX_LENGTH.toInt() + 1)
        val script = "#!/bin/sh\nformat_multiple_lines \"::set-output name=RESULT::$oversize\""

        val file = generateScript(buildId, script, workspace)

        val (exitCode, console) = runShWith("sh", file, workspace)
        Assertions.assertNotEquals(0, exitCode, "超限内容须显式失败: ${console.take(500)}")
        Assertions.assertTrue(console.contains("content too large"), "须给出明确原因: ${console.take(500)}")
        Assertions.assertEquals(
            emptyList<String>(),
            ScriptEnvUtils.getMultipleLines(buildId, workspace),
            "超限时不应产出多行变量"
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

    /**
     * 断言脚本以 0 退出。失败时把控制台输出与生成脚本一并写入断言消息，
     */
    private fun assertShellOk(shell: String, scriptFile: File, exitCode: Int, console: String) {
        Assertions.assertEquals(
            0,
            exitCode,
            "[$shell] exit=$exitCode\n--- console ---\n$console\n--- generated script ---\n${scriptFile.readText()}"
        )
    }

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
    @DisplayName("真实 bash 执行且值逐字符往返一致")
    @EnabledOnOs(OS.LINUX)
    fun formatMultipleLinesEndToEndTest() {
        val buildId = "sh_e2e_multi"
        val workspace = newWorkspace("sh_e2e_multi_workspace")
        /* 覆盖：多行 / 百分号 / 字面 %0A / 中文 / CR / 多变量追加 */
        val result = "line1\n100% done\nliteral %0A here\n中文\r"
        val script = "format_multiple_lines \"::set-output name=RESULT::$result\"\n" +
            "format_multiple_lines \"::set-output name=COUNT::42\""

        val file = generateScript(buildId, script, workspace)

        val (exitCode, console) = runSh(file, workspace)
        assertShellOk("bash", file, exitCode, console)

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
    @DisplayName("重定向目标引号形态与 setEnv/setGateValue 一致")
    fun formatMultipleLinesRedirectionQuotingTest() {
        val buildId = "sh_quote_target"
        val workspace = newWorkspace("sh_quote_target_workspace")

        val file = generateScript(buildId, "format_multiple_lines \"::set-output name=RESULT::v\"", workspace)

        val content = file.readText()
        val target = File(workspace, ScriptEnvUtils.getMultipleLineFile(buildId)).absolutePath
        /* 占位符不自带引号、替换值带引号，与 setEnv/setGateValue 保持同一配对方式 */
        Assertions.assertTrue(
            content.contains(">> \"$target\""),
            "重定向目标应为单层引号: $content"
        )
        Assertions.assertFalse(
            content.contains(">> \"\"$target\"\""),
            "重定向目标不得出现双层引号: $content"
        )

        file.delete()
        workspace.deleteRecursively()
    }

    @Test
    @DisplayName("POSIX 与 bash 两实现产物逐字节一致")
    @EnabledOnOs(OS.LINUX)
    fun formatMultipleLinesPosixMatchesBashTest() {
        /* 同一输入分别经 bash 分支与 POSIX(sh) 分支编码，产物应逐字节相同 */
        val bashBuildId = "sh_impl_compare_bash"
        val posixBuildId = "sh_impl_compare_posix"
        val bashWorkspace = newWorkspace("sh_impl_compare_bash_workspace")
        val posixWorkspace = newWorkspace("sh_impl_compare_posix_workspace")
        /* 覆盖：多行 / 百分号 / 字面 %0A / 中文 / CR / 尾换行 / 叹号 */
        val value = "line1\n100% done\nliteral %0A here\n中文\r\nhello!world\n"
        val call = "format_multiple_lines \"::set-output name=RESULT::$value\""

        val bashFile = generateScript(bashBuildId, "#!/bin/bash\n$call", bashWorkspace)
        val posixFile = generateScript(posixBuildId, "#!/bin/sh\n$call", posixWorkspace)

        val (bashExit, bashConsole) = runShWith("bash", bashFile, bashWorkspace)
        assertShellOk("bash", bashFile, bashExit, bashConsole)
        val (posixExit, posixConsole) = runShWith("sh", posixFile, posixWorkspace)
        assertShellOk("sh", posixFile, posixExit, posixConsole)

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

    @Test
    @DisplayName("内容含引号与命令替换时不得被执行")
    @EnabledOnOs(OS.LINUX)
    fun formatMultipleLinesPosixInjectionResistanceTest() {
        /* 内容经变量传入（而非内联进调用字面量），以便构造含引号的用例 */
        val buildId = "sh_inject_test"
        val workspace = newWorkspace("sh_inject_test_workspace")

        val sideEffect = File(workspace, "PWNED")
        val marker = sideEffect.absolutePath
        val value = "x\"; touch $marker; echo \"\$(touch $marker)"
        val script = "#!/bin/sh\n__v=" + shellSingleQuote(value) +
            "\nformat_multiple_lines \"::set-output name=K::\$__v\""

        val file = generateScript(buildId, script, workspace)

        val (exitCode, console) = runSh(file, workspace)
        assertShellOk("bash", file, exitCode, console)
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
    @DisplayName("POSIX 分支超长内容经管道传输且往返一致")
    @EnabledOnOs(OS.LINUX)
    fun formatMultipleLinesPosixLargeContentTest() {
        /* 单条内容超过 128KB（原单参数上限），经管道传输后须完整落盘并可往返解码 */
        val buildId = "sh_posix_large_content"
        val workspace = newWorkspace("sh_posix_large_content_workspace")
        val value = "a".repeat(200 * 1024)
        val script = "#!/bin/sh\n__v=" + shellSingleQuote(value) +
            "\nformat_multiple_lines \"::set-output name=BIG::\$__v\""

        val file = generateScript(buildId, script, workspace)

        val (exitCode, console) = runShWith("sh", file, workspace)
        assertShellOk("sh", file, exitCode, console)

        val decoded = ScriptTask.decodeMultipleLines(
            lines = ScriptEnvUtils.getMultipleLines(buildId, workspace),
            jobId = jobId,
            stepId = stepId
        )
        Assertions.assertEquals(value, decoded["jobs.$jobId.steps.$stepId.outputs.BIG"])

        file.delete()
        workspace.deleteRecursively()
    }

    @Test
    @DisplayName("多行文件超限时保留已读记录并丢弃超出部分")
    fun getMultipleLinesOverLimitKeepsReadRecordsTest() {
        /* 读取预算按已读字节累计：装得下的记录保留，放不下的记录及其后续一律丢弃 */
        val buildId = "ml_partial_test"
        val workspace = newWorkspace("ml_partial_test_workspace")

        val head = "::set-output name=HEAD::" + "a".repeat(100)
        val tail = "::set-output name=TAIL::" + "b".repeat(ScriptEnvUtils.MULTILINE_FILE_MAX_LENGTH.toInt())
        File(workspace, ScriptEnvUtils.getMultipleLineFile(buildId))
            .writeText("$head\n$tail\n")

        val lines = ScriptEnvUtils.getMultipleLines(buildId, workspace)

        Assertions.assertEquals(1, lines.size)
        Assertions.assertEquals(head, lines[0])

        workspace.deleteRecursively()
    }

    @Test
    @DisplayName("单行超预算时该行整体丢弃且不抛异常")
    fun getMultipleLinesSingleOversizeLineDroppedTest() {
        val buildId = "ml_single_oversize_test"
        val workspace = newWorkspace("ml_single_oversize_test_workspace")

        val file = File(workspace, ScriptEnvUtils.getMultipleLineFile(buildId))
        file.outputStream().use { it.write(ByteArray(10 * 1024 * 1024 + 1)) }

        Assertions.assertEquals(emptyList<String>(), ScriptEnvUtils.getMultipleLines(buildId, workspace))

        workspace.deleteRecursively()
    }

    @Test
    @DisplayName("多行文件不存在时返回空列表")
    fun getMultipleLinesMissingFileReturnsEmptyTest() {
        val buildId = "ml_missing_test"
        val workspace = newWorkspace("ml_missing_test_workspace")

        Assertions.assertEquals(emptyList<String>(), ScriptEnvUtils.getMultipleLines(buildId, workspace))

        workspace.deleteRecursively()
    }

    @Test
    @DisplayName("字面 \\n / \\N / 反斜杠原样往返")
    @EnabledOnOs(OS.LINUX)
    fun formatMultipleLinesLiteralBackslashEndToEndTest() {
        /* 验收 A-3：Windows 路径与 JSON 转义中的字面 \n \N \\ 不得被解释为换行 */
        val buildId = "sh_e2e_literal"
        val workspace = newWorkspace("sh_e2e_literal_workspace")
        val cases = linkedMapOf(
            "LITERAL_N" to """C:\newlogs\report.txt""",
            "LITERAL_NU" to """C:\Program Files (x86)\NVIDIA Corporation\PhysX\Common""",
            "JSON" to """{"path":"C:\\data\\file"}"""
        )
        val script = cases.entries.joinToString("\n") { (key, value) ->
            "format_multiple_lines '::set-output name=$key::$value'"
        }

        val file = generateScript(buildId, script, workspace)

        val (exitCode, console) = runSh(file, workspace)
        assertShellOk("bash", file, exitCode, console)

        val decoded = ScriptTask.decodeMultipleLines(
            lines = ScriptEnvUtils.getMultipleLines(buildId, workspace),
            jobId = jobId,
            stepId = stepId
        )
        cases.forEach { (key, value) ->
            Assertions.assertEquals(value, decoded["jobs.$jobId.steps.$stepId.outputs.$key"])
        }

        file.delete()
        workspace.deleteRecursively()
    }

    @Test
    @DisplayName("空值产生空字符串变量")
    @EnabledOnOs(OS.LINUX)
    fun formatMultipleLinesEmptyValueEndToEndTest() {
        val buildId = "sh_e2e_empty"
        val workspace = newWorkspace("sh_e2e_empty_workspace")

        val file = generateScript(buildId, "format_multiple_lines \"::set-output name=EMPTY::\"", workspace)

        val (exitCode, console) = runSh(file, workspace)
        assertShellOk("bash", file, exitCode, console)

        val decoded = ScriptTask.decodeMultipleLines(
            lines = ScriptEnvUtils.getMultipleLines(buildId, workspace),
            jobId = jobId,
            stepId = stepId
        )
        Assertions.assertEquals("", decoded["jobs.$jobId.steps.$stepId.outputs.EMPTY"])

        file.delete()
        workspace.deleteRecursively()
    }

    @Test
    @DisplayName("同一 KEY 多次调用后者覆盖前者")
    @EnabledOnOs(OS.LINUX)
    fun formatMultipleLinesSameKeyTwiceEndToEndTest() {
        val buildId = "sh_e2e_same_key"
        val workspace = newWorkspace("sh_e2e_same_key_workspace")

        val file = generateScript(
            buildId,
            "format_multiple_lines \"::set-output name=DUP::first\"\n" +
                "format_multiple_lines \"::set-output name=DUP::second\"",
            workspace
        )

        val (exitCode, console) = runSh(file, workspace)
        assertShellOk("bash", file, exitCode, console)

        val decoded = ScriptTask.decodeMultipleLines(
            lines = ScriptEnvUtils.getMultipleLines(buildId, workspace),
            jobId = jobId,
            stepId = stepId
        )
        Assertions.assertEquals("second", decoded["jobs.$jobId.steps.$stepId.outputs.DUP"])

        file.delete()
        workspace.deleteRecursively()
    }

    @Test
    @DisplayName("POSIX 分支注入 bash 存在性检查")
    fun formatMultipleLinesPosixBashCheckInjectedTest() {
        val buildId = "sh_bash_check_test"
        val workspace = newWorkspace("sh_bash_check_test_workspace")

        val content = generateScript(buildId, "#!/bin/sh\necho hi", workspace).readText()
        Assertions.assertTrue(content.contains("command -v bash"))
        Assertions.assertTrue(content.contains("bash not found"))

        workspace.deleteRecursively()
    }

    @Test
    @DisplayName("无 bash 时显式报错而非静默丢失")
    @EnabledOnOs(OS.LINUX)
    fun formatMultipleLinesNoBashFailsLoudlyTest() {
        val buildId = "sh_no_bash"
        val workspace = newWorkspace("sh_no_bash_workspace")

        val file = generateScript(buildId, "#!/bin/sh\nformat_multiple_lines \"::set-output name=K::v\"", workspace)
        val consoleFile = File.createTempFile("sh_nobash_console_", ".log")
        consoleFile.deleteOnExit()
        val process = ProcessBuilder("/bin/sh", file.absolutePath)
            .directory(workspace)
            .redirectErrorStream(true)
            .redirectOutput(consoleFile)
            .apply { environment()["PATH"] = "/nonexistent" }
            .start()
        val finished = process.waitFor(60, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
        }
        Assertions.assertTrue(finished, "sh 执行超时")
        Assertions.assertTrue(
            consoleFile.readText().contains("bash not found"),
            "无 bash 时应显式报错: ${consoleFile.readText()}"
        )

        workspace.deleteRecursively()
    }

    /** 转成单引号 shell 字面量，用于把待测内容安全嵌入被测脚本 */
    private fun shellSingleQuote(s: String): String = "'" + s.replace("'", "'\\''") + "'"
}

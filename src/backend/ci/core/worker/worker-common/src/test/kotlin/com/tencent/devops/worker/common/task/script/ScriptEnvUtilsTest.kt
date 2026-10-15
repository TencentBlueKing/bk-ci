package com.tencent.devops.worker.common.task.script

import java.io.File
import kotlin.text.Charsets
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ScriptEnvUtilsTest {

    private val tmpDir = File(System.getProperty("java.io.tmpdir"))

    @Test
    fun cleanWhenEndRemovesMultilineFilesTest() {
        /*NFR-2：步骤结束时 multiLine.log 与内联块临时文件均被主动清理*/
        val buildId = "clean_multiline_test"
        val workspace = File(tmpDir, "clean_multiline_test_workspace")
        workspace.deleteRecursively()
        workspace.mkdirs()

        val multiLineFile = File(workspace, ScriptEnvUtils.getMultipleLineFile(buildId)).apply {
            writeText("::set-output name=K::v", Charsets.UTF_8)
        }
        val blockFiles = (0..1).map { index ->
            File(tmpDir, ScriptEnvUtils.getMultipleLineBlockFileName(buildId, index)).apply {
                writeText("block$index", Charsets.UTF_8)
            }
        }

        ScriptEnvUtils.cleanWhenEnd(buildId, workspace)

        Assertions.assertFalse(multiLineFile.exists())
        blockFiles.forEach { Assertions.assertFalse(it.exists()) }

        workspace.deleteRecursively()
    }

    @Test
    fun getMultipleLinesAppendsAcrossWritesTest() {
        /*约束 8：同一 buildId 未清理前多次写入为追加，同 KEY 后写覆盖先写*/
        val buildId = "ml_append_test"
        val workspace = File(tmpDir, "ml_append_test_workspace")
        workspace.deleteRecursively()
        workspace.mkdirs()

        val file = File(workspace, ScriptEnvUtils.getMultipleLineFile(buildId))
        file.writeText("::set-output name=K::first\n", Charsets.UTF_8)
        file.appendText("::set-output name=K::second\n", Charsets.UTF_8)

        val lines = ScriptEnvUtils.getMultipleLines(buildId, workspace)
        Assertions.assertEquals(2, lines.size)
        Assertions.assertEquals(
            "second",
            ScriptTask.decodeMultipleLines(
                lines = lines,
                jobId = "job_xx",
                stepId = "step_xx"
            )["jobs.job_xx.steps.step_xx.outputs.K"]
        )

        workspace.deleteRecursively()
    }

    @Test
    fun getMultipleLinesAtExactLimitReturnsContentTest() {
        /*边界：文件大小恰好等于上限时不得被整体丢弃（判定为 > 上限）*/
        val buildId = "ml_exact_limit_test"
        val workspace = File(tmpDir, "ml_exact_limit_test_workspace")
        workspace.deleteRecursively()
        workspace.mkdirs()

        val prefix = "::set-output name=K::"
        val pad = ScriptEnvUtils.MULTILINE_FILE_MAX_LENGTH.toInt() - prefix.length
        val file = File(workspace, ScriptEnvUtils.getMultipleLineFile(buildId))
        file.writeText(prefix + "a".repeat(pad), Charsets.UTF_8)
        Assertions.assertEquals(ScriptEnvUtils.MULTILINE_FILE_MAX_LENGTH, file.length())

        val lines = ScriptEnvUtils.getMultipleLines(buildId, workspace)
        Assertions.assertEquals(1, lines.size, "恰好等于上限时应正常返回内容")
        Assertions.assertTrue(lines[0].startsWith(prefix))

        workspace.deleteRecursively()
    }
}

package com.tencent.devops.worker.common.task.script

import com.tencent.devops.worker.common.utils.CommandLineUtils
import java.io.File
import kotlin.text.Charsets
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ScriptTaskTest {

    private val jobId = "job_xx"
    private val stepId = "step_xx"

    private fun decode(lines: List<String>) =
        ScriptTask.decodeMultipleLines(lines, jobId, stepId)

    @Test
    fun decodeMultipleLinesEmptyTest() {
        /*空列表返回空 Map*/
        Assertions.assertEquals(emptyMap<String, String>(), decode(emptyList()))
    }

    @Test
    fun decodeMultipleLinesNullJobIdTest() {
        /*jobId 为空返回空 Map*/
        Assertions.assertEquals(
            emptyMap<String, String>(),
            ScriptTask.decodeMultipleLines(listOf("::set-output name=KEY::value"), null, stepId)
        )
    }

    @Test
    fun decodeMultipleLinesNullStepIdTest() {
        /*stepId 为空返回空 Map*/
        Assertions.assertEquals(
            emptyMap<String, String>(),
            ScriptTask.decodeMultipleLines(listOf("::set-output name=KEY::value"), jobId, null)
        )
    }

    @Test
    fun decodeMultipleLinesBasicTest() {
        /*基本单行值解码*/
        val result = decode(listOf("::set-output name=RESULT::test"))
        Assertions.assertEquals(
            mapOf("jobs.$jobId.steps.$stepId.outputs.RESULT" to "test"),
            result
        )
    }

    @Test
    fun decodeMultipleLinesNewlineTest() {
        /*%0A 解码为 \n*/
        val result = decode(listOf("::set-output name=RESULT::line1%0Aline2%0Aline3"))
        Assertions.assertEquals(
            mapOf("jobs.$jobId.steps.$stepId.outputs.RESULT" to "line1\nline2\nline3"),
            result
        )
    }

    @Test
    fun decodeMultipleLinesCarriageReturnTest() {
        /*%0D 解码为 \r*/
        val result = decode(listOf("::set-output name=RESULT::line1%0Dline2"))
        Assertions.assertEquals(
            mapOf("jobs.$jobId.steps.$stepId.outputs.RESULT" to "line1\rline2"),
            result
        )
    }

    @Test
    fun decodeMultipleLinesPercentTest() {
        /*%25 解码为 %*/
        val result = decode(listOf("::set-output name=RESULT::100%25 done"))
        Assertions.assertEquals(
            mapOf("jobs.$jobId.steps.$stepId.outputs.RESULT" to "100% done"),
            result
        )
    }

    @Test
    fun decodeMultipleLinesDecodeOrderTest() {
        /*解码顺序: %0D/%0A 先于 %25，避免 %25 还原出的 % 与后续 0A 拼出假的 %0A*/
        val result = decode(listOf("::set-output name=RESULT::percent%253A"))
        Assertions.assertEquals(
            mapOf("jobs.$jobId.steps.$stepId.outputs.RESULT" to "percent%3A"),
            result
        )
    }

    @Test
    fun decodeMultipleLinesDecodeOrderEdgeCaseTest() {
        /*%250A 应还原为字面 %0A 而非换行符 — %25 最后解码确保不会拼出假的 %0A*/
        val result = decode(listOf("::set-output name=RESULT::name%250Avalue"))
        Assertions.assertEquals(
            mapOf("jobs.$jobId.steps.$stepId.outputs.RESULT" to "name%0Avalue"),
            result
        )
    }

    @Test
    fun decodeMultipleLinesMixedEncodingTest() {
        /*%0A, %25, %0D 混合解码*/
        val result = decode(listOf("::set-output name=RESULT::line1%0A80%25 line2%0Dline3"))
        Assertions.assertEquals(
            mapOf("jobs.$jobId.steps.$stepId.outputs.RESULT" to "line1\n80% line2\rline3"),
            result
        )
    }

    @Test
    fun decodeMultipleLinesMultipleRecordsTest() {
        /*多条记录同时解码*/
        val result = decode(
            listOf(
                "::set-output name=KEY_A::value_a",
                "::set-output name=KEY_B::line1%0Aline2"
            )
        )
        Assertions.assertEquals(
            mapOf(
                "jobs.$jobId.steps.$stepId.outputs.KEY_A" to "value_a",
                "jobs.$jobId.steps.$stepId.outputs.KEY_B" to "line1\nline2"
            ),
            result
        )
    }

    @Test
    fun decodeMultipleLinesSkipNonSetOutputTest() {
        /*非 ::set-output 行被忽略*/
        val result = decode(
            listOf(
                "some random log",
                "::set-output name=RESULT::value",
                "another log"
            )
        )
        Assertions.assertEquals(
            mapOf("jobs.$jobId.steps.$stepId.outputs.RESULT" to "value"),
            result
        )
    }

    @Test
    fun decodeMultipleLinesSkipLineWithoutSeparatorTest() {
        /*缺少 :: 分隔符的行被忽略*/
        val result = decode(listOf("::set-output name=RESULT_no_separator"))
        Assertions.assertEquals(emptyMap<String, String>(), result)
    }

    @Test
    fun decodeMultipleLinesSkipEmptyKeyTest() {
        /*空 key 被忽略*/
        val result = decode(listOf("::set-output name=::value"))
        Assertions.assertEquals(emptyMap<String, String>(), result)
    }

    @Test
    fun decodeMultipleLinesSkipInvalidKeyTest() {
        /*含 - 的非法 key 被忽略，且回调收到该 key*/
        val invalidKeys = mutableListOf<String>()
        val result = ScriptTask.decodeMultipleLines(
            lines = listOf("::set-output name=my-key::value"),
            jobId = jobId,
            stepId = stepId,
            onInvalidKey = { invalidKeys.add(it) }
        )
        Assertions.assertEquals(emptyMap<String, String>(), result)
        Assertions.assertEquals(listOf("my-key"), invalidKeys)
    }

    @Test
    fun decodeMultipleLinesInvalidLineCallbackTest() {
        /*缺少 set-output 前缀的行被忽略，且回调收到该行内容*/
        val invalidLines = mutableListOf<String>()
        val result = ScriptTask.decodeMultipleLines(
            lines = listOf("some random log", "::set-output name=RESULT::value"),
            jobId = jobId,
            stepId = stepId,
            onInvalidLine = { invalidLines.add(it) }
        )
        Assertions.assertEquals(
            mapOf("jobs.$jobId.steps.$stepId.outputs.RESULT" to "value"),
            result
        )
        Assertions.assertEquals(listOf("some random log"), invalidLines)
    }

    @Test
    fun decodeMultipleLinesDuplicateKeyTest() {
        /*同一 KEY 出现多次时后一条覆盖前一条*/
        val result = decode(
            listOf(
                "::set-output name=DUP::first",
                "::set-output name=DUP::second"
            )
        )
        Assertions.assertEquals(
            mapOf("jobs.$jobId.steps.$stepId.outputs.DUP" to "second"),
            result
        )
    }

    @Test
    fun decodeMultipleLinesLongValueNotTruncatedTest() {
        /*解码层不截断：4000 字符上限由 ScriptTask 的 failIfVariableInvalidCheck 另行判定*/
        val longValue = "A".repeat(5000)
        val result = decode(listOf("::set-output name=LONG::$longValue"))
        Assertions.assertEquals(
            mapOf("jobs.$jobId.steps.$stepId.outputs.LONG" to longValue),
            result
        )
    }

    @Test
    fun multiLineOverridesSingleLineOutputTest() {
        /*验收 A-11：单行通道与多行通道写入同一 KEY 时，取多行值*/
        val buildId = "override_single_line_test"
        val workspace = File(System.getProperty("java.io.tmpdir"), "override_single_line_ws")
        workspace.deleteRecursively()
        workspace.mkdirs()

        CommandLineUtils.appendOutputToFile(
            tmpLine = "::set-output name=SAME::from_single_line",
            workspace = workspace,
            resultLogFile = ScriptEnvUtils.getContextFile(buildId),
            jobId = jobId,
            stepId = stepId
        )
        File(workspace, ScriptEnvUtils.getMultipleLineFile(buildId))
            .writeText("::set-output name=SAME::from_multi_line", Charsets.UTF_8)

        val merged = linkedMapOf<String, String>().apply {
            putAll(ScriptEnvUtils.getContext(buildId, workspace))
            putAll(
                ScriptTask.decodeMultipleLines(
                    lines = ScriptEnvUtils.getMultipleLines(buildId, workspace),
                    jobId = jobId,
                    stepId = stepId
                )
            )
        }
        Assertions.assertEquals("from_multi_line", merged["jobs.$jobId.steps.$stepId.outputs.SAME"])

        workspace.deleteRecursively()
    }
}

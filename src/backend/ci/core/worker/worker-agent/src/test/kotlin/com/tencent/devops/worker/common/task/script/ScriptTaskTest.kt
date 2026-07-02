package com.tencent.devops.worker.common.task.script

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
}

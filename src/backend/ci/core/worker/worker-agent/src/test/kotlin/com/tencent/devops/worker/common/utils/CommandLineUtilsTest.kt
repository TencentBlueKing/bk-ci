package com.tencent.devops.worker.common.utils

import java.io.File
import java.security.AccessController
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import sun.security.action.GetPropertyAction

class CommandLineUtilsTest {

    @Test
    fun reportProgressRateTest() {
        fun func(str: String) = CommandLineUtils.reportProgressRate("test", str)
        /*不识别*/
        Assertions.assertEquals(func("echo \"::set-progress-rate 0.3758\""), null)
        Assertions.assertEquals(func("echo '::set-progress-rate 0.3758'"), null)
        Assertions.assertEquals(func("echo ::set-progress-rate 0.3758"), null)
        Assertions.assertEquals(func("print(\"::set-progress-rate 0.3758\")"), null)
        /*windows兼容*/
        Assertions.assertEquals(func("\"::set-progress-rate 0.3758\""), 0.3758)
        /*默认*/
        Assertions.assertEquals(func("::set-progress-rate 0.3758"), 0.3758)
        /*兼容多空格*/
        Assertions.assertEquals(func("::set-progress-rate    0.3758"), 0.3758)
        Assertions.assertEquals(func(" ::set-progress-rate 0.3758"), 0.3758)
    }

    @Test
    fun appendVariableToFileTest() {
        fun func(str: String) = CommandLineUtils.appendVariableToFile(
            str, File(
                AccessController
                    .doPrivileged(GetPropertyAction("java.io.tmpdir"))
            ), "appendVariableToFileTest"
        )
        /*不识别*/
        Assertions.assertEquals(func("echo \"::set-variable name=RESULT::test\""), null)
        Assertions.assertEquals(func("echo '::set-variable name=RESULT::test'"), null)
        Assertions.assertEquals(func("echo ::set-variable name=RESULT::test"), null)
        Assertions.assertEquals(func("print(\"::set-variable name=RESULT::test\")"), null)
        /*多空格*/
        Assertions.assertEquals(func(" ::set-variable name=RESULT::test"), null)
        Assertions.assertEquals(func("::set-variable   name=RESULT::test"), null)
        /*windows兼容*/
        Assertions.assertEquals(func("\"::set-variable name=RESULT::test\""), "variables.RESULT=test\n")
        /*默认*/
        Assertions.assertEquals(func("::set-variable name=RESULT::test"), "variables.RESULT=test\n")
    }

    @Test
    fun appendRemarkToFileTest() {
        fun func(str: String) = CommandLineUtils.appendRemarkToFile(
            str, File(
                AccessController
                    .doPrivileged(GetPropertyAction("java.io.tmpdir"))
            ), "appendRemarkToFileTest"
        )
        /*不识别*/
        Assertions.assertEquals(func("echo \"::set-remark 备注信息\""), null)
        Assertions.assertEquals(func("echo '::set-remark 备注信息'"), null)
        Assertions.assertEquals(func("echo ::set-remark 备注信息"), null)
        Assertions.assertEquals(func("print(\"::set-remark 备注信息\")"), null)
        /*多空格*/
        Assertions.assertEquals(func(" ::set-remark 备注信息"), null)
        /*windows兼容*/
        Assertions.assertEquals(func("\"::set-remark 备注信息\""), "BK_CI_BUILD_REMARK=备注信息\n")
        /*默认*/
        Assertions.assertEquals(func("::set-remark 备注信息"), "BK_CI_BUILD_REMARK=备注信息\n")
        Assertions.assertEquals(func("::set-remark   备注信息"), "BK_CI_BUILD_REMARK=  备注信息\n")
    }

    @Test
    fun appendOutputToFileTest() {
        val jobId = "job_xx"
        val stepId = "step_xx"
        fun func(str: String) = CommandLineUtils.appendOutputToFile(
            tmpLine = str,
            workspace = File(
                AccessController
                    .doPrivileged(GetPropertyAction("java.io.tmpdir"))
            ),
            resultLogFile = "appendOutputToFileTest",
            jobId = jobId,
            stepId = stepId
        )
        /*不识别*/
        Assertions.assertEquals(func("echo \"::set-output name=RESULT::test\""), null)
        Assertions.assertEquals(func("echo '::set-output name=RESULT::test'"), null)
        Assertions.assertEquals(func("echo ::set-output name=RESULT::test"), null)
        Assertions.assertEquals(func("print(\"::set-output name=RESULT::test\")"), null)
        /*多空格*/
        Assertions.assertEquals(func(" ::set-output name=RESULT::test"), null)
        Assertions.assertEquals(func("::set-output    name=RESULT::test"), null)
        /*windows兼容*/
        Assertions.assertEquals(
            func("\"::set-output name=RESULT::test\""),
            "jobs.$jobId.steps.$stepId.outputs.RESULT=test\n"
        )
        /*默认*/
        Assertions.assertEquals(
            func("::set-output name=RESULT::test"),
            "jobs.$jobId.steps.$stepId.outputs.RESULT=test\n"
        )
    }

    @Test
    fun appendGateToFileTest() {
        fun func(str: String) = CommandLineUtils.appendGateToFile(
            str, File(
                AccessController
                    .doPrivileged(GetPropertyAction("java.io.tmpdir"))
            ), "appendGateToFileTest"
        )
        /*不识别*/
        Assertions.assertEquals(func("echo \"::set-gate-value name=pass_rate::0.9\""), null)
        Assertions.assertEquals(func("echo '::set-gate-value name=pass_rate::0.9'"), null)
        Assertions.assertEquals(func("echo ::set-gate-value name=pass_rate::0.9"), null)
        Assertions.assertEquals(func("print(\"::set-gate-value name=pass_rate::0.9\")"), null)
        /*多空格*/
        Assertions.assertEquals(func(" ::set-gate-value name=pass_rate::0.9"), null)
        Assertions.assertEquals(func("::set-gate-value   name=pass_rate::0.9"), null)
        /*windows兼容*/
        Assertions.assertEquals(func("\"::set-gate-value name=pass_rate::0.9\""), "pass_rate=0.9\n")
        /*默认*/
        Assertions.assertEquals(func("::set-gate-value name=pass_rate::0.9"), "pass_rate=0.9\n")
    }
}

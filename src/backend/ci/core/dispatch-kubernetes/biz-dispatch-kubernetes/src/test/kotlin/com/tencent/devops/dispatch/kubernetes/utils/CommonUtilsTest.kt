package com.tencent.devops.dispatch.kubernetes.utils

import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class CommonUtilsTest {
    @Test
    fun parseImage() {
        val (project, module, version) = CommonUtils.parseImage("bkci/ci:latest")
        Assertions.assertEquals(project, "bkci")
        Assertions.assertEquals(module, "ci")
        Assertions.assertEquals(version, "latest")
    }

    @Test
    fun onFailure() {
        Assertions.assertThrows(BuildFailureException::class.java) {
            CommonUtils.onFailure(
                errorType = ErrorType.PLUGIN,
                errorCode = 20001,
                message = "测试错误",
                formatErrorMessage = "<p>测试错误</p>"
            )
        }
    }
}

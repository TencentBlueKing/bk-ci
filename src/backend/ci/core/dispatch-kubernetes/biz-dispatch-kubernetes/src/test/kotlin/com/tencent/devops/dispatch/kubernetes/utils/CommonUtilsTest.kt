package com.tencent.devops.dispatch.kubernetes.utils

import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import org.junit.Assert
import org.junit.Test

class CommonUtilsTest {
    @Test
    fun parseImage() {
        val (project, module, version) = CommonUtils.parseImage("bkci/ci:latest")
        Assert.assertEquals(project, "bkci")
        Assert.assertEquals(module, "ci")
        Assert.assertEquals(version, "latest")
    }

    @Test(expected = BuildFailureException::class)
    fun onFailure() {
        CommonUtils.onFailure(
            errorType = ErrorType.PLUGIN,
            errorCode = 20001,
            message = "测试错误",
            formatErrorMessage = "<p>测试错误</p>"
        )
    }
}

package com.tencent.devops.scm.constant

object ScmCode {
    const val BK_FILE_CANNOT_EXCEED = "BkFileCannotExceed" // 请求文件不能超过1M
    const val BK_GIT_TOKEN_EMPTY = "BkGitTokenEmpty" // Git Token为空
    const val BK_INCORRECT_GIT_TOKEN = "BkIncorrectGitToken" // Git Token不正确
    const val BK_LOCAL_WAREHOUSE_CREATION_FAILED = "BkLocalWarehouseCreationFailed" //工程({0})本地仓库创建失败
    const val BK_TRIGGER_METHOD = "BkTriggerMethod" //触发方式
    const val BK_QUALITY_RED_LINE = "BkQualityRedLine" //质量红线
    const val BK_QUALITY_RED_LINE_OUTPUT = "BkQualityRedLineOutput" //质量红线产出插件
    const val BK_METRIC = "BkMetric" //质量红线产出插件
    const val BK_RESULT = "BkResult" //结果
    const val BK_EXPECT = "BkExpect" //预期
}
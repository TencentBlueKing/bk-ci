package com.tencent.devops.plugin.worker.task.codecc.util

import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.codecc.CodeccSDKApi
import com.tencent.devops.worker.common.logger.LoggerService
import java.io.File

class CodeccScriptUtils : AbstractBuildResourceApi() {

    private val api = ApiFactory.create(CodeccSDKApi::class)

    fun downloadScriptFile(codeccWorkspace: File): File {
        val result = api.getSingleCodeccScript()
        LoggerService.addNormalLine("get file content: $result")
        val map = result.data!!
        val file = File(codeccWorkspace, map["scriptName"]!!)
        file.writeText(map["script"]!!)
        return file
    }
}

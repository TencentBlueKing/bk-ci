package com.tencent.devops.plugin.worker.api

import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.common.api.pojo.Result
import okhttp3.Response

interface CodeccSDKApi {
    fun saveTask(projectId: String, pipelineId: String, buildId: String): Result<String>
    fun downloadTool(tool: String, osType: OSType, fileMd5: String, is32Bit: Boolean = false): Response
    fun downloadToolScript(osType: OSType, fileMd5: String): Response
}
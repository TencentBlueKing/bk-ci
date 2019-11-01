package com.tencent.devops.plugin.worker.api

import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import okhttp3.Protocol
import okhttp3.Response
import org.springframework.http.HttpStatus

class PluginCodeccResourceApi : AbstractBuildResourceApi(), CodeccSDKApi {

    override fun saveTask(projectId: String, pipelineId: String, buildId: String): Result<String> {
        val path = "/ms/plugin/api/build/codecc/save/task/$projectId/$pipelineId/$buildId"
        val request = buildPost(path)
        val responseContent = request(request, "保存CodeCC原子信息失败")
        return Result(responseContent)
    }

    override fun downloadTool(tool: String, osType: OSType, fileMd5: String, is32Bit: Boolean): Response {
        val path = "/ms/plugin/api/build/codecc/$tool?osType=${osType.name}&fileMd5=$fileMd5&is32Bit=$is32Bit"
        val request = buildGet(path)

        val response = requestForResponse(request)
        if (response.code() == HttpStatus.NOT_MODIFIED.value()) {
            return Response.Builder().request(request)
                .protocol(Protocol.HTTP_1_1)
                .message("")
                .code(HttpStatus.NOT_MODIFIED.value()).build()
        }
        if (!response.isSuccessful) {
            throw RemoteServiceException("下载Codecc的 $tool 工具失败")
        }
        return response
    }

    override fun downloadToolScript(osType: OSType, fileMd5: String): Response {
        val path = "/ms/plugin/api/build/codecc/tools/script?osType=${osType.name}&fileMd5=$fileMd5"
        val request = buildGet(path)
        val response = requestForResponse(request)
        if (response.code() == HttpStatus.NOT_MODIFIED.value()) {
            return Response.Builder().request(request)
                .protocol(Protocol.HTTP_1_1)
                .message("")
                .code(HttpStatus.NOT_MODIFIED.value()).build()
        }

        if (!response.isSuccessful) {
            throw RemoteServiceException("下载codecc的多工具执行脚本失败")
        }
        return response
    }
}

package com.tencent.devops.plugin.codecc.resources

import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.codecc.api.BuildCodeccResource
import com.tencent.devops.plugin.codecc.service.CodeccDownloaderService
import com.tencent.devops.plugin.codecc.service.CodeccService
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class BuildCodeccResourceImpl @Autowired constructor(
    private val codeccService: CodeccService,
    private val codeccDownloaderService: CodeccDownloaderService
) : BuildCodeccResource {

    override fun downloadTool(toolName: String, osType: OSType, fileMd5: String, is32Bit: Boolean?): Response {
        return codeccDownloaderService.downloadTool(toolName, osType, fileMd5, is32Bit)
    }

    override fun downloadToolsScript(osType: OSType, fileMd5: String): Response {
        return codeccDownloaderService.downloadToolsScript(osType, fileMd5)
    }

    override fun queryCodeccTaskDetailUrl(projectId: String, pipelineId: String, buildId: String): String {
        return codeccService.queryCodeccTaskDetailUrl(projectId, pipelineId, buildId)
    }

    override fun saveCodeccTask(projectId: String, pipelineId: String, buildId: String): Result<Int> {
        return Result(codeccService.saveCodeccTask(projectId, pipelineId, buildId))
    }
}
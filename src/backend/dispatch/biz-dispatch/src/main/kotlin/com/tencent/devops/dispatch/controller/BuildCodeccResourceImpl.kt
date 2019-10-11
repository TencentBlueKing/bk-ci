package com.tencent.devops.dispatch.controller

import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.BuildCodeccResource
import com.tencent.devops.dispatch.service.CodeccDownloaderService
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class BuildCodeccResourceImpl @Autowired constructor(
    private val codeccDownloaderService: CodeccDownloaderService
) : BuildCodeccResource {
    override fun downloadTool(toolName: String, osType: OSType, fileMd5: String, is32Bit: Boolean?): Response {
        return codeccDownloaderService.downloadTool(toolName, osType, fileMd5, is32Bit)
    }

    override fun downloadCovScript(osType: OSType, fileMd5: String): Response {
        return codeccDownloaderService.downloadCovScript(osType, fileMd5)
    }

    override fun downloadToolsScript(osType: OSType, fileMd5: String): Response {
        return codeccDownloaderService.downloadToolsScript(osType, fileMd5)
    }
}
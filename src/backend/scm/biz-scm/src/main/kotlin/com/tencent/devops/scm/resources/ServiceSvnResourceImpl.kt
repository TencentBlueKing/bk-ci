package com.tencent.devops.scm.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.scm.api.ServiceSvnResource
import com.tencent.devops.scm.pojo.SvnFileInfo
import com.tencent.devops.scm.services.SvnService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceSvnResourceImpl @Autowired constructor(
    private val svnService: SvnService
) : ServiceSvnResource {

    override fun getFileContent(
        url: String,
        userId: String,
        svnType: String,
        filePath: String,
        reversion: Long,
        credential1: String,
        credential2: String?
    ): Result<String> {
        return Result(svnService.getFileContent(url, userId, svnType, filePath, reversion, credential1, credential2))
    }

    override fun getDirectories(
        url: String,
        userId: String,
        svnType: String,
        svnPath: String?,
        revision: Long,
        credential1: String,
        credential2: String,
        credential3: String?
    ): Result<List<SvnFileInfo>> {
        return Result(svnService.getDirectories(url, userId, svnType, svnPath, revision, credential1, credential2, credential3))
    }
}
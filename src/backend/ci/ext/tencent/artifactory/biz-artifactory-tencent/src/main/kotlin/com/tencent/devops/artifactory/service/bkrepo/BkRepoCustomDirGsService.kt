package com.tencent.devops.artifactory.service.bkrepo

import com.tencent.devops.artifactory.client.BkRepoClient
import com.tencent.devops.artifactory.client.JFrogApiService
import com.tencent.devops.artifactory.service.JFrogService
import com.tencent.devops.artifactory.service.CustomDirGsService
import com.tencent.devops.artifactory.util.JFrogUtil
import com.tencent.devops.artifactory.util.RepoUtils
import com.tencent.devops.common.api.exception.OperationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.NotFoundException

@Service
class BkRepoCustomDirGsService @Autowired constructor(
    private val jFrogApiService: JFrogApiService,
    private val jFrogService: JFrogService,
    private val bkRepoClient: BkRepoClient,
    private val bkRepoService: BkRepoService
) : CustomDirGsService {
    override fun getDownloadUrl(projectId: String, fileName: String, userId: String): String {
        val path = JFrogUtil.getCustomDirPath(projectId, fileName)

        bkRepoClient.getFileDetail(userId, projectId, RepoUtils.CUSTOM_REPO, path)
            ?: throw NotFoundException("文件不存在")

        // todo
        throw OperationException("not implemented")

        // return bkRepoService.internalDownloadUrl(resultPath, 3*24*3600, userId)
    }
}
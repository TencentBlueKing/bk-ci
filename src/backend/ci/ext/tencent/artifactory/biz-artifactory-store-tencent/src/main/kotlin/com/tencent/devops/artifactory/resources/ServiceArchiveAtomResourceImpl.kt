package com.tencent.devops.artifactory.resources

import com.tencent.devops.artifactory.api.ServiceArchiveAtomResource
import com.tencent.devops.artifactory.config.BkRepoStoreConfig
import com.tencent.devops.artifactory.pojo.enums.BkRepoEnum
import com.tencent.devops.artifactory.service.ArchiveAtomService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.archive.client.BkRepoClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.RestController

@Primary
@RestController
class ServiceArchiveAtomResourceImpl @Autowired constructor(
    private val archiveAtomService: ArchiveAtomService,
    private val bkRepoClient: BkRepoClient,
    private val bkRepoStoreConfig: BkRepoStoreConfig
) : ServiceArchiveAtomResource {

    override fun getAtomFileContent(filePath: String): Result<String> {
        return Result(archiveAtomService.getAtomFileContent(filePath))
    }

    override fun deleteAtomFile(userId: String, projectCode: String, atomCode: String): Result<Boolean> {
        bkRepoClient.delete(userId, bkRepoStoreConfig.bkrepoStoreProjectName, BkRepoEnum.PLUGIN.repoName, atomCode)
        return Result(true)
    }
}

package com.tencent.devops.artifactory.service.impl

import com.tencent.devops.artifactory.config.BkRepoStoreConfig
import com.tencent.devops.artifactory.pojo.ArchiveAtomRequest
import com.tencent.devops.artifactory.pojo.ArchiveAtomResponse
import com.tencent.devops.artifactory.pojo.ReArchiveAtomRequest
import com.tencent.devops.artifactory.pojo.enums.BkRepoEnum
import com.tencent.devops.artifactory.service.ArchiveAtomService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.archive.client.BkRepoClient
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.io.InputStream

@Primary
@Service
class TxArchiveAtomServiceImpl(
    private val bkRepoClient: BkRepoClient,
    private val bkRepoStoreConfig: BkRepoStoreConfig
) : ArchiveAtomService {

    override fun archiveAtom(
        userId: String,
        inputStream: InputStream,
        disposition: FormDataContentDisposition,
        atomId: String,
        archiveAtomRequest: ArchiveAtomRequest
    ): Result<ArchiveAtomResponse?> {
        throw UnsupportedOperationException()
    }

    override fun reArchiveAtom(
        userId: String,
        inputStream: InputStream,
        disposition: FormDataContentDisposition,
        reArchiveAtomRequest: ReArchiveAtomRequest
    ): Result<ArchiveAtomResponse?> {
        throw UnsupportedOperationException()
    }

    override fun getAtomFileContent(filePath: String): String {
        throw UnsupportedOperationException()
    }

    override fun deleteAtom(userId: String, projectCode: String, atomCode: String) {
        bkRepoClient.delete(
            userId = bkRepoStoreConfig.bkrepoStoreUserName,
            projectId = bkRepoStoreConfig.bkrepoStoreProjectName,
            repoName = BkRepoEnum.PLUGIN.repoName,
            path = atomCode
        )
    }

    override fun updateArchiveFile(
        projectCode: String,
        atomCode: String,
        version: String,
        fileName: String,
        content: String
    ): Boolean {
        throw UnsupportedOperationException()
    }
}

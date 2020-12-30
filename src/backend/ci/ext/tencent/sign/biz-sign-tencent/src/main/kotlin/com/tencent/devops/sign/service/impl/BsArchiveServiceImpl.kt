package com.tencent.devops.sign.service.impl

import com.tencent.devops.common.archive.client.DirectBkRepoClient
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.service.ArchiveService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File

@Service
class BsArchiveServiceImpl @Autowired constructor(
    private val directBkRepoClient: DirectBkRepoClient
) : ArchiveService {

    override fun archive(signedIpaFile: File, ipaSignInfo: IpaSignInfo, properties: Map<String, String>?): Boolean {
        logger.info("archive, signedIpaFile: ${signedIpaFile.absolutePath}, ipaSignInfo: $ipaSignInfo, properties: $properties")
        val path = if (ipaSignInfo.archiveType.toLowerCase() == "pipeline") {
            "${ipaSignInfo.pipelineId}/${ipaSignInfo.buildId}/${signedIpaFile.name}"
        } else {
            "${ipaSignInfo.archivePath}/${signedIpaFile.name}"
        }
        directBkRepoClient.uploadLocalFile(
            userId = ipaSignInfo.userId,
            projectId = ipaSignInfo.projectId,
            repoName = ipaSignInfo.archiveType.toLowerCase(),
            path = path,
            file = signedIpaFile,
            metadata = properties ?: mapOf(),
            override = true
        )
        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BsArchiveServiceImpl::class.java)
    }
}
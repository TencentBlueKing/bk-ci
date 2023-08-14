package com.tencent.devops.artifactory.resources

import com.tencent.devops.artifactory.api.user.UserArtifactoryResource
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.SearchProps
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.pojo.enums.FileChannelTypeEnum
import com.tencent.devops.artifactory.service.ArchiveFileService
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import org.springframework.beans.factory.annotation.Autowired

class UserArtifactoryResourceImpl @Autowired constructor(
    private val archiveFileService: ArchiveFileService
) : UserArtifactoryResource {

    override fun search(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        searchProps: SearchProps
    ): Result<Page<FileInfo>> {
        return Result(archiveFileService.searchFileList(
            userId = userId,
            projectId = projectId,
            page = page,
            pageSize = pageSize,
            searchProps = searchProps
        ))
    }

    override fun show(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<FileDetail> {
        return Result(archiveFileService.show(
            userId = userId,
            projectId = projectId,
            artifactoryType = artifactoryType,
            path = path
        ))
    }

    override fun downloadUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<Url> {
        val urls = archiveFileService.getFileDownloadUrls(
            userId = userId,
            projectId = projectId,
            fileChannelType = FileChannelTypeEnum.WEB_DOWNLOAD,
            filePath = path,
            artifactoryType = artifactoryType,
            fullUrl = false
        )
        return Result(Url(url = urls.fileUrlList[0], url2 = urls.fileUrlList[0]))
    }
}

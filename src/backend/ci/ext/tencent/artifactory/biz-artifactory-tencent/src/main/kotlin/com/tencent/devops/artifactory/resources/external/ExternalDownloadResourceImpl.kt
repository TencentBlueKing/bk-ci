package com.tencent.devops.artifactory.resources.external

import com.tencent.devops.artifactory.api.external.ExternalDownloadResource
import com.tencent.devops.artifactory.pojo.HapJson5Info
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.service.bkrepo.BkRepoDownloadService
import com.tencent.devops.common.api.constant.CommonMessageCode.FILE_NOT_EXIST
import com.tencent.devops.common.api.enums.PlatformEnum
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import javax.ws.rs.BadRequestException
import javax.ws.rs.NotFoundException
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ExternalDownloadResourceImpl @Autowired constructor(
    private val bkRepoDownloadService: BkRepoDownloadService
) : ExternalDownloadResource {
    override fun getHapJson5(projectId: String, artifactoryType: ArtifactoryType, token: String): String {
        val hapJson5Info = bkRepoDownloadService.getObjectByToken(token, HapJson5Info::class.java)
            ?: throw NotFoundException(
                I18nUtil.getCodeLanMessage(
                    messageCode = FILE_NOT_EXIST,
                    params = arrayOf(token)
                )
            )
        val userId = hapJson5Info.userId
        val path = hapJson5Info.filePath
        val ttl = hapJson5Info.ttl
        val experienceHashId = hapJson5Info.experienceHashId
        val organization = hapJson5Info.organization

        if (PlatformEnum.ofTail(path) != PlatformEnum.HAP) {
            throw BadRequestException("Path must end with ${PlatformEnum.HAP.tails}")
        }
        return bkRepoDownloadService.outerHapJson5Content(
            userId = userId,
            projectId = projectId,
            artifactoryType = artifactoryType,
            argPath = path,
            ttl = ttl,
            experienceHashId = experienceHashId,
            organization = organization
        )
    }
}
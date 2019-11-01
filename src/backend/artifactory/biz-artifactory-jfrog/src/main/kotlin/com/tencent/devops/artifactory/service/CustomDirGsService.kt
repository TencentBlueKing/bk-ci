package com.tencent.devops.artifactory.service

import com.tencent.devops.artifactory.util.JFrogUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.NotFoundException

@Service
class CustomDirGsService @Autowired constructor(
    private val jFrogApiService: JFrogApiService,
    private val jFrogService: JFrogService
) {
    fun getDownloadUrl(projectId: String, fileName: String, userId: String): String {
        val path = JFrogUtil.getCustomDirPath(projectId, fileName)

        if (!jFrogService.exist(path)) {
            throw NotFoundException("文件不存在")
        }

        return jFrogApiService.internalDownloadUrl(path, 3*24*3600, userId)
    }
}
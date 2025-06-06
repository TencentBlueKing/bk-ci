/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.remotedev.service.projectworkspace.image

import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditAttribute
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.audit.TencentActionAuditContent
import com.tencent.devops.common.auth.api.TencentActionId
import com.tencent.devops.common.auth.api.TencentResourceTypeId
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.remotedev.dao.ImageManageDao
import com.tencent.devops.remotedev.dao.WindowsResourceZoneDao
import com.tencent.devops.remotedev.dispatch.kubernetes.interfaces.ServiceStartCloudInterface
import com.tencent.devops.remotedev.dispatch.kubernetes.service.factory.RemoteDevServiceFactory
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.image.DeleteImageResp
import com.tencent.devops.remotedev.pojo.image.ImageStatus
import com.tencent.devops.remotedev.pojo.image.ListImagesData
import com.tencent.devops.remotedev.pojo.image.ListImagesResp
import com.tencent.devops.remotedev.pojo.image.ProjectImage
import com.tencent.devops.remotedev.pojo.image.StandardVmImage
import com.tencent.devops.remotedev.service.PermissionService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ImageManageService @Autowired constructor(
    private val dslContext: DSLContext,
    private val imageManageDao: ImageManageDao,
    private val windowsResourceZoneDao: WindowsResourceZoneDao,
    private val permissionService: PermissionService,
    private val remoteDevServiceFactory: RemoteDevServiceFactory
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ImageManageService::class.java)
    }

    // 获取工作空间模板
    fun getProjectImageList(projectId: String, imageId: String?): List<ProjectImage> {
        logger.info("ImageManageService|getProjectImageList|projectId|$projectId")
        val result = mutableListOf<ProjectImage>()
        imageManageDao.queryImageList(
            projectId = projectId,
            dslContext = dslContext,
            imageId = imageId
        ).forEach {
            val sourceCgsZoneShortName = it.sourceCgsZone.replace(Regex("[^a-zA-Z]"), "")
            val sourceCgsZoneName = windowsResourceZoneDao.fetchAny(dslContext, sourceCgsZoneShortName)
            result.add(
                ProjectImage(
                    id = it.id,
                    projectId = it.projectId,
                    imageName = it.imageName,
                    imageId = it.imageId,
                    imageCosFile = it.imageCosFile,
                    size = it.size,
                    sourceCgsId = it.sourceCgsId,
                    sourceCgsType = it.sourceCgsType,
                    sourceCgsZone = it.sourceCgsZone,
                    sourceCgsZoneShortName = sourceCgsZoneShortName,
                    sourceCgsZoneName = sourceCgsZoneName?.zone ?: "",
                    creator = it.creator,
                    status = ImageStatus.values()[it.status],
                    createdTime = it.createTime.timestamp(),
                    errMsg = it.errorMsg
                )
            )
        }
        return result
    }

    @ActionAuditRecord(
        actionId = TencentActionId.IMAGE_DELETE,
        instance = AuditInstanceRecord(
            resourceType = TencentResourceTypeId.IMAGE,
            instanceNames = "#imageId",
            instanceIds = "#imageId"
        ),
        attributes = [AuditAttribute(name = TencentActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = TencentActionAuditContent.IMAGE_DELETE_CONTENT
    )
    fun deleteProjectImage(userId: String, projectId: String, imageId: String): Boolean {
        logger.info("$userId delete projectImage: $imageId")
        permissionService.checkUserManager(userId, projectId)
        imageManageDao.updateWorkspaceImageStatus(projectId, imageId, ImageStatus.DELETED, dslContext)
        return true
    }

    fun getVmStandardImages(): List<StandardVmImage> {
        return kotlin.runCatching {
            SpringContextUtil.getBean(ServiceStartCloudInterface::class.java).getVmStandardImages().data
        }.onFailure {
            logger.warn("Error get vm stanadard image list: ${it.message}")
        }.getOrNull() ?: emptyList()
    }

    fun updateImageName(id: Long, imageName: String) {
        imageManageDao.updateImageName(dslContext, id, imageName)
    }

    // 校验是否基础镜像
    fun checkBaseImage(imageCosFile: String): Boolean {
        return getVmStandardImages().any { it.cosFile == imageCosFile }
    }

    // 校验项目下是否有该镜像:true表示存在；false表示不存在
    fun checkProjectImage(projectId: String, imageCosFile: String): Boolean {
        return imageManageDao.queryImageList(
            projectId = projectId,
            dslContext = dslContext,
            imageId = null,
            imageCosfile = imageCosFile
        ).isNotEmpty
    }

    fun fetchImages(
        userId: String,
        data: ListImagesData
    ): ListImagesResp? {
        permissionService.checkUserManager(userId, data.projectId)
        return remoteDevServiceFactory.loadRemoteDevService(WorkspaceMountType.BCS).fetchImages(data)
    }

    fun deleteImage(
        userId: String,
        projectId: String,
        imageId: String,
        delaySeconds: Int?
    ): DeleteImageResp {
        permissionService.checkUserManager(userId, projectId)
        val taskId = remoteDevServiceFactory.loadRemoteDevService(WorkspaceMountType.BCS).deleteImage(
            imageId = imageId,
            delaySeconds = delaySeconds
        )
        return DeleteImageResp(taskId = taskId)
    }
}

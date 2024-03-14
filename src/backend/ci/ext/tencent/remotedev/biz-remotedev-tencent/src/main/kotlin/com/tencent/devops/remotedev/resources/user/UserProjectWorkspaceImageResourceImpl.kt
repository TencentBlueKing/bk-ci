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

package com.tencent.devops.remotedev.resources.user

import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.user.UserProjectWorkspaceImageResource
import com.tencent.devops.remotedev.pojo.image.ProjectImage
import com.tencent.devops.remotedev.pojo.image.StandardVmImage
import com.tencent.devops.remotedev.pojo.image.UpdateImageNameInfo
import com.tencent.devops.remotedev.service.projectworkspace.image.ImageManageService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
@Suppress("ALL")
class UserProjectWorkspaceImageResourceImpl @Autowired constructor(
    private val projectImageManageService: ImageManageService
) : UserProjectWorkspaceImageResource {
    companion object {
        val logger = LoggerFactory.getLogger(UserProjectWorkspaceImageResourceImpl::class.java)!!
    }

    override fun getProjectImageList(userId: String, projectId: String): Result<List<ProjectImage>> {
        logger.info("UserImageManageResourceImpl|getProjectImageList|userId|$userId|projectId|$projectId")
        return Result(projectImageManageService.getProjectImageList(projectId))
    }

    @AuditEntry(actionId = ActionId.IMAGE_DELETE)
    override fun deleteProjectImage(userId: String, projectId: String, imageId: String): Result<Boolean> {
        return Result(projectImageManageService.deleteProjectImage(userId, projectId, imageId))
    }

    override fun getVmStandardImages(userId: String, projectId: String): Result<List<StandardVmImage>> {
        logger.info("UserImageManageResourceImpl|getProjectImageList|userId|$userId|projectId|$projectId")
        return Result(projectImageManageService.getVmStandardImages())
    }

    override fun updateImageName(userId: String, projectId: String, data: UpdateImageNameInfo): Result<Boolean> {
        projectImageManageService.updateImageName(data.id, data.imageName)
        return Result(true)
    }
}

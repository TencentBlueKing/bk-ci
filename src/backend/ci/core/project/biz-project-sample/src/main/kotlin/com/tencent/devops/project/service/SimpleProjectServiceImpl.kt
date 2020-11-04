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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.project.service

import com.tencent.devops.artifactory.api.service.ServiceFileResource
import com.tencent.devops.artifactory.pojo.enums.FileChannelTypeEnum
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.dispatch.ProjectDispatcher
import com.tencent.devops.project.jmx.api.ProjectJmxApi
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.service.impl.AbsProjectServiceImpl
import com.tencent.devops.project.util.ImageUtil
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File

@Service
class SimpleProjectServiceImpl @Autowired constructor(
    projectPermissionService: ProjectPermissionService,
    private val dslContext: DSLContext,
    private val projectDao: ProjectDao,
    projectJmxApi: ProjectJmxApi,
    redisOperation: RedisOperation,
    gray: Gray,
    client: Client,
    projectDispatcher: ProjectDispatcher
) : AbsProjectServiceImpl(projectPermissionService, dslContext, projectDao, projectJmxApi, redisOperation, gray, client, projectDispatcher) {

    override fun getDeptInfo(userId: String): UserDeptDetail {
        return UserDeptDetail(
                bgName = "",
                bgId = "1",
                centerName = "",
                centerId = "1",
                deptName = "",
                deptId = "1",
                groupId = "0",
                groupName = ""
        )
    }

    override fun createExtProjectInfo(userId: String, projectId: String, accessToken: String?, projectCreateInfo: ProjectCreateInfo, isUserProject: Boolean?) {
        return
    }

    override fun saveLogoAddress(userId: String, projectCode: String, logoFile: File): String {
        // 保存Logo文件
        val serviceUrlPrefix = client.getServiceUrl(ServiceFileResource::class)
        val result =
                CommonUtils.serviceUploadFile(userId, serviceUrlPrefix, logoFile, FileChannelTypeEnum.WEB_SHOW.name)
        if (result.isNotOk()) {
            throw OperationException("${result.status}:${result.message}")
        }
        return result.data!!
    }

    override fun deleteAuth(projectId: String, accessToken: String?) {
        projectPermissionService.deleteResource(projectId)
    }

    override fun getProjectFromAuth(userId: String?, accessToken: String?): Set<String> {
        return projectPermissionService.getUserProjects(userId!!).toSet()
    }

    override fun updateInfoReplace(projectUpdateInfo: ProjectUpdateInfo) {
        return
    }

    override fun drawFile(projectCode: String): File {
        // 随机生成首字母图片
        val firstChar = projectCode.substring(0, 1).toUpperCase()
        return ImageUtil.drawImage(
                firstChar,
                Width,
                Height
        )
    }

    override fun validatePermission(projectCode: String, userId: String, permission: AuthPermission): Boolean {
        val validate = projectPermissionService.verifyUserProjectPermission(
                projectCode = projectCode,
                userId = userId,
                permission = permission
        )
        if (!validate) {
            logger.warn("$projectCode| $userId| ${permission.value} validatePermission fail")
            throw PermissionForbiddenException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PEM_CHECK_FAIL))
        }
        return true
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}

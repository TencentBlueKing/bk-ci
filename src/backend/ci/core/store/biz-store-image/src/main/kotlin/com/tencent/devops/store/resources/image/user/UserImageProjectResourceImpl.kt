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
package com.tencent.devops.store.resources.image.user

import com.tencent.devops.common.api.constant.CommonMessageCode.PERMISSION_DENIED
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.image.user.UserImageProjectResource
import com.tencent.devops.store.pojo.image.request.InstallImageReq
import com.tencent.devops.store.pojo.image.response.ImageDetail
import com.tencent.devops.store.pojo.image.response.ProjectSimpleInfo
import com.tencent.devops.store.service.image.ImageProjectService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserImageProjectResourceImpl @Autowired constructor(
    private val imageProjectService: ImageProjectService
) : UserImageProjectResource {

    override fun installImage(accessToken: String, userId: String, installImageReq: InstallImageReq): Result<Boolean> {
        return imageProjectService.installImage(
            userId = userId,
            projectCodeList = installImageReq.projectCodeList,
            imageCode = installImageReq.imageCode,
            channelCode = ChannelCode.BS,
            interfaceName = "/user/market/image/install"
        )
    }

    override fun getInstalledProjects(accessToken: String, imageCode: String): Result<List<ProjectSimpleInfo?>> {
        return imageProjectService.getInstalledProjects(
            accessToken,
            imageCode,
            interfaceName = "/user/market/image/install"
        )
    }

    override fun getAvailableImagesByProjectCode(
        accessToken: String,
        userId: String,
        projectCode: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<ImageDetail?>?> {
        try {
            return Result(
                imageProjectService.getAvailableImagesByProjectCode(
                    accessToken = accessToken,
                    userId = userId,
                    projectCode = projectCode,
                    page = page,
                    pageSize = pageSize,
                    interfaceName = "/user/market/image/availableImages,get"
                )
            )
        } catch (e: PermissionForbiddenException) {
            return MessageCodeUtil.generateResponseDataObject(PERMISSION_DENIED, arrayOf(userId, projectCode))
        }
    }

    override fun getMarketImagesByProjectCode(
        accessToken: String,
        userId: String,
        projectCode: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<ImageDetail?>?> {
        try {
            return Result(
                imageProjectService.getMarketImagesByProjectCode(
                    accessToken = accessToken,
                    userId = userId,
                    projectCode = projectCode,
                    page = page,
                    pageSize = pageSize,
                    interfaceName = "/user/market/image/marketImages,get"
                )
            )
        } catch (e: PermissionForbiddenException) {
            return MessageCodeUtil.generateResponseDataObject(PERMISSION_DENIED, arrayOf(userId, projectCode))
        }
    }

    override fun searchMarketImages(
        accessToken: String,
        userId: String,
        projectCode: String,
        imageNamePart: String?,
        classifyCodeList: List<String>?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<ImageDetail?>?> {
        try {
            return Result(
                imageProjectService.searchMarketImages(
                    accessToken = accessToken,
                    userId = userId,
                    projectCode = projectCode,
                    imageNamePart = imageNamePart,
                    classifyCodeList = classifyCodeList,
                    page = page,
                    pageSize = pageSize,
                    interfaceName = "/user/market/image/marketImages/search,get"
                )
            )
        } catch (e: PermissionForbiddenException) {
            return MessageCodeUtil.generateResponseDataObject(PERMISSION_DENIED, arrayOf(userId, projectCode))
        }
    }
}
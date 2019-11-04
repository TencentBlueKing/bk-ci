/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

package com.tencent.devops.dispatch.service

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.dispatch.exception.ImageNotInstalledException
import com.tencent.devops.dispatch.exception.UnknownImageSourceType
import com.tencent.devops.store.api.image.service.ServiceImageResource
import com.tencent.devops.store.constant.StoreMessageCode.USER_IMAGE_NOT_INSTALLED
import com.tencent.devops.store.constant.StoreMessageCode.USER_IMAGE_UNKNOWN_SOURCE_TYPE
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * @Description
 * @Date 2019/10/17
 * @Version 1.0
 */

@Service
class StoreImageService @Autowired constructor(
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(StoreImageService::class.java)
    }

    // 从镜像商店获取完整的镜像名称
    fun getCompleteImageName(
        userId: String,
        projectId: String,
        imageCode: String?,
        imageVersion: String?,
        defaultPrefix: String?
    ): String {
        logger.info("getCompleteImageName:Input($userId,$projectId,$imageCode,$imageVersion)")
        // 鉴权：安装了才有权限使用
        if (null == imageCode) {
            throw InvalidParamException("Input:($userId,$projectId,$imageCode),imageCode is null")
        }
        val permissionResult = client.get(ServiceImageResource::class).isInstalled(userId, projectId, imageCode)
        if (permissionResult.isNotOk() || (!permissionResult.data!!)) {
            throw ImageNotInstalledException("Input:($userId,$projectId,$imageCode)", USER_IMAGE_NOT_INSTALLED)
        }
        // 调商店接口获取镜像信息
        val result = client.get(ServiceImageResource::class)
            .getImageDetailByCodeAndVersion(
                userId = userId,
                projectCode = projectId,
                imageCode = imageCode,
                imageVersion = imageVersion
            )
        val imageDetail = result.data!!
        var completeImageName = ""
        val cleanImageRepoUrl = imageDetail.imageRepoUrl.trimEnd { ch ->
            ch == '/'
        }
        val cleanImageRepoName = imageDetail.imageRepoName.trimStart { ch ->
            ch == '/'
        }
        if (ImageType.BKDEVOPS.name == imageDetail.imageSourceType || ImageType.THIRD.name == imageDetail.imageSourceType) {
            //蓝盾项目源镜像或第三方源镜像
            completeImageName = "${cleanImageRepoUrl}/${cleanImageRepoName}"
        } else {
            throw UnknownImageSourceType(
                "imageId=${imageDetail.id},imageSourceType=${imageDetail.imageSourceType}",
                USER_IMAGE_UNKNOWN_SOURCE_TYPE
            )
        }
        completeImageName += if (!imageDetail.imageTag.isBlank()) {
            ":" + imageDetail.imageTag
        } else {
            ":latest"
        }
        logger.info("getCompleteImageName:Output($completeImageName)")
        return completeImageName
    }
}
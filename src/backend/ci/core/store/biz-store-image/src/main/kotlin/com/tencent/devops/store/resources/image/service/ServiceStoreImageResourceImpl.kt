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
package com.tencent.devops.store.resources.image.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.image.service.ServiceStoreImageResource
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.image.response.ImageRepoInfo
import com.tencent.devops.store.service.common.StoreProjectService
import com.tencent.devops.store.service.image.ImageFeatureService
import com.tencent.devops.store.service.image.ImageService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceStoreImageResourceImpl @Autowired constructor(
    private val imageService: ImageService,
    private val imageFeatureService: ImageFeatureService,
    private val storeProjectService: StoreProjectService
) : ServiceStoreImageResource {
    override fun getSelfDevelopPublicImages(): Result<List<ImageRepoInfo>> {
        return Result(
            imageService.getSelfDevelopPublicImages(
                interfaceName = "/service/market/image/self_develop/public_images"
            )
        )
    }

    override fun getImageStatusByCodeAndVersion(
        imageCode: String,
        imageVersion: String
    ): Result<String> {
        return Result(
            imageService.getImageStatusByCodeAndVersion(
                imageCode = imageCode,
                imageVersion = imageVersion
            )
        )
    }

    override fun isInstalled(userId: String, projectCode: String, imageCode: String): Result<Boolean> {
        return Result(
            // 公共镜像视为默认安装
            imageFeatureService.isImagePublic(imageCode) ||
                storeProjectService.isInstalledByProject(
                    projectCode = projectCode,
                    storeCode = imageCode,
                    storeType = StoreTypeEnum.IMAGE.type.toByte()
                )
        )
    }

    override fun getImageRepoInfoByCodeAndVersion(
        userId: String,
        projectCode: String,
        imageCode: String,
        imageVersion: String?,
        pipelineId: String?,
        buildId: String?
    ): Result<ImageRepoInfo> {
        return Result(
            imageService.getImageRepoInfoByCodeAndVersion(
                userId = userId,
                projectCode = projectCode,
                pipelineId = pipelineId,
                buildId = buildId,
                imageCode = imageCode,
                imageVersion = imageVersion,
                interfaceName = "/image/imageCodes/{imageCode}/imageVersions/{imageVersion}"
            )
        )
    }
}

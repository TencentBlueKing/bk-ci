/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.store.image.resources

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.image.UserMarketImageResource
import com.tencent.devops.store.pojo.common.version.VersionInfo
import com.tencent.devops.store.pojo.image.enums.ImageRDTypeEnum
import com.tencent.devops.store.pojo.image.enums.MarketImageSortTypeEnum
import com.tencent.devops.store.pojo.image.request.ImageBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.image.response.ImageDetail
import com.tencent.devops.store.pojo.image.response.MarketImageMain
import com.tencent.devops.store.pojo.image.response.MarketImageResp
import com.tencent.devops.store.pojo.image.response.MyImage
import com.tencent.devops.store.image.service.ImageService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserMarketImageResourceImpl @Autowired constructor(
    private val imageService: ImageService
) : UserMarketImageResource {
    override fun getPipelineImageVersions(projectCode: String, imageCode: String): Result<List<VersionInfo>> {
        return Result(imageService.getPipelineImageVersions(projectCode, imageCode))
    }

    override fun delete(userId: String, imageCode: String): Result<Boolean> {
        return imageService.delete(
            userId = userId,
            imageCode = imageCode,
            interfaceName = "/user/market/image/imageCodes/{imageCode},delete"
        )
    }

    override fun updateImageBaseInfo(
        userId: String,
        imageCode: String,
        imageBaseInfoUpdateRequest: ImageBaseInfoUpdateRequest
    ): Result<Boolean> {
        // 用户不可更新镜像大小信息
        imageBaseInfoUpdateRequest.imageSize = null
        return imageService.updateImageBaseInfo(
            userId = userId,
            imageCode = imageCode,
            imageBaseInfoUpdateRequest = imageBaseInfoUpdateRequest,
            interfaceName = "/user/market/baseInfo/images/{imageCode},put"
        )
    }

    override fun getImageVersionListByCode(
        userId: String,
        imageCode: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<ImageDetail>> {
        return imageService.getImageVersionListByCode(
            userId = userId,
            imageCode = imageCode,
            page = page,
            pageSize = pageSize,
            interfaceName = "/user/market/image/imageCodes/{imageCode}/version/list"
        )
    }

    override fun searchImage(
        userId: String,
        keyword: String?,
        imageSourceType: ImageType?,
        classifyCode: String?,
        categoryCode: String?,
        rdType: ImageRDTypeEnum?,
        labelCode: String?,
        score: Int?,
        sortType: MarketImageSortTypeEnum?,
        recommendFlag: Boolean?,
        page: Int?,
        pageSize: Int?
    ): Result<MarketImageResp> {
        return imageService.searchImage(
            userId = userId,
            keyword = keyword,
            imageSourceType = imageSourceType,
            classifyCode = classifyCode,
            categoryCode = categoryCode,
            rdType = rdType,
            labelCode = labelCode,
            score = score,
            sortType = sortType,
            recommendFlag = recommendFlag,
            page = page,
            pageSize = pageSize,
            interfaceName = "/user/market/image/list"
        )
    }

    override fun mainPageList(userId: String, page: Int?, pageSize: Int?): Result<List<MarketImageMain>> {
        return imageService.mainPageList(
            userId = userId,
            page = page,
            pageSize = pageSize,
            interfaceName = "/user/market/image/list/main"
        )
    }

    override fun getMyImageList(
        userId: String,
        imageName: String?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<MyImage>> {
        return imageService.getMyImageList(
            userId = userId,
            imageName = imageName,
            page = page,
            pageSize = pageSize,
            interfaceName = "/user/market/desk/image/list"
        )
    }

    override fun getImageDetailById(userId: String, imageId: String): Result<ImageDetail> {
        return Result(
            imageService.getImageDetailById(
                userId = userId,
                imageId = imageId,
                interfaceName = "/user/market/image/imageIds/{imageId}"
            )
        )
    }

    override fun getImageDetailByCode(userId: String, imageCode: String): Result<ImageDetail> {
        return Result(
            imageService.getLatestImageDetailByCode(
                userId = userId,
                imageCode = imageCode,
                interfaceName = "/user/market/image/imageCodes/{imageCode}"
            )
        )
    }
}

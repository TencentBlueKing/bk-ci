package com.tencent.devops.store.resources.image.user

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.image.user.UserImageResource
import com.tencent.devops.store.pojo.common.VersionInfo
import com.tencent.devops.store.pojo.image.enums.ImageRDTypeEnum
import com.tencent.devops.store.pojo.image.request.ImageBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.image.response.ImageDetail
import com.tencent.devops.store.pojo.image.response.MarketImageMain
import com.tencent.devops.store.pojo.image.response.MarketImageResp
import com.tencent.devops.store.pojo.image.response.MyImage
import com.tencent.devops.store.service.image.ImageService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserImageResourceImpl @Autowired constructor(
    private val imageService: ImageService
) : UserImageResource {
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
        imageName: String?,
        imageSourceType: ImageType?,
        classifyCode: String?,
        categoryCode: String?,
        rdType: ImageRDTypeEnum?,
        labelCode: String?,
        score: Int?,
        sortType: String?,
        page: Int?,
        pageSize: Int?
    ): Result<MarketImageResp> {
        return imageService.searchImage(
            userId = userId,
            imageName = imageName,
            imageSourceType = imageSourceType,
            classifyCode = classifyCode,
            categoryCode = categoryCode,
            rdType = rdType,
            labelCode = labelCode,
            score = score,
            sortType = sortType,
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

    override fun getMyImageList(userId: String, imageName: String?, page: Int?, pageSize: Int?): Result<Page<MyImage>> {
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
            imageService.getImageDetailByCode(
                userId = userId,
                imageCode = imageCode,
                interfaceName = "/user/market/image/imageCodes/{imageCode}"
            )
        )
    }
}
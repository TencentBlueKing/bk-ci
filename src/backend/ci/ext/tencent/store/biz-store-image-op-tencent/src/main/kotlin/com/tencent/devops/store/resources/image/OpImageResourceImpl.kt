package com.tencent.devops.store.resources.image

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.image.pojo.DockerRepo
import com.tencent.devops.store.api.image.OpImageResource
import com.tencent.devops.store.pojo.common.StoreVisibleDeptResp
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.image.request.ApproveImageReq
import com.tencent.devops.store.pojo.image.request.ImageCreateRequest
import com.tencent.devops.store.pojo.image.request.ImageUpdateRequest
import com.tencent.devops.store.pojo.image.request.OpImageSortTypeEnum
import com.tencent.devops.store.pojo.image.response.ImageDetail
import com.tencent.devops.store.pojo.image.response.OpImageResp
import com.tencent.devops.store.service.common.StoreVisibleDeptService
import com.tencent.devops.store.service.image.ImageReleaseService
import com.tencent.devops.store.service.image.ImageService
import com.tencent.devops.store.service.image.OpImageService
import com.tencent.devops.store.service.image.impl.TxImageRepoService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpImageResourceImpl @Autowired constructor(
    private val imageService: ImageService,
    private val imageReleaseService: ImageReleaseService,
    private val imageRepoService: TxImageRepoService,
    private val opImageService: OpImageService,
    private val storeVisibleDeptService: StoreVisibleDeptService
) : OpImageResource {

    override fun addImage(
        accessToken: String,
        userId: String,
        imageCreateRequest: ImageCreateRequest
    ): Result<String> {
        return opImageService.addImage(accessToken, userId, imageCreateRequest)
    }

    override fun passTest(userId: String, imageId: String): Result<Boolean> {
        return imageReleaseService.passTest(userId, imageId, false)
    }

    override fun recheck(userId: String, imageId: String): Result<Boolean> {
        return imageReleaseService.recheck(userId, imageId, false)
    }

    override fun updateImage(userId: String, imageId: String, imageUpdateRequest: ImageUpdateRequest): Result<Boolean> {
        return imageService.update(
            userId = userId,
            imageId = imageId,
            imageUpdateRequest = imageUpdateRequest,
            interfaceName = "/op/pipeline/image/{imageId},update"
        )
    }

    override fun deleteImageById(userId: String, imageId: String): Result<Boolean> {
        return imageService.deleteById(
            userId = userId,
            imageId = imageId,
            interfaceName = "/op/pipeline/image/imageIds/{imageId},delete"
        )
    }

    override fun getImageById(userId: String, imageId: String): Result<ImageDetail> {
        return Result(
            imageService.getImageDetailById(
                userId = userId,
                imageId = imageId,
                interfaceName = "/op/pipeline/image/images/{imageId},get"
            )
        )
    }

    override fun getImagesByCode(userId: String, imageCode: String, version: String?): Result<ImageDetail> {
        return Result(
            imageService.getImageDetailByCode(
                userId = userId,
                imageCode = imageCode,
                interfaceName = "/op/pipeline/image/imageCodes/{imageCode},get"
            )
        )
    }

    override fun getImageVersionsByCode(
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
            interfaceName = "/op/pipeline/image/imageCodes/{imageCode}/versions/list"
        )
    }

    override fun offlineImage(userId: String, imageCode: String, version: String?, reason: String?): Result<Boolean> {
        return imageReleaseService.offlineMarketImage(
            userId = userId,
            imageCode = imageCode,
            version = version,
            reason = reason,
            validateUserFlag = false,
            interfaceName = "/op/pipeline/image/offline/imageCodes/{imageCode}/versions"
        )
    }

    override fun listImages(
        userId: String,
        imageName: String?,
        imageSourceType: ImageType?,
        processFlag: Boolean?,
        classifyCode: String?,
        categoryCodes: Set<String>?,
        labelCodes: Set<String>?,
        sortType: OpImageSortTypeEnum?,
        desc: Boolean?,
        page: Int?,
        pageSize: Int?
    ): Result<OpImageResp> {
        return opImageService.list(
            userId = userId,
            imageName = imageName,
            imageSourceType = imageSourceType,
            processFlag = processFlag,
            classifyCode = classifyCode,
            categoryCodes = categoryCodes,
            labelCodes = labelCodes,
            sortType = sortType,
            desc = desc,
            page = page,
            pageSize = pageSize,
            interfaceName = "/op/pipeline/image/"
        )
    }

    override fun approveImage(userId: String, imageId: String, approveImageReq: ApproveImageReq): Result<Boolean> {
        return opImageService.approveImage(userId, imageId, approveImageReq)
    }

    override fun getVisibleDept(userId: String, imageCode: String): Result<StoreVisibleDeptResp?> {
        return storeVisibleDeptService.getVisibleDept(
            storeCode = imageCode,
            storeType = StoreTypeEnum.IMAGE,
            deptStatus = null
        )
    }

    override fun getBkRelImageInfo(userId: String, imageRepoName: String, imageId: String?): Result<DockerRepo?> {
        return imageRepoService.getBkRelImageInfo(userId, imageRepoName, imageId)
    }
}
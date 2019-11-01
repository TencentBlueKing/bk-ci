package com.tencent.devops.store.resources.image

import com.tencent.devops.common.api.constant.CommonMessageCode.PERMISSION_DENIED
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.image.UserImageProjectResource
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
            accessToken = accessToken,
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
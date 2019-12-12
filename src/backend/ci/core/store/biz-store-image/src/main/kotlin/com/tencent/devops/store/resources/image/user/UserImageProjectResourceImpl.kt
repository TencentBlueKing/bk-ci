package com.tencent.devops.store.resources.image.user

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.image.user.UserImageProjectResource
import com.tencent.devops.store.pojo.common.InstalledProjRespItem
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageAgentTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageRDTypeEnum
import com.tencent.devops.store.pojo.image.request.InstallImageReq
import com.tencent.devops.store.pojo.image.response.JobImageItem
import com.tencent.devops.store.pojo.image.response.JobMarketImageItem
import com.tencent.devops.store.service.common.StoreProjectService
import com.tencent.devops.store.service.image.ImageProjectService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserImageProjectResourceImpl @Autowired constructor(
    private val imageProjectService: ImageProjectService,
    private val storeProjectService: StoreProjectService
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

    override fun getInstalledProjects(accessToken: String, userId: String, imageCode: String): Result<List<InstalledProjRespItem>> {
        return storeProjectService.getInstalledProjects(accessToken, userId, imageCode, StoreTypeEnum.IMAGE)
    }

    override fun getAvailableImagesByProjectCode(
        accessToken: String,
        userId: String,
        projectCode: String,
        agentType: ImageAgentTypeEnum?,
        recommendFlag: Boolean?,
        classifyId: String?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<JobImageItem>?> {
        return Result(
            imageProjectService.getJobImages(
                accessToken = accessToken,
                userId = userId,
                projectCode = projectCode,
                agentType = agentType,
                recommendFlag = recommendFlag,
                classifyId = classifyId,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun getJobMarketImagesByProjectCode(
        accessToken: String,
        userId: String,
        projectCode: String,
        agentType: ImageAgentTypeEnum,
        recommendFlag: Boolean?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<JobMarketImageItem?>?> {
        return Result(
            imageProjectService.getJobMarketImagesByProjectCode(
                accessToken = accessToken,
                userId = userId,
                projectCode = projectCode,
                agentType = agentType,
                recommendFlag = recommendFlag,
                page = page,
                pageSize = pageSize,
                interfaceName = "/user/market/image/marketImages,get"
            )
        )
    }

    override fun searchJobMarketImages(
        accessToken: String,
        userId: String,
        projectCode: String,
        agentType: ImageAgentTypeEnum,
        recommendFlag: Boolean?,
        imageNamePart: String?,
        classifyId: String?,
        categoryCode: String?,
        rdType: ImageRDTypeEnum?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<JobMarketImageItem?>?> {
        return Result(
            imageProjectService.searchJobMarketImages(
                accessToken = accessToken,
                userId = userId,
                projectCode = projectCode,
                agentType = agentType,
                recommendFlag = recommendFlag,
                imageNamePart = imageNamePart,
                classifyId = classifyId,
                categoryCode = categoryCode,
                rdType = rdType,
                page = page,
                pageSize = pageSize,
                interfaceName = "/user/market/image/marketImages/search,get"
            )
        )
    }
}
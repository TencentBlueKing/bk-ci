package com.tencent.devops.store.service.image

import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_CODE
import com.tencent.devops.store.dao.image.ImageFeatureDao
import com.tencent.devops.store.dao.image.MarketImageDao
import com.tencent.devops.store.dao.image.MarketImageFeatureDao
import com.tencent.devops.store.exception.image.ImageNotExistException
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.image.enums.MarketImageSortTypeEnum
import com.tencent.devops.store.pojo.image.response.ImageDetail
import com.tencent.devops.store.pojo.image.response.ProjectSimpleInfo
import com.tencent.devops.store.service.common.StoreProjectService
import com.tencent.devops.store.service.common.StoreUserService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 镜像发布逻辑处理
 * author: carlyin
 * since: 2019-09-12
 */
@Service
class ImageProjectService @Autowired constructor(
    private val dslContext: DSLContext,
    private val imageService: ImageService,
    private val storeUserService: StoreUserService,
    private val imageFeatureDao: ImageFeatureDao,
    private val marketImageDao: MarketImageDao,
    private val marketImageFeatureDao: MarketImageFeatureDao,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val storeProjectService: StoreProjectService,
    private val client: Client
) {
    private val logger = LoggerFactory.getLogger(ImageProjectService::class.java)

    /**
     * 校验权限
     */
    private fun checkPermission(
        accessToken: String,
        userId: String,
        projectCode: String
    ) {
        val result =
            client.get(ServiceProjectResource::class).verifyUserProjectPermission(
                projectCode = projectCode,
                userId = userId
            )
        if (result.isNotOk()) {
            throw PermissionForbiddenException("userId=$userId,projectCode=$projectCode")
        }
    }

    /**
     * 根据项目标识获取可用镜像列表（公共+已安装）
     */
    fun getAvailableImagesByProjectCode(
        accessToken: String,
        userId: String,
        projectCode: String,
        page: Int?,
        pageSize: Int?,
        interfaceName: String? = "Anon interface"
    ): Page<ImageDetail?>? {
        logger.info("$interfaceName:getAvailableImagesByProjectCode:Input:($accessToken,$userId,$projectCode,$page,$pageSize)")
        // 1.参数校验
        val validPage = PageUtil.getValidPage(page)
        // 默认拉取所有
        val validPageSize = pageSize ?: -1
        // 2.权限校验：用户是否有该项目的权限
        checkPermission(accessToken, userId, projectCode)
        // 3.查数据库，弱一致，不用事务
        val offset = (validPage - 1) * validPageSize
        val limit = validPageSize
        // 3.1 查公共镜像
        val commonImageCodes = imageFeatureDao.getPublicImageCodes(dslContext, offset, limit)?.map {
            it.get(KEY_IMAGE_CODE) as String
        }?.toList() ?: emptyList()
        val commonImageSize = commonImageCodes.size
        logger.info("$interfaceName:getAvailableImagesByProjectCode:Inner:commonImageCodes.size=$commonImageSize,commonImageCodes=$commonImageCodes")
        var installedImageCodes = emptyList<String>()
        if (-1 != validPageSize && commonImageSize == validPageSize) {
            // 在公共镜像中就已拿到该页全部数据
            // 直接封装返回
            logger.info("$interfaceName:get all images in public images of page $validPage and pageSize $validPageSize")
        } else {
            // 在公共镜像中未取得该页全部数据，则需要继续查询已安装镜像
            // 3.2 查已安装镜像
            // 混合数据源分页计算
            val totalStartIndex = (validPage - 1) * validPageSize
            val (newOffset, newLimit) = when {
                -1 == validPageSize -> {
                    // 不分页直接查全量
                    Pair(0, -1)
                }
                commonImageSize >= totalStartIndex -> {
                    // 公共中查一部分，安装中查一部分
                    // 为排除公共镜像中也有已安装的重复镜像，直接查validPageSize数量的出来，从前往后补足公共镜像至validPageSize个为止
                    Pair(0, validPageSize)
                }
                else -> {
                    // 全在安装中查
                    Pair(totalStartIndex - commonImageSize, validPageSize)
                }
            }
            installedImageCodes =
                storeProjectRelDao.getInstalledComponent(
                    dslContext = dslContext,
                    projectCode = projectCode,
                    storeType = StoreTypeEnum.IMAGE.type.toByte(),
                    offset = newOffset,
                    limit = newLimit
                )?.map {
                    it.storeCode as String
                }?.toList() ?: emptyList()
            logger.info("$interfaceName:getAvailableImagesByProjectCode:Inner:installedImageCodes.size=${installedImageCodes.size},installedImageCodes=$installedImageCodes")
        }
        // 既需要有序，也需要去重
        var allImageCodes = mutableListOf<String>()
        for (imageCode in commonImageCodes.plus(installedImageCodes)) {
            if (!allImageCodes.contains(imageCode)) {
                allImageCodes.add(imageCode)
            }
        }
        if (-1 != validPageSize) {
            if (allImageCodes.isNotEmpty() && validPageSize < allImageCodes.size) {
                allImageCodes = allImageCodes.subList(0, validPageSize)
            }
        }
        logger.info("$interfaceName:getAvailableImagesByProjectCode:Inner:allImageCodes.size=${allImageCodes.size}")
        // 4.封装结果
        val imageDetailList = mutableListOf<ImageDetail>()
        allImageCodes.forEach {
            imageDetailList.add(
                imageService.getImageDetailByCode(
                    userId = userId,
                    imageCode = it,
                    interfaceName = interfaceName
                )
            )
        }
        // 查总数
        val count = if (-1 == validPageSize) {
            imageDetailList.size.toLong()
        } else {
            val allCommonImageCodes = imageFeatureDao.getPublicImageCodes(dslContext)?.map {
                it.get(KEY_IMAGE_CODE) as String
            }?.toSet() ?: emptySet()
            logger.info("$interfaceName:getAvailableImagesByProjectCode:Inner:allCommonImageCodes.size=${allCommonImageCodes.size}")
            val allInstalledImageCodes =
                storeProjectRelDao.getInstalledComponent(
                    dslContext = dslContext,
                    projectCode = projectCode,
                    storeType = StoreTypeEnum.IMAGE.type.toByte()
                )?.map {
                    it.storeCode as String
                }?.toSet() ?: emptySet()
            logger.info("$interfaceName:getAvailableImagesByProjectCode:Inner:allInstalledImageCodes.size=${allInstalledImageCodes.size}")
            allCommonImageCodes.plus(allInstalledImageCodes).size.toLong()
        }
        val pageObj = Page(
            page = validPage,
            pageSize = validPageSize,
            count = count,
            records = imageDetailList
        )
        logger.info("$interfaceName:getAvailableImagesByProjectCode:Output:Page($validPage,$validPageSize,imageDetailList.size=${imageDetailList.size})")
        return pageObj
    }

    /**
     * 根据项目标识获取商店镜像列表
     */
    fun getMarketImagesByProjectCode(
        accessToken: String,
        userId: String,
        projectCode: String,
        page: Int?,
        pageSize: Int?,
        interfaceName: String? = "Anon interface"
    ): Page<ImageDetail?>? {
        logger.info("$interfaceName:getMarketImagesByProjectCode:Input:($accessToken,$userId,$projectCode,$page,$pageSize)")
        return searchMarketImages(
            accessToken = accessToken,
            userId = userId,
            projectCode = projectCode,
            imageNamePart = null,
            classifyCodeList = null,
            page = page,
            pageSize = pageSize,
            interfaceName = interfaceName
        )
    }

    fun searchMarketImages(
        accessToken: String,
        userId: String,
        projectCode: String,
        imageNamePart: String?,
        classifyCodeList: List<String>?,
        page: Int?,
        pageSize: Int?,
        interfaceName: String? = "Anon interface"
    ): Page<ImageDetail?>? {
        logger.info("$interfaceName:searchMarketImages:Input:($accessToken,$userId,$projectCode,$imageNamePart,$page,$pageSize)")
        // 1.参数校验
        val validPage = PageUtil.getValidPage(page)
        // 默认拉取所有
        val validPageSize = pageSize ?: -1
        // 2.权限校验：用户是否有该项目的权限
        checkPermission(accessToken, userId, projectCode)
        // 3.查数据库
        // 获取用户组织架构
        val userDeptList = storeUserService.getUserDeptList(userId)
        logger.info("$interfaceName:searchMarketImages:Inner:userDeptList=$userDeptList")
        val imagesResult = imageService.doList(
            userId = userId,
            userDeptList = userDeptList,
            imageName = imageNamePart,
            classifyCodeList = classifyCodeList,
            labelCode = null,
            score = null,
            imageSourceType = null,
            sortType = MarketImageSortTypeEnum.DOWNLOAD_COUNT,
            desc = true,
            page = page,
            pageSize = pageSize,
            interfaceName = interfaceName
        )
        val resultList = mutableListOf<ImageDetail>()
        imagesResult.forEach {
            val isInstalled = storeProjectRelDao.isInstalledByProject(
                dslContext = dslContext,
                projectCode = projectCode,
                storeCode = it.code,
                storeType = StoreTypeEnum.IMAGE.type.toByte()
            )
            val imageDetail = imageService.getImageDetailByCode(userId, it.code, interfaceName)
            imageDetail.isInstalled = isInstalled
            resultList.add(imageDetail)
        }
        val count = marketImageDao.count(
            dslContext = dslContext,
            imageName = imageNamePart,
            classifyCodeList = classifyCodeList,
            labelCodeList = null,
            score = null,
            imageSourceType = null
        )
        val pageObj = Page(
            count = count.toLong(),
            page = validPage,
            pageSize = validPageSize,
            totalPages = if (validPageSize > 0) Math.ceil(count * 1.0 / validPageSize).toInt() else 1,
            records = resultList
        )
        logger.info("$interfaceName:searchMarketImages:Output:Page($validPage,$validPageSize,$count,resultList.size=${resultList.size})")
        return pageObj
    }

    /**
     * 安装镜像到项目
     */
    fun installImage(
        accessToken: String,
        userId: String,
        projectCodeList: ArrayList<String>,
        imageCode: String,
        channelCode: ChannelCode,
        interfaceName: String? = "Anon interface"
    ): Result<Boolean> {
        logger.info("$interfaceName:installImage:Input:($accessToken,$userId,$projectCodeList,$imageCode)")
        // 判断镜像标识是否合法
        val image = marketImageDao.getLatestImageByCode(dslContext, imageCode)
            ?: throw ImageNotExistException("imageCode=$imageCode")
        val imageFeature = marketImageFeatureDao.getExistedImageFeature(dslContext, imageCode)
        val validateInstallResult = storeProjectService.validateInstallPermission(
            publicFlag = imageFeature.publicFlag,
            userId = userId,
            storeCode = image.imageCode,
            storeType = StoreTypeEnum.IMAGE,
            accessToken = accessToken,
            projectCodeList = projectCodeList
        )
        if (validateInstallResult.isNotOk()) {
            return validateInstallResult
        }
        logger.info("$interfaceName:installImage:Inner:image.id=${image.id},imageFeature.publicFlag=${imageFeature.publicFlag}")
        return storeProjectService.installStoreComponent(
            accessToken = accessToken,
            userId = userId,
            projectCodeList = projectCodeList,
            storeId = image.id,
            storeCode = image.imageCode,
            storeType = StoreTypeEnum.IMAGE,
            publicFlag = imageFeature.publicFlag,
            channelCode = channelCode
        )
    }

    /**
     * 根据商城组件标识获取已安装的项目列表
     */
    fun getInstalledProjects(
        accessToken: String,
        imageCode: String,
        interfaceName: String? = "Anon interface"
    ): Result<List<ProjectSimpleInfo>> {
        logger.info("$interfaceName:getInstalledProjects:Input:($accessToken,$imageCode)")
        // 获取用户有权限的项目列表
        val projectList = client.get(ServiceProjectResource::class).list(accessToken).data
        val projectCodeList = projectList?.map { it.englishName }
        logger.info("$interfaceName:getInstalledProjects:Inner:projectList=$projectCodeList")
        if (projectList?.count() == 0) {
            return Result(mutableListOf())
        }
        val projectCodeMap = projectList?.map { it.projectCode to it }?.toMap()!!
        val records =
            storeProjectRelDao.getInstalledProject(
                dslContext,
                imageCode,
                StoreTypeEnum.IMAGE.type.toByte(),
                projectCodeMap.keys
            )
        val result = mutableListOf<ProjectSimpleInfo>()
        records?.forEach {
            result.add(
                ProjectSimpleInfo(
                    projectCode = it.projectCode,
                    projectName = projectCodeMap[it.projectCode]?.projectName!!,
                    creator = projectCodeMap[it.projectCode]?.creator,
                    createTime = projectCodeMap[it.projectCode]?.createdAt
                )
            )
        }
        return Result(result)
    }
}

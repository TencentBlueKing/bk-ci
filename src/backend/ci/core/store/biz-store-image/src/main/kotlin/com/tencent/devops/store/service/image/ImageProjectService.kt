package com.tencent.devops.store.service.image

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.dao.image.Constants.KEY_CATEGORY_CODE
import com.tencent.devops.store.dao.image.Constants.KEY_CATEGORY_NAME
import com.tencent.devops.store.dao.image.Constants.KEY_CREATE_TIME
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_AGENT_TYPE_SCOPE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_CODE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_FEATURE_CERTIFICATION_FLAG
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_FEATURE_PUBLIC_FLAG
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_FEATURE_RECOMMEND_FLAG
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_FEATURE_WEIGHT
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_ICON
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_ID
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_LOGO_URL
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_NAME
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_RD_TYPE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_REPO_NAME
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_REPO_URL
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_SOURCE_TYPE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_SUMMARY
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_TAG
import com.tencent.devops.store.dao.image.Constants.KEY_LABEL_CODE
import com.tencent.devops.store.dao.image.Constants.KEY_LABEL_ID
import com.tencent.devops.store.dao.image.Constants.KEY_LABEL_NAME
import com.tencent.devops.store.dao.image.Constants.KEY_LABEL_TYPE
import com.tencent.devops.store.dao.image.Constants.KEY_MODIFIER
import com.tencent.devops.store.dao.image.Constants.KEY_PUBLISHER
import com.tencent.devops.store.dao.image.Constants.KEY_UPDATE_TIME
import com.tencent.devops.store.dao.image.ImageAgentTypeDao
import com.tencent.devops.store.dao.image.ImageDao
import com.tencent.devops.store.dao.image.ImageLabelRelDao
import com.tencent.devops.store.dao.image.MarketImageDao
import com.tencent.devops.store.dao.image.MarketImageFeatureDao
import com.tencent.devops.store.exception.image.ImageNotExistException
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageAgentTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageRDTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
import com.tencent.devops.store.pojo.image.enums.LabelTypeEnum
import com.tencent.devops.store.pojo.image.enums.MarketImageSortTypeEnum
import com.tencent.devops.store.pojo.image.response.ImageDetail
import com.tencent.devops.store.pojo.image.response.JobImageItem
import com.tencent.devops.store.pojo.image.response.JobMarketImageItem
import com.tencent.devops.store.pojo.image.response.Label
import com.tencent.devops.store.service.common.StoreProjectService
import com.tencent.devops.store.service.common.StoreUserService
import com.tencent.devops.store.util.MultiSourceDataPaginator
import com.tencent.devops.store.util.PagableDataSource
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record21
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.StopWatch
import java.time.LocalDateTime
import kotlin.math.ceil

@Service
class ImageProjectService @Autowired constructor(
    private val dslContext: DSLContext,
    private val imageService: ImageService,
    private val storeUserService: StoreUserService,
    private val imageDao: ImageDao,
    private val imageLabelRelDao: ImageLabelRelDao,
    private val marketImageDao: MarketImageDao,
    private val imageAgentTypeDao: ImageAgentTypeDao,
    private val marketImageFeatureDao: MarketImageFeatureDao,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val storeProjectService: StoreProjectService,
    private val client: Client
) {
    @Value("\${store.baseImageDocsLink}")
    private lateinit var baseImageDocsLink: String
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
                accessToken = accessToken,
                projectCode = projectCode,
                userId = userId
            )
        if (result.isNotOk()) {
            throw ErrorCodeException(StoreMessageCode.USER_QUERY_PROJECT_PERMISSION_IS_INVALID, null)
        }
    }

    /**
     * 根据项目标识获取可用镜像列表（公共+已安装）
     */
    fun getJobImages(
        accessToken: String,
        userId: String,
        projectCode: String,
        agentType: ImageAgentTypeEnum?,
        recommendFlag: Boolean?,
        classifyId: String?,
        page: Int?,
        pageSize: Int?
    ): Page<JobImageItem>? {
        logger.info("getJobImages accessToken is :$accessToken,userId is :$userId,projectCode is :$projectCode,agentType is :$agentType")
        logger.info("getJobImages recommendFlag is :$recommendFlag,classifyId is :$classifyId,page is :$page,pageSize is :$pageSize")
        // 校验用户是否有该项目的权限
        checkPermission(accessToken, userId, projectCode)
        val totalSize: Long
        val jobImageItemList = mutableListOf<JobImageItem>()
        if (agentType != null) {
            val totalAvailableAgentJobImageSize = imageDao.getJobImageCount(
                dslContext = dslContext,
                projectCode = projectCode,
                agentType = agentType,
                isContainAgentType = true,
                classifyId = classifyId,
                recommendFlag = recommendFlag
            )
            val totalInvalidJobImageSize = imageDao.getJobImageCount(
                dslContext = dslContext,
                projectCode = projectCode,
                agentType = agentType,
                isContainAgentType = false,
                classifyId = classifyId,
                recommendFlag = recommendFlag
            )
            logger.info("getJobImages totalAvailableAgentJobImageSize is :$totalAvailableAgentJobImageSize,totalInvalidJobImageSize is :$totalInvalidJobImageSize")
            totalSize = totalAvailableAgentJobImageSize + totalInvalidJobImageSize
            if (page != null && pageSize != null) {
                // 判断可用的镜像是否已到最后一页
                val totalAvailableAgentJobImagePage = PageUtil.calTotalPage(pageSize, totalAvailableAgentJobImageSize)
                logger.info("getJobImages totalAvailableAgentJobImagePage is :$totalAvailableAgentJobImagePage")
                if (page < totalAvailableAgentJobImagePage) {
                    // 当前页未到可用镜像最后一页，不需要处理临界点（最后一页）的情况
                    handleJobImageItemList(
                        projectCode = projectCode,
                        agentType = agentType,
                        isContainAgentType = true,
                        classifyId = classifyId,
                        recommendFlag = recommendFlag,
                        page = page,
                        pageSize = pageSize,
                        jobImageItemList = jobImageItemList
                    )
                } else if (page == totalAvailableAgentJobImagePage && totalAvailableAgentJobImageSize > 0) {
                    //  查询可用镜像最后一页不满页的数量
                    val lastPageRemainNum = pageSize - totalAvailableAgentJobImageSize % pageSize
                    logger.info("getJobImages lastPageRemainNum is :$lastPageRemainNum")
                    handleJobImageItemList(
                        projectCode = projectCode,
                        agentType = agentType,
                        isContainAgentType = true,
                        classifyId = classifyId,
                        recommendFlag = recommendFlag,
                        page = page,
                        pageSize = pageSize,
                        jobImageItemList = jobImageItemList
                    )
                    // 可用镜像最后一页不满页的数量需用不可用的镜像填充
                    if (lastPageRemainNum > 0 && totalInvalidJobImageSize > 0) {
                        handleJobImageItemList(
                            projectCode = projectCode,
                            agentType = agentType,
                            isContainAgentType = false,
                            classifyId = classifyId,
                            recommendFlag = recommendFlag,
                            page = page,
                            pageSize = lastPageRemainNum.toInt(),
                            jobImageItemList = jobImageItemList
                        )
                    }
                } else {
                    // 当前页大于可用镜像最后一页，需要排除掉可用镜像最后一页不满页的数量用不可用的镜像填充的情况
                    val lastPageRemainNum = if (totalAvailableAgentJobImageSize > 0) pageSize - totalAvailableAgentJobImageSize % pageSize else 0
                    logger.info("getJobImages lastPageRemainNum is :$lastPageRemainNum")
                    handleJobImageItemList(
                        projectCode = projectCode,
                        agentType = agentType,
                        isContainAgentType = false,
                        classifyId = classifyId,
                        recommendFlag = recommendFlag,
                        page = page - totalAvailableAgentJobImagePage,
                        pageSize = pageSize,
                        jobImageItemList = jobImageItemList,
                        offsetNum = lastPageRemainNum.toInt()
                    )
                }
            } else {
                // 不分页查询
                handleJobImageItemList(
                    projectCode = projectCode,
                    agentType = agentType,
                    isContainAgentType = true,
                    classifyId = classifyId,
                    recommendFlag = recommendFlag,
                    page = page,
                    pageSize = pageSize,
                    jobImageItemList = jobImageItemList
                )
                handleJobImageItemList(
                    projectCode = projectCode,
                    agentType = agentType,
                    isContainAgentType = false,
                    classifyId = classifyId,
                    recommendFlag = recommendFlag,
                    page = page,
                    pageSize = pageSize,
                    jobImageItemList = jobImageItemList
                )
            }
        } else {
            // 统计镜像总数量
            totalSize = imageDao.getJobImageCount(
                dslContext = dslContext,
                projectCode = projectCode,
                agentType = agentType,
                isContainAgentType = null,
                recommendFlag = recommendFlag,
                classifyId = classifyId
            )
            handleJobImageItemList(
                projectCode = projectCode,
                agentType = agentType,
                isContainAgentType = true,
                classifyId = classifyId,
                recommendFlag = recommendFlag,
                page = page,
                pageSize = pageSize,
                jobImageItemList = jobImageItemList
            )
        }
        val totalPages = PageUtil.calTotalPage(pageSize, totalSize)
        return Page(
            count = totalSize, page = page ?: 1, pageSize = pageSize
            ?: totalSize.toInt(), totalPages = totalPages, records = jobImageItemList
        )
    }

    private fun handleJobImageItemList(
        projectCode: String,
        agentType: ImageAgentTypeEnum?,
        isContainAgentType: Boolean,
        classifyId: String?,
        recommendFlag: Boolean?,
        page: Int?,
        pageSize: Int?,
        jobImageItemList: MutableList<JobImageItem>,
        offsetNum: Int? = null
    ) {
        val jobImageRecords = imageDao.getJobImages(
            dslContext = dslContext,
            projectCode = projectCode,
            agentType = agentType,
            isContainAgentType = isContainAgentType,
            classifyId = classifyId,
            recommendFlag = recommendFlag,
            page = page,
            pageSize = pageSize,
            offsetNum = offsetNum
        )
        logger.info("jobImageRecords is : $jobImageRecords")
        jobImageRecords?.forEach {
            val jobImageItem = generateJobImageItem(isContainAgentType, it)
            jobImageItemList.add(jobImageItem)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun generateJobImageItem(isContainAgentType: Boolean, it: Record): JobImageItem {
        val imageId = it["imageId"] as String
        val imageCode = it["imageCode"] as String
        val imageName = it["imageName"] as String
        val version = it["version"] as String
        val versionPrefix = version.substring(0, version.indexOf(".") + 1)
        val defaultVersion = "$versionPrefix*"
        val imageStatus = it["imageStatus"] as Byte
        val dbClassifyId = it["classifyId"] as String
        val classifyCode = it["classifyCode"] as String
        val classifyName = it["classifyName"] as String
        val logoUrl = it["logoUrl"] as? String
        val icon = it["icon"] as? String
        val summary = it["summary"] as? String
        val publisher = it["publisher"] as? String
        val pubTime = it["pubTime"] as? LocalDateTime
        val creator = it["creator"] as String
        val createTime = it["createTime"] as LocalDateTime
        val latestFlag = it["latestFlag"] as Boolean
        val agentTypeScopeStr = it["agentTypeScope"] as? String
        val agentTypeScopeList = if (!agentTypeScopeStr.isNullOrBlank()) JsonUtil.getObjectMapper().readValue(
            agentTypeScopeStr,
            List::class.java
        ) as List<String> else listOf()
        val imageSourceType = it["imageSourceType"] as String
        val imageRepoUrl = it["imageRepoUrl"] as? String
        val imageRepoName = it["imageRepoName"] as String
        val imageTag = it["imageTag"] as String
        val imageSize = it["imageSize"] as String
        val certificationFlag = it["certificationFlag"] as? Boolean
        val publicFlag = it["publicFlag"] as? Boolean
        val imageType = it["imageType"] as? Byte
        val weight = it["weight"] as? Int
        val recommendFlag = it["recommendFlag"] as? Boolean
        val labelNames = it["labelNames"] as? String
        val modifier = it["modifier"] as String?
        val updateTime = (it["updateTime"] as LocalDateTime).timestampmilli()
        return JobImageItem(
            id = imageId,
            code = imageCode,
            name = imageName,
            version = version,
            defaultVersion = defaultVersion,
            imageStatus = ImageStatusEnum.getImageStatus(imageStatus.toInt()),
            classifyId = dbClassifyId,
            classifyCode = classifyCode,
            classifyName = classifyName,
            logoUrl = logoUrl,
            icon = icon,
            summary = summary,
            docsLink = baseImageDocsLink + imageCode,
            publisher = publisher,
            pubTime = pubTime?.timestampmilli(),
            creator = creator,
            createTime = createTime.timestampmilli(),
            latestFlag = latestFlag,
            agentTypeScope = agentTypeScopeList,
            imageSourceType = ImageType.getType(imageSourceType),
            imageRepoUrl = imageRepoUrl,
            imageRepoName = imageRepoName,
            imageTag = imageTag,
            imageSize = imageSize,
            certificationFlag = certificationFlag,
            publicFlag = publicFlag,
            imageType = if (null != imageType) ImageRDTypeEnum.getImageRDType(imageType.toInt()) else null,
            weight = weight,
            recommendFlag = recommendFlag,
            labelNames = labelNames,
            availableFlag = isContainAgentType,
            modifier = modifier ?: "",
            updateTime = updateTime
        )
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
            categoryCode = null,
            rdType = null,
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
        categoryCode: String?,
        rdType: ImageRDTypeEnum?,
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
            categoryCodeList = if (null != categoryCode) listOf(categoryCode) else null,
            rdType = rdType,
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
            rdType = rdType,
            score = null,
            imageSourceType = null
        )
        val pageObj = Page(
            count = count.toLong(),
            page = validPage,
            pageSize = validPageSize,
            totalPages = if (validPageSize > 0) ceil(count * 1.0 / validPageSize).toInt() else 1,
            records = resultList
        )
        logger.info("$interfaceName:searchMarketImages:Output:Page($validPage,$validPageSize,$count,resultList.size=${resultList.size})")
        return pageObj
    }

    /**
     * 根据项目标识获取商店镜像列表
     */
    fun getJobMarketImagesByProjectCode(
        accessToken: String,
        userId: String,
        projectCode: String,
        agentType: ImageAgentTypeEnum,
        recommendFlag: Boolean?,
        page: Int?,
        pageSize: Int?,
        interfaceName: String? = "Anon interface"
    ): Page<JobMarketImageItem?>? {
        logger.info("$interfaceName:getMarketImagesByProjectCode:Input:($accessToken,$userId,$projectCode,$page,$pageSize)")
        return searchJobMarketImages(
            accessToken = accessToken,
            userId = userId,
            projectCode = projectCode,
            agentType = agentType,
            recommendFlag = recommendFlag,
            imageNamePart = null,
            classifyId = null,
            categoryCode = null,
            rdType = null,
            page = page,
            pageSize = pageSize,
            interfaceName = interfaceName
        )
    }

    fun searchJobMarketImages(
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
        pageSize: Int?,
        interfaceName: String? = "Anon interface"
    ): Page<JobMarketImageItem?>? {
        logger.info("$interfaceName:searchMarketImages:Input:($accessToken,$userId,$projectCode,$imageNamePart,$page,$pageSize)")
        val watch = StopWatch()
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
        val installImageCodes = marketImageDao.getInstalledImageCodes(dslContext, projectCode)
        var testImageCodes = storeProjectRelDao.getTestImageCodes(dslContext, projectCode, StoreTypeEnum.IMAGE)?.map { it.value1() }
            ?: emptyList()
        testImageCodes = marketImageDao.getTestingImageCodes(dslContext, testImageCodes)?.map {
            it.value1()
        } ?: emptyList()
        val visibleImageCodes = marketImageDao.getVisibleImageCodes(dslContext, projectCode, userDeptList)
        val agentTypeImageCodes = imageAgentTypeDao.getImageCodesByAgentType(dslContext, agentType)?.map { it.value1() }
            ?: emptyList()
        // （1）未安装、可安装、agentType符合的镜像
        val canInstallCurrentAgentDataSource = object : PagableDataSource<JobMarketImageItem> {
            override fun getData(offset: Int, limit: Int): List<JobMarketImageItem> {
                watch.start("canInstallCurrentAgentJobMarketImages")
                val canInstallCurrentAgentJobMarketImages = marketImageDao.listCanInstallJobMarketImages(
                    dslContext = dslContext,
                    inImageCodes = agentTypeImageCodes,
                    notInImageCodes = null,
                    recommendFlag = recommendFlag,
                    imageNamePart = imageNamePart,
                    classifyId = classifyId,
                    categoryCode = categoryCode,
                    rdType = rdType,
                    installedImageCodes = installImageCodes,
                    visibleImageCodes = visibleImageCodes,
                    offset = offset,
                    limit = limit
                )?.map {
                    genJobMarketImageItem(
                        it = it,
                        canInstallFlag = true,
                        installFlag = false,
                        availableFlag = true
                    )
                } ?: emptyList()
                watch.stop()
                logger.info("canInstallCurrentAgentJobMarketImages:timecosuming:$watch")
                return canInstallCurrentAgentJobMarketImages
            }

            override fun getDataSize(): Int {
                watch.start("count canInstallCurrentAgentJobMarketImages")
                val canInstallCurrentAgentJobMarketImagesCount = marketImageDao.countCanInstallJobMarketImages(
                    dslContext = dslContext,
                    inImageCodes = agentTypeImageCodes,
                    notInImageCodes = null,
                    recommendFlag = recommendFlag,
                    imageNamePart = imageNamePart,
                    classifyId = classifyId,
                    categoryCode = categoryCode,
                    rdType = rdType,
                    installedImageCodes = installImageCodes,
                    visibleImageCodes = visibleImageCodes
                )
                watch.stop()
                logger.info("count canInstallCurrentAgentJobMarketImages:timecosuming:$watch")
                return canInstallCurrentAgentJobMarketImagesCount
            }
        }
        // （2）未安装、可安装、agentType不符合的镜像
        val canInstallOtherAgentDataSource = object : PagableDataSource<JobMarketImageItem> {
            override fun getData(offset: Int, limit: Int): List<JobMarketImageItem> {
                watch.start("canInstallOtherAgentJobMarketImages")
                val canInstallOtherAgentJobMarketImages = marketImageDao.listCanInstallJobMarketImages(
                    dslContext = dslContext,
                    inImageCodes = null,
                    notInImageCodes = agentTypeImageCodes,
                    recommendFlag = recommendFlag,
                    imageNamePart = imageNamePart,
                    classifyId = classifyId,
                    categoryCode = categoryCode,
                    rdType = rdType,
                    installedImageCodes = installImageCodes,
                    visibleImageCodes = visibleImageCodes,
                    offset = offset,
                    limit = limit
                )?.map {
                    genJobMarketImageItem(
                        it = it,
                        canInstallFlag = true,
                        installFlag = false,
                        availableFlag = false
                    )
                } ?: emptyList()
                watch.stop()
                logger.info("canInstallOtherAgentJobMarketImages:timecosuming:$watch")
                return canInstallOtherAgentJobMarketImages
            }

            override fun getDataSize(): Int {
                watch.start("count canInstallOtherAgentJobMarketImages")
                val canInstallOtherAgentJobMarketImagesCount = marketImageDao.countCanInstallJobMarketImages(
                    dslContext = dslContext,
                    inImageCodes = null,
                    notInImageCodes = agentTypeImageCodes,
                    recommendFlag = recommendFlag,
                    imageNamePart = imageNamePart,
                    classifyId = classifyId,
                    categoryCode = categoryCode,
                    rdType = rdType,
                    installedImageCodes = installImageCodes,
                    visibleImageCodes = visibleImageCodes
                )
                watch.stop()
                logger.info("count canInstallOtherAgentJobMarketImages:timecosuming:$watch")
                return canInstallOtherAgentJobMarketImagesCount
            }
        }
        // （3.1）agentType符合的调试中镜像
        val agentTypeTestImageCodes = agentTypeImageCodes.intersect(testImageCodes)
        val testingCurrentAgentDataSource = object : PagableDataSource<JobMarketImageItem> {
            override fun getData(offset: Int, limit: Int): List<JobMarketImageItem> {
                watch.start("testingCurrentAgentJobMarketImages")
                val testingCurrentAgentJobMarketImages = marketImageDao.listTestingJobMarketImages(
                    dslContext = dslContext,
                    // agentType符合与调试中镜像的交集
                    inImageCodes = agentTypeTestImageCodes,
                    notInImageCodes = null,
                    recommendFlag = recommendFlag,
                    imageNamePart = imageNamePart,
                    classifyId = classifyId,
                    categoryCode = categoryCode,
                    rdType = rdType,
                    offset = offset,
                    limit = limit
                )?.map {
                    genJobMarketImageItem(
                        it = it,
                        canInstallFlag = true,
                        installFlag = true,
                        availableFlag = true
                    )
                } ?: emptyList()
                watch.stop()
                logger.info("testingCurrentAgentJobMarketImages:timecosuming:$watch")
                return testingCurrentAgentJobMarketImages
            }

            override fun getDataSize(): Int {
                watch.start("count testingCurrentAgentJobMarketImages")
                val testingCurrentAgentJobMarketImagesCount = marketImageDao.countTestingJobMarketImages(
                    dslContext = dslContext,
                    inImageCodes = agentTypeTestImageCodes,
                    notInImageCodes = null,
                    recommendFlag = recommendFlag,
                    imageNamePart = imageNamePart,
                    classifyId = classifyId,
                    categoryCode = categoryCode,
                    rdType = rdType
                )
                watch.stop()
                logger.info("count testingCurrentAgentJobMarketImages:timecosuming:$watch")
                return testingCurrentAgentJobMarketImagesCount
            }
        }
        // （3.2）agentType不符合的调试中镜像
        val testingOtherAgentDataSource = object : PagableDataSource<JobMarketImageItem> {
            override fun getData(offset: Int, limit: Int): List<JobMarketImageItem> {
                watch.start("testingOtherAgentJobMarketImages")
                val testingOtherAgentJobMarketImages = marketImageDao.listTestingJobMarketImages(
                    dslContext = dslContext,
                    // agentType符合与调试中镜像的交集
                    inImageCodes = testImageCodes,
                    notInImageCodes = agentTypeImageCodes,
                    recommendFlag = recommendFlag,
                    imageNamePart = imageNamePart,
                    classifyId = classifyId,
                    categoryCode = categoryCode,
                    rdType = rdType,
                    offset = offset,
                    limit = limit
                )?.map {
                    genJobMarketImageItem(
                        it = it,
                        canInstallFlag = true,
                        installFlag = true,
                        availableFlag = true
                    )
                } ?: emptyList()
                watch.stop()
                logger.info("testingOtherAgentJobMarketImages:timecosuming:$watch")
                return testingOtherAgentJobMarketImages
            }

            override fun getDataSize(): Int {
                watch.start("count testingOtherAgentJobMarketImages")
                val testingOtherAgentJobMarketImagesCount = marketImageDao.countTestingJobMarketImages(
                    dslContext = dslContext,
                    inImageCodes = testImageCodes,
                    notInImageCodes = agentTypeImageCodes,
                    recommendFlag = recommendFlag,
                    imageNamePart = imageNamePart,
                    classifyId = classifyId,
                    categoryCode = categoryCode,
                    rdType = rdType
                )
                watch.stop()
                logger.info("count testingOtherAgentJobMarketImages:timecosuming:$watch")
                return testingOtherAgentJobMarketImagesCount
            }
        }
        // （3.3）已安装、agentType符合的镜像（不含调试中镜像）
        val installedCurrentAgentDataSource = object : PagableDataSource<JobMarketImageItem> {
            override fun getData(offset: Int, limit: Int): List<JobMarketImageItem> {
                watch.start("installedCurrentAgentJobMarketImages")
                val installedCurrentAgentJobMarketImages = marketImageDao.listInstalledJobMarketImages(
                    dslContext = dslContext,
                    inImageCodes = agentTypeImageCodes,
                    notInImageCodes = testImageCodes,
                    recommendFlag = recommendFlag,
                    imageNamePart = imageNamePart,
                    classifyId = classifyId,
                    categoryCode = categoryCode,
                    rdType = rdType,
                    installedImageCodes = installImageCodes,
                    offset = offset,
                    limit = limit
                )?.map {
                    genJobMarketImageItem(
                        it = it,
                        canInstallFlag = true,
                        installFlag = true,
                        availableFlag = true
                    )
                } ?: emptyList()
                watch.stop()
                logger.info("installedCurrentAgentJobMarketImages:timecosuming:$watch")
                return installedCurrentAgentJobMarketImages
            }

            override fun getDataSize(): Int {
                watch.start("count installedCurrentAgentJobMarketImages")
                val installedCurrentAgentJobMarketImagesCount = marketImageDao.countInstalledJobMarketImages(
                    dslContext = dslContext,
                    inImageCodes = agentTypeImageCodes,
                    notInImageCodes = null,
                    recommendFlag = recommendFlag,
                    imageNamePart = imageNamePart,
                    classifyId = classifyId,
                    categoryCode = categoryCode,
                    rdType = rdType,
                    installedImageCodes = installImageCodes
                )
                watch.stop()
                logger.info("count installedCurrentAgentJobMarketImages:timecosuming:$watch")
                return installedCurrentAgentJobMarketImagesCount
            }
        }
        // （4）已安装、agentType不符合的镜像
        val installedOtherAgentDataSource = object : PagableDataSource<JobMarketImageItem> {
            override fun getData(offset: Int, limit: Int): List<JobMarketImageItem> {
                watch.start("installedOtherAgentJobMarketImages")
                val installedOtherAgentJobMarketImages = marketImageDao.listInstalledJobMarketImages(
                    dslContext = dslContext,
                    inImageCodes = null,
                    notInImageCodes = agentTypeImageCodes,
                    recommendFlag = recommendFlag,
                    imageNamePart = imageNamePart,
                    classifyId = classifyId,
                    categoryCode = categoryCode,
                    rdType = rdType,
                    installedImageCodes = installImageCodes,
                    offset = offset,
                    limit = limit
                )?.map {
                    genJobMarketImageItem(
                        it = it,
                        canInstallFlag = true,
                        installFlag = true,
                        availableFlag = false
                    )
                } ?: emptyList()
                watch.stop()
                logger.info("installedOtherAgentJobMarketImages:timecosuming:$watch")
                return installedOtherAgentJobMarketImages
            }

            override fun getDataSize(): Int {
                watch.start("count installedOtherAgentJobMarketImages")
                val installedOtherAgentJobMarketImagesCount = marketImageDao.countInstalledJobMarketImages(
                    dslContext = dslContext,
                    inImageCodes = null,
                    notInImageCodes = agentTypeImageCodes,
                    recommendFlag = recommendFlag,
                    imageNamePart = imageNamePart,
                    classifyId = classifyId,
                    categoryCode = categoryCode,
                    rdType = rdType,
                    installedImageCodes = installImageCodes
                )
                watch.stop()
                logger.info("count installedOtherAgentJobMarketImages:timecosuming:$watch")
                return installedOtherAgentJobMarketImagesCount
            }
        }
        // （5）不可见、agentType符合的镜像
        val noVisibleCurrentAgentDataSource = object : PagableDataSource<JobMarketImageItem> {
            override fun getData(offset: Int, limit: Int): List<JobMarketImageItem> {
                watch.start("noVisibleCurrentAgentJobMarketImages")
                val noVisibleCurrentAgentJobMarketImages = marketImageDao.listNoVisibleJobMarketImages(
                    dslContext = dslContext,
                    inImageCodes = agentTypeImageCodes,
                    notInImageCodes = null,
                    recommendFlag = recommendFlag,
                    imageNamePart = imageNamePart,
                    classifyId = classifyId,
                    categoryCode = categoryCode,
                    rdType = rdType,
                    visibleImageCodes = visibleImageCodes,
                    offset = offset,
                    limit = limit
                )?.map {
                    genJobMarketImageItem(
                        it = it,
                        canInstallFlag = false,
                        installFlag = false,
                        availableFlag = true
                    )
                } ?: emptyList()
                watch.stop()
                logger.info("noVisibleCurrentAgentJobMarketImages:timecosuming:$watch")
                return noVisibleCurrentAgentJobMarketImages
            }

            override fun getDataSize(): Int {
                watch.start("count noVisibleCurrentAgentJobMarketImages")
                val noVisibleCurrentAgentJobMarketImagesCount = marketImageDao.countNoVisibleJobMarketImages(
                    dslContext = dslContext,
                    inImageCodes = agentTypeImageCodes,
                    notInImageCodes = null,
                    recommendFlag = recommendFlag,
                    imageNamePart = imageNamePart,
                    classifyId = classifyId,
                    categoryCode = categoryCode,
                    rdType = rdType,
                    visibleImageCodes = visibleImageCodes
                )
                watch.stop()
                logger.info("count noVisibleCurrentAgentJobMarketImages:timecosuming:$watch")
                return noVisibleCurrentAgentJobMarketImagesCount
            }
        }
        // （6）不可见、agentType不符合的镜像
        val noVisibleOtherAgentDataSource = object : PagableDataSource<JobMarketImageItem> {
            override fun getData(offset: Int, limit: Int): List<JobMarketImageItem> {
                watch.start("noVisibleOtherAgentJobMarketImages")
                val noVisibleOtherAgentJobMarketImages = marketImageDao.listNoVisibleJobMarketImages(
                    dslContext = dslContext,
                    inImageCodes = null,
                    notInImageCodes = agentTypeImageCodes,
                    recommendFlag = recommendFlag,
                    imageNamePart = imageNamePart,
                    classifyId = classifyId,
                    categoryCode = categoryCode,
                    rdType = rdType,
                    visibleImageCodes = visibleImageCodes,
                    offset = offset,
                    limit = limit
                )?.map {
                    genJobMarketImageItem(
                        it = it,
                        canInstallFlag = false,
                        installFlag = false,
                        availableFlag = false
                    )
                } ?: emptyList()
                watch.stop()
                logger.info("noVisibleOtherAgentJobMarketImages:timecosuming:$watch")
                return noVisibleOtherAgentJobMarketImages
            }

            override fun getDataSize(): Int {
                watch.start("count noVisibleOtherAgentJobMarketImages")
                val noVisibleOtherAgentJobMarketImagesCount = marketImageDao.countNoVisibleJobMarketImages(
                    dslContext = dslContext,
                    inImageCodes = null,
                    notInImageCodes = agentTypeImageCodes,
                    recommendFlag = recommendFlag,
                    imageNamePart = imageNamePart,
                    classifyId = classifyId,
                    categoryCode = categoryCode,
                    rdType = rdType,
                    visibleImageCodes = visibleImageCodes
                )
                watch.stop()
                logger.info("count noVisibleOtherAgentJobMarketImages:timecosuming:$watch")
                return noVisibleOtherAgentJobMarketImagesCount
            }
        }
        val paginator = MultiSourceDataPaginator(
            // 已安装可选择
            installedCurrentAgentDataSource,
            // 类型符合的调试中镜像
            testingCurrentAgentDataSource,
            // 类型不符合的调试中镜像
            testingOtherAgentDataSource,
            // 可安装
            canInstallCurrentAgentDataSource,
            // 类型符合但不可见
            noVisibleCurrentAgentDataSource,
            // 已安装类型不符
            installedOtherAgentDataSource,
            // 可安装类型不符
            canInstallOtherAgentDataSource,
            // 类型不符也不可见
            noVisibleOtherAgentDataSource
        )

        val resultList = paginator.getPagedData(validPage, validPageSize)
        val count = paginator.getTotalCount().toLong()
        val pageObj = Page(
            page = validPage,
            pageSize = validPageSize,
            count = count,
            records = resultList
        )
        logger.info("$interfaceName:searchMarketImages:Output:Page($validPage,$validPageSize,resultList.size=${resultList.size})")
        return pageObj
    }

    fun genJobMarketImageItem(
        it: Record21<String, String, String, Byte, String, String, String, Int, String, String, String, String, String, String, String, String, LocalDateTime, Boolean, Boolean, Boolean, String>,
        canInstallFlag: Boolean,
        installFlag: Boolean,
        availableFlag: Boolean
    ): JobMarketImageItem {
        val id = it.get(KEY_IMAGE_ID) as String
        val code = it.get(KEY_IMAGE_CODE) as String
        val name = it.get(KEY_IMAGE_NAME) as String
        val rdType = ImageRDTypeEnum.getImageRDType((it.get(KEY_IMAGE_RD_TYPE) as Byte).toInt())

        // 单独查询agentTypeScope
        val agentTypeScopeStr = it.get(KEY_IMAGE_AGENT_TYPE_SCOPE) as String?
        val agentTypeScope = agentTypeScopeStr?.split(",")?.map { ImageAgentTypeEnum.getImageAgentType(it)!! }?.toList()
            ?: emptyList()

        val logoUrl = it.get(KEY_IMAGE_LOGO_URL) as String?
        val icon = it.get(KEY_IMAGE_ICON) as String?
        val summary = it.get(KEY_IMAGE_SUMMARY) as String?
        val docsLink = baseImageDocsLink + code
        val weight = it.get(KEY_IMAGE_FEATURE_WEIGHT) as Int?
        val imageSourceType = ImageType.getType(it.get(KEY_IMAGE_SOURCE_TYPE) as String).name
        val imageRepoUrl = it.get(KEY_IMAGE_REPO_URL) as String?
        val imageRepoName = it.get(KEY_IMAGE_REPO_NAME) as String
        val imageTag = it.get(KEY_IMAGE_TAG) as String
        // 单独查询
        val labelList = imageLabelRelDao.getLabelsByImageId(dslContext, id)?.map {
            Label(
                id = it.get(KEY_LABEL_ID) as String,
                labelCode = it.get(KEY_LABEL_CODE) as String,
                labelName = it.get(KEY_LABEL_NAME) as String,
                labelType = LabelTypeEnum.getLabelType((it.get(KEY_LABEL_TYPE) as Byte).toInt()),
                createTime = (it.get(KEY_CREATE_TIME) as LocalDateTime).timestampmilli(),
                updateTime = (it.get(KEY_UPDATE_TIME) as LocalDateTime).timestampmilli()
            )
        } ?: emptyList()
        val category = it.get(KEY_CATEGORY_CODE) as String?
        val categoryName = it.get(KEY_CATEGORY_NAME) as String?
        val publisher = it.get(KEY_PUBLISHER) as String
        val publicFlag = it.get(KEY_IMAGE_FEATURE_PUBLIC_FLAG) as Boolean
        // 是否可安装
        val recommendFlag = it.get(KEY_IMAGE_FEATURE_RECOMMEND_FLAG) as Boolean
        val certificationFlag = it.get(KEY_IMAGE_FEATURE_CERTIFICATION_FLAG) as Boolean
        val modifier = it.get(KEY_MODIFIER) as String?
        val updateTime = (it.get(KEY_UPDATE_TIME) as LocalDateTime).timestampmilli()
        return JobMarketImageItem(
            imageId = id,
            id = id,
            imageCode = code,
            code = code,
            imageName = name,
            name = name,
            rdType = rdType,
            agentTypeScope = agentTypeScope,
            availableFlag = availableFlag,
            logoUrl = logoUrl ?: "",
            icon = icon ?: "",
            summary = summary ?: "",
            docsLink = docsLink,
            weight = weight ?: 0,
            imageSourceType = imageSourceType,
            imageRepoUrl = imageRepoUrl ?: "",
            imageRepoName = imageRepoName,
            imageTag = imageTag,
            labelNames = labelList.map { it.labelName }.joinToString { it },
            category = category ?: "",
            categoryName = categoryName ?: "",
            publisher = publisher,
            publicFlag = publicFlag,
            flag = canInstallFlag,
            recommendFlag = recommendFlag,
            certificationFlag = certificationFlag,
            isInstalled = installFlag,
            modifier = modifier ?: "",
            updateTime = updateTime
        )
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
}

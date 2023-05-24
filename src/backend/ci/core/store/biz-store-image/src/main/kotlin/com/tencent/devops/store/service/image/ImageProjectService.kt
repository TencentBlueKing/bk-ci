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

package com.tencent.devops.store.service.image

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_AGENT_TYPE_SCOPE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_CODE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_DOCKER_FILE_CONTENT
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_DOCKER_FILE_TYPE
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
import com.tencent.devops.store.dao.image.ImageAgentTypeDao
import com.tencent.devops.store.dao.image.ImageDao
import com.tencent.devops.store.dao.image.MarketImageDao
import com.tencent.devops.store.dao.image.MarketImageFeatureDao
import com.tencent.devops.store.exception.image.ImageNotExistException
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_CODE
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_NAME
import com.tencent.devops.store.pojo.common.KEY_MODIFIER
import com.tencent.devops.store.pojo.common.KEY_PUBLISHER
import com.tencent.devops.store.pojo.common.KEY_UPDATE_TIME
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageAgentTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageRDTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
import com.tencent.devops.store.pojo.image.enums.MarketImageSortTypeEnum
import com.tencent.devops.store.pojo.image.response.ImageDetail
import com.tencent.devops.store.pojo.image.response.JobImageItem
import com.tencent.devops.store.pojo.image.response.JobMarketImageItem
import com.tencent.devops.store.service.common.StoreCommonService
import com.tencent.devops.store.service.common.StoreProjectService
import com.tencent.devops.store.service.common.StoreUserService
import com.tencent.devops.store.util.MultiSourceDataPaginator
import com.tencent.devops.store.util.PagableDataSource
import com.tencent.devops.store.utils.VersionUtils
import org.jooq.DSLContext
import org.jooq.Record
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import kotlin.math.ceil

@Suppress("ALL")
@Service
class ImageProjectService @Autowired constructor(
    private val dslContext: DSLContext,
    private val imageService: ImageService,
    private val storeUserService: StoreUserService,
    private val imageDao: ImageDao,
    private val marketImageDao: MarketImageDao,
    private val imageAgentTypeDao: ImageAgentTypeDao,
    private val marketImageFeatureDao: MarketImageFeatureDao,
    private val imageLabelService: ImageLabelService,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val storeProjectService: StoreProjectService,
    private val storeCommonService: StoreCommonService,
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
                accessToken = accessToken,
                projectCode = projectCode,
                userId = userId
            )
        if (result.isNotOk()) {
            throw ErrorCodeException(errorCode = StoreMessageCode.USER_QUERY_PROJECT_PERMISSION_IS_INVALID)
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
                    val lastPageRemainNum = if (totalAvailableAgentJobImageSize > 0) {
                        pageSize - totalAvailableAgentJobImageSize % pageSize
                    } else 0
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
        val defaultVersion = VersionUtils.convertLatestVersion(version)
        val imageStatus = it["imageStatus"] as Byte
        val dbClassifyId = it["classifyId"] as String
        val classifyCode = it["classifyCode"] as String
        val classifyName = it["classifyName"] as String
        val classifyLanName = I18nUtil.getCodeLanMessage(
            messageCode = "${StoreTypeEnum.IMAGE.name}.classify.$classifyCode",
            defaultMessage = classifyName
        )
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
            classifyName = classifyLanName,
            logoUrl = logoUrl,
            icon = icon,
            summary = summary,
            docsLink = storeCommonService.getStoreDetailUrl(StoreTypeEnum.IMAGE, imageCode),
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
            imageType = if (null != imageType) ImageRDTypeEnum.getImageRDTypeStr(imageType.toInt()) else null,
            weight = weight,
            recommendFlag = recommendFlag,
            labelNames = labelNames,
            availableFlag = isContainAgentType,
            modifier = modifier ?: "",
            updateTime = updateTime
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
            keyword = imageNamePart,
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
            val imageDetail = imageService.getLatestImageDetailByCode(userId, it.code, interfaceName)
            imageDetail.installedFlag = isInstalled
            resultList.add(imageDetail)
        }
        val count = marketImageDao.count(
            dslContext = dslContext,
            keyword = imageNamePart,
            classifyCodeList = classifyCodeList,
            labelCodeList = null,
            rdType = rdType,
            score = null,
            imageSourceType = null
        )
        return Page(
            count = count.toLong(),
            page = validPage,
            pageSize = validPageSize,
            totalPages = if (validPageSize > 0) ceil(count * 1.0 / validPageSize).toInt() else 1,
            records = resultList
        )
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
        return searchJobMarketImages(
            accessToken = accessToken,
            userId = userId,
            projectCode = projectCode,
            agentType = agentType,
            recommendFlag = recommendFlag,
            keyword = null,
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
        keyword: String?,
        classifyId: String?,
        categoryCode: String?,
        rdType: ImageRDTypeEnum?,
        page: Int?,
        pageSize: Int?,
        interfaceName: String? = "Anon interface"
    ): Page<JobMarketImageItem?>? {
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
        var testImageCodes = storeProjectRelDao.getTestStoreCodes(
            dslContext = dslContext,
            projectCode = projectCode,
            storeType = StoreTypeEnum.IMAGE
        )?.map { it.value1() }
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
                val watcher = Watcher(id = "JobMarketImageItem|$projectCode|$userId|$page|$pageSize")
                watcher.start("canInstallCurrentAgentJobMarketImages")
                val canInstallCurrentAgentJobMarketImages = marketImageDao.listCanInstallJobMarketImages(
                    dslContext = dslContext,
                    inImageCodes = agentTypeImageCodes,
                    notInImageCodes = null,
                    recommendFlag = recommendFlag,
                    keyword = keyword,
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
                LogUtils.printCostTimeWE(watcher = watcher)
                return canInstallCurrentAgentJobMarketImages
            }

            override fun getDataSize(): Int {
                val watcher = Watcher(id = "JobMarketImageItem|$projectCode|$userId|$page|$pageSize")
                watcher.start("count canInstallCurrentAgentJobMarketImages")
                val canInstallCurrentAgentJobMarketImagesCount = marketImageDao.countCanInstallJobMarketImages(
                    dslContext = dslContext,
                    inImageCodes = agentTypeImageCodes,
                    notInImageCodes = null,
                    recommendFlag = recommendFlag,
                    keyword = keyword,
                    classifyId = classifyId,
                    categoryCode = categoryCode,
                    rdType = rdType,
                    installedImageCodes = installImageCodes,
                    visibleImageCodes = visibleImageCodes
                )
                LogUtils.printCostTimeWE(watcher = watcher)
                return canInstallCurrentAgentJobMarketImagesCount
            }
        }
        // （2）未安装、可安装、agentType不符合的镜像
        val canInstallOtherAgentDataSource = object : PagableDataSource<JobMarketImageItem> {
            override fun getData(offset: Int, limit: Int): List<JobMarketImageItem> {
                val watcher = Watcher(id = "JobMarketImageItem|$projectCode|$userId|$page|$pageSize")
                watcher.start("canInstallOtherAgentJobMarketImages")
                val canInstallOtherAgentJobMarketImages = marketImageDao.listCanInstallJobMarketImages(
                    dslContext = dslContext,
                    inImageCodes = null,
                    notInImageCodes = agentTypeImageCodes,
                    recommendFlag = recommendFlag,
                    keyword = keyword,
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
                LogUtils.printCostTimeWE(watcher = watcher)
                return canInstallOtherAgentJobMarketImages
            }

            override fun getDataSize(): Int {
                val watcher = Watcher(id = "JobMarketImageItem|$projectCode|$userId|$page|$pageSize")
                watcher.start("count canInstallOtherAgentJobMarketImages")
                val canInstallOtherAgentJobMarketImagesCount = marketImageDao.countCanInstallJobMarketImages(
                    dslContext = dslContext,
                    inImageCodes = null,
                    notInImageCodes = agentTypeImageCodes,
                    recommendFlag = recommendFlag,
                    keyword = keyword,
                    classifyId = classifyId,
                    categoryCode = categoryCode,
                    rdType = rdType,
                    installedImageCodes = installImageCodes,
                    visibleImageCodes = visibleImageCodes
                )
                LogUtils.printCostTimeWE(watcher = watcher)
                return canInstallOtherAgentJobMarketImagesCount
            }
        }
        // （3.1）agentType符合的调试中镜像
        val agentTypeTestImageCodes = agentTypeImageCodes.intersect(testImageCodes)
        val testingCurrentAgentDataSource = object : PagableDataSource<JobMarketImageItem> {
            override fun getData(offset: Int, limit: Int): List<JobMarketImageItem> {
                val watcher = Watcher(id = "JobMarketImageItem|$projectCode|$userId|$page|$pageSize")
                watcher.start("testingCurrentAgentJobMarketImages")
                val testingCurrentAgentJobMarketImages = marketImageDao.listTestingJobMarketImages(
                    dslContext = dslContext,
                    // agentType符合与调试中镜像的交集
                    inImageCodes = agentTypeTestImageCodes,
                    notInImageCodes = null,
                    recommendFlag = recommendFlag,
                    keyword = keyword,
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
                LogUtils.printCostTimeWE(watcher = watcher)
                return testingCurrentAgentJobMarketImages
            }

            override fun getDataSize(): Int {
                val watcher = Watcher(id = "JobMarketImageItem|$projectCode|$userId|$page|$pageSize")
                watcher.start("count testingCurrentAgentJobMarketImages")
                val testingCurrentAgentJobMarketImagesCount = marketImageDao.countTestingJobMarketImages(
                    dslContext = dslContext,
                    inImageCodes = agentTypeTestImageCodes,
                    notInImageCodes = null,
                    recommendFlag = recommendFlag,
                    keyword = keyword,
                    classifyId = classifyId,
                    categoryCode = categoryCode,
                    rdType = rdType
                )
                LogUtils.printCostTimeWE(watcher = watcher)
                return testingCurrentAgentJobMarketImagesCount
            }
        }
        // （3.2）agentType不符合的调试中镜像
        val testingOtherAgentDataSource = object : PagableDataSource<JobMarketImageItem> {
            override fun getData(offset: Int, limit: Int): List<JobMarketImageItem> {
                val watcher = Watcher(id = "JobMarketImageItem|$projectCode|$userId|$page|$pageSize")
                watcher.start("testingOtherAgentJobMarketImages")
                val testingOtherAgentJobMarketImages = marketImageDao.listTestingJobMarketImages(
                    dslContext = dslContext,
                    // agentType符合与调试中镜像的交集
                    inImageCodes = testImageCodes,
                    notInImageCodes = agentTypeImageCodes,
                    recommendFlag = recommendFlag,
                    keyword = keyword,
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
                LogUtils.printCostTimeWE(watcher = watcher)
                return testingOtherAgentJobMarketImages
            }

            override fun getDataSize(): Int {
                val watcher = Watcher(id = "JobMarketImageItem|$projectCode|$userId|$page|$pageSize")
                watcher.start("count testingOtherAgentJobMarketImages")
                val testingOtherAgentJobMarketImagesCount = marketImageDao.countTestingJobMarketImages(
                    dslContext = dslContext,
                    inImageCodes = testImageCodes,
                    notInImageCodes = agentTypeImageCodes,
                    recommendFlag = recommendFlag,
                    keyword = keyword,
                    classifyId = classifyId,
                    categoryCode = categoryCode,
                    rdType = rdType
                )
                LogUtils.printCostTimeWE(watcher = watcher)
                return testingOtherAgentJobMarketImagesCount
            }
        }
        // （3.3）已安装、agentType符合的镜像（不含调试中镜像）
        val installedCurrentAgentDataSource = object : PagableDataSource<JobMarketImageItem> {
            override fun getData(offset: Int, limit: Int): List<JobMarketImageItem> {
                val watcher = Watcher(id = "JobMarketImageItem|$projectCode|$userId|$page|$pageSize")
                watcher.start("installedCurrentAgentJobMarketImages")
                val installedCurrentAgentJobMarketImages = marketImageDao.listInstalledJobMarketImages(
                    dslContext = dslContext,
                    inImageCodes = agentTypeImageCodes,
                    notInImageCodes = testImageCodes,
                    recommendFlag = recommendFlag,
                    keyword = keyword,
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
                LogUtils.printCostTimeWE(watcher = watcher)
                return installedCurrentAgentJobMarketImages
            }

            override fun getDataSize(): Int {
                val watcher = Watcher(id = "JobMarketImageItem|$projectCode|$userId|$page|$pageSize")
                watcher.start("count installedCurrentAgentJobMarketImages")
                val installedCurrentAgentJobMarketImagesCount = marketImageDao.countInstalledJobMarketImages(
                    dslContext = dslContext,
                    inImageCodes = agentTypeImageCodes,
                    notInImageCodes = null,
                    recommendFlag = recommendFlag,
                    keyword = keyword,
                    classifyId = classifyId,
                    categoryCode = categoryCode,
                    rdType = rdType,
                    installedImageCodes = installImageCodes
                )
                LogUtils.printCostTimeWE(watcher = watcher)
                return installedCurrentAgentJobMarketImagesCount
            }
        }
        // （4）已安装、agentType不符合的镜像
        val installedOtherAgentDataSource = object : PagableDataSource<JobMarketImageItem> {
            override fun getData(offset: Int, limit: Int): List<JobMarketImageItem> {
                val watcher = Watcher(id = "JobMarketImageItem|$projectCode|$userId|$page|$pageSize")
                watcher.start("installedOtherAgentJobMarketImages")
                val installedOtherAgentJobMarketImages = marketImageDao.listInstalledJobMarketImages(
                    dslContext = dslContext,
                    inImageCodes = null,
                    notInImageCodes = agentTypeImageCodes,
                    recommendFlag = recommendFlag,
                    keyword = keyword,
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
                LogUtils.printCostTimeWE(watcher = watcher)
                return installedOtherAgentJobMarketImages
            }

            override fun getDataSize(): Int {
                val watcher = Watcher(id = "JobMarketImageItem|$projectCode|$userId|$page|$pageSize")
                watcher.start("count installedOtherAgentJobMarketImages")
                val installedOtherAgentJobMarketImagesCount = marketImageDao.countInstalledJobMarketImages(
                    dslContext = dslContext,
                    inImageCodes = null,
                    notInImageCodes = agentTypeImageCodes,
                    recommendFlag = recommendFlag,
                    keyword = keyword,
                    classifyId = classifyId,
                    categoryCode = categoryCode,
                    rdType = rdType,
                    installedImageCodes = installImageCodes
                )
                LogUtils.printCostTimeWE(watcher = watcher)
                return installedOtherAgentJobMarketImagesCount
            }
        }
        // （5）不可见、agentType符合的镜像
        val noVisibleCurrentAgentDataSource = object : PagableDataSource<JobMarketImageItem> {
            override fun getData(offset: Int, limit: Int): List<JobMarketImageItem> {
                val watcher = Watcher(id = "JobMarketImageItem|$projectCode|$userId|$page|$pageSize")
                watcher.start("noVisibleCurrentAgentJobMarketImages")
                val noVisibleCurrentAgentJobMarketImages = marketImageDao.listNoVisibleJobMarketImages(
                    dslContext = dslContext,
                    inImageCodes = agentTypeImageCodes,
                    notInImageCodes = null,
                    recommendFlag = recommendFlag,
                    keyword = keyword,
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
                LogUtils.printCostTimeWE(watcher = watcher)
                return noVisibleCurrentAgentJobMarketImages
            }

            override fun getDataSize(): Int {
                val watcher = Watcher(id = "JobMarketImageItem|$projectCode|$userId|$page|$pageSize")
                watcher.start("count noVisibleCurrentAgentJobMarketImages")
                val noVisibleCurrentAgentJobMarketImagesCount = marketImageDao.countNoVisibleJobMarketImages(
                    dslContext = dslContext,
                    inImageCodes = agentTypeImageCodes,
                    notInImageCodes = null,
                    recommendFlag = recommendFlag,
                    keyword = keyword,
                    classifyId = classifyId,
                    categoryCode = categoryCode,
                    rdType = rdType,
                    visibleImageCodes = visibleImageCodes
                )
                LogUtils.printCostTimeWE(watcher = watcher)
                return noVisibleCurrentAgentJobMarketImagesCount
            }
        }
        // （6）不可见、agentType不符合的镜像
        val noVisibleOtherAgentDataSource = object : PagableDataSource<JobMarketImageItem> {
            override fun getData(offset: Int, limit: Int): List<JobMarketImageItem> {
                val watcher = Watcher(id = "JobMarketImageItem|$projectCode|$userId|$page|$pageSize")
                watcher.start("noVisibleOtherAgentJobMarketImages")
                val noVisibleOtherAgentJobMarketImages = marketImageDao.listNoVisibleJobMarketImages(
                    dslContext = dslContext,
                    inImageCodes = null,
                    notInImageCodes = agentTypeImageCodes,
                    recommendFlag = recommendFlag,
                    keyword = keyword,
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
                LogUtils.printCostTimeWE(watcher = watcher)
                return noVisibleOtherAgentJobMarketImages
            }

            override fun getDataSize(): Int {
                val watcher = Watcher(id = "JobMarketImageItem|$projectCode|$userId|$page|$pageSize")
                watcher.start("count noVisibleOtherAgentJobMarketImages")
                val noVisibleOtherAgentJobMarketImagesCount = marketImageDao.countNoVisibleJobMarketImages(
                    dslContext = dslContext,
                    inImageCodes = null,
                    notInImageCodes = agentTypeImageCodes,
                    recommendFlag = recommendFlag,
                    keyword = keyword,
                    classifyId = classifyId,
                    categoryCode = categoryCode,
                    rdType = rdType,
                    visibleImageCodes = visibleImageCodes
                )
                LogUtils.printCostTimeWE(watcher = watcher)
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
        return Page(
            page = validPage,
            pageSize = validPageSize,
            count = count,
            records = resultList
        )
    }

    fun genJobMarketImageItem(
        it: Record,
        canInstallFlag: Boolean,
        installFlag: Boolean,
        availableFlag: Boolean
    ): JobMarketImageItem {
        val id = it.get(KEY_IMAGE_ID) as String
        val code = it.get(KEY_IMAGE_CODE) as String
        val name = it.get(KEY_IMAGE_NAME) as String
        val rdType = ImageRDTypeEnum.getImageRDTypeStr((it.get(KEY_IMAGE_RD_TYPE) as Byte?)?.toInt())

        // 单独查询agentTypeScope
        val agentTypeScopeStr = it.get(KEY_IMAGE_AGENT_TYPE_SCOPE) as String?
        val agentTypeScope = agentTypeScopeStr?.split(",")?.map { ImageAgentTypeEnum.getImageAgentType(it)!! }?.toList()
            ?: emptyList()

        val logoUrl = it.get(KEY_IMAGE_LOGO_URL) as String?
        val icon = it.get(KEY_IMAGE_ICON) as String?
        val summary = it.get(KEY_IMAGE_SUMMARY) as String?
        val docsLink = storeCommonService.getStoreDetailUrl(StoreTypeEnum.IMAGE, code)
        val weight = it.get(KEY_IMAGE_FEATURE_WEIGHT) as Int?
        val imageSourceType = ImageType.getType(it.get(KEY_IMAGE_SOURCE_TYPE) as String).name
        val imageRepoUrl = it.get(KEY_IMAGE_REPO_URL) as String?
        val imageRepoName = it.get(KEY_IMAGE_REPO_NAME) as String
        val imageTag = it.get(KEY_IMAGE_TAG) as String
        // 单独查询
        val labelList = imageLabelService.getLabelsByImageId(id).data
        val category = it.get(KEY_CATEGORY_CODE) as String?
        val categoryName = it.get(KEY_CATEGORY_NAME) as String?
        val categoryLanName = I18nUtil.getCodeLanMessage(
            messageCode = "${StoreTypeEnum.IMAGE.name}.category.$category",
            defaultMessage = categoryName
        )
        val publisher = it.get(KEY_PUBLISHER) as String
        val publicFlag = it.get(KEY_IMAGE_FEATURE_PUBLIC_FLAG) as Boolean
        val recommendFlag = it.get(KEY_IMAGE_FEATURE_RECOMMEND_FLAG) as Boolean
        val certificationFlag = it.get(KEY_IMAGE_FEATURE_CERTIFICATION_FLAG) as Boolean
        val modifier = it.get(KEY_MODIFIER) as String?
        val updateTime = (it.get(KEY_UPDATE_TIME) as LocalDateTime).timestampmilli()
        val dockerFileType = it.get(KEY_IMAGE_DOCKER_FILE_TYPE) as String? ?: "INPUT"
        val dockerFileContent = it.get(KEY_IMAGE_DOCKER_FILE_CONTENT) as String? ?: ""
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
            dockerFileType = dockerFileType,
            dockerFileContent = dockerFileContent,
            labelNames = labelList?.map { it.labelName }?.joinToString { it } ?: "",
            category = category ?: "",
            categoryName = categoryLanName,
            publisher = publisher,
            publicFlag = publicFlag,
            flag = canInstallFlag,
            recommendFlag = recommendFlag,
            certificationFlag = certificationFlag,
            // 公共镜像视为已安装
            installedFlag = installFlag || publicFlag,
            modifier = modifier ?: "",
            updateTime = updateTime
        )
    }

    /**
     * 安装镜像到项目
     */
    fun installImage(
        userId: String,
        projectCodeList: ArrayList<String>,
        imageCode: String,
        channelCode: ChannelCode,
        interfaceName: String? = "Anon interface"
    ): Result<Boolean> {
        logger.info("$interfaceName:installImage:Input:($userId,$projectCodeList,$imageCode)")
        // 判断镜像标识是否合法
        val image = marketImageDao.getLatestImageByCode(dslContext, imageCode)
            ?: throw ImageNotExistException("imageCode=$imageCode")
        val imageFeature = marketImageFeatureDao.getExistedImageFeature(dslContext, imageCode)
        val validateInstallResult = storeProjectService.validateInstallPermission(
            publicFlag = imageFeature.publicFlag,
            userId = userId,
            storeCode = image.imageCode,
            storeType = StoreTypeEnum.IMAGE,
            projectCodeList = projectCodeList
        )
        if (validateInstallResult.isNotOk()) {
            return validateInstallResult
        }
        return storeProjectService.installStoreComponent(
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

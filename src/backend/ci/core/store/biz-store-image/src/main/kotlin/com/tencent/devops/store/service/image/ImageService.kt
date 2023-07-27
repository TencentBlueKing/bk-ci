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

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.store.tables.records.TImageRecord
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.constant.StoreMessageCode.NO_COMPONENT_ADMIN_PERMISSION
import com.tencent.devops.store.constant.StoreMessageCode.GET_INFO_NO_PERMISSION
import com.tencent.devops.store.constant.StoreMessageCode.USER_IMAGE_VERSION_NOT_EXIST
import com.tencent.devops.store.dao.common.CategoryDao
import com.tencent.devops.store.dao.common.ClassifyDao
import com.tencent.devops.store.dao.common.StoreHonorDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.dao.image.Constants
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_CODE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_FEATURE_PUBLIC_FLAG
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_FEATURE_RECOMMEND_FLAG
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_ID
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_LOGO_URL
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_NAME
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_RD_TYPE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_REPO_NAME
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_REPO_URL
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_SIZE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_SOURCE_TYPE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_STATUS
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_SUMMARY
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_TAG
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_VERSION
import com.tencent.devops.store.dao.image.ImageAgentTypeDao
import com.tencent.devops.store.dao.image.ImageCategoryRelDao
import com.tencent.devops.store.dao.image.ImageDao
import com.tencent.devops.store.dao.image.ImageFeatureDao
import com.tencent.devops.store.dao.image.ImageLabelRelDao
import com.tencent.devops.store.dao.image.ImageVersionLogDao
import com.tencent.devops.store.dao.image.MarketImageDao
import com.tencent.devops.store.dao.image.MarketImageFeatureDao
import com.tencent.devops.store.exception.image.CategoryNotExistException
import com.tencent.devops.store.exception.image.ImageNotExistException
import com.tencent.devops.store.pojo.common.HOTTEST
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_CODE
import com.tencent.devops.store.pojo.common.KEY_CLASSIFY_ID
import com.tencent.devops.store.pojo.common.KEY_CREATE_TIME
import com.tencent.devops.store.pojo.common.KEY_CREATOR
import com.tencent.devops.store.pojo.common.KEY_MODIFIER
import com.tencent.devops.store.pojo.common.KEY_PUBLISHER
import com.tencent.devops.store.pojo.common.KEY_PUB_TIME
import com.tencent.devops.store.pojo.common.KEY_UPDATE_TIME
import com.tencent.devops.store.pojo.common.LATEST
import com.tencent.devops.store.pojo.common.MarketItem
import com.tencent.devops.store.pojo.common.STORE_IMAGE_STATUS
import com.tencent.devops.store.pojo.common.VersionInfo
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageAgentTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageRDTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
import com.tencent.devops.store.pojo.image.enums.MarketImageSortTypeEnum
import com.tencent.devops.store.pojo.image.exception.UnknownImageSourceType
import com.tencent.devops.store.pojo.image.request.ImageBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.image.response.ImageDetail
import com.tencent.devops.store.pojo.image.response.ImageRepoInfo
import com.tencent.devops.store.pojo.image.response.MarketImageItem
import com.tencent.devops.store.pojo.image.response.MarketImageMain
import com.tencent.devops.store.pojo.image.response.MarketImageResp
import com.tencent.devops.store.pojo.image.response.MyImage
import com.tencent.devops.store.service.common.ClassifyService
import com.tencent.devops.store.service.common.StoreCommentService
import com.tencent.devops.store.service.common.StoreCommonService
import com.tencent.devops.store.service.common.StoreHonorService
import com.tencent.devops.store.service.common.StoreIndexManageService
import com.tencent.devops.store.service.common.StoreMemberService
import com.tencent.devops.store.service.common.StoreTotalStatisticService
import com.tencent.devops.store.service.common.StoreUserService
import com.tencent.devops.store.util.ImageUtil
import java.time.LocalDateTime
import java.util.Date
import kotlin.math.ceil
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.stereotype.Service

@Suppress("ALL")
@RefreshScope
@Service
abstract class ImageService @Autowired constructor() {
    @Autowired
    lateinit var dslContext: DSLContext
    @Autowired
    lateinit var imageDao: ImageDao
    @Autowired
    lateinit var imageCategoryRelDao: ImageCategoryRelDao
    @Autowired
    lateinit var classifyDao: ClassifyDao
    @Autowired
    lateinit var categoryDao: CategoryDao
    @Autowired
    lateinit var imageFeatureDao: ImageFeatureDao
    @Autowired
    lateinit var imageAgentTypeDao: ImageAgentTypeDao
    @Autowired
    lateinit var imageVersionLogDao: ImageVersionLogDao
    @Autowired
    lateinit var imageLabelRelDao: ImageLabelRelDao
    @Autowired
    lateinit var marketImageDao: MarketImageDao
    @Autowired
    lateinit var marketImageFeatureDao: MarketImageFeatureDao
    @Autowired
    lateinit var storeMemberDao: StoreMemberDao
    @Autowired
    lateinit var storeHonorDao: StoreHonorDao
    @Autowired
    lateinit var storeIndexManageService: StoreIndexManageService
    @Autowired
    lateinit var storeHonorService: StoreHonorService
    @Autowired
    lateinit var storeProjectRelDao: StoreProjectRelDao
    @Autowired
    lateinit var imageCommonService: ImageCommonService
    @Autowired
    lateinit var storeCommentService: StoreCommentService
    @Autowired
    lateinit var storeUserService: StoreUserService
    @Autowired
    @Qualifier("imageMemberService")
    lateinit var storeMemberService: StoreMemberService
    @Autowired
    lateinit var classifyService: ClassifyService
    @Autowired
    lateinit var supportService: SupportService
    @Autowired
    lateinit var storeTotalStatisticService: StoreTotalStatisticService
    @Autowired
    lateinit var imageLabelService: ImageLabelService
    @Autowired
    lateinit var imageCategoryService: ImageCategoryService
    @Autowired
    lateinit var storeCommonService: StoreCommonService
    @Autowired
    lateinit var client: Client

    private val logger = LoggerFactory.getLogger(ImageService::class.java)

    fun getImageVersionListByCode(
        userId: String,
        imageCode: String,
        page: Int?,
        pageSize: Int?,
        interfaceName: String? = "Anon interface"
    ): Result<Page<ImageDetail>> {
        logger.info("$interfaceName:getImageVersionListByCode:Input:($userId,$imageCode,$page,$pageSize)")
        // 判断当前用户是否是该镜像的成员
        if (!storeMemberDao.isStoreMember(
                dslContext = dslContext,
                userId = userId,
                storeCode = imageCode,
                storeType = StoreTypeEnum.IMAGE.type.toByte()
            )
        ) {
            throw ErrorCodeException(
                errorCode = GET_INFO_NO_PERMISSION,
                params = arrayOf(imageCode)
            )
        }
        // 参数校验
        val validPage = PageUtil.getValidPage(page)
        // 默认拉取所有
        val validPageSize = pageSize ?: -1
        // 查数据库
        val count = imageDao.countByCode(dslContext, imageCode)
        val imageVersionList = imageDao.listImageByCode(
            dslContext = dslContext,
            imageCode = imageCode,
            page = page,
            pageSize = pageSize
        )?.map { it ->
            val imageId = it.get(KEY_IMAGE_ID) as String
            getImageDetailById(userId, imageId, interfaceName)
        } ?: emptyList()
        val pageObj = Page(
            count = count.toLong(),
            page = validPage,
            pageSize = validPageSize,
            totalPages = if (validPageSize > 0) ceil(count * 1.0 / validPageSize).toInt() else -1,
            records = imageVersionList
        )
        return Result(pageObj)
    }

    @Suppress("UNCHECKED_CAST")
    fun count(
        userId: String,
        userDeptList: List<Int>,
        keyword: String?,
        classifyCodeList: List<String>?,
        categoryCodeList: List<String>?,
        rdType: ImageRDTypeEnum?,
        labelCode: String?,
        score: Int?,
        imageSourceType: ImageType?,
        interfaceName: String? = "Anon interface"
    ): Int {
        // 获取镜像
        val labelCodeList = if (labelCode.isNullOrEmpty()) listOf() else labelCode.split(",")
        return marketImageDao.count(
            dslContext = dslContext,
            keyword = keyword,
            classifyCodeList = classifyCodeList,
            categoryCodeList = categoryCodeList,
            rdType = rdType,
            labelCodeList = labelCodeList,
            score = score,
            imageSourceType = imageSourceType
        )
    }

    @Suppress("UNCHECKED_CAST")
    fun doList(
        userId: String,
        userDeptList: List<Int>,
        keyword: String?,
        classifyCodeList: List<String>?,
        categoryCodeList: List<String>?,
        rdType: ImageRDTypeEnum?,
        labelCode: String?,
        score: Int?,
        imageSourceType: ImageType?,
        sortType: MarketImageSortTypeEnum?,
        desc: Boolean?,
        page: Int?,
        pageSize: Int?,
        interfaceName: String? = "Anon interface"
    ): List<MarketImageItem> {
        val results = mutableListOf<MarketImageItem>()

        // 获取镜像
        val labelCodeList = if (labelCode.isNullOrEmpty()) listOf() else labelCode.split(",")
        val images = marketImageDao.list(
            dslContext = dslContext,
            keyword = keyword,
            classifyCodeList = classifyCodeList,
            categoryCodeList = categoryCodeList,
            rdType = rdType,
            labelCodeList = labelCodeList,
            score = score,
            imageSourceType = imageSourceType,
            sortType = sortType,
            desc = desc,
            page = page,
            pageSize = pageSize
        )
            ?: return emptyList()

        val imageCodeList = images.map {
            it[KEY_IMAGE_CODE] as String
        }.toList()
        logger.info("$interfaceName:doList:Inner:imageCodeList.size=${imageCodeList.size},imageCodeList=$imageCodeList")

        // 获取可见范围
        val storeType = StoreTypeEnum.IMAGE
        val imageVisibleData = storeCommonService.generateStoreVisibleData(imageCodeList, storeType)
        val imageVisibleDataStr = StringBuilder("\n")
        imageVisibleData?.forEach {
            imageVisibleDataStr.append("${it.key}->${it.value}\n")
        }
        val imageStatisticData = storeTotalStatisticService.getStatisticByCodeList(
            storeType = storeType.type.toByte(),
            storeCodeList = imageCodeList
        )
        val imageHonorInfoMap = storeHonorService.getHonorInfosByStoreCodes(storeType, imageCodeList)
        val imageIndexInfosMap = storeIndexManageService.getStoreIndexInfosByStoreCodes(storeType, imageCodeList)
        // 获取用户
        val memberData = storeMemberService.batchListMember(imageCodeList, storeType).data

        // 获取分类
        val classifyList = classifyService.getAllClassify(storeType.type.toByte()).data
        val classifyMap = mutableMapOf<String, String>()
        classifyList?.forEach {
            classifyMap[it.id] = it.classifyCode
        }

        images.forEach {
            val imageCode = it[KEY_IMAGE_CODE] as String
            val visibleList = imageVisibleData?.get(imageCode)
            val statistic = imageStatisticData[imageCode]
            val honorInfos = imageHonorInfoMap[imageCode]
            val indexInfos = imageIndexInfosMap[imageCode]
            val members = memberData?.get(imageCode)

            val installFlag = storeCommonService.generateInstallFlag(
                defaultFlag = it[KEY_IMAGE_FEATURE_PUBLIC_FLAG] as Boolean,
                members = members,
                userId = userId,
                visibleList = visibleList,
                userDeptList = userDeptList
            )
            val classifyId = it[KEY_CLASSIFY_ID] as String
            val (imageSizeNum, imageSize) = getImageSizeInfoByStr(it.get(KEY_IMAGE_SIZE) as String)
            results.add(
                MarketImageItem(
                    id = it[KEY_IMAGE_ID] as String,
                    code = imageCode,
                    name = it[KEY_IMAGE_NAME] as String,
                    rdType = ImageRDTypeEnum.getImageRDTypeStr((it[KEY_IMAGE_RD_TYPE] as Byte?)?.toInt()),
                    imageSourceType = ImageType.getType(it[KEY_IMAGE_SOURCE_TYPE] as String).name,
                    imageSize = imageSize,
                    imageSizeNum = imageSizeNum,
                    classifyCode = if (classifyMap.containsKey(classifyId)) classifyMap[classifyId] ?: "" else "",
                    logoUrl = it[KEY_IMAGE_LOGO_URL] as? String,
                    version = it[KEY_IMAGE_VERSION] as String,
                    summary = it[KEY_IMAGE_SUMMARY] as? String,
                    score = statistic?.score ?: 0.toDouble(),
                    downloads = statistic?.downloads ?: 0,
                    publicFlag = it[KEY_IMAGE_FEATURE_PUBLIC_FLAG] as Boolean,
                    flag = installFlag,
                    recommendFlag = it[KEY_IMAGE_FEATURE_RECOMMEND_FLAG] as Boolean,
                    publisher = it[KEY_PUBLISHER] as String,
                    pubTime = (it[KEY_PUB_TIME] as LocalDateTime?)?.timestampmilli(),
                    creator = it[KEY_CREATOR] as String,
                    modifier = it[KEY_MODIFIER] as String,
                    createTime = (it[KEY_CREATE_TIME] as LocalDateTime).timestampmilli(),
                    updateTime = (it[KEY_UPDATE_TIME] as LocalDateTime).timestampmilli(),
                    installedFlag = null,
                    honorInfos = honorInfos,
                    indexInfos = indexInfos,
                    hotFlag = statistic?.hotFlag
                )
            )
        }
        return results
    }

    private fun getDefaultDescTypeBySortType(sortType: MarketImageSortTypeEnum?): Boolean {
        return when (sortType) {
            // 名称与发布者升序
            MarketImageSortTypeEnum.NAME, MarketImageSortTypeEnum.PUBLISHER -> {
                false
            }
            // 其他含数量意义的指标降序
            else -> {
                true
            }
        }
    }

    /**
     * 镜像市场搜索镜像
     */
    fun searchImage(
        userId: String,
        keyword: String?,
        imageSourceType: ImageType?,
        classifyCode: String?,
        categoryCode: String?,
        rdType: ImageRDTypeEnum?,
        labelCode: String?,
        score: Int?,
        sortType: MarketImageSortTypeEnum?,
        page: Int?,
        pageSize: Int?,
        interfaceName: String? = "Anon interface"
    ): Result<MarketImageResp> {
        // 获取用户组织架构
        val userDeptList = storeUserService.getUserDeptList(userId)
        val result = MarketImageResp(
            count = count(
                userId = userId,
                userDeptList = userDeptList,
                keyword = keyword,
                classifyCodeList = if (null != classifyCode) listOf(classifyCode) else null,
                categoryCodeList = if (null != categoryCode) listOf(categoryCode) else null,
                rdType = rdType,
                labelCode = labelCode,
                score = score,
                imageSourceType = imageSourceType
            ),
            page = page,
            pageSize = pageSize,
            records = doList(
                userId = userId,
                userDeptList = userDeptList,
                keyword = keyword,
                classifyCodeList = if (null != classifyCode) listOf(classifyCode) else null,
                categoryCodeList = if (null != categoryCode) listOf(categoryCode) else null,
                rdType = rdType,
                labelCode = labelCode,
                score = score,
                imageSourceType = imageSourceType,
                sortType = sortType,
                desc = getDefaultDescTypeBySortType(sortType),
                page = page,
                pageSize = pageSize,
                interfaceName = interfaceName
            ).map {
                val categories = imageCategoryRelDao.getCategorysByImageId(dslContext, it.id)?.map { categoryRecord ->
                    categoryRecord.get(KEY_CATEGORY_CODE) as String
                } ?: emptyList()
                MarketItem(
                    id = it.id,
                    name = it.name,
                    code = it.code,
                    version = it.version,
                    // 仅用于插件区分Agent/AgentLess
                    type = "",
                    rdType = it.rdType,
                    classifyCode = it.classifyCode,
                    category = categories.joinToString(","),
                    logoUrl = it.logoUrl,
                    publisher = it.publisher ?: "",
                    os = emptyList(),
                    downloads = it.downloads,
                    score = it.score,
                    summary = it.summary,
                    flag = it.flag,
                    publicFlag = it.publicFlag,
                    buildLessRunFlag = false,
                    docsLink = storeCommonService.getStoreDetailUrl(StoreTypeEnum.IMAGE, it.code),
                    modifier = it.modifier,
                    updateTime = DateTimeUtil.formatDate(Date(it.updateTime)),
                    recommendFlag = it.recommendFlag,
                    hotFlag = it.hotFlag
                )
            }
        )
        return Result(result)
    }

    /**
     * 首页镜像列表
     */
    fun mainPageList(
        userId: String,
        page: Int?,
        pageSize: Int?,
        interfaceName: String? = "Anon interface"
    ): Result<List<MarketImageMain>> {
        logger.info("$interfaceName:mainPageList:Input:($userId,$page,$pageSize)")
        val result = mutableListOf<MarketImageMain>()
        // 获取用户组织架构
        val userDeptList = storeUserService.getUserDeptList(userId)
        logger.info("$interfaceName:mainPageList:Inner:userDeptList=$userDeptList")
        result.add(
            MarketImageMain(
                key = LATEST,
                label = I18nUtil.getCodeLanMessage(messageCode = LATEST, language = I18nUtil.getLanguage(userId)),
                records = doList(
                    userId = userId,
                    userDeptList = userDeptList,
                    keyword = null,
                    classifyCodeList = null,
                    categoryCodeList = null,
                    rdType = null,
                    labelCode = null,
                    score = null,
                    imageSourceType = null,
                    sortType = MarketImageSortTypeEnum.UPDATE_TIME,
                    desc = true,
                    page = page,
                    pageSize = pageSize,
                    interfaceName = interfaceName
                )
            )
        )
        result.add(
            MarketImageMain(
                key = HOTTEST,
                label = I18nUtil.getCodeLanMessage(messageCode = HOTTEST, language = I18nUtil.getLanguage(userId)),
                records = doList(
                    userId = userId,
                    userDeptList = userDeptList,
                    keyword = null,
                    classifyCodeList = null,
                    categoryCodeList = null,
                    rdType = null,
                    labelCode = null,
                    score = null,
                    imageSourceType = null,
                    sortType = MarketImageSortTypeEnum.DOWNLOAD_COUNT,
                    desc = true,
                    page = page,
                    pageSize = pageSize,
                    interfaceName = interfaceName
                )
            )
        )
        val classifyList = classifyDao.getAllClassify(dslContext, StoreTypeEnum.IMAGE.type.toByte())
        classifyList.forEach {
            val classifyCode = it.classifyCode
            if (classifyCode != "trigger") {
                val classifyLanName = I18nUtil.getCodeLanMessage(
                    messageCode = "${StoreTypeEnum.IMAGE.name}.classify.$classifyCode",
                    defaultMessage = it.classifyName,
                    language = I18nUtil.getLanguage(userId)
                )
                result.add(
                    MarketImageMain(
                        key = classifyCode,
                        label = classifyLanName,
                        records = doList(
                            userId = userId,
                            userDeptList = userDeptList,
                            keyword = null,
                            classifyCodeList = listOf(classifyCode),
                            categoryCodeList = null,
                            rdType = null,
                            labelCode = null,
                            score = null,
                            imageSourceType = null,
                            sortType = MarketImageSortTypeEnum.DOWNLOAD_COUNT,
                            desc = true,
                            page = page,
                            pageSize = pageSize,
                            interfaceName = interfaceName
                        )
                    )
                )
            }
        }
        logger.info("$interfaceName:mainPageList:Output:result.size=${result.size}")
        return Result(result)
    }

    fun getMyImageList(
        userId: String,
        imageName: String?,
        page: Int?,
        pageSize: Int?,
        interfaceName: String? = "Anon interface"
    ): Result<Page<MyImage>> {
        logger.info("$interfaceName:getMyImageList:Input:($userId:$imageName:$page:$pageSize)")
        // 参数校验
        val validPage = PageUtil.getValidPage(page)
        // 默认拉取所有
        val validPageSize = pageSize ?: -1
        val projectCodeList = mutableListOf<String>()
        val myImageCodeList = mutableListOf<String>()
        // 查数据库，弱一致，无需事务
        // 1.查总数
        val count = imageDao.countByUserIdAndName(dslContext, userId, imageName)
        // 2.查分页列表
        val myImageRecords = imageDao.listImageByNameLike(
            dslContext = dslContext,
            userId = userId,
            imageName = imageName,
            page = validPage,
            pageSize = validPageSize
        )
        myImageRecords?.forEach {
            val imageCode = it.get(KEY_IMAGE_CODE) as String
            val projectCode = storeProjectRelDao.getUserStoreTestProjectCode(
                dslContext = dslContext,
                userId = userId,
                storeCode = imageCode,
                storeType = StoreTypeEnum.IMAGE) ?: ""
            myImageCodeList.add(imageCode)
            projectCodeList.add(projectCode)
        }

        // 根据projectCodeList调用一次微服务接口批量获取projectName
        val projectListResult = client.get(ServiceProjectResource::class).listByProjectCodeList(projectCodeList)
        val projectList = projectListResult.data!!
        val projectListIdsStr = StringBuilder()
        projectList.forEach {
            projectListIdsStr.append(it.id)
            projectListIdsStr.append(",")
        }
        logger.info("$interfaceName:getMyImageList:Inner:projectList.size=${projectList.size}:$projectListIdsStr")
        // 封装结果返回
        val myImageList = ArrayList<MyImage>()
        val end = myImageRecords?.size ?: 0
        for (i in 0 until end) {
            val it = myImageRecords!![i]
            val imageCode = myImageCodeList[i]
            val projectCode = projectCodeList[i]
            val projectV0 = projectList[i]
            val projectName = projectV0.projectName
            // enable字段为null时默认为true
            val projectEnabled = projectV0.enabled ?: true
            val (imageSizeNum, imageSize) = getImageSizeInfoByStr(it.get(KEY_IMAGE_SIZE) as String)
            myImageList.add(
                MyImage(
                    imageId = it.get(KEY_IMAGE_ID) as String,
                    imageCode = imageCode,
                    imageName = it.get(KEY_IMAGE_NAME) as String,
                    imageSourceType = ImageType.getType((it.get(KEY_IMAGE_SOURCE_TYPE) as String)).name,
                    imageRepoUrl = (it.get(KEY_IMAGE_REPO_URL) as String?) ?: "",
                    imageRepoName = it.get(KEY_IMAGE_REPO_NAME) as String,
                    version = it.get(KEY_IMAGE_VERSION) as String,
                    imageTag = (it.get(KEY_IMAGE_TAG) as String?) ?: "",
                    imageSize = imageSize,
                    imageSizeNum = imageSizeNum,
                    imageStatus = ImageStatusEnum.getImageStatus((it.get(KEY_IMAGE_STATUS) as Byte).toInt()),
                    projectCode = projectCode,
                    projectName = projectName,
                    projectEnabled = projectEnabled,
                    creator = it.get(KEY_CREATOR) as String,
                    modifier = it.get(KEY_MODIFIER) as String,
                    createTime = (it.get(KEY_CREATE_TIME) as LocalDateTime).timestampmilli(),
                    updateTime = (it.get(KEY_UPDATE_TIME) as LocalDateTime).timestampmilli(),
                    publicFlag = (it.get(KEY_IMAGE_FEATURE_PUBLIC_FLAG) as Boolean? ?: false)
                )
            )
        }
        val pageObj = Page(
            count = count.toLong(),
            page = validPage,
            pageSize = validPageSize,
            totalPages = if (validPageSize > 0) ceil(count * 1.0 / validPageSize).toInt() else -1,
            records = myImageList
        )
        return Result(pageObj)
    }

    fun getImageDetailById(
        userId: String,
        imageId: String,
        interfaceName: String? = "Anon interface"
    ): ImageDetail {
        logger.info("$interfaceName:getImageDetailById:Input:($userId,$imageId)")
        val imageRecord =
            imageDao.getImage(dslContext, imageId) ?: throw InvalidParamException(
                "image is null,imageId=$imageId",
                params = arrayOf(imageId)
            )
        return getImageDetail(userId, imageRecord)
    }

    @Value("\${store.buildResultBaseUrl}")
    private lateinit var buildResultBaseUrl: String

    fun getImageRepoInfoByCodeAndVersion(
        userId: String,
        projectCode: String,
        pipelineId: String?,
        buildId: String?,
        imageCode: String,
        imageVersion: String?,
        interfaceName: String? = "Anon interface"
    ): ImageRepoInfo {
        // 区分是否为调试项目
        val imageStatusList = imageCommonService.generateImageStatusList(imageCode, projectCode)
        val imageRecords =
            imageDao.getImagesByBaseVersion(
                dslContext = dslContext,
                imageCode = imageCode,
                imageStatusSet = imageStatusList.toSet(),
                baseVersion = imageVersion
            )
        imageRecords?.sortWith(Comparator { o1, o2 ->
            ImageUtil.compareVersion(o2.get(KEY_IMAGE_VERSION) as String?, o1.get(KEY_IMAGE_VERSION) as String?)
        })
        val latestImage = imageRecords?.get(0)
        return if (null == latestImage) {
            // 运行时异常情况兜底，通知管理员
            val titleParams = mutableMapOf<String, String>()
            titleParams["userId"] = userId
            titleParams["projectCode"] = projectCode
            titleParams["imageCode"] = imageCode
            titleParams["imageVersion"] = imageVersion ?: ""
            val bodyParams = mutableMapOf<String, String>()
            bodyParams["userId"] = userId
            bodyParams["projectCode"] = projectCode
            bodyParams["imageCode"] = imageCode
            bodyParams["imageVersion"] = imageVersion ?: ""
            bodyParams["pipelineId"] = pipelineId ?: ""
            bodyParams["buildId"] = buildId ?: ""
            bodyParams["url"] = buildResultBaseUrl.removeSuffix("/") + "/$projectCode/$pipelineId/detail/$buildId"
            try {
                supportService.sendImageExecuteNullToManagers(titleParams, bodyParams)
            } catch (ignored: Throwable) {
                // 通知失败不应影响执行
                logger.warn("sendImageExecuteNullToManagers fail", ignored)
            }
            getDefaultImageRepoInfo()
        } else {
            getImageRepoInfoByRecord(latestImage)
        }
    }

    fun getSelfDevelopPublicImages(
        interfaceName: String? = "Anon interface"
    ): List<ImageRepoInfo> {
        val records = imageDao.listRunnableSelfDevelopPublicImages(dslContext)
        return records?.map {
            getImageRepoInfoByRecord(it)
        } ?: emptyList()
    }

    @Value("\${store.defaultImageSourceType}")
    private lateinit var defaultImageSourceType: String
    @Value("\${store.defaultImageRepoUrl}")
    private lateinit var defaultImageRepoUrl: String
    @Value("\${store.defaultImageRepoName}")
    private lateinit var defaultImageRepoName: String
    @Value("\${store.defaultImageTag}")
    private lateinit var defaultImageTag: String
    @Value("\${store.defaultTicketId}")
    private lateinit var defaultTicketId: String
    @Value("\${store.defaultTicketProject}")
    private lateinit var defaultTicketProject: String
    @Value("\${store.defaultImageRDType}")
    private lateinit var defaultImageRDType: String

    fun getDefaultImageRepoInfo(): ImageRepoInfo {
        return ImageRepoInfo(
            sourceType = ImageType.getType(defaultImageSourceType),
            repoUrl = defaultImageRepoUrl,
            repoName = defaultImageRepoName,
            repoTag = defaultImageTag,
            ticketId = defaultTicketId,
            ticketProject = defaultTicketProject,
            publicFlag = true,
            rdType = ImageRDTypeEnum.getImageRDTypeByName(defaultImageRDType)
        )
    }

    fun getImageRepoInfoByRecord(imageRecord: Record): ImageRepoInfo {
        val id = imageRecord.get(KEY_IMAGE_ID) as String
        val imageCode = imageRecord.get(KEY_IMAGE_CODE) as String
        val imageFeature = imageFeatureDao.getImageFeature(dslContext, imageCode)
        val publicFlag = imageFeature.publicFlag ?: false
        // 默认第三方
        val rdType = ImageRDTypeEnum.getImageRDType(imageFeature.imageType?.toInt() ?: 1)
        val sourceType = ImageType.getType(imageRecord.get(KEY_IMAGE_SOURCE_TYPE) as String)
        val repoUrl = imageRecord.get(KEY_IMAGE_REPO_URL) as String? ?: ""
        val repoName = imageRecord.get(KEY_IMAGE_REPO_NAME) as String? ?: ""
        val tag = imageRecord.get(KEY_IMAGE_TAG) as String? ?: ""
        val ticketId = imageRecord.get(Constants.KEY_IMAGE_TICKET_ID) as String? ?: ""
        val ticketProject = imageRecord.get(Constants.KEY_IMAGE_INIT_PROJECT) as String? ?: ""
        val cleanImageRepoUrl = repoUrl.trimEnd { ch ->
            ch == '/'
        }
        val cleanImageRepoName = repoName.trimStart { ch ->
            ch == '/'
        }
        val cleanTag = if (!tag.isBlank()) tag else {
            "latest"
        }
        if (sourceType != ImageType.BKDEVOPS && sourceType != ImageType.THIRD) throw UnknownImageSourceType(
            "imageId=$id,imageSourceType=${sourceType.name}",
            StoreMessageCode.USER_IMAGE_UNKNOWN_SOURCE_TYPE
        )
        else {
            return ImageRepoInfo(
                sourceType = sourceType,
                repoUrl = cleanImageRepoUrl,
                repoName = cleanImageRepoName,
                repoTag = cleanTag,
                ticketId = ticketId,
                ticketProject = ticketProject,
                publicFlag = publicFlag,
                rdType = rdType
            )
        }
    }

    fun getImageDetailByCodeAndVersion(
        userId: String,
        imageCode: String,
        imageVersion: String?,
        interfaceName: String? = "Anon interface"
    ): ImageDetail {
        logger.info("$interfaceName:getLatestImageDetailByCode:Input:($userId,$imageCode,$imageVersion)")
        return if (null == imageVersion) {
            // 不传version默认返回最新版本
            getLatestImageDetailByCode(userId, imageCode, interfaceName)
        } else {
            val imageRecord =
                imageDao.getImageByCodeAndVersion(dslContext, imageCode, imageVersion) ?: throw ErrorCodeException(
                    errorCode = USER_IMAGE_VERSION_NOT_EXIST,
                    defaultMessage = "image is null,imageCode=$imageCode, imageVersion=$imageVersion",
                    params = arrayOf(imageCode, imageVersion)
                )
            getImageDetail(userId, imageRecord)
        }
    }

    fun getImageStatusByCodeAndVersion(
        imageCode: String,
        imageVersion: String
    ): String {
        logger.info("getImageStatusByCodeAndVersion:Input:($imageCode,$imageVersion)")
        val imageRecord =
            imageDao.getImage(dslContext, imageCode, imageVersion) ?: throw ErrorCodeException(
                errorCode = USER_IMAGE_VERSION_NOT_EXIST,
                defaultMessage = "image is null,imageCode=$imageCode, imageVersion=$imageVersion",
                params = arrayOf(imageCode, imageVersion)
            )
        return ImageStatusEnum.getImageStatus(imageRecord.imageStatus.toInt())
    }

    fun getLatestImageDetailByCode(
        userId: String,
        imageCode: String,
        interfaceName: String? = "Anon interface"
    ): ImageDetail {
        logger.info("$interfaceName:getLatestImageDetailByCode:Input:($userId,$imageCode)")
        val imageRecord =
            imageDao.getLatestImageByCode(dslContext, imageCode) ?: throw InvalidParamException(
                message = "image is null,imageCode=$imageCode",
                params = arrayOf(imageCode)
            )
        return getImageDetail(userId, imageRecord)
    }

    /**
     * 镜像大小信息格式化
     */
    private fun getImageSizeInfoByStr(imageSizeStr: String): Pair<Long, String> {
        var imageSizeNum = 0L
        try {
            imageSizeNum = if ("" == imageSizeStr.trim()) {
                0L
            } else {
                imageSizeStr.toLong()
            }
        } catch (ignored: Throwable) {
            logger.warn("imageSizeStr=$imageSizeStr", ignored)
        }
        val imageSize = if (0L == imageSizeNum) {
            "-"
        } else {
            String.format("%.2f", imageSizeNum / 1024.0 / 1024.0) + " MB"
        }
        return Pair(imageSizeNum, imageSize)
    }

    private fun getImageDetail(userId: String, imageRecord: TImageRecord): ImageDetail {
        val imageId = imageRecord.id
        val imageCode = imageRecord.imageCode
        val storeStatistic = storeTotalStatisticService.getStatisticByCode(
            userId = userId,
            storeCode = imageCode,
            storeType = StoreTypeEnum.IMAGE.type.toByte()
        )
        val storeHonorInfos = storeHonorService.getStoreHonor(userId, StoreTypeEnum.IMAGE, imageCode)
        val storeIndexInfos = storeIndexManageService.getStoreIndexInfosByStoreCode(StoreTypeEnum.IMAGE, imageCode)
        val classifyRecord = classifyService.getClassify(imageRecord.classifyId).data
        val imageFeatureRecord = imageFeatureDao.getImageFeature(dslContext, imageRecord.imageCode)
        val imageVersionLog = imageVersionLogDao.getLatestImageVersionLogByImageId(dslContext, imageId)?.get(0)
        val publicFlag = imageFeatureRecord.publicFlag
        // 生成icon
        val icon = imageRecord.icon
        // 判断installFlag
        val installFlag =
            storeUserService.isCanInstallStoreComponent(
                defaultFlag = publicFlag,
                userId = userId,
                storeCode = imageCode,
                storeType = StoreTypeEnum.IMAGE
            ) // 是否能安装
        // 判断releaseFlag
        var releaseFlag = false
        val count = marketImageDao.countReleaseImageByCode(dslContext, imageCode)
        if (count > 0) {
            releaseFlag = true
        }
        // 查LabelList
        val labelList = imageLabelService.getLabelsByImageId(imageId).data
        labelList?.sortedBy { it.labelName }
        // 查CategoryList
        val categoryList = imageCategoryService.getCategorysByImageId(imageId).data
        // 查UserCommentInfo
        val userCommentInfo = storeCommentService.getStoreUserCommentInfo(
            userId = userId,
            storeCode = imageCode,
            storeType = StoreTypeEnum.IMAGE
        )
        // 查关联镜像时的调试项目
        val projectCode = storeProjectRelDao.getUserStoreTestProjectCode(dslContext = dslContext,
            userId = userId,
            storeCode = imageCode,
            storeType = StoreTypeEnum.IMAGE)
        val (imageSizeNum, imageSize) = getImageSizeInfoByStr(imageRecord.imageSize as String)
        val agentTypeScope = if (ImageStatusEnum.getInprocessStatusSet().contains(imageRecord.imageStatus.toInt())) {
            // 非终止态镜像应采用当前版本范畴与适用机器类型
            JsonUtil.to(imageRecord.agentTypeScope!!, object : TypeReference<List<ImageAgentTypeEnum>>() {})
        } else {
            // 终止态镜像采用最终适用机器类型
            imageAgentTypeDao.getAgentTypeByImageCode(dslContext, imageCode)?.map {
                ImageAgentTypeEnum.getImageAgentType(it.get(Constants.KEY_IMAGE_AGENT_TYPE) as String)!!
            } ?: emptyList()
        }
        val category = if (null != categoryList && categoryList.isNotEmpty()) {
            categoryList[0]
        } else {
            null
        }
        // 组装返回
        return ImageDetail(
            imageId = imageId,
            id = imageId,
            imageCode = imageCode,
            code = imageCode,
            imageName = imageRecord.imageName,
            name = imageRecord.imageName,
            logoUrl = imageRecord.logoUrl ?: "",
            icon = icon ?: "",
            summary = imageRecord.summary ?: "",
            docsLink = storeCommonService.getStoreDetailUrl(StoreTypeEnum.IMAGE, imageCode),
            projectCode = projectCode ?: "",
            score = storeStatistic.score ?: 0.0,
            downloads = storeStatistic.downloads,
            classifyId = classifyRecord?.id ?: "",
            classifyCode = classifyRecord?.classifyCode ?: "",
            classifyName = classifyRecord?.classifyName ?: "",
            imageSourceType = ImageType.getType(imageRecord.imageSourceType).name,
            imageRepoUrl = imageRecord.imageRepoUrl ?: "",
            imageRepoName = imageRecord.imageRepoName ?: "",
            rdType = ImageRDTypeEnum.getImageRDTypeStr(imageFeatureRecord.imageType?.toInt()),
            weight = imageFeatureRecord.weight,
            agentTypeScope = agentTypeScope,
            ticketId = imageRecord.ticketId ?: "",
            imageTag = imageRecord.imageTag ?: "",
            imageSize = imageSize,
            imageSizeNum = imageSizeNum,
            imageStatus = ImageStatusEnum.getImageStatus(imageRecord.imageStatus.toInt()),
            description = imageRecord.description ?: "",
            dockerFileType = imageRecord.dockerFileType ?: "INPUT",
            dockerFileContent = imageRecord.dockerFileContent ?: "",
            labelList = labelList ?: listOf(),
            category = category?.categoryCode ?: "",
            categoryName = category?.categoryName ?: "",
            latestFlag = imageRecord.latestFlag,
            publisher = imageRecord.publisher ?: "",
            pubTime = imageRecord.pubTime?.timestampmilli(),
            publicFlag = imageFeatureRecord.publicFlag,
            flag = installFlag,
            releaseFlag = releaseFlag,
            recommendFlag = imageFeatureRecord.recommendFlag,
            certificationFlag = imageFeatureRecord.certificationFlag,
            userCommentInfo = userCommentInfo,
            version = imageRecord.version ?: "",
            releaseType = ReleaseTypeEnum.getReleaseType(imageVersionLog?.releaseType?.toInt() ?: 0),
            versionContent = imageVersionLog?.content ?: "",
            editFlag = imageCommonService.checkEditCondition(imageCode),
            creator = imageVersionLog?.creator,
            modifier = imageVersionLog?.modifier,
            createTime = (imageVersionLog?.createTime ?: imageRecord.createTime).timestampmilli(),
            updateTime = (imageVersionLog?.updateTime ?: imageRecord.updateTime).timestampmilli(),
            honorInfos = storeHonorInfos,
            indexInfos = storeIndexInfos
        )
    }

    fun deleteById(
        userId: String,
        imageId: String,
        interfaceName: String? = "Anon interface"
    ): Result<Boolean> {
        val imageRecord = imageDao.getImage(dslContext, imageId) ?: throw ImageNotExistException("imageId=$imageId")
        return delete(userId, imageRecord.imageCode, interfaceName)
    }

    fun delete(
        userId: String,
        imageCode: String,
        interfaceName: String? = "Anon interface"
    ): Result<Boolean> {
        logger.info("$interfaceName:delete:Input:($userId,$imageCode)")
        val type = StoreTypeEnum.IMAGE.type.toByte()
        val isOwner = storeMemberDao.isStoreAdmin(dslContext, userId, imageCode, type)
        if (!isOwner) {
            return I18nUtil.generateResponseDataObject(
                messageCode = NO_COMPONENT_ADMIN_PERMISSION,
                params = arrayOf(imageCode),
                language = I18nUtil.getLanguage(userId)
            )
        }

        val releasedCnt = marketImageDao.countReleaseImageByCode(dslContext, imageCode)
        if (releasedCnt > 0) {
            return I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_IMAGE_RELEASED,
                params = arrayOf(imageCode),
                language = I18nUtil.getLanguage(userId)
            )
        }
        logger.info("$interfaceName:delete:Inner:releasedCnt=$releasedCnt")

        // 如果已经被安装到其他项目下使用，不能删除关联
        val installedCnt = storeProjectRelDao.countInstalledProject(dslContext, imageCode, type)
        logger.info("$interfaceName:delete:Inner:installedCnt=$installedCnt")
        if (installedCnt > 0) {
            return I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_IMAGE_USED,
                params = arrayOf(imageCode),
                language = I18nUtil.getLanguage(userId)
            )
        }
        deleteImage(userId, imageCode)
        return Result(true)
    }

    fun deleteImage(
        userId: String,
        imageCode: String
    ) {
        dslContext.transaction { t ->
            val context = DSL.using(t)
            val imageIds = marketImageDao.getImagesIdByImageCode(context, imageCode)
            storeCommonService.deleteStoreInfo(context, imageCode, StoreTypeEnum.IMAGE.type.toByte())
            // 删除镜像代理类型数据
            imageAgentTypeDao.deleteAgentTypeByImageCode(context, imageCode)
            // 删除镜像特性信息
            marketImageFeatureDao.daleteImageFeature(context, imageCode)
            if (!imageIds.isNullOrEmpty()) {
                // 删除镜像与范畴关联关系
                imageCategoryRelDao.batchDeleteByImageId(context, imageIds)
                // 删除镜像与标签关联关系
                imageLabelRelDao.deleteByImageIds(context, imageIds)
                // 删除镜像版本日志
                imageVersionLogDao.deleteByImageIds(context, imageIds)
                imageDao.deleteByImageIds(context, imageIds)
            }
        }
    }

    fun saveImageCategoryByIds(
        context: DSLContext,
        userId: String,
        imageId: String,
        categoryIdList: List<String>
    ) {
        if (categoryIdList.isNotEmpty()) {
            categoryIdList.forEach {
                if (categoryDao.countById(context, it.trim(), StoreTypeEnum.IMAGE.type.toByte()) == 0) {
                    throw CategoryNotExistException(
                        message = "category does not exist, categoryId:$it",
                        params = arrayOf(it)
                    )
                }
            }
            imageCategoryRelDao.deleteByImageId(context, imageId)
            imageCategoryRelDao.batchAdd(context, userId, imageId, categoryIdList)
        }
    }

    fun saveImageCategoryByCode(
        context: DSLContext,
        userId: String,
        imageId: String,
        categoryCode: String?
    ) {
        if (!categoryCode.isNullOrBlank()) {
            if (categoryDao.countByCode(context, categoryCode, StoreTypeEnum.IMAGE.type.toByte()) == 0) {
                throw CategoryNotExistException(
                    message = "category does not exist, categoryCode:$categoryCode",
                    params = arrayOf(categoryCode)
                )
            }
            imageCategoryRelDao.deleteByImageId(context, imageId)
            val categoryId = categoryDao.getCategoryByCodeAndType(
                dslContext = context,
                categoryCode = categoryCode,
                type = StoreTypeEnum.IMAGE.type.toByte()
            )!!.id
            imageCategoryRelDao.batchAdd(context, userId, imageId, listOf(categoryId))
        }
    }

    /**
     * 根据项目代码、插件代码和版本号获取插件信息
     */
    @Suppress("UNCHECKED_CAST")
    fun getPipelineImageVersions(projectCode: String, imageCode: String): List<VersionInfo> {
        val imageStatusList = imageCommonService.generateImageStatusList(imageCode, projectCode)
        val versionList = mutableListOf<VersionInfo>()
        val versionRecords =
            imageDao.getVersionsByImageCode(dslContext, projectCode, imageCode, imageStatusList) // 查询插件版本信息
        var tmpVersionPrefix = ""
        versionRecords?.forEach {
            // 通用处理
            val imageVersion = it["version"] as String
            val index = imageVersion.indexOf(".")
            val versionPrefix = imageVersion.substring(0, index + 1)
            var versionName = imageVersion
            var latestVersionName = "${versionPrefix}latest"
            val imageStatus = it["imageStatus"] as Byte
            val imageTag = it["imageTag"] as String
            val imageVersionStatusList = listOf(
                ImageStatusEnum.TESTING.status.toByte(),
                ImageStatusEnum.UNDERCARRIAGING.status.toByte(),
                ImageStatusEnum.UNDERCARRIAGED.status.toByte()
            )
            // 特殊情况单独覆盖处理
            if (imageVersionStatusList.contains(imageStatus)) {
                // 处于测试中、下架中、已下架的插件版本的版本名称加下说明
                val imageStatusName = ImageStatusEnum.getImageStatus(imageStatus.toInt())
                val storeImageStatusPrefix = STORE_IMAGE_STATUS + "_"
                val imageStatusMsg = I18nUtil.getCodeLanMessage(
                    messageCode = "$storeImageStatusPrefix$imageStatusName"
                )
                versionName = "$versionName / $imageStatusMsg"
                latestVersionName = "$latestVersionName / $imageStatusMsg"
            }
            if (tmpVersionPrefix != versionPrefix) {
                versionList.add(VersionInfo(latestVersionName, "$versionPrefix*")) // 添加大版本号的通用最新模式（如1.*）
                tmpVersionPrefix = versionPrefix
            }
            versionList.add(VersionInfo(versionName + "(Tag: $imageTag)", imageVersion)) // 添加具体的版本号
        }
        return versionList
    }

    fun updateImageBaseInfo(
        userId: String,
        imageCode: String,
        imageBaseInfoUpdateRequest: ImageBaseInfoUpdateRequest,
        interfaceName: String? = "Anon interface"
    ): Result<Boolean> {
        logger.info("$interfaceName:updateImageBaseInfo:Input($userId,$imageCode,$imageBaseInfoUpdateRequest")
        // 判断当前用户是否是该镜像的成员
        if (!storeMemberDao.isStoreMember(dslContext, userId, imageCode, StoreTypeEnum.IMAGE.type.toByte())) {
            return I18nUtil.generateResponseDataObject(
                messageCode = GET_INFO_NO_PERMISSION,
                language = I18nUtil.getLanguage(userId)
            )
        }
        // 查询镜像的最新记录
        val newestImageRecord = marketImageDao.getNewestImageByCode(dslContext, imageCode)
            ?: throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf(imageCode))
        val editFlag = imageCommonService.checkEditCondition(imageCode)
        if (!editFlag) {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.USER_IMAGE_VERSION_IS_NOT_FINISH,
                params = arrayOf(newestImageRecord.imageName, newestImageRecord.version)
            )
        }
        val imageIdList = mutableListOf(newestImageRecord.id)
        val latestImageRecord = imageDao.getLatestImageByCode(dslContext, imageCode)
        if (null != latestImageRecord) {
            imageIdList.add(latestImageRecord.id)
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            marketImageDao.updateImageBaseInfo(
                dslContext = context,
                userId = userId,
                imageIdList = imageIdList,
                imageBaseInfoUpdateRequest = imageBaseInfoUpdateRequest
            )
            // 更新标签信息
            val labelIdList = imageBaseInfoUpdateRequest.labelIdList
            if (null != labelIdList) {
                imageIdList.forEach {
                    imageLabelService.updateImageLabels(
                        dslContext = dslContext,
                        userId = userId,
                        imageId = it,
                        labelIdList = labelIdList
                    )
                }
            }
            // 更新范畴信息
            imageIdList.forEach {
                imageCategoryRelDao.updateCategory(
                    dslContext = context,
                    userId = userId,
                    imageId = it,
                    categoryCode = imageBaseInfoUpdateRequest.category
                )
            }
        }
        return Result(true)
    }
}

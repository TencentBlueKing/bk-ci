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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.tencent.devops.store.service.image

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.DataConsistencyException
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.model.store.tables.records.TClassifyRecord
import com.tencent.devops.model.store.tables.records.TImageRecord
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.common.ClassifyDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.dao.common.StoreStatisticDao
import com.tencent.devops.store.dao.image.Constants.KEY_CATEGORY_CODE
import com.tencent.devops.store.dao.image.Constants.KEY_CATEGORY_ICON_URL
import com.tencent.devops.store.dao.image.Constants.KEY_CATEGORY_ID
import com.tencent.devops.store.dao.image.Constants.KEY_CATEGORY_NAME
import com.tencent.devops.store.dao.image.Constants.KEY_CATEGORY_TYPE
import com.tencent.devops.store.dao.image.Constants.KEY_CLASSIFY_ID
import com.tencent.devops.store.dao.image.Constants.KEY_CREATE_TIME
import com.tencent.devops.store.dao.image.Constants.KEY_CREATOR
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_CODE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_FEATURE_PUBLIC_FLAG
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_FEATURE_RECOMMEND_FLAG
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_ID
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_LOGO_URL
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_NAME
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_REPO_NAME
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_REPO_URL
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_SIZE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_SOURCE_TYPE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_STATUS
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_SUMMARY
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_TAG
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_VERSION
import com.tencent.devops.store.dao.image.Constants.KEY_LABEL_CODE
import com.tencent.devops.store.dao.image.Constants.KEY_LABEL_ID
import com.tencent.devops.store.dao.image.Constants.KEY_LABEL_NAME
import com.tencent.devops.store.dao.image.Constants.KEY_LABEL_TYPE
import com.tencent.devops.store.dao.image.Constants.KEY_MODIFIER
import com.tencent.devops.store.dao.image.Constants.KEY_PUBLISHER
import com.tencent.devops.store.dao.image.Constants.KEY_PUB_TIME
import com.tencent.devops.store.dao.image.Constants.KEY_UPDATE_TIME
import com.tencent.devops.store.dao.image.ImageCategoryRelDao
import com.tencent.devops.store.dao.image.ImageDao
import com.tencent.devops.store.dao.image.ImageFeatureDao
import com.tencent.devops.store.dao.image.ImageLabelRelDao
import com.tencent.devops.store.dao.image.ImageVersionLogDao
import com.tencent.devops.store.dao.image.MarketImageDao
import com.tencent.devops.store.pojo.common.STORE_IMAGE_STATUS
import com.tencent.devops.store.pojo.common.VersionInfo
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.image.enums.CategoryTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
import com.tencent.devops.store.pojo.image.enums.LabelTypeEnum
import com.tencent.devops.store.pojo.image.enums.MarketImageSortTypeEnum
import com.tencent.devops.store.pojo.image.request.ImageUpdateRequest
import com.tencent.devops.store.pojo.image.response.Category
import com.tencent.devops.store.pojo.image.response.ImageDetail
import com.tencent.devops.store.pojo.image.response.Label
import com.tencent.devops.store.pojo.image.response.MarketImageItem
import com.tencent.devops.store.pojo.image.response.MarketImageMain
import com.tencent.devops.store.pojo.image.response.MyImage
import com.tencent.devops.store.service.common.ClassifyService
import com.tencent.devops.store.service.common.StoreCommentService
import com.tencent.devops.store.service.common.StoreMemberService
import com.tencent.devops.store.service.common.StoreUserService
import com.tencent.devops.store.service.common.StoreVisibleDeptService
import com.tencent.devops.store.exception.image.ClassifyNotExistException
import com.tencent.devops.store.exception.image.ImageNotExistException
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import kotlin.math.ceil

@Service
class ImageService @Autowired constructor(
    private val dslContext: DSLContext,
    private val imageDao: ImageDao,
    private val imageCategoryRelDao: ImageCategoryRelDao,
    private val storeStatisticDao: StoreStatisticDao,
    private val classifyDao: ClassifyDao,
    private val imageFeatureDao: ImageFeatureDao,
    private val storeMemberDao: StoreMemberDao,
    private val imageVersionLogDao: ImageVersionLogDao,
    private val marketImageDao: MarketImageDao,
    private val imageLabelRelDao: ImageLabelRelDao,
    private val storeVisibleDeptService: StoreVisibleDeptService,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val imageCommonService: ImageCommonService,
    private val storeCommentService: StoreCommentService,
    private val storeUserService: StoreUserService,
    @Qualifier("imageMemberService")
    private val storeMemberService: StoreMemberService,
    private val classifyService: ClassifyService,
    private val marketImageStatisticService: MarketImageStatisticService,
    private val client: Client
) {
    @Value("\${store.baseImageDocsLink}")
    private lateinit var baseImageDocsLink: String
    private val logger = LoggerFactory.getLogger(ImageService::class.java)

    fun getImageVersionListByCode(
        userId: String,
        imageCode: String,
        page: Int?,
        pageSize: Int?,
        interfaceName: String? = "Anon interface"
    ): Result<Page<ImageDetail>> {
        logger.info("$interfaceName:getImageVersionListByCode:Input:($userId,$imageCode,$page,$pageSize)")
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
        logger.info("$interfaceName:getImageVersionListByCode:Output:Page($count,$validPage,$validPageSize,imageVersionList.size=${imageVersionList.size})")
        return Result(pageObj)
    }

    @Suppress("UNCHECKED_CAST")
    fun doList(
        userId: String,
        userDeptList: List<Int>,
        imageName: String?,
        classifyCodeList: List<String>?,
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
        val labelCodeList = if (labelCode.isNullOrEmpty()) listOf() else labelCode?.split(",")
        val images = marketImageDao.list(
            dslContext = dslContext,
            imageName = imageName,
            classifyCode = classifyCodeList,
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
        val imageVisibleData = storeVisibleDeptService.batchGetVisibleDept(imageCodeList, StoreTypeEnum.IMAGE).data
        val imageVisibleDataStr = StringBuilder("\n")
        imageVisibleData?.forEach {
            imageVisibleDataStr.append("${it.key}->${it.value}\n")
        }
        logger.info("$interfaceName:doList:Inner:imageVisibleData=$imageVisibleDataStr")

        // 获取热度
        val statField = mutableListOf<String>()
        statField.add("DOWNLOAD")
        val imageStatisticData = marketImageStatisticService.getStatisticByCodeList(imageCodeList).data

        // 获取用户
        val memberData = storeMemberService.batchListMember(imageCodeList, StoreTypeEnum.IMAGE).data

        // 获取分类
        val classifyList = classifyService.getAllClassify(StoreTypeEnum.IMAGE.type.toByte()).data
        val classifyMap = mutableMapOf<String, String>()
        classifyList?.forEach {
            classifyMap[it.id] = it.classifyCode
        }

        images.forEach {
            val imageCode = it[KEY_IMAGE_CODE] as String
            val visibleList = imageVisibleData?.get(imageCode)
            val statistic = imageStatisticData?.get(imageCode)
            val members = memberData?.get(imageCode)

            val installFlag =
                if (it[KEY_IMAGE_FEATURE_PUBLIC_FLAG] as Boolean || (members != null && members.contains(userId))) {
                    true
                } else {
                    visibleList != null && (visibleList.contains(0) || visibleList.intersect(userDeptList).count() > 0)
                }
            val classifyId = it[KEY_CLASSIFY_ID] as String
            val (imageSizeNum, imageSize) = getImageSizeInfoByStr(it.get(KEY_IMAGE_SIZE) as String)
            results.add(
                MarketImageItem(
                    id = it[KEY_IMAGE_ID] as String,
                    code = imageCode,
                    name = it[KEY_IMAGE_NAME] as String,
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
                    isInstalled = null
                )
            )
        }
        return results
    }

    /**
     * 镜像市场搜索镜像
     */
    fun searchImage(
        userId: String,
        imageName: String?,
        imageSourceType: ImageType?,
        classifyCode: String?,
        labelCode: String?,
        score: Int?,
        sortType: String?,
        page: Int?,
        pageSize: Int?,
        interfaceName: String? = "Anon interface"
    ): Result<List<MarketImageMain>> {
        logger.info("$interfaceName:searchImage:Input:($userId,$imageName,$imageSourceType,$classifyCode,$labelCode,$score,$sortType,$page,$pageSize)")
        val result = mutableListOf<MarketImageMain>()
        // 获取用户组织架构
        val userDeptList = storeUserService.getUserDeptList(userId)
        logger.info("$interfaceName:searchImage:Inner:userDeptList=$userDeptList")
        val classifyList: List<TClassifyRecord>
        classifyList = if (classifyCode == null) {
            classifyDao.getAllClassify(dslContext, StoreTypeEnum.IMAGE.type.toByte()).toList()
        } else {
            val record = classifyDao.getClassifyByCode(dslContext, classifyCode, StoreTypeEnum.IMAGE)
                ?: throw ClassifyNotExistException("classifyCode:$classifyCode")
            listOf(record)
        }
        classifyList.forEach {
            val code = it.classifyCode
            if (code != "trigger") {
                result.add(
                    MarketImageMain(
                        key = code,
                        label = MessageCodeUtil.getMessageByLocale(it.classifyName, it.classifyCode),
                        records = doList(
                            userId = userId,
                            userDeptList = userDeptList,
                            imageName = imageName,
                            classifyCodeList = listOf(code),
                            labelCode = labelCode,
                            score = score,
                            imageSourceType = imageSourceType,
                            sortType = MarketImageSortTypeEnum.NAME,
                            desc = false,
                            page = page,
                            pageSize = pageSize,
                            interfaceName = interfaceName
                        )
                    )
                )
            }
        }
        logger.info("$interfaceName:searchImage:Output:result.size=${result.size}")
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
                key = "latest",
                label = "最新",
                records = doList(
                    userId = userId,
                    userDeptList = userDeptList,
                    imageName = null,
                    classifyCodeList = null,
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
                key = "hottest",
                label = "最热",
                records = doList(
                    userId = userId,
                    userDeptList = userDeptList,
                    imageName = null,
                    classifyCodeList = null,
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
                result.add(
                    MarketImageMain(
                        key = classifyCode,
                        label = it.classifyName,
                        records = doList(
                            userId = userId,
                            userDeptList = userDeptList,
                            imageName = null,
                            classifyCodeList = listOf(classifyCode),
                            labelCode = null,
                            score = null,
                            imageSourceType = null,
                            sortType = MarketImageSortTypeEnum.NAME,
                            desc = false,
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
        myImageRecords.forEach {
            val imageCode = it.get(KEY_IMAGE_CODE) as String
            val projectCode = storeProjectRelDao.getInitProjectCodeByStoreCode(
                dslContext = dslContext,
                storeCode = imageCode,
                storeType = StoreTypeEnum.IMAGE.type.toByte()
            )
                ?: throw DataConsistencyException(
                    "storeCode=$imageCode,storeType=${StoreTypeEnum.IMAGE.name}",
                    "T_STORE_PROJECT_REL.projectCode",
                    "Data does not exist"
                )
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
        for (i in 0 until myImageRecords.size) {
            val it = myImageRecords[i]
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
                    updateTime = (it.get(KEY_UPDATE_TIME) as LocalDateTime).timestampmilli()
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
        logger.info("$interfaceName:getMyImageList:Output:Page($validPage:$validPageSize:$count:myImageList.size=${myImageList.size})")
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

    fun getImageDetailByCodeAndVersion(
        userId: String,
        projectCode: String,
        imageCode: String,
        imageVersion: String?,
        interfaceName: String? = "Anon interface"
    ): ImageDetail {
        logger.info("$interfaceName:getImageDetailByCodeAndVersion:Input:($userId,$projectCode,$imageCode,$imageVersion)")
        // 区分是否为调试项目
        val imageStatusList = imageCommonService.generateImageStatusList(imageCode, projectCode)
        val imageRecord =
            imageDao.getLatestImageByBaseVersion(
                dslContext = dslContext,
                imageCode = imageCode,
                imageStatusSet = imageStatusList.toSet(),
                baseVersion = imageVersion?.replace("*", "")
            )
                ?: throw InvalidParamException(
                    message = "image is null,projectCode=$projectCode,imageCode=$imageCode,imageVersion=$imageVersion",
                    params = arrayOf(imageCode, imageVersion ?: "")
                )
        return getImageDetail(userId, imageRecord)
    }

    fun getImageDetailByCode(
        userId: String,
        imageCode: String,
        interfaceName: String? = "Anon interface"
    ): ImageDetail {
        logger.info("$interfaceName:getImageDetailByCode:Input:($userId,$imageCode)")
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
    private fun getImageSizeInfoByStr(imageSizeStr: String): Pair<Int, String> {
        var imageSizeNum = 0
        try {
            if ("" == imageSizeStr.trim()) {
                imageSizeNum = 0
            } else {
                imageSizeNum = imageSizeStr.toInt()
            }
        } catch (e: NumberFormatException) {
            logger.warn("imageSizeStr=$imageSizeStr", e)
        }
        val imageSize = if (0 == imageSizeNum) {
            "-"
        } else {
            String.format("%.2f", imageSizeNum / 1024.0 / 1024.0) + "MB"
        }
        return Pair(imageSizeNum, imageSize)
    }

    private fun getImageDetail(userId: String, imageRecord: TImageRecord): ImageDetail {
        val imageId = imageRecord.id
        val storeStatisticRecord =
            storeStatisticDao.getStatisticByStoreId(
                dslContext = dslContext,
                storeId = imageId,
                storeType = StoreTypeEnum.IMAGE.type.toByte()
            )
        val classifyRecord = classifyDao.getClassify(dslContext, imageRecord.classifyId)
        val imageFeatureRecord = imageFeatureDao.getImageFeature(dslContext, imageRecord.imageCode)
            ?: throw InvalidParamException("imageFeature is null,imageCode=${imageRecord.imageCode}")
        val imageVersionLog = imageVersionLogDao.getLatestImageVersionLogByImageId(dslContext, imageId)?.get(0)
        val imageCode = imageRecord.imageCode
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
        val labelList = ArrayList<Label>()
        val records = imageLabelRelDao.getLabelsByImageId(dslContext, imageId)
        records?.forEach {
            labelList.add(
                Label(
                    id = it[KEY_LABEL_ID] as String,
                    labelCode = it[KEY_LABEL_CODE] as String,
                    labelName = it[KEY_LABEL_NAME] as String,
                    labelType = LabelTypeEnum.getLabelType((it[KEY_LABEL_TYPE] as Byte).toInt()),
                    createTime = (it[KEY_CREATE_TIME] as LocalDateTime).timestampmilli(),
                    updateTime = (it[KEY_UPDATE_TIME] as LocalDateTime).timestampmilli()
                )
            )
        }
        // 查CategoryList
        val categoryList = ArrayList<Category>()
        val categoryRecords = imageCategoryRelDao.getCategorysByImageId(dslContext, imageId)
        categoryRecords?.forEach {
            categoryList.add(
                Category(
                    id = it[KEY_CATEGORY_ID] as String,
                    categoryCode = it[KEY_CATEGORY_CODE] as String,
                    categoryName = it[KEY_CATEGORY_NAME] as String,
                    categoryType = CategoryTypeEnum.getCategoryType((it[KEY_CATEGORY_TYPE] as Byte).toInt()),
                    iconUrl = (it[KEY_CATEGORY_ICON_URL] as String?) ?: "",
                    createTime = (it[KEY_CREATE_TIME] as LocalDateTime).timestampmilli(),
                    updateTime = (it[KEY_UPDATE_TIME] as LocalDateTime).timestampmilli()
                )
            )
        }
        // 查UserCommentInfo
        val userCommentInfo = storeCommentService.getStoreUserCommentInfo(
            userId = userId,
            storeCode = imageCode,
            storeType = StoreTypeEnum.IMAGE
        )
        // 查关联镜像时的调试项目
        val projectCode =
            storeProjectRelDao.getInitProjectCodeByStoreCode(dslContext, imageCode, StoreTypeEnum.IMAGE.type.toByte())
                ?: throw DataConsistencyException(
                    "imageCode:$imageCode",
                    "projectCode of Table StoreProjectRel",
                    "No initial projectCode"
                )
        val (imageSizeNum, imageSize) = getImageSizeInfoByStr(imageRecord.imageSize as String)
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
            docsLink = baseImageDocsLink + imageCode,
            projectCode = projectCode,
            score = storeStatisticRecord?.value3()?.toDouble() ?: 0.0,
            downloads = storeStatisticRecord?.value1()?.toInt() ?: 0,
            classifyCode = classifyRecord?.classifyCode ?: "",
            classifyName = classifyRecord?.classifyName ?: "",
            imageSourceType = ImageType.getType(imageRecord.imageSourceType).name,
            imageRepoUrl = imageRecord.imageRepoUrl ?: "",
            imageRepoName = imageRecord.imageRepoName ?: "",
            imageTag = imageRecord.imageTag ?: "",
            imageSize = imageSize,
            imageSizeNum = imageSizeNum,
            imageStatus = ImageStatusEnum.getImageStatus(imageRecord.imageStatus.toInt()),
            description = imageRecord.description ?: "",
            labelList = labelList,
            categoryList = categoryList,
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
            creator = imageVersionLog?.creator,
            modifier = imageVersionLog?.modifier,
            createTime = imageVersionLog?.createTime?.timestampmilli(),
            updateTime = imageVersionLog?.updateTime?.timestampmilli()
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
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED, arrayOf(imageCode))
        }

        val releasedCnt = marketImageDao.countReleaseImageByCode(dslContext, imageCode)
        if (releasedCnt > 0) {
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_IMAGE_RELEASED, arrayOf(imageCode))
        }
        logger.info("$interfaceName:delete:Inner:releasedCnt=$releasedCnt")

        // 如果已经被安装到其他项目下使用，不能删除关联
        val installedCnt = storeProjectRelDao.countInstalledProject(dslContext, imageCode, type)
        logger.info("$interfaceName:delete:Inner:installedCnt=$installedCnt")
        if (installedCnt > 0) {
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_IMAGE_USED, arrayOf(imageCode))
        }

        dslContext.transaction { t ->
            val context = DSL.using(t)
            storeMemberDao.deleteAll(context, imageCode, type)
            storeProjectRelDao.deleteAllRel(context, imageCode, type)
            imageFeatureDao.deleteAll(context, imageCode)
            marketImageDao.delete(context, imageCode)
        }
        return Result(true)
    }

    fun update(
        userId: String,
        imageId: String,
        imageUpdateRequest: ImageUpdateRequest,
        interfaceName: String? = "Anon interface"
    ): Result<Boolean> {
        val imageRecord = imageDao.getImage(dslContext, imageId) ?: throw ImageNotExistException("imageId=$imageId")
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            imageDao.updateImage(
                dslContext = context,
                imageId = imageId,
                imageUpdateBean = ImageDao.ImageUpdateBean(
                    imageName = imageUpdateRequest.imageName,
                    classifyId = imageUpdateRequest.classifyId,
                    version = imageUpdateRequest.version,
                    imageSourceType = imageUpdateRequest.imageSourceType,
                    imageRepoUrl = imageUpdateRequest.imageRepoUrl,
                    imageRepoName = imageUpdateRequest.imageRepoName,
                    imageRepoPath = imageUpdateRequest.imageRepoPath,
                    ticketId = imageUpdateRequest.ticketId,
                    // 镜像状态及对应信息只在审核接口中变动
                    imageStatus = null,
                    imageStatusMsg = null,
                    imageSize = imageUpdateRequest.imageSize,
                    imageTag = imageUpdateRequest.imageTag,
                    logoUrl = imageUpdateRequest.logoUrl,
                    icon = imageUpdateRequest.icon,
                    summary = imageUpdateRequest.summary,
                    description = imageUpdateRequest.description,
                    publisher = imageUpdateRequest.publisher,
                    // 是否为最新版本镜像只走发布和下架逻辑更新
                    latestFlag = null,
                    modifier = userId
                )
            )
            imageFeatureDao.update(
                dslContext = context,
                imageCode = imageRecord.imageCode,
                publicFlag = imageUpdateRequest.publicFlag,
                recommendFlag = imageUpdateRequest.recommendFlag,
                certificationFlag = imageUpdateRequest.certificationFlag,
                weight = imageUpdateRequest.weight,
                modifier = userId
            )
        }
        return Result(true)
    }

    /**
     * 根据项目代码、插件代码和版本号获取插件信息
     */
    @Suppress("UNCHECKED_CAST")
    fun getPipelineImageVersions(projectCode: String, imageCode: String): List<VersionInfo> {
        logger.info("the projectCode is: $projectCode,imageCode is: $imageCode")
        val imageStatusList = imageCommonService.generateImageStatusList(imageCode, projectCode)
        val versionList = mutableListOf<VersionInfo>()
        val versionRecords =
            imageDao.getVersionsByImageCode(dslContext, projectCode, imageCode, imageStatusList) // 查询插件版本信息
        var tmpVersionPrefix = ""
        versionRecords?.forEach {
            // 通用处理
            val imageVersion = it["version"] as String
            val imageTag = it["imageTag"] as String
            val index = imageVersion.indexOf(".")
            val versionPrefix = imageVersion.substring(0, index + 1)
            var versionName = "$imageVersion tag=$imageTag"
            var latestVersionName = "${versionPrefix}latest tag=$imageTag"
            val imageStatus = it["imageStatus"] as Byte
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
                val imageStatusMsg = MessageCodeUtil.getCodeLanMessage("$storeImageStatusPrefix$imageStatusName")
                versionName = "$versionName ($imageStatusMsg)"
                latestVersionName = "$latestVersionName ($imageStatusMsg)"
            }
            if (tmpVersionPrefix != versionPrefix) {
                versionList.add(VersionInfo(latestVersionName, "$versionPrefix*")) // 添加大版本号的通用最新模式（如1.*）
                tmpVersionPrefix = versionPrefix
            }
            versionList.add(VersionInfo(versionName, imageVersion)) // 添加具体的版本号
        }
        logger.info("the imageCode is: $imageCode,versionList is: $versionList")
        return versionList
    }
}

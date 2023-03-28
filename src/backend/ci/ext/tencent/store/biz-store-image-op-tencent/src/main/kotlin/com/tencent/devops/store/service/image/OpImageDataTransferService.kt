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

import com.tencent.devops.common.api.constant.I18NConstant.BK_AFTER_IMAGE_STORE_ONLINE
import com.tencent.devops.common.api.constant.I18NConstant.BK_AUTOMATICALLY_CONVERTED
import com.tencent.devops.common.api.constant.I18NConstant.BK_COPY_FOR_BUILD_IMAGE
import com.tencent.devops.common.api.constant.I18NConstant.BK_IMAGE_STORE_ONLINE
import com.tencent.devops.common.api.constant.I18NConstant.BK_OLD_VERSION_BUILD_IMAGE
import com.tencent.devops.common.api.constant.I18NConstant.BK_OTHER
import com.tencent.devops.common.api.constant.I18NConstant.BK_PIPELINED_JOB
import com.tencent.devops.common.api.constant.I18NConstant.BK_PROJECT_MANAGER_CAN_OPERATION
import com.tencent.devops.common.api.exception.DataConsistencyException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.image.api.ServiceImageResource
import com.tencent.devops.image.pojo.DockerTag
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import com.tencent.devops.store.dao.OpImageDao
import com.tencent.devops.store.dao.common.CategoryDao
import com.tencent.devops.store.dao.common.ClassifyDao
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_CODE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_ID
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_STATUS
import com.tencent.devops.store.dao.image.ImageDao
import com.tencent.devops.store.dao.image.ImageFeatureDao
import com.tencent.devops.store.pojo.common.ClassifyRequest
import com.tencent.devops.store.pojo.common.StoreMemberReq
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreMemberTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageAgentTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
import com.tencent.devops.store.pojo.image.request.ImageCreateRequest
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class OpImageDataTransferService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val imageDao: ImageDao,
    private val imageFeatureDao: ImageFeatureDao,
    private val opImageDao: OpImageDao,
    private val classifyDao: ClassifyDao,
    private val categoryDao: CategoryDao,
    private val opImageService: OpImageService,
    private val imageMemberService: ImageMemberService,
    private val imageReleaseService: ImageReleaseService
) {
    companion object {
        const val CLASSIFYCODE_OTHER = "OTHER"
        const val CATEGORY_PIPELINE_JOB = "PIPELINE_JOB"
    }

    private val logger = LoggerFactory.getLogger(OpImageDataTransferService::class.java)

    @Value("\${store.imageAdminUsers}")
    private val imageAdminUsersStr: String? = null

    private val finishedSet: HashSet<Triple<String, String, String>> = HashSet()

    private lateinit var imageAdminUsers: Set<String>

    /**
     * 生成数据迁移专有镜像分类
     */
    fun createSystemInitClassify(classifyCode: String, classifyName: String) {
        if (null == classifyDao.getClassifyByCode(
                dslContext,
                classifyCode,
                StoreTypeEnum.IMAGE
            )
        ) {
            try {
                classifyDao.add(
                    dslContext = dslContext,
                    id = UUIDUtil.generate(),
                    classifyRequest = ClassifyRequest(
                        classifyCode = classifyCode,
                        classifyName = classifyName,
                        weight = null
                    ),
                    type = StoreTypeEnum.IMAGE.type.toByte()
                )
            } catch (e: DuplicateKeyException) {
                // 并发创建可能引起的主键冲突直接忽略
            }
        }
    }

    /**
     * 生成数据迁移专有镜像范畴
     */
    fun createSystemInitCategory(categoryCode: String, categoryName: String) {
        if (null == categoryDao.getCategoryByCodeAndType(
                dslContext,
                categoryCode,
                StoreTypeEnum.IMAGE.type.toByte()
            )
        ) {
            try {
                categoryDao.add(
                    dslContext = dslContext,
                    id = UUIDUtil.generate(),
                    categoryCode = categoryCode,
                    categoryName = categoryName,
                    iconUrl = "http://radosgw.open.oa.com/paas_backend/ieod/prod/file/png/random_15649905585375037820270514184859.png?v=1564990558",
                    type = StoreTypeEnum.IMAGE.type.toByte()
                )
            } catch (e: DuplicateKeyException) {
                // 并发创建可能引起的主键冲突直接忽略
            }
        }
    }

    @PostConstruct
    fun init() {
        imageAdminUsers = imageAdminUsersStr!!.trim().split(";").toSet()
        logger.info("imageAdminUsers= $imageAdminUsers")
    }

    fun initClassifyAndCategory(
        userId: String,
        classifyCode: String?,
        classifyName: String?,
        categoryCode: String?,
        categoryName: String?,
        interfaceName: String? = "Anon interface"
    ): Int {
        logger.info("$interfaceName:initClassifyAndCategory:Input($classifyCode,$classifyName,$categoryCode,$categoryName)")
        createSystemInitClassify(classifyCode ?: CLASSIFYCODE_OTHER, classifyName ?:
        MessageUtil.getMessageByLocale(
            messageCode = BK_OTHER,
            language = I18nUtil.getLanguage(userId)
        ))
        logger.info("$interfaceName:initClassifyAndCategory:Inner:createSystemInitClassify end,begin to createSystemInitCategory")
        createSystemInitCategory(categoryCode ?: CATEGORY_PIPELINE_JOB, categoryName ?:
        MessageUtil.getMessageByLocale(
            messageCode = BK_PIPELINED_JOB,
            language = I18nUtil.getLanguage(userId)
        ))
        logger.info("$interfaceName:initClassifyAndCategory:Output:createSystemInitCategory end")
        return 0
    }

    /**
     * 以项目为单位批量重新验证
     */
    fun batchRecheckByProject(
        userId: String,
        projectCode: String,
        interfaceName: String? = "Anon interface"
    ): Int {
        logger.info("$interfaceName:batchRecheckByProject:Input($userId,$projectCode)")
        // 0.鉴权：管理员才有权限操作
        if (!imageAdminUsers.contains(userId.trim())) {
            throw PermissionForbiddenException("Permission Denied,userId=$userId")
        }
        val records = opImageDao.listProjectImages(dslContext, projectCode)
        var count = 0
        records?.forEach { it ->
            val imageId = it.get(KEY_IMAGE_ID) as String
            val imageCode = it.get(KEY_IMAGE_CODE) as String
            val imageStatus = ((it.get(KEY_IMAGE_STATUS) as Byte?)?.toInt() ?: 0)
            logger.info("$interfaceName:batchRecheckByProject:$projectCode:($imageCode,$imageId,$imageStatus)")
            if (imageStatus == ImageStatusEnum.CHECK_FAIL.status) {
                logger.info("$interfaceName:batchRecheckByProject:$projectCode:($imageCode,$imageId,$imageStatus)recheck")
                imageReleaseService.recheckWithoutValidate(context = dslContext, userId = userId, imageId = imageId)
                count++
            }
        }
        return count
    }

    /**
     * 重新验证所有验证失败的镜像
     */
    fun batchRecheckAll(
        userId: String,
        interfaceName: String? = "Anon interface"
    ): Int {
        logger.info("$interfaceName:batchRecheckAll:Input($userId)")
        // 0.鉴权：管理员才有权限操作
        if (!imageAdminUsers.contains(userId.trim())) {
            throw PermissionForbiddenException("Permission Denied,userId=$userId")
        }
        val records = opImageDao.listAllImages(dslContext)
        var count = 0
        records?.forEach { it ->
            val imageId = it.get(KEY_IMAGE_ID) as String
            val imageCode = it.get(KEY_IMAGE_CODE) as String
            val imageStatus = ((it.get(KEY_IMAGE_STATUS) as Byte?)?.toInt() ?: 0)
            logger.info("$interfaceName:batchRecheckAll:($imageCode,$imageId,$imageStatus)")
            if (imageStatus == ImageStatusEnum.CHECK_FAIL.status) {
                logger.info("$interfaceName:batchRecheckAll:($imageCode,$imageId,$imageStatus)recheck")
                imageReleaseService.recheckWithoutValidate(context = dslContext, userId = userId, imageId = imageId)
                count++
            }
        }
        return count
    }

    /**
     * 迁移一个项目的历史数据
     */
    fun transferImage(
        userId: String,
        projectCode: String,
        classifyCode: String?,
        categoryCode: String?,
        interfaceName: String? = "Anon interface"
    ): Int {
        val realClassifyCode = classifyCode ?: CLASSIFYCODE_OTHER
        val realCategoryCode = categoryCode ?: CATEGORY_PIPELINE_JOB
        logger.info("$interfaceName:transferImage:==========$projectCode===========")
        logger.info("$interfaceName:transferImage:Input($userId,$projectCode)")
        // 0.鉴权：管理员才有权限操作
        if (!imageAdminUsers.contains(userId.trim())) {
            throw PermissionForbiddenException("Permission Denied,userId=$userId")
        }
        var changedCount = 0
        // 1.调image接口获取项目构建镜像信息
        val dockerBuildImageList =
            client.get(ServiceImageResource::class).listDockerBuildImages(userId, projectCode)
                .data // linux环境第三方镜像
        logger.info("$interfaceName:transferImage:Inner(dockerBuildImageList?.size=${dockerBuildImageList?.size})")
        logger.info("$interfaceName:transferImage:Inner(dockerBuildImageList=${dockerBuildImageList?.map { it.image }})")
        changedCount += transferImageList(
            projectCode = projectCode,
            realClassifyCode = realClassifyCode,
            realCategoryCode = realCategoryCode,
            // Docker镜像默认在Devnet与IDC均可使用
            agentTypeList = listOf(ImageAgentTypeEnum.DOCKER, ImageAgentTypeEnum.IDC),
            imageList = dockerBuildImageList!!,
            interfaceName = interfaceName
        )
        // 1.调image接口获取项目构建镜像信息
        val devCloudImageList =
            client.get(ServiceImageResource::class).listDevCloudImages(userId, projectCode, false)
                .data // linux环境第三方镜像
        logger.info("$interfaceName:transferImage:Inner(devCloudImageList?.size=${devCloudImageList?.size})")
        logger.info("$interfaceName:transferImage:Inner(devCloudImageList=${devCloudImageList?.map { it.image }})")
        changedCount += transferImageList(
            projectCode = projectCode,
            realClassifyCode = realClassifyCode,
            realCategoryCode = realCategoryCode,
            agentTypeList = listOf(ImageAgentTypeEnum.PUBLIC_DEVCLOUD),
            imageList = devCloudImageList!!,
            interfaceName = interfaceName
        )
        return changedCount
    }

    fun transferImageList(
        projectCode: String,
        realClassifyCode: String,
        realCategoryCode: String,
        agentTypeList: List<ImageAgentTypeEnum>,
        imageList: List<DockerTag>,
        interfaceName: String? = "Anon interface"
    ): Int {
        imageList.sortedBy { it.created }
        var changedCount = 0
        // 2.调project接口获取项目负责人信息
        val projectInfo =
            client.get(ServiceProjectResource::class).listOnlyByProjectCode(setOf(projectCode)).data?.get(0)
                ?: throw DataConsistencyException(
                    srcData = "projectCode=$projectCode",
                    targetData = "projectDetail",
                    message = "Fail to get projectDetail by projectCode($projectCode)"
                )
        val creator = projectInfo.creator ?: "system"
        imageList?.forEach loop@{
            val imageRepoUrl = it.image!!.split("/")[0]
            val imageRepoName = it.repo!!
            // 3.获取tag
            val imageTag = it.tag ?: "latest"
            logger.warn("$interfaceName:transferImage:Inner:processing image:($imageRepoUrl,$imageRepoName,$imageTag)")
            if (opImageDao.countImageByRepoInfo(dslContext, imageRepoUrl, imageRepoName, imageTag) > 0) {
                // 该条数据已迁移过，不再处理
                logger.warn("$interfaceName:transferImage:Inner:already processed:$imageRepoUrl/$imageRepoName:$imageTag")
                return@loop
            }

            // 4.镜像标识生成
            var imageCode = ""
            var imagesNum = 0
            if (opImageDao.countImageByRepoInfo(dslContext, imageRepoUrl, imageRepoName, null) > 0) {
                // 复用已有的code
                val records = opImageDao.getImagesByRepoInfo(dslContext, imageRepoUrl, imageRepoName, null)
                imagesNum = records?.size ?: 0
                imageCode = records!![0].imageCode
                // 将已迁移完成的老版本镜像全部置为已发布
                records.forEach { record ->
                    logger.info("$interfaceName:transferImage:release existed image(${record.id},${record.imageCode},${record.version},${record.imageRepoUrl},${record.imageRepoName},${record.imageTag})")
                    if (record.imageStatus != ImageStatusEnum.RELEASED.status.toByte()) {
                        opImageService.releaseImageDirectly(
                            context = dslContext,
                            userId = creator,
                            imageCode = record.imageCode,
                            imageId = record.id
                        )
                    }
                }
            } else {
                // 生成code
                imageCode = it.repo!!.removePrefix("/").removeSuffix("/").replace("paas/bkdevops/", "").replace("/", "_")
                // 超长处理
                if (imageCode.length > 60) {
                    imageCode = imageCode.substring(imageCode.length - 60)
                    if (imageCode.indexOf("_") != -1) {
                        imageCode = imageCode.substring(imageCode.indexOf("_") + 1)
                    }
                    if (imageCode.isEmpty()) {
                        imageCode = "build_image"
                    }
                }
                // 重复处理
                var imageCodeNum = 0
                var tempImageCode = imageCode
                while (imageFeatureDao.countByCode(dslContext, tempImageCode) > 0) {
                    logger.warn("$interfaceName:transferImage:Inner:imageCode $tempImageCode already exists")
                    imageCodeNum += 1
                    tempImageCode = imageCode + "_$imageCodeNum"
                }
                imageCode = tempImageCode
            }
            // 5.镜像名称生成
            val index = it.repo!!.lastIndexOf("/")
            var imageName = if (-1 != index) {
                it.repo!!.substring(index + 1)
            } else {
                it.repo ?: ""
            }
            // 超长处理
            if (imageName.length > 60) {
                imageName = imageName.substring(imageName.length - 60)
                if (imageName.indexOf("_") != -1) {
                    imageName = imageName.substring(imageName.indexOf("_") + 1)
                }
                if (imageName.isEmpty()) {
                    imageName = "build_image"
                }
            }
            // 重复处理
            var imageNameNum = 0
            var tempImageName = imageName
            while (imageDao.countByName(dslContext, tempImageName) > 0) {
                logger.warn("$interfaceName:transferImage:Inner:imageName $tempImageName already exists")
                imageNameNum += 1
                tempImageName = imageName + "_$imageNameNum"
            }
            imageName = tempImageName
            // 6.调OP新增镜像接口
            // image字段是含repoUrl、repoName、tag的完整字段
            logger.info("$interfaceName:transferImage:Inner:ImageCreateRequest($creator,$projectCode,$imageName,$imageCode,${it.image},${it.repo},$imageTag)")
            val imageId = opImageService.addImage(
                accessToken = "",
                userId = creator!!,
                imageCreateRequest = ImageCreateRequest(
                    projectCode = projectCode,
                    imageName = imageName!!,
                    imageCode = imageCode,
                    classifyCode = realClassifyCode,
                    category = realCategoryCode,
                    agentTypeScope = agentTypeList,
                    version = "1.0.$imagesNum",
                    releaseType = if (imagesNum == 0) {
                        ReleaseTypeEnum.NEW
                    } else {
                        ReleaseTypeEnum.COMPATIBILITY_FIX
                    },
                    versionContent = MessageUtil.getMessageByLocale(
                        messageCode = BK_IMAGE_STORE_ONLINE,
                        language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                    ),
                    imageSourceType = ImageType.BKDEVOPS,
                    imageRepoUrl = imageRepoUrl,
                    imageRepoName = imageRepoName,
                    ticketId = null,
                    imageTag = imageTag,
                    dockerFileType = null,
                    dockerFileContent = null,
                    logoUrl = "http://radosgw.open.oa.com/paas_backend/ieod/prod/file/png/random_15755397330026456632033301754111.png?v=1575539733",
                    iconData = null,
                    summary = MessageUtil.getMessageByLocale(
                        messageCode = BK_OLD_VERSION_BUILD_IMAGE,
                        language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                    ) + "。\n" +
                            MessageUtil.getMessageByLocale(
                                messageCode = BK_AUTOMATICALLY_CONVERTED,
                                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                            ),
                    description = MessageUtil.getMessageByLocale(
                        messageCode = BK_COPY_FOR_BUILD_IMAGE,
                        language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                    ) + "\n" + MessageUtil.getMessageByLocale(
                                messageCode = BK_AFTER_IMAGE_STORE_ONLINE,
                                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                            ) + "\n" + MessageUtil.getMessageByLocale(
                        messageCode = BK_PROJECT_MANAGER_CAN_OPERATION,
                        language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                    ),
                    publisher = creator,
                    labelIdList = null
                ),
                checkLatest = false,
                needAuth = false,
                // 批量迁移不发送通知，避免打扰
                sendCheckResultNotify = false,
                runCheckPipeline = false
            ).data
            logger.info("$interfaceName:transferImage:Inner:imageId=$imageId")
            logger.info("$interfaceName:transferImage:Output:Start validating")
            // 将项目所有管理员添加为镜像管理员
            val managers = client.get(ServiceTxProjectResource::class).getProjectManagers(projectCode).data
            if (managers != null) {
                // 内部已做防重复处理
                imageMemberService.add(
                    userId = creator,
                    storeMemberReq = StoreMemberReq(
                        member = managers,
                        type = StoreMemberTypeEnum.ADMIN,
                        storeCode = imageCode,
                        storeType = StoreTypeEnum.IMAGE
                    ),
                    storeType = StoreTypeEnum.IMAGE,
                    sendNotify = false
                )
            }
            // 睡眠1s，规避mysql日期精度问题
            Thread.sleep(1000)
            changedCount += 1
        }
        return changedCount
    }
}

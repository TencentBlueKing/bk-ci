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

import com.tencent.devops.common.api.constant.APPROVE
import com.tencent.devops.common.api.constant.BEGIN
import com.tencent.devops.common.api.constant.CHECK
import com.tencent.devops.common.api.constant.COMMIT
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.DOING
import com.tencent.devops.common.api.constant.END
import com.tencent.devops.common.api.constant.FAIL
import com.tencent.devops.common.api.constant.LATEST
import com.tencent.devops.common.api.constant.NUM_FIVE
import com.tencent.devops.common.api.constant.NUM_FOUR
import com.tencent.devops.common.api.constant.NUM_ONE
import com.tencent.devops.common.api.constant.NUM_SIX
import com.tencent.devops.common.api.constant.NUM_THREE
import com.tencent.devops.common.api.constant.NUM_TWO
import com.tencent.devops.common.api.constant.SUCCESS
import com.tencent.devops.common.api.constant.TEST
import com.tencent.devops.common.api.constant.UNDO
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.CheckImageInitPipelineReq
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.image.api.ServiceImageResource
import com.tencent.devops.model.store.tables.records.TImageRecord
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineInitResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.dao.common.StorePipelineBuildRelDao
import com.tencent.devops.store.dao.common.StorePipelineRelDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.dao.common.StoreReleaseDao
import com.tencent.devops.store.dao.image.ImageDao
import com.tencent.devops.store.dao.image.ImageLabelRelDao
import com.tencent.devops.store.dao.image.MarketImageDao
import com.tencent.devops.store.dao.image.MarketImageFeatureDao
import com.tencent.devops.store.dao.image.MarketImageVersionLogDao
import com.tencent.devops.store.pojo.common.ReleaseProcessItem
import com.tencent.devops.store.pojo.common.StoreProcessInfo
import com.tencent.devops.store.pojo.common.StoreReleaseCreateRequest
import com.tencent.devops.store.pojo.common.enums.AuditTypeEnum
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreMemberTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
import com.tencent.devops.store.pojo.image.request.ImageFeatureCreateRequest
import com.tencent.devops.store.pojo.image.request.ImageStatusInfoUpdateRequest
import com.tencent.devops.store.pojo.image.request.MarketImageRelRequest
import com.tencent.devops.store.pojo.image.request.MarketImageUpdateRequest
import com.tencent.devops.store.service.common.StoreCommonService
import com.tencent.devops.ticket.api.ServiceCredentialResource
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import java.time.LocalDateTime
import java.util.Base64

@Service
class ImageReleaseService @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val imageDao: ImageDao,
    private val marketImageDao: MarketImageDao,
    private val marketImageFeatureDao: MarketImageFeatureDao,
    private val marketImageVersionLogDao: MarketImageVersionLogDao,
    private val imageLabelRelDao: ImageLabelRelDao,
    private val storeMemberDao: StoreMemberDao,
    private val storePipelineRelDao: StorePipelineRelDao,
    private val storePipelineBuildRelDao: StorePipelineBuildRelDao,
    private val storeReleaseDao: StoreReleaseDao,
    private val storeCommonService: StoreCommonService,
    private val imageNotifyService: ImageNotifyService,
    private val client: Client
) {
    private val logger = LoggerFactory.getLogger(ImageReleaseService::class.java)

    fun addMarketImage(
        accessToken: String,
        userId: String,
        imageCode: String,
        marketImageRelRequest: MarketImageRelRequest,
        needAuth: Boolean = true
    ): Result<String> {
        logger.info("addMarketImage accessToken is :$accessToken, userId is :$userId")
        logger.info("addMarketImage imageCode is :$imageCode, marketImageRelRequest is :$marketImageRelRequest")
        // 判断镜像代码是否存在
        val codeCount = imageDao.countByCode(dslContext, imageCode)
        if (codeCount > 0) {
            // 抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_EXIST, arrayOf(imageCode), null)
        }
        val imageName = marketImageRelRequest.imageName
        // 判断镜像名称是否存在
        val nameCount = imageDao.countByName(dslContext, imageName)
        if (nameCount > 0) {
            // 抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_EXIST, arrayOf(imageName), null)
        }
        if (needAuth) {
            val validateFlag: Boolean?
            try {
                // 判断用户是否项目的成员
                validateFlag = client.get(ServiceProjectResource::class).verifyUserProjectPermission(
                    accessToken = accessToken,
                    projectCode = marketImageRelRequest.projectCode,
                    userId = userId
                ).data
            } catch (e: Exception) {
                logger.error("verifyUserProjectPermission error is :$e")
                return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
            }
            logger.info("the validateFlag is :$validateFlag")
            if (null == validateFlag || !validateFlag) {
                // 抛出错误提示
                return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
            }
        }
        val imageId = addMarketImageToDB(accessToken, userId, imageCode, marketImageRelRequest)
        return if (null != imageId) {
            Result(imageId)
        } else {
            Result("")
        }
    }

    fun addMarketImageToDB(
        accessToken: String,
        userId: String,
        imageCode: String,
        marketImageRelRequest: MarketImageRelRequest
    ): String? {
        val imageId = UUIDUtil.generate()
        dslContext.transaction { t ->
            val context = DSL.using(t)
            marketImageDao.addMarketImage(context, userId, imageId, imageCode, marketImageRelRequest)
            // 添加镜像与项目关联关系
            storeProjectRelDao.addStoreProjectRel(
                dslContext = context,
                userId = userId,
                storeCode = imageCode,
                projectCode = marketImageRelRequest.projectCode,
                type = StoreProjectTypeEnum.INIT.type.toByte(),
                storeType = StoreTypeEnum.IMAGE.type.toByte()
            )
            storeProjectRelDao.addStoreProjectRel(
                dslContext = context,
                userId = userId,
                storeCode = imageCode,
                projectCode = marketImageRelRequest.projectCode,
                type = StoreProjectTypeEnum.TEST.type.toByte(),
                storeType = StoreTypeEnum.IMAGE.type.toByte()
            )
            // 默认给关联镜像的人赋予管理员权限
            storeMemberDao.addStoreMember(
                dslContext = context,
                userId = userId,
                storeCode = imageCode,
                userName = userId,
                type = StoreMemberTypeEnum.ADMIN.type.toByte(),
                storeType = StoreTypeEnum.IMAGE.type.toByte()
            )
            // 添加镜像特性信息
            marketImageFeatureDao.addImageFeature(
                dslContext = context,
                userId = userId,
                imageFeatureCreateRequest = ImageFeatureCreateRequest(
                    imageCode = imageCode
                )
            )
        }
        return imageId
    }

    fun updateMarketImage(
        userId: String,
        marketImageUpdateRequest: MarketImageUpdateRequest,
        checkLatest: Boolean = true
    ): Result<String?> {
        logger.info("updateMarketImage userId is :$userId, marketImageUpdateRequest is :$marketImageUpdateRequest")
        val imageCode = marketImageUpdateRequest.imageCode
        val imageTag = marketImageUpdateRequest.imageTag
        // 判断镜像tag是否为latest
        if (checkLatest && imageTag == LATEST) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(imageTag))
        }
        val imageRecords = marketImageDao.getImagesByImageCode(dslContext, imageCode)
        logger.info("the imageRecords is :$imageRecords")
        if (null == imageRecords || imageRecords.size == 0) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(imageCode))
        }
        val imageName = marketImageUpdateRequest.imageName
        // 判断更新的名称是否已存在
        val count = imageDao.countByName(dslContext, imageName)
        if (validateNameIsExist(count, imageRecords, imageName)) return MessageCodeUtil.generateResponseDataObject(
            CommonMessageCode.PARAMETER_IS_EXIST,
            arrayOf(imageName)
        )
        val imageRecord = imageRecords[0]
        val imageSourceType = marketImageUpdateRequest.imageSourceType
        val imageRepoName = marketImageUpdateRequest.imageRepoName
        if (imageSourceType == ImageType.BKDEVOPS) {
            // 判断用户发布的镜像是否是自已名下有权限操作的镜像
            val projectCode = storeProjectRelDao.getUserStoreTestProjectCode(dslContext, userId, imageCode, StoreTypeEnum.IMAGE)
            val listProjectImagesResult = client.get(ServiceImageResource::class).listProjectImages(
                userId = userId,
                projectId = projectCode!!,
                searchKey = imageRepoName,
                start = null,
                limit = null
                )
            logger.info("the listProjectImagesResult is :$listProjectImagesResult")
            if (listProjectImagesResult.isNotOk()) {
                return Result(listProjectImagesResult.status, listProjectImagesResult.message, null)
            }
            val projectImagePageData = listProjectImagesResult.data
            if (null == projectImagePageData || projectImagePageData.total < 1) {
                // 查询是否上架的是公共镜像
                val listPublicImagesResult = client.get(ServiceImageResource::class).listPublicImages(
                    userId = userId,
                    searchKey = imageRepoName,
                    start = null,
                    limit = null
                )
                logger.info("the listPublicImagesResult is :$listPublicImagesResult")
                if (listPublicImagesResult.isNotOk()) {
                    return Result(listPublicImagesResult.status, listPublicImagesResult.message, null)
                }
                val publicImagePageData = listPublicImagesResult.data
                if (null == publicImagePageData || publicImagePageData.total < 1) {
                    return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
                }
            }
        }
        // 判断镜像的tag是否被关联过
        val relFlag = imageDao.countByTag(
            dslContext = dslContext,
            imageRepoUrl = marketImageUpdateRequest.imageRepoUrl,
            imageRepoName = imageRepoName,
            imageTag = imageTag
        ) == 0 || (imageRecord.imageRepoUrl == marketImageUpdateRequest.imageRepoUrl?.trim() &&
            imageRecord.imageRepoName == marketImageUpdateRequest.imageRepoName.trim() &&
            imageRecord.imageTag == marketImageUpdateRequest.imageTag.trim())
        if (!relFlag) {
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_EXIST,
                arrayOf(imageTag)
            )
        }
        // 校验前端传的版本号是否正确
        val releaseType = marketImageUpdateRequest.releaseType
        val version = marketImageUpdateRequest.version
        val dbVersion = imageRecord.version
        // 最近的版本处于上架中止状态，重新升级版本号不变
        val cancelFlag = imageRecord.imageStatus == ImageStatusEnum.GROUNDING_SUSPENSION.status.toByte()
        val requireVersion = if (cancelFlag && releaseType == ReleaseTypeEnum.CANCEL_RE_RELEASE) dbVersion else storeCommonService.getRequireVersion(dbVersion, releaseType)
        if (version != requireVersion) {
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_IMAGE_VERSION_IS_INVALID, arrayOf(version, requireVersion))
        }
        if (imageRecords.size > 1) {
            // 判断最近一个镜像版本的状态，只有处于审核驳回、已发布、上架中止和已下架的状态才允许添加新的版本
            val imageFinalStatusList = listOf(
                ImageStatusEnum.AUDIT_REJECT.status.toByte(),
                ImageStatusEnum.RELEASED.status.toByte(),
                ImageStatusEnum.GROUNDING_SUSPENSION.status.toByte(),
                ImageStatusEnum.UNDERCARRIAGED.status.toByte()
            )
            if (!imageFinalStatusList.contains(imageRecord.imageStatus)) {
                return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_IMAGE_VERSION_IS_NOT_FINISH, arrayOf(imageRecord.imageName, imageRecord.version))
            }
        }
        var imageId = UUIDUtil.generate()
        dslContext.transaction { t ->
            val context = DSL.using(t)
            if (StringUtils.isEmpty(imageRecord.version) || (cancelFlag && releaseType == ReleaseTypeEnum.CANCEL_RE_RELEASE)) {
                // 首次创建版本或者取消发布后不变更版本号重新上架，则在该版本的记录上做更新操作
                imageId = imageRecord.id
                val finalReleaseType = if (releaseType == ReleaseTypeEnum.CANCEL_RE_RELEASE) {
                    val imageVersion = marketImageVersionLogDao.getImageVersion(context, imageId)
                    imageVersion.releaseType
                } else {
                    releaseType.releaseType.toByte()
                }
                updateMarketImage(
                    context = context,
                    userId = userId,
                    imageId = imageId,
                    imageSize = "",
                    releaseType = finalReleaseType,
                    marketImageUpdateRequest = marketImageUpdateRequest
                )
            } else {
                // 升级镜像
                upgradeMarketImage(
                    context = context,
                    userId = userId,
                    imageId = imageId,
                    imageSize = "",
                    imageRecord = imageRecord,
                    marketImageUpdateRequest = marketImageUpdateRequest
                )
            }
            // 更新标签信息
            imageLabelRelDao.deleteByImageId(context, imageId)
            val labelIdList = marketImageUpdateRequest.labelIdList
            if (null != labelIdList && labelIdList.isNotEmpty()) {
                imageLabelRelDao.batchAdd(context, userId, imageId, labelIdList)
            }
            // 运行检查镜像合法性的流水线
            runCheckImagePipeline(context, userId, imageId)
        }
        return Result(imageId)
    }

    fun recheck(
        userId: String,
        imageId: String,
        validateUserFlag: Boolean = true
    ): Result<Boolean> {
        logger.info("recheck userId is:$userId,imageId is:$imageId, validateUserFlag:$validateUserFlag")
        // 判断是否可以重新验证镜像
        val status = ImageStatusEnum.CHECKING.status.toByte()
        val (checkResult, code, params) = checkImageVersionOptRight(userId, imageId, status, validateUserFlag)
        if (!checkResult) {
            return MessageCodeUtil.generateResponseDataObject(code!!, params, false)
        }
        runCheckImagePipeline(dslContext, userId, imageId)
        return Result(true)
    }

    fun passTest(
        userId: String,
        imageId: String,
        validateUserFlag: Boolean = true
    ): Result<Boolean> {
        logger.info("passTest, userId:$userId, imageId:$imageId, validateUserFlag:$validateUserFlag")
        val imageRecord = imageDao.getImage(dslContext, imageId)
        logger.info("passTest imageRecord is:$imageRecord")
        if (null == imageRecord) {
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf(imageId),
                false
            )
        }
        // 查看当前版本之前的版本是否有已发布的，如果有已发布的版本则只是普通的升级操作而不需要审核
        val imageCode = imageRecord.imageCode
        val isNormalUpgrade = getNormalUpgradeFlag(imageCode, imageRecord.imageStatus.toInt())
        logger.info("passTest isNormalUpgrade is:$isNormalUpgrade")
        val imageStatus =
            if (isNormalUpgrade) ImageStatusEnum.RELEASED.status.toByte() else ImageStatusEnum.AUDITING.status.toByte()
        val (checkResult, code, params) = checkImageVersionOptRight(userId, imageId, imageStatus, validateUserFlag)
        if (!checkResult) {
            return MessageCodeUtil.generateResponseDataObject(code!!, params, false)
        }
        if (isNormalUpgrade) {
            dslContext.transaction { t ->
                val context = DSL.using(t)
                // 清空旧版本LATEST_FLAG
                marketImageDao.cleanLatestFlag(context, imageRecord.imageCode)
                val pubTime = LocalDateTime.now()
                // 记录发布信息
                storeReleaseDao.addStoreReleaseInfo(
                    dslContext = context,
                    userId = userId,
                    storeReleaseCreateRequest = StoreReleaseCreateRequest(
                        storeCode = imageCode,
                        storeType = StoreTypeEnum.IMAGE,
                        latestUpgrader = imageRecord.creator,
                        latestUpgradeTime = pubTime
                    )
                )
                marketImageDao.updateImageStatusInfo(
                    dslContext = context,
                    imageId = imageId,
                    imageStatus = imageStatus,
                    imageStatusMsg = "",
                    latestFlag = true,
                    pubTime = pubTime
                )
            }
            // 通知发布者
            imageNotifyService.sendImageReleaseAuditNotifyMessage(imageId, AuditTypeEnum.AUDIT_SUCCESS)
        } else {
            marketImageDao.updateImageStatusById(dslContext, imageId, imageStatus, userId, "")
        }
        return Result(true)
    }

    private fun runCheckImagePipeline(
        context: DSLContext,
        userId: String,
        imageId: String
    ) {
        logger.info("runCheckImagePipeline userId is:$userId,imageId is:$imageId")
        val imageRecord = imageDao.getImage(context, imageId)!!
        val imageCode = imageRecord.imageCode
        val version = imageRecord.version
        val imagePipelineRelRecord = storePipelineRelDao.getStorePipelineRel(context, imageCode, StoreTypeEnum.IMAGE)
        val projectCode = storeProjectRelDao.getInitProjectCodeByStoreCode(
            context,
            imageCode,
            StoreTypeEnum.IMAGE.type.toByte()
        ) // 查找新增镜像时关联的项目
        logger.info("runCheckImagePipeline imagePipelineRelRecord is:$imagePipelineRelRecord,projectCode is:$projectCode")
        val ticketId = imageRecord.ticketId
        var userName: String? = null
        var password: String? = null
        if (!ticketId.isNullOrBlank()) {
            val pair = DHUtil.initKey()
            val encoder = Base64.getEncoder()
            val decoder = Base64.getDecoder()
            val credentialResult = client.get(ServiceCredentialResource::class).get(projectCode!!, ticketId,
                encoder.encodeToString(pair.publicKey))
            if (credentialResult.isNotOk() || credentialResult.data == null) {
                throw ParamBlankException("Fail to get the credential($ticketId) of project($projectCode)")
            }
            val credential = credentialResult.data!!
            userName = String(DHUtil.decrypt(
                decoder.decode(credential.v1),
                decoder.decode(credential.publicKey),
                pair.privateKey))
            if (credential.v2 != null && credential.v2!!.isNotEmpty()) {
                password = String(DHUtil.decrypt(
                    decoder.decode(credential.v2),
                    decoder.decode(credential.publicKey),
                    pair.privateKey))
            }
        }
        val dockerImageName = "${imageRecord.imageRepoUrl}/${imageRecord.imageRepoName}:${imageRecord.imageTag}"
        val imageSourceType = imageRecord.imageSourceType
        if (null == imagePipelineRelRecord) {
            val checkImageInitPipelineReq = CheckImageInitPipelineReq(
                imageCode = imageCode,
                imageName = dockerImageName,
                version = version,
                imageType = imageSourceType,
                registryUser = userName,
                registryPwd = password
            )
            val checkImageInitPipelineResp = client.get(ServicePipelineInitResource::class)
                .initCheckImagePipeline(userId, projectCode!!, checkImageInitPipelineReq).data
            logger.info("the checkImageInitPipelineResp is:$checkImageInitPipelineResp")
            if (null != checkImageInitPipelineResp) {
                storePipelineRelDao.add(context, imageCode, StoreTypeEnum.IMAGE, checkImageInitPipelineResp.pipelineId)
                marketImageDao.updateImageStatusById(
                    context,
                    imageId,
                    checkImageInitPipelineResp.imageCheckStatus.status.toByte(),
                    userId,
                    null
                )
                val buildId = checkImageInitPipelineResp.buildId
                if (null != buildId) {
                    storePipelineBuildRelDao.add(context, imageId, checkImageInitPipelineResp.pipelineId, buildId)
                }
            }
        } else {
            // 触发执行流水线
            val startParams = mutableMapOf<String, String>() // 启动参数
            startParams["imageCode"] = imageCode
            startParams["imageName"] = dockerImageName
            startParams["version"] = version
            if (null != imageSourceType) {
                startParams["imageType"] = imageSourceType
            }
            val buildIdObj = client.get(ServiceBuildResource::class).manualStartup(
                userId, projectCode!!, imagePipelineRelRecord.pipelineId, startParams,
                ChannelCode.AM
            ).data
            logger.info("the buildIdObj is:$buildIdObj")
            if (null != buildIdObj) {
                storePipelineBuildRelDao.add(context, imageId, imagePipelineRelRecord.pipelineId, buildIdObj.id)
                marketImageDao.updateImageStatusById(
                    context,
                    imageId,
                    ImageStatusEnum.CHECKING.status.toByte(),
                    userId,
                    null
                ) // 验证中
            } else {
                marketImageDao.updateImageStatusById(
                    context,
                    imageId,
                    ImageStatusEnum.CHECK_FAIL.status.toByte(),
                    userId,
                    null
                ) // 验证失败
            }
        }
    }

    private fun updateMarketImage(
        context: DSLContext,
        userId: String,
        imageId: String,
        imageSize: String,
        releaseType: Byte,
        marketImageUpdateRequest: MarketImageUpdateRequest
    ) {
        marketImageDao.updateMarketImage(
            dslContext = context,
            userId = userId,
            imageId = imageId,
            imageSize = imageSize,
            marketImageUpdateRequest = marketImageUpdateRequest
        )
        marketImageVersionLogDao.addMarketImageVersion(
            dslContext = context,
            userId = userId,
            imageId = imageId,
            releaseType = releaseType,
            versionContent = marketImageUpdateRequest.versionContent
        )
    }

    private fun upgradeMarketImage(
        context: DSLContext,
        userId: String,
        imageId: String,
        imageSize: String,
        imageRecord: TImageRecord,
        marketImageUpdateRequest: MarketImageUpdateRequest
    ) {
        marketImageDao.upgradeMarketImage(
            dslContext = context,
            userId = userId,
            imageId = imageId,
            imageSize = imageSize,
            imageRecord = imageRecord,
            marketImageUpdateRequest = marketImageUpdateRequest
        )
        marketImageVersionLogDao.addMarketImageVersion(
            dslContext = context,
            userId = userId,
            imageId = imageId,
            releaseType = marketImageUpdateRequest.releaseType.releaseType.toByte(),
            versionContent = marketImageUpdateRequest.versionContent
        )
    }

    /**
     * 获取发布进度
     */
    fun getProcessInfo(userId: String, imageId: String): Result<StoreProcessInfo> {
        logger.info("getProcessInfo imageId: $imageId")
        val record = imageDao.getImage(dslContext, imageId)
        logger.info("getProcessInfo record: $record")
        if (null == record) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(imageId))
        } else {
            val status = record.imageStatus.toInt()
            val imageCode = record.imageCode
            // 查看当前版本之前的版本是否有已发布的，如果有已发布的版本则只是普通的升级操作而不需要审核
            val isNormalUpgrade = getNormalUpgradeFlag(imageCode, status)
            logger.info("getProcessInfo isNormalUpgrade: $isNormalUpgrade")
            val processInfo = initProcessInfo(isNormalUpgrade)
            val totalStep = if (isNormalUpgrade) NUM_FIVE else NUM_SIX
            when (status) {
                ImageStatusEnum.INIT.status, ImageStatusEnum.COMMITTING.status -> {
                    storeCommonService.setProcessInfo(processInfo, totalStep, NUM_TWO, DOING)
                }
                ImageStatusEnum.CHECKING.status -> {
                    storeCommonService.setProcessInfo(processInfo, totalStep, NUM_THREE, DOING)
                }
                ImageStatusEnum.CHECK_FAIL.status -> {
                    storeCommonService.setProcessInfo(processInfo, totalStep, NUM_THREE, FAIL)
                }
                ImageStatusEnum.TESTING.status -> {
                    storeCommonService.setProcessInfo(processInfo, totalStep, NUM_FOUR, DOING)
                }
                ImageStatusEnum.AUDITING.status -> {
                    storeCommonService.setProcessInfo(processInfo, totalStep, NUM_FIVE, DOING)
                }
                ImageStatusEnum.AUDIT_REJECT.status -> {
                    storeCommonService.setProcessInfo(processInfo, totalStep, NUM_FIVE, FAIL)
                }
                ImageStatusEnum.RELEASED.status -> {
                    val currStep = if (isNormalUpgrade) NUM_FIVE else NUM_SIX
                    storeCommonService.setProcessInfo(processInfo, totalStep, currStep, SUCCESS)
                }
            }
            val storeProcessInfo = storeCommonService.generateStoreProcessInfo(userId, imageId, imageCode, StoreTypeEnum.IMAGE, record.modifier, processInfo)
            logger.info("getProcessInfo storeProcessInfo: $storeProcessInfo")
            return Result(storeProcessInfo)
        }
    }

    /**
     * 初始化进度
     */
    private fun initProcessInfo(isNormalUpgrade: Boolean): List<ReleaseProcessItem> {
        val processInfo = mutableListOf<ReleaseProcessItem>()
        processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(BEGIN), BEGIN, NUM_ONE, SUCCESS))
        processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(COMMIT), COMMIT, NUM_TWO, UNDO))
        processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(CHECK), CHECK, NUM_THREE, UNDO))
        processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(TEST), TEST, NUM_FOUR, UNDO))
        if (isNormalUpgrade) {
            processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(END), END, NUM_FIVE, UNDO))
        } else {
            processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(APPROVE), APPROVE, NUM_FIVE, UNDO))
            processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(END), END, NUM_SIX, UNDO))
        }
        return processInfo
    }

    /**
     * 取消发布
     */
    fun cancelRelease(userId: String, imageId: String): Result<Boolean> {
        logger.info("cancelRelease userId is:$userId, imageId is:$imageId")
        val status = ImageStatusEnum.GROUNDING_SUSPENSION.status.toByte()
        // 判断用户是否有权限
        val (checkResult, code, params) = checkImageVersionOptRight(userId, imageId, status)
        if (!checkResult) {
            return MessageCodeUtil.generateResponseDataObject(code!!, params, false)
        }
        marketImageDao.updateImageStatusById(dslContext, imageId, status, userId, "cancel release")
        return Result(true)
    }

    /**
     * 检查版本发布过程中的操作权限：重新构建、确认测试完成、取消发布
     */
    private fun checkImageVersionOptRight(
        userId: String,
        imageId: String,
        status: Byte,
        validateUserFlag: Boolean = true
    ): Triple<Boolean, String?, Array<String>?> {
        logger.info("checkImageVersionOptRight userId is:$userId, imageId is:$imageId, status is:$status, validateUserFlag is:$validateUserFlag")
        val imageRecord =
            imageDao.getImage(dslContext, imageId) ?: return Triple(false, CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(imageId))
        val imageCode = imageRecord.imageCode
        val modifier = imageRecord.modifier
        val imageStatus = imageRecord.imageStatus
        // 判断用户是否有权限
        if (!(storeMemberDao.isStoreAdmin(
                dslContext,
                userId,
                imageCode,
                StoreTypeEnum.IMAGE.type.toByte()
            ) || modifier == userId || !validateUserFlag)
        ) {
            return Triple(false, CommonMessageCode.PERMISSION_DENIED, null)
        }
        logger.info("imageRecord status=$imageStatus, status=$status")
        if (status == ImageStatusEnum.AUDITING.status.toByte() &&
            imageStatus != ImageStatusEnum.TESTING.status.toByte()
        ) {
            return Triple(false, StoreMessageCode.USER_IMAGE_RELEASE_STEPS_ERROR, null)
        } else if (status == ImageStatusEnum.CHECKING.status.toByte() &&
            imageStatus !in (listOf(ImageStatusEnum.CHECK_FAIL.status.toByte(), ImageStatusEnum.TESTING.status.toByte()))
        ) {
            return Triple(false, StoreMessageCode.USER_IMAGE_RELEASE_STEPS_ERROR, null)
        } else if (status == ImageStatusEnum.GROUNDING_SUSPENSION.status.toByte() &&
            imageStatus in (listOf(ImageStatusEnum.RELEASED.status.toByte()))
        ) {
            return Triple(false, StoreMessageCode.USER_IMAGE_RELEASE_STEPS_ERROR, null)
        }
        return Triple(true, null, null)
    }

    private fun validateNameIsExist(
        count: Int,
        imageRecords: org.jooq.Result<TImageRecord>,
        imageName: String
    ): Boolean {
        var flag = false
        if (count > 0) {
            for (item in imageRecords) {
                if (imageName == item.imageName) {
                    flag = true
                    break
                }
            }
            if (!flag) {
                return true
            }
        }
        return false
    }

    private fun getNormalUpgradeFlag(imageCode: String, status: Int): Boolean {
        // 判断镜像是首次上架还是普通升级
        val releaseTotalNum = marketImageDao.countReleaseImageByCode(dslContext, imageCode)
        val currentNum = if (status == ImageStatusEnum.RELEASED.status) 1 else 0
        return releaseTotalNum > currentNum
    }

    /**
     * 下架镜像
     */
    fun offlineMarketImage(
        userId: String,
        imageCode: String,
        version: String?,
        reason: String?,
        validateUserFlag: Boolean = true,
        interfaceName: String? = "Anon interface"
    ): Result<Boolean> {
        logger.info("$interfaceName:offlineMarketImage:Input:($userId,$imageCode,$version,$reason)")
        // 参数校验
        val validUserId = userId.trim()
        val validImageCode = imageCode.trim()
        val validVersion = version?.trim()
        // 判断用户是否有权限下架镜像
        if (!(storeMemberDao.isStoreAdmin(
                dslContext = dslContext,
                userId = validUserId,
                storeCode = validImageCode,
                storeType = StoreTypeEnum.IMAGE.type.toByte()
            ) || !validateUserFlag)
        ) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        if (!version.isNullOrEmpty()) {
            val imageRecord = imageDao.getImage(dslContext, validImageCode, validVersion!!)
                ?: return MessageCodeUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                    params = arrayOf(validImageCode, validVersion),
                    data = false
                )
            logger.info("$interfaceName:offlineMarketImage:Inner:imageRecord=(${imageRecord.imageCode},${imageRecord.version})")
            if (ImageStatusEnum.RELEASED.status.toByte() != imageRecord.imageStatus) {
                return MessageCodeUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                    params = arrayOf(validImageCode, validVersion),
                    data = false
                )
            }
            dslContext.transaction { t ->
                val context = DSL.using(t)
                val releaseImageRecords = marketImageDao.getReleaseImagesByCode(context, validImageCode)
                logger.info("$interfaceName:offlineMarketImage:Inner:releaseImageRecords.size=${releaseImageRecords?.size}")
                if (null != releaseImageRecords && releaseImageRecords.size > 0) {
                    marketImageDao.updateImageStatusInfoById(
                        dslContext = context,
                        imageId = imageRecord.id,
                        userId = validUserId,
                        imageStatusInfoUpdateRequest = ImageStatusInfoUpdateRequest(
                            imageStatus = ImageStatusEnum.UNDERCARRIAGED,
                            imageStatusMsg = reason,
                            latestFlag = false
                        )
                    )
                    val newestReleaseImageRecord = releaseImageRecords[0]
                    if (newestReleaseImageRecord.id == imageRecord.id) {
                        if (releaseImageRecords.size == 1) {
                            val newestUndercarriagedImage =
                                marketImageDao.getNewestUndercarriagedImageByCode(context, validImageCode)
                            logger.info("$interfaceName:offlineMarketImage:Inner:newestUndercarriagedImage=(${newestUndercarriagedImage?.imageCode},${newestUndercarriagedImage?.version})")
                            if (null != newestUndercarriagedImage) {
                                marketImageDao.updateImageStatusInfoById(
                                    dslContext = context,
                                    imageId = newestUndercarriagedImage.id,
                                    userId = validUserId,
                                    imageStatusInfoUpdateRequest = ImageStatusInfoUpdateRequest(
                                        latestFlag = true
                                    )
                                )
                            }
                        } else {
                            // 把前一个发布的版本的latestFlag置为true
                            val tmpImageRecord = releaseImageRecords[1]
                            logger.info("$interfaceName:offlineMarketImage:Inner:lastReleasedImage=(${tmpImageRecord?.imageCode},${tmpImageRecord?.version})")
                            marketImageDao.updateImageStatusInfoById(
                                dslContext = context,
                                imageId = tmpImageRecord.id,
                                userId = validUserId,
                                imageStatusInfoUpdateRequest = ImageStatusInfoUpdateRequest(
                                    latestFlag = true
                                )
                            )
                        }
                    }
                }
            }
        } else {
            // 把镜像所有已发布的版本全部下架
            logger.info("$interfaceName:offlineMarketImage:Inner:undercarriage all images")
            dslContext.transaction { t ->
                val context = DSL.using(t)
                marketImageDao.updateImageStatusByCode(
                    context, validImageCode, false, ImageStatusEnum.RELEASED.status.toByte(),
                    ImageStatusEnum.UNDERCARRIAGED.status.toByte(), validUserId, "undercarriage"
                )
                val newestUndercarriagedImage =
                    marketImageDao.getNewestUndercarriagedImageByCode(context, validImageCode)
                logger.info("$interfaceName:offlineMarketImage:Inner:newestUndercarriagedImage=(${newestUndercarriagedImage?.imageCode},${newestUndercarriagedImage?.version})")
                if (null != newestUndercarriagedImage) {
                    // 把发布时间最晚的下架版本latestFlag置为true
                    marketImageDao.updateImageLatestFlagById(
                        dslContext = context,
                        imageId = newestUndercarriagedImage.id,
                        userId = validUserId,
                        latestFlag = true
                    )
                }
            }
        }
        logger.info("$interfaceName:offlineMarketImage:Output:true")
        return Result(true)
    }
}
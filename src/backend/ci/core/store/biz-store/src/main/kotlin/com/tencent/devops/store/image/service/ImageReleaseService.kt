/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.store.image.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.INIT_VERSION
import com.tencent.devops.common.api.constant.LATEST
import com.tencent.devops.common.api.exception.DataConsistencyException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.image.api.ServiceImageResource
import com.tencent.devops.model.store.tables.records.TImageRecord
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.store.common.configuration.StoreInnerPipelineConfig
import com.tencent.devops.store.common.dao.BusinessConfigDao
import com.tencent.devops.store.common.dao.StoreMemberDao
import com.tencent.devops.store.common.dao.StorePipelineBuildRelDao
import com.tencent.devops.store.common.dao.StorePipelineRelDao
import com.tencent.devops.store.common.dao.StoreProjectRelDao
import com.tencent.devops.store.common.dao.StoreReleaseDao
import com.tencent.devops.store.common.dao.StoreStatisticTotalDao
import com.tencent.devops.store.common.service.StoreCommonService
import com.tencent.devops.store.common.service.StorePipelineService
import com.tencent.devops.store.common.utils.VersionUtils
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.constant.StoreMessageCode.IMAGE_ADD_NO_PROJECT_MEMBER
import com.tencent.devops.store.constant.StoreMessageCode.IMAGE_PUBLISH_REPO_NO_PERMISSION
import com.tencent.devops.store.constant.StoreMessageCode.NO_COMPONENT_ADMIN_PERMISSION
import com.tencent.devops.store.image.dao.ImageAgentTypeDao
import com.tencent.devops.store.image.dao.ImageCategoryRelDao
import com.tencent.devops.store.image.dao.ImageDao
import com.tencent.devops.store.image.dao.ImageFeatureDao
import com.tencent.devops.store.image.dao.ImageLabelRelDao
import com.tencent.devops.store.image.dao.MarketImageDao
import com.tencent.devops.store.image.dao.MarketImageFeatureDao
import com.tencent.devops.store.image.dao.MarketImageVersionLogDao
import com.tencent.devops.store.pojo.common.CLOSE
import com.tencent.devops.store.pojo.common.OPEN
import com.tencent.devops.store.pojo.common.PASS
import com.tencent.devops.store.pojo.common.REJECT
import com.tencent.devops.store.pojo.common.enums.AuditTypeEnum
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreMemberTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.ReleaseProcessItem
import com.tencent.devops.store.pojo.common.publication.StoreProcessInfo
import com.tencent.devops.store.pojo.common.publication.StoreReleaseCreateRequest
import com.tencent.devops.store.pojo.image.enums.ImageAgentTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageRDTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
import com.tencent.devops.store.pojo.image.request.ApproveImageReq
import com.tencent.devops.store.pojo.image.request.ImageFeatureCreateRequest
import com.tencent.devops.store.pojo.image.request.ImageStatusInfoUpdateRequest
import com.tencent.devops.store.pojo.image.request.MarketImageRelRequest
import com.tencent.devops.store.pojo.image.request.MarketImageUpdateRequest
import com.tencent.devops.store.pojo.image.response.ImageAgentTypeInfo
import com.tencent.devops.ticket.api.ServiceCredentialResource
import java.time.LocalDateTime
import java.util.Base64
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

@Suppress("ALL")
abstract class ImageReleaseService {

    private final val CATEGORY_PIPELINE_JOB = "PIPELINE_JOB"

    @Autowired
    lateinit var dslContext: DSLContext

    @Autowired
    lateinit var storeProjectRelDao: StoreProjectRelDao

    @Autowired
    lateinit var imageDao: ImageDao

    @Autowired
    lateinit var marketImageDao: MarketImageDao

    @Autowired
    lateinit var imageCategoryRelDao: ImageCategoryRelDao

    @Autowired
    lateinit var marketImageFeatureDao: MarketImageFeatureDao

    @Autowired
    lateinit var marketImageVersionLogDao: MarketImageVersionLogDao

    @Autowired
    lateinit var imageLabelRelDao: ImageLabelRelDao

    @Autowired
    lateinit var storeMemberDao: StoreMemberDao

    @Autowired
    lateinit var storePipelineRelDao: StorePipelineRelDao

    @Autowired
    lateinit var storePipelineBuildRelDao: StorePipelineBuildRelDao

    @Autowired
    lateinit var storeReleaseDao: StoreReleaseDao

    @Autowired
    lateinit var imageAgentTypeDao: ImageAgentTypeDao

    @Autowired
    lateinit var imageFeatureDao: ImageFeatureDao

    @Autowired
    lateinit var businessConfigDao: BusinessConfigDao

    @Autowired
    lateinit var storeStatisticTotalDao: StoreStatisticTotalDao

    @Autowired
    lateinit var storeCommonService: StoreCommonService

    @Autowired
    lateinit var imageNotifyService: ImageNotifyService

    @Autowired
    lateinit var storePipelineService: StorePipelineService

    @Autowired
    lateinit var client: Client

    @Autowired
    lateinit var storeInnerPipelineConfig: StoreInnerPipelineConfig

    private val logger = LoggerFactory.getLogger(ImageReleaseService::class.java)

    @Value("\${store.imageApproveSwitch:close}")
    protected lateinit var imageApproveSwitch: String

    @Value("\${store.imageAgentTypes:DOCKER}")
    protected lateinit var imageAgentTypes: String

    abstract fun handleImageExtend(
        userId: String,
        imageCode: String,
        marketImageRelRequest: MarketImageRelRequest
    )

    fun addMarketImage(
        userId: String,
        imageCode: String,
        marketImageRelRequest: MarketImageRelRequest,
        needAuth: Boolean = true
    ): Result<String> {
        logger.info("addMarketImage params:[$userId|$imageCode|$marketImageRelRequest|$needAuth]")
        // 判断镜像代码是否存在
        val codeCount = imageDao.countByCode(dslContext, imageCode)
        if (codeCount > 0) {
            // 抛出错误提示
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_EXIST,
                params = arrayOf(imageCode),
                data = null,
                language = I18nUtil.getLanguage(userId)
            )
        }
        val imageName = marketImageRelRequest.imageName
        // 判断镜像名称是否存在
        val nameCount = imageDao.countByName(dslContext, imageName)
        if (nameCount > 0) {
            // 抛出错误提示
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_EXIST,
                params = arrayOf(imageName),
                data = null,
                language = I18nUtil.getLanguage(userId)
            )
        }
        if (needAuth) {
            val projectCode = marketImageRelRequest.projectCode
            val validateFlag: Boolean?
            try {
                // 判断用户是否项目的成员
                validateFlag = client.get(ServiceProjectResource::class)
                    .verifyUserProjectPermission(projectCode = projectCode, userId = userId).data
            } catch (ignored: Throwable) {
                logger.warn("verifyUserProjectPermission error, params[$userId|$projectCode]", ignored)
                return I18nUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.SYSTEM_ERROR,
                    language = I18nUtil.getLanguage(userId)
                )
            }
            logger.info("verifyUserProjectPermission validateFlag is :$validateFlag")
            if (null == validateFlag || !validateFlag) {
                // 抛出错误提示
                return I18nUtil.generateResponseDataObject(
                    messageCode = IMAGE_ADD_NO_PROJECT_MEMBER,
                    params = arrayOf(projectCode),
                    language = I18nUtil.getLanguage(userId)
                )
            }
        }
        val imageId = addMarketImageToDB(userId, imageCode, marketImageRelRequest)
        handleImageExtend(userId, imageCode, marketImageRelRequest)
        return if (null != imageId) {
            Result(imageId)
        } else {
            Result("")
        }
    }

    fun addMarketImageToDB(
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
            // 初始化统计表数据
            storeStatisticTotalDao.initStatisticData(
                dslContext = context,
                storeCode = imageCode,
                storeType = StoreTypeEnum.IMAGE.type.toByte()
            )
        }
        return imageId
    }

    fun updateMarketImage(
        userId: String,
        marketImageUpdateRequest: MarketImageUpdateRequest,
        checkLatest: Boolean = false,
        sendCheckResultNotify: Boolean = true,
        runCheckPipeline: Boolean = true
    ): Result<String?> {
        logger.info("updateMarketImage params:[$userId|$marketImageUpdateRequest|$checkLatest|$sendCheckResultNotify]")
        if (marketImageUpdateRequest.category.equals(CATEGORY_PIPELINE_JOB) &&
            marketImageUpdateRequest.agentTypeScope.isEmpty()) {
            throw InvalidParamException(
                message = "agentTypeScope cannot be empty",
                params = arrayOf("agentTypeScope=[]")
            )
        }
        val imageCode = marketImageUpdateRequest.imageCode
        val imageTag = marketImageUpdateRequest.imageTag
        // 判断镜像tag是否为latest
        if (checkLatest && imageTag == LATEST) {
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(imageTag),
                language = I18nUtil.getLanguage(userId)
            )
        }
        val imageCount = imageDao.countByCode(dslContext, imageCode)
        if (imageCount < 1) {
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(imageCode),
                language = I18nUtil.getLanguage(userId)
            )
        }
        val imageName = marketImageUpdateRequest.imageName
        // 判断更新的名称是否已存在
        if (validateNameIsExist(imageCode, imageName)) return I18nUtil.generateResponseDataObject(
            messageCode = CommonMessageCode.PARAMETER_IS_EXIST,
            params = arrayOf(imageName),
            language = I18nUtil.getLanguage(userId)
        )
        val imageRecord = marketImageDao.getNewestImageByCode(dslContext, imageCode)!!
        val imageSourceType = marketImageUpdateRequest.imageSourceType
        val imageRepoName = marketImageUpdateRequest.imageRepoName
        if (imageSourceType == ImageType.BKDEVOPS) {
            // 判断用户发布的镜像是否是自已名下有权限操作的镜像
            val projectCode =
                storeProjectRelDao.getUserStoreTestProjectCode(dslContext, userId, imageCode, StoreTypeEnum.IMAGE)
                    ?: throw DataConsistencyException(
                        srcData = "(IMAGE,$userId,$imageCode)",
                        targetData = "TestProjectCode",
                        message = "Cannot find testproject record"
                    )
            val listProjectImagesResult = client.get(ServiceImageResource::class).listAllProjectImages(
                userId = userId,
                projectId = projectCode,
                searchKey = imageRepoName
            )
            if (listProjectImagesResult.isNotOk()) {
                return Result(listProjectImagesResult.status, listProjectImagesResult.message, null)
            }
            val projectImageListResp = listProjectImagesResult.data
            if ((null == projectImageListResp ||
                    projectImageListResp.imageList.map { it.repo }.contains(imageRepoName))) {
                // 查询是否上架的是公共镜像
                val listPublicImagesResult = client.get(ServiceImageResource::class).listAllPublicImages(
                    userId = userId,
                    searchKey = imageRepoName
                )
                logger.info("$imageRepoName listPublicImagesResult is :$listPublicImagesResult")
                if (listPublicImagesResult.isNotOk()) {
                    return Result(listPublicImagesResult.status, listPublicImagesResult.message, null)
                }
                val publicImageListResp = listPublicImagesResult.data
                if ((null == publicImageListResp ||
                        publicImageListResp.imageList.map { it.repo }.contains(imageRepoName))) {
                    return I18nUtil.generateResponseDataObject(
                        messageCode = IMAGE_PUBLISH_REPO_NO_PERMISSION,
                        language = I18nUtil.getLanguage(userId)
                    )
                }
            }
        }
        // 判断镜像的tag是否被关联过
        val relFlag = imageDao.countReleaseImageByTag(
            dslContext = dslContext,
            imageCode = imageCode,
            imageRepoUrl = marketImageUpdateRequest.imageRepoUrl,
            imageRepoName = imageRepoName,
            imageTag = imageTag
        ) == 0
        if (!relFlag) {
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_EXIST,
                params = arrayOf(imageTag),
                language = I18nUtil.getLanguage(userId)
            )
        }
        // 校验前端传的版本号是否正确
        val releaseType = marketImageUpdateRequest.releaseType
        val version = marketImageUpdateRequest.version
        val dbVersion = imageRecord.version
        val imageStatus = imageRecord.imageStatus
        // 判断镜像首个版本对应的请求是否合法
        if (releaseType == ReleaseTypeEnum.NEW && dbVersion == INIT_VERSION &&
            imageStatus != ImageStatusEnum.INIT.status.toByte()) {
            throw ErrorCodeException(errorCode = CommonMessageCode.ERROR_REST_EXCEPTION_COMMON_TIP)
        }
        // 最近的版本处于上架中止状态，重新升级版本号不变
        val cancelFlag = imageStatus == ImageStatusEnum.GROUNDING_SUSPENSION.status.toByte()
        val requireVersionList =
            if (cancelFlag && releaseType == ReleaseTypeEnum.CANCEL_RE_RELEASE) {
                listOf(dbVersion)
            } else {
                // 历史大版本下的小版本更新模式需获取要更新大版本下的最新版本
                val reqVersion = if (releaseType == ReleaseTypeEnum.HIS_VERSION_UPGRADE) {
                    imageDao.getImage(
                        dslContext = dslContext,
                        imageCode = imageCode,
                        version = VersionUtils.convertLatestVersion(version)
                    )?.version
                } else {
                    null
                }
                storeCommonService.getRequireVersion(
                    reqVersion = reqVersion,
                    dbVersion = dbVersion,
                    releaseType = releaseType
                )
            }
        if (!requireVersionList.contains(version)) {
            return I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_IMAGE_VERSION_IS_INVALID,
                params = arrayOf(version, requireVersionList.toString()),
                language = I18nUtil.getLanguage(userId)
            )
        }
        // 判断最近一个镜像版本的状态，如果不是首次发布，则只有处于审核驳回、已发布、上架中止和已下架的插件状态才允许添加新的版本
        val imageFinalStatusList = mutableListOf(
            ImageStatusEnum.AUDIT_REJECT.status.toByte(),
            ImageStatusEnum.RELEASED.status.toByte(),
            ImageStatusEnum.GROUNDING_SUSPENSION.status.toByte(),
            ImageStatusEnum.UNDERCARRIAGED.status.toByte()
        )
        if (imageCount == 1) {
            // 如果是首次发布，处于初始化的镜像状态也允许添加新的版本
            imageFinalStatusList.add(ImageStatusEnum.INIT.status.toByte())
        }
        if (!imageFinalStatusList.contains(imageStatus)) {
            return I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_IMAGE_VERSION_IS_NOT_FINISH,
                params = arrayOf(imageRecord.imageName, imageRecord.version),
                language = I18nUtil.getLanguage(userId)
            )
        }
        var imageId = UUIDUtil.generate()
        dslContext.transaction { t ->
            val context = DSL.using(t)
            if (imageRecord.version.isNullOrBlank() ||
                (cancelFlag && releaseType == ReleaseTypeEnum.CANCEL_RE_RELEASE)) {
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
            if (!labelIdList.isNullOrEmpty()) {
                imageLabelRelDao.batchAdd(context, userId, imageId, labelIdList)
            }
            if (runCheckPipeline) {
                // 运行检查镜像合法性的流水线
                runCheckImagePipeline(
                    context = context,
                    userId = userId,
                    imageId = imageId
                )
            } else {
                // 直接置为测试中状态
                marketImageDao.updateImageStatusById(
                    dslContext = context,
                    imageId = imageId,
                    imageStatus = ImageStatusEnum.TESTING.status.toByte(),
                    userId = userId,
                    msg = "no check"
                )
            }
        }
        return Result(imageId)
    }

    fun recheck(
        userId: String,
        imageId: String,
        validateUserFlag: Boolean = true
    ): Result<Boolean> {
        logger.info("recheck params:[$userId|$imageId|$validateUserFlag]")
        // 判断是否可以重新验证镜像
        val status = ImageStatusEnum.CHECKING.status.toByte()
        val (checkResult, code, params) = checkImageVersionOptRight(userId, imageId, status, validateUserFlag)
        if (!checkResult) {
            return I18nUtil.generateResponseDataObject(
                messageCode = code!!,
                params = params,
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        }
        runCheckImagePipeline(dslContext, userId, imageId)
        return Result(true)
    }

    abstract fun getPassTestStatus(isNormalUpgrade: Boolean): Byte

    fun passTest(
        userId: String,
        imageId: String,
        validateUserFlag: Boolean = true
    ): Result<Boolean> {
        logger.info("passTest params:[$userId|$imageId|$validateUserFlag]")
        val imageRecord = imageDao.getImage(dslContext, imageId)
            ?: return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(imageId),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        // 查看当前版本之前的版本是否有已发布的，如果有已发布的版本则只是普通的升级操作而不需要审核
        val imageCode = imageRecord.imageCode
        val isNormalUpgrade = getNormalUpgradeFlag(imageCode, imageRecord.imageStatus.toInt())
        logger.info("passTest isNormalUpgrade is:$isNormalUpgrade")
        val imageStatus = getPassTestStatus(isNormalUpgrade)
        val (checkResult, code, params) = checkImageVersionOptRight(
            userId = userId,
            imageId = imageId,
            status = imageStatus,
            validateUserFlag = validateUserFlag,
            isNormalUpgrade = isNormalUpgrade
        )
        if (!checkResult) {
            return I18nUtil.generateResponseDataObject(
                messageCode = code!!,
                params = params,
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        }
        if (isNormalUpgrade) {
            val imageFeature = imageFeatureDao.getImageFeature(dslContext, imageCode)
            // 自动通过
            approveImage(
                userId = userId,
                imageId = imageId,
                approveImageReq = ApproveImageReq(
                    imageCode = imageCode,
                    publicFlag = imageFeature.publicFlag ?: false,
                    recommendFlag = imageFeature.recommendFlag ?: true,
                    certificationFlag = imageFeature.certificationFlag ?: false,
                    rdType = ImageRDTypeEnum.getImageRDType(imageFeature.imageType.toInt()),
                    weight = imageFeature.weight ?: 0,
                    result = PASS,
                    message = "ok"
                )
            )
        } else {
            if (imageApproveSwitch == OPEN) {
                marketImageDao.updateImageStatusById(dslContext, imageId, imageStatus, userId, "")
            } else {
                // 自动通过
                approveImage(
                    userId = userId,
                    imageId = imageId,
                    approveImageReq = ApproveImageReq(
                        imageCode = imageCode,
                        publicFlag = false,
                        recommendFlag = true,
                        certificationFlag = false,
                        rdType = ImageRDTypeEnum.THIRD_PARTY,
                        weight = 1,
                        result = PASS,
                        message = "ok"
                    )
                )
            }
        }
        return Result(true)
    }

    fun recheckWithoutValidate(
        context: DSLContext,
        userId: String,
        imageId: String,
        sendCheckResultNotify: Boolean = true
    ) {
        runCheckImagePipeline(
            context = context,
            userId = userId,
            imageId = imageId
        )
    }

    private fun runCheckImagePipeline(
        context: DSLContext,
        userId: String,
        imageId: String
    ) {
        logger.info("runCheckImagePipeline params:[$userId|$imageId]")
        val imageRecord = imageDao.getImage(context, imageId)!!
        val imageCode = imageRecord.imageCode
        val version = imageRecord.version
        val imagePipelineRelRecord = storePipelineRelDao.getStorePipelineRel(context, imageCode, StoreTypeEnum.IMAGE)
        // 查找新增镜像时关联的项目
        val projectCode = storeProjectRelDao.getInitProjectCodeByStoreCode(
            dslContext = context,
            storeCode = imageCode,
            storeType = StoreTypeEnum.IMAGE.type.toByte()
        ) ?: throw ErrorCodeException(errorCode = CommonMessageCode.SYSTEM_ERROR)
        val ticketId = imageRecord.ticketId
        var userName: String? = null
        var password: String? = null
        if (!ticketId.isNullOrBlank()) {
            val pair = DHUtil.initKey()
            val encoder = Base64.getEncoder()
            val decoder = Base64.getDecoder()
            val credentialResult = client.get(ServiceCredentialResource::class).get(
                projectId = projectCode,
                credentialId = ticketId,
                publicKey = encoder.encodeToString(pair.publicKey),
                padding = true
            )
            if (credentialResult.isNotOk() || credentialResult.data == null) {
                throw ParamBlankException("Fail to get the credential($ticketId) of project($projectCode)")
            }
            val credential = credentialResult.data!!
            userName = String(
                DHUtil.decrypt(
                    decoder.decode(credential.v1),
                    decoder.decode(credential.publicKey),
                    pair.privateKey
                )
            )
            if (credential.v2 != null && credential.v2!!.isNotEmpty()) {
                password = String(
                    DHUtil.decrypt(
                        decoder.decode(credential.v2),
                        decoder.decode(credential.publicKey),
                        pair.privateKey
                    )
                )
            }
        }
        val dockerImageName = if (imageRecord.imageRepoUrl.isNullOrBlank()) {
            "${imageRecord.imageRepoName}:${imageRecord.imageTag}"
        } else {
            "${imageRecord.imageRepoUrl}/${imageRecord.imageRepoName}:${imageRecord.imageTag}"
        }
        val imageSourceType = imageRecord.imageSourceType
        val imageRepoUrl = imageRecord.imageRepoUrl
        val startParams = mutableMapOf<String, String>() // 启动参数
        startParams["imageCode"] = imageCode
        startParams["imageName"] = dockerImageName
        startParams["version"] = version
        imageSourceType?.let { startParams["imageType"] = it }
        userName?.let { startParams["registryUser"] = it }
        password?.let { startParams["registryPwd"] = it }
        imageRepoUrl?.let { startParams["registryHost"] = it }
        val pipelineId: String?
        if (null == imagePipelineRelRecord) {
            pipelineId = storePipelineService.creatStorePipelineByStoreCode(storeType = StoreTypeEnum.IMAGE.name)
            storePipelineRelDao.add(
                dslContext = context,
                storeCode = imageCode,
                storeType = StoreTypeEnum.IMAGE,
                pipelineId = pipelineId,
                projectCode = storeInnerPipelineConfig.innerPipelineProject
            )
        } else {
            pipelineId = imagePipelineRelRecord.pipelineId
        }
        // 触发执行流水线
        val startProjectCode = if (imagePipelineRelRecord == null) {
            storeInnerPipelineConfig.innerPipelineProject
        } else {
            imagePipelineRelRecord.projectCode
        }
        val buildIdObj = client.get(ServiceBuildResource::class).manualStartupNew(
            userId = if (imagePipelineRelRecord != null) userId else storeInnerPipelineConfig.innerPipelineUser,
            projectId = startProjectCode,
            pipelineId = pipelineId!!,
            values = startParams,
            channelCode = ChannelCode.AM,
            startType = StartType.SERVICE
        ).data
        logger.info("the buildIdObj is:$buildIdObj")
        if (null != buildIdObj) {
            storePipelineBuildRelDao.add(context, imageId, pipelineId, buildIdObj.id)
            marketImageDao.updateImageStatusById(
                dslContext = context,
                imageId = imageId,
                imageStatus = ImageStatusEnum.CHECKING.status.toByte(),
                userId = userId,
                msg = null
            ) // 验证中
        } else {
            marketImageDao.updateImageStatusById(
                dslContext = context,
                imageId = imageId,
                imageStatus = ImageStatusEnum.CHECK_FAIL.status.toByte(),
                userId = userId,
                msg = null
            ) // 验证失败
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
        imageCategoryRelDao.updateCategory(
            dslContext = context,
            userId = userId,
            imageId = imageId,
            categoryCode = marketImageUpdateRequest.category
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
        imageCategoryRelDao.updateCategory(
            dslContext = context,
            userId = userId,
            imageId = imageId,
            categoryCode = marketImageUpdateRequest.category
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
        logger.info("getProcessInfo params: [$userId|$imageId]")
        val record = imageDao.getImage(dslContext, imageId)
        if (null == record) {
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(imageId),
                language = I18nUtil.getLanguage(userId)
            )
        } else {
            val status = record.imageStatus.toInt()
            val imageCode = record.imageCode
            val isNormalUpgrade = if (imageApproveSwitch == CLOSE) {
                true
            } else {
                // 查看当前版本之前的版本是否有已发布的，如果有已发布的版本则只是普通的升级操作而不需要审核
                getNormalUpgradeFlag(imageCode, status)
            }
            logger.info("getProcessInfo isNormalUpgrade: $isNormalUpgrade")
            val processInfo = handleProcessInfo(isNormalUpgrade, status)

            val storeProcessInfo = storeCommonService.generateStoreProcessInfo(
                userId = userId,
                storeId = imageId,
                storeCode = imageCode,
                storeType = StoreTypeEnum.IMAGE,
                creator = record.creator,
                processInfo = processInfo
            )
            logger.info("getProcessInfo storeProcessInfo: $storeProcessInfo")
            return Result(storeProcessInfo)
        }
    }

    abstract fun handleProcessInfo(isNormalUpgrade: Boolean, status: Int): List<ReleaseProcessItem>

    /**
     * 取消发布
     */
    fun cancelRelease(userId: String, imageId: String): Result<Boolean> {
        logger.info("cancelRelease params:[$userId|$imageId]")
        val status = ImageStatusEnum.GROUNDING_SUSPENSION.status.toByte()
        // 判断用户是否有权限
        val (checkResult, code, params) = checkImageVersionOptRight(userId, imageId, status)
        if (!checkResult) {
            return I18nUtil.generateResponseDataObject(
                messageCode = code!!,
                params = params,
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        }
        marketImageDao.updateImageStatusById(dslContext, imageId, status, userId, "cancel release")
        return Result(true)
    }

    /**
     * 检查版本发布过程中的操作权限
     */
    protected fun checkImageVersionOptRight(
        userId: String,
        imageId: String,
        status: Byte,
        validateUserFlag: Boolean = true,
        isNormalUpgrade: Boolean? = null
    ): Triple<Boolean, String?, Array<String>?> {
        val imageRecord =
            imageDao.getImage(dslContext, imageId) ?: return Triple(
                false,
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf(imageId)
            )
        val imageCode = imageRecord.imageCode
        val creator = imageRecord.creator
        val imageStatus = imageRecord.imageStatus
        // 判断用户是否有权限(当前版本的创建者和管理员可以操作)
        if (!(storeMemberDao.isStoreAdmin(
                dslContext,
                userId,
                imageCode,
                StoreTypeEnum.IMAGE.type.toByte()
            ) || creator == userId || !validateUserFlag)
        ) {
            return Triple(false, NO_COMPONENT_ADMIN_PERMISSION, arrayOf(imageCode))
        }
        val allowReleaseStatus = getAllowReleaseStatus(isNormalUpgrade)
        var validateFlag = true
        if (status == ImageStatusEnum.COMMITTING.status.toByte() &&
            imageStatus != ImageStatusEnum.INIT.status.toByte()
        ) {
            validateFlag = false
        } else if (status == ImageStatusEnum.CHECKING.status.toByte() &&
            imageStatus !in (
                listOf(
                    ImageStatusEnum.COMMITTING.status.toByte(),
                    ImageStatusEnum.CHECK_FAIL.status.toByte(),
                    ImageStatusEnum.TESTING.status.toByte()
                ))
        ) {
            validateFlag = false
        } else if (status == ImageStatusEnum.CHECK_FAIL.status.toByte() &&
            imageStatus !in (
                listOf(
                    ImageStatusEnum.COMMITTING.status.toByte(),
                    ImageStatusEnum.CHECKING.status.toByte(),
                    ImageStatusEnum.CHECK_FAIL.status.toByte(),
                    ImageStatusEnum.TESTING.status.toByte()
                ))
        ) {
            validateFlag = false
        } else if (status == ImageStatusEnum.TESTING.status.toByte() &&
            imageStatus != ImageStatusEnum.CHECKING.status.toByte()
        ) {
            validateFlag = false
        } else if (status == ImageStatusEnum.AUDITING.status.toByte() &&
            imageStatus != ImageStatusEnum.TESTING.status.toByte()
        ) {
            validateFlag = false
        } else if (status == ImageStatusEnum.AUDIT_REJECT.status.toByte() &&
            imageStatus != ImageStatusEnum.AUDITING.status.toByte()
        ) {
            validateFlag = false
        } else if (status == ImageStatusEnum.RELEASED.status.toByte() &&
            imageStatus != allowReleaseStatus.status.toByte()
        ) {
            validateFlag = false
        } else if (status == ImageStatusEnum.GROUNDING_SUSPENSION.status.toByte() &&
            imageStatus == ImageStatusEnum.RELEASED.status.toByte()
        ) {
            validateFlag = false
        } else if (status == ImageStatusEnum.UNDERCARRIAGING.status.toByte() &&
            imageStatus == ImageStatusEnum.RELEASED.status.toByte()
        ) {
            validateFlag = false
        } else if (status == ImageStatusEnum.UNDERCARRIAGED.status.toByte() &&
            imageStatus !in (
                listOf(
                    ImageStatusEnum.UNDERCARRIAGING.status.toByte(),
                    ImageStatusEnum.RELEASED.status.toByte()
                ))
        ) {
            validateFlag = false
        }

        return if (validateFlag) Triple(true, null, null) else Triple(
            false,
            StoreMessageCode.USER_IMAGE_RELEASE_STEPS_ERROR,
            null
        )
    }

    abstract fun getAllowReleaseStatus(isNormalUpgrade: Boolean?): ImageStatusEnum

    private fun validateNameIsExist(
        imageCode: String,
        imageName: String
    ): Boolean {
        var flag = false
        val count = imageDao.countByName(dslContext, imageName)
        if (count > 0) {
            // 判断镜像名称是否重复（镜像升级允许名称一样）
            flag = imageDao.countByName(dslContext = dslContext, imageCode = imageCode, imageName = imageName) < count
        }
        return flag
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
            return I18nUtil.generateResponseDataObject(
                messageCode = NO_COMPONENT_ADMIN_PERMISSION,
                params = arrayOf(imageCode),
                language = I18nUtil.getLanguage(userId)
            )
        }
        if (!version.isNullOrEmpty()) {
            val imageRecord = imageDao.getImage(dslContext, validImageCode, validVersion!!)
                ?: return I18nUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                    params = arrayOf(validImageCode, validVersion),
                    data = false,
                    language = I18nUtil.getLanguage(userId)
                )
            if (ImageStatusEnum.RELEASED.status.toByte() != imageRecord.imageStatus) {
                return I18nUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                    params = arrayOf(validImageCode, validVersion),
                    data = false,
                    language = I18nUtil.getLanguage(userId)
                )
            }
            dslContext.transaction { t ->
                val context = DSL.using(t)
                val releaseImageRecords = marketImageDao.getReleaseImagesByCode(context, validImageCode)
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

    /**
     * 审核镜像
     */
    fun approveImageWithoutNotify(
        userId: String,
        imageId: String,
        approveImageReq: ApproveImageReq,
        checkCurrentStatus: Boolean = true
    ): AuditTypeEnum {
        // 参数校验
        // 判断镜像是否存在
        val image = imageDao.getImage(dslContext, imageId)
            ?: throw InvalidParamException(
                message = "imageId=$imageId is not valid",
                params = arrayOf(imageId)
            )
        val oldStatus = image.imageStatus
        val standardStatus = if (imageApproveSwitch == OPEN) {
            ImageStatusEnum.AUDITING.status.toByte() // 非待审核状态直接返回
        } else ImageStatusEnum.TESTING.status.toByte()
        if (checkCurrentStatus && oldStatus != standardStatus) {
            throw InvalidParamException(
                message = "imageId=$imageId is not in approving state",
                params = arrayOf(imageId)
            )
        }
        // 审核结果校验
        val approveResult = approveImageReq.result
        if (approveResult != PASS && approveResult != REJECT) {
            throw InvalidParamException(
                message = "approveResult=$approveResult is not valid,should be PASS/REJECT",
                params = arrayOf(approveResult)
            )
        }
        val imageStatus =
            if (approveResult == PASS) {
                ImageStatusEnum.RELEASED.status.toByte()
            } else {
                ImageStatusEnum.AUDIT_REJECT.status.toByte()
            }
        val type = if (approveResult == PASS) AuditTypeEnum.AUDIT_SUCCESS else AuditTypeEnum.AUDIT_REJECT
        val imageStatusMsg = approveImageReq.message
        dslContext.transaction { t ->
            val context = DSL.using(t)
            handleImageRelease(
                context = context,
                userId = userId,
                approveResult = approveResult,
                image = image,
                imageStatus = imageStatus,
                imageStatusMsg = imageStatusMsg,
                publicFlag = approveImageReq.publicFlag,
                recommendFlag = approveImageReq.recommendFlag,
                certificationFlag = approveImageReq.certificationFlag,
                rdType = approveImageReq.rdType,
                weight = approveImageReq.weight
            )
        }
        return type
    }

    fun approveImage(userId: String, imageId: String, approveImageReq: ApproveImageReq): Result<Boolean> {
        val type = approveImageWithoutNotify(
            userId = userId,
            imageId = imageId,
            approveImageReq = approveImageReq
        )
        // 通知发布者
        imageNotifyService.sendImageReleaseAuditNotifyMessage(imageId, type)
        return Result(true)
    }

    fun saveImageAgentTypeToFeature(
        context: DSLContext,
        imageCode: String,
        agentTypeList: List<ImageAgentTypeEnum>
    ) {
        imageAgentTypeDao.deleteAgentTypeByImageCode(context, imageCode)
        agentTypeList.forEach {
            imageAgentTypeDao.addAgentTypeByImageCode(context, imageCode, it)
        }
    }

    fun handleImageRelease(
        context: DSLContext,
        userId: String,
        approveResult: String,
        image: TImageRecord,
        imageStatus: Byte,
        imageStatusMsg: String,
        publicFlag: Boolean,
        recommendFlag: Boolean,
        certificationFlag: Boolean,
        rdType: ImageRDTypeEnum?,
        weight: Int?
    ) {
        val latestFlag = approveResult == PASS
        var pubTime: LocalDateTime? = null
        if (latestFlag) {
            // 清空旧版本LATEST_FLAG
            marketImageDao.cleanLatestFlag(context, image.imageCode)
            pubTime = LocalDateTime.now()
            // 记录发布信息
            storeReleaseDao.addStoreReleaseInfo(
                dslContext = context,
                userId = userId,
                storeReleaseCreateRequest = StoreReleaseCreateRequest(
                    storeCode = image.imageCode,
                    storeType = StoreTypeEnum.IMAGE,
                    latestUpgrader = image.modifier,
                    latestUpgradeTime = pubTime
                )
            )
            imageFeatureDao.update(
                dslContext = context,
                imageCode = image.imageCode,
                publicFlag = publicFlag,
                recommendFlag = recommendFlag,
                certificationFlag = certificationFlag,
                rdType = rdType,
                modifier = userId,
                weight = weight
            )
        }
        marketImageDao.updateImageStatusInfo(
            dslContext = context,
            imageId = image.id,
            imageStatus = imageStatus,
            imageStatusMsg = imageStatusMsg,
            latestFlag = latestFlag,
            pubTime = pubTime
        )
        saveImageAgentTypeToFeature(
            context,
            image.imageCode,
            JsonUtil.to(image.agentTypeScope, object : TypeReference<List<ImageAgentTypeEnum>>() {})
        )
    }

    fun getImageAgentTypes(userId: String): List<ImageAgentTypeInfo> {
        val types = imageAgentTypes.split(",")
        val imageAgentTypes = mutableListOf<ImageAgentTypeInfo>()
        types.forEach { type ->
            val buildType = BuildType.valueOf(type)
            val i18nTypeName = I18nUtil.getCodeLanMessage(
                messageCode = "${StoreMessageCode.MSG_CODE_BUILD_TYPE_PREFIX}${buildType.name}",
                defaultMessage = buildType.value,
                language = I18nUtil.getLanguage(userId)
            )
            imageAgentTypes.add(ImageAgentTypeInfo(buildType.name, i18nTypeName))
        }
        return imageAgentTypes
    }
}

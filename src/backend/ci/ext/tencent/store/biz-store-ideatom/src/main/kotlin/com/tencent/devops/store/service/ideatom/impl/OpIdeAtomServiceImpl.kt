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

package com.tencent.devops.store.service.ideatom.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.repository.api.ServiceGitRepositoryResource
import com.tencent.devops.repository.pojo.RepositoryInfo
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.repository.pojo.git.UpdateGitProjectInfo
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.common.ClassifyDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.dao.common.StoreReleaseDao
import com.tencent.devops.store.dao.ideatom.IdeAtomCategoryRelDao
import com.tencent.devops.store.dao.ideatom.IdeAtomDao
import com.tencent.devops.store.dao.ideatom.IdeAtomEnvInfoDao
import com.tencent.devops.store.dao.ideatom.IdeAtomLabelRelDao
import com.tencent.devops.store.dao.ideatom.MarketIdeAtomFeatureDao
import com.tencent.devops.store.dao.ideatom.MarketIdeAtomVersionLogDao
import com.tencent.devops.store.pojo.common.StoreReleaseCreateRequest
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreMemberTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.ideatom.IdeAtomBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.ideatom.IdeAtomCreateRequest
import com.tencent.devops.store.pojo.ideatom.IdeAtomEnvInfoCreateRequest
import com.tencent.devops.store.pojo.ideatom.IdeAtomFeatureRequest
import com.tencent.devops.store.pojo.ideatom.IdeAtomReleaseRequest
import com.tencent.devops.store.pojo.ideatom.IdeAtomUpdateRequest
import com.tencent.devops.store.pojo.ideatom.OpIdeAtomItem
import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomStatusEnum
import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomTypeEnum
import com.tencent.devops.store.service.common.StoreCommonService
import com.tencent.devops.store.service.ideatom.IdeAtomCategoryService
import com.tencent.devops.store.service.ideatom.OpIdeAtomService
import com.tencent.devops.store.utils.VersionUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class OpIdeAtomServiceImpl @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val storeMemberDao: StoreMemberDao,
    private val classifyDao: ClassifyDao,
    private val ideAtomCategoryRelDao: IdeAtomCategoryRelDao,
    private val ideAtomDao: IdeAtomDao,
    private val marketIdeAtomFeatureDao: MarketIdeAtomFeatureDao,
    private val ideAtomLabelRelDao: IdeAtomLabelRelDao,
    private val marketIdeAtomVersionLogDao: MarketIdeAtomVersionLogDao,
    private val ideAtomEnvInfoDao: IdeAtomEnvInfoDao,
    private val storeReleaseDao: StoreReleaseDao,
    private val atomCategoryService: IdeAtomCategoryService,
    private val storeCommonService: StoreCommonService
) : OpIdeAtomService {

    private val logger = LoggerFactory.getLogger(OpIdeAtomServiceImpl::class.java)

    @Value("\${git.idePlugin.nameSpaceId}")
    private lateinit var idePluginNameSpaceId: String

    override fun addIdeAtom(userId: String, ideAtomCreateRequest: IdeAtomCreateRequest): Result<Boolean> {
        logger.info("addIdeAtom userId is :$userId , ideAtomCreateRequest is :$ideAtomCreateRequest")
        val atomCode = ideAtomCreateRequest.atomCode
        val atomCount = ideAtomDao.countByCode(dslContext, atomCode)
        val addIdeAtomResult = if (atomCount < 1) {
            // 首次创建版本
            createIdeAtom(userId, atomCode, ideAtomCreateRequest)
        } else {
            // 升级版本
            upgradeIdeAtom(userId, atomCode, ideAtomCreateRequest)
        }
        logger.info("the addIdeAtomResult is :$addIdeAtomResult")
        if (addIdeAtomResult.isNotOk()) {
            return addIdeAtomResult
        }
        return Result(true)
    }

    override fun updateIdeAtom(
        userId: String,
        atomId: String,
        ideAtomUpdateRequest: IdeAtomUpdateRequest
    ): Result<Boolean> {
        logger.info("updateIdeAtom userId is :$userId , ideAtomUpdateRequest is :$ideAtomUpdateRequest")
        val atomRecord = ideAtomDao.getIdeAtomById(dslContext, atomId)
            ?: return MessageUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(atomId),language = I18nUtil.getLanguage(userId)
            )
        val atomCode = atomRecord.atomCode
        val atomName = ideAtomUpdateRequest.atomName
        if (null != atomName) {
            // 判断更新的名称是否已存在
            if (validateNameIsExist(atomCode, atomName)) return MessageUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_EXIST,
                params = arrayOf(atomName),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        }
        // 更新git代码库信息
        val visibilityLevel = ideAtomUpdateRequest.visibilityLevel
        val dbVisibilityLevel = atomRecord.visibilityLevel
        if (null != visibilityLevel && visibilityLevel.level != dbVisibilityLevel) {
            val changeResult = changeGitRepositoryVisibility(
                atomCode = atomRecord.atomCode,
                userId = userId,
                visibilityLevel = visibilityLevel
            )
            if (changeResult.isNotOk()) {
                return changeResult
            }
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            ideAtomDao.updateAtomFromOp(context, userId, atomId, ideAtomUpdateRequest)
            // 更新IDE插件特性信息
            marketIdeAtomFeatureDao.updateIdeAtomFeature(
                dslContext = context,
                userId = userId,
                atomFeatureRequest = IdeAtomFeatureRequest(
                    atomCode = atomRecord.atomCode,
                    atomType = ideAtomUpdateRequest.atomType,
                    publicFlag = ideAtomUpdateRequest.publicFlag,
                    recommendFlag = ideAtomUpdateRequest.recommendFlag,
                    weight = ideAtomUpdateRequest.weight
                )
            )
            val versionContent = ideAtomUpdateRequest.versionContent
            if (null != versionContent) {
                // 更新版本日志信息
                marketIdeAtomVersionLogDao.updateMarketIdeAtomVersion(dslContext, userId, atomId, versionContent)
            }
            // 更新标签信息
            val labelIdList = ideAtomUpdateRequest.labelIdList
            if (null != labelIdList) {
                ideAtomLabelRelDao.deleteByAtomId(context, atomId)
                if (labelIdList.isNotEmpty())
                    ideAtomLabelRelDao.batchAdd(context, userId, atomId, labelIdList)
            }
            // 更新范畴信息
            val categoryIdList = ideAtomUpdateRequest.categoryIdList
            if (null != categoryIdList) {
                ideAtomCategoryRelDao.deleteByIdeAtomId(context, atomId)
                if (categoryIdList.isNotEmpty())
                    ideAtomCategoryRelDao.batchAdd(context, userId, atomId, categoryIdList)
            }
        }
        return Result(true)
    }

    override fun deleteIdeAtomById(atomId: String): Result<Boolean> {
        logger.info("deleteIdeAtomById atomId is :$atomId")
        val atomRecord = ideAtomDao.getIdeAtomById(dslContext, atomId)
            ?: return MessageUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(atomId),
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
        val atomCode = atomRecord.atomCode
        val releasedCount = ideAtomDao.countReleaseAtomById(dslContext, atomId)
        logger.info("releasedCount: $releasedCount")
        if (releasedCount > 0) {
            return MessageUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_ATOM_RELEASED_IS_NOT_ALLOW_DELETE,
                params = arrayOf(atomCode),
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            val atomRecords = ideAtomDao.getIdeAtomsByAtomCode(dslContext, atomRecord.atomCode)
            if (null == atomRecords || atomRecords.isEmpty()) {
                storeMemberDao.deleteAll(context, atomCode, StoreTypeEnum.IDE_ATOM.type.toByte())
            }
            ideAtomDao.deleteIdeAtomById(context, atomId)
        }
        return Result(true)
    }

    override fun getIdeAtomVersionsByCode(
        atomCode: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<OpIdeAtomItem>?> {
        val atomRecords = ideAtomDao.getIdeAtomsByAtomCode(dslContext, atomCode)
        val opIdeAtomItemList = mutableListOf<OpIdeAtomItem>()
        atomRecords?.forEach {
            val atomFeatureRecord = marketIdeAtomFeatureDao.getIdeAtomFeature(dslContext, atomCode)!!
            val classifyRecord = classifyDao.getClassify(dslContext, it.classifyId)
            val classifyCode = classifyRecord?.classifyCode
            val classifyName = classifyRecord?.classifyName
            val classifyLanName = I18nUtil.getCodeLanMessage(
                messageCode = "${StoreMessageCode.MSG_CODE_STORE_CLASSIFY_PREFIX}$classifyCode",
                defaultMessage = classifyName
            )
            val atomEnvInfoRecord = ideAtomEnvInfoDao.getIdeAtomEnvInfo(dslContext, it.id)
            opIdeAtomItemList.add(
                OpIdeAtomItem(
                    atomId = it.id,
                    atomName = it.atomName,
                    atomCode = it.atomCode,
                    atomType = IdeAtomTypeEnum.getAtomTypeObj(atomFeatureRecord.atomType.toInt()),
                    atomVersion = it.version,
                    atomStatus = IdeAtomStatusEnum.getIdeAtomStatusObj(it.atomStatus.toInt())!!,
                    classifyCode = classifyCode,
                    classifyName = classifyLanName,
                    categoryList = atomCategoryService.getCategorysByAtomId(it.id).data,
                    publisher = it.publisher,
                    pubTime = if (null != it.pubTime) DateTimeUtil.toDateTime(it.pubTime) else null,
                    latestFlag = it.latestFlag,
                    publicFlag = atomFeatureRecord.publicFlag,
                    recommendFlag = atomFeatureRecord.recommendFlag,
                    weight = atomFeatureRecord.weight,
                    pkgName = atomEnvInfoRecord?.pkgPath?.replace("${it.atomCode}/${it.version}/", ""),
                    creator = it.creator,
                    createTime = DateTimeUtil.toDateTime(it.createTime),
                    modifier = it.modifier,
                    updateTime = DateTimeUtil.toDateTime(it.updateTime)
                )
            )
        }
        val atomCount = ideAtomDao.countByCode(dslContext, atomCode)
        val totalPages = PageUtil.calTotalPage(pageSize, atomCount)
        return Result(
            data = Page(
                count = atomCount,
                page = page ?: 1,
                pageSize = pageSize ?: -1,
                totalPages = totalPages,
                records = opIdeAtomItemList
            )
        )
    }

    override fun listIdeAtoms(
        atomName: String?,
        atomType: IdeAtomTypeEnum?,
        classifyCode: String?,
        categoryCodes: String?,
        labelCodes: String?,
        processFlag: Boolean?,
        page: Int,
        pageSize: Int
    ): Result<Page<OpIdeAtomItem>?> {
        logger.info("listIdeAtoms params:[$atomName|$atomType|$classifyCode|$categoryCodes|$labelCodes|$processFlag")
        val labelCodeList = if (labelCodes.isNullOrEmpty()) null else labelCodes.split(",")
        val categoryCodeList = if (categoryCodes.isNullOrEmpty()) null else categoryCodes.split(",")
        val atomRecords = ideAtomDao.listOpIdeAtoms(
            dslContext = dslContext,
            atomName = atomName,
            atomType = atomType,
            classifyCode = classifyCode,
            categoryCodeList = categoryCodeList,
            labelCodeList = labelCodeList,
            processFlag = processFlag,
            page = page,
            pageSize = pageSize
        )
        val opIdeAtomItemList = mutableListOf<OpIdeAtomItem>()
        atomRecords?.forEach {
            val atomId = it["atomId"] as String
            val atomCode = it["atomCode"] as String
            val atomVersion = it["atomVersion"] as String
            val atomEnvInfoRecord = ideAtomEnvInfoDao.getIdeAtomEnvInfo(dslContext, atomId)
            val atomClassifyCode = it["classifyCode"] as? String
            val classifyName = it["classifyName"] as? String
            val classifyLanName = I18nUtil.getCodeLanMessage(
                messageCode = "${StoreMessageCode.MSG_CODE_STORE_CLASSIFY_PREFIX}$atomClassifyCode",
                defaultMessage = classifyName
            )
            val opIdeAtomItem = OpIdeAtomItem(
                atomId = atomId,
                atomName = it["atomName"] as String,
                atomCode = it["atomCode"] as String,
                atomType = if (null != it["atomType"]) IdeAtomTypeEnum.getAtomTypeObj((it["atomType"] as Byte).toInt()) else null,
                atomVersion = it["atomVersion"] as String,
                atomStatus = IdeAtomStatusEnum.getIdeAtomStatusObj((it["atomStatus"] as Byte).toInt())!!,
                classifyCode = atomClassifyCode,
                classifyName = classifyLanName,
                categoryList = atomCategoryService.getCategorysByAtomId(atomId).data,
                publisher = it["publisher"] as String,
                pubTime = if (null != it["pubTime"]) DateTimeUtil.toDateTime(it["pubTime"] as LocalDateTime) else null,
                latestFlag = it["latestFlag"] as Boolean,
                publicFlag = it["publicFlag"] as? Boolean,
                recommendFlag = it["recommendFlag"] as? Boolean,
                weight = it["weight"] as? Int,
                pkgName = atomEnvInfoRecord?.pkgPath?.replace("$atomCode/$atomVersion/", ""),
                creator = it["creator"] as String,
                createTime = DateTimeUtil.toDateTime(it["createTime"] as LocalDateTime),
                modifier = it["modifier"] as String,
                updateTime = DateTimeUtil.toDateTime(it["updateTime"] as LocalDateTime)
            )
            val newestAtomRecord = ideAtomDao.getNewestAtomByCode(dslContext, atomCode)!!
            // 查询插件最新的一条记录是否需要管理员介入
            val atomFinalStatusList = listOf(
                IdeAtomStatusEnum.INIT.status.toByte(),
                IdeAtomStatusEnum.AUDITING.status.toByte()
            )
            if (atomFinalStatusList.contains(newestAtomRecord.atomStatus)) {
                opIdeAtomItem.opAtomId = newestAtomRecord.id
                opIdeAtomItem.opAtomStatus = IdeAtomStatusEnum.getIdeAtomStatusObj(newestAtomRecord.atomStatus.toInt())
                opIdeAtomItem.opAtomVersion = newestAtomRecord.version
            }
            opIdeAtomItemList.add(
                opIdeAtomItem
            )
        }
        val atomCount = ideAtomDao.countOpIdeAtom(
            dslContext = dslContext,
            atomName = atomName,
            atomType = atomType,
            classifyCode = classifyCode,
            categoryCodeList = categoryCodeList,
            labelCodeList = labelCodeList,
            processFlag = processFlag
        )
        val totalPages = PageUtil.calTotalPage(pageSize, atomCount)
        return Result(
            Page(
                count = atomCount,
                page = page,
                pageSize = pageSize,
                totalPages = totalPages,
                records = opIdeAtomItemList
            )
        )
    }

    override fun releaseIdeAtom(
        userId: String,
        atomId: String,
        ideAtomReleaseRequest: IdeAtomReleaseRequest
    ): Result<Boolean> {
        logger.info("releaseIdeAtom userId:$userId,atomId:$atomId,ideAtomReleaseRequest:$ideAtomReleaseRequest")
        val atomRecord = ideAtomDao.getIdeAtomById(dslContext, atomId)
            ?: return MessageUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(atomId),
                language = I18nUtil.getLanguage(userId)
            )
        // 判断插件是否可以发布
        val atomFinalStatusList = listOf(
            IdeAtomStatusEnum.INIT.status.toByte(),
            IdeAtomStatusEnum.AUDITING.status.toByte()
        )
        if (!atomFinalStatusList.contains(atomRecord.atomStatus)) {
            return MessageUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_ATOM_RELEASE_STEPS_ERROR,
                language = I18nUtil.getLanguage(userId))
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            // 清空旧版本LATEST_FLAG
            ideAtomDao.cleanLatestFlag(context, atomRecord.atomCode)
            // 把当前版本LATEST_FLAG置为true
            ideAtomDao.updateAtomLatestFlag(context, userId, atomId, true)
            val pubTime = LocalDateTime.now()
            // 把IDE插件置为上架状态
            ideAtomDao.updateAtomBaseInfoById(
                dslContext = dslContext,
                atomId = atomRecord.id,
                userId = userId,
                ideAtomBaseInfoUpdateRequest = IdeAtomBaseInfoUpdateRequest(
                    atomStatus = IdeAtomStatusEnum.RELEASED,
                    pubTime = pubTime
                )
            )
            // 记录发布信息
            storeReleaseDao.addStoreReleaseInfo(
                dslContext = context,
                userId = userId,
                storeReleaseCreateRequest = StoreReleaseCreateRequest(
                    storeCode = atomRecord.atomCode,
                    storeType = StoreTypeEnum.IDE_ATOM,
                    latestUpgrader = atomRecord.creator,
                    latestUpgradeTime = pubTime
                )
            )
            // 更新IDE插件特性信息
            marketIdeAtomFeatureDao.updateIdeAtomFeature(
                dslContext = context,
                userId = userId,
                atomFeatureRequest = IdeAtomFeatureRequest(
                    atomCode = atomRecord.atomCode,
                    atomType = ideAtomReleaseRequest.atomType,
                    publicFlag = ideAtomReleaseRequest.publicFlag,
                    recommendFlag = ideAtomReleaseRequest.recommendFlag,
                    weight = ideAtomReleaseRequest.weight
                )
            )
            // 添加插件环境信息
            ideAtomEnvInfoDao.addIdeAtomEnvInfo(
                dslContext = context,
                userId = userId,
                ideAtomEnvInfoCreateRequest = IdeAtomEnvInfoCreateRequest(
                    atomId = atomRecord.id,
                    pkgPath = "${atomRecord.atomCode}/${atomRecord.version}/${ideAtomReleaseRequest.pkgName}"
                )
            )
        }
        return Result(true)
    }

    override fun offlineIdeAtom(
        userId: String,
        atomCode: String,
        version: String?,
        reason: String?
    ): Result<Boolean> {
        logger.info("offlineIdeAtom userId is :$userId,atomCode is :$atomCode,version is :$version,reason is :$reason")
        if (!version.isNullOrEmpty()) {
            val atomRecord = ideAtomDao.getIdeAtom(dslContext, atomCode, version.trim())
                ?: return MessageUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                    params = arrayOf("$atomCode:$version"),
                    data = false,
                    language = I18nUtil.getLanguage(userId)
                )
            if (IdeAtomStatusEnum.RELEASED.status.toByte() != atomRecord.atomStatus) {
                return MessageUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.PERMISSION_DENIED,
                    language = I18nUtil.getLanguage(userId))
            }
            dslContext.transaction { t ->
                val context = DSL.using(t)
                val releaseAtomRecords = ideAtomDao.getReleaseAtomsByCode(context, atomCode)
                if (null != releaseAtomRecords && releaseAtomRecords.size > 0) {
                    ideAtomDao.updateAtomBaseInfoById(
                        dslContext = context,
                        atomId = atomRecord.id,
                        userId = userId,
                        ideAtomBaseInfoUpdateRequest = IdeAtomBaseInfoUpdateRequest(
                            atomStatus = IdeAtomStatusEnum.UNDERCARRIAGED,
                            atomStatusMsg = reason,
                            latestFlag = false
                        )
                    )
                    val newestReleaseAtomRecord = releaseAtomRecords[0]
                    if (newestReleaseAtomRecord.id == atomRecord.id) {
                        var atomId: String? = null
                        if (releaseAtomRecords.size == 1) {
                            val newestUndercarriagedAtom =
                                ideAtomDao.getNewestUndercarriagedAtomsByCode(context, atomCode)
                            if (null != newestUndercarriagedAtom) {
                                atomId = newestUndercarriagedAtom.id
                            }
                        } else {
                            // 把前一个发布的版本的latestFlag置为true
                            val tmpAtomRecord = releaseAtomRecords[1]
                            atomId = tmpAtomRecord.id
                        }
                        if (null != atomId) {
                            ideAtomDao.updateAtomBaseInfoById(
                                dslContext = context,
                                atomId = atomId,
                                userId = userId,
                                ideAtomBaseInfoUpdateRequest = IdeAtomBaseInfoUpdateRequest(
                                    latestFlag = true
                                )
                            )
                        }
                    }
                }
            }
        } else {
            // 把IDE插件所有已发布的版本全部下架
            dslContext.transaction { t ->
                val context = DSL.using(t)
                ideAtomDao.updateAtomStatusByCode(
                    dslContext = context,
                    atomCode = atomCode,
                    latestFlag = false,
                    atomOldStatus = IdeAtomStatusEnum.RELEASED.status.toByte(),
                    atomNewStatus = IdeAtomStatusEnum.UNDERCARRIAGED.status.toByte(),
                    userId = userId,
                    msg = "undercarriage"
                )
                val newestUndercarriagedAtom = ideAtomDao.getNewestUndercarriagedAtomsByCode(context, atomCode)
                if (null != newestUndercarriagedAtom) {
                    // 把发布时间最晚的下架版本latestFlag置为true
                    ideAtomDao.updateAtomBaseInfoById(
                        dslContext = context,
                        atomId = newestUndercarriagedAtom.id,
                        userId = userId,
                        ideAtomBaseInfoUpdateRequest = IdeAtomBaseInfoUpdateRequest(
                            latestFlag = true
                        )
                    )
                }
            }
        }
        return Result(true)
    }

    private fun upgradeIdeAtom(
        userId: String,
        atomCode: String,
        ideAtomCreateRequest: IdeAtomCreateRequest
    ): Result<Boolean> {
        val atomName = ideAtomCreateRequest.atomName
        // 判断更新的名称是否已存在
        if (validateNameIsExist(atomCode, atomName)) return MessageUtil.generateResponseDataObject(
            messageCode = CommonMessageCode.PARAMETER_IS_EXIST,
            params = arrayOf(atomName),
            language = I18nUtil.getLanguage(userId)
        )
        val atomRecord = ideAtomDao.getNewestAtomByCode(dslContext, atomCode)
            ?: return MessageUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(atomCode),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        // 校验前端传的版本号是否正确
        val releaseType = ideAtomCreateRequest.releaseType
        val version = ideAtomCreateRequest.version
        val dbVersion = atomRecord.version
        // 最近的版本处于上架中止状态，重新升级版本号不变
        val cancelFlag = atomRecord.atomStatus == IdeAtomStatusEnum.GROUNDING_SUSPENSION.status.toByte()
        val requireVersionList =
            if (cancelFlag && releaseType == ReleaseTypeEnum.CANCEL_RE_RELEASE) {
                listOf(dbVersion)
            } else {
                // 历史大版本下的小版本更新模式需获取要更新大版本下的最新版本
                val reqVersion = if (releaseType == ReleaseTypeEnum.HIS_VERSION_UPGRADE) {
                    ideAtomDao.getIdeAtom(
                        dslContext = dslContext,
                        atomCode = atomRecord.atomCode,
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
            return MessageUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_ATOM_VERSION_IS_INVALID,
                params = arrayOf(version, requireVersionList.toString()),
                language = I18nUtil.getLanguage(userId)
            )
        }
        // 判断最近一个IDE插件版本的状态，只有处于审核驳回、已发布、上架中止和已下架的状态才允许添加新的版本
        val atomFinalStatusList = listOf(
            IdeAtomStatusEnum.AUDIT_REJECT.status.toByte(),
            IdeAtomStatusEnum.RELEASED.status.toByte(),
            IdeAtomStatusEnum.GROUNDING_SUSPENSION.status.toByte(),
            IdeAtomStatusEnum.UNDERCARRIAGED.status.toByte()
        )
        if (!atomFinalStatusList.contains(atomRecord.atomStatus)) {
            return MessageUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_ATOM_VERSION_IS_NOT_FINISH,
                params = arrayOf(atomRecord.atomName, atomRecord.version),
                language = I18nUtil.getLanguage(userId)
            )
        }
        // 更新git代码库信息
        val visibilityLevel = ideAtomCreateRequest.visibilityLevel
        val dbVisibilityLevel = atomRecord.visibilityLevel
        if (visibilityLevel.level != dbVisibilityLevel) {
            val changeResult = changeGitRepositoryVisibility(atomCode, userId, visibilityLevel)
            if (changeResult.isNotOk()) {
                return changeResult
            }
        }
        val atomId = UUIDUtil.generate()
        val finalReleaseType = if (releaseType == ReleaseTypeEnum.CANCEL_RE_RELEASE) {
            val imageVersion = marketIdeAtomVersionLogDao.getIdeAtomVersion(dslContext, atomId)
            imageVersion.releaseType
        } else {
            releaseType.releaseType.toByte()
        }
        logger.info("finalReleaseType is $finalReleaseType")
        upgradeIdeAtom(userId, ideAtomCreateRequest, finalReleaseType)
        return Result(true)
    }

    private fun changeGitRepositoryVisibility(
        atomCode: String,
        userId: String,
        visibilityLevel: VisibilityLevelEnum
    ): Result<Boolean> {
        // 更新git代码库可见范围
        val updateGitRepositoryResult: Result<Boolean>
        val atomFeatureRecord = marketIdeAtomFeatureDao.getIdeAtomFeature(dslContext, atomCode)!!
        try {
            updateGitRepositoryResult =
                client.get(ServiceGitRepositoryResource::class).updateGitCodeRepositoryByProjectName(
                    userId = userId,
                    projectName = atomFeatureRecord.namespacePath,
                    updateGitProjectInfo = UpdateGitProjectInfo(
                        visibilityLevel = visibilityLevel.level,
                        forkEnabled = visibilityLevel == VisibilityLevelEnum.LOGIN_PUBLIC
                    ),
                    tokenType = TokenTypeEnum.PRIVATE_KEY
                )
        } catch (ignored: Throwable) {
            logger.warn("atom[$atomCode] updateGitCodeRepository fail", ignored)
            return MessageUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.SYSTEM_ERROR,
                language = I18nUtil.getLanguage(userId))
        }
        logger.info("atom[$atomCode] updateGitRepositoryResult is :$updateGitRepositoryResult")
        if (updateGitRepositoryResult.isNotOk()) {
            return Result(updateGitRepositoryResult.status, updateGitRepositoryResult.message
                ?: "") // 工蜂更新代码信息失败则把报错信息返回给前端
        }
        return Result(true)
    }

    private fun createIdeAtom(
        userId: String,
        atomCode: String,
        ideAtomCreateRequest: IdeAtomCreateRequest
    ): Result<Boolean> {
        logger.info("createIdeAtom params:[$userId|$atomCode|$ideAtomCreateRequest]")
        // 判断插件代码是否存在
        val codeCount = ideAtomDao.countByCode(dslContext, atomCode)
        if (codeCount > 0) {
            // 抛出错误提示
            return MessageUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_EXIST,
                params = arrayOf(atomCode),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        }
        val atomName = ideAtomCreateRequest.atomName
        // 判断插件分类名称是否存在
        val nameCount = ideAtomDao.countByName(dslContext, atomName)
        if (nameCount > 0) {
            // 抛出错误提示
            return MessageUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_EXIST,
                params = arrayOf(atomName),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        }
        // 远程调工蜂接口创建代码库
        val repositoryInfo: RepositoryInfo?
        try {
            val createGitRepositoryResult = client.get(ServiceGitRepositoryResource::class).createGitCodeRepository(
                userId = userId,
                projectCode = null,
                repositoryName = atomCode,
                sampleProjectPath = null,
                namespaceId = idePluginNameSpaceId.toInt(),
                visibilityLevel = ideAtomCreateRequest.visibilityLevel,
                tokenType = TokenTypeEnum.PRIVATE_KEY,
                frontendType = null
            )
            logger.info("the createGitRepositoryResult is :$createGitRepositoryResult")
            if (createGitRepositoryResult.isOk()) {
                repositoryInfo = createGitRepositoryResult.data
            } else {
                return Result(createGitRepositoryResult.status, createGitRepositoryResult.message, false)
            }
        } catch (ignored: Throwable) {
            logger.warn("atom[$atomCode] createGitCodeRepository fail", ignored)
            return MessageUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_CREATE_REPOSITORY_FAIL,
                data = false,
                language = I18nUtil.getLanguage(userId))
        }
        if (null == repositoryInfo) {
            // 创建代码库失败抛出错误提示
            return MessageUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_CREATE_REPOSITORY_FAIL,
                data = false,
                language = I18nUtil.getLanguage(userId))
        }
        saveIdeAtom(userId, ideAtomCreateRequest, atomCode, repositoryInfo)
        return Result(true)
    }

    private fun saveIdeAtom(
        userId: String,
        ideAtomCreateRequest: IdeAtomCreateRequest,
        atomCode: String,
        repositoryInfo: RepositoryInfo
    ) {
        val atomId = UUIDUtil.generate()
        dslContext.transaction { t ->
            val context = DSL.using(t)
            ideAtomDao.addIdeAtomFromOp(context, userId, atomId, true, ideAtomCreateRequest)
            // 默认给新建IDE插件的人赋予管理员权限
            storeMemberDao.addStoreMember(
                dslContext = context,
                userId = userId,
                storeCode = atomCode,
                userName = userId,
                type = StoreMemberTypeEnum.ADMIN.type.toByte(),
                storeType = StoreTypeEnum.IDE_ATOM.type.toByte()
            )
            // 添加IDE插件特性信息
            marketIdeAtomFeatureDao.addIdeAtomFeature(
                dslContext = context,
                userId = userId,
                atomFeatureRequest = IdeAtomFeatureRequest(
                    atomCode = atomCode,
                    codeSrc = repositoryInfo.url,
                    nameSpacePath = repositoryInfo.aliasName
                ))
            // 添加版本日志信息
            marketIdeAtomVersionLogDao.addMarketIdeAtomVersion(
                dslContext = context,
                userId = userId,
                atomId = atomId,
                releaseType = ideAtomCreateRequest.releaseType.releaseType.toByte(),
                versionContent = ideAtomCreateRequest.versionContent
            )
            // 更新标签信息
            val labelIdList = ideAtomCreateRequest.labelIdList
            if (null != labelIdList) {
                ideAtomLabelRelDao.deleteByAtomId(context, atomId)
                if (labelIdList.isNotEmpty())
                ideAtomLabelRelDao.batchAdd(context, userId, atomId, labelIdList)
            }
            // 更新范畴信息
            ideAtomCategoryRelDao.deleteByIdeAtomId(context, atomId)
            ideAtomCategoryRelDao.batchAdd(context, userId, atomId, ideAtomCreateRequest.categoryIdList)
        }
    }

    private fun upgradeIdeAtom(
        userId: String,
        ideAtomCreateRequest: IdeAtomCreateRequest,
        releaseType: Byte
    ) {
        val atomId = UUIDUtil.generate()
        dslContext.transaction { t ->
            val context = DSL.using(t)
            ideAtomDao.addIdeAtomFromOp(context, userId, atomId, false, ideAtomCreateRequest)
            // 添加版本日志信息
            marketIdeAtomVersionLogDao.addMarketIdeAtomVersion(
                dslContext = context,
                userId = userId,
                atomId = atomId,
                releaseType = releaseType,
                versionContent = ideAtomCreateRequest.versionContent
            )
            // 更新标签信息
            val labelIdList = ideAtomCreateRequest.labelIdList
            if (null != labelIdList) {
                ideAtomLabelRelDao.deleteByAtomId(context, atomId)
                if (labelIdList.isNotEmpty())
                ideAtomLabelRelDao.batchAdd(context, userId, atomId, labelIdList)
            }
            // 更新范畴信息
            ideAtomCategoryRelDao.deleteByIdeAtomId(context, atomId)
            ideAtomCategoryRelDao.batchAdd(context, userId, atomId, ideAtomCreateRequest.categoryIdList)
        }
    }

    private fun validateNameIsExist(
        atomCode: String,
        atomName: String
    ): Boolean {
        var flag = false
        val count = ideAtomDao.countByName(dslContext, atomName)
        if (count > 0) {
            // 判断IDE插件名称是否重复（IDE插件升级允许名称一样）
            flag = ideAtomDao.countByName(dslContext = dslContext, atomName = atomName, atomCode = atomCode) < count
        }
        return flag
    }
}

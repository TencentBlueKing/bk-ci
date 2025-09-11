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

package com.tencent.devops.store.atom.service.impl

import com.fasterxml.jackson.core.JsonProcessingException
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.devops.artifactory.pojo.ArchiveAtomRequest
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.INIT_VERSION
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.util.ThreadPoolUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.store.tables.TAtom
import com.tencent.devops.model.store.tables.records.TAtomRecord
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.atom.dao.AtomDao
import com.tencent.devops.store.atom.dao.MarketAtomDao
import com.tencent.devops.store.atom.dao.MarketAtomFeatureDao
import com.tencent.devops.store.atom.dao.MarketAtomVersionLogDao
import com.tencent.devops.store.atom.service.AtomNotifyService
import com.tencent.devops.store.atom.service.AtomQualityService
import com.tencent.devops.store.atom.service.AtomReleaseService
import com.tencent.devops.store.atom.service.MarketAtomService
import com.tencent.devops.store.atom.service.OpAtomService
import com.tencent.devops.store.common.dao.LabelDao
import com.tencent.devops.store.common.service.ClassifyService
import com.tencent.devops.store.common.service.StoreFileService
import com.tencent.devops.store.common.service.StoreI18nMessageService
import com.tencent.devops.store.common.service.StoreLogoService
import com.tencent.devops.store.common.service.StoreWebsocketService
import com.tencent.devops.store.common.service.action.StoreDecorateFactory
import com.tencent.devops.store.common.utils.StoreFileAnalysisUtil
import com.tencent.devops.store.common.utils.StoreUtils
import com.tencent.devops.store.common.utils.VersionUtils
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.constant.StoreMessageCode.USER_UPLOAD_FILE_PATH_ERROR
import com.tencent.devops.store.constant.StoreMessageCode.USER_UPLOAD_PACKAGE_INVALID
import com.tencent.devops.store.pojo.atom.ApproveReq
import com.tencent.devops.store.pojo.atom.Atom
import com.tencent.devops.store.pojo.atom.AtomFeatureUpdateRequest
import com.tencent.devops.store.pojo.atom.AtomReleaseRequest
import com.tencent.devops.store.pojo.atom.AtomResp
import com.tencent.devops.store.pojo.atom.MarketAtomCreateRequest
import com.tencent.devops.store.pojo.atom.MarketAtomUpdateRequest
import com.tencent.devops.store.pojo.atom.ReleaseInfo
import com.tencent.devops.store.pojo.atom.enums.AtomCategoryEnum
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.atom.enums.AtomTypeEnum
import com.tencent.devops.store.pojo.atom.enums.OpSortTypeEnum
import com.tencent.devops.store.pojo.common.KEY_RELEASE_INFO
import com.tencent.devops.store.pojo.common.PASS
import com.tencent.devops.store.pojo.common.REJECT
import com.tencent.devops.store.pojo.common.TASK_JSON_NAME
import com.tencent.devops.store.pojo.common.classify.Classify
import com.tencent.devops.store.pojo.common.enums.AuditTypeEnum
import com.tencent.devops.store.pojo.common.enums.PackageSourceTypeEnum
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.file.FileSystems
import java.util.concurrent.Executors
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.FileSystemUtils

@Service
@Suppress("LongParameterList", "LongMethod", "ReturnCount", "ComplexMethod", "NestedBlockDepth")
class OpAtomServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val atomDao: AtomDao,
    private val marketAtomDao: MarketAtomDao,
    private val atomFeatureDao: MarketAtomFeatureDao,
    private val marketAtomVersionLogDao: MarketAtomVersionLogDao,
    private val atomQualityService: AtomQualityService,
    private val atomNotifyService: AtomNotifyService,
    private val labelDao: LabelDao,
    private val atomReleaseService: AtomReleaseService,
    private val storeLogoService: StoreLogoService,
    private val storeWebsocketService: StoreWebsocketService,
    private val classifyService: ClassifyService,
    private val storeI18nMessageService: StoreI18nMessageService,
    private val storeFileService: StoreFileService,
    private val redisOperation: RedisOperation,
    private val client: Client,
    private val marketAtomService: MarketAtomService
) : OpAtomService {

    private val logger = LoggerFactory.getLogger(OpAtomServiceImpl::class.java)
    private val fileSeparator: String = FileSystems.getDefault().separator
    private val executorService by lazy {
        Executors.newFixedThreadPool(1).apply {
            Runtime.getRuntime().addShutdownHook(Thread { shutdown() })
        }
    }

    /**
     * op系统获取插件信息
     */
    override fun getOpPipelineAtoms(
        atomName: String?,
        atomCode: String?,
        atomType: AtomTypeEnum?,
        serviceScope: String?,
        os: String?,
        category: String?,
        classifyId: String?,
        atomStatus: AtomStatusEnum?,
        sortType: OpSortTypeEnum?,
        desc: Boolean?,
        page: Int,
        pageSize: Int
    ): Result<AtomResp<Atom>?> {
        logger.info("getOpPipelineAtoms|atomName=$atomName,serviceScope=$serviceScope,os=$os,atomType=$atomType")
        logger.info("getOpPipelineAtoms|category=$category,classifyId=$classifyId,page=$page,pageSize=$pageSize")
        val pipelineAtomList = atomDao.getOpPipelineAtoms(
            dslContext = dslContext,
            atomName = atomName,
            atomCode = atomCode,
            atomType = atomType,
            serviceScope = serviceScope,
            os = os,
            category = category,
            classifyId = classifyId,
            atomStatus = atomStatus,
            sortType = sortType?.sortType,
            desc = desc,
            page = page,
            pageSize = pageSize
        ).map {
            generatePipelineAtom(it)
        }
        // 处理分页逻辑
        val totalSize = atomDao.getOpPipelineAtomCount(
            dslContext = dslContext,
            atomName = atomName,
            atomCode = atomCode,
            atomType = atomType,
            serviceScope = serviceScope,
            os = os,
            category = category,
            classifyId = classifyId,
            atomStatus = atomStatus
        )
        val totalPage = PageUtil.calTotalPage(pageSize, totalSize)
        return Result(
            AtomResp(
                count = totalSize,
                page = page,
                pageSize = pageSize,
                totalPages = totalPage,
                records = pipelineAtomList
            )
        )
    }

    /**
     * 根据id获取插件信息
     */
    override fun getPipelineAtom(id: String): Result<Atom?> {
        val pipelineAtomRecord = atomDao.getPipelineAtom(dslContext, id)
        return Result(
            if (pipelineAtomRecord == null) {
                null
            } else {
                generatePipelineAtom(pipelineAtomRecord)
            }
        )
    }

    /**
     * 根据插件代码和版本号获取插件信息
     */
    override fun getPipelineAtom(atomCode: String, version: String): Result<Atom?> {
        logger.info("getPipelineAtom atomCode: $atomCode,version:$version")
        val pipelineAtomRecord = atomDao.getPipelineAtom(dslContext, atomCode, version)
        return Result(
            if (pipelineAtomRecord == null) {
                null
            } else {
                generatePipelineAtom(pipelineAtomRecord)
            }
        )
    }

    /**
     * 生成插件对象
     */
    private fun generatePipelineAtom(it: TAtomRecord): Atom {
        val classify = classifyService.getClassify(it.classifyId).data
        return convert(it, classify)
    }

    @Suppress("UNCHECKED_CAST")
    private fun convert(atomRecord: TAtomRecord, classify: Classify?): Atom {
        val atomFeature = atomFeatureDao.getAtomFeature(dslContext, atomRecord.atomCode)
        return Atom(
            id = atomRecord.id,
            name = atomRecord.name,
            atomCode = atomRecord.atomCode,
            classType = atomRecord.classType,
            logoUrl = atomRecord.logoUrl?.let {
                StoreDecorateFactory.get(StoreDecorateFactory.Kind.HOST)?.decorate(it) as? String
            },
            icon = atomRecord.icon,
            summary = atomRecord.summary,
            serviceScope = JsonUtil.toOrNull(atomRecord.serviceScope, List::class.java) as List<String>?,
            jobType = atomRecord.jobType,
            os = JsonUtil.toOrNull(atomRecord.os, List::class.java) as List<String>?,
            classifyId = classify?.id,
            classifyCode = classify?.classifyCode,
            classifyName = classify?.classifyName,
            docsLink = atomRecord.docsLink,
            category = AtomCategoryEnum.getAtomCategory(atomRecord.categroy.toInt()),
            atomType = AtomTypeEnum.getAtomType(atomRecord.atomType.toInt()),
            atomStatus = AtomStatusEnum.getAtomStatus(atomRecord.atomStatus.toInt()),
            description = atomRecord.description?.let {
                StoreDecorateFactory.get(StoreDecorateFactory.Kind.HOST)?.decorate(it) as? String
            },
            version = atomRecord.version,
            creator = atomRecord.creator,
            createTime = DateTimeUtil.toDateTime(atomRecord.createTime),
            modifier = atomRecord.modifier,
            updateTime = DateTimeUtil.toDateTime(atomRecord.updateTime),
            defaultFlag = atomRecord.defaultFlag,
            latestFlag = atomRecord.latestFlag,
            htmlTemplateVersion = atomRecord.htmlTemplateVersion,
            buildLessRunFlag = atomRecord.buildLessRunFlag,
            weight = atomRecord.weight,
            props = atomRecord.props?.let {
                val propJsonStr = storeI18nMessageService.parseJsonStrI18nInfo(
                    jsonStr = it,
                    keyPrefix = StoreUtils.getStoreFieldKeyPrefix(
                        storeType = StoreTypeEnum.ATOM,
                        storeCode = atomRecord.atomCode,
                        version = atomRecord.version
                    )
                )
                StoreDecorateFactory.get(StoreDecorateFactory.Kind.PROPS)
                    ?.decorate(propJsonStr) as Map<String, Any>?
            },
            data = atomRecord.data?.let {
                StoreDecorateFactory.get(StoreDecorateFactory.Kind.DATA)
                    ?.decorate(atomRecord.data) as Map<String, Any>?
            },
            recommendFlag = atomFeature?.recommendFlag,
            yamlFlag = atomFeature?.yamlFlag,
            certificationFlag = atomFeature?.certificationFlag,
            publisher = atomRecord.publisher,
            visibilityLevel = VisibilityLevelEnum.getVisibilityLevel(atomRecord.visibilityLevel as Int),
            privateReason = atomRecord.privateReason
        )
    }

    /**
     * 审核插件
     */
    override fun approveAtom(userId: String, atomId: String, approveReq: ApproveReq): Result<Boolean> {
        // 判断插件是否存在
        val atom = marketAtomDao.getAtomRecordById(dslContext, atomId)
            ?: return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(atomId),
                language = I18nUtil.getLanguage(userId)
            )

        val oldStatus = atom.atomStatus
        if (oldStatus != AtomStatusEnum.AUDITING.status.toByte()) {
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(atomId),
                language = I18nUtil.getLanguage(userId)
            )
        }

        if (approveReq.result != PASS && approveReq.result != REJECT) {
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(approveReq.result),
                language = I18nUtil.getLanguage(userId)
            )
        }
        val atomCode = atom.atomCode
        val passFlag = approveReq.result == PASS
        val atomStatus =
            if (passFlag) {
                AtomStatusEnum.RELEASED.status.toByte()
            } else {
                AtomStatusEnum.AUDIT_REJECT.status.toByte()
            }
        val atomReleaseRecord = marketAtomVersionLogDao.getAtomVersion(dslContext, atomId)
        val releaseType = ReleaseTypeEnum.getReleaseTypeObj(atomReleaseRecord.releaseType.toInt())!!
        // 入库信息
        marketAtomDao.approveAtomFromOp(
            dslContext = dslContext,
            userId = userId,
            atomId = atomId,
            atomStatus = atomStatus,
            approveReq = approveReq
        )
        if (passFlag) {
            atomReleaseService.handleAtomRelease(
                userId = userId,
                releaseFlag = true,
                atomReleaseRequest = AtomReleaseRequest(
                    atomId = atomId,
                    atomCode = atomCode,
                    version = atom.version,
                    atomStatus = atomStatus,
                    releaseType = releaseType,
                    repositoryHashId = atom.repositoryHashId,
                    branch = atom.branch,
                    publisher = atom.modifier
                )
            )
        } else {
            // 更新质量红线信息
            atomQualityService.updateQualityInApprove(approveReq.atomCode, atomStatus)
            // 发送通知消息
            atomNotifyService.sendAtomReleaseAuditNotifyMessage(atomId, AuditTypeEnum.AUDIT_REJECT)
        }
        // 更新默认插件缓存
        if (approveReq.defaultFlag) {
            redisOperation.addSetValue(StoreUtils.getStorePublicFlagKey(StoreTypeEnum.ATOM.name), atomCode)
        } else {
            redisOperation.removeSetMember(StoreUtils.getStorePublicFlagKey(StoreTypeEnum.ATOM.name), atomCode)
        }
        // 通过websocket推送状态变更消息,推送所有有该插件权限的用户
        storeWebsocketService.sendWebsocketMessageByAtomCodeAndAtomId(atomCode, atomId)
        return Result(true)
    }

    @Suppress("UNCHECKED_CAST")
    override fun releaseAtom(
        userId: String,
        atomCode: String,
        inputStream: InputStream,
        disposition: FormDataContentDisposition,
        publisher: String?,
        releaseType: ReleaseTypeEnum?,
        version: String?
    ): Result<Boolean> {
        val (atomPath, file) = StoreFileAnalysisUtil.extractStorePackage(
            storeCode = atomCode,
            storeType = StoreTypeEnum.ATOM,
            inputStream = inputStream,
            disposition = disposition
        )
        val taskJsonFile = File("$atomPath$fileSeparator$TASK_JSON_NAME")
        if (!taskJsonFile.exists()) {
            return I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.STORE_PACKAGE_FILE_NOT_FOUND,
                params = arrayOf(TASK_JSON_NAME),
                language = I18nUtil.getLanguage(userId)
            )
        }
        val taskJsonMap: Map<String, Any>
        val releaseInfo: ReleaseInfo
        // 解析task.json文件
        try {
            val taskJsonStr = taskJsonFile.readText(Charset.forName("UTF-8"))
            taskJsonMap = JsonUtil.toMap(taskJsonStr).toMutableMap()
            val releaseInfoMap = taskJsonMap[KEY_RELEASE_INFO]
            releaseInfo = JsonUtil.mapTo(releaseInfoMap as Map<String, Any>, ReleaseInfo::class.java)
        } catch (e: JsonProcessingException) {
            return I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_REPOSITORY_TASK_JSON_FIELD_IS_INVALID,
                params = arrayOf(KEY_RELEASE_INFO),
                language = I18nUtil.getLanguage(userId)
            )
        }
        val versionInfo = releaseInfo.versionInfo
        if (!publisher.isNullOrBlank()) {
            // 如果接口query参数的发布者不为空，发布者以接口query参数的发布者为准
            versionInfo.publisher = publisher
        }
        releaseType?.let {
            // 如果接口query参数的发布类型不为空，发布类型以接口query参数的发布类型为准
            versionInfo.releaseType = releaseType
        }
        if (!version.isNullOrBlank()) {
            // 如果接口query参数的版本号不为空，发布者以接口query参数的版本号为准
            versionInfo.version = version
        }
        if (versionInfo.releaseType == ReleaseTypeEnum.NEW && atomDao.getPipelineAtom(
                dslContext = dslContext,
                atomCode = atomCode,
                version = INIT_VERSION
            ) == null
        ) {
            // 新增插件
            val addMarketAtomResult = atomReleaseService.addMarketAtom(
                userId,
                MarketAtomCreateRequest(
                    projectCode = releaseInfo.projectId,
                    atomCode = atomCode,
                    name = releaseInfo.name,
                    language = releaseInfo.language,
                    frontendType = releaseInfo.configInfo.frontendType,
                    packageSourceType = PackageSourceTypeEnum.UPLOAD
                )
            )
            if (addMarketAtomResult.isNotOk()) {
                return Result(data = false, message = addMarketAtomResult.message)
            }
        }
        // 远程logo资源不做处理
        if (!releaseInfo.logoUrl.startsWith("http")) {
            // 解析logoUrl
            val logoUrlAnalysisResult = StoreFileAnalysisUtil.logoUrlAnalysis(releaseInfo.logoUrl)
            if (logoUrlAnalysisResult.isNotOk()) {
                return Result(
                    data = false,
                    status = logoUrlAnalysisResult.status,
                    message = logoUrlAnalysisResult.message
                )
            }
            val relativePath = logoUrlAnalysisResult.data
            val logoFile = File(
                "$atomPath${File.separator}file" +
                    "${File.separator}${relativePath?.removePrefix(File.separator)}"
            )
            if (logoFile.exists()) {
                val result = storeLogoService.uploadStoreLogo(
                    userId = userId,
                    contentLength = logoFile.length(),
                    inputStream = logoFile.inputStream(),
                    disposition = FormDataContentDisposition(
                        "form-data; name=\"logo\"; filename=\"${logoFile.name}\""
                    )
                )
                if (result.isOk()) {
                    result.data?.logoUrl?.let { releaseInfo.logoUrl = it }
                } else {
                    return Result(
                        data = false,
                        status = result.status,
                        message = result.message
                    )
                }
            } else {
                throw ErrorCodeException(
                    errorCode = USER_UPLOAD_FILE_PATH_ERROR,
                    params = arrayOf(relativePath ?: "")
                )
            }
        }
        // 解析description
        releaseInfo.description = storeFileService.textReferenceFileAnalysis(
            content = releaseInfo.description,
            fileDirPath = "$atomPath${fileSeparator}file",
            userId = userId
        )
        taskJsonMap[KEY_RELEASE_INFO] = releaseInfo
        // 将替换好的文本写入task.json文件
        val taskJson = taskJsonMap.toJsonString()
        val fileOutputStream = taskJsonFile.outputStream()
        fileOutputStream.use {
            it.write(taskJson.toByteArray(charset("utf-8")))
        }
        try {
            if (file.exists()) {
                val archiveAtomResult = StoreFileAnalysisUtil.serviceArchiveAtomFile(
                    userId = userId,
                    client = client,
                    file = file,
                    archiveAtomRequest = ArchiveAtomRequest(
                        atomCode = atomCode,
                        projectCode = releaseInfo.projectId,
                        version = versionInfo.version,
                        releaseType = versionInfo.releaseType,
                        os = JsonUtil.toJson(releaseInfo.os),
                    )
                )
                if (archiveAtomResult.isNotOk()) {
                    return Result(
                        data = false,
                        status = archiveAtomResult.status,
                        message = archiveAtomResult.message
                    )
                }
            }
        } catch (ignored: Throwable) {
            logger.warn("BKSystemErrorMonitor|archive atom file fail|$atomCode|error=${ignored.message}")
            throw ErrorCodeException(
                errorCode = USER_UPLOAD_PACKAGE_INVALID
            )
        } finally {
            file.delete()
            FileSystemUtils.deleteRecursively(File(atomPath).parentFile)
        }
        val labelIds = if (releaseInfo.labelCodes != null) {
            ArrayList(labelDao.getIdsByCodes(dslContext, releaseInfo.labelCodes!!, 0))
        } else null

        // 升级插件
        val updateMarketAtomResult = atomReleaseService.updateMarketAtom(
            userId,
            releaseInfo.projectId,
            MarketAtomUpdateRequest(
                atomCode = atomCode,
                name = releaseInfo.name,
                category = releaseInfo.category,
                jobType = releaseInfo.jobType,
                os = releaseInfo.os,
                summary = releaseInfo.summary,
                description = releaseInfo.description,
                version = versionInfo.version,
                releaseType = versionInfo.releaseType,
                versionContent = versionInfo.versionContent,
                publisher = versionInfo.publisher,
                labelIdList = labelIds,
                frontendType = releaseInfo.configInfo.frontendType,
                logoUrl = releaseInfo.logoUrl,
                classifyCode = releaseInfo.classifyCode
            )
        )
        if (updateMarketAtomResult.isNotOk()) {
            return Result(
                data = false,
                status = updateMarketAtomResult.status,
                message = updateMarketAtomResult.message
            )
        }
        if (releaseInfo.configInfo.defaultFlag) {
            setDefault(userId, atomCode)
        }
        val atomId = updateMarketAtomResult.data!!
        // 确认测试通过
        return atomReleaseService.passTest(userId, atomId)
    }

    override fun setDefault(userId: String, atomCode: String): Boolean {
        return try {
            dslContext.transaction { t ->
                val context = DSL.using(t)
                atomDao.updateAtomByCode(context, userId, atomCode, AtomFeatureUpdateRequest(defaultFlag = true))
                redisOperation.delete(StoreUtils.getStorePublicFlagKey(StoreTypeEnum.ATOM.name)) // 直接删除重建
            }
            true
        } catch (e: Exception) {
            logger.error("set default atom failed , userId:$userId , atomCode:$atomCode")
            false
        }
    }

    override fun updateAtomRepoFlag(userId: String, atomCode: String?): Result<Boolean> {
        ThreadPoolUtil.submitAction(
            action = {
                updateAtomRepoFlagAction(
                    userId = userId,
                    atomCode = atomCode
                )
            },
            actionTitle = "updateAtomRepoFlag"
        )
        return Result(true)
    }

    private fun updateAtomRepoFlagAction(
        userId: String,
        atomCode: String?
    ) {
        val limit = 100
        var offset = 0
        try {
            do {
                val atomRecords = atomDao.getAtomRepoInfoByCode(
                    dslContext = dslContext,
                    atomCode = atomCode,
                    limit = limit,
                    offset = offset
                )
                val recordSize = atomRecords.size
                logger.info("recordSize:$recordSize")
                try {
                    client.get(ServiceRepositoryResource::class).updateAtomRepoFlag(
                        userId = userId,
                        atomRefRepositoryInfo = atomRecords
                    )
                } catch (ignored: Exception) {
                    logger.warn("fail to insert atom flag|atomRefRepositoryInfos[$atomRecords]", ignored)
                }
                offset += limit
            } while (recordSize == limit)
        } catch (ignored: Exception) {
            logger.warn("updateAtomRepoFlag failed", ignored)
        }
    }

    override fun updateAtomConfigCache(
        userId: String,
        kProperty: String,
        atomCode: String?
    ): Result<Boolean> {
        executorService.submit {
            logger.info("begin updateAtomSensitiveCacheConfig!!")
            val statusList = listOf(
                AtomStatusEnum.TESTING.status.toByte(),
                AtomStatusEnum.AUDITING.status.toByte(),
                AtomStatusEnum.RELEASED.status.toByte()
            )
            try {
                if (atomCode.isNullOrBlank()) {
                    batchUpdateAtomConfigCache(null, kProperty, statusList)
                } else {
                    batchUpdateAtomConfigCache(atomCode, kProperty, statusList)
                }
            } catch (ignored: Exception) {
                logger.warn("updateAtomSensitiveCacheConfig failed", ignored)
            }
            logger.info("end updateAtomSensitiveCacheConfig!!")
        }
        return Result(true)
    }

    private fun batchUpdateAtomConfigCache(
        atomCode: String? = null,
        kProperty: String,
        statusList: List<Byte>
    ) {
        val limit = 100
        var offset = 0
        do {
            val result = atomDao.queryAtomByStatus(
                dslContext = dslContext,
                atomCode = atomCode,
                statusList = statusList,
                offset = offset,
                limit = limit
            )
            val tAtom = TAtom.T_ATOM
            result.forEach {
                val latestFlag = it[tAtom.LATEST_FLAG]
                marketAtomService.updateAtomConfigCache(
                    atomCode = it[tAtom.ATOM_CODE],
                    atomVersion = it[tAtom.VERSION],
                    kProperty = kProperty,
                    props = it[tAtom.PROPS]
                )
                if (latestFlag == true) {
                    marketAtomService.updateAtomConfigCache(
                        atomCode = it[tAtom.ATOM_CODE],
                        atomVersion = VersionUtils.convertLatestVersion(it[tAtom.VERSION]),
                        kProperty = kProperty,
                        props = it[tAtom.PROPS]
                    )
                }
            }
            offset += limit
        } while (result.size == limit)
    }
}

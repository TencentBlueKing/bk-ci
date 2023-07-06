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

package com.tencent.devops.store.service.atom.impl

import com.fasterxml.jackson.core.JsonProcessingException
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.devops.artifactory.api.ServiceArchiveAtomFileResource
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.INIT_VERSION
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.ZipUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.store.tables.records.TAtomRecord
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.constant.StoreMessageCode.USER_UPLOAD_FILE_PATH_ERROR
import com.tencent.devops.store.constant.StoreMessageCode.USER_UPLOAD_PACKAGE_INVALID
import com.tencent.devops.store.dao.atom.AtomDao
import com.tencent.devops.store.dao.atom.MarketAtomDao
import com.tencent.devops.store.dao.atom.MarketAtomFeatureDao
import com.tencent.devops.store.dao.atom.MarketAtomVersionLogDao
import com.tencent.devops.store.dao.common.LabelDao
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
import com.tencent.devops.store.pojo.common.Classify
import com.tencent.devops.store.pojo.common.KEY_RELEASE_INFO
import com.tencent.devops.store.pojo.common.PASS
import com.tencent.devops.store.pojo.common.REJECT
import com.tencent.devops.store.pojo.common.TASK_JSON_NAME
import com.tencent.devops.store.pojo.common.enums.AuditTypeEnum
import com.tencent.devops.store.pojo.common.enums.PackageSourceTypeEnum
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.AtomNotifyService
import com.tencent.devops.store.service.atom.AtomQualityService
import com.tencent.devops.store.service.atom.AtomReleaseService
import com.tencent.devops.store.service.atom.OpAtomService
import com.tencent.devops.store.service.atom.action.AtomDecorateFactory
import com.tencent.devops.store.service.common.ClassifyService
import com.tencent.devops.store.service.common.StoreI18nMessageService
import com.tencent.devops.store.service.common.StoreLogoService
import com.tencent.devops.store.service.websocket.StoreWebsocketService
import com.tencent.devops.store.utils.AtomReleaseTxtAnalysisUtil
import com.tencent.devops.store.utils.StoreUtils
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.file.Files
import java.time.LocalDateTime
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
    private val redisOperation: RedisOperation,
    private val client: Client
) : OpAtomService {

    private val logger = LoggerFactory.getLogger(OpAtomServiceImpl::class.java)
    private val fileSeparator: String = System.getProperty("file.separator")

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
            logoUrl = atomRecord.logoUrl,
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
            description = atomRecord.description,
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
                AtomDecorateFactory.get(AtomDecorateFactory.Kind.PROPS)
                    ?.decorate(propJsonStr) as Map<String, Any>?
            },
            data = atomRecord.data?.let {
                AtomDecorateFactory.get(AtomDecorateFactory.Kind.DATA)
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
        val latestFlag = if (releaseType == ReleaseTypeEnum.HIS_VERSION_UPGRADE || atom.version == INIT_VERSION) {
            // 历史大版本下的小版本更新或者插件首个版本上架审核时不更新latestFlag
            null
        } else {
            approveReq.result == PASS
        }
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
                    branch = atom.branch
                )
            )
        } else {
            // 更新质量红线信息
            atomQualityService.updateQualityInApprove(approveReq.atomCode, atomStatus)
            // 发送通知消息
            atomNotifyService.sendAtomReleaseAuditNotifyMessage(atomId, AuditTypeEnum.AUDIT_REJECT)
        }
        // 入库信息，并设置当前版本的LATEST_FLAG
        marketAtomDao.approveAtomFromOp(
            dslContext = dslContext,
            userId = userId,
            atomId = atomId,
            atomStatus = atomStatus,
            approveReq = approveReq,
            latestFlag = latestFlag,
            pubTime = LocalDateTime.now()
        )
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
        disposition: FormDataContentDisposition
    ): Result<Boolean> {
        // 解压插件包到临时目录
        val fileName = disposition.fileName
        val index = fileName.lastIndexOf(".")
        val fileType = fileName.substring(index + 1)
        val file = Files.createTempFile(UUIDUtil.generate(), ".$fileType").toFile()
        file.outputStream().use {
            inputStream.copyTo(it)
        }
        val atomPath = AtomReleaseTxtAnalysisUtil.buildAtomArchivePath(userId, atomCode)
        if (!File(atomPath).exists()) {
            ZipUtil.unZipFile(file, atomPath, false)
        }
        val taskJsonFile = File("$atomPath$fileSeparator$TASK_JSON_NAME")
        if (!taskJsonFile.exists()) {
            return I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_ATOM_CONF_INVALID,
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
        if (releaseInfo.versionInfo.releaseType == ReleaseTypeEnum.NEW) {
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
            val logoUrlAnalysisResult = AtomReleaseTxtAnalysisUtil.logoUrlAnalysis(releaseInfo.logoUrl)
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
        releaseInfo.description = AtomReleaseTxtAnalysisUtil.descriptionAnalysis(
            description = releaseInfo.description,
            atomPath = atomPath,
            client = client,
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
                val archiveAtomResult = AtomReleaseTxtAnalysisUtil.serviceArchiveAtomFile(
                    userId = userId,
                    projectCode = releaseInfo.projectId,
                    atomCode = atomCode,
                    version = releaseInfo.versionInfo.version,
                    serviceUrlPrefix = client.getServiceUrl(ServiceArchiveAtomFileResource::class),
                    releaseType = releaseInfo.versionInfo.releaseType.name,
                    file = file,
                    os = JsonUtil.toJson(releaseInfo.os)
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
                version = releaseInfo.versionInfo.version,
                releaseType = releaseInfo.versionInfo.releaseType,
                versionContent = releaseInfo.versionInfo.versionContent,
                publisher = releaseInfo.versionInfo.publisher,
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
}

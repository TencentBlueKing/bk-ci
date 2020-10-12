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

package com.tencent.devops.store.service.atom.impl

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.process.api.service.ServiceMeasurePipelineResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.atom.AtomDao
import com.tencent.devops.store.dao.atom.AtomLabelRelDao
import com.tencent.devops.store.dao.atom.MarketAtomFeatureDao
import com.tencent.devops.store.dao.common.ReasonRelDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.pojo.atom.AtomBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.atom.AtomCreateRequest
import com.tencent.devops.store.pojo.atom.AtomFeatureRequest
import com.tencent.devops.store.pojo.atom.AtomResp
import com.tencent.devops.store.pojo.atom.AtomRespItem
import com.tencent.devops.store.pojo.atom.AtomUpdateRequest
import com.tencent.devops.store.pojo.atom.InstalledAtom
import com.tencent.devops.store.pojo.atom.PipelineAtom
import com.tencent.devops.store.pojo.atom.enums.AtomCategoryEnum
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.atom.enums.AtomTypeEnum
import com.tencent.devops.store.pojo.common.KEY_CREATE_TIME
import com.tencent.devops.store.pojo.common.KEY_LABEL_CODE
import com.tencent.devops.store.pojo.common.KEY_LABEL_ID
import com.tencent.devops.store.pojo.common.KEY_LABEL_NAME
import com.tencent.devops.store.pojo.common.KEY_LABEL_TYPE
import com.tencent.devops.store.pojo.common.KEY_UPDATE_TIME
import com.tencent.devops.store.pojo.common.Label
import com.tencent.devops.store.pojo.common.STORE_ATOM_STATUS
import com.tencent.devops.store.pojo.common.UnInstallReq
import com.tencent.devops.store.pojo.common.VersionInfo
import com.tencent.devops.store.pojo.common.enums.ReasonTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.AtomService
import com.tencent.devops.store.service.atom.MarketAtomCommonService
import com.tencent.devops.store.service.common.ClassifyService
import com.tencent.devops.store.service.common.StoreProjectService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.StringUtils
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * 插件业务逻辑类
 *
 * since: 2018-12-20
 */
abstract class AtomServiceImpl @Autowired constructor() : AtomService {

    @Autowired
    lateinit var dslContext: DSLContext
    @Autowired
    lateinit var atomDao: AtomDao
    @Autowired
    lateinit var atomFeatureDao: MarketAtomFeatureDao
    @Autowired
    lateinit var atomLabelRelDao: AtomLabelRelDao
    @Autowired
    lateinit var storeProjectRelDao: StoreProjectRelDao
    @Autowired
    lateinit var reasonRelDao: ReasonRelDao
    @Autowired
    lateinit var storeMemberDao: StoreMemberDao
    @Autowired
    lateinit var storeProjectService: StoreProjectService
    @Autowired
    lateinit var classifyService: ClassifyService
    @Autowired
    lateinit var marketAtomCommonService: MarketAtomCommonService
    @Autowired
    lateinit var client: Client

    private val logger = LoggerFactory.getLogger(javaClass)

    private val cache = CacheBuilder.newBuilder().maximumSize(1000)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build(object : CacheLoader<String, Map<String, String>>() {
            override fun load(projectId: String): Map<String, String> {
                val elementMapData = serviceGetPipelineAtoms(null, null, projectId, null, null, null, null).data
                return elementMapData?.records?.map {
                    it.atomCode to it.name
                }?.toMap() ?: mapOf()
            }
        })

    /**
     * 获取插件列表
     */
    @Suppress("UNCHECKED_CAST")
    override fun getPipelineAtoms(
        accessToken: String,
        userId: String,
        serviceScope: String?,
        os: String?,
        projectCode: String,
        category: String?,
        classifyId: String?,
        page: Int?,
        pageSize: Int?
    ): Result<AtomResp<AtomRespItem>?> {
        // 根据token校验用户有没有查询该项目的权限
        logger.info("getPipelineAtoms accessToken is :$accessToken,userId is :$userId,serviceScope is :$serviceScope,os is :$os")
        logger.info("getPipelineAtoms projectCode is :$projectCode,category is :$category,classifyId is :$classifyId,page is :$page,pageSize is :$pageSize")
        val validateFlag: Boolean?
        try {
            validateFlag = client.get(ServiceProjectResource::class).verifyUserProjectPermission(
                accessToken = accessToken,
                projectCode = projectCode,
                userId = userId
            ).data
        } catch (e: Exception) {
            logger.info("verifyUserProjectPermission error is :$e")
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
        }
        logger.info("the validateFlag is :$validateFlag")
        if (null == validateFlag || !validateFlag) {
            // 抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_QUERY_PROJECT_PERMISSION_IS_INVALID)
        }
        return serviceGetPipelineAtoms(serviceScope, os, projectCode, category, classifyId, page, pageSize)
    }

    @Suppress("UNCHECKED_CAST")
    override fun serviceGetPipelineAtoms(
        serviceScope: String?,
        os: String?,
        projectCode: String,
        category: String?,
        classifyId: String?,
        page: Int?,
        pageSize: Int?
    ): Result<AtomResp<AtomRespItem>?> {
        val dataList = mutableListOf<AtomRespItem>()
        val pipelineAtoms =
            atomDao.getPipelineAtoms(dslContext, serviceScope, os, projectCode, category, classifyId, page, pageSize)
        pipelineAtoms?.forEach {
            val name = it["name"] as String
            val atomCode = it["atomCode"] as String
            val version = it["version"] as String
            val versionPrefix = version.substring(0, version.indexOf(".") + 1)
            val defaultVersion = "$versionPrefix*"
            val classType = it["classType"] as String
            val serviceScopeStr = it["serviceScope"] as? String
            val serviceScopeList = if (!serviceScopeStr.isNullOrBlank()) JsonUtil.getObjectMapper().readValue(serviceScopeStr, List::class.java) as List<String> else listOf()
            val osList = JsonUtil.getObjectMapper().readValue(it["os"] as String, ArrayList::class.java) as ArrayList<String>
            val classifyCode = it["classifyCode"] as String
            val classifyName = it["classifyName"] as String
            val classifyLanName = MessageCodeUtil.getCodeLanMessage(
                messageCode = "${StoreMessageCode.MSG_CODE_STORE_CLASSIFY_PREFIX}$classifyCode",
                defaultMessage = classifyName
            )
            val logoUrl = it["logoUrl"] as? String
            val icon = it["icon"] as? String
            val categoryFlag = it["category"] as Byte
            val summary = it["summary"] as? String
            val docsLink = it["docsLink"] as? String
            val atomType = it["atomType"] as Byte
            val atomStatus = it["atomStatus"] as Byte
            val description = it["description"] as? String
            val publisher = it["publisher"] as? String
            val creator = it["creator"] as String
            val defaultFlag = it["defaultFlag"] as Boolean
            val latestFlag = it["latestFlag"] as Boolean
            val buildLessRunFlag = it["buildLessRunFlag"] as? Boolean
            val htmlTemplateVersion = it["htmlTemplateVersion"] as String
            val weight = it["weight"] as? Int
            val recommendFlag = it["recommendFlag"] as? Boolean
            val pipelineAtomRespItem = AtomRespItem(
                name = name,
                atomCode = atomCode,
                defaultVersion = defaultVersion,
                classType = classType,
                serviceScope = serviceScopeList,
                os = osList,
                logoUrl = logoUrl,
                icon = icon,
                classifyCode = classifyCode,
                classifyName = classifyLanName,
                category = AtomCategoryEnum.getAtomCategory(categoryFlag.toInt()),
                summary = summary,
                docsLink = docsLink,
                atomType = AtomTypeEnum.getAtomType(atomType.toInt()),
                atomStatus = AtomStatusEnum.getAtomStatus(atomStatus.toInt()),
                description = description,
                publisher = publisher,
                creator = creator,
                defaultFlag = defaultFlag,
                latestFlag = latestFlag,
                htmlTemplateVersion = htmlTemplateVersion,
                buildLessRunFlag = buildLessRunFlag,
                weight = weight,
                recommendFlag = recommendFlag
            )
            dataList.add(pipelineAtomRespItem)
        }
        // 处理分页逻辑
        val totalSize = atomDao.getPipelineAtomCount(dslContext, serviceScope, os, projectCode, category, classifyId)
        val totalPage = PageUtil.calTotalPage(pageSize, totalSize)
        return Result(AtomResp(totalSize, page, pageSize, totalPage, dataList))
    }

    override fun getProjectElements(projectCode: String): Result<Map<String, String>> {
        return Result(cache.get(projectCode))
    }

    /**
     * 根据插件代码和版本号获取插件信息
     */
    override fun getPipelineAtom(projectCode: String, atomCode: String, version: String): Result<PipelineAtom?> {
        logger.info("projectCode is: $projectCode,atomCode is: $atomCode,version is:$version")
        val atomResult = getPipelineAtomDetail(projectCode, atomCode, version)
        val atom = atomResult.data
        if (null != atom) {
            val defaultFlag = atom.defaultFlag
            // 非默认类插件需要校验是否有插件的查看权限
            if (null != defaultFlag && !defaultFlag) {
                val count = storeProjectRelDao.countInstalledProject(dslContext, projectCode, atomCode, StoreTypeEnum.ATOM.type.toByte())
                if (count == 0) return MessageCodeUtil.generateResponseDataObject(
                    CommonMessageCode.PARAMETER_IS_INVALID,
                    arrayOf("$projectCode+$atomCode")
                )
            }
        }
        return atomResult
    }

    /**
     * 根据项目代码、插件代码和版本号获取插件信息
     */
    @Suppress("UNCHECKED_CAST")
    override fun getPipelineAtomDetail(projectCode: String, atomCode: String, version: String): Result<PipelineAtom?> {
        logger.info("the projectCode is: $projectCode,atomCode is: $atomCode,version is:$version")
        val atomStatusList = generateAtomStatusList(atomCode, projectCode)
        atomStatusList.add(AtomStatusEnum.UNDERCARRIAGED.status.toByte()) // 也要给那些还在使用已下架的插件插件展示详情
        val pipelineAtomRecord = atomDao.getPipelineAtom(dslContext, projectCode, atomCode, version.replace("*", ""), atomStatusList)
        logger.info("the pipelineAtomRecord is :$pipelineAtomRecord")
        return Result(
            if (pipelineAtomRecord == null) {
                null
            } else {
                val atomClassify = classifyService.getClassify(pipelineAtomRecord.classifyId).data
                val versionList = getPipelineAtomVersions(projectCode, atomCode).data
                val atomLabelList = mutableListOf<Label>()
                val atomLabelRecords = atomLabelRelDao.getLabelsByAtomId(dslContext, pipelineAtomRecord.id) // 查询插件标签信息
                atomLabelRecords?.forEach {
                    atomLabelList.add(
                        Label(
                            it[KEY_LABEL_ID] as String,
                            it[KEY_LABEL_CODE] as String,
                            it[KEY_LABEL_NAME] as String,
                            StoreTypeEnum.getStoreType((it[KEY_LABEL_TYPE] as Byte).toInt()),
                            (it[KEY_CREATE_TIME] as LocalDateTime).timestampmilli(),
                            (it[KEY_UPDATE_TIME] as LocalDateTime).timestampmilli()
                        )
                    )
                }
                val atomFeature = atomFeatureDao.getAtomFeature(dslContext, atomCode)
                PipelineAtom(
                    id = pipelineAtomRecord.id,
                    name = pipelineAtomRecord.name,
                    atomCode = pipelineAtomRecord.atomCode,
                    classType = pipelineAtomRecord.classType,
                    logoUrl = pipelineAtomRecord.logoUrl,
                    icon = pipelineAtomRecord.icon,
                    summary = pipelineAtomRecord.summary,
                    serviceScope = if (!StringUtils.isEmpty(pipelineAtomRecord.serviceScope)) JsonUtil.getObjectMapper().readValue(pipelineAtomRecord.serviceScope, List::class.java) as List<String> else null,
                    jobType = pipelineAtomRecord.jobType,
                    os = if (!StringUtils.isEmpty(pipelineAtomRecord.os)) JsonUtil.getObjectMapper().readValue(pipelineAtomRecord.os, List::class.java) as List<String> else null,
                    classifyId = atomClassify?.id,
                    classifyCode = atomClassify?.classifyCode,
                    classifyName = atomClassify?.classifyName,
                    docsLink = pipelineAtomRecord.docsLink,
                    category = AtomCategoryEnum.getAtomCategory(pipelineAtomRecord.categroy.toInt()),
                    atomType = AtomTypeEnum.getAtomType(pipelineAtomRecord.atomType.toInt()),
                    atomStatus = AtomStatusEnum.getAtomStatus(pipelineAtomRecord.atomStatus.toInt()),
                    description = pipelineAtomRecord.description,
                    versionList = versionList!!,
                    atomLabelList = atomLabelList,
                    creator = pipelineAtomRecord.creator,
                    defaultFlag = pipelineAtomRecord.defaultFlag,
                    latestFlag = pipelineAtomRecord.latestFlag,
                    htmlTemplateVersion = pipelineAtomRecord.htmlTemplateVersion,
                    buildLessRunFlag = pipelineAtomRecord.buildLessRunFlag,
                    weight = pipelineAtomRecord.weight,
                    props = atomDao.convertString(pipelineAtomRecord.props),
                    data = atomDao.convertString(pipelineAtomRecord.data),
                    recommendFlag = atomFeature?.recommendFlag
                )
            }
        )
    }

    /**
     * 根据项目代码、插件代码和版本号获取插件信息
     */
    @Suppress("UNCHECKED_CAST")
    override fun getPipelineAtomVersions(projectCode: String, atomCode: String): Result<List<VersionInfo>> {
        logger.info("the projectCode is: $projectCode,atomCode is: $atomCode")
        val atomStatusList = generateAtomStatusList(atomCode, projectCode)
        val versionList = mutableListOf<VersionInfo>()
        val versionRecords = atomDao.getVersionsByAtomCode(dslContext, projectCode, atomCode, atomStatusList) // 查询插件版本信息
        var tmpVersionPrefix = ""
        versionRecords?.forEach {
            val atomVersion = it["version"] as String
            val index = atomVersion.indexOf(".")
            val versionPrefix = atomVersion.substring(0, index + 1)
            var versionName = atomVersion
            var latestVersionName = versionPrefix + "latest"
            val atomStatus = it["atomStatus"] as Byte
            val atomVersionStatusList = listOf(AtomStatusEnum.TESTING.status.toByte(), AtomStatusEnum.UNDERCARRIAGING.status.toByte(), AtomStatusEnum.UNDERCARRIAGED.status.toByte())
            if (atomVersionStatusList.contains(atomStatus)) {
                // 处于测试中、下架中、已下架的插件版本的版本名称加下说明
                val atomStatusName = AtomStatusEnum.getAtomStatus(atomStatus.toInt())
                val storeAtomStatusPrefix = STORE_ATOM_STATUS + "_"
                val atomStatusMsg = MessageCodeUtil.getCodeLanMessage("$storeAtomStatusPrefix$atomStatusName")
                versionName = "$atomVersion ($atomStatusMsg)"
                latestVersionName = "$latestVersionName ($atomStatusMsg)"
            }
            if (tmpVersionPrefix != versionPrefix) {
                versionList.add(VersionInfo(latestVersionName, "$versionPrefix*")) // 添加大版本号的通用最新模式（如1.*）
                tmpVersionPrefix = versionPrefix
            }
            versionList.add(VersionInfo(versionName, atomVersion)) // 添加具体的版本号
        }
        logger.info("the atomCode is: $atomCode,versionList is: $versionList")
        return Result(versionList)
    }

    private fun generateAtomStatusList(
        atomCode: String,
        projectCode: String
    ): MutableList<Byte> {
        val flag = storeProjectRelDao.isInitTestProjectCode(dslContext, atomCode, StoreTypeEnum.ATOM, projectCode)
        logger.info("the isInitTestProjectCode flag is :$flag")
        // 普通项目的查已发布和下架中的插件
        var atomStatusList =
            mutableListOf(AtomStatusEnum.RELEASED.status.toByte(), AtomStatusEnum.UNDERCARRIAGING.status.toByte())
        if (flag) {
            // 原生初始化项目有和申请插件协作者指定的调试项目权查处于测试中、审核中、已发布和下架中的插件
            atomStatusList = mutableListOf(
                AtomStatusEnum.TESTING.status.toByte(),
                AtomStatusEnum.AUDITING.status.toByte(),
                AtomStatusEnum.RELEASED.status.toByte(),
                AtomStatusEnum.UNDERCARRIAGING.status.toByte()
            )
        }
        return atomStatusList
    }

    /**
     * 添加插件信息
     */
    override fun savePipelineAtom(userId: String, atomRequest: AtomCreateRequest): Result<Boolean> {
        val id = UUIDUtil.generate()
        logger.info("the atomRequest is :$atomRequest")
        // 判断插件代码是否存在
        val atomCode = atomRequest.atomCode
        val codeCount = atomDao.countByCode(dslContext, atomCode)
        if (codeCount > 0) {
            // 抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_EXIST, arrayOf(atomCode), false)
        }
        val atomName = atomRequest.name
        // 判断插件分类名称是否存在
        val nameCount = atomDao.countByName(dslContext, atomName)
        if (nameCount > 0) {
            // 抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_EXIST, arrayOf(atomName), false)
        }
        // 校验插件分类是否合法
        classifyService.getClassify(atomRequest.classifyId).data
            ?: return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(atomRequest.classifyId), false)
        val classType = handleClassType(atomRequest.os)
        atomRequest.os.sort() // 给操作系统排序
        atomDao.addAtomFromOp(dslContext, userId, id, classType, atomRequest)
        return Result(true)
    }

    private fun handleClassType(osList: MutableList<String>): String {
        var classType = "marketBuild" // 默认为有构建环境
        if (osList.isEmpty() || osList.contains("NONE")) {
            classType = "marketBuildLess" // 无构建环境
            osList.clear()
        }
        return classType
    }

    /**
     * 更新插件信息
     */
    override fun updatePipelineAtom(userId: String, id: String, atomUpdateRequest: AtomUpdateRequest): Result<Boolean> {
        logger.info("the update id is :$id , the atomUpdateRequest is :$atomUpdateRequest")
        // 校验插件分类是否合法
        classifyService.getClassify(atomUpdateRequest.classifyId).data
            ?: return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(atomUpdateRequest.classifyId), false)
        val atomRecord = atomDao.getPipelineAtom(dslContext, id)
        logger.info("the atomRecord is :$atomRecord")
        return if (null != atomRecord) {
            val visibilityLevel = atomUpdateRequest.visibilityLevel
            val dbVisibilityLevel = atomRecord.visibilityLevel
            val updateRepoInfoResult = updateRepoInfo(
                visibilityLevel = visibilityLevel,
                dbVisibilityLevel = dbVisibilityLevel,
                userId = userId,
                repositoryHashId = atomRecord.repositoryHashId
            )
            if (updateRepoInfoResult.isNotOk()) {
                return updateRepoInfoResult
            }
            val htmlTemplateVersion = atomRecord.htmlTemplateVersion
            var classType = atomRecord.classType
            if ("1.0" != htmlTemplateVersion
                && (classType == "marketBuild" || classType == "marketBuildLess")
            ) {
                // 更新插件市场的插件才需要根据操作系统来生成插件大类
                classType = handleClassType(atomUpdateRequest.os)
            }
            atomUpdateRequest.os.sort() // 给操作系统排序
            dslContext.transaction { t ->
                val context = DSL.using(t)
                atomDao.updateAtomFromOp(context, userId, id, classType, atomUpdateRequest)
                val recommendFlag = atomUpdateRequest.recommendFlag
                if (null != recommendFlag) {
                    // 为了兼容老插件特性表没有记录的情况，如果没有记录就新增
                    val atomFeatureRecord = atomFeatureDao.getAtomFeature(context, atomRecord.atomCode)
                    if (null != atomFeatureRecord) {
                        atomFeatureDao.updateAtomFeature(
                            context, userId, AtomFeatureRequest(
                                atomCode = atomRecord.atomCode,
                                recommendFlag = recommendFlag,
                                yamlFlag = atomUpdateRequest.yamlFlag
                            )
                        )
                    } else {
                        atomFeatureDao.addAtomFeature(
                            context, userId, AtomFeatureRequest(
                                atomCode = atomRecord.atomCode,
                                recommendFlag = recommendFlag,
                                yamlFlag = atomUpdateRequest.yamlFlag
                            )
                        )
                    }
                }
            }
            Result(true)
        } else {
            MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(id), false)
        }
    }

    /**
     * 删除插件信息
     */
    override fun deletePipelineAtom(id: String): Result<Boolean> {
        logger.info("the delete id is :$id")
        dslContext.transaction { t ->
            val context = DSL.using(t)
            // 删除插件信息
            atomDao.delete(context, id)
        }
        return Result(true)
    }

    /**
     * 根据插件ID和插件代码判断插件是否存在
     */
    override fun judgeAtomExistByIdAndCode(atomId: String, atomCode: String): Result<Boolean> {
        logger.info("the atomId is:$atomId, atomCode is:$atomCode")
        val count = atomDao.countByIdAndCode(dslContext, atomId, atomCode)
        if (count < 1) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf("atomId:$atomId,atomCode:$atomCode"), false)
        }
        return Result(true)
    }

    /**
     * 根据用户ID和插件代码判断该插件是否由该用户创建
     */
    override fun judgeAtomIsCreateByUserId(userId: String, atomCode: String): Result<Boolean> {
        logger.info("judgeAtomExistByIdAndCode userId is:$userId, atomCode is:$atomCode")
        val count = atomDao.countByUserIdAndCode(dslContext, userId, atomCode)
        if (count < 1) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED, false)
        }
        return Result(true)
    }

    /**
     * 获取已安装的插件列表
     */
    override fun getInstalledAtoms(
        userId: String,
        projectCode: String,
        classifyCode: String?,
        page: Int?,
        pageSize: Int?
    ): Page<InstalledAtom> {
        logger.info("getInstalledAtoms projectCode is $projectCode, classifyCode is $classifyCode, page is $page, pageSize is $pageSize")

        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 100

        // 项目下已安装插件记录
        val result = mutableListOf<InstalledAtom>()
        val count = atomDao.countInstalledAtoms(dslContext, projectCode, classifyCode)
        if (count == 0) {
            return Page(pageNotNull, pageSizeNotNull, count.toLong(), result)
        }

        val records = atomDao.getInstalledAtoms(dslContext, projectCode, classifyCode, pageNotNull, pageSizeNotNull)
        val atomCodeList = mutableListOf<String>()
        records?.forEach {
            atomCodeList.add(it["atomCode"] as String)
        }

        // 插件关联的流水线
        val pipelineStat = client.get(ServiceMeasurePipelineResource::class).batchGetPipelineCountByAtomCode(atomCodeList.joinToString(","), projectCode).data
        val hasManagerPermission = hasManagerPermission(projectCode, userId)
        records?.forEach {
            val atomCode = it["atomCode"] as String
            val installer = it["installer"] as String
            val installType = it["installType"] as Byte
            // 判断项目是否是初始化项目或者调试项目
            val isInitTest = installType == StoreProjectTypeEnum.INIT.type.toByte() || installType == StoreProjectTypeEnum.TEST.type.toByte()
            val atomClassifyCode = it["classifyCode"] as String
            val classifyName = it["classifyName"] as String
            val classifyLanName = MessageCodeUtil.getCodeLanMessage(
                messageCode = "${StoreMessageCode.MSG_CODE_STORE_CLASSIFY_PREFIX}$atomClassifyCode",
                defaultMessage = classifyName
            )
            result.add(InstalledAtom(
                atomId = it["atomId"] as String,
                atomCode = atomCode,
                name = it["atomName"] as String,
                logoUrl = it["logoUrl"] as? String,
                classifyCode = atomClassifyCode,
                classifyName = classifyLanName,
                category = AtomCategoryEnum.getAtomCategory((it["category"] as Byte).toInt()),
                summary = it["summary"] as? String,
                publisher = it["publisher"] as? String,
                installer = installer,
                installTime = DateTimeUtil.toDateTime(it["installTime"] as LocalDateTime),
                installType = StoreProjectTypeEnum.getProjectType((it["installType"] as Byte).toInt()),
                pipelineCnt = pipelineStat?.get(atomCode) ?: 0,
                hasPermission = !isInitTest && (hasManagerPermission || installer == userId)
            ))
        }

        return Page(pageNotNull, pageSizeNotNull, count.toLong(), result)
    }

    /**
     * 获取已安装的插件列表
     */
    override fun listInstalledAtomByProject(
        projectCode: String
    ): List<InstalledAtom> {

        // 获取已安装插件
        val records = atomDao.getInstalledAtoms(dslContext, projectCode, null, null, null)
        val atomCodeList = mutableListOf<String>()
        records?.forEach {
            atomCodeList.add(it["atomCode"] as String)
        }
        val installAtoms = records?.map {
            val atomCode = it["atomCode"] as String
            val installer = it["installer"] as String
            val classifyCode = it["classifyCode"] as String
            val classifyName = it["classifyName"] as String
            val classifyLanName = MessageCodeUtil.getCodeLanMessage(
                messageCode = "${StoreMessageCode.MSG_CODE_STORE_CLASSIFY_PREFIX}$classifyCode",
                defaultMessage = classifyName
            )
            // 判断项目是否是初始化项目或者调试项目
            InstalledAtom(
                atomId = it["atomId"] as String,
                atomCode = atomCode,
                name = it["atomName"] as String,
                logoUrl = it["logoUrl"] as? String,
                classifyCode = classifyCode,
                classifyName = classifyLanName,
                category = AtomCategoryEnum.getAtomCategory((it["category"] as Byte).toInt()),
                summary = it["summary"] as? String,
                publisher = it["publisher"] as? String,
                installer = installer,
                installTime = DateTimeUtil.toDateTime(it["installTime"] as LocalDateTime),
                installType = StoreProjectTypeEnum.getProjectType((it["installType"] as Byte).toInt()),
                pipelineCnt = 0,
                hasPermission = true
            )
        } ?: listOf()

        // 获取自研插件
        val selfAtoms = atomDao.getSelfDevelopAtoms(dslContext)?.map {
            InstalledAtom(
                atomId = it.id,
                atomCode = it.atomCode,
                name = it.name,
                logoUrl = it.logoUrl,
                classifyCode = "",
                classifyName = "",
                category = AtomCategoryEnum.getAtomCategory((it.categroy).toInt()),
                summary = it.summary,
                publisher = it.publisher,
                installer = "",
                installTime = "",
                installType = "",
                pipelineCnt = 0,
                hasPermission = true
            )
        } ?: listOf()

        // 返回结果
        val result = mutableListOf<InstalledAtom>()
        result.addAll(installAtoms)
        result.addAll(selfAtoms)

        return result
    }

    /**
     * 卸载插件
     */
    override fun uninstallAtom(
        userId: String,
        projectCode: String,
        atomCode: String,
        unInstallReq: UnInstallReq
    ): Result<Boolean> {
        logger.info("uninstallAtom, $projectCode | $atomCode | $userId")
        // 用户是否有权限卸载
        val isInstaller = storeProjectRelDao.isInstaller(dslContext, userId, atomCode, StoreTypeEnum.ATOM.type.toByte())
        logger.info("uninstallAtom, isInstaller=$isInstaller")
        if (!(hasManagerPermission(projectCode, userId) || isInstaller)) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED, arrayOf(atomCode))
        }

        // 是否还有流水线使用待卸载的插件
        val pipelineCnt = client.get(ServiceMeasurePipelineResource::class).getPipelineCountByAtomCode(atomCode, projectCode).data
            ?: 0
        if (pipelineCnt > 0) {
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_ATOM_USED, arrayOf(atomCode, projectCode))
        }

        dslContext.transaction { t ->
            val context = DSL.using(t)

            // 卸载
            storeProjectService.uninstall(StoreTypeEnum.ATOM, atomCode, projectCode)

            // 是否需要删除质量红线指标？ -- todo

            // 入库卸载原因
            unInstallReq.reasonList.forEach {
                if (it?.reasonId != null) {
                    reasonRelDao.add(
                        dslContext = context,
                        id = UUIDUtil.generate(),
                        userId = userId,
                        storeCode = atomCode,
                        storeType = StoreTypeEnum.ATOM.type.toByte(),
                        reasonId = it.reasonId,
                        note = it.note,
                        type = ReasonTypeEnum.UNINSTALLATOM.type
                    )
                }
            }
        }

        return Result(true)
    }

    override fun updateAtomBaseInfo(userId: String, atomCode: String, atomBaseInfoUpdateRequest: AtomBaseInfoUpdateRequest): Result<Boolean> {
        logger.info("updateAtomBaseInfo userId is :$userId, atomCode is :$atomCode, atomBaseInfoUpdateRequest is :$atomBaseInfoUpdateRequest")
        // 判断当前用户是否是该插件的成员
        if (!storeMemberDao.isStoreMember(dslContext, userId, atomCode, StoreTypeEnum.ATOM.type.toByte())) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        // 查询插件的最新记录
        val newestAtomRecord = atomDao.getNewestAtomByCode(dslContext, atomCode)
            ?: throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf(atomCode))
        val editFlag = marketAtomCommonService.checkEditCondition(atomCode)
        if (!editFlag) {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.USER_ATOM_VERSION_IS_NOT_FINISH,
                params = arrayOf(newestAtomRecord.name, newestAtomRecord.version)
            )
        }
        val visibilityLevel = atomBaseInfoUpdateRequest.visibilityLevel
        val dbVisibilityLevel = newestAtomRecord.visibilityLevel
        val updateRepoInfoResult = updateRepoInfo(visibilityLevel, dbVisibilityLevel, userId, newestAtomRecord.repositoryHashId)
        if (updateRepoInfoResult.isNotOk()) {
            return updateRepoInfoResult
        }
        val atomIdList = mutableListOf(newestAtomRecord.id)
        val latestAtomRecord = atomDao.getLatestAtomByCode(dslContext, atomCode)
        logger.info("updateAtomBaseInfo latestAtomRecord is :$latestAtomRecord")
        if (null != latestAtomRecord) {
            atomIdList.add(latestAtomRecord.id)
        }
        logger.info("updateAtomBaseInfo atomIdList is :$atomIdList")
        dslContext.transaction { t ->
            val context = DSL.using(t)
            atomDao.updateAtomBaseInfo(context, userId, atomIdList, atomBaseInfoUpdateRequest)
            // 更新标签信息
            val labelIdList = atomBaseInfoUpdateRequest.labelIdList
            if (null != labelIdList) {
                atomIdList.forEach {
                    atomLabelRelDao.deleteByAtomId(context, it)
                    if (labelIdList.isNotEmpty())
                        atomLabelRelDao.batchAdd(context, userId, it, labelIdList)
                }
            }
        }
        return Result(true)
    }

    abstract fun updateRepoInfo(
        visibilityLevel: VisibilityLevelEnum?,
        dbVisibilityLevel: Int?,
        userId: String,
        repositoryHashId: String
    ): Result<Boolean>
}

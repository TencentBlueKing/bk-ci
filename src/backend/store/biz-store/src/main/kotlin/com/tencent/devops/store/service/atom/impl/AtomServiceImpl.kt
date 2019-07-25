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
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.model.store.tables.records.TAtomRecord
import com.tencent.devops.store.dao.atom.AtomDao
import com.tencent.devops.store.dao.atom.AtomLabelRelDao
import com.tencent.devops.store.dao.common.ClassifyDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.pojo.atom.Atom
import com.tencent.devops.store.pojo.atom.AtomCreateRequest
import com.tencent.devops.store.pojo.atom.AtomResp
import com.tencent.devops.store.pojo.atom.AtomRespItem
import com.tencent.devops.store.pojo.atom.AtomUpdateRequest
import com.tencent.devops.store.pojo.atom.PipelineAtom
import com.tencent.devops.store.pojo.atom.VersionInfo
import com.tencent.devops.store.pojo.atom.enums.AtomCategoryEnum
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.atom.enums.AtomTypeEnum
import com.tencent.devops.store.pojo.common.Label
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.AtomService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * 插件业务逻辑类
 *
 * since: 2018-12-20
 */
@Service
class AtomServiceImpl @Autowired constructor() : AtomService {

    @Autowired
    lateinit var dslContext: DSLContext
    @Autowired
    lateinit var atomDao: AtomDao
    @Autowired
    lateinit var atomLabelRelDao: AtomLabelRelDao
    @Autowired
    lateinit var atomClassifyDao: ClassifyDao
    @Autowired
    lateinit var storeProjectRelDao: StoreProjectRelDao

    private val logger = LoggerFactory.getLogger(AtomServiceImpl::class.java)

    /**
     * 生成插件对象
     */
    private fun generatePipelineAtom(it: TAtomRecord): Atom {
        val atomClassifyRecord = atomClassifyDao.getClassify(dslContext, it.classifyId)
        return atomDao.convert(it, atomClassifyRecord)
    }

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
        logger.info("the accessToken is :$accessToken,userId is :$userId,serviceScope is :$serviceScope,os is :$os,projectCode is :$projectCode,category is :$category,classifyId is :$classifyId,page is :$page,pageSize is :$pageSize")
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
            val serviceScopeList = if (!serviceScopeStr.isNullOrBlank()) JsonUtil.getObjectMapper().readValue(
                serviceScopeStr,
                List::class.java
            ) as List<String> else listOf()
            val osList =
                JsonUtil.getObjectMapper().readValue(it["os"] as String, ArrayList::class.java) as ArrayList<String>
            val classifyCode = it["classifyCode"] as String
            val classifyName = it["classifyName"] as String
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
                classifyName = classifyName,
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
                weight = weight
            )
            dataList.add(pipelineAtomRespItem)
        }
        // 处理分页逻辑
        val totalSize = atomDao.getPipelineAtomCount(dslContext, serviceScope, os, projectCode, category, classifyId)
        val totalPage = PageUtil.calTotalPage(pageSize, totalSize)
        return Result(AtomResp(totalSize, page, pageSize, totalPage, dataList))
    }

    /**
     * 根据id获取插件信息
     */
    override fun getPipelineAtom(id: String): Result<Atom?> {
        logger.info("the id is :{}", id)
        val pipelineAtomRecord = atomDao.getPipelineAtom(dslContext, id)
        logger.info("the pipelineAtomRecord is :{}", pipelineAtomRecord)
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
    override fun getPipelineAtom(projectCode: String, atomCode: String, version: String): Result<PipelineAtom?> {
        logger.info("projectCode is: $projectCode,atomCode is: $atomCode,version is:$version")
        val atomResult = getPipelineAtomDetail(projectCode, atomCode, version)
        val atom = atomResult.data
        if (null != atom) {
            val defaultFlag = atom.defaultFlag
            // 非默认类插件需要校验是否有插件的查看权限
            if (null != defaultFlag && !defaultFlag) {
                val count = storeProjectRelDao.countInstalledProject(
                    dslContext,
                    projectCode,
                    atomCode,
                    StoreTypeEnum.ATOM.type.toByte()
                )
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
        logger.info("getPipelineAtomDetail projectCode is: $projectCode,atomCode is: $atomCode,version is:$version")
        val atomStatusList = generateAtomStatusList(atomCode, projectCode)
        atomStatusList.add(AtomStatusEnum.UNDERCARRIAGED.status.toByte()) // 也要给那些还在使用已下架的插件插件展示详情
        val pipelineAtomRecord =
            atomDao.getPipelineAtom(dslContext, projectCode, atomCode, version.replace("*", ""), atomStatusList)
        logger.info("getPipelineAtomDetail pipelineAtomRecord is :$pipelineAtomRecord")
        return Result(
            if (pipelineAtomRecord == null) {
                null
            } else {
                val atomClassifyRecord = atomClassifyDao.getClassify(dslContext, pipelineAtomRecord.classifyId)
                val versionList = getPipelineAtomVersions(projectCode, atomCode).data
                val atomLabelList = mutableListOf<Label>()
                val atomLabelRecords = atomLabelRelDao.getLabelsByAtomId(dslContext, pipelineAtomRecord.id) // 查询插件标签信息
                atomLabelRecords?.forEach {
                    atomLabelList.add(
                        Label(
                            it["id"] as String,
                            it["labelCode"] as String,
                            it["labelName"] as String,
                            StoreTypeEnum.getStoreType((it["labelType"] as Byte).toInt()),
                            (it["createTime"] as LocalDateTime).timestampmilli(),
                            (it["updateTime"] as LocalDateTime).timestampmilli()
                        )
                    )
                }
                PipelineAtom(
                    id = pipelineAtomRecord.id,
                    name = pipelineAtomRecord.name,
                    atomCode = pipelineAtomRecord.atomCode,
                    classType = pipelineAtomRecord.classType,
                    logoUrl = pipelineAtomRecord.logoUrl,
                    icon = pipelineAtomRecord.icon,
                    summary = pipelineAtomRecord.summary,
                    serviceScope = if (!StringUtils.isEmpty(pipelineAtomRecord.serviceScope)) JsonUtil.getObjectMapper().readValue(
                        pipelineAtomRecord.serviceScope,
                        List::class.java
                    ) as List<String> else null,
                    jobType = pipelineAtomRecord.jobType,
                    os = if (!StringUtils.isEmpty(pipelineAtomRecord.os)) JsonUtil.getObjectMapper().readValue(
                        pipelineAtomRecord.os,
                        List::class.java
                    ) as List<String> else null,
                    classifyId = atomClassifyRecord?.id,
                    classifyCode = atomClassifyRecord?.classifyCode,
                    classifyName = atomClassifyRecord?.classifyName,
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
                    data = atomDao.convertString(pipelineAtomRecord.data)
                )
            }
        )
    }

    /**
     * 根据项目代码、插件代码和版本号获取插件信息
     */
    @Suppress("UNCHECKED_CAST")
    override fun getPipelineAtomVersions(projectCode: String, atomCode: String): Result<List<VersionInfo>> {
        logger.info("getPipelineAtomVersions projectCode is: $projectCode,atomCode is: $atomCode")
        val atomStatusList = generateAtomStatusList(atomCode, projectCode)
        val versionList = mutableListOf<VersionInfo>()
        val versionRecords =
            atomDao.getVersionsByAtomCode(dslContext, projectCode, atomCode, atomStatusList) // 查询插件版本信息
        var latestVersionName = ""
        versionRecords?.forEach {
            val atomVersion = it["version"] as String
            val index = atomVersion.indexOf(".")
            val versionPrefix = atomVersion.substring(0, index + 1)
            val versionName = versionPrefix + "latest"
            if (latestVersionName != versionName) {
                versionList.add(VersionInfo(versionName, "$versionPrefix*")) // 添加大版本号的通用最新模式（如1.*）
                latestVersionName = versionName
            }
            versionList.add(VersionInfo(atomVersion, atomVersion)) // 添加具体的版本号
        }
        logger.info("getPipelineAtomVersions atomCode is: $atomCode,versionList is: $versionList")
        return Result(versionList)
    }

    private fun generateAtomStatusList(
        atomCode: String,
        projectCode: String
    ): MutableList<Byte> {
        val initProjectCode =
            storeProjectRelDao.getInitProjectCodeByStoreCode(dslContext, atomCode, StoreTypeEnum.ATOM.type.toByte())
        logger.info("the initProjectCode is :$initProjectCode")
        // 普通项目的查已发布和下架中的插件
        var atomStatusList =
            mutableListOf(AtomStatusEnum.RELEASED.status.toByte(), AtomStatusEnum.UNDERCARRIAGING.status.toByte())
        if (projectCode == initProjectCode) {
            // 原生项目有权查处于测试中、审核中、已发布和下架中的插件
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
     * 根据插件代码和版本号获取插件信息
     */
    override fun getPipelineAtom(atomCode: String, version: String): Result<Atom?> {
        logger.info("the atomCode is: $atomCode,version is:$version")
        val pipelineAtomRecord = atomDao.getPipelineAtom(dslContext, atomCode, version.replace("*", ""))
        logger.info("the pipelineAtomRecord is :$pipelineAtomRecord")
        return Result(
            if (pipelineAtomRecord == null) {
                null
            } else {
                generatePipelineAtom(pipelineAtomRecord)
            }
        )
    }

    /**
     * 添加插件信息
     */
    override fun savePipelineAtom(userId: String, atomRequest: AtomCreateRequest): Result<Boolean> {
        val id = UUIDUtil.generate()
        logger.info("savePipelineAtom userId is :$userId,atomRequest is :$atomRequest")
        // 判断插件代码是否存在
        val atomCode = atomRequest.atomCode
        val codeCount = atomDao.countByCode(dslContext, atomCode)
        if (codeCount > 0) {
            // 抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_EXIST,
                arrayOf(atomCode),
                false
            )
        }
        val atomName = atomRequest.name
        // 判断插件分类名称是否存在
        val nameCount = atomDao.countByName(dslContext, atomName)
        if (nameCount > 0) {
            // 抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_EXIST,
                arrayOf(atomName),
                false
            )
        }
        // 校验插件分类是否合法
        atomClassifyDao.getClassify(dslContext, atomRequest.classifyId)
            ?: return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf(atomRequest.classifyId),
                false
            )
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
        atomClassifyDao.getClassify(dslContext, atomUpdateRequest.classifyId)
            ?: return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf(atomUpdateRequest.classifyId),
                false
            )
        val atomRecord = atomDao.getPipelineAtom(dslContext, id)
        logger.info("the atomRecord is :$atomRecord")
        return if (null != atomRecord) {
            val htmlTemplateVersion = atomRecord.htmlTemplateVersion
            var classType = atomRecord.classType
            if ("1.0" != htmlTemplateVersion) {
                // 更新插件市场的插件才需要根据操作系统来生成插件大类
                classType = handleClassType(atomUpdateRequest.os)
            }
            atomUpdateRequest.os.sort() // 给操作系统排序
            dslContext.transaction { t ->
                val context = DSL.using(t)
                atomDao.updateAtomFromOp(context, userId, id, classType, atomUpdateRequest)
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
        logger.info("judgeAtomExistByIdAndCode atomId is:$atomId, atomCode is:$atomCode")
        val count = atomDao.countByIdAndCode(dslContext, atomId, atomCode)
        if (count < 1) {
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf("atomId:$atomId,atomCode:$atomCode"),
                false
            )
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

    override fun getProjectAtomNames(projectCode: String): Result<Map<String, String>> {
        return Result(cache.get(projectCode))
    }

    private val cache = CacheBuilder.newBuilder().maximumSize(1000)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build(object : CacheLoader<String, Map<String, String>>() {
            override fun load(projectId: String): Map<String, String> {
                val elementMapData = getPipelineAtoms(
                    accessToken = "",
                    userId = "",
                    serviceScope = null,
                    os = null,
                    projectCode = projectId,
                    category = null,
                    classifyId = null,
                    page = null,
                    pageSize = null
                ).data
                return elementMapData?.records?.map {
                    it.atomCode to it.name
                }?.toMap() ?: mapOf()
            }
        })
}

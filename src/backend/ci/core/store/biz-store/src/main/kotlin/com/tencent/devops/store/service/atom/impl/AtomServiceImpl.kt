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

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.KEY_DESCRIPTION
import com.tencent.devops.common.api.constant.KEY_DOCSLINK
import com.tencent.devops.common.api.constant.KEY_OS
import com.tencent.devops.common.api.constant.KEY_SUMMARY
import com.tencent.devops.common.api.constant.KEY_VERSION
import com.tencent.devops.common.api.constant.KEY_WEIGHT
import com.tencent.devops.common.api.constant.NAME
import com.tencent.devops.common.api.constant.VERSION
import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.common.web.service.ServiceI18nMessageResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.api.service.ServiceMeasurePipelineResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.constant.StoreMessageCode.GET_INFO_NO_PERMISSION
import com.tencent.devops.store.constant.StoreMessageCode.PROJECT_NO_PERMISSION
import com.tencent.devops.store.dao.atom.AtomDao
import com.tencent.devops.store.dao.atom.AtomLabelRelDao
import com.tencent.devops.store.dao.atom.MarketAtomFeatureDao
import com.tencent.devops.store.dao.common.ReasonRelDao
import com.tencent.devops.store.dao.common.StoreErrorCodeInfoDao
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
import com.tencent.devops.store.pojo.common.KEY_ATOM_CODE
import com.tencent.devops.store.pojo.common.KEY_ATOM_STATUS
import com.tencent.devops.store.pojo.common.KEY_ATOM_TYPE
import com.tencent.devops.store.pojo.common.KEY_AVG_SCORE
import com.tencent.devops.store.pojo.common.KEY_BUILD_LESS_RUN_FLAG
import com.tencent.devops.store.pojo.common.KEY_CATEGORY
import com.tencent.devops.store.pojo.common.KEY_CLASSIFY_CODE
import com.tencent.devops.store.pojo.common.KEY_CLASSIFY_NAME
import com.tencent.devops.store.pojo.common.KEY_CLASS_TYPE
import com.tencent.devops.store.pojo.common.KEY_CREATE_TIME
import com.tencent.devops.store.pojo.common.KEY_CREATOR
import com.tencent.devops.store.pojo.common.KEY_DEFAULT_FLAG
import com.tencent.devops.store.pojo.common.KEY_HOT_FLAG
import com.tencent.devops.store.pojo.common.KEY_HTML_TEMPLATE_VERSION
import com.tencent.devops.store.pojo.common.KEY_ICON
import com.tencent.devops.store.pojo.common.KEY_ID
import com.tencent.devops.store.pojo.common.KEY_INSTALLER
import com.tencent.devops.store.pojo.common.KEY_INSTALL_TIME
import com.tencent.devops.store.pojo.common.KEY_INSTALL_TYPE
import com.tencent.devops.store.pojo.common.KEY_LATEST_FLAG
import com.tencent.devops.store.pojo.common.KEY_LOGO_URL
import com.tencent.devops.store.pojo.common.KEY_MODIFIER
import com.tencent.devops.store.pojo.common.KEY_PUBLISHER
import com.tencent.devops.store.pojo.common.KEY_RECENT_EXECUTE_NUM
import com.tencent.devops.store.pojo.common.KEY_RECOMMEND_FLAG
import com.tencent.devops.store.pojo.common.KEY_SERVICE_SCOPE
import com.tencent.devops.store.pojo.common.KEY_UPDATE_TIME
import com.tencent.devops.store.pojo.common.STORE_ATOM_STATUS
import com.tencent.devops.store.pojo.common.UnInstallReq
import com.tencent.devops.store.pojo.common.VersionInfo
import com.tencent.devops.store.pojo.common.enums.ReasonTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.AtomLabelService
import com.tencent.devops.store.service.atom.AtomService
import com.tencent.devops.store.service.atom.MarketAtomCommonService
import com.tencent.devops.store.service.atom.action.AtomDecorateFactory
import com.tencent.devops.store.service.common.ClassifyService
import com.tencent.devops.store.service.common.StoreCommonService
import com.tencent.devops.store.service.common.StoreHonorService
import com.tencent.devops.store.service.common.StoreI18nMessageService
import com.tencent.devops.store.service.common.StoreIndexManageService
import com.tencent.devops.store.service.common.StoreProjectService
import com.tencent.devops.store.service.common.StoreUserService
import com.tencent.devops.store.utils.StoreUtils
import com.tencent.devops.store.utils.VersionUtils
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import org.apache.commons.collections4.ListUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

/**
 * 插件业务逻辑类
 *
 * since: 2018-12-20
 */
@Suppress("ALL")
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
    lateinit var storeErrorCodeInfoDao: StoreErrorCodeInfoDao

    @Autowired
    lateinit var storeHonorService: StoreHonorService

    @Autowired
    lateinit var storeIndexManageService: StoreIndexManageService

    @Autowired
    lateinit var storeProjectService: StoreProjectService

    @Autowired
    lateinit var classifyService: ClassifyService

    @Autowired
    lateinit var marketAtomCommonService: MarketAtomCommonService

    @Autowired
    lateinit var atomLabelService: AtomLabelService

    @Autowired
    lateinit var atomMemberService: AtomMemberServiceImpl

    @Autowired
    lateinit var storeCommonService: StoreCommonService

    @Autowired
    lateinit var storeUserService: StoreUserService

    @Autowired
    lateinit var storeI18nMessageService: StoreI18nMessageService

    @Autowired
    lateinit var redisOperation: RedisOperation

    @Autowired
    lateinit var client: Client

    private val logger = LoggerFactory.getLogger(javaClass)

    private val atomNameCache = Caffeine.newBuilder()
        .maximumSize(2000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build<String, Map<String, String>>()

    /**
     * 获取插件列表
     */
    @Suppress("UNCHECKED_CAST")
    @BkTimed(extraTags = ["get", "getPipelineAtom"], value = "store_get_pipeline_atom")
    override fun getPipelineAtoms(
        accessToken: String,
        userId: String,
        serviceScope: String?,
        jobType: String?,
        os: String?,
        projectCode: String,
        category: String?,
        classifyId: String?,
        recommendFlag: Boolean?,
        keyword: String?,
        queryProjectAtomFlag: Boolean,
        fitOsFlag: Boolean?,
        queryFitAgentBuildLessAtomFlag: Boolean?,
        page: Int,
        pageSize: Int
    ): Result<AtomResp<AtomRespItem>?> {
        if (queryProjectAtomFlag) {
            // 根据token校验用户有没有查询该项目的权限
            val validateFlag: Boolean?
            try {
                validateFlag = client.get(ServiceProjectResource::class).verifyUserProjectPermission(
                    accessToken = accessToken,
                    projectCode = projectCode,
                    userId = userId
                ).data
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
                    messageCode = StoreMessageCode.USER_QUERY_PROJECT_PERMISSION_IS_INVALID,
                    language = I18nUtil.getLanguage(userId)
                )
            }
        }
        return serviceGetPipelineAtoms(
            userId = userId,
            serviceScope = serviceScope,
            jobType = jobType,
            os = os,
            projectCode = projectCode,
            category = category,
            classifyId = classifyId,
            recommendFlag = recommendFlag,
            queryProjectAtomFlag = queryProjectAtomFlag,
            keyword = keyword,
            fitOsFlag = fitOsFlag,
            queryFitAgentBuildLessAtomFlag = queryFitAgentBuildLessAtomFlag,
            page = page,
            pageSize = pageSize
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun serviceGetPipelineAtoms(
        userId: String,
        serviceScope: String?,
        jobType: String?,
        os: String?,
        projectCode: String,
        category: String?,
        classifyId: String?,
        recommendFlag: Boolean?,
        keyword: String?,
        queryProjectAtomFlag: Boolean,
        fitOsFlag: Boolean?,
        queryFitAgentBuildLessAtomFlag: Boolean?,
        page: Int?,
        pageSize: Int?
    ): Result<AtomResp<AtomRespItem>?> {
        val dataList = mutableListOf<AtomRespItem>()
        val pipelineAtoms = atomDao.getPipelineAtoms(
            dslContext = dslContext,
            serviceScope = serviceScope,
            jobType = jobType,
            os = os,
            projectCode = projectCode,
            category = category,
            classifyId = classifyId,
            recommendFlag = recommendFlag,
            keyword = keyword,
            fitOsFlag = fitOsFlag,
            queryProjectAtomFlag = queryProjectAtomFlag,
            queryFitAgentBuildLessAtomFlag = queryFitAgentBuildLessAtomFlag,
            page = page,
            pageSize = pageSize
        )
        val atomIdSet = mutableSetOf<String>()
        val atomCodeSet = mutableSetOf<String>()
        pipelineAtoms?.forEach {
            atomIdSet.add(it[KEY_ID] as String)
            atomCodeSet.add(it[KEY_ATOM_CODE] as String)
        }
        val atomHonorInfoMap = storeHonorService.getHonorInfosByStoreCodes(StoreTypeEnum.ATOM, atomCodeSet.toList())
        val atomIndexInfosMap =
            storeIndexManageService.getStoreIndexInfosByStoreCodes(StoreTypeEnum.ATOM, atomCodeSet.toList())
        val atomLabelInfoMap = atomLabelService.getLabelsByAtomIds(atomIdSet)
        // 查询使用插件的流水线数量
        var atomPipelineCntMap: Map<String, Int>? = null
        var atomVisibleDataMap: Map<String, MutableList<Int>>? = null
        var memberDataMap: Map<String, MutableList<String>>? = null
        var installedAtomList: List<String>? = null
        var userDeptList: List<Int>? = null
        if (queryProjectAtomFlag && !atomCodeSet.isNullOrEmpty()) {
            atomPipelineCntMap = client.get(ServiceMeasurePipelineResource::class).batchGetPipelineCountByAtomCode(
                atomCodes = atomCodeSet.joinToString(","),
                projectCode = projectCode
            ).data
        } else if (!queryProjectAtomFlag && !atomCodeSet.isNullOrEmpty()) {
            val atomCodeList = atomCodeSet.toList()
            atomVisibleDataMap = storeCommonService.generateStoreVisibleData(atomCodeList, StoreTypeEnum.ATOM)
            memberDataMap = atomMemberService.batchListMember(atomCodeList, StoreTypeEnum.ATOM).data
            userDeptList = storeUserService.getUserDeptList(userId)
            installedAtomList = storeProjectRelDao.getValidStoreCodesByProject(
                dslContext = dslContext,
                projectCode = projectCode,
                storeCodes = atomCodeSet,
                storeType = StoreTypeEnum.ATOM
            )?.map { it.value1() }
        }
        pipelineAtoms?.forEach {
            val name = it[NAME] as String
            val atomCode = it[KEY_ATOM_CODE] as String
            val version = it[VERSION] as String
            val defaultVersion = VersionUtils.convertLatestVersion(version)
            val classType = it[KEY_CLASS_TYPE] as String
            val serviceScopeStr = it[KEY_SERVICE_SCOPE] as? String
            val honorInfos = atomHonorInfoMap[atomCode]
            val indexInfos = atomIndexInfosMap[atomCode]
            val serviceScopeList = if (!serviceScopeStr.isNullOrBlank()) {
                JsonUtil.getObjectMapper().readValue(serviceScopeStr, List::class.java) as List<String>
            } else listOf()
            val osList =
                JsonUtil.getObjectMapper().readValue(it[KEY_OS] as String, ArrayList::class.java) as ArrayList<String>
            val classifyCode = it[KEY_CLASSIFY_CODE] as String
            val classifyName = it[KEY_CLASSIFY_NAME] as String
            val classifyLanName = I18nUtil.getCodeLanMessage(
                messageCode = "${StoreTypeEnum.ATOM.name}.classify.$classifyCode",
                defaultMessage = classifyName
            )
            // 社区版插件归档bkrepo后删除local参数
            var logoUrl = it["logoUrl"] as? String
            logoUrl = if (logoUrl?.contains("?") == true) {
                logoUrl.plus("&logo=true")
            } else {
                logoUrl?.plus("?logo=true")
            }
            val categoryFlag = it[KEY_CATEGORY] as Byte
            val atomType = it[KEY_ATOM_TYPE] as Byte
            val atomStatus = it[KEY_ATOM_STATUS] as Byte
            val atomPipelineCnt = atomPipelineCntMap?.get(atomCode)
            val installFlag = if (!queryProjectAtomFlag) storeCommonService.generateInstallFlag(
                defaultFlag = it[KEY_DEFAULT_FLAG] as Boolean,
                members = memberDataMap?.get(atomCode),
                userId = userId,
                visibleList = atomVisibleDataMap?.get(atomCode),
                userDeptList = userDeptList!!) else null
            val pipelineAtomRespItem = AtomRespItem(
                name = name,
                atomCode = atomCode,
                version = version,
                defaultVersion = defaultVersion,
                classType = classType,
                serviceScope = serviceScopeList,
                os = osList,
                logoUrl = logoUrl,
                icon = it[KEY_ICON] as? String,
                classifyCode = classifyCode,
                classifyName = classifyLanName,
                category = AtomCategoryEnum.getAtomCategory(categoryFlag.toInt()),
                summary = it[KEY_SUMMARY] as? String,
                docsLink = it[KEY_DOCSLINK] as? String,
                atomType = AtomTypeEnum.getAtomType(atomType.toInt()),
                atomStatus = AtomStatusEnum.getAtomStatus(atomStatus.toInt()),
                description = it[KEY_DESCRIPTION] as? String,
                publisher = it[KEY_PUBLISHER] as? String,
                creator = it[KEY_CREATOR] as String,
                modifier = it[KEY_MODIFIER] as String,
                createTime = DateTimeUtil.toDateTime(it[KEY_CREATE_TIME] as LocalDateTime),
                updateTime = DateTimeUtil.toDateTime(it[KEY_UPDATE_TIME] as LocalDateTime),
                defaultFlag = it[KEY_DEFAULT_FLAG] as Boolean,
                latestFlag = it[KEY_LATEST_FLAG] as Boolean,
                htmlTemplateVersion = it[KEY_HTML_TEMPLATE_VERSION] as String,
                buildLessRunFlag = it[KEY_BUILD_LESS_RUN_FLAG] as? Boolean,
                weight = it[KEY_WEIGHT] as? Int,
                recommendFlag = it[KEY_RECOMMEND_FLAG] as? Boolean,
                score = String.format("%.1f", (it[KEY_AVG_SCORE] as? BigDecimal)?.toDouble()).toDoubleOrNull(),
                recentExecuteNum = it[KEY_RECENT_EXECUTE_NUM] as? Int,
                uninstallFlag = if (atomPipelineCnt == null) null else atomPipelineCnt < 1,
                labelList = atomLabelInfoMap?.get(it[KEY_ID] as String),
                installFlag = installFlag,
                installed = if (queryProjectAtomFlag) true else installedAtomList?.contains(atomCode),
                honorInfos = honorInfos,
                indexInfos = indexInfos,
                hotFlag = it[KEY_HOT_FLAG] as Boolean
            )
            dataList.add(pipelineAtomRespItem)
        }
        // 处理分页逻辑
        val totalSize = atomDao.getPipelineAtomCount(
            dslContext = dslContext,
            serviceScope = serviceScope,
            jobType = jobType,
            os = os,
            projectCode = projectCode,
            category = category,
            classifyId = classifyId,
            recommendFlag = recommendFlag,
            keyword = keyword,
            fitOsFlag = fitOsFlag,
            queryProjectAtomFlag = queryProjectAtomFlag,
            queryFitAgentBuildLessAtomFlag = queryFitAgentBuildLessAtomFlag
        )
        val totalPage = PageUtil.calTotalPage(pageSize, totalSize)
        return Result(AtomResp(totalSize, page, pageSize, totalPage, dataList))
    }

    override fun getProjectElements(projectCode: String): Result<Map<String, String>> {
        // 从缓存中取出插件的名称集合信息
        var atomNameMap = atomNameCache.getIfPresent(projectCode)
        if (atomNameMap == null) {
            // 缓存中没有名称信息则实时去DB查
            val defaultAtomCodeRecords = atomDao.batchGetDefaultAtomCode(dslContext)
            val defaultAtomCodeList = defaultAtomCodeRecords.map { it.value1() }
            val projectAtomCodeRecords = storeProjectRelDao.getValidStoreCodes(
                dslContext = dslContext,
                projectCode = projectCode,
                storeType = StoreTypeEnum.ATOM
            )
            val projectAtomCodeList = projectAtomCodeRecords?.map { it.value1() }
            if (!projectAtomCodeList.isNullOrEmpty()) {
                defaultAtomCodeList.addAll(projectAtomCodeList)
            }
            // 插件去重
            val atomCodeList = defaultAtomCodeList.toSet().toList()
            atomNameMap = mutableMapOf()
            // 分批获取插件的名称
            ListUtils.partition(atomCodeList, 50).forEach { rids ->
                val atomNameRecords = atomDao.batchGetAtomName(dslContext, rids)
                val i18nMessageList = atomNameRecords?.let {
                    client.get(ServiceI18nMessageResource::class).getI18nMessages(
                        keys = atomNameRecords.map { "ATOM.${it.value1()}.${it.value3()}.releaseInfo.name" },
                        moduleCode = SystemModuleEnum.STORE.name,
                        language = I18nUtil.getRequestUserLanguage()
                    ).data
                }
                atomNameRecords?.forEach { atomNameRecord ->
                    val atomCode = atomNameRecord.value1()
                    val i18nMessage = i18nMessageList?.find {
                        it.key == "ATOM.${atomNameRecord.value1()}.${atomNameRecord.value3()}.releaseInfo.name"
                    }
                    val atomName = i18nMessage?.value ?: atomNameRecord.value2()
                    atomNameMap[atomCode] = atomName
                }
            }
            // 把插件的名称信息放入缓存
            atomNameCache.put(projectCode, atomNameMap)
        }
        return Result(atomNameMap)
    }

    override fun getProjectElementsInfo(projectCode: String): Result<Map<String, String>> {
        // 从缓存中取出插件的名称集合信息
        val cacheKey = projectCode + "_info"
        var atomNameMap = atomNameCache.getIfPresent(cacheKey)
        if (atomNameMap == null) {
            // 缓存中没有名称信息则实时去DB查
            val defaultAtomCodeRecords = atomDao.batchGetDefaultAtomCode(dslContext)
            atomNameMap = defaultAtomCodeRecords.associate {
                it.value1() to StoreProjectTypeEnum.COMMON.name
            }.toMutableMap()
            val projectAtomCodeRecords = storeProjectRelDao.getValidStoreCodes(
                dslContext = dslContext,
                projectCode = projectCode,
                storeType = StoreTypeEnum.ATOM
            )
            projectAtomCodeRecords?.map {
                if (atomNameMap[it.value1()] == null ||
                    it.value2().toInt() == StoreProjectTypeEnum.INIT.type ||
                    it.value2().toInt() == StoreProjectTypeEnum.TEST.type) {
                    atomNameMap[it.value1()] = StoreProjectTypeEnum.getProjectType(it.value2().toInt())
                }
            }
            // 把插件的名称信息放入缓存
            atomNameCache.put(cacheKey, atomNameMap)
        }
        return Result(atomNameMap)
    }

    /**
     * 根据插件代码和版本号获取插件信息
     */
    @BkTimed(extraTags = ["get", "getPipelineAtom"], value = "store_get_pipeline_atom")
    override fun getPipelineAtom(
        projectCode: String,
        atomCode: String,
        version: String,
        atomStatus: Byte?,
        queryOfflineFlag: Boolean
    ): Result<PipelineAtom?> {
        logger.info("getPipelineAtom $projectCode,$atomCode,$version,$atomStatus,$queryOfflineFlag")
        val atomResult = getPipelineAtomDetail(
            projectCode = projectCode,
            atomCode = atomCode,
            version = version,
            atomStatus = atomStatus,
            queryOfflineFlag = queryOfflineFlag
        )
        val atom = atomResult.data
        if (null != atom) {
            val defaultFlag = atom.defaultFlag
            // 非默认类插件需要校验是否有插件的查看权限
            if (null != defaultFlag && !defaultFlag) {
                val count = storeProjectRelDao.countInstalledProject(
                    dslContext = dslContext,
                    projectCode = projectCode,
                    storeCode = atomCode,
                    storeType = StoreTypeEnum.ATOM.type.toByte()
                )
                if (count == 0) return I18nUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                    params = arrayOf("$projectCode+$atomCode")
                )
            }
        }
        return atomResult
    }

    /**
     * 根据项目代码、插件代码和版本号获取插件信息
     */
    @Suppress("UNCHECKED_CAST")
    @BkTimed(extraTags = ["get", "getPipelineAtom"], value = "store_get_pipeline_atom")
    override fun getPipelineAtomDetail(
        projectCode: String?,
        atomCode: String,
        version: String,
        atomStatus: Byte?,
        queryOfflineFlag: Boolean
    ): Result<PipelineAtom?> {
        logger.info("getPipelineAtomDetail $projectCode,$atomCode,$version,$atomStatus,$queryOfflineFlag")
        val atomStatusList = if (atomStatus != null) {
            mutableListOf(atomStatus)
        } else {
            if (projectCode != null) {
                generateAtomStatusList(atomCode, projectCode)
            } else {
                null
            }
        }
        if (queryOfflineFlag) {
            atomStatusList?.add(AtomStatusEnum.UNDERCARRIAGED.status.toByte()) // 也要给那些还在使用已下架的插件插件展示详情
        }
        val pipelineAtomRecord = if (projectCode != null) {
            atomDao.getPipelineAtom(
                dslContext = dslContext,
                projectCode = projectCode,
                atomCode = atomCode,
                version = version,
                defaultFlag = marketAtomCommonService.isPublicAtom(atomCode),
                atomStatusList = atomStatusList
            )
        } else {
            atomDao.getPipelineAtom(
                dslContext = dslContext,
                atomCode = atomCode,
                version = version,
                atomStatusList = atomStatusList
            )
        }
        return Result(
            if (pipelineAtomRecord == null) {
                null
            } else {
                val atomClassify = classifyService.getClassify(pipelineAtomRecord.classifyId).data
                val versionList = getPipelineAtomVersions(projectCode, atomCode).data
                val atomLabelList = atomLabelService.getLabelsByAtomId(pipelineAtomRecord.id)
                val atomFeature = atomFeatureDao.getAtomFeature(dslContext, atomCode)
                PipelineAtom(
                    id = pipelineAtomRecord.id,
                    name = pipelineAtomRecord.name,
                    atomCode = pipelineAtomRecord.atomCode,
                    version = pipelineAtomRecord.version,
                    classType = pipelineAtomRecord.classType,
                    logoUrl = pipelineAtomRecord.logoUrl,
                    icon = pipelineAtomRecord.icon,
                    summary = pipelineAtomRecord.summary,
                    serviceScope =
                    JsonUtil.toOrNull(pipelineAtomRecord.serviceScope, List::class.java) as List<String>?,
                    jobType = pipelineAtomRecord.jobType,
                    os = JsonUtil.toOrNull(pipelineAtomRecord.os, List::class.java) as List<String>?,
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
                    props = pipelineAtomRecord.props?.let {
                        val propJsonStr = storeI18nMessageService.parseJsonStrI18nInfo(
                            jsonStr = it,
                            keyPrefix = StoreUtils.getStoreFieldKeyPrefix(
                                storeType = StoreTypeEnum.ATOM,
                                storeCode = atomCode,
                                version = pipelineAtomRecord.version
                            )
                        )
                        AtomDecorateFactory.get(AtomDecorateFactory.Kind.PROPS)
                            ?.decorate(propJsonStr) as Map<String, Any>?
                    },
                    data = pipelineAtomRecord.data?.let {
                        AtomDecorateFactory.get(AtomDecorateFactory.Kind.DATA)
                            ?.decorate(pipelineAtomRecord.data) as Map<String, Any>?
                    },
                    recommendFlag = atomFeature?.recommendFlag,
                    frontendType = FrontendTypeEnum.getFrontendTypeObj(pipelineAtomRecord.htmlTemplateVersion),
                    visibilityLevel = VisibilityLevelEnum.getVisibilityLevel(pipelineAtomRecord.visibilityLevel as Int),
                    createTime = pipelineAtomRecord.createTime.timestampmilli(),
                    updateTime = pipelineAtomRecord.updateTime.timestampmilli()
                )
            }
        )
    }

    /**
     * 根据项目代码、插件代码和版本号获取插件信息
     */
    @Suppress("UNCHECKED_CAST")
    @BkTimed(extraTags = ["get", "getPipelineAtom"], value = "store_get_pipeline_atom")
    override fun getPipelineAtomVersions(projectCode: String?, atomCode: String): Result<List<VersionInfo>> {
        logger.info("getPipelineAtomVersions projectCode is: $projectCode,atomCode is: $atomCode")
        val atomStatusList = if (projectCode != null) {
            generateAtomStatusList(atomCode, projectCode)
        } else {
            null
        }
        val versionList = mutableListOf<VersionInfo>()
        // 查询插件版本信息
        val versionRecords = if (projectCode != null) {
            val defaultFlag = marketAtomCommonService.isPublicAtom(atomCode)
            atomDao.getVersionsByAtomCode(
                dslContext = dslContext,
                projectCode = projectCode,
                atomCode = atomCode,
                defaultFlag = defaultFlag,
                atomStatusList = atomStatusList
            )
        } else {
            atomDao.getVersionsByAtomCode(
                dslContext = dslContext,
                atomCode = atomCode,
                atomStatusList = atomStatusList
            )
        }
        var tmpVersionPrefix = ""
        versionRecords?.forEach {
            val atomVersion = it[KEY_VERSION] as String
            val index = atomVersion.indexOf(".")
            val versionPrefix = atomVersion.substring(0, index + 1)
            var versionName = atomVersion
            var latestVersionName = VersionUtils.convertLatestVersionName(atomVersion)
            val atomStatus = it[KEY_ATOM_STATUS] as Byte
            val atomVersionStatusList = listOf(
                AtomStatusEnum.TESTING.status.toByte(),
                AtomStatusEnum.UNDERCARRIAGING.status.toByte(),
                AtomStatusEnum.UNDERCARRIAGED.status.toByte()
            )
            if (atomVersionStatusList.contains(atomStatus)) {
                // 处于测试中、下架中、已下架的插件版本的版本名称加下说明
                val atomStatusName = AtomStatusEnum.getAtomStatus(atomStatus.toInt())
                val storeAtomStatusPrefix = STORE_ATOM_STATUS + "_"
                val atomStatusMsg = I18nUtil.getCodeLanMessage(
                    messageCode = "$storeAtomStatusPrefix$atomStatusName"
                )
                versionName = "$atomVersion ($atomStatusMsg)"
                latestVersionName = "$latestVersionName ($atomStatusMsg)"
            }
            if (tmpVersionPrefix != versionPrefix) {
                versionList.add(VersionInfo(latestVersionName, "$versionPrefix*")) // 添加大版本号的通用最新模式（如1.*）
                tmpVersionPrefix = versionPrefix
            }
            versionList.add(VersionInfo(versionName, atomVersion)) // 添加具体的版本号
        }
        return Result(versionList)
    }

    private fun generateAtomStatusList(
        atomCode: String,
        projectCode: String
    ): MutableList<Byte> {
        val flag = storeProjectRelDao.isTestProjectCode(dslContext, atomCode, StoreTypeEnum.ATOM, projectCode)
        logger.info("isInitTestProjectCode atomCode=$atomCode|projectCode=$projectCode|flag=$flag")
        // 普通项目的查已发布和下架中的插件
        var atomStatusList =
            mutableListOf(AtomStatusEnum.RELEASED.status.toByte(), AtomStatusEnum.UNDERCARRIAGING.status.toByte())
        if (flag) {
            // 原生初始化项目有和申请插件协作者指定的调试项目权查处于构建中、测试中、代码检查中、审核中、已发布和下架中的插件
            atomStatusList = mutableListOf(
                AtomStatusEnum.BUILDING.status.toByte(),
                AtomStatusEnum.TESTING.status.toByte(),
                AtomStatusEnum.CODECCING.status.toByte(),
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
        logger.info("savePipelineAtom userId=$userId|atomRequest=$atomRequest")
        // 判断插件代码是否存在
        val atomCode = atomRequest.atomCode
        val codeCount = atomDao.countByCode(dslContext, atomCode)
        if (codeCount > 0) {
            // 抛出错误提示
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_EXIST,
                params = arrayOf(atomCode),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        }
        val atomName = atomRequest.name
        // 判断插件分类名称是否存在
        val nameCount = atomDao.countByName(dslContext, atomName)
        if (nameCount > 0) {
            // 抛出错误提示
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_EXIST,
                params = arrayOf(atomName),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        }
        // 校验插件分类是否合法
        classifyService.getClassify(atomRequest.classifyId).data
            ?: return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(atomRequest.classifyId),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        val classType = handleClassType(atomRequest.os)
        atomRequest.os.sort() // 给操作系统排序
        atomDao.addAtomFromOp(dslContext, userId, id, classType, atomRequest)
        return Result(true)
    }

    private fun handleClassType(osList: MutableList<String>): String {
        var classType = MarketBuildAtomElement.classType // 默认为有构建环境
        if (osList.isEmpty() || osList.contains("NONE")) {
            classType = MarketBuildLessAtomElement.classType // 无构建环境
            osList.clear()
        }
        return classType
    }

    /**
     * 更新插件信息
     */
    override fun updatePipelineAtom(
        userId: String,
        id: String,
        atomUpdateRequest: AtomUpdateRequest
    ): Result<Boolean> {
        logger.info("updatePipelineAtom userId=$userId|id=$id|atomUpdateRequest=$atomUpdateRequest")
        // 校验插件分类是否合法
        classifyService.getClassify(atomUpdateRequest.classifyId).data
            ?: return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(atomUpdateRequest.classifyId),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        val atomRecord = atomDao.getPipelineAtom(dslContext, id)
        return if (null != atomRecord) {
            // 触发器类的插件repositoryHashId字段值为空
            if (atomRecord.repositoryHashId != null) {
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
            }
            val htmlTemplateVersion = atomRecord.htmlTemplateVersion
            var classType = atomRecord.classType
            if (FrontendTypeEnum.HISTORY.typeVersion != htmlTemplateVersion &&
                (classType == MarketBuildAtomElement.classType || classType == MarketBuildLessAtomElement.classType)
            ) {
                // 更新插件市场的插件才需要根据操作系统来生成插件大类
                classType = handleClassType(atomUpdateRequest.os)
            }
            atomUpdateRequest.os.sort() // 给操作系统排序
            val atomCode = atomRecord.atomCode
            dslContext.transaction { t ->
                val context = DSL.using(t)
                atomDao.updateAtomFromOp(context, userId, id, classType, atomUpdateRequest)
                val recommendFlag = atomUpdateRequest.recommendFlag
                if (null != recommendFlag) {
                    // 为了兼容老插件特性表没有记录的情况，如果没有记录就新增
                    val atomFeatureRecord = atomFeatureDao.getAtomFeature(context, atomCode)
                    if (null != atomFeatureRecord) {
                        atomFeatureDao.updateAtomFeature(
                            context, userId, AtomFeatureRequest(
                                atomCode = atomCode,
                                recommendFlag = recommendFlag,
                                yamlFlag = atomUpdateRequest.yamlFlag,
                                qualityFlag = atomUpdateRequest.qualityFlag,
                                certificationFlag = atomUpdateRequest.certificationFlag
                            )
                        )
                    } else {
                        atomFeatureDao.addAtomFeature(
                            context, userId, AtomFeatureRequest(
                                atomCode = atomCode,
                                recommendFlag = recommendFlag,
                                yamlFlag = atomUpdateRequest.yamlFlag,
                                qualityFlag = atomUpdateRequest.qualityFlag,
                                certificationFlag = atomUpdateRequest.certificationFlag
                            )
                        )
                    }
                }
                // 更新默认插件缓存
                if (atomUpdateRequest.defaultFlag) {
                    redisOperation.addSetValue(StoreUtils.getStorePublicFlagKey(StoreTypeEnum.ATOM.name), atomCode)
                } else {
                    redisOperation.removeSetMember(StoreUtils.getStorePublicFlagKey(StoreTypeEnum.ATOM.name), atomCode)
                }
                // 更新插件运行时信息缓存
                marketAtomCommonService.updateAtomRunInfoCache(
                    atomId = id,
                    atomName = atomUpdateRequest.name,
                    jobType = atomUpdateRequest.jobType,
                    buildLessRunFlag = atomUpdateRequest.buildLessRunFlag,
                    props = atomUpdateRequest.props
                )
            }
            Result(true)
        } else {
            I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(id),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        }
    }

    /**
     * 删除插件信息
     */
    override fun deletePipelineAtom(id: String): Result<Boolean> {
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
        logger.info("judgeAtomExistByIdAndCode atomId=$atomId|atomCode=$atomCode")
        val count = atomDao.countByIdAndCode(dslContext, atomId, atomCode)
        if (count < 1) {
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf("atomId:$atomId,atomCode:$atomCode"),
                data = false,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
        }
        return Result(true)
    }

    /**
     * 根据用户ID和插件代码判断该插件是否由该用户创建
     */
    override fun judgeAtomIsCreateByUserId(userId: String, atomCode: String): Result<Boolean> {
        logger.info("judgeAtomExistByIdAndCode userId=$userId|atomCode=$atomCode")
        val count = atomDao.countByUserIdAndCode(dslContext, userId, atomCode)
        if (count < 1) {
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PERMISSION_DENIED,
                params = null,
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
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
        name: String?,
        page: Int,
        pageSize: Int
    ): Page<InstalledAtom> {
        // 项目下已安装插件记录
        val result = mutableListOf<InstalledAtom>()
        val count = atomDao.countInstalledAtoms(dslContext, projectCode, classifyCode, name)
        if (count == 0) {
            return Page(page, pageSize, count.toLong(), result)
        }
        val records = atomDao.getInstalledAtoms(
            dslContext = dslContext,
            projectCode = projectCode,
            classifyCode = classifyCode,
            name = name,
            page = page,
            pageSize = pageSize
        )
        val atomCodeList = mutableListOf<String>()
        records?.forEach {
            atomCodeList.add(it[KEY_ATOM_CODE] as String)
        }

        // 插件关联的流水线
        val pipelineStat = client.get(ServiceMeasurePipelineResource::class).batchGetPipelineCountByAtomCode(
            atomCodes = atomCodeList.joinToString(","),
            projectCode = projectCode
        ).data
        val hasManagerPermission = hasManagerPermission(projectCode, userId)
        records?.forEach {
            val atomCode = it[KEY_ATOM_CODE] as String
            val installer = it[KEY_INSTALLER] as String
            val installType = it[KEY_INSTALL_TYPE] as Byte
            // 判断项目是否是初始化项目或者调试项目
            val isInitTest = installType == StoreProjectTypeEnum.INIT.type.toByte() ||
                installType == StoreProjectTypeEnum.TEST.type.toByte()
            val atomClassifyCode = it[KEY_CLASSIFY_CODE] as String
            val classifyName = it[KEY_CLASSIFY_NAME] as String
            val classifyLanName = I18nUtil.getCodeLanMessage(
                messageCode = "${StoreTypeEnum.ATOM.name}.classify.$atomClassifyCode",
                defaultMessage = classifyName
            )
            result.add(
                InstalledAtom(
                    atomId = it[KEY_ID] as String,
                    atomCode = atomCode,
                    version = it[KEY_VERSION] as String,
                    name = it[NAME] as String,
                    logoUrl = it[KEY_LOGO_URL] as? String,
                    classifyCode = atomClassifyCode,
                    classifyName = classifyLanName,
                    category = AtomCategoryEnum.getAtomCategory((it[KEY_CATEGORY] as Byte).toInt()),
                    summary = it[KEY_SUMMARY] as? String,
                    publisher = it[KEY_PUBLISHER] as? String,
                    installer = installer,
                    installTime = DateTimeUtil.toDateTime(it[KEY_INSTALL_TIME] as LocalDateTime),
                    installType = StoreProjectTypeEnum.getProjectType((it[KEY_INSTALL_TYPE] as Byte).toInt()),
                    pipelineCnt = pipelineStat?.get(atomCode) ?: 0,
                    hasPermission = !isInitTest && (hasManagerPermission || installer == userId)
                )
            )
        }
        return Page(page, pageSize, count.toLong(), result)
    }

    /**
     * 获取已安装的插件列表
     */
    override fun listInstalledAtomByProject(
        projectCode: String
    ): List<InstalledAtom> {

        // 获取已安装插件
        val records = atomDao.getInstalledAtoms(dslContext, projectCode)
        val atomCodeList = mutableListOf<String>()
        records?.forEach {
            atomCodeList.add(it[KEY_ATOM_CODE] as String)
        }
        val installAtoms = records?.map {
            val atomCode = it[KEY_ATOM_CODE] as String
            val installer = it[KEY_INSTALLER] as String
            val classifyCode = it[KEY_CLASSIFY_CODE] as String
            val classifyName = it[KEY_CLASSIFY_NAME] as String
            val classifyLanName = I18nUtil.getCodeLanMessage(
                messageCode = "${StoreTypeEnum.ATOM.name}.classify.$classifyCode",
                defaultMessage = classifyName
            )
            // 判断项目是否是初始化项目或者调试项目
            InstalledAtom(
                atomId = it[KEY_ID] as String,
                atomCode = atomCode,
                version = it[KEY_VERSION] as String,
                name = it[NAME] as String,
                logoUrl = it[KEY_LOGO_URL] as? String,
                classifyCode = classifyCode,
                classifyName = classifyLanName,
                category = AtomCategoryEnum.getAtomCategory((it[KEY_CATEGORY] as Byte).toInt()),
                summary = it[KEY_SUMMARY] as? String,
                publisher = it[KEY_PUBLISHER] as? String,
                installer = installer,
                installTime = DateTimeUtil.toDateTime(it[KEY_INSTALL_TIME] as LocalDateTime),
                installType = StoreProjectTypeEnum.getProjectType((it[KEY_INSTALL_TYPE] as Byte).toInt()),
                pipelineCnt = 0,
                hasPermission = true
            )
        } ?: listOf()

        // 获取自研插件
        val selfAtoms = atomDao.getSelfDevelopAtoms(dslContext)?.map {
            InstalledAtom(
                atomId = it.id,
                atomCode = it.atomCode,
                version = it[KEY_VERSION] as String,
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
            return I18nUtil.generateResponseDataObject(
                messageCode = PROJECT_NO_PERMISSION,
                params = arrayOf(projectCode, atomCode),
                language = I18nUtil.getLanguage(userId)
            )
        }

        // 是否还有流水线使用待卸载的插件
        val pipelineCnt = client.get(ServiceMeasurePipelineResource::class).getPipelineCountByAtomCode(
            atomCode = atomCode,
            projectCode = projectCode
        ).data
            ?: 0
        if (pipelineCnt > 0) {
            return I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_ATOM_USED,
                params = arrayOf(atomCode, projectCode),
                language = I18nUtil.getLanguage(userId)
            )
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

    override fun updateAtomBaseInfo(
        userId: String,
        atomCode: String,
        atomBaseInfoUpdateRequest: AtomBaseInfoUpdateRequest
    ): Result<Boolean> {
        logger.info("updateAtomBaseInfo userId:$userId,atomCode:$atomCode,updateRequest:$atomBaseInfoUpdateRequest")
        // 判断当前用户是否是该插件的成员
        if (!storeMemberDao.isStoreMember(dslContext, userId, atomCode, StoreTypeEnum.ATOM.type.toByte())) {
            return I18nUtil.generateResponseDataObject(
                messageCode = GET_INFO_NO_PERMISSION,
                params = arrayOf(atomCode),
                language = I18nUtil.getLanguage(atomCode)
            )
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
        val updateRepoInfoResult = updateRepoInfo(
            visibilityLevel = visibilityLevel,
            dbVisibilityLevel = dbVisibilityLevel,
            userId = userId,
            repositoryHashId = newestAtomRecord.repositoryHashId
        )
        if (updateRepoInfoResult.isNotOk()) {
            return updateRepoInfoResult
        }
        val atomIdList = mutableListOf(newestAtomRecord.id)
        val latestAtomRecord = atomDao.getLatestAtomByCode(dslContext, atomCode)
        if (null != latestAtomRecord) {
            atomIdList.add(latestAtomRecord.id)
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            atomDao.updateAtomBaseInfo(context, userId, atomIdList, atomBaseInfoUpdateRequest)
            // 更新标签信息
            val labelIdList = atomBaseInfoUpdateRequest.labelIdList
            atomIdList.forEach { atomId ->
                if (labelIdList?.isNotEmpty() == true) {
                    atomLabelRelDao.deleteByAtomId(context, atomId)
                    atomLabelRelDao.batchAdd(context, userId, atomId, labelIdList)
                }
                marketAtomCommonService.updateAtomRunInfoCache(
                    atomId = atomId,
                    atomName = atomBaseInfoUpdateRequest.name
                )
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

    override fun getAtomRealVersion(projectCode: String, atomCode: String, version: String): Result<String?> {
        return if (VersionUtils.isLatestVersion(version)) {
            // 获取插件真实的版本号
            val atomStatusList = generateAtomStatusList(atomCode, projectCode)
            atomStatusList.add(AtomStatusEnum.UNDERCARRIAGED.status.toByte())
            val realVersion = atomDao.getAtomRealVersion(
                dslContext = dslContext,
                projectCode = projectCode,
                atomCode = atomCode,
                version = version,
                defaultFlag = marketAtomCommonService.isPublicAtom(atomCode),
                atomStatusList = atomStatusList
            )
            Result(realVersion)
        } else {
            Result(version)
        }
    }

    override fun getAtomDefaultValidVersion(projectCode: String, atomCode: String): Result<VersionInfo?> {
        val defaultFlag = marketAtomCommonService.isPublicAtom(atomCode)
        val defaultVersionRecord = atomDao.getVersionsByAtomCode(
            dslContext = dslContext,
            projectCode = projectCode,
            atomCode = atomCode,
            defaultFlag = defaultFlag,
            atomStatusList = generateAtomStatusList(atomCode, projectCode),
            limitNum = 1
        )?.getOrNull(0)
        val versionInfo = defaultVersionRecord?.let {
            val version = it[KEY_VERSION] as String
            VersionInfo(
                versionName = VersionUtils.convertLatestVersionName(version),
                versionValue = VersionUtils.convertLatestVersion(version)
            )
        }
        return Result(versionInfo)
    }
}

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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.artifactory.api.ServiceArchiveAtomResource
import com.tencent.devops.common.api.constant.AND
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.DANG
import com.tencent.devops.common.api.constant.DEFAULT
import com.tencent.devops.common.api.constant.INIT_VERSION
import com.tencent.devops.common.api.constant.MULTIPLE_SELECTOR
import com.tencent.devops.common.api.constant.NO_LABEL
import com.tencent.devops.common.api.constant.OPTIONS
import com.tencent.devops.common.api.constant.OR
import com.tencent.devops.common.api.constant.OUTPUT_DESC
import com.tencent.devops.common.api.constant.REQUIRED
import com.tencent.devops.common.api.constant.SINGLE_SELECTOR
import com.tencent.devops.common.api.constant.TIMETOSELECT
import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.AtomBaseInfo
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.common.util.RegexUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.store.tables.TAtom
import com.tencent.devops.model.store.tables.TAtomEnvInfo
import com.tencent.devops.model.store.tables.TAtomFeature
import com.tencent.devops.model.store.tables.TAtomVersionLog
import com.tencent.devops.model.store.tables.TClassify
import com.tencent.devops.model.store.tables.records.TAtomRecord
import com.tencent.devops.process.api.service.ServiceMeasurePipelineResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.constant.StoreMessageCode.NO_COMPONENT_ADMIN_PERMISSION
import com.tencent.devops.store.constant.StoreMessageCode.GET_INFO_NO_PERMISSION
import com.tencent.devops.store.constant.StoreMessageCode.TASK_JSON_CONFIGURE_FORMAT_ERROR
import com.tencent.devops.store.dao.atom.AtomApproveRelDao
import com.tencent.devops.store.dao.atom.AtomDao
import com.tencent.devops.store.dao.atom.AtomLabelRelDao
import com.tencent.devops.store.dao.atom.MarketAtomClassifyDao
import com.tencent.devops.store.dao.atom.MarketAtomDao
import com.tencent.devops.store.dao.atom.MarketAtomEnvInfoDao
import com.tencent.devops.store.dao.atom.MarketAtomFeatureDao
import com.tencent.devops.store.dao.atom.MarketAtomVersionLogDao
import com.tencent.devops.store.dao.common.StoreBuildInfoDao
import com.tencent.devops.store.dao.common.StoreErrorCodeInfoDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.pojo.atom.AtomDevLanguage
import com.tencent.devops.store.pojo.atom.AtomOutput
import com.tencent.devops.store.pojo.atom.AtomPostInfo
import com.tencent.devops.store.pojo.atom.AtomPostReqItem
import com.tencent.devops.store.pojo.atom.AtomPostResp
import com.tencent.devops.store.pojo.atom.AtomVersion
import com.tencent.devops.store.pojo.atom.AtomVersionListItem
import com.tencent.devops.store.pojo.atom.GetRelyAtom
import com.tencent.devops.store.pojo.atom.InstallAtomReq
import com.tencent.devops.store.pojo.atom.MarketAtomResp
import com.tencent.devops.store.pojo.atom.MarketMainItem
import com.tencent.devops.store.pojo.atom.MarketMainItemLabel
import com.tencent.devops.store.pojo.atom.MyAtomResp
import com.tencent.devops.store.pojo.atom.MyAtomRespItem
import com.tencent.devops.store.pojo.atom.enums.AtomCategoryEnum
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.atom.enums.AtomTypeEnum
import com.tencent.devops.store.pojo.atom.enums.MarketAtomSortTypeEnum
import com.tencent.devops.store.pojo.common.ATOM_OUTPUT
import com.tencent.devops.store.pojo.common.ATOM_POST_NORMAL_PROJECT_FLAG_KEY_PREFIX
import com.tencent.devops.store.pojo.common.ERROR_JSON_NAME
import com.tencent.devops.store.pojo.common.HOTTEST
import com.tencent.devops.store.pojo.common.KEY_CLASSIFY_CODE
import com.tencent.devops.store.pojo.common.KEY_CLASSIFY_NAME
import com.tencent.devops.store.pojo.common.KEY_STORE_CODE
import com.tencent.devops.store.pojo.common.LATEST
import com.tencent.devops.store.pojo.common.MarketItem
import com.tencent.devops.store.pojo.common.StoreDailyStatistic
import com.tencent.devops.store.pojo.common.StoreErrorCodeInfo
import com.tencent.devops.store.pojo.common.StoreShowVersionInfo
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.AtomLabelService
import com.tencent.devops.store.service.atom.MarketAtomCommonService
import com.tencent.devops.store.service.atom.MarketAtomEnvService
import com.tencent.devops.store.service.atom.MarketAtomService
import com.tencent.devops.store.service.common.ClassifyService
import com.tencent.devops.store.service.common.StoreCommentService
import com.tencent.devops.store.service.common.StoreCommonService
import com.tencent.devops.store.service.common.StoreDailyStatisticService
import com.tencent.devops.store.service.common.StoreHonorService
import com.tencent.devops.store.service.common.StoreI18nMessageService
import com.tencent.devops.store.service.common.StoreIndexManageService
import com.tencent.devops.store.service.common.StoreProjectService
import com.tencent.devops.store.service.common.StoreTotalStatisticService
import com.tencent.devops.store.service.common.StoreUserService
import com.tencent.devops.store.service.websocket.StoreWebsocketService
import com.tencent.devops.store.utils.StoreUtils
import java.time.LocalDateTime
import java.util.Calendar
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

@Suppress("ALL")
abstract class MarketAtomServiceImpl @Autowired constructor() : MarketAtomService {

    @Autowired
    lateinit var dslContext: DSLContext

    @Autowired
    lateinit var atomDao: AtomDao

    @Autowired
    lateinit var marketAtomDao: MarketAtomDao

    @Autowired
    lateinit var storeProjectRelDao: StoreProjectRelDao

    @Autowired
    lateinit var storeMemberDao: StoreMemberDao

    @Autowired
    lateinit var storeBuildInfoDao: StoreBuildInfoDao

    @Autowired
    lateinit var marketAtomEnvInfoDao: MarketAtomEnvInfoDao

    @Autowired
    lateinit var marketAtomClassifyDao: MarketAtomClassifyDao

    @Autowired
    lateinit var marketAtomFeatureDao: MarketAtomFeatureDao

    @Autowired
    lateinit var marketAtomVersionLogDao: MarketAtomVersionLogDao

    @Autowired
    lateinit var atomApproveRelDao: AtomApproveRelDao

    @Autowired
    lateinit var atomLabelRelDao: AtomLabelRelDao

    @Autowired
    lateinit var storeErrorCodeInfoDao: StoreErrorCodeInfoDao

    @Autowired
    lateinit var storeHonorService: StoreHonorService

    @Autowired
    lateinit var storeIndexManageService: StoreIndexManageService

    @Autowired
    lateinit var storeTotalStatisticService: StoreTotalStatisticService

    @Autowired
    lateinit var atomLabelService: AtomLabelService

    @Autowired
    lateinit var storeProjectService: StoreProjectService

    @Autowired
    lateinit var storeUserService: StoreUserService

    @Autowired
    lateinit var atomMemberService: AtomMemberServiceImpl

    @Autowired
    lateinit var storeCommentService: StoreCommentService

    @Autowired
    lateinit var classifyService: ClassifyService

    @Autowired
    lateinit var storeWebsocketService: StoreWebsocketService

    @Autowired
    lateinit var storeCommonService: StoreCommonService

    @Autowired
    lateinit var marketAtomCommonService: MarketAtomCommonService

    @Autowired
    lateinit var marketAtomEnvService: MarketAtomEnvService

    @Autowired
    lateinit var storeDailyStatisticService: StoreDailyStatisticService

    @Autowired
    lateinit var storeI18nMessageService: StoreI18nMessageService

    @Autowired
    lateinit var redisOperation: RedisOperation

    @Autowired
    lateinit var client: Client

    @Value("\${store.defaultAtomErrorCodeLength:6}")
    private var defaultAtomErrorCodeLength: Int = 6

    @Value("\${store.defaultAtomErrorCodePrefix:8}")
    private lateinit var defaultAtomErrorCodePrefix: String

    companion object {
        private val logger = LoggerFactory.getLogger(MarketAtomServiceImpl::class.java)
        private val executor = Executors.newFixedThreadPool(30)
    }

    @Suppress("UNCHECKED_CAST", "LongParameterList", "LongMethod")
    private fun doList(
        userId: String,
        userDeptList: List<Int>,
        keyword: String?,
        classifyCode: String?,
        labelCode: String?,
        score: Int?,
        rdType: AtomTypeEnum?,
        yamlFlag: Boolean?,
        recommendFlag: Boolean?,
        qualityFlag: Boolean?,
        sortType: MarketAtomSortTypeEnum?,
        desc: Boolean?,
        page: Int?,
        pageSize: Int?,
        urlProtocolTrim: Boolean = false
    ): Future<MarketAtomResp> {
        return executor.submit(Callable<MarketAtomResp> {
            val results = mutableListOf<MarketItem>()
            // 获取插件
            val labelCodeList = if (labelCode.isNullOrEmpty()) listOf() else labelCode.split(",")
            val count = marketAtomDao.count(
                dslContext = dslContext,
                keyword = keyword,
                classifyCode = classifyCode,
                labelCodeList = labelCodeList,
                score = score,
                rdType = rdType,
                yamlFlag = yamlFlag,
                recommendFlag = recommendFlag,
                qualityFlag = qualityFlag
            )
            val atoms = marketAtomDao.list(
                dslContext = dslContext,
                keyword = keyword,
                classifyCode = classifyCode,
                labelCodeList = labelCodeList,
                score = score,
                rdType = rdType,
                yamlFlag = yamlFlag,
                recommendFlag = recommendFlag,
                qualityFlag = qualityFlag,
                sortType = sortType,
                desc = desc,
                page = page,
                pageSize = pageSize
            )
                ?: return@Callable MarketAtomResp(0, page, pageSize, results)
            val tAtom = TAtom.T_ATOM
            val tAtomFeature = TAtomFeature.T_ATOM_FEATURE
            val atomCodeList = atoms.map {
                it[tAtom.ATOM_CODE] as String
            }.toList()
            // 获取可见范围
            val storeType = StoreTypeEnum.ATOM
            val atomVisibleData = storeCommonService.generateStoreVisibleData(atomCodeList, storeType)
            val atomStatisticData = storeTotalStatisticService.getStatisticByCodeList(
                storeType = storeType.type.toByte(),
                storeCodeList = atomCodeList
            )
            val atomHonorInfoMap = storeHonorService.getHonorInfosByStoreCodes(storeType, atomCodeList)
            val atomIndexInfosMap = storeIndexManageService.getStoreIndexInfosByStoreCodes(storeType, atomCodeList)
            // 获取用户
            val memberData = atomMemberService.batchListMember(atomCodeList, storeType).data

            // 获取分类
            val classifyList = classifyService.getAllClassify(storeType.type.toByte()).data
            val classifyMap = mutableMapOf<String, String>()
            classifyList?.forEach {
                classifyMap[it.id] = it.classifyCode
            }

            atoms.forEach {
                val atomCode = it[tAtom.ATOM_CODE] as String
                val visibleList = atomVisibleData?.get(atomCode)
                val statistic = atomStatisticData[atomCode]
                val atomHonorInfos = atomHonorInfoMap[atomCode]
                val atomIndexInfos = atomIndexInfosMap[atomCode]
                val members = memberData?.get(atomCode)
                val defaultFlag = it[tAtom.DEFAULT_FLAG] as Boolean
                val flag = storeCommonService.generateInstallFlag(defaultFlag = defaultFlag,
                    members = members,
                    userId = userId,
                    visibleList = visibleList,
                    userDeptList = userDeptList)
                val classifyId = it[tAtom.CLASSIFY_ID] as String
                var logoUrl = it[tAtom.LOGO_URL]
                logoUrl = if (logoUrl?.contains("?") == true) {
                    logoUrl.plus("&logo=true")
                } else {
                    logoUrl?.plus("?logo=true")
                }
                if (urlProtocolTrim) { // #4796 LogoUrl跟随主站协议
                    logoUrl = RegexUtils.trimProtocol(logoUrl)
                }
                val osStr = it[tAtom.OS]
                results.add(
                    MarketItem(
                        id = it[tAtom.ID] as String,
                        name = it[tAtom.NAME] as String,
                        code = atomCode,
                        version = it[tAtom.VERSION] as String,
                        type = it[tAtom.JOB_TYPE] as String,
                        rdType = AtomTypeEnum.getAtomType((it[tAtom.ATOM_TYPE] as Byte).toInt()),
                        classifyCode = if (classifyMap.containsKey(classifyId)) classifyMap[classifyId] else "",
                        category = AtomCategoryEnum.getAtomCategory((it[tAtom.CATEGROY] as Byte).toInt()),
                        logoUrl = logoUrl,
                        publisher = it[tAtom.PUBLISHER] as String,
                        os = if (!osStr.isNullOrBlank()) JsonUtil.getObjectMapper().readValue(
                            osStr,
                            List::class.java
                        ) as List<String> else null,
                        downloads = statistic?.downloads ?: 0,
                        score = statistic?.score ?: 0.toDouble(),
                        summary = it[tAtom.SUMMARY],
                        flag = flag,
                        publicFlag = it[tAtom.DEFAULT_FLAG] as Boolean,
                        buildLessRunFlag = if (it[tAtom.BUILD_LESS_RUN_FLAG] == null) {
                            false
                        } else it[tAtom.BUILD_LESS_RUN_FLAG] as Boolean,
                        docsLink = if (it[tAtom.DOCS_LINK] == null) "" else it[tAtom.DOCS_LINK] as String,
                        modifier = it[tAtom.MODIFIER] as String,
                        updateTime = DateTimeUtil.toDateTime(it[tAtom.UPDATE_TIME] as LocalDateTime),
                        recommendFlag = it[tAtomFeature.RECOMMEND_FLAG],
                        yamlFlag = it[tAtomFeature.YAML_FLAG],
                        recentExecuteNum = statistic?.recentExecuteNum ?: 0,
                        indexInfos = atomIndexInfos,
                        honorInfos = atomHonorInfos,
                        hotFlag = statistic?.hotFlag
                    )
                )
            }

            return@Callable MarketAtomResp(count, page, pageSize, results)
        })
    }

    /**
     * 插件市场，首页
     */
    @BkTimed(extraTags = ["web_operation", "mainPageList"], value = "store_web_operation")
    override fun mainPageList(
        userId: String,
        page: Int?,
        pageSize: Int?,
        urlProtocolTrim: Boolean
    ): Result<List<MarketMainItem>> {
        val result = mutableListOf<MarketMainItem>()
        // 获取用户组织架构
        val userDeptList = storeUserService.getUserDeptList(userId)
        val futureList = mutableListOf<Future<MarketAtomResp>>()
        val labelInfoList = mutableListOf<MarketMainItemLabel>()
        labelInfoList.add(
            MarketMainItemLabel(LATEST, I18nUtil.getCodeLanMessage(LATEST))
        )
        futureList.add(
            doList(
                userId = userId,
                userDeptList = userDeptList,
                keyword = null,
                classifyCode = null,
                labelCode = null,
                score = null,
                rdType = null,
                yamlFlag = null,
                recommendFlag = null,
                qualityFlag = null,
                sortType = MarketAtomSortTypeEnum.UPDATE_TIME,
                desc = true,
                page = page,
                pageSize = pageSize,
                urlProtocolTrim = urlProtocolTrim
            )
        )
        labelInfoList.add(
            MarketMainItemLabel(
                HOTTEST,
                I18nUtil.getCodeLanMessage(HOTTEST)
            )
        )
        futureList.add(
            doList(
                userId = userId,
                userDeptList = userDeptList,
                keyword = null,
                classifyCode = null,
                labelCode = null,
                score = null,
                rdType = null,
                yamlFlag = null,
                recommendFlag = null,
                qualityFlag = null,
                sortType = MarketAtomSortTypeEnum.RECENT_EXECUTE_NUM,
                desc = true,
                page = page,
                pageSize = pageSize,
                urlProtocolTrim = urlProtocolTrim
            )
        )

        val classifyList = marketAtomClassifyDao.getAllAtomClassify(dslContext)
        classifyList?.forEach {
            val classifyCode = it[KEY_CLASSIFY_CODE] as String
            if (classifyCode != "trigger") {
                val classifyName = it[KEY_CLASSIFY_NAME] as String
                val classifyLanName = I18nUtil.getCodeLanMessage(
                    messageCode = "${StoreTypeEnum.ATOM.name}.classify.$classifyCode",
                    defaultMessage = classifyName,
                    language = I18nUtil.getLanguage(userId)
                )
                labelInfoList.add(MarketMainItemLabel(classifyCode, classifyLanName))
                futureList.add(
                    doList(
                        userId = userId,
                        userDeptList = userDeptList,
                        keyword = null,
                        classifyCode = classifyCode,
                        labelCode = null,
                        score = null,
                        rdType = null,
                        yamlFlag = null,
                        recommendFlag = null,
                        qualityFlag = null,
                        sortType = MarketAtomSortTypeEnum.RECENT_EXECUTE_NUM,
                        desc = true,
                        page = page,
                        pageSize = pageSize,
                        urlProtocolTrim = urlProtocolTrim
                    )
                )
            }
        }
        for (index in futureList.indices) {
            val labelInfo = labelInfoList[index]
            result.add(
                MarketMainItem(
                    key = labelInfo.key,
                    label = labelInfo.label,
                    records = futureList[index].get().records
                )
            )
        }
        return Result(result)
    }

    /**
     * 插件市场，查询插件列表
     */
    @BkTimed(extraTags = ["web_operation", "getAtomList"], value = "store_web_operation")
    override fun list(
        userId: String,
        keyword: String?,
        classifyCode: String?,
        labelCode: String?,
        score: Int?,
        rdType: AtomTypeEnum?,
        yamlFlag: Boolean?,
        recommendFlag: Boolean?,
        qualityFlag: Boolean?,
        sortType: MarketAtomSortTypeEnum?,
        page: Int?,
        pageSize: Int?,
        urlProtocolTrim: Boolean
    ): MarketAtomResp {
        logger.info("[list]enter")

        // 获取用户组织架构
        val userDeptList = storeUserService.getUserDeptList(userId)
        logger.info("[list]get userDeptList:$userDeptList")

        return doList(
            userId = userId,
            userDeptList = userDeptList,
            keyword = keyword,
            classifyCode = classifyCode,
            labelCode = labelCode,
            score = score,
            rdType = rdType,
            sortType = sortType,
            yamlFlag = yamlFlag,
            recommendFlag = recommendFlag,
            qualityFlag = qualityFlag,
            desc = true,
            page = page,
            pageSize = pageSize,
            urlProtocolTrim = urlProtocolTrim
        ).get()
    }

    /**
     * 根据用户和插件名称获取插件信息
     */
    override fun getMyAtoms(
        accessToken: String,
        userId: String,
        atomName: String?,
        page: Int,
        pageSize: Int
    ): Result<MyAtomResp?> {
        logger.info("getMyAtoms params:[$userId|$atomName|$page|$pageSize]")
        // 获取有权限的插件代码列表
        val records = marketAtomDao.getMyAtoms(dslContext, userId, atomName, page, pageSize)
        val count = marketAtomDao.countMyAtoms(dslContext, userId, atomName)
        // 获取项目ID对应的名称
        val projectCodeList = mutableListOf<String>()
        val atomCodeList = mutableListOf<String>()
        val atomProjectMap = mutableMapOf<String, String>()
        val tAtom = TAtom.T_ATOM
        val tAtomEnvInfo = TAtomEnvInfo.T_ATOM_ENV_INFO
        records?.forEach {
            val atomCode = it[tAtom.ATOM_CODE] as String
            atomCodeList.add(atomCode)
            val testProjectCode = storeProjectRelDao.getUserStoreTestProjectCode(
                dslContext = dslContext,
                userId = userId,
                storeCode = atomCode,
                storeType = StoreTypeEnum.ATOM
            )
            if (null != testProjectCode) {
                projectCodeList.add(testProjectCode)
                atomProjectMap[atomCode] = testProjectCode
            }
        }
        val projectMap = client.get(ServiceProjectResource::class).getNameByCode(projectCodeList.joinToString(",")).data
        val processingAtomRecords = marketAtomDao.getAtomsByConditions(
            dslContext = dslContext,
            atomCodeList = atomCodeList,
            atomStatusList = AtomStatusEnum.getProcessingStatusList()
        )
        // 获取插件处于流程中的版本信息
        var processingVersionInfoMap: MutableMap<String, MutableList<AtomBaseInfo>>? = null
        processingAtomRecords?.forEach { processingAtomRecord ->
            if (processingAtomRecord.version == INIT_VERSION || processingAtomRecord.version.isNullOrBlank()) {
                return@forEach
            }
            if (processingVersionInfoMap == null) {
                processingVersionInfoMap = mutableMapOf()
            }
            val atomCode = processingAtomRecord.atomCode
            val atomBaseInfo = AtomBaseInfo(
                atomId = processingAtomRecord.id,
                atomCode = atomCode,
                version = processingAtomRecord.version,
                atomStatus = AtomStatusEnum.getAtomStatus(processingAtomRecord.atomStatus.toInt())
            )
            if (processingVersionInfoMap!!.containsKey(atomCode)) {
                val atomBaseInfoList = processingVersionInfoMap!![atomCode]
                atomBaseInfoList?.add(atomBaseInfo)
            } else {
                processingVersionInfoMap!![atomCode] = mutableListOf(atomBaseInfo)
            }
        }
        val myAtoms = mutableListOf<MyAtomRespItem?>()
        records?.forEach {
            val atomCode = it[tAtom.ATOM_CODE] as String
            var releaseFlag = false // 是否有处于上架状态的插件插件版本
            val releaseAtomNum = marketAtomDao.countReleaseAtomByCode(dslContext, atomCode)
            if (releaseAtomNum > 0) {
                releaseFlag = true
            }
            myAtoms.add(
                MyAtomRespItem(
                    atomId = it[tAtom.ID] as String,
                    name = it[tAtom.NAME] as String,
                    atomCode = atomCode,
                    language = it[tAtomEnvInfo.LANGUAGE],
                    category = AtomCategoryEnum.getAtomCategory((it[tAtom.CATEGROY] as Byte).toInt()),
                    logoUrl = it[tAtom.LOGO_URL],
                    version = it[tAtom.VERSION] as String,
                    atomStatus = AtomStatusEnum.getAtomStatus((it[tAtom.ATOM_STATUS] as Byte).toInt()),
                    projectName = projectMap?.get(atomProjectMap[atomCode]) ?: "",
                    releaseFlag = releaseFlag,
                    creator = it[tAtom.CREATOR] as String,
                    modifier = it[tAtom.MODIFIER] as String,
                    createTime = DateTimeUtil.toDateTime(it[tAtom.CREATE_TIME] as LocalDateTime),
                    updateTime = DateTimeUtil.toDateTime(it[tAtom.UPDATE_TIME] as LocalDateTime),
                    processingVersionInfos = processingVersionInfoMap?.get(atomCode)
                )
            )
        }
        return Result(MyAtomResp(count, page, pageSize, myAtoms))
    }

    /**
     * 根据插件版本ID获取版本基本信息、发布信息
     */
    @Suppress("UNCHECKED_CAST")
    override fun getAtomById(atomId: String, userId: String): Result<AtomVersion?> {
        return getAtomVersion(atomId, userId)
    }

    /**
     * 根据插件标识获取插件回显版本信息
     */
    override fun getAtomShowVersionInfo(userId: String, atomCode: String): Result<StoreShowVersionInfo> {
        val record = marketAtomDao.getNewestAtomByCode(dslContext, atomCode) ?: throw ErrorCodeException(
            errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
            params = arrayOf(atomCode)
        )
        val cancelFlag = record.atomStatus == AtomStatusEnum.GROUNDING_SUSPENSION.status.toByte()
        val showVersion = if (cancelFlag) {
            record.version
        } else {
            marketAtomDao.getMaxVersionAtomByCode(dslContext, atomCode)?.version
        }
        val releaseType = if (record.atomStatus == AtomStatusEnum.INIT.status.toByte()) {
            null
        } else {
            marketAtomVersionLogDao.getAtomVersion(dslContext, record.id).releaseType
        }
        val showReleaseType = if (releaseType != null) {
            ReleaseTypeEnum.getReleaseTypeObj(releaseType.toInt())
        } else {
            null
        }
        val showVersionInfo = storeCommonService.getStoreShowVersionInfo(cancelFlag, showReleaseType, showVersion)
        return Result(showVersionInfo)
    }

    override fun updateAtomErrorCodeInfo(
        userId: String,
        projectCode: String,
        storeErrorCodeInfo: StoreErrorCodeInfo
    ): Result<Boolean> {
        val atomCode = storeErrorCodeInfo.storeCode ?: throw ErrorCodeException(
            errorCode = CommonMessageCode.PARAMETER_IS_NULL,
            params = arrayOf(KEY_STORE_CODE)
        )
        val isStoreMember = storeMemberDao.isStoreMember(
            dslContext = dslContext,
            userId = userId,
            storeCode = atomCode,
            storeType = StoreTypeEnum.ATOM.type.toByte()
        )
        if (!isStoreMember) {
            return I18nUtil.generateResponseDataObject(
                messageCode = GET_INFO_NO_PERMISSION,
                params = arrayOf(atomCode),
                language = I18nUtil.getLanguage(userId)
            )
        }
        val errorCodes = storeErrorCodeInfo.errorCodes
        // 校验code码是否符合插件自定义错误码规范
        errorCodes.forEach { errorCode ->
            val errorCodeStr = errorCode.toString()
            if (errorCodeStr.length != defaultAtomErrorCodeLength ||
                (!errorCodeStr.startsWith(defaultAtomErrorCodePrefix))
            ) {
                throw ErrorCodeException(errorCode = StoreMessageCode.USER_REPOSITORY_ERROR_JSON_FIELD_IS_INVALID)
            }
        }
        val errorJsonStr = JsonUtil.toJson(storeErrorCodeInfo.errorCodes)
        // 修改插件error.json文件内容
        val updateAtomFileContentResult = updateAtomFileContent(
            userId = userId,
            projectCode = projectCode,
            atomCode = atomCode,
            content = errorJsonStr,
            filePath = ERROR_JSON_NAME
        )
        if (updateAtomFileContentResult.isNotOk()) {
            return updateAtomFileContentResult
        }
        // 文件内容修改成功，同步到数据库
        storeErrorCodeInfoDao.batchUpdateErrorCodeInfo(
            dslContext = dslContext,
            userId = userId,
            storeErrorCodeInfo = storeErrorCodeInfo
        )
        val dbErrorCodes = storeErrorCodeInfoDao.getStoreErrorCodes(
            dslContext = dslContext, storeCode = atomCode, storeType = StoreTypeEnum.ATOM
        ).toMutableSet()
        dbErrorCodes.removeAll(errorCodes)
        if (dbErrorCodes.isNotEmpty()) {
            storeErrorCodeInfoDao.batchDeleteErrorCodeInfo(
                dslContext = dslContext,
                storeCode = atomCode,
                storeType = StoreTypeEnum.ATOM,
                errorCodes = dbErrorCodes
            )
        }
        return Result(true)
    }

    @Suppress("UNCHECKED_CAST")
    @BkTimed(extraTags = ["web_operation", "getAtomVersion"], value = "store_web_operation")
    private fun getAtomVersion(atomId: String, userId: String): Result<AtomVersion?> {
        val record = marketAtomDao.getAtomById(dslContext, atomId)
        return if (null == record) {
            Result(data = null)
        } else {
            val tAtom = TAtom.T_ATOM
            val tAtomVersionLog = TAtomVersionLog.T_ATOM_VERSION_LOG
            val tClassify = TClassify.T_CLASSIFY
            val atomCode = record[tAtom.ATOM_CODE] as String
            val defaultFlag = record[tAtom.DEFAULT_FLAG] as Boolean
            val htmlTemplateVersion = record[tAtom.HTML_TEMPLATE_VERSION] as String
            val projectCode = if (htmlTemplateVersion == FrontendTypeEnum.HISTORY.typeVersion) {
                ""
            } else {
                storeProjectRelDao.getInitProjectCodeByStoreCode(
                    dslContext = dslContext,
                    storeCode = atomCode,
                    storeType = StoreTypeEnum.ATOM.type.toByte()
                )
            }
            val repositoryHashId = record[tAtom.REPOSITORY_HASH_ID]
            val repositoryInfoResult = getRepositoryInfo(projectCode, repositoryHashId)
            if (repositoryInfoResult.isNotOk()) {
                Result(repositoryInfoResult.status, repositoryInfoResult.message, null)
            }
            val repositoryInfo = repositoryInfoResult.data
            val flag = storeUserService.isCanInstallStoreComponent(defaultFlag, userId, atomCode, StoreTypeEnum.ATOM)
            val labelList = atomLabelService.getLabelsByAtomId(atomId) // 查找标签列表
            val userCommentInfo = storeCommentService.getStoreUserCommentInfo(userId, atomCode, StoreTypeEnum.ATOM)
            val feature = marketAtomFeatureDao.getAtomFeature(dslContext, atomCode)
            val classifyCode = record[tClassify.CLASSIFY_CODE]
            val classifyName = record[tClassify.CLASSIFY_NAME]
            val classifyLanName = if (classifyCode != null) {
                I18nUtil.getCodeLanMessage(
                    messageCode = "${StoreTypeEnum.ATOM.name}.classify.$classifyCode",
                    defaultMessage = classifyName
                )
            } else classifyName
            val releaseType = if (record[tAtomVersionLog.RELEASE_TYPE] != null) {
                ReleaseTypeEnum.getReleaseTypeObj((record[tAtomVersionLog.RELEASE_TYPE] as Byte).toInt())
            } else null
            val atomStatus = AtomStatusEnum.getAtomStatus((record[tAtom.ATOM_STATUS] as Byte).toInt())
            val version = record[tAtom.VERSION]
            val osStr = record[tAtom.OS]
            val defaultAtomEnvRecord = marketAtomEnvInfoDao.getDefaultAtomEnvInfo(dslContext, atomId)
            Result(
                AtomVersion(
                    atomId = atomId,
                    atomCode = atomCode,
                    name = record[tAtom.NAME] as String,
                    logoUrl = record[tAtom.LOGO_URL],
                    classifyCode = classifyCode,
                    classifyName = classifyLanName,
                    category = AtomCategoryEnum.getAtomCategory((record[tAtom.CATEGROY] as Byte).toInt()),
                    docsLink = record[tAtom.DOCS_LINK],
                    htmlTemplateVersion = htmlTemplateVersion,
                    atomType = AtomTypeEnum.getAtomType((record[tAtom.ATOM_TYPE] as Byte).toInt()),
                    jobType = record[tAtom.JOB_TYPE],
                    os = if (!osStr.isNullOrBlank()) JsonUtil.getObjectMapper().readValue(
                        osStr,
                        List::class.java
                    ) as List<String> else null,
                    summary = record[tAtom.SUMMARY],
                    description = record[tAtom.DESCRIPTION],
                    version = version,
                    atomStatus = atomStatus,
                    releaseType = releaseType?.name,
                    versionContent = record[tAtomVersionLog.CONTENT],
                    language = defaultAtomEnvRecord?.language,
                    codeSrc = record[tAtom.CODE_SRC],
                    publisher = record[tAtom.PUBLISHER] as String,
                    modifier = record[tAtom.MODIFIER] as String,
                    creator = record[tAtom.CREATOR] as String,
                    createTime = DateTimeUtil.toDateTime(record[tAtom.CREATE_TIME] as LocalDateTime),
                    updateTime = DateTimeUtil.toDateTime(record[tAtom.UPDATE_TIME] as LocalDateTime),
                    flag = flag,
                    repositoryAuthorizer = repositoryInfo?.userName,
                    defaultFlag = defaultFlag,
                    projectCode = storeProjectRelDao.getUserStoreTestProjectCode(
                        dslContext = dslContext,
                        userId = userId,
                        storeCode = atomCode,
                        storeType = StoreTypeEnum.ATOM
                    ),
                    initProjectCode = projectCode,
                    labelList = labelList ?: emptyList(),
                    userCommentInfo = userCommentInfo,
                    visibilityLevel = VisibilityLevelEnum.getVisibilityLevel(record[tAtom.VISIBILITY_LEVEL] as Int),
                    privateReason = record[tAtom.PRIVATE_REASON],
                    recommendFlag = feature?.recommendFlag,
                    frontendType = FrontendTypeEnum.getFrontendTypeObj(htmlTemplateVersion),
                    // 开启插件yml显示
                    yamlFlag = true,
                    dailyStatisticList = getRecentDailyStatisticList(atomCode),
                    editFlag = marketAtomCommonService.checkEditCondition(atomCode),
                    honorInfos = storeHonorService.getStoreHonor(userId, StoreTypeEnum.ATOM, atomCode),
                    indexInfos = storeIndexManageService.getStoreIndexInfosByStoreCode(StoreTypeEnum.ATOM, atomCode)
                )
            )
        }
    }

    private fun getRecentDailyStatisticList(
        atomCode: String
    ): List<StoreDailyStatistic>? {
        // 统计昨天为截止日期的最近一周的数据
        val endTime = DateTimeUtil.convertDateToFormatLocalDateTime(
            date = DateTimeUtil.getFutureDateFromNow(Calendar.DAY_OF_MONTH, -1),
            format = "yyyy-MM-dd"
        )
        return storeDailyStatisticService.getDailyStatisticListByCode(
            storeCode = atomCode,
            storeType = StoreTypeEnum.ATOM.type.toByte(),
            startTime = DateTimeUtil.convertDateToLocalDateTime(
                DateTimeUtil.getFutureDate(
                    localDateTime = endTime,
                    unit = Calendar.DAY_OF_MONTH,
                    timeSpan = -6
                )
            ),
            endTime = endTime
        )
    }

    abstract fun getRepositoryInfo(projectCode: String?, repositoryHashId: String?): Result<Repository?>

    /**
     * 根据插件标识获取插件最新、正式版本息
     */
    override fun getAtomByCode(userId: String, atomCode: String): Result<AtomVersion?> {
        val record = marketAtomDao.getLatestAtomByCode(dslContext, atomCode)
        return (if (null == record) {
            Result(data = null)
        } else {
            getAtomVersion(record.id, userId)
        })
    }

    /**
     * 根据标识获取最新版本信息（若最新版本为测试中，取最新版本，否则取最新正式版本）
     */
    override fun getNewestAtomByCode(userId: String, atomCode: String): Result<AtomVersion?> {
        val newest = marketAtomDao.getNewestAtomByCode(dslContext, atomCode)
        val latest = marketAtomDao.getLatestAtomByCode(dslContext, atomCode)
        return if (null == newest || null == latest) {
            I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(atomCode),
                language = I18nUtil.getLanguage(userId)
            )
        } else {
            val record = if (latest.id != newest.id &&
                (newest.atomStatus as Byte).toInt() == AtomStatusEnum.TESTING.status
            ) {
                newest
            } else {
                latest
            }
            getAtomVersion(record.id, userId)
        }
    }

    /**
     * 安装插件到项目
     */
    @BkTimed(extraTags = ["web_operation", "installAtom"], value = "store_web_operation")
    override fun installAtom(
        accessToken: String,
        userId: String,
        channelCode: ChannelCode,
        installAtomReq: InstallAtomReq
    ): Result<Boolean> {
        // 判断插件标识是否合法
        logger.info("installAtom params:[$accessToken|$userId|$channelCode|$installAtomReq]")
        val atom = marketAtomDao.getLatestAtomByCode(dslContext, installAtomReq.atomCode)
        if (null == atom || atom.deleteFlag == true) {
            return I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_INSTALL_ATOM_CODE_IS_INVALID,
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        }
        return storeProjectService.installStoreComponent(
            userId = userId,
            projectCodeList = installAtomReq.projectCode,
            storeId = atom.id,
            storeCode = atom.atomCode,
            storeType = StoreTypeEnum.ATOM,
            publicFlag = atom.defaultFlag,
            channelCode = channelCode
        )
    }

    /**
     * 设置插件构建状态
     */
    override fun setAtomBuildStatusByAtomCode(
        atomCode: String,
        version: String,
        userId: String,
        atomStatus: AtomStatusEnum,
        msg: String?
    ): Result<Boolean> {
        logger.info("setAtomBuildStatus|$userId,atomCode:$atomCode,version:$version,atomStatus:$atomStatus,msg:$msg")
        val atomRecord = atomDao.getPipelineAtom(dslContext, atomCode, version)
        if (null == atomRecord) {
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf("$atomCode+$version"),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        } else {
            // 只有处于构建中的插件才允许改构建结束后的构建状态
            if (AtomStatusEnum.BUILDING.status.toByte() == atomRecord.atomStatus) {
                marketAtomDao.setAtomStatusById(dslContext, atomRecord.id, atomStatus.status.toByte(), userId, msg)
                // 通过websocket推送状态变更消息
                storeWebsocketService.sendWebsocketMessage(userId, atomRecord.id)
            }
        }
        return Result(true)
    }

    /**
     * 根据插件标识获取插件版本列表
     */
    override fun getAtomVersionsByCode(
        userId: String,
        atomCode: String,
        page: Int,
        pageSize: Int
    ): Result<Page<AtomVersionListItem>> {
        // 判断当前用户是否是该插件的成员
        if (!storeMemberDao.isStoreMember(
                dslContext = dslContext,
                userId = userId,
                storeCode = atomCode,
                storeType = StoreTypeEnum.ATOM.type.toByte()
            )
        ) {
            throw ErrorCodeException(
                errorCode = GET_INFO_NO_PERMISSION,
                params = arrayOf(atomCode)
            )
        }
        val totalCount = atomDao.countByCode(dslContext, atomCode)
        val records = marketAtomDao.getAtomsByAtomCode(dslContext, atomCode, page, pageSize)
        val atomVersions = mutableListOf<AtomVersionListItem>()
        if (records != null) {
            val atomIds = records.map { it.id }
            // 批量获取版本内容
            val versionRecords = marketAtomVersionLogDao.getAtomVersions(dslContext, atomIds)
            val versionMap = mutableMapOf<String, String>()
            versionRecords?.forEach { versionRecord ->
                versionMap[versionRecord.atomId] = versionRecord.content
            }
            records.forEach {
                atomVersions.add(
                    AtomVersionListItem(
                        atomId = it.id,
                        atomCode = it.atomCode,
                        name = it.name,
                        category = AtomCategoryEnum.getAtomCategory((it.categroy as Byte).toInt()),
                        version = it.version,
                        versionContent = versionMap[it.id].toString(),
                        atomStatus = AtomStatusEnum.getAtomStatus((it.atomStatus as Byte).toInt()),
                        creator = it.creator,
                        createTime = DateTimeUtil.toDateTime(it.createTime)
                    )
                )
            }
        }
        return Result(Page(page, pageSize, totalCount.toLong(), atomVersions))
    }

    /**
     * 获取插件开发支持的语言
     */
    override fun listLanguage(): Result<List<AtomDevLanguage?>> {
        val records = storeBuildInfoDao.list(dslContext, StoreTypeEnum.ATOM)
        val ret = mutableListOf<AtomDevLanguage>()
        records?.forEach {
            ret.add(
                AtomDevLanguage(
                    language = it.language
                )
            )
        }
        return Result(ret)
    }

    /**
     * 删除插件
     */
    override fun deleteAtom(userId: String, atomCode: String): Result<Boolean> {
        logger.info("deleteAtom userId: $userId , atomCode: $atomCode")
        val type = StoreTypeEnum.ATOM.type.toByte()
        val isOwner = storeMemberDao.isStoreAdmin(dslContext, userId, atomCode, type)
        if (!isOwner) {
            return I18nUtil.generateResponseDataObject(
                messageCode = NO_COMPONENT_ADMIN_PERMISSION,
                params = arrayOf(atomCode),
                language = I18nUtil.getLanguage(userId)
            )
        }
        val releasedCount = marketAtomDao.countReleaseAtomByCode(dslContext, atomCode)
        logger.info("releasedCount: $releasedCount")
        if (releasedCount > 0) {
            return I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_ATOM_RELEASED_IS_NOT_ALLOW_DELETE,
                params = arrayOf(atomCode),
                language = I18nUtil.getLanguage(userId)
            )
        }
        // 如果已经有流水线在使用该插件，则不能删除
        val pipelineStat =
            client.get(ServiceMeasurePipelineResource::class).batchGetPipelineCountByAtomCode(atomCode, null).data
        val pipelines = pipelineStat?.get(atomCode) ?: 0
        if (pipelines > 0) {
            return I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_ATOM_USED_IS_NOT_ALLOW_DELETE,
                params = arrayOf(atomCode),
                language = I18nUtil.getLanguage(userId)
            )
        }
        // 删除仓库插件包文件
        val initProjectCode = storeProjectRelDao.getInitProjectCodeByStoreCode(
            dslContext = dslContext,
            storeCode = atomCode,
            storeType = StoreTypeEnum.ATOM.type.toByte()
        ) ?: ""
        val deleteAtomFileResult =
            client.get(ServiceArchiveAtomResource::class).deleteAtomFile(userId, initProjectCode, atomCode)
        if (deleteAtomFileResult.isNotOk()) {
            return deleteAtomFileResult
        }
        // 删除代码库
        val atomRecord = marketAtomDao.getLatestAtomByCode(dslContext, atomCode)
        val deleteAtomRepositoryResult = deleteAtomRepository(
            userId = userId,
            projectCode = initProjectCode,
            repositoryHashId = atomRecord!!.repositoryHashId,
            tokenType = TokenTypeEnum.PRIVATE_KEY
        )
        if (deleteAtomRepositoryResult.isNotOk()) {
            return deleteAtomRepositoryResult
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            storeCommonService.deleteStoreInfo(context, atomCode, StoreTypeEnum.ATOM.type.toByte())
            atomApproveRelDao.deleteByAtomCode(context, atomCode)
            marketAtomEnvInfoDao.deleteAtomEnvInfoByCode(context, atomCode)
            marketAtomFeatureDao.deleteAtomFeature(context, atomCode)
            atomLabelRelDao.deleteByAtomCode(context, atomCode)
            marketAtomVersionLogDao.deleteByAtomCode(context, atomCode)
            marketAtomDao.deleteByAtomCode(context, atomCode)
            // 删除插件默认标识缓存
            redisOperation.removeSetMember(StoreUtils.getStorePublicFlagKey(StoreTypeEnum.ATOM.name), atomCode)
            // 清空插件post信息缓存
            redisOperation.delete("$ATOM_POST_NORMAL_PROJECT_FLAG_KEY_PREFIX:$atomCode")
            // 清空插件运行时信息缓存
            redisOperation.delete(StoreUtils.getStoreRunInfoKey(StoreTypeEnum.ATOM.name, atomCode))
        }
        return Result(true)
    }

    override fun generateCiYaml(
        atomCode: String?,
        os: String?,
        classType: String?,
        defaultShowFlag: Boolean?
    ): String {
        val atomCodeList = if (atomCode.isNullOrBlank()) {
            marketAtomDao.getSupportGitCiAtom(dslContext, os, classType).map { it.value1() }
        } else {
            listOf(atomCode)
        }

        val buf = StringBuilder()
        atomCodeList.filterNotNull().forEach {
            val atom = marketAtomDao.getLatestAtomByCode(dslContext, it) ?: return@forEach
            val feature = marketAtomFeatureDao.getAtomFeature(dslContext, it) ?: return@forEach
            if (null == feature.recommendFlag || feature.recommendFlag) {
                buf.append(generateYaml(atom, defaultShowFlag))
                buf.append("\r\n")
                buf.append("\r\n")
            } else {
                return@forEach
            }
        }

        return buf.toString()
    }

    override fun generateCiV2Yaml(
        atomCode: String,
        os: String?,
        classType: String?,
        defaultShowFlag: Boolean?
    ): String {
        val atom = marketAtomDao.getLatestAtomByCode(dslContext, atomCode) ?: return ""
        val feature = marketAtomFeatureDao.getAtomFeature(dslContext, atomCode) ?: return ""
        return if (null == feature.recommendFlag || feature.recommendFlag) {
            generateV2Yaml(atom, defaultShowFlag)
        } else {
            ""
        }
    }

    override fun getAtomOutput(atomCode: String): List<AtomOutput> {
        val atom = marketAtomDao.getLatestAtomByCode(dslContext, atomCode) ?: return emptyList()
        val propJsonStr = storeI18nMessageService.parseJsonStrI18nInfo(
            jsonStr = atom.props,
            keyPrefix = StoreUtils.getStoreFieldKeyPrefix(StoreTypeEnum.ATOM, atom.atomCode, atom.version)
        )
        val propMap = JsonUtil.toMap(propJsonStr)
        val outputDataMap = propMap[ATOM_OUTPUT] as? Map<String, Any>
        return outputDataMap?.keys?.map { outputKey ->
            val outputDataObj = outputDataMap[outputKey]
            AtomOutput(
                name = outputKey,
                desc = if (outputDataObj is Map<*, *>) outputDataObj[OUTPUT_DESC]?.toString() else null
            )
        } ?: emptyList()
    }

    override fun getAtomsRely(getRelyAtom: GetRelyAtom): Map<String, Map<String, Any>> {
        val atomList = marketAtomDao.getLatestAtomListByCodes(
            dslContext = dslContext,
            atomCodes = getRelyAtom.thirdPartyElementList.map { it.atomCode }
        )
        val getMap = getRelyAtom.thirdPartyElementList.map { it.atomCode to it.version }.toMap()
        val result = mutableMapOf<String, Map<String, Any>>()
        atomList.forEach lit@{
            if (it == null) return@lit
            var value = it
            val atom = getMap[it.atomCode]
            if (atom?.contains("*") == true &&
                !it.version.startsWith(atom.replace("*", ""))
            ) {
                value = atomDao.getPipelineAtom(dslContext, it.atomCode, atom) ?: return@lit
            }
            val itemMap = mutableMapOf<String, Any>()
            val propJsonStr = storeI18nMessageService.parseJsonStrI18nInfo(
                jsonStr = value.props,
                keyPrefix = StoreUtils.getStoreFieldKeyPrefix(StoreTypeEnum.ATOM, value.atomCode, value.version)
            )
            val props: Map<String, Any> = jacksonObjectMapper().readValue(propJsonStr)
            if (null != props["input"]) {
                val input = props["input"] as Map<String, Any>
                input.forEach { inputIt ->
                    val paramKey = inputIt.key
                    val paramValueMap = inputIt.value as Map<String, Any>
                    val rely = paramValueMap["rely"]
                    if (rely != null) {
                        itemMap[paramKey] = rely
                    }
                }
            }
            result[it.atomCode] = itemMap
        }
        return result
    }

    @Suppress("UNCHECKED_CAST")
    private fun generateYaml(atom: TAtomRecord, defaultShowFlag: Boolean?): String {
        val sb = StringBuilder()
        if (defaultShowFlag != null && defaultShowFlag) {
            sb.append("h2. ${atom.name}\r\n")
                .append("{code:theme=Midnight|linenumbers=true|language=YAML|collapse=false}\r\n")
        }
        sb.append("- taskType: marketBuild@latest\r\n")
            .append("  displayName: ${atom.name}\r\n")
            .append("  inputs:\r\n")
            .append("    atomCode: ${atom.atomCode}\r\n")
            .append("    name: ${atom.name}\r\n")
            .append("    version: ${atom.version}\r\n")
            .append("    data:\r\n")
            .append("      input:\r\n")
        val propJsonStr = storeI18nMessageService.parseJsonStrI18nInfo(
            jsonStr = atom.props,
            keyPrefix = StoreUtils.getStoreFieldKeyPrefix(StoreTypeEnum.ATOM, atom.atomCode, atom.version)
        )
        val props: Map<String, Any> = jacksonObjectMapper().readValue(propJsonStr)
        if (null != props["input"]) {
            val input = props["input"] as Map<String, Any>
            input.forEach {
                val paramKey = it.key
                val paramValueMap = it.value as Map<String, Any>

                val label = paramValueMap["label"]
                val text = paramValueMap["text"]
                val desc = paramValueMap["desc"]
                val description = if (label?.toString().isNullOrBlank()) {
                    if (text?.toString().isNullOrBlank()) {
                        desc
                    } else {
                        text
                    }
                } else {
                    label
                }
                val type = paramValueMap["type"]
                val required = paramValueMap["required"]
                val defaultValue = paramValueMap["default"]
                val multipleMap = paramValueMap["optionsConf"]
                val multiple = if (null != multipleMap && null != (multipleMap as Map<String, String>)["multiple"]) {
                    "true".equals(multipleMap["multiple"].toString(), true)
                } else {
                    false
                }
                val requiredName = I18nUtil.getCodeLanMessage(
                    messageCode = REQUIRED
                )
                val defaultName = I18nUtil.getCodeLanMessage(
                    messageCode = DEFAULT
                )
                if ((type == "selector" && multiple) ||
                    type in listOf("atom-checkbox-list", "staff-input", "company-staff-input", "parameter")
                ) {
                    sb.append("        $paramKey: ")
                    sb.append("\t\t# $description")
                    if (null != required && "true".equals(required.toString(), true)) {
                        sb.append(", $requiredName")
                    }
                    if (null != defaultValue && (defaultValue.toString()).isNotBlank()) {
                        sb.append(", $defaultName: ${defaultValue.toString().replace("\n", "")}")
                    }
                    sb.append("\r\n")
                    sb.append("        - string\r\n")
                    sb.append("        - string\r\n")
                } else {
                    sb.append("        $paramKey: ")
                    if (type == "atom-checkbox") {
                        sb.append("boolean")
                    } else {
                        sb.append("string")
                    }
                    sb.append("\t\t# ${description.toString().replace("\n", "")}")
                    if (null != required && "true".equals(required.toString(), true)) {
                        sb.append(", $requiredName")
                    }
                    if (null != defaultValue && (defaultValue.toString()).isNotBlank()) {
                        sb.append(", $defaultName: ${defaultValue.toString().replace("\n", "")}")
                    }
                    sb.append("\r\n")
                }
            }
        }

        if (null != props["output"]) {
            sb.append("      output: \r\n")
            val output = props["output"] as Map<String, Any>
            output.forEach {
                sb.append("        ${it.key}: string \r\n")
            }
        } else {
            sb.append("      output: {}\r\n")
        }
        if (defaultShowFlag != null && defaultShowFlag) {
            sb.append("{code}\r\n \r\n")
        }
        return sb.toString()
    }

    @Suppress("UNCHECKED_CAST")
    private fun generateV2Yaml(atom: TAtomRecord, defaultShowFlag: Boolean?): String {
        val userId = I18nUtil.getRequestUserId()
        val sb = StringBuilder()
        if (defaultShowFlag != null && defaultShowFlag) {
            sb.append("h2. ${atom.name}\r\n")
                .append("{code:theme=Midnight|linenumbers=true|language=YAML|collapse=false}\r\n")
        }
        val latestVersion = "${atom.version.split('.').first()}.*"
        sb.append("- uses: ${atom.atomCode}@$latestVersion\r\n")
            .append("  name: ${atom.name}\r\n")
        val propJsonStr = storeI18nMessageService.parseJsonStrI18nInfo(
            jsonStr = atom.props,
            keyPrefix = StoreUtils.getStoreFieldKeyPrefix(StoreTypeEnum.ATOM, atom.atomCode, atom.version)
        )
        val props: Map<String, Any> = jacksonObjectMapper().readValue(propJsonStr)
        if (null != props["input"]) {
            sb.append("  with:\r\n")
            val input = props["input"] as Map<String, Any>
            input.forEach {
                val paramKey = it.key
                val paramValueMap = it.value as Map<String, Any>

                val label = paramValueMap["label"]
                val text = paramValueMap["text"]
                val desc = paramValueMap["desc"]
                val description = if (!label?.toString().isNullOrBlank()) {
                    label.toString()
                } else if (!text?.toString().isNullOrBlank()) {
                    text.toString()
                } else if (!desc?.toString().isNullOrBlank()) {
                    desc.toString()
                } else {
                    I18nUtil.getCodeLanMessage(NO_LABEL)
                }
                val type = paramValueMap["type"]
                val required = null != paramValueMap["required"] &&
                        "true".equals(paramValueMap["required"].toString(), true)
                val defaultValue = paramValueMap["default"]
                val multipleMap = paramValueMap["optionsConf"]
                val multiple = if (null != multipleMap && null != (multipleMap as Map<String, String>)["multiple"]) {
                    "true".equals(multipleMap["multiple"].toString(), true)
                } else {
                    false
                }
                val requiredName = I18nUtil.getCodeLanMessage(REQUIRED)
                val defaultName = I18nUtil.getCodeLanMessage(DEFAULT)
                val optionsName = I18nUtil.getCodeLanMessage(OPTIONS)
                val multipleName =
                    I18nUtil.getCodeLanMessage(MULTIPLE_SELECTOR)
                val singleName =
                    I18nUtil.getCodeLanMessage(SINGLE_SELECTOR)
                try {
                    if ((type == "selector" && multiple) ||
                        type in listOf("atom-checkbox-list", "staff-input", "company-staff-input", "parameter")
                    ) {
                        addParamComment(
                            builder = sb,
                            description = description,
                            paramKey = paramKey,
                            required = required,
                            optionsName = optionsName,
                            selectorTypeName = multipleName,
                            paramValueMap = paramValueMap,
                            requiredName = requiredName,
                            defaultValue = defaultValue,
                            defaultName = defaultName
                        )
                        sb.append("\r\n")
                        sb.append("    $paramKey:\r\n")
                        sb.append("        - string\r\n")
                        sb.append("        - string\r\n")
                    } else {
                        addParamComment(
                            builder = sb,
                            description = description,
                            paramKey = paramKey,
                            required = required,
                            optionsName = optionsName,
                            selectorTypeName = singleName,
                            paramValueMap = paramValueMap,
                            requiredName = requiredName,
                            defaultValue = defaultValue,
                            defaultName = defaultName
                        )
                        sb.append("\r\n")
                        sb.append("    $paramKey: ")
                        when (type) {
                            "atom-checkbox" -> sb.append("boolean")
                            "key-value-normal" -> sb.append(
                                "\n    - key: string" +
                                "\n      value: string"
                            )
                            else -> sb.append("string")
                        }
                        sb.append("\r\n")
                    }
                } catch (ignored: Throwable) {
                    sb.insert(
                        0,
                        MessageUtil.getMessageByLocale(
                            TASK_JSON_CONFIGURE_FORMAT_ERROR,
                            I18nUtil.getLanguage(userId),
                            arrayOf(paramKey, "${ignored.message}")
                        )
                    )
                }
            }
        }

        if (defaultShowFlag != null && defaultShowFlag) {
            sb.append("{code}\r\n \r\n")
        }
        return sb.toString()
    }

    override fun getPostAtoms(projectCode: String, atomItems: Set<AtomPostReqItem>): Result<AtomPostResp> {
        logger.info("getPostAtoms projectCode:$projectCode,atomItems:$atomItems")
        val postAtoms = mutableListOf<AtomPostInfo>()
        atomItems.forEach { atomItem ->
            val atomCode = atomItem.atomCode
            val version = atomItem.version
            val atomEnvResult = marketAtomEnvService.getMarketAtomEnvInfo(projectCode, atomCode, version)
            val atomEnv = atomEnvResult.data
            if (atomEnvResult.isNotOk()) {
                throw ErrorCodeException(
                    errorCode = atomEnvResult.status.toString(),
                    defaultMessage = atomEnvResult.message
                )
            }
            if (atomEnv == null) {
                throw ErrorCodeException(
                    errorCode = StoreMessageCode.USER_ATOM_IS_NOT_ALLOW_USE_IN_PROJECT,
                    params = arrayOf(projectCode, atomCode)
                )
            }
            val atomPostInfo = atomEnv.atomPostInfo
            if (atomPostInfo != null) {
                postAtoms.add(atomPostInfo)
            }
        }
        val atomPostResp = AtomPostResp(postAtoms)
        return Result(atomPostResp)
    }

    abstract fun deleteAtomRepository(
        userId: String,
        projectCode: String?,
        repositoryHashId: String,
        tokenType: TokenTypeEnum
    ): Result<Boolean>

    abstract fun updateAtomFileContent(
        userId: String,
        projectCode: String,
        atomCode: String,
        content: String,
        filePath: String
    ): Result<Boolean>

    @Suppress("UNCHECKED_CAST")
    private fun addParamComment(
        builder: StringBuilder,
        description: String,
        paramKey: String,
        required: Boolean,
        optionsName: String,
        selectorTypeName: String,
        paramValueMap: Map<String, Any>,
        requiredName: Any?,
        defaultValue: Any?,
        defaultName: Any?
    ) {
        builder.append("    # ${description.replace("\n", "")}")
        if (required) {
            builder.append(", $requiredName")
        }
        if (null != defaultValue && (defaultValue.toString()).isNotBlank()) {
            builder.append(", $defaultName: ${defaultValue.toString().replace("\n", "")}")
        }
        val rely = paramValueMap["rely"]
        if (null != rely) {
            parseRely(builder, rely as Map<String, Any>)
        }
        val options = paramValueMap["options"]
        if (null != options) {
            builder.append(", $selectorTypeName")
            builder.append(", $optionsName:")
            parseOptions(builder, options as List<Map<String, Any>>)
        }
        val list = paramValueMap["list"]
        if (null != list) {
            builder.append(", $optionsName:")
            parseList(builder, list as List<Map<String, Any>>)
        }
    }

    private fun parseRely(builder: StringBuilder, rely: Map<String, Any>) {
        val dang = I18nUtil.getCodeLanMessage(messageCode = DANG)
        val and = I18nUtil.getCodeLanMessage(messageCode = AND)
        val or = I18nUtil.getCodeLanMessage(messageCode = OR)
        val timeToSelect =
            I18nUtil.getCodeLanMessage(messageCode = TIMETOSELECT)
        try {
            if (null != rely["expression"]) {
                val expression = rely["expression"] as List<Map<String, Any>>
                builder.append(", $dang")
                val link = if (rely["operation"] == "AND") and else or
                expression.map { " [${it["key"]}] = [${it["value"]}] " }.forEachIndexed { index, value ->
                    builder.append(value)
                    if (index < expression.size - 1) {
                        builder.append(link)
                    }
                }
                builder.append(timeToSelect)
            }
        } catch (ignored: Throwable) {
            logger.warn("load atom input[rely] with error", ignored)
        }
    }

    private fun parseOptions(builder: StringBuilder, options: List<Map<String, Any>>) {
        try {
            options.forEachIndexed { index, map ->
                if (index == options.size - 1) builder.append(" ${map["id"]}[${map["name"]}]")
                else builder.append(" ${map["id"]}[${map["name"]}] |")
            }
            builder.removeSuffix("|")
        } catch (ignored: Throwable) {
            logger.warn("load atom input[options] with error", ignored)
        }
    }

    private fun parseList(builder: StringBuilder, list: List<Map<String, Any>>) {
        try {
            list.forEachIndexed { index, map ->
                val key = if (null != map["label"]) map["label"] else if (null != map["id"]) map["id"] else
                    null ?: return
                val value = if (null != map["value"]) map["value"] else if (null != map["name"]) map["name"] else
                    null ?: return
                if (index == list.size - 1) builder.append(" $key[$value]")
                else builder.append(" $key[$value] |")
            }
            builder.removeSuffix("|")
        } catch (ignored: Throwable) {
            logger.warn("load atom input[list] with error", ignored)
        }
    }
}

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

import com.tencent.devops.artifactory.api.ServiceArchiveAtomResource
import com.tencent.devops.common.api.auth.REFERER
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.INIT_VERSION
import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.ThreadLocalUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.AtomBaseInfo
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.common.util.RegexUtils
import com.tencent.devops.common.web.utils.BkApiUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.store.tables.TAtom
import com.tencent.devops.model.store.tables.TAtomEnvInfo
import com.tencent.devops.model.store.tables.TAtomFeature
import com.tencent.devops.model.store.tables.TAtomVersionLog
import com.tencent.devops.model.store.tables.TClassify
import com.tencent.devops.process.api.service.ServiceMeasurePipelineResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.atom.dao.AtomApproveRelDao
import com.tencent.devops.store.atom.dao.AtomDao
import com.tencent.devops.store.atom.dao.AtomLabelRelDao
import com.tencent.devops.store.atom.dao.MarketAtomClassifyDao
import com.tencent.devops.store.atom.dao.MarketAtomDao
import com.tencent.devops.store.pojo.atom.MarketAtomDaoQuery
import com.tencent.devops.store.atom.dao.MarketAtomEnvInfoDao
import com.tencent.devops.store.atom.dao.MarketAtomFeatureDao
import com.tencent.devops.store.atom.dao.MarketAtomVersionLogDao
import com.tencent.devops.store.atom.service.AtomLabelService
import com.tencent.devops.store.atom.service.AtomPropsService
import com.tencent.devops.store.atom.service.AtomYamlGenerateService
import com.tencent.devops.store.atom.service.MarketAtomCommonService
import com.tencent.devops.store.atom.service.MarketAtomEnvService
import com.tencent.devops.store.atom.service.MarketAtomService
import com.tencent.devops.store.atom.util.AtomServiceScopeUtil
import com.tencent.devops.store.common.dao.StoreBuildInfoDao
import com.tencent.devops.store.common.dao.StoreErrorCodeInfoDao
import com.tencent.devops.store.common.dao.StoreMemberDao
import com.tencent.devops.store.common.dao.StoreProjectRelDao
import com.tencent.devops.store.common.dao.ClassifyDao
import com.tencent.devops.store.common.service.ClassifyService
import com.tencent.devops.store.common.service.StoreCommentService
import com.tencent.devops.store.common.service.StoreCommonService
import com.tencent.devops.store.common.service.StoreDailyStatisticService
import com.tencent.devops.store.common.service.StoreHonorService
import com.tencent.devops.store.common.service.StoreI18nMessageService
import com.tencent.devops.store.common.service.StoreIndexManageService
import com.tencent.devops.store.common.service.StoreProjectService
import com.tencent.devops.store.common.service.StoreTotalStatisticService
import com.tencent.devops.store.common.service.StoreUserService
import com.tencent.devops.store.common.service.StoreWebsocketService
import com.tencent.devops.store.common.service.action.StoreDecorateFactory
import com.tencent.devops.store.common.utils.StoreUtils
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.constant.StoreMessageCode.GET_INFO_NO_PERMISSION
import com.tencent.devops.store.constant.StoreMessageCode.NO_COMPONENT_ADMIN_PERMISSION
import com.tencent.devops.store.pojo.atom.AtomDevLanguage
import com.tencent.devops.store.pojo.atom.AtomOutput
import com.tencent.devops.store.pojo.atom.AtomPostInfo
import com.tencent.devops.store.pojo.atom.AtomPostReqItem
import com.tencent.devops.store.pojo.atom.AtomPostResp
import com.tencent.devops.store.pojo.atom.AtomRunInfo
import com.tencent.devops.store.pojo.atom.AtomVersion
import com.tencent.devops.store.pojo.atom.AtomVersionListItem
import com.tencent.devops.store.pojo.atom.ElementThirdPartySearchParam
import com.tencent.devops.store.pojo.atom.GetRelyAtom
import com.tencent.devops.store.pojo.atom.InstallAtomReq
import com.tencent.devops.store.pojo.atom.MarketAtomListQuery
import com.tencent.devops.store.pojo.atom.MarketAtomResp
import com.tencent.devops.store.pojo.atom.MyAtomResp
import com.tencent.devops.store.pojo.atom.MyAtomRespItem
import com.tencent.devops.store.pojo.atom.enums.AtomCategoryEnum
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.atom.enums.AtomTypeEnum
import com.tencent.devops.store.pojo.atom.enums.MarketAtomSortTypeEnum
import com.tencent.devops.store.pojo.common.ATOM_POST_NORMAL_PROJECT_FLAG_KEY_PREFIX
import com.tencent.devops.store.pojo.common.ERROR_JSON_NAME
import com.tencent.devops.store.pojo.common.HOTTEST
import com.tencent.devops.store.pojo.common.InstallStoreReq
import com.tencent.devops.store.pojo.common.KEY_CLASSIFY_CODE
import com.tencent.devops.store.pojo.common.KEY_CLASSIFY_NAME
import com.tencent.devops.store.pojo.common.KEY_STORE_CODE
import com.tencent.devops.store.pojo.common.LATEST
import com.tencent.devops.store.pojo.common.MarketItem
import com.tencent.devops.store.pojo.common.MarketMainItem
import com.tencent.devops.store.pojo.common.MarketMainItemLabel
import com.tencent.devops.store.pojo.common.StoreErrorCodeInfo
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.ServiceScopeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.honor.HonorInfo
import com.tencent.devops.store.pojo.common.index.StoreIndexInfo
import com.tencent.devops.store.pojo.common.statistic.StoreDailyStatistic
import com.tencent.devops.store.pojo.common.statistic.StoreStatistic
import com.tencent.devops.store.pojo.common.version.StoreShowVersionInfo
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import java.time.LocalDateTime
import java.util.Calendar
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

@Suppress("ALL")
abstract class MarketAtomServiceImpl @Autowired constructor() : MarketAtomService {

    @Autowired
    lateinit var dslContext: DSLContext

    @Autowired
    lateinit var atomDao: AtomDao

    @Autowired
    lateinit var marketAtomDao: MarketAtomDao

    @Autowired
    lateinit var classifyDao: ClassifyDao

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
    lateinit var atomServiceScopeUtil: AtomServiceScopeUtil

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
    lateinit var atomYamlGenerateService: AtomYamlGenerateService

    @Autowired
    lateinit var atomPropsService: AtomPropsService

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

    @Suppress("UNCHECKED_CAST")
    private fun doList(
        query: MarketAtomListQuery,
        userDeptList: List<Int>,
        desc: Boolean?
    ): Future<MarketAtomResp> {
        val referer = BkApiUtil.getHttpServletRequest()?.getHeader(REFERER)
        return executor.submit(Callable<MarketAtomResp> {
            referer?.let { ThreadLocalUtil.set(REFERER, it) }
            try {
                val daoQuery = MarketAtomDaoQuery(
                    keyword = query.keyword,
                    classifyCode = query.classifyCode,
                    labelCodeList = query.labelCode?.takeIf { it.isNotEmpty() }?.split(","),
                    score = query.score,
                    rdType = query.rdType,
                    yamlFlag = query.yamlFlag,
                    recommendFlag = query.recommendFlag,
                    qualityFlag = query.qualityFlag,
                    serviceScope = query.serviceScope,
                    sortType = query.sortType,
                    desc = desc,
                    page = query.page,
                    pageSize = query.pageSize
                )
                val count = marketAtomDao.count(dslContext, daoQuery)
                val atoms = marketAtomDao.list(dslContext, daoQuery)
                    ?: return@Callable MarketAtomResp(0, query.page, query.pageSize, emptyList())

                val atomCodeList = atoms.map { it[TAtom.T_ATOM.ATOM_CODE] as String }
                val aggregateData = loadAtomAggregateData(atomCodeList, query.userId, query.serviceScope)
                val results = atoms.map { record ->
                    buildMarketItem(record, aggregateData, userDeptList, query)
                }
                MarketAtomResp(count, query.page, query.pageSize, results)
            } finally {
                ThreadLocalUtil.remove(REFERER)
            }
        })
    }

    private data class AtomAggregateData(
        val visibleData: HashMap<String, MutableList<Int>>?,
        val statisticData: HashMap<String, StoreStatistic>,
        val honorInfoMap: Map<String, List<HonorInfo>>,
        val indexInfosMap: Map<String, List<StoreIndexInfo>>,
        val memberData: HashMap<String, MutableList<String>>?,
        val classifyMap: Map<String, String>
    )

    private fun loadAtomAggregateData(
        atomCodeList: List<String>,
        userId: String,
        serviceScope: ServiceScopeEnum?
    ): AtomAggregateData {
        val storeType = StoreTypeEnum.ATOM
        val visibleData = storeCommonService.generateStoreVisibleData(atomCodeList, storeType)
        val statisticData = storeTotalStatisticService.getStatisticByCodeList(
            storeType = storeType.type.toByte(),
            storeCodeList = atomCodeList
        )
        val honorInfoMap = storeHonorService.getHonorInfosByStoreCodes(
            storeType = storeType,
            storeCodes = atomCodeList,
            userId = userId
        )
        val indexInfosMap = storeIndexManageService.getStoreIndexInfosByStoreCodes(storeType, atomCodeList)
        val memberData = atomMemberService.batchListMember(atomCodeList, storeType).data
        val classifyList = classifyService.getAllClassify(storeType.type.toByte(), serviceScope).data
        val classifyMap = classifyList?.associate { it.id to it.classifyCode } ?: emptyMap()
        return AtomAggregateData(
            visibleData = visibleData,
            statisticData = statisticData,
            honorInfoMap = honorInfoMap,
            indexInfosMap = indexInfosMap,
            memberData = memberData,
            classifyMap = classifyMap
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildMarketItem(
        record: org.jooq.Record,
        aggregateData: AtomAggregateData,
        userDeptList: List<Int>,
        query: MarketAtomListQuery
    ): MarketItem {
        val tAtom = TAtom.T_ATOM
        val tAtomFeature = TAtomFeature.T_ATOM_FEATURE
        val atomCode = record[tAtom.ATOM_CODE] as String
        val statistic = aggregateData.statisticData[atomCode]
        val classifyId = record[tAtom.CLASSIFY_ID] as String
        val defaultFlag = record[tAtom.DEFAULT_FLAG] as Boolean
        val flag = storeCommonService.generateInstallFlag(
            defaultFlag = defaultFlag,
            members = aggregateData.memberData?.get(atomCode),
            userId = query.userId,
            visibleList = aggregateData.visibleData?.get(atomCode),
            userDeptList = userDeptList
        )
        val logoUrl = processLogoUrl(record[tAtom.LOGO_URL], query.urlProtocolTrim)
        val osStr = record[tAtom.OS]
        return MarketItem(
            id = record[tAtom.ID] as String,
            name = record[tAtom.NAME] as String,
            code = atomCode,
            version = record[tAtom.VERSION] as String,
            status = AtomStatusEnum.getAtomStatus((record[tAtom.ATOM_STATUS] as Byte).toInt()),
            type = StoreTypeEnum.ATOM.name,
            rdType = AtomTypeEnum.getAtomType((record[tAtom.ATOM_TYPE] as Byte).toInt()),
            classifyCode = aggregateData.classifyMap[classifyId] ?: "",
            category = AtomCategoryEnum.getAtomCategory((record[tAtom.CATEGROY] as Byte).toInt()),
            logoUrl = logoUrl,
            publisher = record[tAtom.PUBLISHER] as String,
            os = if (!osStr.isNullOrBlank()) JsonUtil.getObjectMapper().readValue(
                osStr, List::class.java
            ) as List<String> else null,
            downloads = statistic?.downloads ?: 0,
            score = statistic?.score ?: 0.toDouble(),
            summary = record[tAtom.SUMMARY],
            flag = flag,
            publicFlag = defaultFlag,
            buildLessRunFlag = record[tAtom.BUILD_LESS_RUN_FLAG] ?: false,
            docsLink = record[tAtom.DOCS_LINK] ?: "",
            modifier = record[tAtom.MODIFIER] as String,
            updateTime = DateTimeUtil.toDateTime(record[tAtom.UPDATE_TIME] as LocalDateTime),
            recommendFlag = record[tAtomFeature.RECOMMEND_FLAG],
            yamlFlag = record[tAtomFeature.YAML_FLAG],
            recentExecuteNum = statistic?.recentExecuteNum ?: 0,
            indexInfos = aggregateData.indexInfosMap[atomCode],
            honorInfos = aggregateData.honorInfoMap[atomCode],
            hotFlag = statistic?.hotFlag
        )
    }

    private fun processLogoUrl(rawLogoUrl: String?, urlProtocolTrim: Boolean): String? {
        var logoUrl = rawLogoUrl?.let {
            StoreDecorateFactory.get(StoreDecorateFactory.Kind.HOST)?.decorate(it) as? String
        }
        logoUrl = if (logoUrl?.contains("?") == true) {
            logoUrl.plus("&logo=true")
        } else {
            logoUrl?.plus("?logo=true")
        }
        if (urlProtocolTrim) {
            logoUrl = RegexUtils.trimProtocol(logoUrl)
        }
        return logoUrl
    }

    /**
     * 插件市场，首页
     */
    @BkTimed(extraTags = ["web_operation", "mainPageList"], value = "store_web_operation")
    override fun mainPageList(
        userId: String,
        page: Int?,
        pageSize: Int?,
        urlProtocolTrim: Boolean,
        serviceScope: ServiceScopeEnum?
    ): Result<List<MarketMainItem>> {
        val userDeptList = storeUserService.getUserDeptList(userId)
        val futureList = mutableListOf<Future<MarketAtomResp>>()
        val labelInfoList = mutableListOf<MarketMainItemLabel>()

        val baseQuery = MarketAtomListQuery(
            userId = userId,
            page = page,
            pageSize = pageSize,
            urlProtocolTrim = urlProtocolTrim,
            serviceScope = serviceScope
        )

        labelInfoList.add(MarketMainItemLabel(LATEST, I18nUtil.getCodeLanMessage(LATEST)))
        futureList.add(
            doList(
                query = baseQuery.copy(sortType = MarketAtomSortTypeEnum.UPDATE_TIME),
                userDeptList = userDeptList,
                desc = true
            )
        )

        labelInfoList.add(MarketMainItemLabel(HOTTEST, I18nUtil.getCodeLanMessage(HOTTEST)))
        futureList.add(
            doList(
                query = baseQuery.copy(sortType = MarketAtomSortTypeEnum.RECENT_EXECUTE_NUM),
                userDeptList = userDeptList,
                desc = true
            )
        )

        val classifyList = marketAtomClassifyDao.getAllAtomClassify(dslContext, serviceScope)
        classifyList.forEach {
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
                        query = baseQuery.copy(
                            classifyCode = classifyCode,
                            sortType = MarketAtomSortTypeEnum.RECENT_EXECUTE_NUM
                        ),
                        userDeptList = userDeptList,
                        desc = true
                    )
                )
            }
        }

        val result = futureList.indices.map { index ->
            val labelInfo = labelInfoList[index]
            MarketMainItem(
                key = labelInfo.key,
                label = labelInfo.label,
                records = futureList[index].get().records
            )
        }
        return Result(result)
    }

    /**
     * 插件市场，查询插件列表
     */
    @BkTimed(extraTags = ["web_operation", "getAtomList"], value = "store_web_operation")
    override fun list(query: MarketAtomListQuery): MarketAtomResp {
        val userDeptList = storeUserService.getUserDeptList(query.userId)
        return doList(query = query, userDeptList = userDeptList, desc = true).get()
    }

    /**
     * 根据用户和插件名称获取插件信息
     */
    override fun getMyAtoms(
        userId: String,
        atomName: String?,
        page: Int,
        pageSize: Int
    ): Result<MyAtomResp?> {
        logger.info("getMyAtoms params:[$userId|$atomName|$page|$pageSize]")
        val records = marketAtomDao.getMyAtoms(dslContext, userId, atomName, page, pageSize)
        val count = marketAtomDao.countMyAtoms(dslContext, userId, atomName)
        val tAtom = TAtom.T_ATOM
        val tAtomEnvInfo = TAtomEnvInfo.T_ATOM_ENV_INFO

        val atomProjectMap = mutableMapOf<String, String>()
        val atomCodeList = records?.map { record ->
            val atomCode = record[tAtom.ATOM_CODE] as String
            storeProjectRelDao.getUserStoreTestProjectCode(
                dslContext = dslContext, userId = userId,
                storeCode = atomCode, storeType = StoreTypeEnum.ATOM
            )?.let { atomProjectMap[atomCode] = it }
            atomCode
        } ?: emptyList()

        val projectMap = client.get(ServiceProjectResource::class)
            .getNameByCode(atomProjectMap.values.joinToString(",")).data

        val processingVersionInfoMap = buildProcessingVersionInfoMap(atomCodeList)

        val myAtoms = records?.map { record ->
            val atomCode = record[tAtom.ATOM_CODE] as String
            val logoUrl = record[tAtom.LOGO_URL]
            MyAtomRespItem(
                atomId = record[tAtom.ID] as String,
                name = record[tAtom.NAME] as String,
                atomCode = atomCode,
                language = record[tAtomEnvInfo.LANGUAGE]?.let { I18nUtil.getCodeLanMessage(it) },
                category = AtomCategoryEnum.getAtomCategory((record[tAtom.CATEGROY] as Byte).toInt()),
                logoUrl = logoUrl?.let {
                    StoreDecorateFactory.get(StoreDecorateFactory.Kind.HOST)?.decorate(logoUrl) as? String
                },
                version = record[tAtom.VERSION] as String,
                atomStatus = AtomStatusEnum.getAtomStatus((record[tAtom.ATOM_STATUS] as Byte).toInt()),
                projectName = projectMap?.get(atomProjectMap[atomCode]) ?: "",
                releaseFlag = marketAtomDao.countReleaseAtomByCode(dslContext, atomCode) > 0,
                creator = record[tAtom.CREATOR] as String,
                modifier = record[tAtom.MODIFIER] as String,
                createTime = DateTimeUtil.toDateTime(record[tAtom.CREATE_TIME] as LocalDateTime),
                updateTime = DateTimeUtil.toDateTime(record[tAtom.UPDATE_TIME] as LocalDateTime),
                processingVersionInfos = processingVersionInfoMap?.get(atomCode),
                codeSrc = record[tAtom.CODE_SRC]
            )
        } ?: emptyList()
        return Result(MyAtomResp(count, page, pageSize, myAtoms))
    }

    private fun buildProcessingVersionInfoMap(
        atomCodeList: List<String>
    ): Map<String, List<AtomBaseInfo>>? {
        if (atomCodeList.isEmpty()) return null
        val processingAtomRecords = marketAtomDao.getAtomsByConditions(
            dslContext = dslContext,
            atomCodeList = atomCodeList,
            atomStatusList = AtomStatusEnum.getProcessingStatusList()
        ) ?: return null

        val result = processingAtomRecords
            .filter { it.version != INIT_VERSION && !it.version.isNullOrBlank() && !it.branchTestFlag }
            .groupBy({ it.atomCode }) { record ->
                AtomBaseInfo(
                    atomId = record.id,
                    atomCode = record.atomCode,
                    version = record.version,
                    atomStatus = AtomStatusEnum.getAtomStatus(record.atomStatus.toInt())
                )
            }
        return result.ifEmpty { null }
    }

    /**
     * 根据插件版本ID获取版本基本信息、发布信息
     */
    override fun getAtomById(atomId: String, userId: String, serviceScope: ServiceScopeEnum?): Result<AtomVersion?> {
        return getAtomVersion(atomId, userId, serviceScope)
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

        val isAtomInitStatus = record.atomStatus == AtomStatusEnum.INIT.status.toByte()

        val (releaseType, lastVersionContent) = if (isAtomInitStatus) {
            Pair(null, null)
        } else {
            val log = marketAtomVersionLogDao.getAtomVersion(dslContext, record.id)
            Pair(log.releaseType, log.content)
        }
        val showReleaseType = if (releaseType != null) {
            ReleaseTypeEnum.getReleaseTypeObj(releaseType.toInt())
        } else {
            null
        }
        val showVersionInfo = storeCommonService.getStoreShowVersionInfo(
            storeType = StoreTypeEnum.ATOM,
            cancelFlag = cancelFlag,
            releaseType = showReleaseType,
            version = showVersion,
            lastVersionContent = lastVersionContent
        )
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
    private fun getAtomVersion(
        atomId: String,
        userId: String,
        serviceScope: ServiceScopeEnum? = null
    ): Result<AtomVersion?> {
        val record = marketAtomDao.getAtomById(dslContext, atomId, serviceScope) ?: return Result(data = null)

        val tAtom = TAtom.T_ATOM
        val tAtomVersionLog = TAtomVersionLog.T_ATOM_VERSION_LOG
        val tClassify = TClassify.T_CLASSIFY
        val atomCode = record[tAtom.ATOM_CODE] as String
        val defaultFlag = record[tAtom.DEFAULT_FLAG] as Boolean
        val htmlTemplateVersion = record[tAtom.HTML_TEMPLATE_VERSION] as String

        val projectCode = resolveInitProjectCode(htmlTemplateVersion, atomCode)
        val repositoryInfo = loadRepositoryInfoSafely(atomCode, projectCode, record[tAtom.REPOSITORY_HASH_ID])
        val classifyLanName = resolveClassifyLanName(record[tClassify.CLASSIFY_CODE], record[tClassify.CLASSIFY_NAME])

        val serviceScopeDetails = atomServiceScopeUtil.buildServiceScopeDetails(
            atomId = atomId,
            serviceScopeStr = record[tAtom.SERVICE_SCOPE],
            classifyIdMapJson = record[tAtom.CLASSIFY_ID_MAP],
            pipelineClassifyIdFallback = record[tAtom.CLASSIFY_ID]?.toString(),
            jobTypeValue = record[tAtom.JOB_TYPE],
            jobTypeMapValue = record[tAtom.JOB_TYPE_MAP],
            osValue = record[tAtom.OS],
            osMapValue = record[tAtom.OS_MAP]
        )
        val releaseType = record[tAtomVersionLog.RELEASE_TYPE]?.let {
            ReleaseTypeEnum.getReleaseTypeObj((it as Byte).toInt())
        }
        val osStr = record[tAtom.OS]
        val logoUrl = record[tAtom.LOGO_URL]
        val description = record[tAtom.DESCRIPTION]

        return Result(
            AtomVersion(
                atomId = atomId,
                atomCode = atomCode,
                name = record[tAtom.NAME] as String,
                logoUrl = logoUrl?.let {
                    StoreDecorateFactory.get(StoreDecorateFactory.Kind.HOST)?.decorate(logoUrl) as? String
                },
                classifyCode = record[tClassify.CLASSIFY_CODE],
                classifyName = classifyLanName,
                category = AtomCategoryEnum.getAtomCategory((record[tAtom.CATEGROY] as Byte).toInt()),
                docsLink = record[tAtom.DOCS_LINK],
                htmlTemplateVersion = htmlTemplateVersion,
                atomType = AtomTypeEnum.getAtomType((record[tAtom.ATOM_TYPE] as Byte).toInt()),
                jobType = record[tAtom.JOB_TYPE],
                os = if (!osStr.isNullOrBlank()) JsonUtil.getObjectMapper().readValue(
                    osStr, List::class.java
                ) as List<String> else null,
                summary = record[tAtom.SUMMARY],
                description = description?.let {
                    StoreDecorateFactory.get(StoreDecorateFactory.Kind.HOST)?.decorate(description) as? String
                },
                version = record[tAtom.VERSION],
                atomStatus = AtomStatusEnum.getAtomStatus((record[tAtom.ATOM_STATUS] as Byte).toInt()),
                releaseType = releaseType?.name,
                versionContent = record[tAtomVersionLog.CONTENT],
                language = marketAtomEnvInfoDao.getDefaultAtomEnvInfo(dslContext, atomId)
                    ?.language?.let { I18nUtil.getCodeLanMessage(it) },
                codeSrc = record[tAtom.CODE_SRC],
                publisher = record[tAtom.PUBLISHER] as String,
                modifier = record[tAtom.MODIFIER] as String,
                creator = record[tAtom.CREATOR] as String,
                createTime = DateTimeUtil.toDateTime(record[tAtom.CREATE_TIME] as LocalDateTime),
                updateTime = DateTimeUtil.toDateTime(record[tAtom.UPDATE_TIME] as LocalDateTime),
                flag = storeUserService.isCanInstallStoreComponent(
                    defaultFlag, userId, atomCode, StoreTypeEnum.ATOM
                ),
                repositoryAuthorizer = repositoryInfo?.userName,
                defaultFlag = defaultFlag,
                projectCode = storeProjectRelDao.getUserStoreTestProjectCode(
                    dslContext = dslContext, userId = userId,
                    storeCode = atomCode, storeType = StoreTypeEnum.ATOM
                ),
                initProjectCode = projectCode,
                labelList = atomLabelService.getLabelsByAtomId(atomId, serviceScope) ?: emptyList(),
                userCommentInfo = storeCommentService.getStoreUserCommentInfo(userId, atomCode, StoreTypeEnum.ATOM),
                visibilityLevel = VisibilityLevelEnum.getVisibilityLevel(record[tAtom.VISIBILITY_LEVEL] as Int),
                privateReason = record[tAtom.PRIVATE_REASON],
                recommendFlag = marketAtomFeatureDao.getAtomFeature(dslContext, atomCode)?.recommendFlag,
                frontendType = FrontendTypeEnum.getFrontendTypeObj(htmlTemplateVersion),
                yamlFlag = true,
                dailyStatisticList = getRecentDailyStatisticList(atomCode),
                editFlag = marketAtomCommonService.checkEditCondition(atomCode),
                honorInfos = storeHonorService.getStoreHonor(userId, StoreTypeEnum.ATOM, atomCode),
                indexInfos = storeIndexManageService.getStoreIndexInfosByStoreCode(StoreTypeEnum.ATOM, atomCode),
                serviceScopeDetails = serviceScopeDetails
            )
        )
    }

    private fun resolveInitProjectCode(htmlTemplateVersion: String, atomCode: String): String? {
        return if (htmlTemplateVersion == FrontendTypeEnum.HISTORY.typeVersion) {
            ""
        } else {
            storeProjectRelDao.getInitProjectCodeByStoreCode(
                dslContext = dslContext,
                storeCode = atomCode,
                storeType = StoreTypeEnum.ATOM.type.toByte()
            )
        }
    }

    private fun loadRepositoryInfoSafely(
        atomCode: String,
        projectCode: String?,
        repositoryHashId: String?
    ): Repository? {
        return try {
            getRepositoryInfo(projectCode, repositoryHashId).data
        } catch (ignored: Throwable) {
            logger.warn("atom($atomCode) getAtomVersion|get repository info failed", ignored)
            null
        }
    }

    private fun resolveClassifyLanName(classifyCode: String?, classifyName: String?): String? {
        return if (classifyCode != null) {
            I18nUtil.getCodeLanMessage(
                messageCode = "${StoreTypeEnum.ATOM.name}.classify.$classifyCode",
                defaultMessage = classifyName
            )
        } else {
            classifyName
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
    override fun getAtomByCode(userId: String, atomCode: String, serviceScope: ServiceScopeEnum?): Result<AtomVersion?> {
        val record = marketAtomDao.getLatestAtomByCode(dslContext, atomCode)
        return (if (null == record) {
            Result(data = null)
        } else {
            getAtomVersion(record.id, userId, serviceScope)
        })
    }

    /**
     * 根据标识获取最新版本信息（若最新版本为测试中，取最新版本，否则取最新正式版本）
     */
    override fun getNewestAtomByCode(
        userId: String,
        atomCode: String,
        serviceScope: ServiceScopeEnum?
    ): Result<AtomVersion?> {
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
            getAtomVersion(record.id, userId, serviceScope)
        }
    }

    /**
     * 安装插件到项目
     */
    @BkTimed(extraTags = ["web_operation", "installAtom"], value = "store_web_operation")
    override fun installAtom(
        userId: String,
        channelCode: ChannelCode,
        installAtomReq: InstallAtomReq
    ): Result<Boolean> {
        // 判断插件标识是否合法
        logger.info("installAtom params:[$userId|$channelCode|$installAtomReq]")
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
            storeId = atom.id,
            installStoreReq = InstallStoreReq(
                projectCodes = installAtomReq.projectCode,
                storeCode = atom.atomCode,
                storeType = StoreTypeEnum.ATOM
            ),
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
                    language = it.language,
                    name = I18nUtil.getCodeLanMessage(it.language)
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
                messageCode = StoreMessageCode.USER_COMPONENT_RELEASED_IS_NOT_ALLOW_DELETE,
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
    ): String = atomYamlGenerateService.generateCiYaml(atomCode, os, classType, defaultShowFlag)

    override fun generateCiV2Yaml(
        atomCode: String,
        os: String?,
        classType: String?,
        defaultShowFlag: Boolean?
    ): String = atomYamlGenerateService.generateCiV2Yaml(atomCode, os, classType, defaultShowFlag)

    override fun getAtomOutput(atomCode: String): List<AtomOutput> =
        atomPropsService.getAtomOutput(atomCode)

    override fun getAtomsRely(getRelyAtom: GetRelyAtom): Map<String, Map<String, Any>> =
        atomPropsService.getAtomsRely(getRelyAtom)

    override fun getAtomsDefaultValue(atom: ElementThirdPartySearchParam): Map<String, Any> =
        atomPropsService.getAtomsDefaultValue(atom)

    override fun getPostAtoms(projectCode: String, atomItems: Set<AtomPostReqItem>): Result<AtomPostResp> {
        logger.info("getPostAtoms projectCode:$projectCode,atomItems:$atomItems")
        val postAtoms = mutableListOf<AtomPostInfo>()
        atomItems.forEach { atomItem ->
            val atomCode = atomItem.atomCode
            val version = atomItem.version
            val atomEnvResult = marketAtomEnvService.getMarketAtomEnvInfo(projectCode, atomCode, version)
            val atomEnv = atomEnvResult.data
                ?: throw ErrorCodeException(
                    errorCode = StoreMessageCode.USER_ATOM_IS_NOT_ALLOW_USE_IN_PROJECT,
                    params = arrayOf(projectCode, atomCode)
                )
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

    override fun updateAtomConfigCache(
        atomCode: String,
        atomVersion: String,
        kProperty: String,
        props: String?
    ) {
        try {
            val propsJsonStr = props?.takeIf { it.isNotBlank() }
                ?: atomDao.getAtomProps(dslContext, atomCode, atomVersion)
            if (propsJsonStr.isNullOrBlank()) return

            when (kProperty) {
                AtomRunInfo::sensitiveParams.name -> {
                    val params = marketAtomCommonService.getAtomSensitiveParams(propsJsonStr)
                    if (!params.isNullOrEmpty()) {
                        updateAtomRunInfoCache(atomCode, atomVersion) {
                            it.sensitiveParams = params.joinToString(",")
                        }
                    }
                }
                AtomRunInfo::canPauseBeforeRun.name -> {
                    if (marketAtomCommonService.getAtomCanPauseBeforeRun(propsJsonStr)) {
                        updateAtomRunInfoCache(atomCode, atomVersion) {
                            it.canPauseBeforeRun = true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("updateAtomSensitiveCacheConfig atomCode:$atomCode |atomVersion:$atomVersion failed", e)
        }
    }

    private fun updateAtomRunInfoCache(
        atomCode: String,
        atomVersion: String,
        updater: (AtomRunInfo) -> Unit
    ) {
        val atomRunInfoKey = StoreUtils.getStoreRunInfoKey(StoreTypeEnum.ATOM.name, atomCode)
        val atomRunInfoJson = redisOperation.hget(atomRunInfoKey, atomVersion)
        if (!atomRunInfoJson.isNullOrEmpty()) {
            val atomRunInfo = JsonUtil.to(atomRunInfoJson, AtomRunInfo::class.java)
            updater(atomRunInfo)
            redisOperation.hset(
                key = atomRunInfoKey,
                hashKey = atomVersion,
                values = JsonUtil.toJson(atomRunInfo)
            )
        }
    }
}

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
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.AtomBaseInfo
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.common.util.RegexUtils
import com.tencent.devops.model.store.tables.records.TAtomRecord
import com.tencent.devops.process.api.service.ServiceMeasurePipelineResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.atom.AtomApproveRelDao
import com.tencent.devops.store.dao.atom.AtomDao
import com.tencent.devops.store.dao.atom.AtomLabelRelDao
import com.tencent.devops.store.dao.atom.MarketAtomClassifyDao
import com.tencent.devops.store.dao.atom.MarketAtomDao
import com.tencent.devops.store.dao.atom.MarketAtomEnvInfoDao
import com.tencent.devops.store.dao.atom.MarketAtomFeatureDao
import com.tencent.devops.store.dao.atom.MarketAtomVersionLogDao
import com.tencent.devops.store.dao.common.StoreBuildInfoDao
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
import com.tencent.devops.store.pojo.common.HOTTEST
import com.tencent.devops.store.pojo.common.LATEST
import com.tencent.devops.store.pojo.common.MarketItem
import com.tencent.devops.store.pojo.common.StoreDailyStatistic
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
import com.tencent.devops.store.service.common.StoreProjectService
import com.tencent.devops.store.service.common.StoreTotalStatisticService
import com.tencent.devops.store.service.common.StoreUserService
import com.tencent.devops.store.service.websocket.StoreWebsocketService
import com.tencent.devops.store.utils.StoreUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.StringUtils
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
    lateinit var redisOperation: RedisOperation

    @Autowired
    lateinit var client: Client

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
            val labelCodeList = if (labelCode.isNullOrEmpty()) listOf() else labelCode?.split(",")
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

            val atomCodeList = atoms.map {
                it["ATOM_CODE"] as String
            }.toList()
            // 获取可见范围
            val storeType = StoreTypeEnum.ATOM
            val atomVisibleData = storeCommonService.generateStoreVisibleData(atomCodeList, storeType)
            val atomStatisticData = storeTotalStatisticService.getStatisticByCodeList(
                storeType = storeType.type.toByte(),
                storeCodeList = atomCodeList
            )
            // 获取用户
            val memberData = atomMemberService.batchListMember(atomCodeList, storeType).data

            // 获取分类
            val classifyList = classifyService.getAllClassify(storeType.type.toByte()).data
            val classifyMap = mutableMapOf<String, String>()
            classifyList?.forEach {
                classifyMap[it.id] = it.classifyCode
            }

            atoms.forEach {
                val atomCode = it["ATOM_CODE"] as String
                val visibleList = atomVisibleData?.get(atomCode)
                val statistic = atomStatisticData[atomCode]
                val members = memberData?.get(atomCode)
                val defaultFlag = it["DEFAULT_FLAG"] as Boolean
                val flag = storeCommonService.generateInstallFlag(defaultFlag = defaultFlag,
                    members = members,
                    userId = userId,
                    visibleList = visibleList,
                    userDeptList = userDeptList)
                val classifyId = it["CLASSIFY_ID"] as String
                var logoUrl = it["LOGO_URL"] as? String
                logoUrl = if (logoUrl?.contains("?") == true) {
                    logoUrl.plus("&logo=true")
                } else {
                    logoUrl?.plus("?logo=true")
                }
                if (urlProtocolTrim) { // #4796 LogoUrl跟随主站协议
                    logoUrl = RegexUtils.trimProtocol(logoUrl)
                }
                results.add(
                    MarketItem(
                        id = it["ID"] as String,
                        name = it["NAME"] as String,
                        code = atomCode,
                        type = it["JOB_TYPE"] as String,
                        rdType = AtomTypeEnum.getAtomType((it["ATOM_TYPE"] as Byte).toInt()),
                        classifyCode = if (classifyMap.containsKey(classifyId)) classifyMap[classifyId] else "",
                        category = AtomCategoryEnum.getAtomCategory((it["CATEGROY"] as Byte).toInt()),
                        logoUrl = logoUrl,
                        publisher = it["PUBLISHER"] as String,
                        os = if (!StringUtils.isEmpty(it["OS"])) JsonUtil.getObjectMapper().readValue(
                            it["OS"] as String,
                            List::class.java
                        ) as List<String> else null,
                        downloads = statistic?.downloads ?: 0,
                        score = statistic?.score ?: 0.toDouble(),
                        summary = it["SUMMARY"] as? String,
                        flag = flag,
                        publicFlag = it["DEFAULT_FLAG"] as Boolean,
                        buildLessRunFlag = if (it["BUILD_LESS_RUN_FLAG"] == null) {
                            false
                        } else it["BUILD_LESS_RUN_FLAG"] as Boolean,
                        docsLink = if (it["DOCS_LINK"] == null) "" else it["DOCS_LINK"] as String,
                        modifier = it["MODIFIER"] as String,
                        updateTime = DateTimeUtil.toDateTime(it["UPDATE_TIME"] as LocalDateTime),
                        recommendFlag = it["RECOMMEND_FLAG"] as? Boolean,
                        yamlFlag = it["YAML_FLAG"] as? Boolean,
                        recentExecuteNum = statistic?.recentExecuteNum ?: 0
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
        labelInfoList.add(MarketMainItemLabel(LATEST, MessageCodeUtil.getCodeLanMessage(LATEST)))
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
        labelInfoList.add(MarketMainItemLabel(HOTTEST, MessageCodeUtil.getCodeLanMessage(HOTTEST)))
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
            val classifyCode = it["classifyCode"] as String
            if (classifyCode != "trigger") {
                val classifyName = it["classifyName"] as String
                val classifyLanName = MessageCodeUtil.getCodeLanMessage(
                    messageCode = "${StoreMessageCode.MSG_CODE_STORE_CLASSIFY_PREFIX}$classifyCode",
                    defaultMessage = classifyName
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
        logger.info("the getMyAtoms userId is :$userId,atomName is :$atomName")
        // 获取有权限的插件代码列表
        val records = marketAtomDao.getMyAtoms(dslContext, userId, atomName, page, pageSize)
        val count = marketAtomDao.countMyAtoms(dslContext, userId, atomName)
        // 获取项目ID对应的名称
        val projectCodeList = mutableListOf<String>()
        val atomCodeList = mutableListOf<String>()
        val atomProjectMap = mutableMapOf<String, String>()
        records?.forEach {
            val atomCode = it["atomCode"] as String
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
            val atomCode = it["atomCode"] as String
            var releaseFlag = false // 是否有处于上架状态的插件插件版本
            val releaseAtomNum = marketAtomDao.countReleaseAtomByCode(dslContext, atomCode)
            if (releaseAtomNum > 0) {
                releaseFlag = true
            }
            myAtoms.add(
                MyAtomRespItem(
                    atomId = it["atomId"] as String,
                    name = it["name"] as String,
                    atomCode = atomCode,
                    language = it["language"] as? String,
                    category = AtomCategoryEnum.getAtomCategory((it["category"] as Byte).toInt()),
                    logoUrl = it["logoUrl"] as? String,
                    version = it["version"] as String,
                    atomStatus = AtomStatusEnum.getAtomStatus((it["atomStatus"] as Byte).toInt()),
                    projectName = projectMap?.get(atomProjectMap[atomCode]) ?: "",
                    releaseFlag = releaseFlag,
                    creator = it["creator"] as String,
                    modifier = it["modifier"] as String,
                    createTime = DateTimeUtil.toDateTime(it["createTime"] as LocalDateTime),
                    updateTime = DateTimeUtil.toDateTime(it["updateTime"] as LocalDateTime),
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

    @Suppress("UNCHECKED_CAST")
    @BkTimed(extraTags = ["web_operation", "getAtomVersion"], value = "store_web_operation")
    private fun getAtomVersion(atomId: String, userId: String): Result<AtomVersion?> {
        val record = marketAtomDao.getAtomById(dslContext, atomId)
        return if (null == record) {
            Result(data = null)
        } else {
            val atomCode = record["atomCode"] as String
            val defaultFlag = record["defaultFlag"] as Boolean
            val htmlTemplateVersion = record["htmlTemplateVersion"] as String
            val projectCode =
                if (htmlTemplateVersion == "1.0") "" else storeProjectRelDao.getInitProjectCodeByStoreCode(
                    dslContext,
                    atomCode,
                    StoreTypeEnum.ATOM.type.toByte()
                )
            val repositoryHashId = record["repositoryHashId"] as? String
            val repositoryInfoResult = getRepositoryInfo(projectCode, repositoryHashId)
            if (repositoryInfoResult.isNotOk()) {
                Result(repositoryInfoResult.status, repositoryInfoResult.message, null)
            }
            val repositoryInfo = repositoryInfoResult.data
            val flag = storeUserService.isCanInstallStoreComponent(defaultFlag, userId, atomCode, StoreTypeEnum.ATOM)
            val labelList = atomLabelService.getLabelsByAtomId(atomId) // 查找标签列表
            val userCommentInfo = storeCommentService.getStoreUserCommentInfo(userId, atomCode, StoreTypeEnum.ATOM)
            val atomEnvInfoRecord = marketAtomEnvInfoDao.getMarketAtomEnvInfoByAtomId(dslContext, atomId)
            val feature = marketAtomFeatureDao.getAtomFeature(dslContext, atomCode)
            val classifyCode = record["classifyCode"] as? String
            val classifyName = record["classifyName"] as? String
            val classifyLanName = if (classifyCode != null) {
                MessageCodeUtil.getCodeLanMessage(
                    messageCode = "${StoreMessageCode.MSG_CODE_STORE_CLASSIFY_PREFIX}$classifyCode",
                    defaultMessage = classifyName
                )
            } else classifyName
            val releaseType = if (record["releaseType"] != null) {
                ReleaseTypeEnum.getReleaseTypeObj((record["releaseType"] as Byte).toInt())
            } else null
            val atomStatus = AtomStatusEnum.getAtomStatus((record["atomStatus"] as Byte).toInt())
            val version = record["version"] as? String
            Result(
                AtomVersion(
                    atomId = atomId,
                    atomCode = atomCode,
                    name = record["name"] as String,
                    logoUrl = record["logoUrl"] as? String,
                    classifyCode = classifyCode,
                    classifyName = classifyLanName,
                    category = AtomCategoryEnum.getAtomCategory((record["category"] as Byte).toInt()),
                    docsLink = record["docsLink"] as? String,
                    htmlTemplateVersion = htmlTemplateVersion,
                    atomType = AtomTypeEnum.getAtomType((record["atomType"] as Byte).toInt()),
                    jobType = record["jobType"] as? String,
                    os = if (!StringUtils.isEmpty(record["os"])) JsonUtil.getObjectMapper().readValue(
                        record["os"] as String,
                        List::class.java
                    ) as List<String> else null,
                    summary = record["summary"] as? String,
                    description = record["description"] as? String,
                    version = version,
                    atomStatus = atomStatus,
                    releaseType = releaseType?.name,
                    versionContent = record["versionContent"] as? String,
                    language = record["language"] as? String,
                    codeSrc = record["codeSrc"] as? String,
                    publisher = record["publisher"] as String,
                    modifier = record["modifier"] as String,
                    creator = record["creator"] as String,
                    createTime = DateTimeUtil.toDateTime(record["createTime"] as LocalDateTime),
                    updateTime = DateTimeUtil.toDateTime(record["updateTime"] as LocalDateTime),
                    flag = flag,
                    repositoryAuthorizer = repositoryInfo?.userName,
                    defaultFlag = defaultFlag,
                    projectCode = storeProjectRelDao.getUserStoreTestProjectCode(
                        dslContext,
                        userId,
                        atomCode,
                        StoreTypeEnum.ATOM
                    ),
                    initProjectCode = projectCode,
                    labelList = labelList ?: emptyList(),
                    pkgName = atomEnvInfoRecord?.pkgName,
                    userCommentInfo = userCommentInfo,
                    visibilityLevel = VisibilityLevelEnum.getVisibilityLevel(record["visibilityLevel"] as Int),
                    privateReason = record["privateReason"] as? String,
                    recommendFlag = feature?.recommendFlag,
                    frontendType = FrontendTypeEnum.getFrontendTypeObj(htmlTemplateVersion),
                    // 开启插件yml显示
                    yamlFlag = true,
                    editFlag = marketAtomCommonService.checkEditCondition(atomCode),
                    dailyStatisticList = getRecentDailyStatisticList(atomCode)
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
            MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(atomCode))
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
        logger.info("installAtom accessToken is: $accessToken, userId is: $userId")
        logger.info("installAtom channelCode is: $channelCode, installAtomReq is: $installAtomReq")
        val atom = marketAtomDao.getLatestAtomByCode(dslContext, installAtomReq.atomCode)
        if (null == atom || atom.deleteFlag == true) {
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_INSTALL_ATOM_CODE_IS_INVALID, false)
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
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf("$atomCode+$version"),
                false
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
                errorCode = CommonMessageCode.PERMISSION_DENIED,
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
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED, arrayOf(atomCode))
        }
        val releasedCount = marketAtomDao.countReleaseAtomByCode(dslContext, atomCode)
        logger.info("releasedCount: $releasedCount")
        if (releasedCount > 0) {
            return MessageCodeUtil.generateResponseDataObject(
                StoreMessageCode.USER_ATOM_RELEASED_IS_NOT_ALLOW_DELETE,
                arrayOf(atomCode)
            )
        }
        // 如果已经有流水线在使用该插件，则不能删除
        val pipelineStat =
            client.get(ServiceMeasurePipelineResource::class).batchGetPipelineCountByAtomCode(atomCode, null).data
        val pipelines = pipelineStat?.get(atomCode) ?: 0
        if (pipelines > 0) {
            return MessageCodeUtil.generateResponseDataObject(
                StoreMessageCode.USER_ATOM_USED_IS_NOT_ALLOW_DELETE,
                arrayOf(atomCode)
            )
        }
        // 删除仓库插件包文件
        val initProjectCode =
            storeProjectRelDao.getInitProjectCodeByStoreCode(dslContext, atomCode, StoreTypeEnum.ATOM.type.toByte())
        val deleteAtomFileResult =
            client.get(ServiceArchiveAtomResource::class).deleteAtomFile(userId, initProjectCode!!, atomCode)
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
            marketAtomEnvInfoDao.deleteAtomEnvInfo(context, atomCode)
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
        val propMap = JsonUtil.toMap(atom.props)
        val outputDataMap = propMap[ATOM_OUTPUT] as? Map<String, Any>
        return outputDataMap?.keys?.map { outputKey ->
            val outputDataObj = outputDataMap[outputKey] as Map<String, Any>
            AtomOutput(
                name = outputKey,
                desc = if (outputDataObj[OUTPUT_DESC] == null) {
                    null
                } else {
                    outputDataObj[OUTPUT_DESC].toString()
                }
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
        logger.info("getAtomsRely atomList : $atomList")
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
            val props: Map<String, Any> = jacksonObjectMapper().readValue(value.props)
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

        val props: Map<String, Any> = jacksonObjectMapper().readValue(atom.props)
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
                val requiredName = MessageCodeUtil.getCodeLanMessage(REQUIRED)
                val defaultName = MessageCodeUtil.getCodeLanMessage(DEFAULT)
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
        val sb = StringBuilder()
        if (defaultShowFlag != null && defaultShowFlag) {
            sb.append("h2. ${atom.name}\r\n")
                .append("{code:theme=Midnight|linenumbers=true|language=YAML|collapse=false}\r\n")
        }
        val latestVersion = "${atom.version.split('.').first()}.*"
        sb.append("- uses: ${atom.atomCode}@$latestVersion\r\n")
            .append("  name: ${atom.name}\r\n")

        val props: Map<String, Any> = jacksonObjectMapper().readValue(atom.props)
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
                    MessageCodeUtil.getCodeLanMessage(NO_LABEL)
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
                val requiredName = MessageCodeUtil.getCodeLanMessage(REQUIRED)
                val defaultName = MessageCodeUtil.getCodeLanMessage(DEFAULT)
                val optionsName = MessageCodeUtil.getCodeLanMessage(OPTIONS)
                val multipleName = MessageCodeUtil.getCodeLanMessage(MULTIPLE_SELECTOR)
                val singleName = MessageCodeUtil.getCodeLanMessage(SINGLE_SELECTOR)
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
                    sb.insert(0, "# 参数[$paramKey] 的${ignored.message} ,请检查task.json配置格式是否正确 \n")
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
            try {
                parseRely(builder, rely as Map<String, Any>)
            } catch (e: Exception) {
                throw Exception("rely 配置解析错误")
            }
        }
        val options = paramValueMap["options"]
        if (null != options) {
            builder.append(", $selectorTypeName")
            builder.append(", $optionsName:")
            try {
                parseOptions(builder, options as List<Map<String, Any>>)
            } catch (e: Exception) {
                throw Exception("options 配置解析错误")
            }
        }
        val list = paramValueMap["list"]
        if (null != list) {
            builder.append(", $optionsName:")
            try {
                parseList(builder, list as List<Map<String, Any>>)
            } catch (e: Exception) {
                throw Exception("list 配置解析错误")
            }
        }
    }

    private fun parseRely(builder: StringBuilder, rely: Map<String, Any>) {
        val dang = MessageCodeUtil.getCodeLanMessage(DANG)
        val and = MessageCodeUtil.getCodeLanMessage(AND)
        val or = MessageCodeUtil.getCodeLanMessage(OR)
        val timeToSelect = MessageCodeUtil.getCodeLanMessage(TIMETOSELECT)
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
        } catch (e: Exception) {
            println("load atom input[rely] with error: ${e.message}")
        }
    }

    private fun parseOptions(builder: StringBuilder, options: List<Map<String, Any>>) {
        try {
            options.forEachIndexed { index, map ->
                if (index == options.size - 1) builder.append(" ${map["id"]}[${map["name"]}]")
                else builder.append(" ${map["id"]}[${map["name"]}] |")
            }
            builder.removeSuffix("|")
        } catch (e: Exception) {
            println("load atom input[options] with error: ${e.message}")
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
        } catch (e: Exception) {
            println("load atom input[list] with error: ${e.message} ")
        }
    }
}

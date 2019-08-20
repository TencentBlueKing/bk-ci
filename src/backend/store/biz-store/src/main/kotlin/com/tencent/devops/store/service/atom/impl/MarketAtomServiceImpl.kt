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

import com.tencent.devops.artifactory.api.ServiceImageManageResource
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.DOING
import com.tencent.devops.common.api.constant.FAIL
import com.tencent.devops.common.api.constant.ING
import com.tencent.devops.common.api.constant.SUCCESS
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.model.store.tables.records.TAtomRecord
import com.tencent.devops.project.api.ServiceProjectResource
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.atom.AtomDao
import com.tencent.devops.store.dao.atom.AtomLabelRelDao
import com.tencent.devops.store.dao.atom.MarketAtomClassifyDao
import com.tencent.devops.store.dao.atom.MarketAtomDao
import com.tencent.devops.store.dao.atom.MarketAtomEnvInfoDao
import com.tencent.devops.store.dao.atom.MarketAtomOfflineDao
import com.tencent.devops.store.dao.atom.MarketAtomPipelineBuildRelDao
import com.tencent.devops.store.dao.atom.MarketAtomVersionLogDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.pojo.atom.AtomBuildInfo
import com.tencent.devops.store.pojo.atom.AtomEnvRequest
import com.tencent.devops.store.pojo.atom.AtomOfflineReq
import com.tencent.devops.store.pojo.atom.AtomProcessInfo
import com.tencent.devops.store.pojo.atom.AtomVersion
import com.tencent.devops.store.pojo.atom.AtomVersionListItem
import com.tencent.devops.store.pojo.atom.AtomVersionListResp
import com.tencent.devops.store.pojo.atom.GetAtomConfigResult
import com.tencent.devops.store.pojo.atom.MarketAtomCreateRequest
import com.tencent.devops.store.pojo.atom.MarketAtomResp
import com.tencent.devops.store.pojo.atom.MarketAtomUpdateRequest
import com.tencent.devops.store.pojo.atom.MarketMainItem
import com.tencent.devops.store.pojo.atom.MarketMainItemLabel
import com.tencent.devops.store.pojo.atom.MyAtomResp
import com.tencent.devops.store.pojo.atom.MyAtomRespItem
import com.tencent.devops.store.pojo.atom.enums.AtomCategoryEnum
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.atom.enums.AtomTypeEnum
import com.tencent.devops.store.pojo.atom.enums.MarketAtomSortTypeEnum
import com.tencent.devops.store.pojo.atom.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.HOTTEST
import com.tencent.devops.store.pojo.common.LATEST
import com.tencent.devops.store.pojo.common.MarketItem
import com.tencent.devops.store.pojo.common.ReleaseProcessItem
import com.tencent.devops.store.pojo.common.UN_RELEASE
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.AtomLabelService
import com.tencent.devops.store.service.atom.MarketAtomCommonService
import com.tencent.devops.store.service.atom.MarketAtomService
import com.tencent.devops.store.service.atom.MarketAtomStatisticService
import com.tencent.devops.store.service.common.ClassifyService
import com.tencent.devops.store.service.common.StoreCommentService
import com.tencent.devops.store.service.common.StoreMemberService
import com.tencent.devops.store.service.common.StoreProjectService
import com.tencent.devops.store.service.common.StoreUserService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.util.StringUtils
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

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
    lateinit var marketAtomEnvInfoDao: MarketAtomEnvInfoDao
    @Autowired
    lateinit var storeMemberDao: StoreMemberDao
    @Autowired
    lateinit var marketAtomVersionLogDao: MarketAtomVersionLogDao
    @Autowired
    lateinit var marketAtomOfflineDao: MarketAtomOfflineDao
    @Autowired
    lateinit var marketAtomClassifyDao: MarketAtomClassifyDao
    @Autowired
    lateinit var marketAtomPipelineBuildRelDao: MarketAtomPipelineBuildRelDao
    @Autowired
    lateinit var atomLabelRelDao: AtomLabelRelDao
    @Autowired
    lateinit var marketAtomStatisticService: MarketAtomStatisticService
    @Autowired
    lateinit var atomLabelService: AtomLabelService
    @Autowired
    lateinit var storeProjectService: StoreProjectService
    @Autowired
    lateinit var storeUserService: StoreUserService
    @Autowired
    lateinit var storeMemberService: StoreMemberService
    @Autowired
    lateinit var storeCommentService: StoreCommentService
    @Autowired
    lateinit var classifyService: ClassifyService
    @Autowired
    lateinit var marketAtomCommonService: MarketAtomCommonService
    @Autowired
    lateinit var redisOperation: RedisOperation
    @Autowired
    lateinit var client: Client

    companion object {
        private val logger = LoggerFactory.getLogger(MarketAtomServiceImpl::class.java)
        private val executor = Executors.newFixedThreadPool(30)
    }

    @Value("\${store.atomDetailBaseUrl}")
    protected lateinit var atomDetailBaseUrl: String

    @Suppress("UNCHECKED_CAST")
    private fun doList(
        userId: String,
        userDeptList: List<Int>,
        atomName: String?,
        classifyCode: String?,
        labelCode: String?,
        score: Int?,
        rdType: AtomTypeEnum?,
        sortType: MarketAtomSortTypeEnum?,
        desc: Boolean?,
        page: Int?,
        pageSize: Int?
    ): Future<MarketAtomResp> {
        return executor.submit(Callable<MarketAtomResp> {

            val results = mutableListOf<MarketItem>()
            // 获取插件
            val labelCodeList = if (labelCode.isNullOrEmpty()) listOf() else labelCode?.split(",")
            val count = marketAtomDao.count(dslContext, atomName, classifyCode, labelCodeList, score, rdType)
            val atoms = marketAtomDao.list(
                dslContext,
                atomName,
                classifyCode,
                labelCodeList,
                score,
                rdType,
                sortType,
                desc,
                page,
                pageSize
            )
                ?: return@Callable MarketAtomResp(0, page, pageSize, results)
            logger.info("[list]get atoms: $atoms")

            val atomCodeList = atoms.map {
                it["ATOM_CODE"] as String
            }.toList()
            // 获取可见范围
            val atomVisibleData = generateAtomVisibleData(atomCodeList, StoreTypeEnum.ATOM).data
            logger.info("[list]get atomVisibleData:$atomVisibleData")
            // 获取热度
            val statField = mutableListOf<String>()
            statField.add("DOWNLOAD")
            val atomStatisticData = marketAtomStatisticService.getStatisticByCodeList(atomCodeList, statField).data
            logger.info("[list]get atomStatisticData:$atomStatisticData")
            // 获取用户
            val memberData = storeMemberService.batchListMember(atomCodeList, StoreTypeEnum.ATOM).data
            // 获取分类
            val classifyList = classifyService.getAllClassify(StoreTypeEnum.ATOM.type.toByte()).data
            val classifyMap = mutableMapOf<String, String>()
            classifyList?.forEach {
                classifyMap[it.id] = it.classifyCode
            }
            atoms.forEach {
                val atomCode = it["ATOM_CODE"] as String
                val visibleList = atomVisibleData?.get(atomCode)
                val statistic = atomStatisticData?.get(atomCode)
                val members = memberData?.get(atomCode)
                val defaultFlag = it["DEFAULT_FLAG"] as Boolean
                val flag = generateInstallFlag(defaultFlag, members, userId, visibleList, userDeptList)
                val classifyId = it["CLASSIFY_ID"] as String
                results.add(
                    MarketItem(
                        id = it["ID"] as String,
                        name = it["NAME"] as String,
                        code = atomCode,
                        type = it["JOB_TYPE"] as String,
                        rdType = AtomTypeEnum.getAtomType((it["ATOM_TYPE"] as Byte).toInt()),
                        classifyCode = if (classifyMap.containsKey(classifyId)) classifyMap[classifyId] else "",
                        logoUrl = it["LOGO_URL"] as? String,
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
                        buildLessRunFlag = if (it["BUILD_LESS_RUN_FLAG"] == null) false else it["BUILD_LESS_RUN_FLAG"] as Boolean,
                        docsLink = if (it["DOCS_LINK"] == null) "" else it["DOCS_LINK"] as String
                    )
                )
            }

            logger.info("[list]end")
            return@Callable MarketAtomResp(count, page, pageSize, results)
        })
    }

    abstract fun generateInstallFlag(
        defaultFlag: Boolean,
        members: MutableList<String>?,
        userId: String,
        visibleList: MutableList<Int>?,
        userDeptList: List<Int>
    ): Boolean

    abstract fun generateAtomVisibleData(
        storeCodeList: List<String?>,
        storeType: StoreTypeEnum
    ): Result<HashMap<String, MutableList<Int>>?>

    /**
     * 插件市场，首页
     */
    override fun mainPageList(
        userId: String,
        page: Int?,
        pageSize: Int?
    ): Result<List<MarketMainItem>> {
        val result = mutableListOf<MarketMainItem>()
        // 获取用户组织架构
        val userDeptList = storeUserService.getUserDeptList(userId)
        logger.info("[list]get userDeptList")
        val futureList = mutableListOf<Future<MarketAtomResp>>()
        val labelInfoList = mutableListOf<MarketMainItemLabel>()
        labelInfoList.add(MarketMainItemLabel(LATEST, MessageCodeUtil.getCodeLanMessage(LATEST)))
        futureList.add(
            doList(
                userId = userId,
                userDeptList = userDeptList,
                atomName = null,
                classifyCode = null,
                labelCode = null,
                score = null,
                rdType = null,
                sortType = MarketAtomSortTypeEnum.UPDATE_TIME,
                desc = true,
                page = page,
                pageSize = pageSize
            )
        )
        labelInfoList.add(MarketMainItemLabel(HOTTEST, MessageCodeUtil.getCodeLanMessage(HOTTEST)))
        futureList.add(
            doList(
                userId = userId,
                userDeptList = userDeptList,
                atomName = null,
                classifyCode = null,
                labelCode = null,
                score = null,
                rdType = null,
                sortType = MarketAtomSortTypeEnum.DOWNLOAD_COUNT,
                desc = true,
                page = page,
                pageSize = pageSize
            )
        )

        val classifyList = marketAtomClassifyDao.getAllAtomClassify(dslContext)
        classifyList?.forEach {
            val classifyCode = it["classifyCode"] as String
            if (classifyCode != "trigger") {
                labelInfoList.add(MarketMainItemLabel(classifyCode, it["classifyName"] as String))
                futureList.add(
                    doList(
                        userId = userId,
                        userDeptList = userDeptList,
                        atomName = null,
                        classifyCode = classifyCode,
                        labelCode = null,
                        score = null,
                        rdType = null,
                        sortType = MarketAtomSortTypeEnum.NAME,
                        desc = false,
                        page = page,
                        pageSize = pageSize
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
    override fun list(
        userId: String,
        atomName: String?,
        classifyCode: String?,
        labelCode: String?,
        score: Int?,
        rdType: AtomTypeEnum?,
        sortType: MarketAtomSortTypeEnum?,
        page: Int?,
        pageSize: Int?
    ): MarketAtomResp {
        logger.info("[list]enter")

        // 获取用户组织架构
        val userDeptList = storeUserService.getUserDeptList(userId)
        logger.info("[list]get userDeptList:$userDeptList")

        return doList(
            userId = userId,
            userDeptList = userDeptList,
            atomName = atomName,
            classifyCode = classifyCode,
            labelCode = labelCode,
            score = score,
            rdType = rdType,
            sortType = sortType,
            desc = true,
            page = page,
            pageSize = pageSize
        ).get()
    }

    /**
     * 根据用户和插件名称获取插件信息
     */
    override fun getMyAtoms(
        accessToken: String,
        userId: String,
        atomName: String?,
        page: Int?,
        pageSize: Int?
    ): Result<MyAtomResp?> {
        logger.info("the getMyAtoms userId is :$userId,atomName is :$atomName")
        // 获取有权限的插件代码列表
        val records = marketAtomDao.getMyAtoms(dslContext, userId, atomName, page, pageSize)
        val count = marketAtomDao.countMyAtoms(dslContext, userId, atomName)
        logger.info("the getMyAtoms userId is :$userId,records is :$records,count is :$count")
        // 获取项目ID对应的名称
        val projectCodeList = mutableListOf<String>()
        records?.forEach {
            projectCodeList.add(it["projectCode"] as String)
        }
        logger.info("the getMyAtoms userId is :$userId,projectCodeList is :$projectCodeList")
        val projectMap = client.get(ServiceProjectResource::class).getNameByCode(projectCodeList.joinToString(",")).data
        logger.info("the getMyAtoms userId is :$userId,projectMap is :$projectMap")
        val myAtoms = mutableListOf<MyAtomRespItem?>()
        val df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
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
                    atomCode = it["atomCode"] as String,
                    language = it["language"] as? String,
                    category = AtomCategoryEnum.getAtomCategory((it["category"] as Byte).toInt()),
                    logoUrl = it["logoUrl"] as? String,
                    version = it["version"] as String,
                    atomStatus = AtomStatusEnum.getAtomStatus((it["atomStatus"] as Byte).toInt()),
                    projectName = projectMap?.get(it["projectCode"] as String) as String,
                    releaseFlag = releaseFlag,
                    creator = it["creator"] as String,
                    modifier = it["modifier"] as String,
                    createTime = df.format(it["createTime"] as TemporalAccessor),
                    updateTime = df.format(it["updateTime"] as TemporalAccessor)
                )
            )
        }
        return Result(MyAtomResp(count, page, pageSize, myAtoms))
    }

    protected fun validateAddMarketAtomReq(
        userId: String,
        marketAtomCreateRequest: MarketAtomCreateRequest
    ): Result<Boolean> {
        logger.info("the validateAddMarketAtomReq userId is :$userId,marketAtomCreateRequest is :$marketAtomCreateRequest")
        val atomCode = marketAtomCreateRequest.atomCode
        // 判断插件代码是否存在
        val codeCount = atomDao.countByCode(dslContext, atomCode)
        if (codeCount > 0) {
            // 抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_EXIST,
                arrayOf(atomCode),
                false
            )
        }
        val atomName = marketAtomCreateRequest.name
        // 判断插件名称是否存在
        val nameCount = atomDao.countByName(dslContext, atomName)
        if (nameCount > 0) {
            // 抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_EXIST,
                arrayOf(atomName),
                false
            )
        }
        return Result(true)
    }

    abstract override fun addMarketAtom(
        userId: String,
        marketAtomCreateRequest: MarketAtomCreateRequest
    ): Result<Boolean>

    @Suppress("UNCHECKED_CAST")
    protected fun handleUpdateMarketAtom(
        projectCode: String,
        userId: String,
        taskJsonStr: String,
        atomStatus: AtomStatusEnum,
        marketAtomUpdateRequest: MarketAtomUpdateRequest
    ): Result<String?> {
        logger.info("the handleUpdateMarketAtom userId is :$userId,taskJsonStr is :$taskJsonStr,marketAtomUpdateRequest is :$marketAtomUpdateRequest")
        // 判断插件是不是首次创建版本
        val atomCode = marketAtomUpdateRequest.atomCode
        val atomRecords = marketAtomDao.getAtomsByAtomCode(dslContext, atomCode)
        logger.info("the atomRecords is :$atomRecords")
        if (null == atomRecords || atomRecords.isEmpty()) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(atomCode))
        }
        // 判断更新的插件名称是否重复
        if (validateAtomNameIsExist(
                marketAtomUpdateRequest.name,
                atomRecords
            )
        ) return MessageCodeUtil.generateResponseDataObject(
            CommonMessageCode.PARAMETER_IS_EXIST,
            arrayOf(marketAtomUpdateRequest.name)
        )
        val atomRecord = atomRecords[0]
        // 校验前端传的版本号是否正确
        val releaseType = marketAtomUpdateRequest.releaseType
        val version = marketAtomUpdateRequest.version
        val osList = marketAtomUpdateRequest.os
        val validateAtomVersionResult =
            marketAtomCommonService.validateAtomVersion(atomRecord, releaseType, osList, version)
        logger.info("validateAtomVersionResult is :$validateAtomVersionResult")
        if (validateAtomVersionResult.isNotOk()) {
            return Result(
                status = validateAtomVersionResult.status,
                message = validateAtomVersionResult.message,
                data = null
            )
        }
        var atomId = UUIDUtil.generate()
        val getAtomConfResult = parseTaskJson(taskJsonStr, projectCode, atomCode, version, userId)
        if (getAtomConfResult.errorCode != "0") {
            return MessageCodeUtil.generateResponseDataObject(
                getAtomConfResult.errorCode,
                getAtomConfResult.errorParams
            )
        }

        val taskDataMap = getAtomConfResult.taskDataMap
        val atomEnvRequest = getAtomConfResult.atomEnvRequest ?: return MessageCodeUtil.generateResponseDataObject(
            StoreMessageCode.USER_REPOSITORY_TASK_JSON_FIELD_IS_NULL, arrayOf("execution")
        )
        atomEnvRequest.pkgName = marketAtomUpdateRequest.pkgName
        atomEnvRequest.shaContent = marketAtomUpdateRequest.packageShaContent
        val propsMap = mutableMapOf<String, Any?>()
        propsMap["inputGroups"] = taskDataMap?.get("inputGroups")
        propsMap["input"] = taskDataMap?.get("input")
        propsMap["output"] = taskDataMap?.get("output")

        val classType = if (marketAtomUpdateRequest.os.isEmpty()) "marketBuildLess" else "marketBuild"
        val logoUrl = marketAtomUpdateRequest.logoUrl
        var iconData: String? = ""
        if (null != logoUrl) {
            try {
                iconData = client.get(ServiceImageManageResource::class).compressImage(logoUrl, 18, 18).data
                logger.info("the iconData is :$iconData")
            } catch (e: Exception) {
                logger.error("compressImage error is :$e", e)
            }
        }

        marketAtomUpdateRequest.os.sort() // 给操作系统排序
        dslContext.transaction { t ->
            val context = DSL.using(t)
            val props = JsonUtil.toJson(propsMap)
            if (1 == atomRecords.size) {
                if (StringUtils.isEmpty(atomRecord.version)) {
                    // 首次创建版本
                    atomId = atomRecord.id
                    marketAtomDao.updateMarketAtom(
                        context,
                        userId,
                        atomId,
                        atomStatus,
                        classType,
                        props,
                        iconData,
                        marketAtomUpdateRequest
                    )
                    marketAtomVersionLogDao.addMarketAtomVersion(
                        context,
                        userId,
                        atomId,
                        marketAtomUpdateRequest.releaseType.releaseType.toByte(),
                        marketAtomUpdateRequest.versionContent
                    )
                    marketAtomEnvInfoDao.updateMarketAtomEnvInfo(context, atomRecord.id, atomEnvRequest)
                } else {
                    // 升级插件
                    upgradeMarketAtom(
                        marketAtomUpdateRequest,
                        context,
                        userId,
                        atomId,
                        atomStatus,
                        classType,
                        props,
                        iconData,
                        atomEnvRequest,
                        atomRecord
                    )
                }
            } else {
                // 升级插件
                upgradeMarketAtom(
                    marketAtomUpdateRequest,
                    context,
                    userId,
                    atomId,
                    atomStatus,
                    classType,
                    props,
                    iconData,
                    atomEnvRequest,
                    atomRecord
                )
            }
            // 更新标签信息
            atomLabelRelDao.deleteByAtomId(context, atomId)
            val labelIdList = marketAtomUpdateRequest.labelIdList
            if (null != labelIdList && labelIdList.isNotEmpty()) {
                atomLabelRelDao.batchAdd(context, userId, atomId, labelIdList)
            }
        }
        return Result(atomId)
    }

    abstract override fun updateMarketAtom(
        userId: String,
        projectCode: String,
        marketAtomUpdateRequest: MarketAtomUpdateRequest
    ): Result<String?>

    @Suppress("UNCHECKED_CAST")
    protected fun parseTaskJson(
        taskJsonStr: String,
        projectCode: String,
        atomCode: String,
        version: String,
        userId: String
    ): GetAtomConfigResult {
        val taskDataMap = JsonUtil.toMap(taskJsonStr)
        val getAtomConfResult =
            marketAtomCommonService.parseBaseTaskJson(taskJsonStr, projectCode, atomCode, version, userId)
        return if (getAtomConfResult.errorCode != "0") {
            getAtomConfResult
        } else {
            val executionInfoMap = taskDataMap["execution"] as Map<String, Any>
            val packagePath = executionInfoMap["packagePath"] as? String
            if (!validatePackagePath(packagePath)) {
                GetAtomConfigResult(
                    StoreMessageCode.USER_REPOSITORY_TASK_JSON_FIELD_IS_NULL,
                    arrayOf("packagePath"), null, null
                )
            } else {
                val atomEnvRequest = getAtomConfResult.atomEnvRequest!!
                atomEnvRequest.pkgPath = "$projectCode/$atomCode/$version/$packagePath"
                getAtomConfResult
            }
        }
    }

    abstract fun validatePackagePath(packagePath: String?): Boolean

    private fun validateAtomNameIsExist(
        atomName: String,
        atomRecords: org.jooq.Result<TAtomRecord>
    ): Boolean {
        val count = atomDao.countByName(dslContext, atomName)
        var flag = false
        if (count > 0) {
            for (item in atomRecords) {
                if (atomName == item.name) {
                    flag = true
                    break
                }
            }
            if (!flag) {
                return true
            }
        }
        return false
    }

    private fun upgradeMarketAtom(
        marketAtomUpdateRequest: MarketAtomUpdateRequest,
        context: DSLContext,
        userId: String,
        atomId: String,
        atomStatus: AtomStatusEnum,
        classType: String,
        props: String,
        iconData: String?,
        atomEnvRequest: AtomEnvRequest,
        atomRecord: TAtomRecord
    ) {
        marketAtomDao.upgradeMarketAtom(
            context,
            userId,
            atomId,
            atomStatus,
            classType,
            props,
            iconData,
            atomRecord,
            marketAtomUpdateRequest
        )
        marketAtomEnvInfoDao.addMarketAtomEnvInfo(context, atomId, atomEnvRequest)
        marketAtomVersionLogDao.addMarketAtomVersion(
            context,
            userId,
            atomId,
            marketAtomUpdateRequest.releaseType.releaseType.toByte(),
            marketAtomUpdateRequest.versionContent
        )
    }

    /**
     * 根据插件版本ID获取版本基本信息、发布信息
     */
    @Suppress("UNCHECKED_CAST")
    override fun getAtomById(atomId: String, userId: String): Result<AtomVersion?> {
        return Result(getAtomVersion(atomId, userId))
    }

    @Suppress("UNCHECKED_CAST")
    private fun getAtomVersion(atomId: String, userId: String): AtomVersion? {
        val record = marketAtomDao.getAtomById(dslContext, atomId)
        val df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        return if (null == record) {
            null
        } else {
            val atomCode = record["atomCode"] as String
            val defaultFlag = record["defaultFlag"] as Boolean
            val flag = storeUserService.isCanInstallStoreComponent(defaultFlag, userId, atomCode, StoreTypeEnum.ATOM)
            val labelList = atomLabelService.getLabelsByAtomId(atomId).data // 查找标签列表
            val userCommentInfo = storeCommentService.getStoreUserCommentInfo(userId, atomCode, StoreTypeEnum.ATOM)
            val atomEnvInfoRecord = marketAtomEnvInfoDao.getMarketAtomEnvInfoByAtomId(dslContext, atomId)
            AtomVersion(
                atomId = atomId,
                atomCode = atomCode,
                name = record["name"] as String,
                logoUrl = record["logoUrl"] as? String,
                classifyCode = record["classifyCode"] as? String,
                classifyName = record["classifyName"] as? String,
                category = AtomCategoryEnum.getAtomCategory((record["category"] as Byte).toInt()),
                docsLink = record["docsLink"] as? String,
                atomType = AtomTypeEnum.getAtomType((record["atomType"] as Byte).toInt()),
                jobType = record["jobType"] as? String,
                os = if (!StringUtils.isEmpty(record["os"])) JsonUtil.getObjectMapper().readValue(
                    record["os"] as String,
                    List::class.java
                ) as List<String> else null,
                summary = record["summary"] as? String,
                description = record["description"] as? String,
                version = record["version"] as? String,
                atomStatus = AtomStatusEnum.getAtomStatus((record["atomStatus"] as Byte).toInt()),
                releaseType = if (record["releaseType"] != null) ReleaseTypeEnum.getReleaseType((record["releaseType"] as Byte).toInt()) else null,
                versionContent = record["versionContent"] as? String,
                language = record["language"] as? String,
                codeSrc = record["codeSrc"] as? String,
                publisher = record["publisher"] as String,
                modifier = record["modifier"] as String,
                creator = record["creator"] as String,
                createTime = df.format(record["createTime"] as TemporalAccessor),
                updateTime = df.format(record["updateTime"] as TemporalAccessor),
                flag = flag,
                defaultFlag = defaultFlag,
                projectCode = storeProjectRelDao.getInitProjectCodeByStoreCode(
                    dslContext,
                    atomCode,
                    StoreTypeEnum.ATOM.type.toByte()
                ),
                labelList = labelList,
                pkgName = atomEnvInfoRecord?.pkgName,
                userCommentInfo = userCommentInfo
            )
        }
    }

    /**
     * 根据插件标识获取插件最新、正式版本息
     */
    override fun getAtomByCode(userId: String, atomCode: String): Result<AtomVersion?> {
        val record = marketAtomDao.getLatestAtomByCode(dslContext, atomCode)
        return (if (null == record) {
            Result(0, "", null)
        } else {
            Result(getAtomVersion(record.id, userId))
        })
    }

    /**
     * 安装插件到项目
     */
    override fun installAtom(
        accessToken: String,
        userId: String,
        projectCodeList: ArrayList<String>,
        atomCode: String
    ): Result<Boolean> {
        // 判断插件标识是否合法
        logger.info("atomCode is $atomCode")
        val atom = marketAtomDao.getLatestAtomByCode(dslContext, atomCode)
        logger.info("store is $atom")
        if (null == atom) {
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_INSTALL_ATOM_CODE_IS_INVALID, false)
        }
        return storeProjectService.installStoreComponent(
            accessToken,
            userId,
            projectCodeList,
            atom.id,
            atom.atomCode,
            StoreTypeEnum.ATOM,
            atom.defaultFlag
        )
    }

    /**
     * 根据插件标识获取插件版本列表
     */
    override fun getAtomVersionsByCode(userId: String, atomCode: String): Result<AtomVersionListResp> {
        val records = marketAtomDao.getAtomsByAtomCode(dslContext, atomCode)
        val atomVersions = mutableListOf<AtomVersionListItem?>()
        val df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        records?.forEach {
            atomVersions.add(
                AtomVersionListItem(
                    atomId = it.id,
                    atomCode = it.atomCode,
                    name = it.name,
                    category = AtomCategoryEnum.getAtomCategory((it.categroy as Byte).toInt()),
                    version = it.version,
                    atomStatus = AtomStatusEnum.getAtomStatus((it.atomStatus as Byte).toInt()),
                    creator = it.creator,
                    createTime = df.format(it.createTime)
                )
            )
        }

        return Result(AtomVersionListResp(atomVersions.size, atomVersions))
    }

    /**
     * 获取插件版本发布进度
     */
    override fun getProcessInfo(atomId: String): Result<AtomProcessInfo> {
        val record = marketAtomDao.getAtomById(dslContext, atomId)
        if (null == record) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(atomId))
        } else {
            val status = record["atomStatus"].toString().toInt()
            val processInfo = handleProcessInfo(status)
            val atomProcessInfo = AtomProcessInfo(null, processInfo)
            val atomBuildInfoRecord = marketAtomPipelineBuildRelDao.getAtomPipelineBuildRel(dslContext, atomId)
            val projectCode = storeProjectRelDao.getInitProjectCodeByStoreCode(
                dslContext,
                record["atomCode"] as String,
                StoreTypeEnum.ATOM.type.toByte()
            )
            if (null != atomBuildInfoRecord) {
                atomProcessInfo.atomBuildInfo = AtomBuildInfo(
                    atomId = atomBuildInfoRecord.atomId,
                    pipelineId = atomBuildInfoRecord.pipelineId,
                    buildId = atomBuildInfoRecord.buildId,
                    projectCode = projectCode!!
                )
            }
            return Result(atomProcessInfo)
        }
    }

    abstract fun handleProcessInfo(status: Int): List<ReleaseProcessItem>

    /**
     * 设置插件版本进度
     */
    protected fun setProcessInfo(
        processInfo: List<ReleaseProcessItem>,
        totalStep: Int,
        currStep: Int,
        status: String
    ): Boolean {
        for (item in processInfo) {
            if (item.step < currStep) {
                item.status = SUCCESS
            } else if (item.step == currStep) {
                if (currStep == totalStep) {
                    item.status = SUCCESS
                } else {
                    item.status = status
                    item.name += if (status == DOING) MessageCodeUtil.getCodeLanMessage(ING) else MessageCodeUtil.getCodeLanMessage(
                        FAIL
                    )
                }
            }
        }
        return true
    }

    /**
     * 取消发布
     */
    override fun cancelRelease(userId: String, atomId: String): Result<Boolean> {
        logger.info("cancelRelease, userId=$userId, atomId=$atomId")
        val status = AtomStatusEnum.GROUNDING_SUSPENSION.status.toByte()
        val (checkResult, code) = checkAtomVersionOptRight(userId, atomId, status)
        if (!checkResult) {
            return MessageCodeUtil.generateResponseDataObject(code)
        }
        marketAtomDao.setAtomStatusById(
            dslContext,
            atomId,
            status,
            userId,
            MessageCodeUtil.getCodeLanMessage(UN_RELEASE)
        )
        return Result(true)
    }

    abstract fun getPassTestStatus(): Byte

    abstract fun handlePassTest(userId: String, atomId: String): Result<Boolean>

    /**
     * 通过测试
     */
    override fun passTest(userId: String, atomId: String): Result<Boolean> {
        logger.info("passTest, userId=$userId, atomId=$atomId")
        val status = getPassTestStatus()
        val (checkResult, code) = checkAtomVersionOptRight(userId, atomId, status)
        if (!checkResult) {
            return MessageCodeUtil.generateResponseDataObject(code)
        }
        val result = handlePassTest(userId, atomId)
        logger.info("passTest result=$result")
        if (result.isNotOk()) {
            return result
        }
        return Result(true)
    }

    /**
     * 检查版本发布过程中的操作权限：重新构建、确认测试完成、取消发布
     */
    private fun checkAtomVersionOptRight(userId: String, atomId: String, status: Byte): Pair<Boolean, String> {
        val record =
            marketAtomDao.getAtomById(dslContext, atomId) ?: return Pair(false, CommonMessageCode.PARAMETER_IS_INVALID)
        val atomCode = record["atomCode"] as String
        val creator = record["creator"] as String
        val recordStatus = record["atomStatus"] as Byte

        // 判断用户是否有权限
        if (!(storeMemberDao.isStoreAdmin(
                dslContext,
                userId,
                atomCode,
                StoreTypeEnum.ATOM.type.toByte()
            ) || creator == userId)
        ) {
            return Pair(false, CommonMessageCode.PERMISSION_DENIED)
        }

        logger.info("record status=$recordStatus, status=$status")
        if (status == AtomStatusEnum.AUDITING.status.toByte() &&
            recordStatus != AtomStatusEnum.TESTING.status.toByte()
        ) {
            return Pair(false, StoreMessageCode.USER_ATOM_RELEASE_STEPS_ERROR)
        } else if (status == AtomStatusEnum.BUILDING.status.toByte() &&
            recordStatus !in (listOf(AtomStatusEnum.BUILD_FAIL.status.toByte(), AtomStatusEnum.TESTING.status.toByte()))
        ) {
            return Pair(false, StoreMessageCode.USER_ATOM_RELEASE_STEPS_ERROR)
        } else if (status == AtomStatusEnum.GROUNDING_SUSPENSION.status.toByte() &&
            recordStatus in (listOf(AtomStatusEnum.RELEASED.status.toByte()))
        ) {
            return Pair(false, StoreMessageCode.USER_ATOM_RELEASE_STEPS_ERROR)
        }

        return Pair(true, "")
    }

    /**
     * 处理用户提交的下架插件请求
     */
    override fun offlineAtom(userId: String, atomCode: String, atomOfflineReq: AtomOfflineReq): Result<Boolean> {
        // 判断用户是否有权限下线
        if (!storeMemberDao.isStoreAdmin(dslContext, userId, atomCode, StoreTypeEnum.ATOM.type.toByte())) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }

        // 初始化下线记录
        marketAtomOfflineDao.create(dslContext, atomCode, atomOfflineReq.bufferDay, userId, 0)
        // 设置插件状态为下架中
        marketAtomDao.setAtomStatusByCode(
            dslContext, atomCode, AtomStatusEnum.RELEASED.status.toByte(),
            AtomStatusEnum.UNDERCARRIAGING.status.toByte(), userId, atomOfflineReq.reason
        )

        // 通知使用方插件即将下架 -- todo

        return Result(true)
    }
}

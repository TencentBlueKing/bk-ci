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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.store.tables.TAtom
import com.tencent.devops.model.store.tables.records.TAtomEnvInfoRecord
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.atom.AtomDao
import com.tencent.devops.store.dao.atom.MarketAtomDao
import com.tencent.devops.store.dao.atom.MarketAtomEnvInfoDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.factory.AtomBusHandleFactory
import com.tencent.devops.store.pojo.atom.AtomEnv
import com.tencent.devops.store.pojo.atom.AtomEnvRequest
import com.tencent.devops.store.pojo.atom.AtomPostInfo
import com.tencent.devops.store.pojo.atom.AtomRunInfo
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.atom.enums.JobTypeEnum
import com.tencent.devops.store.pojo.common.ATOM_POST_CONDITION
import com.tencent.devops.store.pojo.common.ATOM_POST_ENTRY_PARAM
import com.tencent.devops.store.pojo.common.ATOM_POST_FLAG
import com.tencent.devops.store.pojo.common.ATOM_POST_NORMAL_PROJECT_FLAG_KEY_PREFIX
import com.tencent.devops.store.pojo.common.ATOM_POST_VERSION_TEST_FLAG_KEY_PREFIX
import com.tencent.devops.store.pojo.common.StoreVersion
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.AtomService
import com.tencent.devops.store.service.atom.MarketAtomCommonService
import com.tencent.devops.store.service.atom.MarketAtomEnvService
import com.tencent.devops.store.service.common.StoreI18nMessageService
import com.tencent.devops.store.utils.StoreUtils
import com.tencent.devops.store.utils.VersionUtils
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 插件执行环境逻辑类
 *
 * since: 2019-01-04
 */
@Suppress("ALL")
@Service
class MarketAtomEnvServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val marketAtomEnvInfoDao: MarketAtomEnvInfoDao,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val atomDao: AtomDao,
    private val marketAtomDao: MarketAtomDao,
    private val atomService: AtomService,
    private val marketAtomCommonService: MarketAtomCommonService,
    private val storeI18nMessageService: StoreI18nMessageService,
    private val redisOperation: RedisOperation
) : MarketAtomEnvService {

    private val logger = LoggerFactory.getLogger(MarketAtomEnvServiceImpl::class.java)

    override fun batchGetAtomRunInfos(
        projectCode: String,
        atomVersions: Set<StoreVersion>
    ): Result<Map<String, AtomRunInfo>?> {
        logger.info("batchGetAtomRunInfos projectCode:$projectCode,atomVersions:$atomVersions")
        // 1、校验插件在项目下是否可用
        val filterAtomVersions = mutableSetOf<StoreVersion>()
        val validateAtomCodeList = mutableListOf<String>()
        handleAtomVersions(
            atomVersions = atomVersions,
            validateAtomCodeList = validateAtomCodeList,
            filterAtomVersions = filterAtomVersions
        )
        val atomCodeMap = mutableMapOf<String, String>()
        val atomCodeList = mutableListOf<String>()
        filterAtomVersions.forEach { atomVersion ->
            val storeCode = atomVersion.storeCode
            atomCodeMap[storeCode] = atomVersion.storeName
            atomCodeList.add(storeCode)
        }
        if (validateAtomCodeList.isNotEmpty()) {
            val validAtomCodeList = storeProjectRelDao.getValidStoreCodesByProject(
                dslContext = dslContext,
                projectCode = projectCode,
                storeCodes = validateAtomCodeList,
                storeType = StoreTypeEnum.ATOM
            )?.map { it.value1() } ?: emptyList()
            // 判断是否存在不可用插件
            validateAtomCodeList.removeAll(validAtomCodeList)
        }
        if (validateAtomCodeList.isNotEmpty()) {
            // 存在不可用插件，给出错误提示
            val inValidAtomNameList = mutableListOf<String>()
            validateAtomCodeList.forEach { atomCode ->
                inValidAtomNameList.add(atomCodeMap[atomCode] ?: atomCode)
            }
            val params = arrayOf(projectCode, JsonUtil.toJson(inValidAtomNameList))
            throw ErrorCodeException(
                errorCode = StoreMessageCode.USER_ATOM_IS_NOT_ALLOW_USE_IN_PROJECT,
                params = params
            )
        }
        // 2、根据插件代码和版本号查找插件运行时信息
        // 判断当前项目是否是插件的调试项目
        val testAtomCodes = storeProjectRelDao.getTestStoreCodes(
            dslContext = dslContext,
            projectCode = projectCode,
            storeType = StoreTypeEnum.ATOM,
            storeCodeList = atomCodeList
        )?.map { it.value1() }
        val atomRunInfoMap = mutableMapOf<String, AtomRunInfo>()
        filterAtomVersions.forEach { atomVersion ->
            val atomCode = atomVersion.storeCode
            val atomName = atomVersion.storeName
            val version = atomVersion.version
            // 获取当前大版本内是否有测试中的版本
            val atomVersionTestFlag = redisOperation.hget(
                key = "$ATOM_POST_VERSION_TEST_FLAG_KEY_PREFIX:$atomCode",
                hashKey = VersionUtils.convertLatestVersion(version)
            )
            val testAtomFlag = testAtomCodes?.contains(atomCode) == true
            val testFlag = testAtomFlag && (atomVersionTestFlag == null || atomVersionTestFlag.toBoolean())
            logger.info("batchGetAtomRunInfos atomCode:$atomCode,version:$version,testFlag:$testFlag")
            // 如果当前的项目属于插件的调试项目且插件当前大版本有测试中的版本则实时去db查
            val atomRunInfoName = "$atomCode:$version"
            if (testFlag) {
                atomRunInfoMap[atomRunInfoName] = queryAtomRunInfoFromDb(
                    projectCode = projectCode,
                    atomCode = atomCode,
                    atomName = atomName,
                    version = version,
                    testFlag = testFlag
                )
            } else {
                // 去缓存中获取插件运行时信息
                val atomRunInfoKey = StoreUtils.getStoreRunInfoKey(StoreTypeEnum.ATOM.name, atomCode)
                val atomRunInfoJson = redisOperation.hget(atomRunInfoKey, version)
                if (!atomRunInfoJson.isNullOrEmpty()) {
                    val atomRunInfo = JsonUtil.to(atomRunInfoJson, AtomRunInfo::class.java)
                    atomRunInfoMap[atomRunInfoName] = atomRunInfo
                } else {
                    atomRunInfoMap[atomRunInfoName] = queryAtomRunInfoFromDb(
                        projectCode = projectCode,
                        atomCode = atomCode,
                        atomName = atomName,
                        version = version,
                        testFlag = testFlag
                    )
                }
            }
        }
        return Result(atomRunInfoMap)
    }

    private fun handleAtomVersions(
        atomVersions: Set<StoreVersion>,
        validateAtomCodeList: MutableList<String>,
        filterAtomVersions: MutableSet<StoreVersion>
    ) {
        atomVersions.forEach { atomVersion ->
            val atomCode = atomVersion.storeCode
            val historyFlag = atomVersion.historyFlag
            val defaultAtomFlag = marketAtomCommonService.isPublicAtom(atomCode)
            if (!(defaultAtomFlag || historyFlag)) {
                // 默认插件和内置插件无需校验可见范围
                validateAtomCodeList.add(atomCode)
            }
            if (!(!defaultAtomFlag && historyFlag)) {
                // 过滤调那些没有存在db中的内置插件
                filterAtomVersions.add(atomVersion)
            }
        }
    }

    private fun queryAtomRunInfoFromDb(
        projectCode: String,
        atomCode: String,
        atomName: String,
        version: String,
        testFlag: Boolean
    ): AtomRunInfo {
        val atomEnvResult = getMarketAtomEnvInfo(projectCode, atomCode, version)
        if (atomEnvResult.isNotOk()) {
            val params = arrayOf(projectCode, atomName)
            throw ErrorCodeException(
                errorCode = StoreMessageCode.USER_ATOM_IS_NOT_ALLOW_USE_IN_PROJECT,
                params = params
            )
        }
        // 查不到当前插件信息则中断流程
        val atomEnv = atomEnvResult.data ?: throw ErrorCodeException(
            errorCode = StoreMessageCode.USER_ATOM_IS_NOT_ALLOW_USE_IN_PROJECT,
            params = arrayOf(projectCode, atomName)
        )
        val atomRunInfo = AtomRunInfo(
            atomCode = atomCode,
            atomName = atomEnv.atomName,
            version = atomEnv.version,
            initProjectCode = atomEnv.projectCode ?: "",
            jobType = atomEnv.jobType,
            buildLessRunFlag = atomEnv.buildLessRunFlag,
            inputTypeInfos = marketAtomCommonService.generateInputTypeInfos(atomEnv.props)
        )
        if (!testFlag) {
            // 将db中的环境信息写入缓存
            val atomRunInfoKey = StoreUtils.getStoreRunInfoKey(StoreTypeEnum.ATOM.name, atomCode)
            redisOperation.hset(atomRunInfoKey, version, JsonUtil.toJson(atomRunInfo))
        }
        return atomRunInfo
    }

    /**
     * 根据插件代码和版本号查看插件执行环境信息
     */
    override fun getMarketAtomEnvInfo(
        projectCode: String,
        atomCode: String,
        version: String,
        atomStatus: Byte?,
        osName: String?,
        osArch: String?,
        convertOsFlag: Boolean?
    ): Result<AtomEnv?> {
        logger.info("getMarketAtomEnvInfo $projectCode,$atomCode,$version,$atomStatus,$osName,$osArch,$convertOsFlag")
        // 判断插件查看的权限
        val atomResult = atomService.getPipelineAtom(
            projectCode = projectCode,
            atomCode = atomCode,
            version = version,
            atomStatus = atomStatus,
            queryOfflineFlag = atomStatus == null
        )
        if (atomResult.isNotOk()) {
            return Result(atomResult.status, atomResult.message ?: "")
        }
        val atom = atomResult.data ?: return Result(data = null)
        val atomId = atom.id
        val props = if (atom.props != null) JsonUtil.toJson(atom.props!!, formatted = false) else null
        val classType = atom.classType
        if (atom.htmlTemplateVersion == FrontendTypeEnum.HISTORY.typeVersion ||
            (classType != MarketBuildAtomElement.classType && classType != MarketBuildLessAtomElement.classType)
        ) {
            // 如果是历史老插件则直接返回环境信息
            return Result(
                AtomEnv(
                    atomId = atomId,
                    atomCode = atom.atomCode,
                    atomName = atom.name,
                    atomStatus = atom.atomStatus,
                    creator = atom.creator,
                    version = atom.version,
                    publicFlag = atom.defaultFlag ?: false,
                    summary = atom.summary,
                    docsLink = atom.docsLink,
                    props = props,
                    buildLessRunFlag = atom.buildLessRunFlag,
                    createTime = atom.createTime,
                    updateTime = atom.updateTime,
                    classifyCode = atom.classifyCode,
                    classifyName = atom.classifyName,
                    authFlag = atom.visibilityLevel != VisibilityLevelEnum.LOGIN_PUBLIC.name
                )
            )
        }
        // 普通项目的查已发布、下架中和已下架（需要兼容那些还在使用已下架插件插件的项目）的插件
        val normalStatusList = listOf(
            AtomStatusEnum.RELEASED.status.toByte(),
            AtomStatusEnum.UNDERCARRIAGING.status.toByte(),
            AtomStatusEnum.UNDERCARRIAGED.status.toByte()
        )
        val atomStatusList = getAtomStatusList(
            atomStatus = atomStatus,
            version = version,
            normalStatusList = normalStatusList,
            atomCode = atomCode,
            projectCode = projectCode
        )
        val atomDefaultFlag = atom.defaultFlag == true
        val atomBaseInfoRecord = marketAtomEnvInfoDao.getProjectAtomBaseInfo(
            dslContext = dslContext,
            projectCode = projectCode,
            atomCode = atomCode,
            version = version,
            atomDefaultFlag = atomDefaultFlag,
            atomStatusList = atomStatusList
        )
        return Result(
            if (atomBaseInfoRecord == null) {
                null
            } else {
                val tAtom = TAtom.T_ATOM
                val atomEnvInfoRecord = getAtomEnvInfoRecord(
                    atomId = atomBaseInfoRecord[tAtom.ID],
                    osName = osName,
                    osArch = osArch,
                    convertOsFlag = convertOsFlag
                )
                val status = atomBaseInfoRecord[tAtom.ATOM_STATUS] as Byte
                val createTime = atomBaseInfoRecord[tAtom.CREATE_TIME] as LocalDateTime
                val updateTime = atomBaseInfoRecord[tAtom.UPDATE_TIME] as LocalDateTime
                val postEntryParam = atomEnvInfoRecord?.postEntryParam
                val postCondition = atomEnvInfoRecord?.postCondition
                var postFlag = true
                val atomPostInfo = if (!postEntryParam.isNullOrBlank() && !postCondition.isNullOrBlank()) {
                    AtomPostInfo(
                        atomCode = atomCode,
                        version = version,
                        postEntryParam = postEntryParam,
                        postCondition = postCondition
                    )
                } else {
                    postFlag = false
                    null
                }
                val atomPostMap = mapOf(
                    ATOM_POST_FLAG to postFlag,
                    ATOM_POST_ENTRY_PARAM to postEntryParam,
                    ATOM_POST_CONDITION to postCondition
                )
                if (status in normalStatusList) {
                    val normalProjectPostKey = "$ATOM_POST_NORMAL_PROJECT_FLAG_KEY_PREFIX:$atomCode"
                    if (redisOperation.hget(normalProjectPostKey, version) == null) {
                        redisOperation.hset(
                            key = normalProjectPostKey,
                            hashKey = version,
                            values = JsonUtil.toJson(atomPostMap)
                        )
                    }
                }
                val initProjectCode = storeProjectRelDao.getInitProjectCodeByStoreCode(
                    dslContext = dslContext,
                    storeCode = atomCode,
                    storeType = StoreTypeEnum.ATOM.type.toByte()
                )
                logger.info("$atomCode initProjectCode is :$initProjectCode")
                val jobType = atomBaseInfoRecord[tAtom.JOB_TYPE]
                AtomEnv(
                    atomId = atomBaseInfoRecord[tAtom.ID],
                    atomCode = atomBaseInfoRecord[tAtom.ATOM_CODE],
                    atomName = atomBaseInfoRecord[tAtom.NAME],
                    atomStatus = AtomStatusEnum.getAtomStatus(status.toInt()),
                    creator = atomBaseInfoRecord[tAtom.CREATOR],
                    version = atomBaseInfoRecord[tAtom.VERSION],
                    publicFlag = atomBaseInfoRecord[tAtom.DEFAULT_FLAG] as Boolean,
                    summary = atomBaseInfoRecord[tAtom.SUMMARY],
                    docsLink = atomBaseInfoRecord[tAtom.DOCS_LINK],
                    props = props?.let {
                        storeI18nMessageService.parseJsonStrI18nInfo(
                            jsonStr = it,
                            keyPrefix = StoreUtils.getStoreFieldKeyPrefix(
                                storeType = StoreTypeEnum.ATOM,
                                storeCode = atomCode,
                                version = atomBaseInfoRecord[tAtom.VERSION]
                            )
                        )
                    },
                    buildLessRunFlag = atomBaseInfoRecord[tAtom.BUILD_LESS_RUN_FLAG],
                    createTime = createTime.timestampmilli(),
                    updateTime = updateTime.timestampmilli(),
                    projectCode = initProjectCode,
                    pkgPath = atomEnvInfoRecord?.pkgPath,
                    language = atomEnvInfoRecord?.language,
                    minVersion = atomEnvInfoRecord?.minVersion,
                    target = atomEnvInfoRecord?.target,
                    shaContent = atomEnvInfoRecord?.shaContent,
                    preCmd = atomEnvInfoRecord?.preCmd,
                    jobType = if (jobType == null) null else JobTypeEnum.valueOf(jobType),
                    atomPostInfo = atomPostInfo,
                    classifyCode = atom.classifyCode,
                    classifyName = atom.classifyName,
                    runtimeVersion = atomEnvInfoRecord?.runtimeVersion,
                    finishKillFlag = atomEnvInfoRecord?.finishKillFlag,
                    authFlag = atom.visibilityLevel != VisibilityLevelEnum.LOGIN_PUBLIC.name
                )
            }
        )
    }

    private fun getAtomEnvInfoRecord(
        atomId: String,
        osName: String?,
        osArch: String?,
        convertOsFlag: Boolean?
    ): TAtomEnvInfoRecord? {
        var finalOsName = osName
        var finalOsArch = osArch
        var defaultAtomEnvInfoRecord: TAtomEnvInfoRecord? = null
        if (convertOsFlag == true) {
            defaultAtomEnvInfoRecord = marketAtomEnvInfoDao.getDefaultAtomEnvInfo(dslContext, atomId)
            // 把操作系统名称和cpu架构转换成开发语言对应的格式
            defaultAtomEnvInfoRecord?.language?.let {
                val atomBusHandleService = AtomBusHandleFactory.createAtomBusHandleService(it)
                osName?.let {
                    finalOsName = atomBusHandleService.handleOsName(osName)
                    finalOsArch = osArch?.let { atomBusHandleService.handleOsArch(osName, osArch) }
                }
            }
        }
        return marketAtomEnvInfoDao.getAtomEnvInfo(
            dslContext = dslContext,
            atomId = atomId,
            osName = finalOsName,
            osArch = finalOsArch
        ) ?: marketAtomEnvInfoDao.getDefaultAtomEnvInfo(dslContext, atomId, osName)
        ?: if (convertOsFlag == true) {
            defaultAtomEnvInfoRecord
        } else {
            marketAtomEnvInfoDao.getDefaultAtomEnvInfo(dslContext, atomId)
        }
    }

    private fun getAtomStatusList(
        atomStatus: Byte?,
        version: String,
        normalStatusList: List<Byte>,
        atomCode: String,
        projectCode: String
    ): List<Byte>? {
        var atomStatusList: List<Byte>? = null
        if (atomStatus != null) {
            mutableListOf(atomStatus)
        } else {
            if (version.contains("*")) {
                atomStatusList = normalStatusList.toMutableList()
                val releaseCount = marketAtomDao.countReleaseAtomByCode(dslContext, atomCode, version)
                if (releaseCount > 0) {
                    // 如果当前大版本内还有已发布的版本，则xx.latest只对应最新已发布的版本
                    atomStatusList = mutableListOf(AtomStatusEnum.RELEASED.status.toByte())
                }
                val flag =
                    storeProjectRelDao.isTestProjectCode(dslContext, atomCode, StoreTypeEnum.ATOM, projectCode)
                logger.info("isInitTestProjectCode flag is :$flag")
                if (flag) {
                    // 原生项目或者调试项目有权查处于测试中、审核中的插件
                    atomStatusList.addAll(
                        listOf(
                            AtomStatusEnum.TESTING.status.toByte(),
                            AtomStatusEnum.AUDITING.status.toByte()
                        )
                    )
                }
            }
        }
        return atomStatusList
    }

    /**
     * 更新插件执行环境信息
     */
    override fun updateMarketAtomEnvInfo(
        projectCode: String,
        atomCode: String,
        version: String,
        atomEnvRequest: AtomEnvRequest
    ): Result<Boolean> {
        logger.info("updateMarketAtomEnvInfo params:[$projectCode,$atomCode,$version,$atomEnvRequest]")
        val atomResult = atomService.getPipelineAtom(projectCode, atomCode, version) // 判断插件查看的权限
        val status = atomResult.status
        if (0 != status) {
            return Result(atomResult.status, atomResult.message ?: "", false)
        }
        val osName = atomEnvRequest.osName
        val osArch = atomEnvRequest.osArch
        atomEnvRequest.language?.let {
            val atomBusHandleService = AtomBusHandleFactory.createAtomBusHandleService(it)
            if (!osName.isNullOrBlank()) {
                atomEnvRequest.osName = atomBusHandleService.handleOsName(osName)
            }
            if (!osName.isNullOrBlank() && !osArch.isNullOrBlank()) {
                atomEnvRequest.osArch = atomBusHandleService.handleOsArch(osName, osArch)
            }
        }
        val atomRecord = atomDao.getPipelineAtom(dslContext, atomCode, version)
        return if (null != atomRecord) {
            val atomId = atomRecord.id
            val atomEnvRecord = marketAtomEnvInfoDao.getAtomEnvInfo(
                dslContext = dslContext,
                atomId = atomId,
                osName = osName,
                osArch = osArch
            )
            atomEnvRecord?.let {
                // 合并用户配置的前置命令和系统预置的前置命令
                val dbPreCmds = CommonUtils.strToList(atomEnvRecord.preCmd ?: "")
                val requestPreCmds = CommonUtils.strToList(atomEnvRequest.preCmd ?: "")
                val finalPreCmds = requestPreCmds.plus(dbPreCmds)
                atomEnvRequest.preCmd = JsonUtil.toJson(finalPreCmds, false)
                marketAtomEnvInfoDao.updateMarketAtomEnvInfo(dslContext, atomId, atomEnvRequest)
            }
            Result(true)
        } else {
            I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf("$atomCode+$version"),
                data = false,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
        }
    }
}

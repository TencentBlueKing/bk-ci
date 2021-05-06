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

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.atom.AtomDao
import com.tencent.devops.store.dao.atom.MarketAtomDao
import com.tencent.devops.store.dao.atom.MarketAtomEnvInfoDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
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
import com.tencent.devops.store.pojo.common.KEY_CREATE_TIME
import com.tencent.devops.store.pojo.common.KEY_UPDATE_TIME
import com.tencent.devops.store.pojo.common.StoreVersion
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.AtomService
import com.tencent.devops.store.service.atom.MarketAtomCommonService
import com.tencent.devops.store.service.atom.MarketAtomEnvService
import com.tencent.devops.store.utils.StoreUtils
import com.tencent.devops.store.utils.VersionUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import java.time.LocalDateTime
import java.util.Optional
import java.util.concurrent.TimeUnit

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
    private val redisOperation: RedisOperation
) : MarketAtomEnvService {

    private val logger = LoggerFactory.getLogger(MarketAtomEnvServiceImpl::class.java)

    private val cache = CacheBuilder.newBuilder().maximumSize(10)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build(object : CacheLoader<String, Optional<Boolean>>() {
            override fun load(key: String): Optional<Boolean> {
                val value = redisOperation.get(key)
                return if (value != null) {
                    Optional.of(value.toBoolean())
                } else {
                    Optional.empty()
                }
            }
        })

    override fun batchGetAtomRunInfos(
        projectCode: String,
        atomVersions: Set<StoreVersion>
    ): Result<Map<String, AtomRunInfo>?> {
        logger.info("batchGetAtomRunInfos projectCode:$projectCode,atomVersions:$atomVersions")
        // 1、校验插件在项目下是否可用
        val atomCodeMap = mutableMapOf<String, String>()
        val atomCodeList = mutableListOf<String>()
        atomVersions.forEach { atomVersion ->
            atomCodeMap[atomVersion.storeCode] = atomVersion.storeName
            atomCodeList.add(atomVersion.storeCode)
        }
        val storePublicFlagKey = StoreUtils.getStorePublicFlagKey(StoreTypeEnum.ATOM.name)
        // 获取从db查询默认插件开关，开关打开则实时从db查
        val queryDefaultAtomSwitchOptional = cache.get("queryDefaultAtomSwitch")
        val queryDefaultAtomSwitch = if (queryDefaultAtomSwitchOptional.isPresent) {
            queryDefaultAtomSwitchOptional.get()
        } else false
        // 获取需要校验的插件（默认公共插件无需校验）
        val validateAtomCodeList = if (queryDefaultAtomSwitch || !redisOperation.hasKey(storePublicFlagKey)) {
            logger.info("$storePublicFlagKey is not exist!")
            if (queryDefaultAtomSwitch && redisOperation.hasKey(storePublicFlagKey)) {
                redisOperation.delete(storePublicFlagKey)
            }
            // 从db去查查默认插件
            val defaultAtomCodeRecords = atomDao.batchGetDefaultAtomCode(dslContext)
            val defaultAtomCodeList = defaultAtomCodeRecords.map { it.value1() }
            if (!queryDefaultAtomSwitch) {
                redisOperation.sadd(storePublicFlagKey, *defaultAtomCodeList.toTypedArray())
            }
            atomCodeList.filter { !defaultAtomCodeList.contains(it) }.toMutableList()
        } else {
            atomCodeList.filter { !redisOperation.isMember(storePublicFlagKey, it) }.toMutableList()
        }
        val validAtomCodeList = storeProjectRelDao.getValidStoreCodesByProject(
            dslContext = dslContext,
            projectCode = projectCode,
            storeCodes = validateAtomCodeList,
            storeType = StoreTypeEnum.ATOM
        )?.map { it.value1() } ?: emptyList()
        // 判断是否存在不可用插件
        validateAtomCodeList.removeAll(validAtomCodeList)
        if (validateAtomCodeList.isNotEmpty()) {
            // 存在不可用插件，给出错误提示
            val inValidAtomNameList = mutableListOf<String>()
            validateAtomCodeList.forEach { atomCode ->
                inValidAtomNameList.add(atomCodeMap[atomCode] ?: atomCode)
            }
            val params = arrayOf(projectCode, JsonUtil.toJson(inValidAtomNameList))
            throw ErrorCodeException(
                errorCode = StoreMessageCode.USER_ATOM_IS_NOT_ALLOW_USE_IN_PROJECT,
                params = params,
                defaultMessage = MessageCodeUtil.getCodeMessage(
                    messageCode = StoreMessageCode.USER_ATOM_IS_NOT_ALLOW_USE_IN_PROJECT,
                    params = params
                )
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
        atomVersions.forEach { atomVersion ->
            val atomCode = atomVersion.storeCode
            val atomName = atomVersion.storeName
            val version = atomVersion.version
            // 获取当前大版本内是否有测试中的版本
            val atomVersionTestFlag = redisOperation.hget(
                key = "$ATOM_POST_VERSION_TEST_FLAG_KEY_PREFIX:$atomCode",
                hashKey = VersionUtils.convertLatestVersion(version)
            )
            val testFlag =
                testAtomCodes?.contains(atomCode) == true && (atomVersionTestFlag == null || atomVersionTestFlag.toBoolean())
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
                params = params,
                defaultMessage = MessageCodeUtil.getCodeMessage(
                    messageCode = StoreMessageCode.USER_ATOM_IS_NOT_ALLOW_USE_IN_PROJECT,
                    params = params
                )
            )
        }
        // 查不到当前插件信息则中断流程
        val atomEnv = atomEnvResult.data ?: throw ErrorCodeException(
            errorCode = StoreMessageCode.USER_ATOM_IS_NOT_ALLOW_USE_IN_PROJECT,
            params = arrayOf(projectCode, atomName),
            defaultMessage = MessageCodeUtil.getCodeMessage(
                messageCode = StoreMessageCode.USER_ATOM_IS_NOT_ALLOW_USE_IN_PROJECT,
                params = arrayOf(projectCode, atomName)
            )
        )
        val atomRunInfo = AtomRunInfo(
            atomCode = atomCode,
            atomName = atomEnv.atomName,
            version = atomEnv.version,
            initProjectCode = atomEnv.projectCode!!,
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
    override fun getMarketAtomEnvInfo(projectCode: String, atomCode: String, version: String): Result<AtomEnv?> {
        logger.info("getMarketAtomEnvInfo projectCode is :$projectCode,atomCode is :$atomCode,version is :$version")
        val atomResult = atomService.getPipelineAtom(projectCode, atomCode, version) // 判断插件查看的权限
        if (atomResult.isNotOk()) {
            return Result(atomResult.status, atomResult.message ?: "")
        }
        val atom = atomResult.data ?: return Result(data = null)
        if (atom.htmlTemplateVersion == FrontendTypeEnum.HISTORY.typeVersion) {
            // 如果是历史老插件则直接返回环境信息
            return Result(
                AtomEnv(
                    atomId = atom.id,
                    atomCode = atom.atomCode,
                    atomName = atom.name,
                    atomStatus = AtomStatusEnum.getAtomStatus(atom.atomStatus.toInt()),
                    creator = atom.creator,
                    version = atom.version,
                    summary = atom.summary,
                    docsLink = atom.docsLink,
                    props = if (atom.props != null) JsonUtil.toJson(atom.props!!) else null,
                    buildLessRunFlag = atom.buildLessRunFlag,
                    createTime = atom.createTime,
                    updateTime = atom.updateTime
                )
            )
        }
        val initProjectCode = storeProjectRelDao.getInitProjectCodeByStoreCode(
            dslContext = dslContext,
            storeCode = atomCode,
            storeType = StoreTypeEnum.ATOM.type.toByte()
        )
        logger.info("the initProjectCode is :$initProjectCode")
        var atomStatusList: List<Byte>? = null
        // 普通项目的查已发布、下架中和已下架（需要兼容那些还在使用已下架插件插件的项目）的插件
        val normalStatusList = listOf(
            AtomStatusEnum.RELEASED.status.toByte(),
            AtomStatusEnum.UNDERCARRIAGING.status.toByte(),
            AtomStatusEnum.UNDERCARRIAGED.status.toByte()
        )
        if (version.contains("*")) {
            atomStatusList = normalStatusList.toMutableList()
            val releaseCount = marketAtomDao.countReleaseAtomByCode(dslContext, atomCode, version)
            if (releaseCount > 0) {
                // 如果当前大版本内还有已发布的版本，则xx.latest只对应最新已发布的版本
                atomStatusList = mutableListOf(AtomStatusEnum.RELEASED.status.toByte())
            }
            val flag = storeProjectRelDao.isInitTestProjectCode(dslContext, atomCode, StoreTypeEnum.ATOM, projectCode)
            logger.info("the isInitTestProjectCode flag is :$flag")
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
        val atomDefaultFlag = atom.defaultFlag == true
        val atomEnvInfoRecord = marketAtomEnvInfoDao.getProjectMarketAtomEnvInfo(
            dslContext = dslContext,
            projectCode = projectCode,
            atomCode = atomCode,
            version = version,
            atomDefaultFlag = atomDefaultFlag,
            atomStatusList = atomStatusList
        )
        return Result(
            if (atomEnvInfoRecord == null) {
                null
            } else {
                val atomStatus = atomEnvInfoRecord["atomStatus"] as Byte
                val createTime = atomEnvInfoRecord[KEY_CREATE_TIME] as LocalDateTime
                val updateTime = atomEnvInfoRecord[KEY_UPDATE_TIME] as LocalDateTime
                val postEntryParam = atomEnvInfoRecord[ATOM_POST_ENTRY_PARAM] as? String
                val postCondition = atomEnvInfoRecord[ATOM_POST_CONDITION] as? String
                var postFlag = true
                val atomPostInfo = if (!StringUtils.isEmpty(postEntryParam) && !StringUtils.isEmpty(postEntryParam)) {
                    AtomPostInfo(
                        atomCode = atomCode,
                        version = version,
                        postEntryParam = postEntryParam!!,
                        postCondition = postCondition!!
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
                if (atomStatus in normalStatusList) {
                    val normalProjectPostKey = "$ATOM_POST_NORMAL_PROJECT_FLAG_KEY_PREFIX:$atomCode"
                    if (redisOperation.hget(normalProjectPostKey, version) == null) {
                        redisOperation.hset(
                            key = normalProjectPostKey,
                            hashKey = version,
                            values = JsonUtil.toJson(atomPostMap)
                        )
                    }
                }
                val jobType = atomEnvInfoRecord["jobType"] as? String
                AtomEnv(
                    atomId = atomEnvInfoRecord["atomId"] as String,
                    atomCode = atomEnvInfoRecord["atomCode"] as String,
                    atomName = atomEnvInfoRecord["atomName"] as String,
                    atomStatus = AtomStatusEnum.getAtomStatus(atomStatus.toInt()),
                    creator = atomEnvInfoRecord["creator"] as String,
                    version = atomEnvInfoRecord["version"] as String,
                    summary = atomEnvInfoRecord["summary"] as? String,
                    docsLink = atomEnvInfoRecord["docsLink"] as? String,
                    props = atomEnvInfoRecord["props"] as? String,
                    buildLessRunFlag = atomEnvInfoRecord["buildLessRunFlag"] as? Boolean,
                    createTime = createTime.timestampmilli(),
                    updateTime = updateTime.timestampmilli(),
                    projectCode = initProjectCode,
                    pkgPath = atomEnvInfoRecord["pkgPath"] as String,
                    language = atomEnvInfoRecord["language"] as? String,
                    minVersion = atomEnvInfoRecord["minVersion"] as? String,
                    target = atomEnvInfoRecord["target"] as String,
                    shaContent = atomEnvInfoRecord["shaContent"] as? String,
                    preCmd = atomEnvInfoRecord["preCmd"] as? String,
                    jobType = if (jobType == null) null else JobTypeEnum.valueOf(jobType),
                    atomPostInfo = atomPostInfo
                )
            }
        )
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
        val atomResult = atomService.getPipelineAtom(projectCode, atomCode, version) // 判断插件查看的权限
        val status = atomResult.status
        if (0 != status) {
            return Result(atomResult.status, atomResult.message ?: "", false)
        }
        val atomRecord = atomDao.getPipelineAtom(dslContext, atomCode, version)
        return if (null != atomRecord) {
            marketAtomEnvInfoDao.updateMarketAtomEnvInfo(dslContext, atomRecord.id, atomEnvRequest)
            Result(true)
        } else {
            MessageCodeUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf("$atomCode+$version"),
                data = false
            )
        }
    }
}

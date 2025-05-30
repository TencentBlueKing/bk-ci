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

package com.tencent.devops.store.common.service.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.INIT_VERSION
import com.tencent.devops.common.api.constant.SUCCESS
import com.tencent.devops.common.api.constant.appendIfNotEmpty
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.store.common.configuration.StoreDetailUrlConfig
import com.tencent.devops.store.common.dao.AbstractStoreCommonDao
import com.tencent.devops.store.common.dao.OperationLogDao
import com.tencent.devops.store.common.dao.ReasonRelDao
import com.tencent.devops.store.common.dao.SensitiveConfDao
import com.tencent.devops.store.common.dao.StoreApproveDao
import com.tencent.devops.store.common.dao.StoreBaseEnvExtManageDao
import com.tencent.devops.store.common.dao.StoreBaseEnvManageDao
import com.tencent.devops.store.common.dao.StoreBaseExtManageDao
import com.tencent.devops.store.common.dao.StoreBaseFeatureExtManageDao
import com.tencent.devops.store.common.dao.StoreBaseFeatureManageDao
import com.tencent.devops.store.common.dao.StoreBaseFeatureQueryDao
import com.tencent.devops.store.common.dao.StoreBaseManageDao
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.dao.StoreCommentDao
import com.tencent.devops.store.common.dao.StoreCommentPraiseDao
import com.tencent.devops.store.common.dao.StoreCommentReplyDao
import com.tencent.devops.store.common.dao.StoreDeptRelDao
import com.tencent.devops.store.common.dao.StoreEnvVarDao
import com.tencent.devops.store.common.dao.StoreLabelRelDao
import com.tencent.devops.store.common.dao.StoreMediaInfoDao
import com.tencent.devops.store.common.dao.StoreMemberDao
import com.tencent.devops.store.common.dao.StorePipelineBuildRelDao
import com.tencent.devops.store.common.dao.StorePipelineRelDao
import com.tencent.devops.store.common.dao.StoreProjectRelDao
import com.tencent.devops.store.common.dao.StoreReleaseDao
import com.tencent.devops.store.common.dao.StoreStatisticDailyDao
import com.tencent.devops.store.common.dao.StoreStatisticDao
import com.tencent.devops.store.common.dao.StoreStatisticTotalDao
import com.tencent.devops.store.common.dao.StoreVersionLogDao
import com.tencent.devops.store.common.service.StoreCommonService
import com.tencent.devops.store.common.utils.VersionUtils
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.ReleaseProcessItem
import com.tencent.devops.store.pojo.common.publication.StoreBuildInfo
import com.tencent.devops.store.pojo.common.publication.StoreProcessInfo
import com.tencent.devops.store.pojo.common.version.StoreShowVersionInfo
import com.tencent.devops.store.pojo.common.version.StoreShowVersionItem
import com.tencent.devops.store.pojo.common.version.VersionModel
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

/**
 * store公共
 * since: 2019-07-23
 */
@Suppress("ALL")
abstract class StoreCommonServiceImpl : StoreCommonService {

    @Autowired
    lateinit var dslContext: DSLContext

    @Autowired
    lateinit var storeMemberDao: StoreMemberDao

    @Autowired
    lateinit var storePipelineBuildRelDao: StorePipelineBuildRelDao

    @Autowired
    lateinit var storeProjectRelDao: StoreProjectRelDao

    @Autowired
    lateinit var operationLogDao: OperationLogDao

    @Autowired
    lateinit var sensitiveConfDao: SensitiveConfDao

    @Autowired
    lateinit var reasonRelDao: ReasonRelDao

    @Autowired
    lateinit var storeApproveDao: StoreApproveDao

    @Autowired
    lateinit var storeCommentDao: StoreCommentDao

    @Autowired
    lateinit var storeCommentPraiseDao: StoreCommentPraiseDao

    @Autowired
    lateinit var storeCommentReplyDao: StoreCommentReplyDao

    @Autowired
    lateinit var storeDeptRelDao: StoreDeptRelDao

    @Autowired
    lateinit var storeEnvVarDao: StoreEnvVarDao

    @Autowired
    lateinit var storeMediaInfoDao: StoreMediaInfoDao

    @Autowired
    lateinit var storePipelineRelDao: StorePipelineRelDao

    @Autowired
    lateinit var storeReleaseDao: StoreReleaseDao

    @Autowired
    lateinit var storeStatisticDao: StoreStatisticDao

    @Autowired
    lateinit var storeStatisticTotalDao: StoreStatisticTotalDao

    @Autowired
    lateinit var storeStatisticDailyDao: StoreStatisticDailyDao

    @Autowired
    lateinit var storeBaseQueryDao: StoreBaseQueryDao

    @Autowired
    lateinit var storeBaseFeatureQueryDao: StoreBaseFeatureQueryDao

    @Autowired
    lateinit var storeDetailUrlConfig: StoreDetailUrlConfig

    @Autowired
    lateinit var storeBaseEnvExtManageDao: StoreBaseEnvExtManageDao

    @Autowired
    lateinit var storeLabelRelDao: StoreLabelRelDao

    @Autowired
    lateinit var storeBaseExtManageDao: StoreBaseExtManageDao

    @Autowired
    lateinit var storeBaseEnvManageDao: StoreBaseEnvManageDao

    @Autowired
    lateinit var storeBaseManageDao: StoreBaseManageDao

    @Autowired
    lateinit var storeBaseFeatureManageDao: StoreBaseFeatureManageDao

    @Autowired
    lateinit var storeBaseFeatureExtManageDao: StoreBaseFeatureExtManageDao

    @Autowired
    lateinit var storeVersionLogDao: StoreVersionLogDao

    @Autowired
    lateinit var client: Client

    private val logger = LoggerFactory.getLogger(StoreCommonServiceImpl::class.java)

    override fun getStoreNameById(
        storeId: String,
        storeType: StoreTypeEnum
    ): String {
        var name = storeBaseQueryDao.getComponentById(dslContext, storeId)?.name
        if (name == null) {
            val storeCommonDao = getStoreCommonDao(storeType.name)
            name = storeCommonDao.getStoreNameById(dslContext, storeId)
        }
        return name ?: ""
    }

    override fun getStorePublicFlagByCode(storeCode: String, storeType: StoreTypeEnum): Boolean {
        var publicFlag = storeBaseFeatureQueryDao.getBaseFeatureByCode(dslContext, storeCode, storeType)?.publicFlag
        if (publicFlag == null) {
            publicFlag = getStoreCommonDao(storeType.name).getStorePublicFlagByCode(dslContext, storeCode)
        }
        return publicFlag
    }

    override fun getStoreRepoHashIdByCode(storeCode: String, storeType: StoreTypeEnum): String? {
        return getStoreCommonDao(storeType.name).getStoreRepoHashIdByCode(dslContext, storeCode)
    }

    private fun getStoreCommonDao(storeType: String): AbstractStoreCommonDao {
        return SpringContextUtil.getBean(AbstractStoreCommonDao::class.java, "${storeType}_COMMON_DAO")
    }

    data class VersionParts(
        val major: Int,
        val minor: Int,
        val patch: Int,
        val suffix: String? = null
    )

    private fun parseVersion(version: String): VersionParts {
        val parts = version.split(".")
        val (major, minor, patch) = parts.take(3).map { it.toInt() }
        return VersionParts(
            major = major,
            minor = minor,
            patch = patch,
            suffix = parts.drop(3).takeIf { it.isNotEmpty() }?.joinToString(".")
        )
    }

    /**
     * 获取正确的升级版本号
     */
    override fun getRequireVersion(
        dbVersion: String,
        releaseType: ReleaseTypeEnum,
        reqVersion: String?
    ): List<String> {
        if (dbVersion.isBlank()) {
            return listOf(INIT_VERSION)
        }
        val dbVersionParts = parseVersion(dbVersion)
        val major = dbVersionParts.major
        val minor = dbVersionParts.minor
        val patch = dbVersionParts.patch
        val suffix = dbVersionParts.suffix
        return when (releaseType) {
            ReleaseTypeEnum.INCOMPATIBILITY_UPGRADE -> listOf("${major + 1}.0.0".appendIfNotEmpty(suffix))
            ReleaseTypeEnum.COMPATIBILITY_UPGRADE -> listOf("$major.${minor + 1}.0".appendIfNotEmpty(suffix))
            ReleaseTypeEnum.COMPATIBILITY_FIX -> listOf("$major.$minor.${patch + 1}".appendIfNotEmpty(suffix))
            ReleaseTypeEnum.CANCEL_RE_RELEASE -> listOf(dbVersion)
            ReleaseTypeEnum.HIS_VERSION_UPGRADE -> {
                if (reqVersion.isNullOrBlank()) {
                    throw ErrorCodeException(errorCode = StoreMessageCode.USER_HIS_VERSION_UPGRADE_INVALID)
                }
                val reqVersionParts = parseVersion(reqVersion)
                val reqMajor = reqVersionParts.major
                val reqMinor = reqVersionParts.minor
                val reqPatch = reqVersionParts.patch
                val reqSuffix = reqVersionParts.suffix
                listOf(
                    "$reqMajor.$reqMinor.${reqPatch + 1}".appendIfNotEmpty(reqSuffix),
                    "$reqMajor.${reqMinor + 1}.0".appendIfNotEmpty(reqSuffix)
                )
            }

            else -> listOf(INIT_VERSION) // 默认返回初始版本号
        }
    }

    /**
     * 设置进度
     */
    override fun setProcessInfo(
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
                }
            }
        }
        return true
    }

    /**
     * 生成发布流程进度信息
     */
    override fun generateStoreProcessInfo(
        userId: String,
        storeId: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        creator: String,
        processInfo: List<ReleaseProcessItem>
    ): StoreProcessInfo {
        val opPermission = storeMemberDao.isStoreAdmin(
            dslContext = dslContext,
            userId = userId,
            storeCode = storeCode,
            storeType = storeType.type.toByte()
        ) || creator == userId
        val storeProcessInfo = StoreProcessInfo(opPermission, null, processInfo)
        val storeBuildInfoRecord = storePipelineBuildRelDao.getStorePipelineBuildRel(dslContext, storeId)
        if (null != storeBuildInfoRecord) {
            val pipelineId = storeBuildInfoRecord.pipelineId
            val storePipelineRelRecord = storePipelineRelDao.getStorePipelineRelByStoreCode(
                dslContext = dslContext,
                storeType = storeType,
                storeCode = storeCode
            )
            var projectCode = storePipelineRelRecord?.projectCode
            if (projectCode.isNullOrBlank()) {
                projectCode =
                    client.get(ServicePipelineResource::class).getPipelineInfoByPipelineId(pipelineId)?.data?.projectId
                        ?: ""
                storePipelineRelDao.updateStorePipelineProject(dslContext, pipelineId, projectCode)
            }
            storeProcessInfo.storeBuildInfo = StoreBuildInfo(
                storeId = storeBuildInfoRecord.storeId,
                pipelineId = pipelineId,
                buildId = storeBuildInfoRecord.buildId,
                projectCode = projectCode
            )
        }
        return storeProcessInfo
    }

    /**
     * 获取store组件详情页地址
     */
    override fun getStoreDetailUrl(storeType: StoreTypeEnum, storeCode: String): String {
        return when (storeType) {
            StoreTypeEnum.ATOM -> getStoreDetailUrl(storeDetailUrlConfig.atomDetailBaseUrl, storeCode)
            StoreTypeEnum.TEMPLATE -> getStoreDetailUrl(storeDetailUrlConfig.templateDetailBaseUrl, storeCode)
            StoreTypeEnum.IMAGE -> getStoreDetailUrl(storeDetailUrlConfig.imageDetailBaseUrl, storeCode)
            StoreTypeEnum.IDE_ATOM -> getStoreDetailUrl(storeDetailUrlConfig.ideAtomDetailBaseUrl, storeCode)
            StoreTypeEnum.SERVICE -> getStoreDetailUrl(storeDetailUrlConfig.serviceDetailBaseUrl, storeCode)
            else -> "${storeDetailUrlConfig.storeDetailBaseUrl}/${storeType.name.lowercase()}/$storeCode"
        }
    }

    private fun getStoreDetailUrl(storeDetailUrlPrefix: String?, storeCode: String): String {
        return if (!storeDetailUrlPrefix.isNullOrBlank()) {
            "$storeDetailUrlPrefix$storeCode"
        } else {
            ""
        }
    }

    override fun deleteStoreInfo(context: DSLContext, storeCode: String, storeType: Byte): Boolean {
        operationLogDao.deleteOperationLog(context, storeCode, storeType)
        sensitiveConfDao.deleteSensitiveConf(context, storeCode, storeType)
        reasonRelDao.deleteReasonRel(context, storeCode, storeType)
        storeApproveDao.deleteApproveInfo(context, storeCode, storeType)
        storeCommentDao.deleteStoreComment(context, storeCode, storeType)
        storeCommentPraiseDao.deleteStoreCommentPraise(context, storeCode, storeType)
        storeCommentReplyDao.deleteStoreCommentReply(context, storeCode, storeType)
        storeDeptRelDao.deleteByStoreCode(context, storeCode, storeType)
        storeEnvVarDao.deleteEnvVar(context, storeCode, storeType)
        storeMediaInfoDao.deleteByStoreCode(context, storeCode, storeType)
        storeMemberDao.deleteAll(context, storeCode, storeType)
        storePipelineBuildRelDao.deleteStorePipelineBuildRel(context, storeCode, storeType)
        storePipelineRelDao.deleteStorePipelineRel(context, storeCode, storeType)
        storeProjectRelDao.deleteAllRel(context, storeCode, storeType)
        storeReleaseDao.deleteStoreReleaseInfo(context, storeCode, storeType)
        storeStatisticDao.deleteStoreStatistic(context, storeCode, storeType)
        storeStatisticTotalDao.deleteStoreStatisticTotal(context, storeCode, storeType)
        storeStatisticDailyDao.deleteDailyStatisticData(context, storeCode, storeType)
        storeBaseFeatureManageDao.deleteStoreBaseFeature(context, storeCode, storeType)
        storeBaseFeatureExtManageDao.deleteStoreBaseFeatureExtInfo(context, storeCode, storeType)
        storeVersionLogDao.deleteByStoreCode(context, storeCode, storeType)
        val storeIds = storeBaseQueryDao.getComponentIds(context, storeCode, storeType)
        if (storeIds.isNotEmpty()) {
            storeBaseEnvManageDao.batchDeleteStoreEnvInfo(context, storeIds)
            storeLabelRelDao.batchDeleteByStoreId(context, storeIds)
            storeBaseExtManageDao.batchDeleteStoreBaseExtInfo(context, storeIds)
            storeBaseEnvExtManageDao.batchDeleteStoreEnvExtInfo(context, storeIds)
        }
        storeBaseManageDao.deleteByComponentCode(context, storeCode, storeType)
        return true
    }

    abstract override fun generateInstallFlag(
        defaultFlag: Boolean,
        members: MutableList<String>?,
        userId: String,
        visibleList: MutableList<Int>?,
        userDeptList: List<Int>
    ): Boolean

    abstract override fun generateStoreVisibleData(
        storeCodeList: List<String>,
        storeType: StoreTypeEnum
    ): HashMap<String, MutableList<Int>>?

    override fun getStoreShowVersionInfo(
        cancelFlag: Boolean,
        releaseType: ReleaseTypeEnum?,
        version: String?
    ): StoreShowVersionInfo {
        val defaultShowReleaseType = when {
            cancelFlag -> {
                ReleaseTypeEnum.CANCEL_RE_RELEASE
            }
            releaseType?.isDefaultShow() == true -> {
                releaseType
            }
            releaseType == null -> {
                ReleaseTypeEnum.NEW
            }
            else -> {
                ReleaseTypeEnum.COMPATIBILITY_FIX
            }
        }
        val dbVersion = version ?: ""
        val defaultShowVersion = getRequireVersion(dbVersion, defaultShowReleaseType)[0]
        val showVersionList = mutableListOf<StoreShowVersionItem>()
        showVersionList.add(StoreShowVersionItem(defaultShowVersion, defaultShowReleaseType.name, true))
        if (dbVersion.isBlank()) {
            return StoreShowVersionInfo(showVersionList)
        }
        val tmpReleaseTypeList = listOf(
            ReleaseTypeEnum.INCOMPATIBILITY_UPGRADE,
            ReleaseTypeEnum.COMPATIBILITY_UPGRADE,
            ReleaseTypeEnum.COMPATIBILITY_FIX
        )
        tmpReleaseTypeList.forEach { tmpReleaseType ->
            if (tmpReleaseType != defaultShowReleaseType) {
                val showVersion = getRequireVersion(dbVersion, tmpReleaseType)[0]
                showVersionList.add(StoreShowVersionItem(showVersion, tmpReleaseType.name))
            }
        }
        return StoreShowVersionInfo(showVersionList)
    }

    override fun validateStoreVersion(
        storeCode: String,
        storeType: StoreTypeEnum,
        versionInfo: VersionModel,
        name: String
    ) {
        val releaseType = versionInfo.releaseType
        val opBaseRecord =
            storeBaseQueryDao.getNewestComponentByCode(dslContext, storeCode, storeType) ?: throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(storeCode)
            )
        val version = versionInfo.version
        val dbVersion = opBaseRecord.version
        val dbStatus = opBaseRecord.status
        // 判断首个版本对应的请求是否合法
        val validStatusList = listOf(
            StoreStatusEnum.INIT.name,
            StoreStatusEnum.COMMITTING.name,
            StoreStatusEnum.GROUNDING_SUSPENSION.name
        )
        if (releaseType == ReleaseTypeEnum.NEW && dbStatus !in validStatusList) {
            throw ErrorCodeException(errorCode = StoreMessageCode.STORE_RELEASE_STEPS_ERROR)
        }

        val dbVersionParts = parseVersion(dbVersion)
        val reqVersionParts = parseVersion(version)

        val checkFlag = when (releaseType) {
            ReleaseTypeEnum.INCOMPATIBILITY_UPGRADE -> {
                reqVersionParts.major > dbVersionParts.major
            }

            ReleaseTypeEnum.COMPATIBILITY_UPGRADE -> {
                reqVersionParts.major == dbVersionParts.major &&
                        reqVersionParts.minor > dbVersionParts.minor
            }

            ReleaseTypeEnum.COMPATIBILITY_FIX -> {
                reqVersionParts.major == dbVersionParts.major &&
                        reqVersionParts.minor == dbVersionParts.minor &&
                        validatePatchVersion(
                            reqVersionParts = reqVersionParts,
                            compareVersionParts = dbVersionParts,
                            storeCode = storeCode,
                            storeType = storeType,
                            version = version
                        )
            }

            ReleaseTypeEnum.CANCEL_RE_RELEASE -> {
                version == dbVersion
            }

            ReleaseTypeEnum.HIS_VERSION_UPGRADE -> {
                val historyVersion = storeBaseQueryDao.getComponent(
                    dslContext = dslContext,
                    storeCode = storeCode,
                    version = VersionUtils.convertLatestVersion(version),
                    storeType = storeType
                )?.version ?: throw ErrorCodeException(errorCode = StoreMessageCode.USER_HIS_VERSION_UPGRADE_INVALID)
                val hisVersionParts = parseVersion(historyVersion)
                reqVersionParts.major == hisVersionParts.major &&
                        reqVersionParts.minor >= hisVersionParts.minor &&
                        validatePatchVersion(
                            reqVersionParts = reqVersionParts,
                            compareVersionParts = hisVersionParts,
                            storeCode = storeCode,
                            storeType = storeType,
                            version = version
                        )
            }

            else -> true
        }
        if (!checkFlag) {
            logger.warn("$storeType[$storeCode]| invalid version: $version")
            throw ErrorCodeException(
                errorCode = StoreMessageCode.STORE_VERSION_IS_INVALID, params = arrayOf(version)
            )
        }
        // 判断最近一个版本的状态，如果不是首次发布，则只有处于终态的组件状态才允许添加新的版本
        checkAddVersionCondition(dbVersion = dbVersion, releaseType = releaseType, dbStatus = dbStatus, name = name)
    }

    private fun validatePatchVersion(
        reqVersionParts: VersionParts,
        compareVersionParts: VersionParts,
        storeCode: String,
        storeType: StoreTypeEnum,
        version: String
    ): Boolean {
        if (reqVersionParts.major != compareVersionParts.major || reqVersionParts.minor != compareVersionParts.minor) {
            return false
        }
        return if (reqVersionParts.suffix == null) {
            // 如果版本号为三部分，则要求修正号要递增
            reqVersionParts.patch > compareVersionParts.patch
        } else {
            // 如果版本号超过三部分且补丁版本号一样，则要求请求版本号在数据库中不存在
            (reqVersionParts.patch > compareVersionParts.patch) || storeBaseQueryDao.countByCondition(
                dslContext = dslContext, storeCode = storeCode, storeType = storeType, version = version
            ) < 1
        }
    }

    private fun checkAddVersionCondition(
        dbVersion: String,
        releaseType: ReleaseTypeEnum,
        dbStatus: String,
        name: String
    ) {
        if (dbVersion.isNotBlank() && releaseType != ReleaseTypeEnum.NEW) {
            val storeFinalStatusList = StoreStatusEnum.getStoreFinalStatusList()
            if (!storeFinalStatusList.contains(dbStatus)) {
                throw ErrorCodeException(
                    errorCode = StoreMessageCode.STORE_VERSION_IS_NOT_FINISH,
                    params = arrayOf(name, dbVersion)
                )
            }
        }
    }

    override fun getNormalUpgradeFlag(storeCode: String, storeType: StoreTypeEnum, status: StoreStatusEnum): Boolean {
        val releaseTotalNum = storeBaseQueryDao.countByCondition(
            dslContext = dslContext,
            storeType = storeType,
            storeCode = storeCode,
            status = StoreStatusEnum.RELEASED
        )
        val currentNum = if (status == StoreStatusEnum.RELEASED) 1 else 0
        return releaseTotalNum > currentNum
    }

    override fun getStoreCodeById(
        storeId: String,
        storeType: StoreTypeEnum
    ): String {
        var code = storeBaseQueryDao.getComponentById(dslContext, storeId)?.storeCode
        if (code == null) {
            val storeCommonDao = getStoreCommonDao(storeType.name)
            code = storeCommonDao.getStoreCodeById(dslContext, storeId)
        }
        return code ?: ""
    }
}

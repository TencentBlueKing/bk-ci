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

package com.tencent.devops.store.common.service.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.store.common.dao.StoreBaseEnvQueryDao
import com.tencent.devops.store.common.dao.StoreBaseExtQueryDao
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.dao.StoreMemberDao
import com.tencent.devops.store.common.dao.StoreProjectRelDao
import com.tencent.devops.store.common.dao.StoreVersionLogDao
import com.tencent.devops.store.common.service.AbstractStoreComponentPkgSizeHandleService
import com.tencent.devops.store.common.service.StoreCommonService
import com.tencent.devops.store.common.service.StoreComponentVersionLogService
import com.tencent.devops.store.common.service.StoreComponentVersionQueryService
import com.tencent.devops.store.common.utils.StoreExtFieldUtil
import com.tencent.devops.store.common.utils.StoreUtils
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.version.StoreComponentVersionItem
import com.tencent.devops.store.pojo.common.version.StoreShowVersionInfo
import com.tencent.devops.store.pojo.common.version.StoreVersionLogInfo
import com.tencent.devops.store.pojo.common.version.StoreVersionSizeInfo
import com.tencent.devops.store.pojo.common.version.VersionInfo
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("TooManyFunctions")
@Service
class StoreComponentVersionQueryServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeBaseQueryDao: StoreBaseQueryDao,
    private val storeMemberDao: StoreMemberDao,
    private val storeVersionLogDao: StoreVersionLogDao,
    private val storeBaseExtQueryDao: StoreBaseExtQueryDao,
    private val storeBaseEnvQueryDao: StoreBaseEnvQueryDao,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val storeCommonService: StoreCommonService,
    private val storeComponentVersionLogService: StoreComponentVersionLogService
) : StoreComponentVersionQueryService {

    override fun getComponentVersionsByCode(
        userId: String,
        storeType: String,
        storeCode: String,
        page: Int,
        pageSize: Int,
        checkPermissionFlag: Boolean,
        storeStatusList: List<String>?
    ): Page<StoreComponentVersionItem> {
        val storeTypeEnum = StoreTypeEnum.valueOf(storeType)
        // 判断当前用户是否是组件的成员
        if (checkPermissionFlag && !storeMemberDao.isStoreMember(
                dslContext = dslContext,
                userId = userId,
                storeCode = storeCode,
                storeType = storeTypeEnum.type.toByte()
            )
        ) {
            throw ErrorCodeException(errorCode = StoreMessageCode.GET_INFO_NO_PERMISSION, params = arrayOf(storeCode))
        }
        val count = storeBaseQueryDao.countByCode(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeTypeEnum,
            storeStatusList = storeStatusList
        )
        if (count == 0) {
            return Page(page = page, pageSize = pageSize, count = 0, records = emptyList())
        }
        val records = storeBaseQueryDao.getComponentsByCode(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeTypeEnum,
            page = page,
            pageSize = pageSize,
            storeStatusList = storeStatusList
        )
        if (records.isEmpty()) {
            return Page(page = page, pageSize = pageSize, count = count.toLong(), records = emptyList())
        }
        val storeIds = records.map { it.id }
        val versionMap = storeVersionLogDao.getStoreVersions(
            dslContext = dslContext,
            storeIds = storeIds,
            getTestVersionFlag = true
        )?.associateBy({ it.storeId }, { it.content }) ?: emptyMap()
        val baseExtRecords = storeBaseExtQueryDao.getBaseExtByIds(dslContext, storeIds)
        val baseExtMap = baseExtRecords.groupBy({ it.storeId }, {
            it.fieldName to StoreExtFieldUtil.formatJson(it.fieldValue)
        }).mapValues { it.value.toMap() }
        val storeVersionInfos = records.map {
            StoreComponentVersionItem(
                storeId = it.id,
                storeCode = it.storeCode,
                storeType = StoreTypeEnum.getStoreType(it.storeType.toInt()),
                name = it.name,
                version = it.version,
                versionContent = versionMap[it.id],
                status = it.status,
                creator = it.creator,
                createTime = DateTimeUtil.toDateTime(it.createTime),
                extData = baseExtMap[it.id]
            )
        }
        return Page(
            page = page,
            pageSize = pageSize,
            count = count.toLong(),
            records = storeVersionInfos
        )
    }

    override fun getComponentShowVersionInfo(
        userId: String,
        storeType: String,
        storeCode: String
    ): StoreShowVersionInfo {
        val storeTypeEnum = StoreTypeEnum.valueOf(storeType)
        val record = storeBaseQueryDao.getNewestComponentByCode(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeTypeEnum
        ) ?: throw ErrorCodeException(
            errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
            params = arrayOf(storeCode)
        )
        // 获取回显版本号
        val cancelFlag = record.status == StoreStatusEnum.GROUNDING_SUSPENSION.name
        val showVersion = if (cancelFlag) {
            record.version
        } else {
            storeBaseQueryDao.getNewestComponentByCode(dslContext, storeCode, storeTypeEnum)?.version
        }
        val releaseType = if (record.status == StoreStatusEnum.INIT.name) {
            null
        } else {
            storeVersionLogDao.getStoreVersion(dslContext, record.id)?.releaseType
        }
        val showReleaseType = if (releaseType != null) {
            ReleaseTypeEnum.getReleaseTypeObj(releaseType.toInt())
        } else {
            null
        }
        return storeCommonService.getStoreShowVersionInfo(
            storeType = storeTypeEnum,
            cancelFlag = cancelFlag,
            releaseType = showReleaseType,
            version = showVersion
        )
    }

    override fun getComponentUpgradeVersionInfo(
        userId: String,
        storeType: String,
        storeCode: String,
        projectCode: String,
        instanceId: String?,
        osName: String?,
        osArch: String?
    ): VersionInfo? {
        val storeTypeEnum = StoreTypeEnum.valueOf(storeType)

        // 判断是否需要处理测试中的版本
        val isTestEnv = storeProjectRelDao.getProjectRelInfo(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeTypeEnum.type.toByte(),
            storeProjectType = StoreProjectTypeEnum.TEST,
            projectCode = projectCode,
            instanceId = instanceId
        )?.any() ?: false

        // 获取最新可用版本：根据环境状态构建版本状态过滤条件
        val statusList = mutableListOf(StoreStatusEnum.RELEASED.name).apply {
            if (isTestEnv) {
                // 增加测试中的状态
                addAll(StoreStatusEnum.getTestStatusList())
            }
        }
        // 查询符合条件的最新版本组件
        val validLatestVersionRecord = storeBaseQueryDao.getMaxVersionComponentByCode(
            dslContext = dslContext,
            storeType = storeTypeEnum,
            storeCode = storeCode,
            statusList = statusList
        ) ?: return null

        // 获取已安装组件关系信息：查询项目关联记录
        val installedRel = storeProjectRelDao.getProjectRelInfo(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeTypeEnum.type.toByte(),
            storeProjectType = StoreProjectTypeEnum.COMMON,
            projectCode = projectCode,
            instanceId = instanceId
        )?.firstOrNull()
        val installedVersion = installedRel?.version
        val validLatestBusNum = validLatestVersionRecord.busNum
        val validLatestVersion = validLatestVersionRecord.version
        // 未安装时直接返回最新版本信息
        if (installedVersion.isNullOrBlank()) {
            return createVersionInfo(validLatestVersion)
        }

        // 获取当前安装版本对应的业务号（用于版本比较）
        val currentBusNum = storeBaseQueryDao.getMaxBusNumByCode(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeTypeEnum,
            version = installedVersion
        )

        // 业务号比较逻辑（决定是否需要升级）
        return when {
            // 无法获取当前业务号时返回null
            currentBusNum == null -> null
            // 最新业务号更大时需要升级
            validLatestBusNum > currentBusNum -> createVersionInfo(validLatestVersion)
            // 业务号相同，但是安装时间晚于最新版本发布时间，需要更新
            validLatestBusNum == currentBusNum && isUpdateRequired(
                storeId = validLatestVersionRecord.id,
                installedTime = installedRel.createTime,
                osName = osName,
                osArch = osArch
            ) -> createVersionInfo(validLatestVersion)
            // 其他情况不需要升级
            else -> null
        }
    }

    override fun getStoreUpgradeStatusInfo(
        userId: String,
        storeType: String,
        storeCode: String,
        version: String
    ): Result<String?> {
        val record = storeBaseQueryDao.getComponentStatusInfo(
            dslContext = dslContext,
            storeCode = storeCode,
            version = version,
            storeType = StoreTypeEnum.valueOf(storeType)
        ) ?: throw ErrorCodeException(
            errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
            params = arrayOf("$storeCode:$version")
        )
        return Result(record.value1())
    }

    override fun getComponentVersionList(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        storeStatus: StoreStatusEnum?
    ): List<VersionInfo> {
        val records = storeBaseQueryDao.getVersionsByStoreCode(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeType,
            status = storeStatus
        )
        return StoreUtils.getVersionService(storeType).convertVersionList(records = records)
    }

    override fun getStoreVersionLogs(
        storeCode: String,
        storeType: StoreTypeEnum,
        page: Int,
        pageSize: Int
    ): Result<Page<StoreVersionLogInfo>> {
        return storeComponentVersionLogService.getStoreComponentVersionLogs(
            storeCode = storeCode,
            storeType = storeType,
            page = page,
            pageSize = pageSize
        )
    }

    override fun getStoreVersionSize(
        storeCode: String,
        storeType: StoreTypeEnum,
        version: String,
        osName: String?,
        osArch: String?
    ): StoreVersionSizeInfo {
        return SpringContextUtil.getBean(
            AbstractStoreComponentPkgSizeHandleService::class.java,
            "${storeType}_PKG_SIZE_HANDLE_SERVICE"
        ).getComponentVersionSize(
            version = version,
            storeCode = storeCode,
            osName = osName,
            osArch = osArch
        )
    }

    private fun isUpdateRequired(
        storeId: String,
        installedTime: LocalDateTime,
        osName: String?,
        osArch: String?
    ): Boolean {
        val envRecord = storeBaseEnvQueryDao.getBaseEnvsByStoreId(
            dslContext = dslContext,
            storeId = storeId,
            osName = osName,
            osArch = osArch
        )?.firstOrNull()
        return envRecord?.updateTime?.let { packageTime ->
            installedTime < packageTime
        } ?: false
    }

    private fun createVersionInfo(versionValue: String, versionName: String = versionValue) =
        VersionInfo(
            versionName = versionName.takeIf { it.isNotBlank() } ?: versionValue,
            versionValue = versionValue
        )
}

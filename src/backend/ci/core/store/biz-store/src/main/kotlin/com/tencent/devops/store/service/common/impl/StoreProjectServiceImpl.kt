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

package com.tencent.devops.store.service.common.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.dao.common.StoreStatisticDailyDao
import com.tencent.devops.store.dao.common.StoreStatisticDao
import com.tencent.devops.store.pojo.common.InstalledProjRespItem
import com.tencent.devops.store.pojo.common.StoreDailyStatisticRequest
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreProjectService
import com.tencent.devops.store.service.common.StoreUserService
import java.util.Date
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * store项目通用业务逻辑类
 *
 * since: 2019-03-22
 */
@Suppress("ALL")
@Service
class StoreProjectServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val storeStatisticDao: StoreStatisticDao,
    private val storeStatisticDailyDao: StoreStatisticDailyDao,
    private val storeUserService: StoreUserService,
    private val redisOperation: RedisOperation
) : StoreProjectService {

    private val logger = LoggerFactory.getLogger(StoreProjectServiceImpl::class.java)

    /**
     * 根据商城组件标识获取已安装的项目列表
     */
    override fun getInstalledProjects(
        accessToken: String,
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum
    ): Result<List<InstalledProjRespItem>> {
        val watcher = Watcher(id = "getInstalledProjects|$userId|$storeCode|$storeType")
        try {
            // 获取用户有权限的项目列表
            watcher.start("get accessible projects")
            val projectList = client.get(ServiceProjectResource::class).list(userId).data
            if (projectList?.count() == 0) {
                return Result(mutableListOf())
            }
            val projectCodeMap = projectList?.map { it.projectCode to it }?.toMap()!!
            watcher.start("getInstalledProject")
            val records = storeProjectRelDao.getInstalledProject(
                dslContext = dslContext,
                storeCode = storeCode,
                storeType = storeType.type.toByte(),
                authorizedProjectCodeList = projectCodeMap.keys
            )
            watcher.stop()
            val result = mutableListOf<InstalledProjRespItem>()
            records?.forEach {
                result.add(
                    InstalledProjRespItem(
                        projectCode = it.projectCode,
                        projectName = projectCodeMap[it.projectCode]?.projectName,
                        creator = it.creator,
                        createTime = DateTimeUtil.toDateTime(it.createTime)
                    )
                )
            }
            return Result(result)
        } finally {
            LogUtils.printCostTimeWE(watcher = watcher)
        }
    }

    override fun installStoreComponent(
        userId: String,
        projectCodeList: ArrayList<String>,
        storeId: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        publicFlag: Boolean,
        channelCode: ChannelCode
    ): Result<Boolean> {
        val testProjectCodeList = storeProjectRelDao.getTestProjectCodesByStoreCode(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeType
        )?.map { it.value1() }
        if (!testProjectCodeList.isNullOrEmpty()) {
            // 剔除需要安装的调试项目
            projectCodeList.removeAll(testProjectCodeList)
        }
        if (projectCodeList.isNullOrEmpty()) {
            // 如果全都是调试项目，无需安装
            return Result(true)
        }
        val validateInstallResult = validateInstallPermission(
            publicFlag = publicFlag,
            userId = userId,
            storeCode = storeCode,
            storeType = storeType,
            projectCodeList = projectCodeList,
            channelCode = channelCode
        )
        if (validateInstallResult.isNotOk()) {
            return validateInstallResult
        }
        var increment = 0
        dslContext.transaction { t ->
            val context = DSL.using(t)
            for (projectCode in projectCodeList) {
                // 判断是否已安装
                val relCount = storeProjectRelDao.countInstalledProject(
                    dslContext = context,
                    projectCode = projectCode,
                    storeCode = storeCode,
                    storeType = storeType.type.toByte()
                )
                if (relCount > 0) {
                    continue
                }
                // 未安装则入库
                val result = storeProjectRelDao.addStoreProjectRel(
                    dslContext = context,
                    userId = userId,
                    storeCode = storeCode,
                    projectCode = projectCode,
                    type = StoreProjectTypeEnum.COMMON.type.toByte(),
                    storeType = storeType.type.toByte()
                )
                // 使用 ON DUPLICATE KEY UPDATE，如果将行作为新行插入，则每行的受影响行值为 1，如果更新现有行，则为 2
                if (result == 1) {
                    increment += 1
                }
            }
            // 更新安装量
            if (increment > 0) {
                val redisLock = RedisLock(redisOperation, "store:$storeId", 10)
                try {
                    redisLock.lock()
                    updateStoreIncrement(
                        context = context,
                        userId = userId,
                        storeId = storeId,
                        storeCode = storeCode,
                        storeType = storeType,
                        increment = increment
                    )
                } finally {
                    redisLock.unlock()
                }
            }
        }
        return Result(true)
    }

    private fun updateStoreIncrement(
        context: DSLContext,
        userId: String,
        storeId: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        increment: Int
    ) {
        storeStatisticDao.updateDownloads(
            dslContext = context,
            userId = userId,
            storeId = storeId,
            storeCode = storeCode,
            storeType = storeType.type.toByte(),
            increment = increment
        )
        val storeStatisticsRecord = storeStatisticDao.batchGetStatisticByStoreCode(
            dslContext = context,
            storeCodeList = listOf(storeCode),
            storeType = storeType.type.toByte()
        )
        val downloads = if (storeStatisticsRecord.isNotEmpty) storeStatisticsRecord[0].value1().toInt() else 0
        val storeDailyStatistic = storeStatisticDailyDao.getDailyStatisticByCode(
            dslContext = context,
            storeCode = storeCode,
            storeType = storeType.type.toByte(),
            statisticsTime = DateTimeUtil.convertDateToFormatLocalDateTime(Date(), "yyyy-MM-dd")
        )
        val storeDailyStatisticRequest = StoreDailyStatisticRequest(
            totalDownloads = downloads,
            dailyDownloads = storeDailyStatistic?.dailyDownloads ?: 0 + increment
        )
        if (storeDailyStatistic != null) {
            storeStatisticDailyDao.updateDailyStatisticData(
                dslContext = context,
                storeCode = storeCode,
                storeType = storeType.type.toByte(),
                storeDailyStatisticRequest = storeDailyStatisticRequest
            )
        } else {
            storeStatisticDailyDao.insertDailyStatisticData(
                dslContext = context,
                storeCode = storeCode,
                storeType = storeType.type.toByte(),
                storeDailyStatisticRequest = storeDailyStatisticRequest
            )
        }
    }

    override fun validateInstallPermission(
        publicFlag: Boolean,
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        projectCodeList: ArrayList<String>,
        channelCode: ChannelCode
    ): Result<Boolean> {
        val installFlag = storeUserService.isCanInstallStoreComponent(publicFlag, userId, storeCode, storeType) // 是否能安装
        if (!installFlag) {
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PERMISSION_DENIED,
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        }
        if (ChannelCode.isNeedAuth(channelCode)) {
            // 获取用户有权限的项目列表
            val projectList = client.get(ServiceProjectResource::class).list(userId).data
            // 判断用户是否有权限安装到对应的项目
            val privilegeProjectCodeList = mutableListOf<String>()
            projectList?.map {
                privilegeProjectCodeList.add(it.projectCode)
            }
            val dataList = mutableListOf<String>()
            dataList.addAll(projectCodeList)
            dataList.removeAll(privilegeProjectCodeList)
            if (dataList.isNotEmpty()) {
                // 存在用户没有安装权限的项目，抛出错误提示
                return I18nUtil.generateResponseDataObject(
                    messageCode = StoreMessageCode.USER_PROJECT_IS_NOT_ALLOW_INSTALL,
                    params = arrayOf(dataList.toString()),
                    data = false,
                    language = I18nUtil.getLanguage(userId)
                )
            }
        }
        return Result(true)
    }

    override fun uninstall(
        storeType: StoreTypeEnum,
        storeCode: String,
        projectCode: String
    ): Result<Boolean> {
        storeProjectRelDao.deleteRel(dslContext, storeCode, storeType.type.toByte(), projectCode)
        return Result(true)
    }

    /**
     * 判断组件是否被项目安装
     */
    override fun isInstalledByProject(
        projectCode: String,
        storeCode: String,
        storeType: Byte
    ): Boolean {
        return storeProjectRelDao.isInstalledByProject(
            dslContext = dslContext,
            projectCode = projectCode,
            storeCode = storeCode,
            storeType = storeType
        )
    }
}

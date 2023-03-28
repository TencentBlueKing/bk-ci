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

package com.tencent.devops.store.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.store.dao.ExtItemServiceDao
import com.tencent.devops.store.dao.ExtServiceDao
import com.tencent.devops.store.dao.ExtServiceFeatureDao
import com.tencent.devops.store.dao.common.ReasonRelDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.pojo.common.InstalledProjRespItem
import com.tencent.devops.store.pojo.common.UnInstallReq
import com.tencent.devops.store.pojo.common.enums.ReasonTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.enums.ExtServiceStatusEnum
import com.tencent.devops.store.pojo.vo.ExtServiceRespItem
import com.tencent.devops.store.service.common.StoreProjectService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.StopWatch
import java.time.LocalDateTime

@Service
class ExtServiceProjectService @Autowired constructor(
    val extServiceDao: ExtServiceDao,
    val extServiceFeatureDao: ExtServiceFeatureDao,
    val extItemServiceDao: ExtItemServiceDao,
    val storeMemberDao: StoreMemberDao,
    val reasonRelDao: ReasonRelDao,
    private val storeProjectRelDao: StoreProjectRelDao,
    val dslContext: DSLContext,
    val client: Client,
    private val storeProjectService: StoreProjectService
) {

    /**
     * 安装扩展服务到项目
     */
    fun installService(
        userId: String,
        projectCodeList: ArrayList<String>,
        serviceCode: String,
        channelCode: ChannelCode
    ): Result<Boolean> {
        logger.info("installService:Input:($userId,$projectCodeList,$serviceCode)")
        // 判断扩展服务标识是否合法
        val serviceRecord = extServiceDao.getServiceLatestByCode(dslContext, serviceCode)
            ?: throw RuntimeException("serviceCode=$serviceCode")
        val serviceFeature = extServiceFeatureDao.getServiceByCode(dslContext, serviceCode)
        val validateInstallResult = storeProjectService.validateInstallPermission(
            publicFlag = serviceFeature?.publicFlag ?: false,
            userId = userId,
            storeCode = serviceRecord.serviceCode,
            storeType = StoreTypeEnum.SERVICE,
            projectCodeList = projectCodeList
        )
        if (validateInstallResult.isNotOk()) {
            return validateInstallResult
        }
        logger.info("installService serviceId=${serviceRecord.id},publicFlag=${serviceFeature?.publicFlag}")
        return storeProjectService.installStoreComponent(
            userId = userId,
            projectCodeList = projectCodeList,
            storeId = serviceRecord.id,
            storeCode = serviceRecord.serviceCode,
            storeType = StoreTypeEnum.SERVICE,
            publicFlag = serviceFeature?.publicFlag ?: false,
            channelCode = channelCode
        )
    }

    fun getInstalledProjects(
        accessToken: String,
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum
    ): Result<List<InstalledProjRespItem>> {
        logger.info("getInstalledProjects params:[$accessToken|$userId|$storeCode|$storeType]")
        val watch = StopWatch()
        // 获取用户有权限的项目列表
        watch.start("get accessible projects")
        val projectList = client.get(ServiceProjectResource::class).list(userId).data
        watch.stop()
        logger.info("$userId accessible projectList is :size=${projectList?.size}")
        if (projectList?.count() == 0) {
            return Result(mutableListOf())
        }
        watch.start("projectCodeMap")
        val projectCodeMap = projectList?.map { it.projectCode to it }?.toMap()!!
        watch.stop()
        watch.start("getInstalledProject")
        val records =
            storeProjectRelDao.getInstalledProject(dslContext, storeCode, storeType.type.toByte(), projectCodeMap.keys)
        watch.stop()
        watch.start("generate InstalledProjRespItem")
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
        watch.stop()
        logger.info("getInstalledProjects:watch:$watch")
        return Result(result)
    }

    fun getServiceByProjectCode(projectCode: String): Result<List<ExtServiceRespItem>> {
        logger.info("getServiceByProjectCode projectCode[$projectCode]")
        val projectRelRecords = extServiceDao.getProjectServiceBy(dslContext, projectCode)
        if (projectRelRecords == null || projectRelRecords.size == 0) {
            return Result(emptyList<ExtServiceRespItem>())
        }

        val serviceIds = projectRelRecords.map { it["serviceId"] as String }

        val serviceItemMap = mutableMapOf<String, Set<String>>()
        extItemServiceDao.getItemByServiceId(dslContext, serviceIds)?.forEach {
            val serviceId = it.serviceId
            val itemId = it.itemId
            var itemList = mutableSetOf<String>()

            if (serviceItemMap.containsKey(serviceId)) {
                itemList = serviceItemMap[serviceId] as MutableSet<String>
                itemList.add(serviceId)
                serviceItemMap[serviceId] = itemList
            } else {
                itemList.add(itemId)
                serviceItemMap[serviceId] = itemList
            }
        }
        logger.info("getServiceByProjectCode serviceItemMap[$serviceItemMap]")

        val serviceRecords = mutableListOf<ExtServiceRespItem>()
        projectRelRecords.forEach {
            val publicFlag = it["publicFlag"] as Boolean
            val projectType = it["projectType"] as Byte
            val installUser: String
            val installTime: String
            if (projectType == StoreProjectTypeEnum.INIT.type.toByte()) {
                installUser = it["publisher"] as String
                installTime = (it["pubTime"] as LocalDateTime).timestampmilli().toString()
            } else {
                installUser = it["projectInstallUser"] as String
                installTime = (it["projectInstallTime"] as LocalDateTime).timestampmilli().toString()
            }
            serviceRecords.add(
                ExtServiceRespItem(
                    serviceId = it["serviceId"] as String,
                    serviceName = it["serviceName"] as String,
                    serviceCode = it["serviceCode"] as String,
                    language = "",
                    category = "",
                    version = it["version"] as String,
                    logoUrl = "",
                    serviceStatus = ExtServiceStatusEnum.getServiceStatus((it["serviceStatus"] as Byte).toInt()),
                    projectName = projectCode,
                    creator = it["creator"] as String,
                    releaseFlag = true,
                    modifier = it["modifier"] as String,
                    itemName = emptyList(),
                    isUninstall = canUninstall(publicFlag, projectType),
                    publisher = installUser,
                    publishTime = installTime,
                    createTime = (it["createTime"] as LocalDateTime).timestampmilli().toString(),
                    updateTime = (it["updateTime"] as LocalDateTime).timestampmilli().toString(),
                    itemIds = serviceItemMap[it["serviceId"] as String] ?: emptySet()
                )
            )
        }
        return Result(serviceRecords)
    }

    // 卸载扩展
    fun uninstallService(
        userId: String,
        projectCode: String,
        serviceCode: String,
        unInstallReq: UnInstallReq
    ): Result<Boolean> {
        logger.info("uninstallService, $projectCode | $serviceCode | $userId")
        // 用户是否有权限卸载
        val isInstaller =
            storeProjectRelDao.isInstaller(dslContext, userId, serviceCode, StoreTypeEnum.SERVICE.type.toByte())
        logger.info("uninstallService, isInstaller=$isInstaller")

        val isAdmin = storeMemberDao.isStoreAdmin(dslContext, userId, serviceCode, StoreTypeEnum.SERVICE.type.toByte())
        logger.info("uninstallService, isAdmin=$isAdmin")

        if (!(isAdmin || isInstaller)) {
            return MessageUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PERMISSION_DENIED,
                params = arrayOf(serviceCode),
                language = I18nUtil.getLanguage(userId)
            )
        }

        dslContext.transaction { t ->
            val context = DSL.using(t)

            // 卸载
            storeProjectService.uninstall(StoreTypeEnum.SERVICE, serviceCode, projectCode)

            // 入库卸载原因
            unInstallReq.reasonList.forEach {
                if (it?.reasonId != null) {
                    val id = UUIDUtil.generate()
                    reasonRelDao.add(
                        dslContext = context,
                        id = id,
                        userId = userId,
                        storeCode = serviceCode,
                        storeType = StoreTypeEnum.SERVICE.type.toByte(),
                        reasonId = it.reasonId,
                        note = it.note,
                        type = ReasonTypeEnum.UNINSTALLATOM.type
                    )
                }
            }
        }

        return Result(true)
    }

    private fun canUninstall(publicFlag: Boolean, projectType: Byte): Boolean {
        // 公共的不可卸载
        if (publicFlag) {
            return false
        }
        // 扩展初始化绑定项目不可卸载
        if (projectType.toInt() == StoreProjectTypeEnum.INIT.type) {
            return false
        }
        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExtServiceProjectService::class.java)
    }
}

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

package com.tencent.devops.remotedev.service

import com.tencent.devops.common.api.constant.HTTP_400
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.project.api.op.OPProjectResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.WindowsGpuResourceDao
import com.tencent.devops.remotedev.dao.WindowsResourceTypeDao
import com.tencent.devops.remotedev.dao.WindowsResourceZoneDao
import com.tencent.devops.remotedev.dao.WindowsSpecResourceDao
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.dao.WorkspaceWindowsDao
import com.tencent.devops.remotedev.dispatch.kubernetes.interfaces.ServiceStartCloudInterface
import com.tencent.devops.remotedev.pojo.CgsResourceConfig
import com.tencent.devops.remotedev.pojo.WindowsResourceTypeConfig
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfig
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfigType
import com.tencent.devops.remotedev.pojo.common.QuotaType
import com.tencent.devops.remotedev.pojo.op.WindowsSpecResInfo
import com.tencent.devops.remotedev.pojo.remotedev.ResourceVmReq
import com.tencent.devops.remotedev.pojo.remotedev.ResourceVmRespDataMachineResource
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import com.tencent.devops.remotedev.utils.CommonUtil
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WindowsResourceConfigService @Autowired constructor(
    private val dslContext: DSLContext,
    private val windowsResourceTypeDao: WindowsResourceTypeDao,
    private val windowsResourceZoneDao: WindowsResourceZoneDao,
    private val windowsSpecResourceDao: WindowsSpecResourceDao,
    private val workspaceCommon: WorkspaceCommon,
    private val client: Client,
    private val workspaceJoinDao: WorkspaceJoinDao,
    private val workspaceWindowsDao: WorkspaceWindowsDao,
    private val windowsGpuResourceDao: WindowsGpuResourceDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WindowsResourceConfigService::class.java)
        private const val SYNC_CHUNKED = 100
    }

    fun syncStartCloudResourceList() {
        val data = workspaceCommon.realtimeStartCloudResourceList()
        if (data.isNotEmpty()) {
            windowsGpuResourceDao.deleteAllResource(dslContext)
            data.chunked(SYNC_CHUNKED).forEach { chunk ->
                windowsGpuResourceDao.createOrUpdateResource(dslContext, chunk)
                Thread.sleep(50)
            }
        }
        // 同步 gpu空闲资源数据
        kotlin.runCatching {
            getAllVmResource()
        }.onFailure {
            logger.warn("get all vm resource failed.|${it.message}")
        }
    }

    // 获取vm空闲资源，包含Devcloud专区和其他
    private fun getAllVmResource() {
        val resList = listOfNotNull(
            SpringContextUtil.getBean(ServiceStartCloudInterface::class.java).getResourceVm(
                ResourceVmReq(null, null, false)
            ).data,
            SpringContextUtil.getBean(ServiceStartCloudInterface::class.java).getResourceVm(
                ResourceVmReq(null, null, true)
            ).data
        )
            .flatten() // 将上述两个list合并成一个
            .flatMap { resource ->
                resource.machineResources?.map { mas ->
                    ResourceVmRespDataMachineResource(
                        zoneId = resource.zoneId,
                        machineType = mas.machineType,
                        cap = mas.cap ?: 0,
                        used = mas.used ?: 0,
                        free = mas.free ?: 0
                    )
                } ?: emptyList()
            }

        if (resList.isNotEmpty()) {
            windowsGpuResourceDao.deleteVmResource(dslContext)
            windowsGpuResourceDao.insertVmResource(dslContext, resList)
        }
    }

    /**
     * 获取cgs资源池的机型和区域列表
     */
    fun getCgsConfig(): CgsResourceConfig {
        val machineTypeList = mutableListOf<String>()
        val zoneList = mutableListOf<String>()
        val cgsConfigList = windowsGpuResourceDao.getCgsConfig(dslContext)
        cgsConfigList.forEach { cgs ->
            if (!machineTypeList.contains(cgs.value2())) {
                machineTypeList.add(cgs.value2())
            }
            if (!zoneList.contains(cgs.value1())) {
                zoneList.add(cgs.value1())
            }
        }
        logger.info("getCgsConfig|machineTypeList|$machineTypeList|zoneList|$zoneList")

        return CgsResourceConfig(
            zoneList = zoneList,
            machineTypeList = machineTypeList
        )
    }

    @Suppress("NestedBlockDepth", "ComplexMethod")
    fun allWindowsQuota(
        searchCustom: Boolean?,
        quotaType: QuotaType,
        withProjectLimit: String?
    ): Map<String, Map<String, Int>> {
        // 自定义镜像为显卡配额，固定镜像为资源池中的配额加上显卡配额
        val res = mutableMapOf<String, MutableMap<String, Int>>()
        val spec = windowsResourceZoneDao.fetchAllSpec(dslContext).map { it.zoneShortName }
        if (searchCustom != true) {
            workspaceCommon.realtimeStartCloudResourceList().forEach {
                if (quotaType == QuotaType.OFFSHORE && it.zoneId in spec) return@forEach
                val key = it.zoneId.replace(Regex("\\d+"), "")
                val map = res.getOrPut(key) { mutableMapOf() }
                if (it.status == 11 && it.locked != true && it.internal == quotaType.getInternal()) {
                    map[it.machineType] = (map[it.machineType] ?: 0) + 1
                }
            }
        }

        SpringContextUtil.getBean(ServiceStartCloudInterface::class.java).getResourceVm(
            ResourceVmReq(null, null, quotaType.getInternal())
        ).data?.forEach { resource ->
            if (quotaType == QuotaType.OFFSHORE && resource.zoneId in spec) return@forEach
            val key = resource.zoneId.replace(Regex("\\d+"), "")
            val map = res.getOrPut(key) { mutableMapOf() }
            resource.machineResources?.forEach { mas ->
                map[mas.machineType] = (map[mas.machineType] ?: 0) + (mas.free ?: 0)
            }
        }

        if (withProjectLimit != null) {
            val nowSizeCount = workspaceJoinDao.fetchProjectMachineTypeCount(dslContext, withProjectLimit)
            val specSizes = windowsResourceTypeDao.fetchAll(
                dslContext = dslContext,
                withUnavailable = false,
                specModel = true
            ).map { it.size }.toSet()
            val projectSpecQuota = windowsSpecResourceDao.fetchSpec(
                projectId = withProjectLimit,
                machineType = null,
                dslContext = dslContext,
                sqlLimit = PageUtil.convertPageSizeToSQLLimit(1, 1000)
            ).map { it.size to it.quota }.toMap()
            res.values.forEach { sizeAndCounts ->
                sizeAndCounts.forEach sizeAndCount@{ (size, allCount) ->
                    // 不是特殊机型，不进行计算
                    if (!specSizes.contains(size)) {
                        return@sizeAndCount
                    }
                    // 没有库存或者项目下没有配额不能选
                    val projectCount = projectSpecQuota[size] ?: 0
                    if (allCount == 0 || (projectCount <= 0)) {
                        sizeAndCounts[size] = 0
                        return@sizeAndCount
                    }
                    // 项目已拥有机器把项目配额占满了也不能选
                    val diff = projectCount - (nowSizeCount[size] ?: 0)
                    if (diff <= 0) {
                        sizeAndCounts[size] = 0
                        return@sizeAndCount
                    }
                    // 有库存的同时有没有占满项目配额就可以选
                    sizeAndCounts[size] = diff
                }
            }
        }

        return res
    }

    /**
     * @return 返回具体的n个区域id
     */
    fun createCheckWhenWinNotAlready(
        windowsZone: WindowsResourceZoneConfig,
        windowsConfig: WindowsResourceTypeConfig,
        newNum: Int,
        quotaType: QuotaType
    ): List<String> {
        val data = kotlin.runCatching {
            SpringContextUtil.getBean(ServiceStartCloudInterface::class.java).getResourceVm(
                ResourceVmReq(
                    zoneId = windowsZone.zoneShortName.replace(Regex("\\d+"), ""),
                    machineType = windowsConfig.size,
                    internal = quotaType.getInternal()
                )
            ).data
        }.getOrElse {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.ZONE_VM_RESOURCE_NOT_ENOUGH.errorCode,
                params = arrayOf(
                    windowsZone.zone,
                    windowsConfig.size,
                    "unkown",
                    newNum.toString()
                )
            )
        }
        val spec = lazy { getAllSpecZoneShortName() }
        val free = CommonUtil.parseResourceVmRespData(
            data = data,
            zoneConfig = windowsZone,
            spec = spec,
            size = windowsConfig.size
        )
        if (free.isEmpty()) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.ZONE_VM_RESOURCE_NOT_ENOUGH.errorCode,
                params = arrayOf(
                    windowsZone.zone,
                    windowsConfig.size,
                    "0",
                    newNum.toString()
                )
            )
        }
        if (free.values.sum() < newNum) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.ZONE_VM_RESOURCE_NOT_ENOUGH.errorCode,
                params = arrayOf(
                    windowsZone.zone,
                    windowsConfig.size,
                    free.toString(),
                    newNum.toString()
                )
            )
        }
        val res = mutableListOf<String>()
        free.forEach { (k, v) ->
            val diff = newNum - res.count()
            if (diff > 0) {
                res.addAll(Array(minOf(diff, v)) { k })
            } else {
                return@forEach
            }
        }
        return res
    }

    fun getAllZone(): List<WindowsResourceZoneConfig> {
        logger.info("get all windows resource zone")
        return windowsResourceZoneDao.fetchAll(dslContext, true)
    }

    fun getAllType(withUnavailable: Boolean?, onlySpecModel: Boolean?): List<WindowsResourceTypeConfig> {
        logger.info("get all windows resource type")
        return windowsResourceTypeDao.fetchAll(dslContext, withUnavailable ?: false, onlySpecModel)
    }

    fun getTypeConfig(
        machineType: String
    ): WindowsResourceTypeConfig? {
        logger.info("get windows resource config type $machineType")
        return windowsResourceTypeDao.fetchAny(dslContext, machineType)
    }

    fun getTypeConfig(
        id: Int
    ): WindowsResourceTypeConfig? {
        logger.info("get windows resource config type $id")
        return windowsResourceTypeDao.fetchAny(dslContext, id)
    }

    fun getZoneConfig(
        zone: String
    ): WindowsResourceZoneConfig? {
        logger.info("get windows resource config zone $zone")
        return windowsResourceZoneDao.fetchAny(dslContext, zone)
    }

    fun getAvailableZone(
        windowsZone: WindowsResourceZoneConfig,
        type: WindowsResourceZoneConfigType?
    ): WindowsResourceZoneConfig? {
        logger.info("get windows resource config zone type $type")
        return windowsResourceZoneDao.fetchAll(
            dslContext = dslContext,
            withUnavailable = true,
            type = type ?: WindowsResourceZoneConfigType.DEFAULT
        ).firstOrNull { it.id == windowsZone.id }
    }

    fun getAvailableZone(
        zoneId: String,
        type: WindowsResourceZoneConfigType
    ): WindowsResourceZoneConfig? {
        logger.info("get windows resource config zone type $type")
        return windowsResourceZoneDao.fetchAll(
            dslContext = dslContext,
            withUnavailable = true,
            type = type
        ).firstOrNull { it.zoneShortName.startsWith(zoneId) }
    }

    fun getAllSpecZone() = windowsResourceZoneDao.fetchAllSpec(dslContext)

    fun getAllSpecZoneShortName() = getAllSpecZone().map { it.zoneShortName }

    // 新增windows硬件资源配置
    fun addWindowsResource(windowsResourceConfig: WindowsResourceTypeConfig): Boolean {
        logger.info("WorkspaceTemplateService|addWindowsResource|windowsResourceConfig|$windowsResourceConfig")
        // 模板信息写入DB
        windowsResourceTypeDao.save(dslContext, windowsResourceConfig)
        return true
    }

    // 新增windows硬件资源配置
    fun addWindowsResourceZone(windowsResourceConfig: WindowsResourceZoneConfig): Boolean {
        logger.info("WorkspaceTemplateService|addWindowsResourceZone|windowsResourceConfig|$windowsResourceConfig")
        // 模板信息写入DB
        windowsResourceZoneDao.save(dslContext, windowsResourceConfig)
        return true
    }

    // 更新windows硬件资源配置
    fun updateWindowsResource(
        id: Long,
        windowsResourceConfig: WindowsResourceTypeConfig
    ): Boolean {
        logger.info(
            "WorkspaceTemplateService|updateWorkspaceTemplate|" +
                "id|$id|windowsResourceConfig|$windowsResourceConfig"
        )

        // 更新模板信息
        windowsResourceTypeDao.updateWindowsResourceConfig(
            id = id,
            config = windowsResourceConfig,
            dslContext = dslContext
        )
        return true
    }

    // 更新windows硬件资源配置
    fun updateWindowsResourceZone(
        id: Long,
        windowsResourceConfig: WindowsResourceZoneConfig
    ): Boolean {
        logger.info(
            "WorkspaceTemplateService|updateWindowsResourceZone|" +
                "id|$id|windowsResourceConfig|$windowsResourceConfig"
        )

        // 更新模板信息
        windowsResourceZoneDao.updateWindowsResourceZoneConfig(
            id = id,
            config = windowsResourceConfig,
            dslContext = dslContext
        )
        return true
    }

    fun deleteWindowsResource(
        id: Long
    ): Boolean {
        logger.info("WindowsResourceConfigService|deleteWindowsResource|id|$id")
        // 删除模板信息
        windowsResourceTypeDao.deleteWindowsResource(
            id = id,
            dslContext = dslContext
        )

        return true
    }

    fun deleteWindowsResourceZone(
        id: Long
    ): Boolean {
        logger.info("WindowsResourceConfigService|deleteWindowsResourceZone|id|$id")
        // 删除模板信息
        windowsResourceZoneDao.deleteWindowsResource(
            id = id,
            dslContext = dslContext
        )

        return true
    }

    fun createOrUpdateSpec(
        data: WindowsSpecResInfo
    ): Boolean {
        return windowsSpecResourceDao.createOrUpdateSpecRes(
            dslContext = dslContext,
            projectId = data.projectId,
            size = data.size,
            quota = data.quota
        )
    }

    fun deleteSpec(
        projectId: String,
        size: String
    ): Boolean {
        return windowsSpecResourceDao.delete(dslContext = dslContext, projectId = projectId, size = size)
    }

    fun updateAndGetAllSpec(
        projectId: String,
        machineType: String?,
        count: Int
    ): Map<String, Int> {
        if (count != 0 && machineType != null) {
            val res = windowsSpecResourceDao.fetchQuota(dslContext, projectId, machineType) ?: 0
            windowsSpecResourceDao.createOrUpdateSpecRes(dslContext, projectId, machineType, count + res)
        }
        return windowsSpecResourceDao.fetchAllQuota(dslContext, projectId)
    }

    fun fetchSpec(
        projectId: String?,
        machineType: String?,
        page: Int?,
        pageSize: Int?
    ): Page<WindowsSpecResInfo> {
        val limit = PageUtil.convertPageSizeToSQLLimit(page ?: 1, pageSize ?: 20)
        val count = windowsSpecResourceDao.fetchSpecCount(projectId, machineType, dslContext)
        val recode = windowsSpecResourceDao.fetchSpec(projectId, machineType, dslContext, limit).map {
            WindowsSpecResInfo(
                projectId = it.projectId,
                size = it.size,
                quota = it.quota
            )
        }
        return Page(
            page = page ?: 1,
            pageSize = pageSize ?: 20,
            count = count,
            records = recode
        )
    }

    // 追加项目的云桌面配额
    fun updateAndGetProjectTotalQuota(
        userId: String,
        projectId: String,
        quota: Int
    ): Int {
        logger.info("addProjectTotalQuota|projectId|$projectId|quota|$quota")
        // 先获取当前项目的properties配置获取当前配额，再追加申请的配额，更新
        val projectInfo = kotlin.runCatching {
            client.get(ServiceProjectResource::class).get(projectId)
        }.onFailure { logger.warn("get project $projectId info error|${it.message}") }
            .getOrElse { null }?.data ?: throw RemoteServiceException(
            "not find project $projectId", HTTP_400
        )
        val projectProperties = projectInfo.properties
        if (projectProperties?.remotedev == null || projectProperties.remotedev == false) {
            logger.info("addProjectTotalQuota|$projectId|not open remotedev")
            throw RemoteServiceException(
                "project $projectId not open remotedev", HTTP_400
            )
        }
        val curQuota = projectProperties.cloudDesktopNum
        if (quota != 0) {
            client.get(OPProjectResource::class).setProjectProperties(
                userId = userId,
                projectCode = projectId,
                properties = projectProperties.copy(
                    cloudDesktopNum = (curQuota + quota)
                )
            )
        }
        return curQuota + quota
    }

    fun addProjectRemotedevManager(
        userId: String,
        projectId: String,
        manager: String,
        delete: Boolean?
    ): Boolean {
        logger.info("addProjectTotalQuota|projectId|$projectId|manager|$manager|delete=$delete")
        // 先获取当前项目的properties配置获取当前配额，再追加申请的配额，更新
        val projectInfo = kotlin.runCatching {
            client.get(ServiceProjectResource::class).get(projectId)
        }.onFailure { logger.warn("get project $projectId info error|${it.message}") }
            .getOrElse { null }?.data ?: throw RemoteServiceException(
            "not find project $projectId", HTTP_400
        )
        val projectProperties = projectInfo.properties
        if (projectProperties?.remotedev == null || projectProperties.remotedev == false) {
            logger.info("addProjectRemotedevManager|$projectId|not open remotedev")
            return false
        }
        val oldManagers = projectProperties.remotedevManager?.split(";")?.toMutableSet() ?: mutableSetOf()
        if (delete == true) {
            oldManagers.removeAll(manager.split(",").toSet())
        } else {
            oldManagers.addAll(manager.split(",").toSet())
        }
        return client.get(OPProjectResource::class).setProjectProperties(
            userId = userId,
            projectCode = projectId,
            properties = projectProperties.copy(remotedevManager = oldManagers.joinToString(";"))
        ).data == true
    }

    fun createCheckSpecLimit(
        windowsType: String,
        projectId: String,
        workspaceNames: Set<String>,
        createCount: Int
    ) {
        val allSpecSize = getAllType(true, true).map { it.size }.toSet()
        if (windowsType.trim() in allSpecSize) {
            val specQuota = windowsSpecResourceDao.fetchQuota(
                dslContext = dslContext,
                projectId = projectId,
                size = windowsType.trim()
            )
            if (specQuota != null) {
                val count = workspaceWindowsDao.fetchUsedSizeCount(
                    dslContext = dslContext,
                    workspaceNames = workspaceNames,
                    size = windowsType.trim()
                )
                if (count + createCount > specQuota) {
                    throw ErrorCodeException(
                        errorCode = ErrorCodeEnum.PROJECT_DESKTOP_SPEC_RESOURCES_INSUFFICIENT.errorCode,
                        params = arrayOf(windowsType.trim(), specQuota.toString(), count.toString())
                    )
                }
            } else {
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.PROJECT_DESKTOP_SPEC_RESOURCES_INSUFFICIENT.errorCode,
                    params = arrayOf(windowsType.trim(), "0", "0")
                )
            }
        }
    }
}

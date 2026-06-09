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
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.store.tables.TStoreDeptRel
import com.tencent.devops.project.api.service.ServiceProjectOrganizationResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.store.common.dao.StoreDeptRelDao
import com.tencent.devops.store.common.dao.StoreMemberDao
import com.tencent.devops.store.common.dao.StoreVisibleProjectRelDao
import com.tencent.devops.store.common.service.StoreVisibleDeptService
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.pojo.common.enums.DeptStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.visible.DeptInfo
import com.tencent.devops.store.pojo.common.visible.StoreVisibleDeptResp
import com.tencent.devops.store.pojo.common.visible.StoreVisibleProjectInfo
import com.tencent.devops.store.pojo.common.visible.UserStoreDeptInfoRequest
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * store组件可见范围逻辑类
 * since: 2019-01-08
 */
@Service
class StoreVisibleDeptServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val storeDeptRelDao: StoreDeptRelDao,
    private val storeMemberDao: StoreMemberDao,
    private val storeVisibleProjectRelDao: StoreVisibleProjectRelDao
) : StoreVisibleDeptService {

    private val logger = LoggerFactory.getLogger(StoreVisibleDeptServiceImpl::class.java)

    /**
     * 查看store组件可见范围
     */
    override fun getVisibleDept(
        storeCode: String,
        storeType: StoreTypeEnum,
        deptStatusInfos: String?
    ): Result<StoreVisibleDeptResp?> {
        val deptStatusList = deptStatusInfos?.split(",")?.map {
            DeptStatusEnum.valueOf(it).status.toByte()
        }
        val storeDeptRelRecords = storeDeptRelDao.getDeptInfosByStoreCode(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeType.type.toByte(),
            deptStatusList = deptStatusList
        )
        // 查询按项目授权的可见范围（仅部分组件类型如DEVX会存在数据）
        val projectInfos = storeVisibleProjectRelDao.getProjectInfosByStoreCode(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeType.type.toByte()
        ).map {
            StoreVisibleProjectInfo(projectCode = it.projectCode, projectName = it.projectName)
        }.ifEmpty { null }
        return Result(
            if (storeDeptRelRecords == null && projectInfos == null) {
                null
            } else {
                var fullScopeVisible = false
                val deptInfos = mutableListOf<DeptInfo>()
                storeDeptRelRecords?.forEach {
                    if (!fullScopeVisible) {
                        // 判断该组件的可见范围是否设置了全公司可见，层级为0，最顶层部门，为全公司
                        fullScopeVisible = client.get(ServiceProjectOrganizationResource::class)
                            .getDeptInfo(
                                userId = null,
                                id = it.deptId
                            ).data?.level?.toInt() == 0
                    }
                    deptInfos.add(
                        DeptInfo(
                            deptId = it.deptId,
                            deptName = it.deptName,
                            status = DeptStatusEnum.getStatus(it.status.toInt()),
                            comment = it.comment
                        )
                    )
                }
                StoreVisibleDeptResp(
                    deptInfos = deptInfos,
                    fullScopeVisible = fullScopeVisible,
                    projectInfos = projectInfos
                )
            }
        )
    }

    /**
     * 批量获取已经审核通过的可见范围
     */
    override fun batchGetVisibleDept(
        storeCodeList: List<String>,
        storeType: StoreTypeEnum
    ): Result<HashMap<String, MutableList<Int>>> {
        val ret = hashMapOf<String, MutableList<Int>>()
        val storeDeptRelRecords = storeDeptRelDao.batchList(
            dslContext = dslContext,
            storeCodeList = storeCodeList,
            storeType = storeType.type.toByte()
        )
        val tStoreDeptRel = TStoreDeptRel.T_STORE_DEPT_REL
        storeDeptRelRecords?.forEach {
            val list = if (ret.containsKey(it[tStoreDeptRel.STORE_CODE] as String)) {
                ret[it[tStoreDeptRel.STORE_CODE] as String]!!
            } else {
                val tmp = mutableListOf<Int>()
                ret[it[tStoreDeptRel.STORE_CODE] as String] = tmp
                tmp
            }
            list.add(it[tStoreDeptRel.DEPT_ID] as Int)
        }
        return Result(ret)
    }

    /**
     * 设置store组件可见范围
     */
    override fun addVisibleDept(
        userId: String,
        storeCode: String,
        deptInfos: List<DeptInfo>?,
        storeType: StoreTypeEnum,
        projectInfos: List<StoreVisibleProjectInfo>?
    ): Result<Boolean> {
        logger.info(
            "addVisibleDept userId:$userId,storeCode:$storeCode,deptInfos:$deptInfos," +
                "storeType:$storeType,projectInfos:$projectInfos"
        )
        // 判断用户是否有权限设置可见范围
        if (!storeMemberDao.isStoreAdmin(
                dslContext = dslContext,
                userId = userId,
                storeCode = storeCode,
                storeType = storeType.type.toByte()
            )) {
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PERMISSION_DENIED,
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        }
        // 设置按组织架构的可见范围
        if (!deptInfos.isNullOrEmpty()) {
            val pendingDeptInfoList = mutableListOf<DeptInfo>()
            deptInfos.forEach forEach@{
                val count = storeDeptRelDao.countByCodeAndDeptId(
                    dslContext = dslContext,
                    storeCode = storeCode,
                    deptId = it.deptId,
                    storeType = storeType.type.toByte()
                )
                if (count > 0) {
                    return@forEach
                }
                pendingDeptInfoList.add(it)
            }
            storeDeptRelDao.batchAdd(
                dslContext = dslContext,
                userId = userId,
                storeCode = storeCode,
                deptInfoList = pendingDeptInfoList,
                storeType = storeType.type.toByte()
            )
        }
        // 设置按项目的可见范围（保存前校验项目ID合法性及用户权限）
        if (!projectInfos.isNullOrEmpty()) {
            addVisibleProjects(
                userId = userId,
                storeCode = storeCode,
                storeType = storeType,
                projectInfos = projectInfos
            )
        }
        return Result(true)
    }

    /**
     * 校验并保存组件按项目授权的可见范围。
     * 校验内容：项目ID是否存在，以及当前用户是否拥有该项目的权限；任一不满足则保存失败。
     */
    private fun addVisibleProjects(
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        projectInfos: List<StoreVisibleProjectInfo>
    ) {
        // 去重，避免重复校验与写入
        val distinctProjectInfos = projectInfos.distinctBy { it.projectCode }
        val projectCodes = distinctProjectInfos.map { it.projectCode }
        val validProjectNameMap = validateVisibleProjects(userId = userId, projectCodes = projectCodes)
        // 补全项目名称（优先使用前端传入的名称，缺省时使用项目服务返回的名称）
        distinctProjectInfos.forEach { it.projectName = it.projectName ?: validProjectNameMap[it.projectCode] }
        storeVisibleProjectRelDao.batchAdd(
            dslContext = dslContext,
            userId = userId,
            storeCode = storeCode,
            storeType = storeType.type.toByte(),
            projectInfoList = distinctProjectInfos
        )
    }

    /**
     * 校验用户填写的项目可见范围是否合法。
     * @return 合法项目的 projectCode -> projectName 映射
     */
    private fun validateVisibleProjects(userId: String, projectCodes: List<String>): Map<String, String> {
        if (projectCodes.isEmpty()) return emptyMap()
        val serviceProjectResource = client.get(ServiceProjectResource::class)
        // 批量获取存在的项目名称，既用于回显也用于校验项目ID是否存在
        val projectNameMap = try {
            serviceProjectResource.getNameByCode(projectCodes.joinToString(",")).data
        } catch (ignored: Throwable) {
            logger.warn("validateVisibleProjects getNameByCode error, projectCodes:$projectCodes", ignored)
            null
        } ?: emptyMap()
        val invalidProjectCodes = projectCodes.filter { projectCode ->
            // 项目不存在直接判定为非法
            if (!projectNameMap.containsKey(projectCode)) {
                return@filter true
            }
            // 校验用户是否拥有该项目的权限
            val hasPermission = try {
                serviceProjectResource
                    .verifyUserProjectPermission(projectCode = projectCode, userId = userId).data ?: false
            } catch (ignored: Throwable) {
                logger.warn("validateVisibleProjects verifyPermission error, $userId|$projectCode", ignored)
                false
            }
            !hasPermission
        }
        if (invalidProjectCodes.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.USER_STORE_VISIBLE_PROJECT_INVALID,
                params = arrayOf(invalidProjectCodes.joinToString(","))
            )
        }
        return projectNameMap
    }

    /**
     * 删除store组件可见范围
     */
    override fun deleteVisibleDept(
        userId: String,
        storeCode: String,
        deptIds: String?,
        storeType: StoreTypeEnum,
        projectCodes: String?
    ): Result<Boolean> {
        logger.info(
            "deleteVisibleDept userId:$userId,storeCode:$storeCode,deptIds:$deptIds," +
                "storeType:$storeType,projectCodes:$projectCodes"
        )
        // 判断用户是否有权限删除可见范围
        if (!storeMemberDao.isStoreAdmin(
                dslContext = dslContext,
                userId = userId,
                storeCode = storeCode,
                storeType = storeType.type.toByte()
            )) {
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PERMISSION_DENIED,
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        }
        if (!deptIds.isNullOrBlank()) {
            val deptIdIntList = deptIds.split(",").filter { it.isNotBlank() }.map { it.trim().toInt() }
            if (deptIdIntList.isNotEmpty()) {
                storeDeptRelDao.batchDelete(
                    dslContext = dslContext,
                    storeCode = storeCode,
                    deptIdList = deptIdIntList,
                    storeType = storeType.type.toByte()
                )
            }
        }
        if (!projectCodes.isNullOrBlank()) {
            val projectCodeList = projectCodes.split(",").filter { it.isNotBlank() }.map { it.trim() }
            if (projectCodeList.isNotEmpty()) {
                storeVisibleProjectRelDao.batchDelete(
                    dslContext = dslContext,
                    storeCode = storeCode,
                    storeType = storeType.type.toByte(),
                    projectCodeList = projectCodeList
                )
            }
        }
        return Result(true)
    }

    override fun checkUserInvalidVisibleStoreInfo(
        userStoreDeptInfoRequest: UserStoreDeptInfoRequest
    ): Boolean {
        // 如果是公共组件，则无需校验与用户的可见范围
        if (!userStoreDeptInfoRequest.publicFlag) {
            val isStoreMember = storeMemberDao.isStoreMember(
                dslContext = dslContext,
                userId = userStoreDeptInfoRequest.userId,
                storeCode = userStoreDeptInfoRequest.storeCode,
                storeType = userStoreDeptInfoRequest.storeType.type.toByte()
            )
            return getValidStoreFlag(
                isStoreMember = isStoreMember,
                storeDepInfoList = userStoreDeptInfoRequest.storeDepInfoList,
                userDeptIdList = userStoreDeptInfoRequest.userDeptIdList
            )
        }
        return true
    }

    private fun getValidStoreFlag(
        isStoreMember: Boolean,
        storeDepInfoList: List<DeptInfo>?,
        userDeptIdList: List<Int>
    ): Boolean {
        return if (isStoreMember) {
            true
        } else {
            validateStoreDept(storeDepInfoList, userDeptIdList)
        }
    }

    private fun validateStoreDept(
        storeDepInfoList: List<DeptInfo>?,
        userDeptIdList: List<Int>
    ): Boolean {
        var flag = false
        run breaking@{
            storeDepInfoList?.forEach deptEach@{ storeDepInfo ->
                val storeDeptId = storeDepInfo.deptId
                flag = validateDeptId(storeDeptId, userDeptIdList)
                if (flag) return@breaking
            }
        }
        return flag
    }

    private fun validateDeptId(storeDeptId: Int, userDeptIdList: List<Int>): Boolean {
        // 如果部门ID为0（全公司可见）或者用户属于该部门，直接通过
        if (storeDeptId == 0 || storeDeptId in userDeptIdList) {
            return true
        }
        // 检查该部门是否没有上级部门（即全公司可见）
        val parentDeptInfos = client.get(ServiceProjectOrganizationResource::class)
            .getParentDeptInfos(storeDeptId.toString(), 1).data
        // 只有当 parentDeptInfos 不为 null 且为空列表时，才表示全公司可见
        return parentDeptInfos != null && parentDeptInfos.isEmpty()
    }
}

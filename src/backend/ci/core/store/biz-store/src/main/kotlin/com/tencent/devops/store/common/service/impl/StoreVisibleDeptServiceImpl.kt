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
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.store.tables.TStoreDeptRel
import com.tencent.devops.project.api.service.ServiceProjectOrganizationResource
import com.tencent.devops.store.common.dao.StoreDeptRelDao
import com.tencent.devops.store.common.dao.StoreMemberDao
import com.tencent.devops.store.common.service.StoreVisibleDeptService
import com.tencent.devops.store.pojo.common.visible.UserStoreDeptInfoRequest
import com.tencent.devops.store.pojo.common.enums.DeptStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.visible.DeptInfo
import com.tencent.devops.store.pojo.common.visible.StoreVisibleDeptResp
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
    private val storeMemberDao: StoreMemberDao
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
        return Result(
            if (storeDeptRelRecords == null) {
                null
            } else {
                val deptInfos = mutableListOf<DeptInfo>()
                storeDeptRelRecords.forEach {
                    deptInfos.add(DeptInfo(
                        deptId = it.deptId,
                        deptName = it.deptName,
                        status = DeptStatusEnum.getStatus(it.status.toInt()),
                        comment = it.comment
                    ))
                }
                StoreVisibleDeptResp(deptInfos)
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
        deptInfos: List<DeptInfo>,
        storeType: StoreTypeEnum
    ): Result<Boolean> {
        logger.info("addVisibleDept userId:$userId,storeCode:$storeCode,deptInfos:$deptInfos,storeType:$storeType")
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
        return Result(true)
    }

    /**
     * 删除store组件可见范围
     */
    override fun deleteVisibleDept(
        userId: String,
        storeCode: String,
        deptIds: String,
        storeType: StoreTypeEnum
    ): Result<Boolean> {
        logger.info("deleteVisibleDept userId:$userId,storeCode:$storeCode,deptIds:$deptIds,storeType:$storeType")
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
        val deptIdIntList = mutableListOf<Int>()
        val deptIdStrList = deptIds.split(",")
        deptIdStrList.forEach {
            deptIdIntList.add(it.toInt())
        }
        storeDeptRelDao.batchDelete(
            dslContext = dslContext,
            storeCode = storeCode,
            deptIdList = deptIdIntList,
            storeType = storeType.type.toByte()
        )
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

    private fun validateDeptId(
        storeDeptId: Int,
        userDeptIdList: List<Int>
    ): Boolean {
        if (storeDeptId == 0 || userDeptIdList.contains(storeDeptId)) {
            return true // 用户在组件的可见范围内
        } else {
            // 判断该组件的可见范围是否设置了全公司可见
            val parentDeptInfoList = client.get(ServiceProjectOrganizationResource::class)
                .getParentDeptInfos(storeDeptId.toString(), 1).data
            if (null != parentDeptInfoList && parentDeptInfoList.isEmpty()) {
                // 没有上级机构说明设置的可见范围是全公司
                return true // 用户在组件的可见范围内
            }
            return false
        }
    }
}

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

package com.tencent.devops.store.service.common.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.store.dao.common.StoreDeptRelDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.pojo.common.DeptInfo
import com.tencent.devops.store.pojo.common.PASS
import com.tencent.devops.store.pojo.common.StoreVisibleDeptResp
import com.tencent.devops.store.pojo.common.VisibleApproveReq
import com.tencent.devops.store.pojo.common.enums.DeptStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreVisibleDeptService
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
    private val storeDeptRelDao: StoreDeptRelDao,
    private val storeMemberDao: StoreMemberDao
) : StoreVisibleDeptService {

    private val logger = LoggerFactory.getLogger(StoreVisibleDeptServiceImpl::class.java)

    /**
     * 查看store组件可见范围
     */
    override fun getVisibleDept(storeCode: String, storeType: StoreTypeEnum, deptStatus: DeptStatusEnum?): Result<StoreVisibleDeptResp?> {
        logger.info("the storeCode is :$storeCode,storeType is :$storeType,deptStatus is :$deptStatus")
        val storeDeptRelRecords = storeDeptRelDao.getDeptInfosByStoreCode(dslContext, storeCode, storeType.type.toByte(), deptStatus, null)
        logger.info("the storeDeptRelRecords is :$storeDeptRelRecords")
        return Result(
            if (storeDeptRelRecords == null) {
                null
            } else {
                val deptInfos = mutableListOf<DeptInfo>()
                storeDeptRelRecords.forEach {
                    deptInfos.add(DeptInfo(it.deptId, it.deptName, DeptStatusEnum.getStatus(it.status.toInt()), it.comment))
                }
                StoreVisibleDeptResp(deptInfos)
            }
        )
    }

    /**
     * 批量获取已经审核通过的可见范围
     */
    override fun batchGetVisibleDept(storeCodeList: List<String?>, storeType: StoreTypeEnum): Result<HashMap<String, MutableList<Int>>> {
        val ret = hashMapOf<String, MutableList<Int>>()
        val storeDeptRelRecords = storeDeptRelDao.batchList(dslContext, storeCodeList, storeType.type.toByte())
        storeDeptRelRecords?.forEach {
            val list = if (ret.containsKey(it["STORE_CODE"] as String)) {
                ret[it["STORE_CODE"] as String]!!
            } else {
                val tmp = mutableListOf<Int>()
                ret[it["STORE_CODE"] as String] = tmp
                tmp
            }
            list.add(it["DEPT_ID"] as Int)
        }
        return Result(ret)
    }

    /**
     * 设置store组件可见范围
     */
    override fun addVisibleDept(userId: String, storeCode: String, deptInfos: List<DeptInfo>, storeType: StoreTypeEnum): Result<Boolean> {
        logger.info("addVisibleDept userId is :$userId,storeCode is :$storeCode,deptInfos is :$deptInfos,storeType is :$storeType")
        // 判断用户是否有权限设置可见范围
        if (!storeMemberDao.isStoreAdmin(dslContext, userId, storeCode, storeType.type.toByte())) {
            return MessageCodeUtil.generateResponseDataObject(messageCode = CommonMessageCode.PERMISSION_DENIED, data = false)
        }
        val deptIdApprovedList = mutableListOf<DeptInfo>()
        deptInfos.forEach forEach@{
            val count = storeDeptRelDao.countByCodeAndDeptId(dslContext, storeCode, it.deptId, storeType.type.toByte())
            if (count>0) {
                return@forEach
            }
            deptIdApprovedList.add(it)
        }
        // 可见范围默认审核通过
        storeDeptRelDao.batchAdd(
            dslContext = dslContext,
            userId = userId,
            storeCode = storeCode,
            deptInfoList = deptIdApprovedList,
            status = DeptStatusEnum.APPROVED.status.toByte(),
            comment = "AUTO APPROVE",
            storeType = storeType.type.toByte()
        )

        return Result(true)
    }

    /**
     * 删除store组件可见范围
     */
    override fun deleteVisibleDept(userId: String, storeCode: String, deptIds: String, storeType: StoreTypeEnum): Result<Boolean> {
        logger.info("the userId is :$userId,storeCode is :$storeCode,deptIds is :$deptIds,storeType is :$storeType")
        // 判断用户是否有权限删除可见范围
        if (!storeMemberDao.isStoreAdmin(dslContext, userId, storeCode, storeType.type.toByte())) {
            return MessageCodeUtil.generateResponseDataObject(messageCode = CommonMessageCode.PERMISSION_DENIED, data = false)
        }
        val deptIdIntList = mutableListOf<Int>()
        val deptIdStrList = deptIds.split(",")
        deptIdStrList.forEach {
            deptIdIntList.add(it.toInt())
        }
        storeDeptRelDao.batchDelete(dslContext, storeCode, deptIdIntList, storeType.type.toByte())
        return Result(true)
    }

    /**
     * 审核可见范围
     */
    override fun approveVisibleDept(userId: String, storeCode: String, visibleApproveReq: VisibleApproveReq, storeType: StoreTypeEnum): Result<Boolean> {
        val deptIdIntList = visibleApproveReq.deptIdList
        logger.info("approveVisibleDept, the userId is :$userId,storeCode is :$storeCode,deptIds is :$deptIdIntList,storeType is :$storeType")
        val status =
            if (visibleApproveReq.result == PASS) {
                DeptStatusEnum.APPROVED.status.toByte()
            } else {
                DeptStatusEnum.REJECT.status.toByte()
            }

        storeDeptRelDao.batchUpdate(dslContext, userId, storeCode, deptIdIntList, status, visibleApproveReq.message, storeType.type.toByte())

        return Result(true)
    }
}
